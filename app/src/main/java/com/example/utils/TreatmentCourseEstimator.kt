package com.example.utils

import com.example.data.*
import java.util.Calendar
import java.util.Date
import kotlin.math.ceil

enum class SafetyStatus {
    NORMAL, WARNING, ELEVATED
}

enum class RefillUrgency {
    OK, REFILL_SOON, CRITICAL
}

data class LabSafetyAssessment(
    val status: SafetyStatus,
    val summaryAr: String,
    val summaryEn: String,
    val details: List<String>,
    val latestLabDateMillis: Long?
)

data class TreatmentEstimations(
    // 1. Course & Cumulative Target
    val totalTargetMg: Int,
    val totalConsumedMg: Int,
    val remainingTargetMg: Int,
    val cumulativeProgressPercent: Float,
    val activeDailyDoseMg: Int,
    val estimatedDaysToCourseCompletion: Int,
    val estimatedCourseCompletionDateMillis: Long,
    val currentTreatmentMonth: Int,
    val elapsedTreatmentDays: Int,

    // 2. Medication Supply & Dispensing Forecast
    val liveRemainingCapsules: Int,
    val liveRemainingDays: Int,
    val estimatedRunOutDateMillis: Long,
    val recommendedRefillDateMillis: Long,
    val refillDueUrgency: RefillUrgency,
    val activePrescription: MedicalPrescription?,
    val refillsUsedCount: Int,
    val refillsAllowedTotal: Int,

    // 3. Clinical Lab Schedule & Safety
    val latestLabAppointment: Appointment?,
    val daysSinceLastLab: Int?,
    val recommendedNextLabDateMillis: Long,
    val labSafetyAssessment: LabSafetyAssessment,
    val nextUpcomingClinicVisit: Appointment?,
    val nextUpcomingLabVisit: Appointment?
)

object TreatmentCourseEstimator {

