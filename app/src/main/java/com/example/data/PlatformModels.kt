package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val titleAr: String, val titleEn: String) {
    PATIENT("حساب المريض", "Patient Profile")
}

@Entity(tableName = "vital_signs")
data class VitalSign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // YYYY-MM-DD
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val bloodSugar: Float? = null, // mg/dL
    val weightKg: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clinics")
data class Clinic(
    @PrimaryKey val id: String,
    val name: String,
    val logoResName: String = "clinic_default",
    val planType: String = "CLINIC_STANDARD",
    val status: String = "Active", // Active, Trial, Suspended
    val trialEndDate: Long = System.currentTimeMillis() + (14L * 24 * 3600 * 1000),
    val monthlyFeeSAR: Double = 499.0,
    val annualFeeSAR: Double = 4990.0,
    val contactEmail: String = "clinic@example.com",
    val contactPhone: String = "+966 11 234 5678",
    val isFrozen: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey val id: String,
    val clinicId: String,
    val name: String,
    val specialty: String,
    val workHours: String = "09:00 ص - 05:00 م",
    val pairingCode: String,
    val phone: String = "",
    val bio: String = "",
    val consultationFee: Double = 150.0,
    val signatureUri: String = "",
    val stampUri: String = "",
    val licenseDocUri: String = "",
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "patient_pairings")
data class PatientPairing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val clinicId: String,
    val doctorId: String,
    val pairingCode: String,
    val status: String = "Approved", // Approved, Pending, Revoked
    val treatmentPhase: String = "الأسبوع 8 (شهر 2)",
    val adherenceRate: Int = 92, // percentage
    val pairedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "doctor_notes")
data class DoctorNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val doctorId: String,
    val doctorName: String,
    val patientId: String,
    val dateStr: String,
    val title: String,
    val content: String,
    val isTemplate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_records")
data class FinancialRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // REVENUE or EXPENSE
    val category: String, // Clinic Subscription, Individual Subscription, Affiliate, Server Hosting, Payment Gateway, Marketing
    val source: String,
    val amount: Double,
    val currency: String = "SAR",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "feature_usage_records")
data class FeatureUsageRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val featureKey: String, // AI_ASSISTANT, PDF_EXPORT, VITALS_TRACKING, DOSE_LOGGING
    val count: Int,
    val estimatedCostSAR: Double,
    val periodMonthYear: String // e.g. "2026-09"
)

@Entity(tableName = "subscription_plans")
data class SubscriptionPlanConfig(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val priceMonthlySAR: Double,
    val priceAnnualSAR: Double,
    val isAiEnabled: Boolean = true,
    val dailyAiLimit: Int = 5,
    val maxPdfExports: Int = 3,
    val trialDays: Int = 14,
    val isPublished: Boolean = true,
    val featuresList: String = "ذكاء اصطناعي طبي, تصدير تقارير PDF, تحليلات سريرية, دعم على مدار الساعة"
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actorRole: String,
    val actorName: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "affiliate_products")
data class AffiliateProduct(
    @PrimaryKey val id: String,
    val partnerName: String, // iHerb, Amazon, Noon, Al-Dawaa
    val productNameAr: String,
    val descriptionAr: String = "",
    val categoryAr: String = "عناية وترطيب",
    val priceSAR: Double,
    val commissionPercent: Double = 10.0,
    val monthlyClicks: Int = 0,
    val monthlySales: Int = 0,
    val relevanceScore: Double = 0.95,
    val rankOrder: Int = 1,
    val url: String = "https://example.com",
    val imageUri: String = "",
    val manufacturerUrl: String = "",
    val pharmacyUrl: String = ""
)

@Entity(tableName = "owner_change_logs")
data class OwnerChangeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // EDIT_PRICE, ADD_PRODUCT, EDIT_PRODUCT, DELETE_PRODUCT, DELETE_DATA, CANCEL_SUBSCRIPTION
    val title: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- AI Advisor Specific Models ---

data class PricingRecommendation(
    val title: String,
    val currentPriceMonthlySAR: Double,
    val recommendedPriceMonthlySAR: Double,
    val currentPriceAnnualSAR: Double,
    val recommendedPriceAnnualSAR: Double,
    val discountPercent: Double,
    val targetSegment: String, // "Clinics" or "Individuals"
    val rationaleAr: String,
    val expectedRevenueImpactPercent: Double,
    val isApplied: Boolean = false
)

data class FeatureRoiRecommendation(
    val featureKey: String,
    val featureNameAr: String,
    val monthlyCalls: Int,
    val monthlyCostSAR: Double,
    val activeUsersCount: Int,
    val usagePercentageAmongFreeUsers: Double,
    val recommendationType: String, // "OPTIMIZE", "TIER_UPGRADE", "RATE_LIMIT", "MAINTAIN"
    val actionProposalAr: String,
    val isBlockedByGuardrail: Boolean = false,
    val guardrailReasonAr: String = "",
    val isApplied: Boolean = false
)

data class ChurnPredictionAlert(
    val entityId: String,
    val entityName: String,
    val entityType: String, // "CLINIC" or "PATIENT"
    val dropPercentage: Double, // e.g. 45.0%
    val daysInactive: Int,
    val recommendedActionAr: String,
    val actionType: String, // "SALES_CALL" or "PROACTIVE_DISCOUNT"
    val isAddressed: Boolean = false
)

data class AffiliateRoiOptimization(
    val partnerKey: String,
    val partnerName: String,
    val currentRank: Int,
    val recommendedRank: Int,
    val conversionRate: Double,
    val totalCommissionSAR: Double,
    val rationaleAr: String,
    val isApplied: Boolean = false
)

data class SuperAdminAdvisorReport(
    val generatedAt: Long = System.currentTimeMillis(),
    val totalActiveClinics: Int,
    val totalActiveDoctors: Int,
    val totalActivePatients: Int,
    val renewalRatePercent: Double,
    val totalRevenueSAR: Double,
    val totalOperationalCostSAR: Double,
    val netProfitSAR: Double,
    val pricingRecommendations: List<PricingRecommendation>,
    val featureRoiRecommendations: List<FeatureRoiRecommendation>,
    val churnAlerts: List<ChurnPredictionAlert>,
    val affiliateOptimizations: List<AffiliateRoiOptimization>,
    val sdaiaComplianceCertified: Boolean = true,
    val guardrailProtectionActive: Boolean = true
)

@Entity(tableName = "medical_prescriptions")
data class MedicalPrescription(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val prescriptionNumber: String = "",
    val medicationName: String,
    val doctorName: String = "",
    val clinicOrHospital: String = "",
    val dailyDose: String = "20 ملغ",
    val capsuleStrengthMg: Int = 20,
    val dailyCapsulesCount: Int = 1,
    val capsulesPerBox: Int = 30,
    val totalAllowedRefills: Int = 3,
    val targetDurationDays: Int = 90,
    val prescriptionDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (90L * 24L * 3600L * 1000L),
    val badgeColorHex: String = "#1976D2",
    val notes: String = "",
    val attachedFileUris: String = "",
    val attachedFileNames: String = "",
    val prescriptionUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = expiryDate > 0 && System.currentTimeMillis() > expiryDate

    val daysUntilExpiry: Int
        get() {
            val diff = expiryDate - System.currentTimeMillis()
            return if (diff > 0) ((diff / (24L * 3600L * 1000L)).toInt() + 1) else 0
        }
}

@Entity(tableName = "dispensed_medications")
data class DispensedMedication(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val medicationName: String,
    val sourceProductId: String? = null,
    val boxCount: Int = 1,
    val dailyDose: String = "20 ملغ",
    val durationDays: Int = 30,
    val startDate: Long = System.currentTimeMillis(),
    val missedDays: Int = 0,
    val badgeColorHex: String = "#1976D2",
    val notes: String = "",
    val attachedFileUris: String = "",
    val attachedFileNames: String = "",
    val prescriptionUrl: String = "",
    val dispenseImageUri: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val capsuleStrengthMg: Int = 20,
    val dailyCapsulesCount: Int = 1,
    val capsulesPerBox: Int = 30,
    val prescriptionId: String? = null,
    val dispenseNumber: Int = 1
) {
    val calculatedEndDate: Long
        get() = startDate + ((durationDays + missedDays).toLong() * 24L * 3600L * 1000L)

    val totalCapsules: Int
        get() = boxCount * (if (capsulesPerBox > 0) capsulesPerBox else 30)

    val totalDispensedMg: Int
        get() = totalCapsules * (if (capsuleStrengthMg > 0) capsuleStrengthMg else 20)

    val remainingDaysUntilEnd: Int
        get() {
            val now = System.currentTimeMillis()
            val diff = calculatedEndDate - now
            return if (diff > 0) ((diff / (24L * 3600L * 1000L)).toInt() + 1) else 0
        }

    val remainingCapsulesCount: Int
        get() {
            val total = totalCapsules
            val daysTotal = (durationDays + missedDays).coerceAtLeast(1)
            val remDays = remainingDaysUntilEnd
            val rate = total.toDouble() / daysTotal
            return (remDays * rate).toInt().coerceIn(0, total)
        }

    val consumedCapsulesCount: Int
        get() = (totalCapsules - remainingCapsulesCount).coerceAtLeast(0)
}