    fun computeEstimations(
        userProfile: UserProfile,
        dispensedMedications: List<DispensedMedication>,
        allLogs: List<DailyLog>,
        allAppointments: List<Appointment>,
        allPrescriptions: List<MedicalPrescription>,
        todayMillis: Long = System.currentTimeMillis()
    ): TreatmentEstimations {
        val audit = computeRoaccutaneInventoryAudit(
            medications = dispensedMedications,
            courseStartDate = userProfile.startDate,
            allLogs = allLogs,
            todayMillis = todayMillis
        )

        // 1. Course calculations
        val totalTargetMg = userProfile.calculatedTargetDoseMg
        val totalConsumedMg = audit.actualCumulativeConsumedMg
        val remainingTargetMg = maxOf(0, totalTargetMg - totalConsumedMg)
        val cumulativeProgressPercent = if (totalTargetMg > 0) {
            ((totalConsumedMg.toFloat() / totalTargetMg.toFloat()) * 100f).coerceIn(0f, 100f)
        } else 0f

        val latestDispense = dispensedMedications.maxByOrNull { it.startDate }
        val strength = latestDispense?.capsuleStrengthMg ?: 20
        val activeDailyDoseMg = if (audit.dailyCapsulesRate > 0) {
            audit.dailyCapsulesRate * strength
        } else if (userProfile.dailyDoseMg > 0) {
            userProfile.dailyDoseMg
        } else 20

        val estimatedDaysToCourseCompletion = if (activeDailyDoseMg > 0) {
            ceil(remainingTargetMg.toDouble() / activeDailyDoseMg.toDouble()).toInt()
        } else 0

        val estimatedCourseCompletionDateMillis = todayMillis + (estimatedDaysToCourseCompletion.toLong() * 24L * 3600L * 1000L)

        val elapsedTreatmentDays = if (userProfile.startDate > 0) {
            val diff = todayMillis - userProfile.startDate
            maxOf(1, (diff / (24L * 3600L * 1000L)).toInt() + 1)
        } else 1

        val currentTreatmentMonth = maxOf(1, ((elapsedTreatmentDays - 1) / 30) + 1)

        // 2. Refill & Supply forecast
        val liveRemainingCaps = audit.trueRemainingCapsules
        val liveRemainingDays = audit.remainingDaysCount
        val estimatedRunOutDate = audit.estimatedRunOutDateMillis
        val recommendedRefillDate = maxOf(todayMillis, estimatedRunOutDate - (5L * 24L * 3600L * 1000L))

        val refillUrgency = when {
            liveRemainingDays <= 3 && liveRemainingCaps > 0 -> RefillUrgency.CRITICAL
            liveRemainingDays <= 7 && liveRemainingCaps > 0 -> RefillUrgency.REFILL_SOON
            else -> RefillUrgency.OK
        }

        val activeRx = allPrescriptions.find { rx ->
            dispensedMedications.any { it.prescriptionId == rx.id }
        } ?: allPrescriptions.maxByOrNull { it.prescriptionDate }

        val refillsUsed = if (activeRx != null) {
            dispensedMedications.count { it.prescriptionId == activeRx.id }
        } else 0
        val refillsAllowed = activeRx?.totalAllowedRefills ?: 1

        // 3. Lab schedule and clinical safety
        val labAppointments = allAppointments.filter {
            it.appointmentType == "LAB" || it.alt != null || it.creatinine != null || it.tCholestrol != null
        }.sortedByDescending { it.dateTime }

        val latestLab = labAppointments.firstOrNull()
        val daysSinceLastLab = if (latestLab != null) {
            ((todayMillis - latestLab.dateTime) / (24L * 3600L * 1000L)).toInt()
        } else null

        val startOfTodayCal = Calendar.getInstance().apply {
            timeInMillis = todayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfTodayMillis = startOfTodayCal.timeInMillis

        val upcomingLabVisit = allAppointments
            .filter { it.dateTime >= startOfTodayMillis && it.appointmentType.equals("LAB", ignoreCase = true) && !it.isCompleted }
            .minByOrNull { it.dateTime }

        val upcomingClinicVisit = allAppointments
            .filter { it.dateTime >= startOfTodayMillis && !it.appointmentType.equals("LAB", ignoreCase = true) && !it.isCompleted }
            .minByOrNull { it.dateTime }

        val recommendedNextLabDateMillis = when {
            upcomingLabVisit != null -> {
                upcomingLabVisit.dateTime
            }
            upcomingClinicVisit != null -> {
                // Recommend 2 days before upcoming consultation so test results are ready
                maxOf(todayMillis, upcomingClinicVisit.dateTime - (2L * 24L * 3600L * 1000L))
            }
            latestLab != null -> {
                // 30 days after last lab
                latestLab.dateTime + (30L * 24L * 3600L * 1000L)
            }
            userProfile.startDate > 0 -> {
                userProfile.startDate + (30L * 24L * 3600L * 1000L)
            }
            else -> todayMillis + (30L * 24L * 3600L * 1000L)
        }

        // Safety assessment
        val safetyAssessment = evaluateLabSafety(latestLab)

        return TreatmentEstimations(
            totalTargetMg = totalTargetMg,
            totalConsumedMg = totalConsumedMg,
            remainingTargetMg = remainingTargetMg,
            cumulativeProgressPercent = cumulativeProgressPercent,
            activeDailyDoseMg = activeDailyDoseMg,
            estimatedDaysToCourseCompletion = estimatedDaysToCourseCompletion,
            estimatedCourseCompletionDateMillis = estimatedCourseCompletionDateMillis,
            currentTreatmentMonth = currentTreatmentMonth,
            elapsedTreatmentDays = elapsedTreatmentDays,
            liveRemainingCapsules = liveRemainingCaps,
            liveRemainingDays = liveRemainingDays,
            estimatedRunOutDateMillis = estimatedRunOutDate,
            recommendedRefillDateMillis = recommendedRefillDate,
            refillDueUrgency = refillUrgency,
            activePrescription = activeRx,
            refillsUsedCount = refillsUsed,
            refillsAllowedTotal = refillsAllowed,
            latestLabAppointment = latestLab,
            daysSinceLastLab = daysSinceLastLab,
            recommendedNextLabDateMillis = recommendedNextLabDateMillis,
            labSafetyAssessment = safetyAssessment,
            nextUpcomingClinicVisit = upcomingClinicVisit,
            nextUpcomingLabVisit = upcomingLabVisit
        )
    }

    private fun evaluateLabSafety(appt: Appointment?): LabSafetyAssessment {
        if (appt == null) {
            return LabSafetyAssessment(
                status = SafetyStatus.NORMAL,
                summaryAr = "لم تُسجل تحاليل مخبرية بعد",
                summaryEn = "No lab tests logged yet",
                details = listOf("يُنصح بجدولة تحاليل وظائف الكبد والدهنيات شهرياً"),
                latestLabDateMillis = null
            )
        }

        val warnings = mutableListOf<String>()
        var hasElevated = false

        val alt = appt.alt
        val ast = appt.ast
        val tg = appt.triglycerides
        val tc = appt.tCholestrol
        val creat = appt.creatinine

        if (alt != null && alt > 56.0) {
            warnings.add("إنزيم الكبد ALT مرتفع (${alt} U/L)")
            hasElevated = true
        }
        if (ast != null && ast > 40.0) {
            warnings.add("إنزيم الكبد AST مرتفع (${ast} U/L)")
            hasElevated = true
        }
        if (tg != null && tg > 150.0) {
            warnings.add("الدهون الثلاثية مرتفعة (${tg} mg/dL)")
            hasElevated = true
        }
        if (tc != null && tc > 200.0) {
            warnings.add("الكوليسترول الكلي مرتفع (${tc} mg/dL)")
            hasElevated = true
        }
        if (creat != null && creat > 1.2) {
            warnings.add("مؤشر الكرياتينين مرتفع (${creat} mg/dL)")
            hasElevated = true
        }

        return if (hasElevated) {
            LabSafetyAssessment(
                status = SafetyStatus.ELEVATED,
                summaryAr = "⚠️ تنبيه أمان: بعض المؤشرات المخبرية مرتفعة",
                summaryEn = "⚠️ Clinical Alert: Elevated lab biomarkers",
                details = warnings,
                latestLabDateMillis = appt.dateTime
            )
        } else {
            LabSafetyAssessment(
                status = SafetyStatus.NORMAL,
                summaryAr = "المؤشرات الحيوية ووظائف الكبد آمنة وطبيعية ✓",
                summaryEn = "Biomarkers & liver enzymes are normal ✓",
                details = listOf("كافة التحاليل المسجلة ضمن النطاق الآمن المعتمد"),
                latestLabDateMillis = appt.dateTime
            )
        }
    }
}