data class HospitalProfile(
    val hospitalName: String,
    val patientFileNumber: String = "",
    val doctorName: String = "",
    val clinic: String = "",
    val building: String = "",
    val hospitalLocation: String = ""
)

data class DoctorProfile(
    val doctorName: String,
    val clinic: String = "",
    val hospitalName: String = "",
    val building: String = ""
)

data class DispensedMedDefaults(
    val medName: String = "",
    val dailyDose: String = "20 ملغ",
    val durationDays: Int = 30,
    val boxCount: Int = 1,
    val capsulesPerBox: Int = 30,
    val colorHex: String = "#1976D2",
    val notes: String = "",
    val capsuleStrengthMg: Int = 20,
    val dailyCapsulesCount: Int = 1
)

enum class BoxConsumptionStatus {
    SEALED_IN_STOCK,    // علبة كاملة مغلقة في المخزون
    CURRENTLY_OPEN,     // العلبة الحالية المفتوحة قيد الاستهلاك
    FULLY_CONSUMED      // مستهلكة بالكامل
}

data class BoxInventoryItem(
    val boxId: String,
    val globalBoxIndex: Int,              // 1, 2, 3...
    val boxIndexInDispense: Int,          // 1..boxCount
    val dispenseId: String,
    val dispenseNumber: Int,
    val dispenseName: String,
    val dispenseDate: Long,
    val capsuleStrengthMg: Int,          // 20 or 10
    val totalCapsulesInBox: Int,         // 30
    val totalMgInBox: Int,               // 600 or 300
    val consumedCapsules: Int,           // 0..30
    val remainingCapsules: Int,          // 0..30
    val consumedMg: Int,                 // 0..600
    val remainingMg: Int,                // 0..600
    val status: BoxConsumptionStatus
)

data class DispensePhaseDetail(
    val dispenseId: String,
    val phaseNumber: Int,
    val dispenseName: String,
    val startDate: Long,
    val calculatedEndDate: Long,
    val durationDays: Int,
    val missedDays: Int,
    val dailyDoseMg: Int,
    val dailyCapsulesCount: Int,
    val capsuleStrengthMg: Int,
    val boxCount: Int,
    val totalCapsules: Int,
    val totalDispensedMg: Int,
    val elapsedActiveDays: Int,
    val phaseConsumedMg: Int,
    val phaseRemainingMg: Int,
    val phaseConsumedCapsules: Int,
    val phaseRemainingCapsules: Int,
    val isActiveNow: Boolean,
    val isPast: Boolean,
    val isFuture: Boolean
)

data class StrengthInventorySummary(
    val strengthMg: Int,                 // 20 or 10
    val totalBoxesDispensed: Int,
    val totalCapsulesDispensed: Int,
    val totalMgDispensed: Int,
    val fullSealedBoxesRemaining: Int,
    val openBoxCapsulesRemaining: Int,   // single capsules left in currently open box (0..29)
    val openBoxCapsulesConsumed: Int,    // single capsules consumed from currently open box (1..30)
    val hasOpenBox: Boolean,
    val totalCapsulesRemaining: Int,
    val totalMgRemaining: Int,
    val totalCapsulesConsumed: Int,
    val totalMgConsumed: Int,
    val fullyConsumedBoxesCount: Int
)

data class RoaccutaneInventoryAudit(
    val courseStartDate: Long,
    val elapsedDaysFromStart: Int,
    val grandTotalDispensedMg: Int,
    val grandTotalDispensedCapsules: Int,
    val grandTotalBoxes: Int,
    val actualCumulativeConsumedMg: Int,
    val actualCumulativeConsumedCapsules: Int,
    val trueRemainingMg: Int,
    val trueRemainingCapsules: Int,
    val consumptionPercent: Int,
    val strength20Mg: StrengthInventorySummary,
    val strength10Mg: StrengthInventorySummary,
    val otherStrengths: List<StrengthInventorySummary>,
    val phases: List<DispensePhaseDetail>,
    val boxes: List<BoxInventoryItem>,
    val currentOpenBoxes: List<BoxInventoryItem>,
    val fullSealedBoxesCount: Int,
    val fullyConsumedBoxesCount: Int,
    val confirmedDaysCount: Int = 0,
    val confirmedConsumedMg: Int = 0,
    val confirmedConsumedCapsules: Int = 0,
    val confirmedRemainingMg: Int = 0,
    val confirmedRemainingCapsules: Int = 0,
    val scheduledCumulativeConsumedMg: Int = 0,
    val scheduledRemainingMg: Int = 0,
    val reconciliationVarianceMg: Int = 0,
    val hasDailyLogs: Boolean = false,
    val calculationSource: String = "DAILY_LOGS",
    val dailyCapsulesRate: Int = 4,
    val remainingDaysCount: Int = 0,
    val estimatedRunOutDateMillis: Long = System.currentTimeMillis()
)

fun calculateFIFOMissedDaysForDispenses(
    medications: List<DispensedMedication>,
    courseStartDate: Long,
    allLogs: List<DailyLog>,
    todayMillis: Long = System.currentTimeMillis()
): Map<String, Int> {
    if (medications.isEmpty()) return emptyMap()

    val sortedMeds = medications.sortedWith(compareBy({ it.startDate }, { it.createdAt }))
    val effectiveStart = if (courseStartDate > 0) courseStartDate else (sortedMeds.firstOrNull()?.startDate ?: todayMillis)
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val logsMap = allLogs.associateBy { it.dateString }

    val todayCal = java.util.Calendar.getInstance().apply {
        timeInMillis = todayMillis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    var cursorCal = java.util.Calendar.getInstance().apply {
        timeInMillis = effectiveStart
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    val resultMap = mutableMapOf<String, Int>()

    sortedMeds.forEach { med ->
        val durationDays = med.durationDays.coerceAtLeast(1)
        val phaseStartCal = cursorCal.clone() as java.util.Calendar

        // If this phase's queue starts strictly after today, no missed days have occurred yet
        if (phaseStartCal.after(todayCal)) {
            resultMap[med.id] = 0
            cursorCal.add(java.util.Calendar.DAY_OF_YEAR, durationDays)
            return@forEach
        }

        var takenDaysInPhase = 0
        var missedDaysInPhase = 0

        while (takenDaysInPhase < durationDays && !cursorCal.after(todayCal)) {
            val dateStr = sdf.format(cursorCal.time)
            val log = logsMap[dateStr]

            if (log != null && log.isTaken && log.doseMg > 0) {
                takenDaysInPhase++
            } else {
                if (log != null && (!log.isTaken || log.doseMg == 0)) {
                    missedDaysInPhase++
                } else if (cursorCal.before(todayCal)) {
                    missedDaysInPhase++
                }
            }
            cursorCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val remainingUnconsumed = (durationDays - takenDaysInPhase).coerceAtLeast(0)
        cursorCal.add(java.util.Calendar.DAY_OF_YEAR, remainingUnconsumed)

        resultMap[med.id] = missedDaysInPhase
    }

    return resultMap
}

fun computeRoaccutaneInventoryAudit(
    medications: List<DispensedMedication>,
    courseStartDate: Long,
    allLogs: List<DailyLog> = emptyList(),
    todayMillis: Long = System.currentTimeMillis()
): RoaccutaneInventoryAudit {
    val todayCal = java.util.Calendar.getInstance().apply {
        timeInMillis = todayMillis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    if (medications.isEmpty()) {
        val emptyStrength20 = StrengthInventorySummary(20, 0, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0)
        val emptyStrength10 = StrengthInventorySummary(10, 0, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0)
        return RoaccutaneInventoryAudit(
            courseStartDate = courseStartDate,
            elapsedDaysFromStart = 0,
            grandTotalDispensedMg = 0,
            grandTotalDispensedCapsules = 0,
            grandTotalBoxes = 0,
            actualCumulativeConsumedMg = 0,
            actualCumulativeConsumedCapsules = 0,
            trueRemainingMg = 0,
            trueRemainingCapsules = 0,
            consumptionPercent = 0,
            strength20Mg = emptyStrength20,
            strength10Mg = emptyStrength10,
            otherStrengths = emptyList(),
            phases = emptyList(),
            boxes = emptyList(),
            currentOpenBoxes = emptyList(),
            fullSealedBoxesCount = 0,
            fullyConsumedBoxesCount = 0,
            confirmedDaysCount = 0,
            confirmedConsumedMg = 0,
            confirmedConsumedCapsules = 0,
            confirmedRemainingMg = 0,
            confirmedRemainingCapsules = 0,
            scheduledCumulativeConsumedMg = 0,
            scheduledRemainingMg = 0,
            reconciliationVarianceMg = 0,
            hasDailyLogs = false,
            calculationSource = "DAILY_LOGS",
            dailyCapsulesRate = 4,
            remainingDaysCount = 0,
            estimatedRunOutDateMillis = todayMillis
        )
    }

    val sortedMeds = medications.sortedWith(compareBy({ it.startDate }, { it.createdAt }))

    val effectiveStart = if (courseStartDate > 0) courseStartDate else (sortedMeds.firstOrNull()?.startDate ?: todayMillis)
    val startCal = java.util.Calendar.getInstance().apply {
        timeInMillis = effectiveStart
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    val elapsedDaysFromStart = if (!todayCal.before(startCal)) {
        ((todayCal.timeInMillis - startCal.timeInMillis) / (24L * 3600L * 1000L)).toInt() + 1
    } else 0

    val grandTotalDispensedMg = sortedMeds.sumOf { it.totalDispensedMg }
    val grandTotalDispensedCapsules = sortedMeds.sumOf { it.totalCapsules }
    val grandTotalBoxes = sortedMeds.sumOf { it.boxCount }

    val fifoMissedMap = if (allLogs.isNotEmpty()) {
        calculateFIFOMissedDaysForDispenses(sortedMeds, effectiveStart, allLogs, todayCal.timeInMillis)
    } else {
        emptyMap()
    }

    var queuedCursorMillis = startCal.timeInMillis

    val phases = sortedMeds.mapIndexed { idx, med ->
        val strength = if (med.capsuleStrengthMg > 0) med.capsuleStrengthMg else 20
        val dailyCaps = if (med.dailyCapsulesCount > 0) med.dailyCapsulesCount else 1
        val dailyDoseMg = strength * dailyCaps
        val duration = med.durationDays.coerceAtLeast(1)
        val missed = fifoMissedMap[med.id] ?: med.missedDays.coerceAtLeast(0)

        // FIFO Queued Start and End Dates:
        // Ensure subsequent dispenses (such as the 8-box 60-day refill) are queued chronologically in FIFO
        val phaseStartMillis = if (idx == 0) {
            med.startDate
        } else {
            maxOf(med.startDate, queuedCursorMillis)
        }
        val phaseEndMillis = phaseStartMillis + ((duration + missed).toLong() * 24L * 3600L * 1000L)
        queuedCursorMillis = phaseEndMillis

        val pStartCal = java.util.Calendar.getInstance().apply {
            timeInMillis = phaseStartMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val pEndCal = java.util.Calendar.getInstance().apply {
            timeInMillis = phaseEndMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val elapsedActiveDays: Int
        val isActive: Boolean
        val isPast: Boolean
        val isFuture: Boolean

        if (todayCal.before(pStartCal)) {
            elapsedActiveDays = 0
            isActive = false
            isPast = false
            isFuture = true
        } else if (todayCal.after(pEndCal)) {
            elapsedActiveDays = duration
            isActive = false
            isPast = true
            isFuture = false
        } else {
            val rawDays = ((todayCal.timeInMillis - pStartCal.timeInMillis) / (24L * 3600L * 1000L)).toInt() + 1
            elapsedActiveDays = (rawDays - missed).coerceIn(0, duration)
            isActive = true
            isPast = false
            isFuture = false
        }

        val phaseConsumedMg = (elapsedActiveDays * dailyDoseMg).coerceIn(0, med.totalDispensedMg)
        val phaseRemainingMg = (med.totalDispensedMg - phaseConsumedMg).coerceAtLeast(0)
        val phaseConsumedCaps = if (strength > 0) (phaseConsumedMg / strength).coerceIn(0, med.totalCapsules) else 0
        val phaseRemainingCaps = (med.totalCapsules - phaseConsumedCaps).coerceAtLeast(0)

        DispensePhaseDetail(
            dispenseId = med.id,
            phaseNumber = idx + 1,
            dispenseName = med.medicationName,
            startDate = phaseStartMillis,
            calculatedEndDate = phaseEndMillis,
            durationDays = duration,
            missedDays = missed,
            dailyDoseMg = dailyDoseMg,
            dailyCapsulesCount = dailyCaps,
            capsuleStrengthMg = strength,
            boxCount = med.boxCount,
            totalCapsules = med.totalCapsules,
            totalDispensedMg = med.totalDispensedMg,
            elapsedActiveDays = elapsedActiveDays,
            phaseConsumedMg = phaseConsumedMg,
            phaseRemainingMg = phaseRemainingMg,
            phaseConsumedCapsules = phaseConsumedCaps,
            phaseRemainingCapsules = phaseRemainingCaps,
            isActiveNow = isActive,
            isPast = isPast,
            isFuture = isFuture
        )
    }

    val scheduledCumulativeConsumedMg = phases.sumOf { it.phaseConsumedMg }.coerceIn(0, grandTotalDispensedMg)
    val scheduledRemainingMg = (grandTotalDispensedMg - scheduledCumulativeConsumedMg).coerceAtLeast(0)

    val confirmedTakenLogs = allLogs.filter { it.isTaken && it.doseMg > 0 }
    val hasDailyLogs = confirmedTakenLogs.isNotEmpty()
    val confirmedDaysCount = confirmedTakenLogs.size
    val confirmedConsumedMg = if (hasDailyLogs) {
        confirmedTakenLogs.sumOf { it.doseMg }.coerceIn(0, grandTotalDispensedMg)
    } else {
        scheduledCumulativeConsumedMg
    }
    val confirmedRemainingMg = (grandTotalDispensedMg - confirmedConsumedMg).coerceAtLeast(0)
    val reconciliationVarianceMg = scheduledCumulativeConsumedMg - confirmedConsumedMg

    val actualCumulativeConsumedMg = if (hasDailyLogs) confirmedConsumedMg else scheduledCumulativeConsumedMg
    val trueRemainingMg = if (hasDailyLogs) confirmedRemainingMg else scheduledRemainingMg
    val calculationSource = if (hasDailyLogs) "DAILY_LOGS" else "SCHEDULED"

    val consumptionPercent = if (grandTotalDispensedMg > 0) {
        ((actualCumulativeConsumedMg.toDouble() / grandTotalDispensedMg) * 100).toInt().coerceIn(0, 100)
    } else 0

    val allBoxes = mutableListOf<BoxInventoryItem>()
    var globalBoxIndex = 1

    sortedMeds.forEach { med ->
        val strength = if (med.capsuleStrengthMg > 0) med.capsuleStrengthMg else 20
        val capsPerBox = if (med.capsulesPerBox > 0) med.capsulesPerBox else 30
        val boxCapacityMg = capsPerBox * strength

        for (b in 1..med.boxCount) {
            allBoxes.add(
                BoxInventoryItem(
                    boxId = "${med.id}_box_$b",
                    globalBoxIndex = globalBoxIndex++,
                    boxIndexInDispense = b,
                    dispenseId = med.id,
                    dispenseNumber = med.dispenseNumber,
                    dispenseName = med.medicationName,
                    dispenseDate = med.startDate,
                    capsuleStrengthMg = strength,
                    totalCapsulesInBox = capsPerBox,
                    totalMgInBox = boxCapacityMg,
                    consumedCapsules = 0,
                    remainingCapsules = capsPerBox,
                    consumedMg = 0,
                    remainingMg = boxCapacityMg,
                    status = BoxConsumptionStatus.SEALED_IN_STOCK
                )
            )
        }
    }

    var remainingToConsumeMg = actualCumulativeConsumedMg
    val simulatedBoxes = allBoxes.map { box ->
        val capacity = box.totalMgInBox
        val strength = box.capsuleStrengthMg
        if (remainingToConsumeMg >= capacity) {
            remainingToConsumeMg -= capacity
            box.copy(
                consumedCapsules = box.totalCapsulesInBox,
                remainingCapsules = 0,
                consumedMg = capacity,
                remainingMg = 0,
                status = BoxConsumptionStatus.FULLY_CONSUMED
            )
        } else if (remainingToConsumeMg > 0) {
            val consumedThisBoxMg = remainingToConsumeMg
            val consumedCaps = (consumedThisBoxMg / strength).coerceIn(0, box.totalCapsulesInBox)
            val remainingCaps = box.totalCapsulesInBox - consumedCaps
            val remainingMgThisBox = remainingCaps * strength
            remainingToConsumeMg = 0
            box.copy(
                consumedCapsules = consumedCaps,
                remainingCapsules = remainingCaps,
                consumedMg = consumedThisBoxMg,
                remainingMg = remainingMgThisBox,
                status = BoxConsumptionStatus.CURRENTLY_OPEN
            )
        } else {
            box.copy(
                consumedCapsules = 0,
                remainingCapsules = box.totalCapsulesInBox,
                consumedMg = 0,
                remainingMg = capacity,
                status = BoxConsumptionStatus.SEALED_IN_STOCK
            )
        }
    }

    val actualCumulativeConsumedCapsules = simulatedBoxes.sumOf { it.consumedCapsules }
    val trueRemainingCapsules = simulatedBoxes.sumOf { it.remainingCapsules }
    val confirmedConsumedCapsules = actualCumulativeConsumedCapsules
    val confirmedRemainingCapsules = trueRemainingCapsules

    fun makeStrengthSummary(strength: Int): StrengthInventorySummary {
        val boxesForStr = simulatedBoxes.filter { it.capsuleStrengthMg == strength }
        val fullSealed = boxesForStr.count { it.status == BoxConsumptionStatus.SEALED_IN_STOCK }
        val fullyConsumed = boxesForStr.count { it.status == BoxConsumptionStatus.FULLY_CONSUMED }
        val openBox = boxesForStr.firstOrNull { it.status == BoxConsumptionStatus.CURRENTLY_OPEN }

        return StrengthInventorySummary(
            strengthMg = strength,
            totalBoxesDispensed = boxesForStr.size,
            totalCapsulesDispensed = boxesForStr.sumOf { it.totalCapsulesInBox },
            totalMgDispensed = boxesForStr.sumOf { it.totalMgInBox },
            fullSealedBoxesRemaining = fullSealed,
            openBoxCapsulesRemaining = openBox?.remainingCapsules ?: 0,
            openBoxCapsulesConsumed = openBox?.consumedCapsules ?: 0,
            hasOpenBox = openBox != null,
            totalCapsulesRemaining = boxesForStr.sumOf { it.remainingCapsules },
            totalMgRemaining = boxesForStr.sumOf { it.remainingMg },
            totalCapsulesConsumed = boxesForStr.sumOf { it.consumedCapsules },
            totalMgConsumed = boxesForStr.sumOf { it.consumedMg },
            fullyConsumedBoxesCount = fullyConsumed
        )
    }

    val summary20 = makeStrengthSummary(20)
    val summary10 = makeStrengthSummary(10)
    val otherStrengthsList = simulatedBoxes
        .map { it.capsuleStrengthMg }
        .distinct()
        .filter { it != 20 && it != 10 }
        .map { makeStrengthSummary(it) }

    val currentOpenBoxes = simulatedBoxes.filter { it.status == BoxConsumptionStatus.CURRENTLY_OPEN }
    val fullSealedTotal = simulatedBoxes.count { it.status == BoxConsumptionStatus.SEALED_IN_STOCK }
    val fullyConsumedTotal = simulatedBoxes.count { it.status == BoxConsumptionStatus.FULLY_CONSUMED }

    val latestMed = sortedMeds.lastOrNull()
    val activeDailyCapsRate = when {
        latestMed != null && latestMed.dailyCapsulesCount > 0 -> latestMed.dailyCapsulesCount
        latestMed != null && latestMed.durationDays > 0 && latestMed.totalCapsules > 0 ->
            (latestMed.totalCapsules / latestMed.durationDays).coerceAtLeast(1)
        phases.any { it.isActiveNow && it.dailyCapsulesCount > 0 } ->
            phases.first { it.isActiveNow && it.dailyCapsulesCount > 0 }.dailyCapsulesCount
        phases.isNotEmpty() && phases.last().dailyCapsulesCount > 0 ->
            phases.last().dailyCapsulesCount
        else -> 4
    }.coerceAtLeast(1)

    // الأيام المتبقية = إجمالي الكبسولات المتبقية الفعلية ÷ معدل الاستهلاك اليومي (مثال: 256 ÷ 4 = 64 يوماً)
    val remainingDaysCount = if (activeDailyCapsRate > 0) {
        (trueRemainingCapsules / activeDailyCapsRate).coerceAtLeast(0)
    } else 0

    // تاريخ النفاذ المتوقع = تاريخ اليوم الحالي + الأيام المتبقية (مثال: 26 أكتوبر 2026 + 64 يوماً = 29 ديسمبر 2026)
    val runOutCal = java.util.Calendar.getInstance().apply {
        timeInMillis = todayCal.timeInMillis
        add(java.util.Calendar.DAY_OF_YEAR, remainingDaysCount)
    }
    val estimatedRunOutDateMillis = if (remainingDaysCount > 0) runOutCal.timeInMillis else todayCal.timeInMillis

    val phasesWithActuals = phases.map { phase ->
        val boxesForMed = simulatedBoxes.filter { it.dispenseId == phase.dispenseId }
        if (boxesForMed.isNotEmpty() && hasDailyLogs) {
            val actualConsumedCaps = boxesForMed.sumOf { it.consumedCapsules }
            val actualRemainingCaps = boxesForMed.sumOf { it.remainingCapsules }
            val actualConsumedMg = boxesForMed.sumOf { it.consumedMg }
            val actualRemainingMg = boxesForMed.sumOf { it.remainingMg }
            phase.copy(
                phaseConsumedCapsules = actualConsumedCaps,
                phaseRemainingCapsules = actualRemainingCaps,
                phaseConsumedMg = actualConsumedMg,
                phaseRemainingMg = actualRemainingMg
            )
        } else {
            phase
        }
    }

    return RoaccutaneInventoryAudit(
        courseStartDate = effectiveStart,
        elapsedDaysFromStart = elapsedDaysFromStart,
        grandTotalDispensedMg = grandTotalDispensedMg,
        grandTotalDispensedCapsules = grandTotalDispensedCapsules,
        grandTotalBoxes = grandTotalBoxes,
        actualCumulativeConsumedMg = actualCumulativeConsumedMg,
        actualCumulativeConsumedCapsules = actualCumulativeConsumedCapsules,
        trueRemainingMg = trueRemainingMg,
        trueRemainingCapsules = trueRemainingCapsules,
        consumptionPercent = consumptionPercent,
        strength20Mg = summary20,
        strength10Mg = summary10,
        otherStrengths = otherStrengthsList,
        phases = phasesWithActuals,
        boxes = simulatedBoxes,
        currentOpenBoxes = currentOpenBoxes,
        fullSealedBoxesCount = fullSealedTotal,
        fullyConsumedBoxesCount = fullyConsumedTotal,
        confirmedDaysCount = confirmedDaysCount,
        confirmedConsumedMg = confirmedConsumedMg,
        confirmedConsumedCapsules = confirmedConsumedCapsules,
        confirmedRemainingMg = confirmedRemainingMg,
        confirmedRemainingCapsules = confirmedRemainingCapsules,
        scheduledCumulativeConsumedMg = scheduledCumulativeConsumedMg,
        scheduledRemainingMg = scheduledRemainingMg,
        reconciliationVarianceMg = reconciliationVarianceMg,
        hasDailyLogs = hasDailyLogs,
        calculationSource = calculationSource,
        dailyCapsulesRate = activeDailyCapsRate,
        remainingDaysCount = remainingDaysCount,
        estimatedRunOutDateMillis = estimatedRunOutDateMillis
    )
}

// --- Linked Account & Google Verification Models ---
data class LinkedAccount(
    val email: String,
    val providerType: String = "GOOGLE", // "GOOGLE" or "EMAIL"
    val linkedTimestamp: Long = System.currentTimeMillis(),
    val isPrimary: Boolean = false,
    val displayName: String = ""
) {
    val isGoogle: Boolean
        get() = providerType.equals("GOOGLE", ignoreCase = true) || isGoogleEmail(email)
}

fun isGoogleEmail(email: String): Boolean {
    val lower = email.trim().lowercase()
    return lower.endsWith("@gmail.com") || lower.endsWith("@googlemail.com")
}



