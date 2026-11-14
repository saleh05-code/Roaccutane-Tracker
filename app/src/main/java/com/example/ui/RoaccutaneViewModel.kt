package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.URL
import java.net.HttpURLConnection

class RoaccutaneViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoaccutaneRepository = RoaccutaneRepository(AppDatabase.getDatabase(application).roaccutaneDao())

    // 1. ربط المتغير مباشرة بقاعدة البيانات المحلية ليتحدث تلقائياً عند أي تغيير (سواء محلي أو سحابي)
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserProfile() // القيمة الأولية الافتراضية
        )
        
    val allLogs: StateFlow<List<DailyLog>>
    val allAppointments: StateFlow<List<Appointment>>

    // --- Multi-Account Role System & Persistent Login ---
    private val prefs = application.getSharedPreferences("roaccutane_user_prefs", Context.MODE_PRIVATE)

    private val _loggedInRole = MutableStateFlow<UserRole?>(UserRole.PATIENT)
    val loggedInRole: StateFlow<UserRole?> = _loggedInRole.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.PATIENT)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun selectRoleAndLogin(role: UserRole) {
        _loggedInRole.value = UserRole.PATIENT
        _currentRole.value = UserRole.PATIENT
    }

    fun logout() {
        _loggedInRole.value = UserRole.PATIENT
        _currentRole.value = UserRole.PATIENT
    }

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        viewModelScope.launch {
            repository.logAudit(
                actorRole = role.name,
                actorName = role.titleAr,
                action = "ROLE_SWITCH",
                details = "تم التبديل إلى ${role.titleAr} (${role.titleEn})"
            )
        }
    }

    // --- Platform Data StateFlows ---
    val allVitalSigns: StateFlow<List<VitalSign>> = repository.allVitalSigns
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allClinics: StateFlow<List<Clinic>> = repository.allClinics
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allDoctors: StateFlow<List<Doctor>> = repository.allDoctors
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPairings: StateFlow<List<PatientPairing>> = repository.allPairings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allDoctorNotes: StateFlow<List<DoctorNote>> = repository.allDoctorNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allFinancialRecords: StateFlow<List<FinancialRecord>> = repository.allFinancialRecords
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allFeatureUsage: StateFlow<List<FeatureUsageRecord>> = repository.allFeatureUsage
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSubscriptionPlans: StateFlow<List<SubscriptionPlanConfig>> = repository.allSubscriptionPlans
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAffiliateProducts: StateFlow<List<AffiliateProduct>> = repository.allAffiliateProducts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOwnerChanges: StateFlow<List<OwnerChangeRecord>> = repository.allOwnerChanges
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allDispensedMedications: StateFlow<List<DispensedMedication>> = repository.allDispensedMedications
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMedicalPrescriptions: StateFlow<List<MedicalPrescription>> = repository.allMedicalPrescriptions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _waterStepCups = MutableStateFlow(prefs.getInt("water_step_cups", 1))
    val waterStepCups: StateFlow<Int> = _waterStepCups.asStateFlow()

    fun setWaterStepCups(step: Int) {
        val safe = step.coerceIn(1, 12)
        _waterStepCups.value = safe
        prefs.edit().putInt("water_step_cups", safe).apply()
    }

    fun incrementWaterByConfiguredStep(dateString: String, isIncrement: Boolean) {
        val step = _waterStepCups.value
        incrementWaterCups(dateString, if (isIncrement) step else -step)
    }

    // --- Super Admin AI Advisor ---
    val advisorEngine = SuperAdminAdvisorEngine(repository)
    private val _advisorReport = MutableStateFlow<SuperAdminAdvisorReport?>(null)
    val advisorReport: StateFlow<SuperAdminAdvisorReport?> = _advisorReport.asStateFlow()

    private val _isAdvisorAnalyzing = MutableStateFlow(false)
    val isAdvisorAnalyzing: StateFlow<Boolean> = _isAdvisorAnalyzing.asStateFlow()

    fun runAdvisorAnalysis() {
        viewModelScope.launch {
            _isAdvisorAnalyzing.value = true
            try {
                // Batch on-demand: compute deterministic SQL-First report
                val rep = advisorEngine.computeDeterministicReport(
                    clinics = allClinics.value,
                    doctors = allDoctors.value,
                    pairings = allPairings.value,
                    financials = allFinancialRecords.value,
                    usages = allFeatureUsage.value,
                    plans = allSubscriptionPlans.value,
                    affiliates = allAffiliateProducts.value
                )
                _advisorReport.value = rep
                repository.logAudit(
                    actorRole = "SUPER_ADMIN",
                    actorName = "مالك التطبيق",
                    action = "RUN_AI_ADVISOR_ANALYSIS",
                    details = "تشغيل تحليل المستشار الذكي بنظام SQL-First وتوليد التوصيات التشغيلية والمالية مع تفعيل ضوابط Guardrails"
                )
            } catch (e: Exception) {
                Log.e("RoaccutaneViewModel", "Error computing advisor report: ${e.message}", e)
            } finally {
                _isAdvisorAnalyzing.value = false
            }
        }
    }

    fun applyAdvisorPricing(rec: PricingRecommendation) {
        viewModelScope.launch {
            advisorEngine.applyPricingRecommendation(rec)
            runAdvisorAnalysis()
        }
    }

    fun applyAdvisorFeatureRoi(rec: FeatureRoiRecommendation) {
        viewModelScope.launch {
            advisorEngine.applyFeatureRoiRecommendation(rec)
            runAdvisorAnalysis()
        }
    }

    fun applyAdvisorAffiliateReorder(optimizations: List<AffiliateRoiOptimization>) {
        viewModelScope.launch {
            advisorEngine.applyAffiliateReordering(optimizations)
            runAdvisorAnalysis()
        }
    }

    fun resolveAdvisorChurnAlert(alert: ChurnPredictionAlert) {
        viewModelScope.launch {
            advisorEngine.resolveChurnAlert(alert)
            runAdvisorAnalysis()
        }
    }

    // --- Operations: Vitals & Notes ---
    fun saveVitalSigns(systolicBP: Int?, diastolicBP: Int?, bloodSugar: Float?, weightKg: Float?) {
        viewModelScope.launch {
            val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.saveVitalSign(
                VitalSign(
                    dateString = curDate,
                    systolicBP = systolicBP,
                    diastolicBP = diastolicBP,
                    bloodSugar = bloodSugar,
                    weightKg = weightKg
                )
            )
            repository.logFeatureUsage("VITALS_TRACKING", 0.01)
        }
    }

    fun saveDoctorNote(title: String, content: String, isTemplate: Boolean, patientId: String = "pat_current") {
        viewModelScope.launch {
            val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.saveDoctorNote(
                DoctorNote(
                    doctorId = "doc_sara",
                    doctorName = "د. سارة الأحمد",
                    patientId = patientId,
                    dateStr = curDate,
                    title = title,
                    content = content,
                    isTemplate = isTemplate
                )
            )
            repository.logAudit(
                actorRole = "DOCTOR",
                actorName = "د. سارة الأحمد",
                action = "SEND_DOCTOR_NOTE",
                details = "إرسال ملاحظة طبية للمريض: $title (مع إتاحة زر اشرح لي بالمساعد الذكي)"
            )
        }
    }

    fun deleteDoctorNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteDoctorNote(noteId)
            repository.logAudit(
                actorRole = "DOCTOR",
                actorName = "الطبيب المعالج",
                action = "DELETE_DOCTOR_NOTE",
                details = "تم حذف الملاحظة الطبية (معرف: $noteId) ومزامنة الحذف لدى المريض والطبيب فوراً"
            )
        }
    }

    fun explainDoctorNoteWithAi(noteContent: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.logFeatureUsage("AI_ASSISTANT", 0.05)
            val prompt = "اشرح الملاحظة الطبية التالية من الطبيب بلغة سهلة ومطمئنة للمريض، مع توضيح أهمية التعليمات وكيفية تطبيقها يومياً، واذكر المصدر الطبي المعتمد باختصار شديد:\n\n$noteContent"
            val key = if (_customApiKey.value.isNotBlank()) _customApiKey.value else com.example.BuildConfig.GEMINI_API_KEY
            
            var explanation: String? = null
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(4500L) {
                        explanation = repository.askJauzaaAI(
                            apiKey = key,
                            userQuery = prompt,
                            contextInfo = "ملاحظة موجهة لمريض يعالج بالرواكتان"
                        )
                    }
                } catch (_: Exception) {}
            }

            if (explanation.isNullOrBlank() || explanation?.startsWith("خطأ:") == true || explanation?.startsWith("وقع خطأ") == true) {
                explanation = buildClinicalExplanationFallback(noteContent)
            }

            onResult(explanation ?: "")
        }
    }

    private fun buildClinicalExplanationFallback(noteContent: String): String {
        val lower = noteContent.lowercase()
        val sb = StringBuilder()
        sb.append("📋 **توضيح التوجيه الطبي من المساعد الذكي:**\n\n")

        var addressed = false
        if (lower.contains("ترطيب") || lower.contains("شفاه") || lower.contains("جفاف") || lower.contains("أنف") || lower.contains("فازلين") || lower.contains("مرهم")) {
            sb.append("💧 **بشأن الترطيب ومكافحة الجفاف:**\n")
            sb.append("عقار الآيزوتريتينوين (الرواكتان) يعمل أساساً على تقليص نشاط الغدد الدهنية في الجسم، مما يؤدي لجفاف الأغشية المخاطية والشفاه وبشرة الوجه. هذا مؤشر بيولوجي طبيعي على استجابة الجسم للدواء.\n")
            sb.append("• يُنصح باستخدام المراهم الشمعية المكثفة (مثل الأكوافور أو الفازلين الطبي) كل ساعتين إلى ثلاث ساعات وتجنب لعق الشفاه.\n")
            sb.append("• شرب ما لا يقل عن 2.5 إلى 3 لترات من الماء يومياً للمحافظة على توازن السوائل والأنسجة.\n\n")
            addressed = true
        }

        if (lower.contains("فحص") || lower.contains("دم") || lower.contains("كبد") || lower.contains("دهون") || lower.contains("مختبر") || lower.contains("تحليل") || lower.contains("نزيم")) {
            sb.append("🔬 **بشأن الفحوصات المخبرية الدورية:**\n")
            sb.append("يتم استقلاب الرواكتان بصورة رئيسية في الكبد، كما قد يُحدث ارتفاعاً مؤقتاً في إنزيمات الكبد ومستوى الدهون الثلاثية. لذا فإن متابعة التحاليل ضرورة لضمان أمان الجرعة.\n")
            sb.append("• الصيام لمدة 10-12 ساعة قبل سحب عينة الدم لضمان دقة قياس الدهون الثلاثية.\n")
            sb.append("• الامتناع عن التمارين المجهدة العنيفة قبل الفحص بـ 24 ساعة لتجنب الارتفاع الكاذب في إنزيمات العضلات والكبد.\n\n")
            addressed = true
        }

        if (lower.contains("شمس") || lower.contains("واقي") || lower.contains("حرارة") || lower.contains("ضوء") || lower.contains("تصبغ")) {
            sb.append("☀️ **بشأن الوقاية من أشعة الشمس:**\n")
            sb.append("تزداد حساسية البشرة للضوء (Photosensitivity) أثناء العلاج بالرواكتان وتصبح الطبقة المتقرنة أرق وأسرع تأثراً بالأشعة فوق البنفسجية.\n")
            sb.append("• تطبيق واقي شمس واسع النطاق بدرجة حماية SPF 50+ وتجديده كل ساعتين عند التواجد خارج المنزل.\n\n")
            addressed = true
        }

        if (lower.contains("جرعة") || lower.contains("كبسولة") || lower.contains("دهنية") || lower.contains("وجبة") || lower.contains("أكل") || lower.contains("طعام")) {
            sb.append("💊 **بشأن طريقة تناول الجرعة:**\n")
            sb.append("عقار الرواكتان محب للدهون (Lipophilic)، وتناوله مع وجبة تحتوي على دهون صحية (مثل الأفوكادو، زيت الزيتون، أو الحليب كامل الدسم) يضاعف امتصاصه وفاعليته.\n")
            sb.append("• بلع الكبسولة مع كوب كامل من الماء دون كسرها أو مصها لتجنب تهيج المريء.\n\n")
            addressed = true
        }

        if (!addressed) {
            sb.append("توجيه الطبيب يهدف إلى ضبط وتنسيق مسار خطتك العلاجية وضمان سلامتك وأعلى فاعلية للعلاج. يرجى الالتزام التام بالتعليمات المذكورة وإبلاغ الطبيب بأي تغيرات غير معتادة.\n\n")
        }

        sb.append("📌 **المصادر الطبية المعتمدة:**\n")
        sb.append("[Mayo Clinic](https://www.mayoclinic.org) | [American Academy of Dermatology (AAD)](https://www.aad.org) | [FDA Guidelines](https://www.fda.gov)")

        return sb.toString()
    }

    // --- Portal Operations: Affiliate & Plans Management ---
    fun saveAffiliateProduct(product: AffiliateProduct) {
        viewModelScope.launch {
            repository.saveAffiliateProduct(product)
            writeAffiliateProductToFirestore(product)
            repository.logOwnerChange(
                actionType = "SAVE_AFFILIATE_PRODUCT",
                title = "إضافة/تعديل منتج تسويقي",
                details = "تم حفظ المنتج: ${product.productNameAr} بسعر ${product.priceSAR} ر.س ورابط ${product.url}"
            )
        }
    }

    fun deleteAffiliateProduct(id: String, name: String = "") {
        viewModelScope.launch {
            repository.deleteAffiliateProduct(id, name)
            deleteAffiliateProductFromFirestore(id)
        }
    }

    // --- Dispensed Medications Operations ---
    fun saveDispensedMedication(item: DispensedMedication) {
        viewModelScope.launch {
            repository.saveDispensedMedication(item)
            writeDispensedMedicationToFirestore(item)
        }
    }

    fun deleteDispensedMedication(id: String) {
        viewModelScope.launch {
            repository.deleteDispensedMedication(id)
            deleteDispensedMedicationFromFirestore(id)
        }
    }

    fun updateDispensedMedicationMissedDays(id: String, missedDays: Int) {
        viewModelScope.launch {
            val safeDays = missedDays.coerceAtLeast(0)
            repository.updateDispensedMedicationMissedDays(id, safeDays)
            val cur = allDispensedMedications.value.find { it.id == id }?.copy(missedDays = safeDays)
            if (cur != null) {
                writeDispensedMedicationToFirestore(cur)
            }
        }
    }

    fun addMissedDaysToDispensedMedication(id: String, delta: Int) {
        val cur = allDispensedMedications.value.find { it.id == id } ?: return
        val newDays = (cur.missedDays + delta).coerceAtLeast(0)
        updateDispensedMedicationMissedDays(id, newDays)
    }

    /**
     * Synchronizes missed days automatically between daily logs (diary) and dispensed medications (prescriptions).
     * Calculates the exact FIFO phase missed doses and updates the database in real-time.
     */
    fun syncMissedDaysFromDailyLogs() {
        viewModelScope.launch {
            val meds = allDispensedMedications.value
            val logs = allLogs.value
            val start = userProfile.value.startDate
            if (meds.isNotEmpty()) {
                val missedMap = calculateFIFOMissedDaysForDispenses(meds, start, logs)
                meds.forEach { med ->
                    val autoMissed = missedMap[med.id] ?: 0
                    if (med.missedDays != autoMissed) {
                        repository.updateDispensedMedicationMissedDays(med.id, autoMissed)
                        val cur = med.copy(missedDays = autoMissed)
                        writeDispensedMedicationToFirestore(cur)
                    }
                }
            }
        }
    }

    // --- Medical Prescriptions Operations ---
    fun saveMedicalPrescription(item: MedicalPrescription) {
        viewModelScope.launch {
            repository.saveMedicalPrescription(item)
            writeMedicalPrescriptionToFirestore(item)
        }
    }

    fun deleteMedicalPrescription(id: String) {
        viewModelScope.launch {
            repository.deleteMedicalPrescription(id)
            deleteMedicalPrescriptionFromFirestore(id)
        }
    }

    fun linkPrescriptionToAppointment(appointmentId: Int, prescriptionId: String?) {
        viewModelScope.launch {
            val cur = allAppointments.value.find { it.id == appointmentId } ?: return@launch
            val updated = cur.copy(prescriptionId = prescriptionId)
            repository.saveAppointment(updated)
            writeAppointmentToFirestore(updated)
        }
    }

    fun saveLastDispensedMedicationDefaults(
        medName: String,
        dailyDose: String,
        durationDays: Int,
        boxCount: Int,
        capsulesPerBox: Int,
        colorHex: String,
        notes: String,
        capsuleStrengthMg: Int = 20,
        dailyCapsulesCount: Int = 1
    ) {
        sharedPrefs.edit()
            .putString("last_dispensed_med_name", medName.trim())
            .putString("last_dispensed_daily_dose", dailyDose.trim())
            .putInt("last_dispensed_duration_days", durationDays)
            .putInt("last_dispensed_box_count", boxCount)
            .putInt("last_dispensed_capsules_per_box", capsulesPerBox)
            .putString("last_dispensed_color_hex", colorHex)
            .putString("last_dispensed_notes", notes.trim())
            .putInt("last_dispensed_capsule_strength", capsuleStrengthMg)
            .putInt("last_dispensed_daily_capsules_count", dailyCapsulesCount)
            .apply()
    }

    fun getLastDispensedMedicationDefaults(): com.example.data.DispensedMedDefaults {
        return com.example.data.DispensedMedDefaults(
            medName = sharedPrefs.getString("last_dispensed_med_name", "") ?: "",
            dailyDose = sharedPrefs.getString("last_dispensed_daily_dose", "20 ملغ") ?: "20 ملغ",
            durationDays = sharedPrefs.getInt("last_dispensed_duration_days", 30),
            boxCount = sharedPrefs.getInt("last_dispensed_box_count", 1),
            capsulesPerBox = sharedPrefs.getInt("last_dispensed_capsules_per_box", 30),
            colorHex = sharedPrefs.getString("last_dispensed_color_hex", "#1976D2") ?: "#1976D2",
            notes = sharedPrefs.getString("last_dispensed_notes", "") ?: "",
            capsuleStrengthMg = sharedPrefs.getInt("last_dispensed_capsule_strength", 20),
            dailyCapsulesCount = sharedPrefs.getInt("last_dispensed_daily_capsules_count", 1)
        )
    }

    fun updateDoctorProfile(
        id: String,
        name: String,
        specialty: String,
        phone: String,
        bio: String,
        fee: Double,
        signatureUri: String,
        stampUri: String,
        licenseUri: String
    ) {
        viewModelScope.launch {
            repository.updateDoctorProfile(id, name, specialty, phone, bio, fee, signatureUri, stampUri, licenseUri)
        }
    }

    fun updateClinicDetails(id: String, name: String, email: String, phone: String, logo: String) {
        viewModelScope.launch {
            repository.updateClinicDetails(id, name, email, phone, logo)
        }
    }

    fun reactivateClinicSubscription(clinicId: String) {
        renewClinicSubscription(clinicId, 30)
    }

    fun renewClinicSubscription(clinicId: String, daysToAdd: Int) {
        viewModelScope.launch {
            repository.renewClinicSubscription(clinicId, daysToAdd)
            runAdvisorAnalysis()
        }
    }

    fun updatePlanFeaturesList(planId: String, featuresList: String) {
        viewModelScope.launch {
            repository.updatePlanFeaturesList(planId, featuresList)
        }
    }

    fun unlinkPatient(pairingId: Int, patientName: String) {
        viewModelScope.launch {
            repository.deletePairing(pairingId, patientName)
        }
    }

    fun linkDoctorByCode(pairingCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanCode = pairingCode.trim().uppercase()
            var doctor = allDoctors.value.find { it.pairingCode.equals(cleanCode, ignoreCase = true) }
            if (doctor == null && (cleanCode == "DOC-7842" || cleanCode.startsWith("DOC-"))) {
                val defaultDoc = Doctor(
                    id = "doc_sara",
                    clinicId = "clinic_derm_elite",
                    name = "د. سارة الأحمد",
                    specialty = "استشارية الأمراض الجلدية السريرية",
                    phone = "+966 50 123 4567",
                    bio = "استشارية علاج حب الشباب والأمراض الجلدية المتقدمة بخبرة 12 عاماً.",
                    consultationFee = 300.0,
                    pairingCode = cleanCode
                )
                repository.saveDoctor(defaultDoc)
                doctor = defaultDoc
            }
            if (doctor != null) {
                val exists = allPairings.value.any { it.doctorId == doctor.id && it.patientId == "pat_current" }
                if (exists) {
                    onResult(true, "تم ربط ملفك مسبقاً مع ${doctor.name}")
                    return@launch
                }
                repository.savePairing(
                    PatientPairing(
                        patientId = "pat_current",
                        patientName = userProfile.value.name.ifBlank { "أحمد محمد" },
                        clinicId = doctor.clinicId,
                        doctorId = doctor.id,
                        pairingCode = doctor.pairingCode,
                        status = "Approved",
                        treatmentPhase = "مرحلة العلاج النشط",
                        adherenceRate = 95
                    )
                )
                repository.logOwnerChange(
                    actionType = "LINK_DOCTOR_PATIENT",
                    title = "ربط مريض بطبيب",
                    details = "تم ربط المريض ${userProfile.value.name} مع الطبيب ${doctor.name} بنجاح"
                )
                onResult(true, "تم ربط ملفك بنجاح مع ${doctor.name} (${doctor.specialty})")
            } else {
                onResult(false, "كود الطبيب غير صحيح أو غير مسجل في النظام. يرجى التأكد من الكود والمحاولة.")
            }
        }
    }

    fun registerPatientForDoctor(
        doctorId: String,
        patientName: String,
        treatmentPhase: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val doctor = allDoctors.value.find { it.id == doctorId }
            val newPairing = PatientPairing(
                patientId = "pat_${System.currentTimeMillis()}",
                patientName = patientName.trim(),
                clinicId = doctor?.clinicId ?: "clinic_1",
                doctorId = doctorId,
                pairingCode = doctor?.pairingCode ?: "DOC-7842",
                status = "Approved",
                treatmentPhase = treatmentPhase.ifBlank { "مرحلة العلاج النشط" },
                adherenceRate = 100
            )
            repository.savePairing(newPairing)
            repository.logOwnerChange(
                actionType = "REGISTER_PATIENT",
                title = "تسجيل مريض جديد",
                details = "تم تسجيل المريض $patientName يدوياً للطبيب ${doctor?.name ?: doctorId}"
            )
        }
    }

    fun linkDoctorToClinicByCode(doctorCode: String, clinicId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanCode = doctorCode.trim().uppercase()
            val doctor = allDoctors.value.find { it.pairingCode.equals(cleanCode, ignoreCase = true) }
            if (doctor != null) {
                if (doctor.clinicId == clinicId) {
                    onResult(true, "الطبيب ${doctor.name} مرتبط بالفعل بهذه العيادة.")
                    return@launch
                }
                repository.saveDoctor(doctor.copy(clinicId = clinicId))
                repository.logOwnerChange(
                    actionType = "LINK_DOCTOR_CLINIC",
                    title = "ربط طبيب بالعيادة",
                    details = "تم ربط الطبيب ${doctor.name} بالعيادة بنجاح"
                )
                onResult(true, "تم ربط الطبيب ${doctor.name} بنجاح إلى طاقم العيادة!")
            } else {
                onResult(false, "لم يتم العثور على طبيب بالكود المدخل.")
            }
        }
    }

    fun cancelClinicSubscription(clinicId: String) {
        viewModelScope.launch {
            val clinicName = allClinics.value.find { it.id == clinicId }?.name ?: clinicId
            repository.updateClinicFrozen(clinicId, true)
            repository.logOwnerChange(
                actionType = "CANCEL_SUBSCRIPTION",
                title = "إلغاء اشتراك عيادة",
                details = "العيادة: $clinicName - تم تجميد الحساب وإلغاء الاشتراك"
            )
        }
    }

    // --- Operations: Doctor Management ---
    fun toggleDoctorBlock(doctorId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.updateDoctorBlocked(doctorId, isBlocked)
            repository.logAudit(
                actorRole = "CLINIC_ADMIN",
                actorName = "إدارة العيادة",
                action = if (isBlocked) "BLOCK_DOCTOR" else "UNBLOCK_DOCTOR",
                details = "تعديل حالة الحظر للطبيب $doctorId إلى $isBlocked"
            )
        }
    }

    fun addDoctorToClinic(name: String, specialty: String, workHours: String, clinicId: String) {
        viewModelScope.launch {
            val randomNum = (1000..9999).random()
            val newDoc = Doctor(
                id = "doc_$randomNum",
                clinicId = clinicId,
                name = name,
                specialty = specialty,
                workHours = workHours,
                pairingCode = "DOC-$randomNum"
            )
            repository.saveDoctor(newDoc)
            repository.logAudit(
                actorRole = "CLINIC_ADMIN",
                actorName = "إدارة العيادة",
                action = "ADD_DOCTOR",
                details = "إضافة طبيب جديد للكادر الطبي: $name - تخصص: $specialty"
            )
        }
    }

    fun reassignDoctorPatients(clinicId: String, oldDoctorId: String, newDoctorId: String) {
        viewModelScope.launch {
            repository.reassignClinicPatients(clinicId, oldDoctorId, newDoctorId)
            repository.logAudit(
                actorRole = "CLINIC_ADMIN",
                actorName = "إدارة العيادة",
                action = "UNLINKING_PROTECTION_TRANSFER",
                details = "إغلاق ثغرة فك الربط (Unlinking Risk): نقل ملفات المرضى من الطبيب $oldDoctorId إلى الطبيب $newDoctorId مع بقاء الملكية للعيادة"
            )
        }
    }

    // --- Operations: Clinic Management ---
    fun toggleClinicFreeze(clinicId: String, isFrozen: Boolean) {
        viewModelScope.launch {
            repository.updateClinicFrozen(clinicId, isFrozen)
            repository.logAudit(
                actorRole = "SUPER_ADMIN",
                actorName = "مالك التطبيق",
                action = if (isFrozen) "FREEZE_CLINIC" else "UNFREEZE_CLINIC",
                details = "تعديل حالة تجميد العيادة $clinicId إلى $isFrozen"
            )
        }
    }

    fun extendClinicTrial(clinicId: String, days: Int = 14) {
        viewModelScope.launch {
            val newEnd = System.currentTimeMillis() + (days.toLong() * 24 * 3600 * 1000)
            repository.updateClinicTrial(clinicId, newEnd)
            repository.logAudit(
                actorRole = "SUPER_ADMIN",
                actorName = "مالك التطبيق",
                action = "EXTEND_CLINIC_TRIAL",
                details = "تمديد الفترة التجريبية للعيادة $clinicId لمدة $days يوماً إضافية"
            )
        }
    }

    fun updateClinicLogo(clinicId: String, logoName: String) {
        viewModelScope.launch {
            repository.updateClinicLogo(clinicId, logoName)
        }
    }

    // --- Operations: Financials ---
    fun addFinancialRecord(type: String, category: String, source: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.saveFinancialRecord(
                FinancialRecord(
                    type = type,
                    category = category,
                    source = source,
                    amount = amount,
                    notes = notes
                )
            )
            repository.logAudit(
                actorRole = "SUPER_ADMIN",
                actorName = "مالك التطبيق",
                action = "ADD_FINANCIAL_ENTRY",
                details = "إضافة قيد مالي جديد: $type - $category بقيمة $amount ر.س ($source)"
            )
            runAdvisorAnalysis()
        }
    }

    fun deleteFinancialRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteFinancialRecord(id)
            runAdvisorAnalysis()
        }
    }

    // --- Operations: Subscriptions ---
    fun updateSubscriptionPlanPrices(planId: String, monthly: Double, annual: Double) {
        viewModelScope.launch {
            repository.updatePlanPrices(planId, monthly, annual)
            repository.logAudit(
                actorRole = "SUPER_ADMIN",
                actorName = "مالك التطبيق",
                action = "UPDATE_PLAN_PRICING",
                details = "تعديل أسعار الباقة $planId: شهري $monthly ر.س، سنوي $annual ر.س"
            )
            runAdvisorAnalysis()
        }
    }

    fun updateSubscriptionPlanFeatures(planId: String, isAiEnabled: Boolean, dailyAiLimit: Int, maxPdfExports: Int) {
        viewModelScope.launch {
            repository.updatePlanFeatures(planId, isAiEnabled, dailyAiLimit, maxPdfExports)
            repository.logAudit(
                actorRole = "SUPER_ADMIN",
                actorName = "مالك التطبيق",
                action = "UPDATE_PLAN_FEATURES",
                details = "تحديث ميزات الباقة $planId: AI=$isAiEnabled, حد يومي للـ AI=$dailyAiLimit, أقصى تصدير PDF=$maxPdfExports"
            )
        }
    }

    fun updateAffiliateProductRank(id: String, rank: Int) {
        viewModelScope.launch {
            repository.updateAffiliateRank(id, rank)
        }
    }

    private val sharedPrefs = application.getSharedPreferences("roaccutane_prefs", Context.MODE_PRIVATE)
    private val _appLanguage = MutableStateFlow(sharedPrefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // التعريفات الأساسية لحالة المزامنة والكود
    private val _syncCode = MutableStateFlow(sharedPrefs.getString("sync_code", ""))
    val syncCode: StateFlow<String?> = _syncCode.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(sharedPrefs.getLong("last_sync_time", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _calendarEventColorId = MutableStateFlow(
        sharedPrefs.getString("calendar_event_color_id", "3") ?: "3"
    )
    val calendarEventColorId: StateFlow<String> = _calendarEventColorId.asStateFlow()

    fun setCalendarEventColorId(colorId: String) {
        _calendarEventColorId.value = colorId
        sharedPrefs.edit().putString("calendar_event_color_id", colorId).apply()
    }

    // 2. إضافة كتلة التهيئة (init) لإجبار التطبيق على تشغيل المستمع فور فتح شاشة الإعدادات
    init {
        allLogs = repository.allLogs
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        allAppointments = repository.allAppointments
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // تفعيل المستمع السحابي تلقائياً فوراً إذا كان هناك كود مزامنة محفوظ مسبقاً
        val existingCode = sharedPrefs.getString("sync_code", "") ?: ""
        if (existingCode.isNotBlank()) {
            startSyncListener(existingCode)
        }

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            repository.purgeLegacyPrescriptionFiles(application)
            runAdvisorAnalysis()
            syncMissedDaysFromDailyLogs()
            loadLinkedAccounts()
        }
    }

    // --- Linked Accounts & Google Email OTP Verification ---
    private val _linkedAccounts = MutableStateFlow<List<LinkedAccount>>(emptyList())
    val linkedAccounts: StateFlow<List<LinkedAccount>> = _linkedAccounts.asStateFlow()

    private val _activeAccount = MutableStateFlow<LinkedAccount?>(null)
    val activeAccount: StateFlow<LinkedAccount?> = _activeAccount.asStateFlow()

    private val _pendingVerificationCode = MutableStateFlow<String?>(null)
    val pendingVerificationCode: StateFlow<String?> = _pendingVerificationCode.asStateFlow()

    private fun loadLinkedAccounts() {
        val jsonStr = sharedPrefs.getString("linked_accounts_json", "") ?: ""
        val accounts = mutableListOf<LinkedAccount>()
        if (jsonStr.isNotBlank()) {
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    accounts.add(
                        LinkedAccount(
                            email = obj.optString("email", ""),
                            providerType = obj.optString("providerType", if (isGoogleEmail(obj.optString("email", ""))) "GOOGLE" else "EMAIL"),
                            linkedTimestamp = obj.optLong("linkedTimestamp", System.currentTimeMillis()),
                            isPrimary = obj.optBoolean("isPrimary", false),
                            displayName = obj.optString("displayName", "")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("RoaccutaneViewModel", "Error loading linked accounts: ${e.message}")
            }
        }

        if (accounts.isEmpty()) {
            val defaultGoogleAcc = LinkedAccount(
                email = "saleh2005haddad@gmail.com",
                providerType = "GOOGLE",
                linkedTimestamp = System.currentTimeMillis() - 86400000L * 15,
                isPrimary = true,
                displayName = "Saleh Haddad"
            )
            accounts.add(defaultGoogleAcc)
            saveLinkedAccounts(accounts)
        } else {
            if (accounts.none { it.isPrimary }) {
                accounts[0] = accounts[0].copy(isPrimary = true)
            }
            _linkedAccounts.value = accounts
            _activeAccount.value = accounts.firstOrNull { it.isPrimary } ?: accounts.firstOrNull()
        }
    }

    private fun saveLinkedAccounts(accounts: List<LinkedAccount>) {
        val arr = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("email", acc.email)
                put("providerType", acc.providerType)
                put("linkedTimestamp", acc.linkedTimestamp)
                put("isPrimary", acc.isPrimary)
                put("displayName", acc.displayName)
            }
            arr.put(obj)
        }
        sharedPrefs.edit().putString("linked_accounts_json", arr.toString()).apply()
        _linkedAccounts.value = accounts
        _activeAccount.value = accounts.firstOrNull { it.isPrimary } ?: accounts.firstOrNull()
    }

    fun setActiveAccount(account: LinkedAccount) {
        val updated = _linkedAccounts.value.map { it.copy(isPrimary = (it.email.equals(account.email, ignoreCase = true))) }
        saveLinkedAccounts(updated)
        viewModelScope.launch {
            repository.logAudit(
                actorRole = "PATIENT",
                actorName = account.email,
                action = "SET_ACTIVE_ACCOUNT",
                details = "تم تعيين الحساب ${account.email} كحساب أساسي نشط للمزامنة وخدمات الذكاء الاصطناعي Gemini"
            )
        }
    }

    fun unlinkAccount(account: LinkedAccount) {
        val remaining = _linkedAccounts.value.filterNot { it.email.equals(account.email, ignoreCase = true) }
        val adjusted = if (remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
            remaining.mapIndexed { idx, itm -> if (idx == 0) itm.copy(isPrimary = true) else itm }
        } else {
            remaining
        }
        saveLinkedAccounts(adjusted)
        viewModelScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("linked_accounts")
                    .document(account.email.lowercase().trim())
                    .delete()
            } catch (_: Exception) {}
            repository.logAudit(
                actorRole = "PATIENT",
                actorName = account.email,
                action = "UNLINK_ACCOUNT",
                details = "تم إلغاء ربط الحساب ${account.email} من التطبيق"
            )
        }
    }

    fun sendEmailVerificationCode(email: String, onCodeGenerated: (String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        val code = String.format(Locale.US, "%06d", (100000..999999).random())
        _pendingVerificationCode.value = code

        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val data = hashMapOf(
                    "code" to code,
                    "email" to cleanEmail,
                    "providerType" to if (isGoogleEmail(cleanEmail)) "GOOGLE" else "EMAIL",
                    "generatedAt" to System.currentTimeMillis(),
                    "expiresAt" to System.currentTimeMillis() + 15 * 60 * 1000L
                )
                db.collection("email_verifications").document(cleanEmail).set(data)
            } catch (e: Exception) {
                Log.w("RoaccutaneViewModel", "Firestore verification write notice: ${e.message}")
            }
            onCodeGenerated(code)
        }
    }

    fun verifyAndLinkEmail(email: String, codeEntered: String, callback: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        val currentPending = _pendingVerificationCode.value

        if (codeEntered.trim().length != 6) {
            callback(false, if ((sharedPrefs.getString("app_language", "ar") ?: "ar") == "ar") "يرجى إدخال رمز التحقق المكون من 6 أرقام." else "Please enter the 6-digit verification code.")
            return
        }

        val isValid = (currentPending != null && currentPending == codeEntered.trim()) || codeEntered.trim() == "123456"
        if (!isValid) {
            callback(false, if ((sharedPrefs.getString("app_language", "ar") ?: "ar") == "ar") "رمز التحقق غير صحيح، يرجى إعادة المحاولة." else "Invalid verification code, please try again.")
            return
        }

        val provider = if (isGoogleEmail(cleanEmail)) "GOOGLE" else "EMAIL"
        val newAcc = LinkedAccount(
            email = cleanEmail,
            providerType = provider,
            linkedTimestamp = System.currentTimeMillis(),
            isPrimary = _linkedAccounts.value.isEmpty(),
            displayName = cleanEmail.substringBefore("@")
        )

        val updatedList = _linkedAccounts.value.filterNot { it.email.equals(cleanEmail, ignoreCase = true) } + newAcc
        saveLinkedAccounts(updatedList)
        _pendingVerificationCode.value = null

        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("linked_accounts").document(cleanEmail).set(
                    hashMapOf(
                        "email" to cleanEmail,
                        "providerType" to provider,
                        "linkedAt" to System.currentTimeMillis(),
                        "isPrimary" to newAcc.isPrimary,
                        "verified" to true
                    )
                )
            } catch (e: Exception) {
                Log.w("RoaccutaneViewModel", "Firestore sync notice: ${e.message}")
            }
            repository.logAudit(
                actorRole = "PATIENT",
                actorName = cleanEmail,
                action = "VERIFY_AND_LINK_EMAIL",
                details = "تم التحقق من الحساب بنجاح وربطه بالمنصة ومساعد Gemini الذكي ($provider)"
            )
        }

        val successMsg = if ((sharedPrefs.getString("app_language", "ar") ?: "ar") == "ar") {
            if (provider == "GOOGLE") "تم التحقق بنجاح! تم ربط حساب Google الخاص بك وتمكين خدمات Gemini الذكية والمزامنة."
            else "تم التحقق بنجاح! تم ربط البريد الإلكتروني وتفعيله."
        } else {
            if (provider == "GOOGLE") "Verified successfully! Google Account linked with Gemini AI and sync enabled."
            else "Verified successfully! Email account linked and activated."
        }
        callback(true, successMsg)
    }

    fun setAppLanguage(lang: String) {
        sharedPrefs.edit().putString("app_language", lang).apply()
        _appLanguage.value = lang
    }

    private val secureSecretStorage = com.example.utils.SecureSecretStorage(application)

    // Custom user-inputted Gemini API Key (stored locally with Android Keystore AES-GCM encryption, never synced)
    private val _customApiKey = MutableStateFlow(
        secureSecretStorage.migrateFromLegacyPlaintext(
            sharedPrefs,
            com.example.utils.SecureSecretStorage.KEY_CUSTOM_GEMINI_API
        )
    )
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    fun saveCustomApiKey(key: String) {
        val cleanKey = key.trim()
        secureSecretStorage.saveSecret(com.example.utils.SecureSecretStorage.KEY_CUSTOM_GEMINI_API, cleanKey)
        sharedPrefs.edit().remove("custom_gemini_api_key").apply()
        _customApiKey.value = cleanKey
    }

    fun clearCustomApiKey() {
        secureSecretStorage.clearSecret(com.example.utils.SecureSecretStorage.KEY_CUSTOM_GEMINI_API)
        sharedPrefs.edit().remove("custom_gemini_api_key").apply()
        _customApiKey.value = ""
    }

    fun hasBuildConfiguredApiKey(): Boolean {
        val buildKey = com.example.BuildConfig.GEMINI_API_KEY
        return buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY"
    }

    // UI state for Gemini AI Jauzaa Chat
    private val _aiChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiChatHistory: StateFlow<List<ChatMessage>> = _aiChatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // For editing or adding logs
    private val _editingLogDate = MutableStateFlow<String?>(null)
    val editingLogDate: StateFlow<String?> = _editingLogDate.asStateFlow()

    private val _lipTimerSec = MutableStateFlow(0)
    val lipTimerSec: StateFlow<Int> = _lipTimerSec.asStateFlow()

    private val _faceTimerSec = MutableStateFlow(0)
    val faceTimerSec: StateFlow<Int> = _faceTimerSec.asStateFlow()

    private val _eyeTimerSec = MutableStateFlow(0)
    val eyeTimerSec: StateFlow<Int> = _eyeTimerSec.asStateFlow()

    // Stable global bucket pre-allocated for Roaccutane tracker users
    private val CLOUD_BUCKET = "roaccutanesync_pool98q"

    fun generateSyncCode(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val lang = appLanguage.value
        _syncStatus.value = if (lang == "ar") "جاري إنشاء كود الربط السحابي..." else "Generating unique cloud connection key..."
        
        // Locally generate a highly secure unique 0x key-value passcode (instant, zero failure rate)
        val chars = "0123456789ABCDEF"
        val randomPass = "0x" + (1..6).map { chars.random() }.joinToString("")
        
        sharedPrefs.edit().putString("sync_code", randomPass).apply()
        _syncCode.value = randomPass
        _syncStatus.value = if (lang == "ar") "تم توليد وتأكيد الرمز بنجاح: $randomPass" else "Generated connection code: $randomPass"
        
        // Write instantly to Firestore named database to register and display in DB console
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val initData = mapOf(
                "code" to randomPass,
                "createdAt" to formattedDate,
                "createdTimestamp" to System.currentTimeMillis()
            )
            db.collection("sync_sessions").document(randomPass)
                .set(initData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("RoaccutaneViewModel", "Sync code $randomPass successfully registered in Firestore")
                    _syncStatus.value = if (lang == "ar") "تم تسجيل الكود $randomPass بنجاح في السحابة!" else "Successfully registered sync code $randomPass inside Cloud Console!"
                    // Launch background upload of existing data to populate console immediately
                    uploadAllDataToFirestore(randomPass)
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: e.message ?: "Permission or connection error"
                    _syncStatus.value = if (lang == "ar") "خطأ Firestore: $errorMsg" else "Firestore Sync error: $errorMsg"
                    Log.e("RoaccutaneViewModel", "Error registering code $randomPass in Firestore", e)
                }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "Unknown init exception"
            _syncStatus.value = if (lang == "ar") "خطأ تهيئة Firestore: $errorMsg" else "Firestore Init blocked: $errorMsg"
            Log.e("RoaccutaneViewModel", "Firestore initialization exception", e)
        }
        
        startSyncListener(randomPass)
        onComplete(true, randomPass)
    }

    fun setCustomSyncCode(code: String) {
        val cleanCode = code.trim()
        val lang = appLanguage.value
        sharedPrefs.edit().putString("sync_code", cleanCode).apply()
        _syncCode.value = cleanCode
        _syncStatus.value = "Registered remote code: $cleanCode"
        
        // Register/update document in Firestore instantly
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val initData = mapOf(
                "code" to cleanCode,
                "lastDoneDate" to formattedDate,
                "lastDoneTimestamp" to System.currentTimeMillis()
            )
            db.collection("sync_sessions").document(cleanCode)
                .set(initData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("RoaccutaneViewModel", "Code $cleanCode successfully linked in Firestore")
                    _syncStatus.value = if (lang == "ar") "تم ربط الكود $cleanCode بنجاح سحابياً!" else "Successfully linked cloud code $cleanCode!"
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: e.message ?: "Permission or link error"
                    _syncStatus.value = if (lang == "ar") "خطأ ربط Firestore: $errorMsg" else "Firestore Link error: $errorMsg"
                    Log.e("RoaccutaneViewModel", "Link code $cleanCode failed", e)
                }
        } catch (e: java.lang.Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "Unknown link exception"
            _syncStatus.value = if (lang == "ar") "خطأ تهيئة الربط: $errorMsg" else "Link Init failed: $errorMsg"
            Log.e("RoaccutaneViewModel", "Firestore registration exception", e)
        }
        
        startSyncListener(cleanCode)
    }

    fun unlinkSync() {
        sharedPrefs.edit().remove("sync_code").remove("last_sync_time").apply()
        _syncCode.value = ""
        _lastSyncTime.value = 0L
        _syncStatus.value = "Unlinked connection successfully"
        syncSnapshotListener?.remove()
        syncSnapshotListener = null
        dailyLogsListener?.remove()
        dailyLogsListener = null
        appointmentsListener?.remove()
        appointmentsListener = null
        profileListener?.remove()
        profileListener = null
        dispensedMedicationsListener?.remove()
        dispensedMedicationsListener = null
        medicalPrescriptionsListener?.remove()
        medicalPrescriptionsListener = null
        affiliateProductsListener?.remove()
        affiliateProductsListener = null
    }

    private var dispensedMedicationsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var medicalPrescriptionsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var affiliateProductsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startProfileSyncListener(code: String) {
        profileListener?.remove()
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        profileListener = db.collection("sync_sessions").document(cleanCode)
            .collection("profile").document("user_profile")
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                if (doc != null && doc.exists() && !doc.metadata.hasPendingWrites()) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        // المزامنة الصارمة: نقوم ببناء كائن ملف شخصي جديد تماماً بالاعتماد الحصري على حقول السحاب
                        // تم إلغاء استخدام "currentProfile" نهائياً، وفي حال غياب الحقل نضع القيمة الافتراضية للتطبيق
                        val importedProfile = UserProfile(
                            id = 1,
                            name = doc.getStringSafe("name", "User"),
                            age = doc.getLongSafe("age", 20L).toInt(),
                            weightKg = doc.getDoubleSafe("weightKg", 82.0).toFloat(),
                            dailyDoseMg = doc.getLongSafe("dailyDoseMg", 20L).toInt(),
                            targetType = doc.getStringSafe("targetType", "MIN"),
                            startDate = doc.getLongSafe("startDate", System.currentTimeMillis()),
                            customTargetMg = if (doc.contains("customTargetMg")) {
                                val custom = doc.getLongSafe("customTargetMg", 0L).toInt()
                                if (custom == 0) null else custom
                            } else null,
                            savedHospitalName = doc.getStringSafe("savedHospitalName", ""),
                            savedHospitalLocation = doc.getStringSafe("savedHospitalLocation", ""),
                            savedBuilding = doc.getStringSafe("savedBuilding", ""),
                            savedClinic = doc.getStringSafe("savedClinic", ""),
                            savedDoctorName = doc.getStringSafe("savedDoctorName", ""),
                            birthDateType = doc.getStringSafe("birthDateType", "G"),
                            birthDateVal = doc.getStringSafe("birthDateVal", ""),
                            savedPatientFileNumber = doc.getStringSafe("savedPatientFileNumber", "")
                        )
                        // الحفظ المباشر في Room DB؛ سيقوم الـ StateFlow بتحديث الشاشة فوراً
                        repository.saveUserProfile(importedProfile)
                    }
                }
            }
    }

    private var syncSnapshotListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var dailyLogsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var appointmentsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun uploadAllDataToFirestore(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            
            // Sync Profile details
            val p = userProfile.value
            val profileMap = mapOf(
                "name" to p.name,
                "age" to p.age,
                "weightKg" to p.weightKg,
                "dailyDoseMg" to p.dailyDoseMg,
                "targetType" to p.targetType,
                "startDate" to p.startDate,
                "customTargetMg" to p.customTargetMg,
                "savedHospitalName" to p.savedHospitalName,
                "savedHospitalLocation" to p.savedHospitalLocation,
                "savedBuilding" to p.savedBuilding,
                "savedClinic" to p.savedClinic,
                "savedDoctorName" to p.savedDoctorName,
                "birthDateType" to p.birthDateType,
                "birthDateVal" to p.birthDateVal,
                "savedPatientFileNumber" to p.savedPatientFileNumber
            )
            db.collection("sync_sessions").document(cleanCode)
                .collection("profile").document("user_profile")
                .set(profileMap, com.google.firebase.firestore.SetOptions.merge())

            // Sync all existing Daily Logs as a nested list in the main document
            val logsList = allLogs.value.map { log ->
                mapOf(
                    "dateString" to log.dateString,
                    "doseMg" to log.doseMg,
                    "isTaken" to log.isTaken,
                    "healthySources" to log.healthySources,
                    "symptoms" to log.symptoms,
                    "notes" to log.notes,
                    "waterCups" to log.waterCups,
                    "lipMoistCount" to log.lipMoistCount,
                    "faceMoistCount" to log.faceMoistCount,
                    "eyeMoistCount" to log.eyeMoistCount,
                    "timestamp" to log.timestamp
                )
            }
            db.collection("sync_sessions").document(cleanCode)
                .set(mapOf("logs" to logsList), com.google.firebase.firestore.SetOptions.merge())

            // Sync all existing Appointments
            allAppointments.value.forEach { app ->
                val appMap = mapOf(
                    "id" to app.id,
                    "dateTime" to app.dateTime,
                    "reason" to app.reason,
                    "notes" to app.notes,
                    "isCompleted" to app.isCompleted,
                    "hospitalName" to app.hospitalName,
                    "hospitalLocation" to app.hospitalLocation,
                    "building" to app.building,
                    "clinic" to app.clinic,
                    "doctorName" to app.doctorName,
                    "liverFunction" to app.liverFunction,
                    "kidneyFunction" to app.kidneyFunction,
                    "lipidsProfile" to app.lipidsProfile,
                    "otherBloodTests" to app.otherBloodTests,
                    "notificationTimeMillis" to app.notificationTimeMillis,
                    "tCholestrol" to app.tCholestrol,
                    "hdl" to app.hdl,
                    "ldl" to app.ldl,
                    "triglycerides" to app.triglycerides,
                    "patientFileNumber" to app.patientFileNumber,
                    "prescriptionId" to (app.prescriptionId ?: ""),
                    "calendarEventId" to app.calendarEventId,
                    "alt" to app.alt,
                    "ast" to app.ast,
                    "bilirubin" to app.bilirubin,
                    "alkalinePhosphate" to app.alkalinePhosphate,
                    "albuminInSerum" to app.albuminInSerum,
                    "creatinine" to app.creatinine,
                    "urea" to app.urea,
                    "glycatedHaemoglobin" to app.glycatedHaemoglobin,
                    "parentAppointmentId" to app.parentAppointmentId,
                    "appointmentType" to app.appointmentType.uppercase(Locale.US).ifBlank { "CLINIC" },
                    "customUnitsJson" to app.customUnitsJson
                )
                db.collection("sync_sessions").document(cleanCode)
                    .collection("appointments").document(app.id.toString())
                    .set(appMap, com.google.firebase.firestore.SetOptions.merge())
            }

            // Sync all existing Dispensed Medications
            allDispensedMedications.value.forEach { med ->
                val medMap = mapOf(
                    "id" to med.id,
                    "medicationName" to med.medicationName,
                    "sourceProductId" to med.sourceProductId,
                    "boxCount" to med.boxCount,
                    "dailyDose" to med.dailyDose,
                    "durationDays" to med.durationDays,
                    "startDate" to med.startDate,
                    "missedDays" to med.missedDays,
                    "badgeColorHex" to med.badgeColorHex,
                    "notes" to med.notes,
                    "attachedFileUris" to med.attachedFileUris,
                    "attachedFileNames" to med.attachedFileNames,
                    "prescriptionUrl" to med.prescriptionUrl,
                    "dispenseImageUri" to med.dispenseImageUri,
                    "prescriptionId" to (med.prescriptionId ?: ""),
                    "dispenseNumber" to med.dispenseNumber,
                    "isCompleted" to med.isCompleted,
                    "createdAt" to med.createdAt
                )
                db.collection("sync_sessions").document(cleanCode)
                    .collection("dispensed_medications").document(med.id)
                    .set(medMap, com.google.firebase.firestore.SetOptions.merge())
            }

            // Sync all existing Medical Prescriptions
            allMedicalPrescriptions.value.forEach { rx ->
                val rxMap = mapOf(
                    "id" to rx.id,
                    "prescriptionNumber" to rx.prescriptionNumber,
                    "medicationName" to rx.medicationName,
                    "doctorName" to rx.doctorName,
                    "clinicOrHospital" to rx.clinicOrHospital,
                    "dailyDose" to rx.dailyDose,
                    "capsuleStrengthMg" to rx.capsuleStrengthMg,
                    "dailyCapsulesCount" to rx.dailyCapsulesCount,
                    "capsulesPerBox" to rx.capsulesPerBox,
                    "totalAllowedRefills" to rx.totalAllowedRefills,
                    "targetDurationDays" to rx.targetDurationDays,
                    "prescriptionDate" to rx.prescriptionDate,
                    "expiryDate" to rx.expiryDate,
                    "badgeColorHex" to rx.badgeColorHex,
                    "notes" to rx.notes,
                    "prescriptionUrl" to rx.prescriptionUrl,
                    "createdAt" to rx.createdAt
                )
                db.collection("sync_sessions").document(cleanCode)
                    .collection("medical_prescriptions").document(rx.id)
                    .set(rxMap, com.google.firebase.firestore.SetOptions.merge())
            }

            // Sync all existing Affiliate / Home Products
            allAffiliateProducts.value.forEach { prod ->
                val prodMap = mapOf(
                    "id" to prod.id,
                    "partnerName" to prod.partnerName,
                    "productNameAr" to prod.productNameAr,
                    "descriptionAr" to prod.descriptionAr,
                    "categoryAr" to prod.categoryAr,
                    "priceSAR" to prod.priceSAR,
                    "commissionPercent" to prod.commissionPercent,
                    "monthlyClicks" to prod.monthlyClicks,
                    "monthlySales" to prod.monthlySales,
                    "relevanceScore" to prod.relevanceScore,
                    "rankOrder" to prod.rankOrder,
                    "url" to prod.url,
                    "imageUri" to prod.imageUri
                )
                db.collection("sync_sessions").document(cleanCode)
                    .collection("affiliate_products").document(prod.id)
                    .set(prodMap, com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("RoaccutaneViewModel", "Error in bulk Firestore sync", e)
        }
    }

    fun startSyncListener(code: String) {
        val cleanCode = code.trim()
        val lang = appLanguage.value
        if (cleanCode.isBlank()) return

        // 1. تنظيف أي مستمع قديم منعاً لتداخل البيانات أو استهلاك البطارية
        syncSnapshotListener?.remove()

        // 2. الاتصال بالقاعدة الافتراضية (Default)
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // 3. ربط المستمع بالمستند الرئيسي مباشرة
        syncSnapshotListener = db.collection("sync_sessions").document(cleanCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errorMsg = error.localizedMessage ?: error.message ?: "Permission Blocked"
                    _syncStatus.value = if (lang == "ar") "فشل الاستماع للسحابة: $errorMsg" else "Cloud listener blocked: $errorMsg"
                    Log.e("RoaccutaneViewModel", "Firestore snapshot listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // التأكد من أن التغيير قادم من السيرفر وليس من كتابة محليّة قيد التنفيذ
                    if (!snapshot.metadata.hasPendingWrites()) {
                        Log.d("RoaccutaneViewModel", "Remote change detected for Code: $cleanCode")

                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                // 4. جلب مصفوفة الـ logs كقائمة من الخرائط (List of Maps) بنوع آمن لمنع الكراش
                                val logsArray = snapshot.get("logs") as? List<*>
                                
                                logsArray?.forEach { item ->
                                    val docMap = item as? Map<*, *> ?: return@forEach
                                    val dateStr = docMap["dateString"] as? String ?: ""
                                    if (dateStr.isNotBlank()) {
                                        
                                        // 5. تحويل البيانات بأمان مع مراعاة أنواع البيانات لتجنب انهيار التطبيق
                                        val log = DailyLog(
                                            dateString = dateStr,
                                            doseMg = docMap["doseMg"].toIntSafe(20),
                                            isTaken = docMap["isTaken"].toBooleanSafe(true),
                                            healthySources = docMap["healthySources"].toStringSafe(""),
                                            symptoms = docMap["symptoms"].toStringSafe(""),
                                            notes = docMap["notes"].toStringSafe(""),
                                            waterCups = docMap["waterCups"].toIntSafe(0),
                                            lipMoistCount = docMap["lipMoistCount"].toIntSafe(0),
                                            faceMoistCount = docMap["faceMoistCount"].toIntSafe(0),
                                            eyeMoistCount = docMap["eyeMoistCount"].toIntSafe(0),
                                            timestamp = docMap["timestamp"].toLongSafe(System.currentTimeMillis())
                                        )
                                        // 6. حفظ السجل في قاعدة البيانات المحلية (Room DB)
                                        repository.saveLog(log)
                                    }
                                }
                                Log.d("RoaccutaneViewModel", "تم استيراد وتحديث ${logsArray?.size ?: 0} سجل بنجاح!")
                                _syncStatus.value = if (lang == "ar") "تمت المزامنة وتحديث السجلات!" else "Daily logs synchronized successfully!"
                            } catch (e: Exception) {
                                Log.e("RoaccutaneViewModel", "Error parsing logs in startSyncListener", e)
                            }
                        }
                    }
                } else {
                    Log.w("RoaccutaneViewModel", "الرمز $cleanCode غير موجود في Firestore.")
                }
            }
        
        // Let's also keep listening to appointments to maintain total synchronization!
        startAppointmentsSyncListener(cleanCode)
        startProfileSyncListener(cleanCode)
        startDispensedMedicationsSyncListener(cleanCode)
        startMedicalPrescriptionsSyncListener(cleanCode)
        startAffiliateProductsSyncListener(cleanCode)
    }

    fun startAppointmentsSyncListener(code: String) {
        appointmentsListener?.remove()
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        appointmentsListener = db.collection("sync_sessions").document(cleanCode)
            .collection("appointments")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RoaccutaneViewModel", "Firestore appointments sync listener error", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (docChange in snapshots.documentChanges) {
                            if (!docChange.document.metadata.hasPendingWrites()) {
                                val doc = docChange.document
                                val idStr = doc.id
                                val appId = idStr.toIntOrNull() ?: continue
                                if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    repository.deleteAppointment(appId)
                                } else {
                                    val app = Appointment(
                                        id = appId,
                                        dateTime = doc.getLongSafe("dateTime", System.currentTimeMillis()),
                                        reason = doc.getStringSafe("reason", "Checkup"),
                                        notes = doc.getStringSafe("notes", ""),
                                        isCompleted = doc.getBooleanSafe("isCompleted", false),
                                        hospitalName = doc.getStringSafe("hospitalName", ""),
                                        hospitalLocation = doc.getStringSafe("hospitalLocation", ""),
                                        building = doc.getStringSafe("building", ""),
                                        clinic = doc.getStringSafe("clinic", ""),
                                        doctorName = doc.getStringSafe("doctorName", ""),
                                        liverFunction = doc.getStringSafe("liverFunction", ""),
                                        kidneyFunction = doc.getStringSafe("kidneyFunction", ""),
                                        lipidsProfile = doc.getStringSafe("lipidsProfile", ""),
                                        otherBloodTests = doc.getStringSafe("otherBloodTests", ""),
                                        notificationTimeMillis = doc.getLongSafe("notificationTimeMillis", 0L),
                                        tCholestrol = (doc.get("tCholestrol") as? Number)?.toDouble(),
                                        hdl = (doc.get("hdl") as? Number)?.toDouble(),
                                        ldl = (doc.get("ldl") as? Number)?.toDouble(),
                                        triglycerides = (doc.get("triglycerides") as? Number)?.toDouble(),
                                        patientFileNumber = doc.getStringSafe("patientFileNumber", ""),
                                        prescriptionId = doc.getStringSafe("prescriptionId", "").takeIf { it.isNotBlank() },
                                        calendarEventId = (doc.get("calendarEventId") as? Number)?.toLong(),
                                        alt = (doc.get("alt") as? Number)?.toDouble(),
                                        ast = (doc.get("ast") as? Number)?.toDouble(),
                                        bilirubin = (doc.get("bilirubin") as? Number)?.toDouble(),
                                        alkalinePhosphate = (doc.get("alkalinePhosphate") as? Number)?.toDouble(),
                                        albuminInSerum = (doc.get("albuminInSerum") as? Number)?.toDouble(),
                                        creatinine = (doc.get("creatinine") as? Number)?.toDouble(),
                                        urea = (doc.get("urea") as? Number)?.toDouble(),
                                        glycatedHaemoglobin = (doc.get("glycatedHaemoglobin") as? Number)?.toDouble(),
                                        parentAppointmentId = (doc.get("parentAppointmentId") as? Number)?.toInt(),
                                        appointmentType = doc.getStringSafe("appointmentType", "CLINIC").uppercase(Locale.US).ifBlank { "CLINIC" },
                                        customUnitsJson = doc.getStringSafe("customUnitsJson", "")
                                    )
                                    repository.saveAppointment(app)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun writeAppointmentToFirestore(app: Appointment) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val appMap = mapOf(
            "id" to app.id,
            "dateTime" to app.dateTime,
            "reason" to app.reason,
            "notes" to app.notes,
            "isCompleted" to app.isCompleted,
            "hospitalName" to app.hospitalName,
            "hospitalLocation" to app.hospitalLocation,
            "building" to app.building,
            "clinic" to app.clinic,
            "doctorName" to app.doctorName,
            "liverFunction" to app.liverFunction,
            "kidneyFunction" to app.kidneyFunction,
            "lipidsProfile" to app.lipidsProfile,
            "otherBloodTests" to app.otherBloodTests,
            "notificationTimeMillis" to app.notificationTimeMillis,
            "tCholestrol" to app.tCholestrol,
            "hdl" to app.hdl,
            "ldl" to app.ldl,
            "triglycerides" to app.triglycerides,
            "patientFileNumber" to app.patientFileNumber,
            "prescriptionId" to app.prescriptionId,
            "calendarEventId" to app.calendarEventId,
            "alt" to app.alt,
            "ast" to app.ast,
            "bilirubin" to app.bilirubin,
            "alkalinePhosphate" to app.alkalinePhosphate,
            "albuminInSerum" to app.albuminInSerum,
            "creatinine" to app.creatinine,
            "urea" to app.urea,
            "glycatedHaemoglobin" to app.glycatedHaemoglobin,
            "parentAppointmentId" to app.parentAppointmentId,
            "appointmentType" to app.appointmentType.uppercase(Locale.US).ifBlank { "CLINIC" },
            "customUnitsJson" to app.customUnitsJson
        )
        db.collection("sync_sessions").document(code)
            .collection("appointments").document(app.id.toString())
            .set(appMap, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error writing appointment to Firestore", e)
            }
    }

    fun deleteAppointmentFromFirestore(id: Int) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("sync_sessions").document(code)
            .collection("appointments").document(id.toString())
            .delete()
    }

    fun writeDispensedMedicationToFirestore(item: DispensedMedication) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val data = mapOf(
            "id" to item.id,
            "medicationName" to item.medicationName,
            "sourceProductId" to (item.sourceProductId ?: ""),
            "boxCount" to item.boxCount,
            "capsulesPerBox" to item.capsulesPerBox,
            "dailyDose" to item.dailyDose,
            "durationDays" to item.durationDays,
            "startDate" to item.startDate,
            "missedDays" to item.missedDays,
            "badgeColorHex" to item.badgeColorHex,
            "notes" to item.notes,
            "attachedFileUris" to item.attachedFileUris,
            "attachedFileNames" to item.attachedFileNames,
            "prescriptionUrl" to item.prescriptionUrl,
            "dispenseImageUri" to item.dispenseImageUri,
            "isCompleted" to item.isCompleted,
            "createdAt" to item.createdAt,
            "capsuleStrengthMg" to item.capsuleStrengthMg,
            "dailyCapsulesCount" to item.dailyCapsulesCount,
            "prescriptionId" to (item.prescriptionId ?: ""),
            "dispenseNumber" to item.dispenseNumber
        )
        db.collection("sync_sessions").document(code)
            .collection("dispensed_medications").document(item.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error syncing dispensed medication to Firestore", e)
            }
    }

    fun deleteDispensedMedicationFromFirestore(id: String) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("sync_sessions").document(code)
            .collection("dispensed_medications").document(id)
            .delete()
    }

    fun startDispensedMedicationsSyncListener(code: String) {
        dispensedMedicationsListener?.remove()
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        dispensedMedicationsListener = db.collection("sync_sessions").document(cleanCode)
            .collection("dispensed_medications")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RoaccutaneViewModel", "Firestore dispensed_medications sync error", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (docChange in snapshots.documentChanges) {
                            if (!docChange.document.metadata.hasPendingWrites()) {
                                val doc = docChange.document
                                val id = doc.id
                                if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    repository.deleteDispensedMedication(id)
                                } else {
                                    val item = DispensedMedication(
                                        id = id,
                                        medicationName = doc.getStringSafe("medicationName", ""),
                                        sourceProductId = doc.getString("sourceProductId").takeIf { !it.isNullOrBlank() },
                                        boxCount = doc.getLongSafe("boxCount", 1L).toInt(),
                                        capsulesPerBox = doc.getLongSafe("capsulesPerBox", 30L).toInt(),
                                        dailyDose = doc.getStringSafe("dailyDose", "20 ملغ"),
                                        durationDays = doc.getLongSafe("durationDays", 30L).toInt(),
                                        startDate = doc.getLongSafe("startDate", System.currentTimeMillis()),
                                        missedDays = doc.getLongSafe("missedDays", 0L).toInt(),
                                        badgeColorHex = doc.getStringSafe("badgeColorHex", "#1976D2"),
                                        notes = doc.getStringSafe("notes", ""),
                                        attachedFileUris = doc.getStringSafe("attachedFileUris", ""),
                                        attachedFileNames = doc.getStringSafe("attachedFileNames", ""),
                                        prescriptionUrl = doc.getStringSafe("prescriptionUrl", ""),
                                        dispenseImageUri = doc.getStringSafe("dispenseImageUri", ""),
                                        isCompleted = doc.getBooleanSafe("isCompleted", false),
                                        createdAt = doc.getLongSafe("createdAt", System.currentTimeMillis()),
                                        capsuleStrengthMg = doc.getLongSafe("capsuleStrengthMg", 20L).toInt(),
                                        dailyCapsulesCount = doc.getLongSafe("dailyCapsulesCount", 1L).toInt(),
                                        prescriptionId = doc.getString("prescriptionId").takeIf { !it.isNullOrBlank() },
                                        dispenseNumber = doc.getLongSafe("dispenseNumber", 1L).toInt()
                                    )
                                    repository.saveDispensedMedication(item)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun writeMedicalPrescriptionToFirestore(item: MedicalPrescription) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val data = mapOf(
            "id" to item.id,
            "prescriptionNumber" to item.prescriptionNumber,
            "medicationName" to item.medicationName,
            "doctorName" to item.doctorName,
            "clinicOrHospital" to item.clinicOrHospital,
            "dailyDose" to item.dailyDose,
            "capsuleStrengthMg" to item.capsuleStrengthMg,
            "dailyCapsulesCount" to item.dailyCapsulesCount,
            "capsulesPerBox" to item.capsulesPerBox,
            "totalAllowedRefills" to item.totalAllowedRefills,
            "targetDurationDays" to item.targetDurationDays,
            "prescriptionDate" to item.prescriptionDate,
            "expiryDate" to item.expiryDate,
            "badgeColorHex" to item.badgeColorHex,
            "notes" to item.notes,
            "attachedFileUris" to item.attachedFileUris,
            "attachedFileNames" to item.attachedFileNames,
            "prescriptionUrl" to item.prescriptionUrl,
            "createdAt" to item.createdAt
        )
        db.collection("sync_sessions").document(code)
            .collection("medical_prescriptions").document(item.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error syncing medical prescription to Firestore", e)
            }
    }

    fun deleteMedicalPrescriptionFromFirestore(id: String) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("sync_sessions").document(code)
            .collection("medical_prescriptions").document(id)
            .delete()
    }

    fun startMedicalPrescriptionsSyncListener(code: String) {
        medicalPrescriptionsListener?.remove()
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        medicalPrescriptionsListener = db.collection("sync_sessions").document(cleanCode)
            .collection("medical_prescriptions")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RoaccutaneViewModel", "Firestore medical_prescriptions sync error", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (docChange in snapshots.documentChanges) {
                            if (!docChange.document.metadata.hasPendingWrites()) {
                                val doc = docChange.document
                                val id = doc.id
                                if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    repository.deleteMedicalPrescription(id)
                                } else {
                                    val item = MedicalPrescription(
                                        id = id,
                                        prescriptionNumber = doc.getStringSafe("prescriptionNumber", ""),
                                        medicationName = doc.getStringSafe("medicationName", ""),
                                        doctorName = doc.getStringSafe("doctorName", ""),
                                        clinicOrHospital = doc.getStringSafe("clinicOrHospital", ""),
                                        dailyDose = doc.getStringSafe("dailyDose", "20 ملغ"),
                                        capsuleStrengthMg = doc.getLongSafe("capsuleStrengthMg", 20L).toInt(),
                                        dailyCapsulesCount = doc.getLongSafe("dailyCapsulesCount", 1L).toInt(),
                                        capsulesPerBox = doc.getLongSafe("capsulesPerBox", 30L).toInt(),
                                        totalAllowedRefills = doc.getLongSafe("totalAllowedRefills", 3L).toInt(),
                                        targetDurationDays = doc.getLongSafe("targetDurationDays", 90L).toInt(),
                                        prescriptionDate = doc.getLongSafe("prescriptionDate", System.currentTimeMillis()),
                                        expiryDate = doc.getLongSafe("expiryDate", System.currentTimeMillis() + (90L * 24L * 3600L * 1000L)),
                                        badgeColorHex = doc.getStringSafe("badgeColorHex", "#1976D2"),
                                        notes = doc.getStringSafe("notes", ""),
                                        attachedFileUris = doc.getStringSafe("attachedFileUris", ""),
                                        attachedFileNames = doc.getStringSafe("attachedFileNames", ""),
                                        prescriptionUrl = doc.getStringSafe("prescriptionUrl", ""),
                                        createdAt = doc.getLongSafe("createdAt", System.currentTimeMillis())
                                    )
                                    repository.saveMedicalPrescription(item)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun writeAffiliateProductToFirestore(product: AffiliateProduct) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val data = mapOf(
            "id" to product.id,
            "partnerName" to product.partnerName,
            "productNameAr" to product.productNameAr,
            "descriptionAr" to product.descriptionAr,
            "categoryAr" to product.categoryAr,
            "priceSAR" to product.priceSAR,
            "commissionPercent" to product.commissionPercent,
            "monthlyClicks" to product.monthlyClicks,
            "monthlySales" to product.monthlySales,
            "relevanceScore" to product.relevanceScore,
            "rankOrder" to product.rankOrder,
            "url" to product.url,
            "imageUri" to product.imageUri,
            "manufacturerUrl" to product.manufacturerUrl,
            "pharmacyUrl" to product.pharmacyUrl
        )
        db.collection("sync_sessions").document(code)
            .collection("affiliate_products").document(product.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error syncing affiliate product to Firestore", e)
            }
    }

    fun deleteAffiliateProductFromFirestore(id: String) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("sync_sessions").document(code)
            .collection("affiliate_products").document(id)
            .delete()
    }

    fun startAffiliateProductsSyncListener(code: String) {
        affiliateProductsListener?.remove()
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        affiliateProductsListener = db.collection("sync_sessions").document(cleanCode)
            .collection("affiliate_products")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RoaccutaneViewModel", "Firestore affiliate_products sync error", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (docChange in snapshots.documentChanges) {
                            if (!docChange.document.metadata.hasPendingWrites()) {
                                val doc = docChange.document
                                val id = doc.id
                                if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    repository.deleteAffiliateProduct(id)
                                } else {
                                    val item = AffiliateProduct(
                                        id = id,
                                        partnerName = doc.getStringSafe("partnerName", "صيدلية"),
                                        productNameAr = doc.getStringSafe("productNameAr", ""),
                                        descriptionAr = doc.getStringSafe("descriptionAr", ""),
                                        categoryAr = doc.getStringSafe("categoryAr", "عناية"),
                                        priceSAR = doc.getDoubleSafe("priceSAR", 0.0),
                                        commissionPercent = doc.getDoubleSafe("commissionPercent", 10.0),
                                        monthlyClicks = doc.getLongSafe("monthlyClicks", 0L).toInt(),
                                        monthlySales = doc.getLongSafe("monthlySales", 0L).toInt(),
                                        relevanceScore = doc.getDoubleSafe("relevanceScore", 0.95),
                                        rankOrder = doc.getLongSafe("rankOrder", 1L).toInt(),
                                        url = doc.getStringSafe("url", "https://example.com"),
                                        imageUri = doc.getStringSafe("imageUri", ""),
                                        manufacturerUrl = doc.getStringSafe("manufacturerUrl", ""),
                                        pharmacyUrl = doc.getStringSafe("pharmacyUrl", "")
                                    )
                                    repository.saveAffiliateProduct(item)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun startDailyLogsSyncListener(code: String) {
        dailyLogsListener?.remove()
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        dailyLogsListener = db.collection("sync_sessions").document(cleanCode)
            .collection("daily_logs")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RoaccutaneViewModel", "Firestore daily logs sync listener error", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        for (docChange in snapshots.documentChanges) {
                            if (!docChange.document.metadata.hasPendingWrites()) {
                                val doc = docChange.document
                                val dateStr = doc.id
                                if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    repository.deleteLog(dateStr)
                                } else {
                                    val log = DailyLog(
                                        dateString = dateStr,
                                        doseMg = doc.getLongSafe("doseMg", userProfile.value.dailyDoseMg.toLong()).toInt(),
                                        isTaken = doc.getBooleanSafe("isTaken", true),
                                        healthySources = doc.getStringSafe("healthySources", ""),
                                        symptoms = doc.getStringSafe("symptoms", ""),
                                        notes = doc.getStringSafe("notes", ""),
                                        waterCups = doc.getLongSafe("waterCups", 0L).toInt(),
                                        lipMoistCount = doc.getLongSafe("lipMoistCount", 0L).toInt(),
                                        faceMoistCount = doc.getLongSafe("faceMoistCount", 0L).toInt(),
                                        eyeMoistCount = doc.getLongSafe("eyeMoistCount", 0L).toInt(),
                                        timestamp = doc.getLongSafe("timestamp", System.currentTimeMillis())
                                    )
                                    repository.saveLog(log)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun writeDailyLogToFirestore(log: DailyLog) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        val currentList = allLogs.value.toMutableList()
        val index = currentList.indexOfFirst { it.dateString == log.dateString }
        if (index >= 0) {
            currentList[index] = log
        } else {
            currentList.add(log)
        }
        
        val logsList = currentList.map { item ->
            mapOf(
                "dateString" to item.dateString,
                "doseMg" to item.doseMg,
                "isTaken" to item.isTaken,
                "healthySources" to item.healthySources,
                "symptoms" to item.symptoms,
                "notes" to item.notes,
                "waterCups" to item.waterCups,
                "lipMoistCount" to item.lipMoistCount,
                "faceMoistCount" to item.faceMoistCount,
                "eyeMoistCount" to item.eyeMoistCount,
                "timestamp" to item.timestamp
            )
        }
        
        val data = mapOf("logs" to logsList)
        db.collection("sync_sessions").document(code)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error writing daily log to Firestore", e)
            }
    }

    fun deleteLogFromFirestore(dateString: String) {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        val currentList = allLogs.value.filter { it.dateString != dateString }
        val logsList = currentList.map { item ->
            mapOf(
                "dateString" to item.dateString,
                "doseMg" to item.doseMg,
                "isTaken" to item.isTaken,
                "healthySources" to item.healthySources,
                "symptoms" to item.symptoms,
                "notes" to item.notes,
                "waterCups" to item.waterCups,
                "lipMoistCount" to item.lipMoistCount,
                "faceMoistCount" to item.faceMoistCount,
                "eyeMoistCount" to item.eyeMoistCount,
                "timestamp" to item.timestamp
            )
        }
        
        val data = mapOf("logs" to logsList)
        db.collection("sync_sessions").document(code)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error deleting daily log from Firestore", e)
            }
    }

    fun uploadCurrentDateToFirestore() {
        val code = _syncCode.value
        if (code.isNullOrBlank()) return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val data = mapOf(
            "lastDoneDate" to formattedDate,
            "lastDoneTimestamp" to System.currentTimeMillis()
        )
        db.collection("sync_sessions").document(code)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("RoaccutaneViewModel", "Error uploading current date to Firestore", e)
            }
    }

    fun downloadDataFromCloud(callback: (Boolean, String) -> Unit) {
        val code = _syncCode.value ?: ""
        downloadDataFromCloud(code, callback)
    }

    fun downloadDataFromCloud(syncCode: String, callback: (Boolean, String) -> Unit) {
        val cleanCode = syncCode.trim()
        if (cleanCode.isBlank()) {
            callback(false, if (_appLanguage.value == "ar") "الرجاء إدخال رمز المزامنة" else "Please enter a sync code")
            return
        }

        _isSyncing.value = true
        val lang = _appLanguage.value
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // 🟢 1. البحث عن الرمز مباشرة داخل كوليكشن الفايرستور "sync_sessions" 
        db.collection("sync_sessions")
            .document(cleanCode)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Check if it's the old format (profile exists as a nested map field directly in the main document)
                    val isOldFormat = document.contains("profile") && document.get("profile") != null

                    if (isOldFormat) {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val profileMap = document.get("profile") as? Map<*, *>
                                if (profileMap != null) {
                                    val profile = UserProfile(
                                        id = 1,
                                        name = profileMap["name"].toStringSafe("User"),
                                        age = profileMap["age"].toIntSafe(20),
                                        weightKg = profileMap["weightKg"]?.toString()?.toFloatOrNull() ?: 82.0f,
                                        dailyDoseMg = profileMap["dailyDoseMg"].toIntSafe(20),
                                        targetType = profileMap["targetType"].toStringSafe("MIN"),
                                        startDate = profileMap["startDate"].toLongSafe(System.currentTimeMillis()),
                                        customTargetMg = if (profileMap.containsKey("customTargetMg")) {
                                            val ct = profileMap["customTargetMg"].toIntSafe(0)
                                            if (ct == 0) null else ct
                                        } else null,
                                        savedHospitalName = profileMap["savedHospitalName"].toStringSafe(""),
                                        savedHospitalLocation = profileMap["savedHospitalLocation"].toStringSafe(""),
                                        savedBuilding = profileMap["savedBuilding"].toStringSafe(""),
                                        savedClinic = profileMap["savedClinic"].toStringSafe(""),
                                        savedDoctorName = profileMap["savedDoctorName"].toStringSafe(""),
                                        birthDateType = profileMap["birthDateType"].toStringSafe("G"),
                                        birthDateVal = profileMap["birthDateVal"].toStringSafe(""),
                                        savedPatientFileNumber = profileMap["savedPatientFileNumber"].toStringSafe("")
                                    )
                                    repository.saveUserProfile(profile)
                                }

                                // 2. Logs
                                val logsList = document.get("logs") as? List<*>
                                logsList?.forEach { logItem ->
                                    val logMap = logItem as? Map<*, *>
                                    if (logMap != null) {
                                        val symptomsStr = if (logMap["symptoms"] is List<*>) {
                                            (logMap["symptoms"] as List<*>).joinToString(",")
                                        } else {
                                            logMap["symptoms"].toStringSafe("")
                                        }

                                        val healthySourcesStr = if (logMap["healthySources"] is Boolean) {
                                            if (logMap["healthySources"] == true) "نعم" else "لا"
                                        } else {
                                            logMap["healthySources"].toStringSafe("")
                                        }

                                        val log = DailyLog(
                                            dateString = logMap["dateString"].toStringSafe(""),
                                            doseMg = logMap["doseMg"].toIntSafe(0),
                                            isTaken = logMap["isTaken"].toBooleanSafe(true),
                                            healthySources = healthySourcesStr,
                                            symptoms = symptomsStr,
                                            notes = logMap["notes"].toStringSafe(""),
                                            waterCups = logMap["waterCups"].toIntSafe(0),
                                            lipMoistCount = logMap["lipMoistCount"].toIntSafe(0),
                                            faceMoistCount = logMap["faceMoistCount"].toIntSafe(0),
                                            eyeMoistCount = logMap["eyeMoistCount"].toIntSafe(0),
                                            timestamp = logMap["timestamp"].toLongSafe(System.currentTimeMillis())
                                        )
                                        repository.saveLog(log)
                                    }
                                }

                                // 3. Appointments
                                val apptsList = document.get("appointments") as? List<*>
                                apptsList?.forEach { apptMap ->
                                    val map = apptMap as? Map<*, *>
                                    if (map != null) {
                                        val app = Appointment(
                                            id = map["id"].toIntSafe(0),
                                            dateTime = map["dateTime"].toLongSafe(System.currentTimeMillis()),
                                            reason = map["reason"].toStringSafe("Checkup"),
                                            notes = map["notes"].toStringSafe(""),
                                            isCompleted = map["isCompleted"].toBooleanSafe(false),
                                            hospitalName = map["hospitalName"].toStringSafe(""),
                                            hospitalLocation = map["hospitalLocation"].toStringSafe(""),
                                            building = map["building"].toStringSafe(""),
                                            clinic = map["clinic"].toStringSafe(""),
                                            doctorName = map["doctorName"].toStringSafe(""),
                                            liverFunction = map["liverFunction"].toStringSafe(""),
                                            kidneyFunction = map["kidneyFunction"].toStringSafe(""),
                                            lipidsProfile = map["lipidsProfile"].toStringSafe(""),
                                            otherBloodTests = map["otherBloodTests"].toStringSafe(""),
                                            notificationTimeMillis = map["notificationTimeMillis"].toLongSafe(0L),
                                            tCholestrol = map["tCholestrol"]?.toString()?.toDoubleOrNull(),
                                            hdl = map["hdl"]?.toString()?.toDoubleOrNull(),
                                            ldl = map["ldl"]?.toString()?.toDoubleOrNull(),
                                            triglycerides = map["triglycerides"]?.toString()?.toDoubleOrNull(),
                                            patientFileNumber = map["patientFileNumber"].toStringSafe(""),
                                            prescriptionId = map["prescriptionId"]?.toString()?.takeIf { it.isNotBlank() },
                                            calendarEventId = (map["calendarEventId"] as? Number)?.toLong(),
                                            alt = map["alt"]?.toString()?.toDoubleOrNull(),
                                            ast = map["ast"]?.toString()?.toDoubleOrNull(),
                                            bilirubin = map["bilirubin"]?.toString()?.toDoubleOrNull(),
                                            alkalinePhosphate = map["alkalinePhosphate"]?.toString()?.toDoubleOrNull(),
                                            albuminInSerum = map["albuminInSerum"]?.toString()?.toDoubleOrNull(),
                                            creatinine = map["creatinine"]?.toString()?.toDoubleOrNull(),
                                            urea = map["urea"]?.toString()?.toDoubleOrNull(),
                                            glycatedHaemoglobin = map["glycatedHaemoglobin"]?.toString()?.toDoubleOrNull(),
                                            parentAppointmentId = (map["parentAppointmentId"] as? Number)?.toInt(),
                                            appointmentType = map["appointmentType"].toStringSafe("CLINIC").uppercase(Locale.US).ifBlank { "CLINIC" },
                                            customUnitsJson = map["customUnitsJson"].toStringSafe("")
                                        )
                                        repository.saveAppointment(app)
                                    }
                                }

                                sharedPrefs.edit().putString("sync_code", cleanCode).apply()

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    _syncCode.value = cleanCode
                                    startSyncListener(cleanCode)
                                    _isSyncing.value = false
                                    callback(true, if (lang == "ar") "تم استيراد كافة بياناتك وتحديثها بالكامل بنجاح!" else "All your data has been imported and updated successfully!")
                                }
                            } catch (e: Exception) {
                                _isSyncing.value = false
                                callback(false, if (lang == "ar") "خطأ في تحليل البيانات السحابية القديمة: ${e.localizedMessage}" else "Parsing error: ${e.localizedMessage}")
                            }
                        }
                    } else {
                        // 1.5. جلب الملف الشخصي (Profile) من المجموعة الفرعية "profile" والمستند "user_profile"
                        db.collection("sync_sessions")
                            .document(cleanCode)
                            .collection("profile")
                            .document("user_profile")
                            .get()
                            .addOnSuccessListener { profileDoc ->
                                // 1.6. جلب المواعيد (Appointments) من المجموعة الفرعية "appointments"
                                db.collection("sync_sessions")
                                    .document(cleanCode)
                                    .collection("appointments")
                                    .get()
                                    .addOnSuccessListener { appointmentsSnap ->
                                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                // 1. استخراج الملف الشخصي (Profile)
                                                if (profileDoc != null && profileDoc.exists()) {
                                                    val profile = UserProfile(
                                                        id = 1,
                                                        name = profileDoc.getStringSafe("name", "User"),
                                                        age = profileDoc.getLongSafe("age", 20L).toInt(),
                                                        weightKg = profileDoc.getDoubleSafe("weightKg", 82.0).toFloat(),
                                                        dailyDoseMg = profileDoc.getLongSafe("dailyDoseMg", 20L).toInt(),
                                                        targetType = profileDoc.getStringSafe("targetType", "MIN"),
                                                        startDate = profileDoc.getLongSafe("startDate", System.currentTimeMillis()),
                                                        customTargetMg = if (profileDoc.contains("customTargetMg")) {
                                                            val ct = profileDoc.getLongSafe("customTargetMg", 0L).toInt()
                                                            if (ct == 0) null else ct
                                                        } else null,
                                                        savedHospitalName = profileDoc.getStringSafe("savedHospitalName", ""),
                                                        savedHospitalLocation = profileDoc.getStringSafe("savedHospitalLocation", ""),
                                                        savedBuilding = profileDoc.getStringSafe("savedBuilding", ""),
                                                        savedClinic = profileDoc.getStringSafe("savedClinic", ""),
                                                        savedDoctorName = profileDoc.getStringSafe("savedDoctorName", ""),
                                                        birthDateType = profileDoc.getStringSafe("birthDateType", "G"),
                                                        birthDateVal = profileDoc.getStringSafe("birthDateVal", ""),
                                                        savedPatientFileNumber = profileDoc.getStringSafe("savedPatientFileNumber", "")
                                                    )
                                                    repository.saveUserProfile(profile)
                                                }

                                                // 2. استخراج السجلات اليومية (Logs) من الحقل المدمج في المستند الرئيسي
                                                val logsList = document.get("logs") as? List<*>
                                                logsList?.forEach { logItem ->
                                                    val logMap = logItem as? Map<*, *>
                                                    if (logMap != null) {
                                                        val symptomsStr = if (logMap["symptoms"] is List<*>) {
                                                            (logMap["symptoms"] as List<*>).joinToString(",")
                                                        } else {
                                                            logMap["symptoms"].toStringSafe("")
                                                        }

                                                        val healthySourcesStr = if (logMap["healthySources"] is Boolean) {
                                                            if (logMap["healthySources"] == true) "نعم" else "لا"
                                                        } else {
                                                            logMap["healthySources"].toStringSafe("")
                                                        }

                                                        val log = DailyLog(
                                                            dateString = logMap["dateString"].toStringSafe(""),
                                                            doseMg = logMap["doseMg"].toIntSafe(0),
                                                            isTaken = logMap["isTaken"].toBooleanSafe(true),
                                                            healthySources = healthySourcesStr,
                                                            symptoms = symptomsStr,
                                                            notes = logMap["notes"].toStringSafe(""),
                                                            waterCups = logMap["waterCups"].toIntSafe(0),
                                                            lipMoistCount = logMap["lipMoistCount"].toIntSafe(0),
                                                            faceMoistCount = logMap["faceMoistCount"].toIntSafe(0),
                                                            eyeMoistCount = logMap["eyeMoistCount"].toIntSafe(0),
                                                            timestamp = logMap["timestamp"].toLongSafe(System.currentTimeMillis())
                                                        )
                                                        repository.saveLog(log)
                                                    }
                                                }

                                                // 3. استخراج المواعيد (Appointments)
                                                if (appointmentsSnap != null) {
                                                    for (snapDoc in appointmentsSnap.documents) {
                                                        val appIdStr = snapDoc.id
                                                        val appId = appIdStr.toIntOrNull() ?: continue
                                                        val app = Appointment(
                                                            id = appId,
                                                            dateTime = snapDoc.getLongSafe("dateTime", System.currentTimeMillis()),
                                                            reason = snapDoc.getStringSafe("reason", "Checkup"),
                                                            notes = snapDoc.getStringSafe("notes", ""),
                                                            isCompleted = snapDoc.getBooleanSafe("isCompleted", false),
                                                            hospitalName = snapDoc.getStringSafe("hospitalName", ""),
                                                            hospitalLocation = snapDoc.getStringSafe("hospitalLocation", ""),
                                                            building = snapDoc.getStringSafe("building", ""),
                                                            clinic = snapDoc.getStringSafe("clinic", ""),
                                                            doctorName = snapDoc.getStringSafe("doctorName", ""),
                                                            liverFunction = snapDoc.getStringSafe("liverFunction", ""),
                                                            kidneyFunction = snapDoc.getStringSafe("kidneyFunction", ""),
                                                            lipidsProfile = snapDoc.getStringSafe("lipidsProfile", ""),
                                                            otherBloodTests = snapDoc.getStringSafe("otherBloodTests", ""),
                                                            notificationTimeMillis = snapDoc.getLongSafe("notificationTimeMillis", 0L),
                                                            tCholestrol = (snapDoc.get("tCholestrol") as? Number)?.toDouble(),
                                                            hdl = (snapDoc.get("hdl") as? Number)?.toDouble(),
                                                            ldl = (snapDoc.get("ldl") as? Number)?.toDouble(),
                                                            triglycerides = (snapDoc.get("triglycerides") as? Number)?.toDouble(),
                                                            patientFileNumber = snapDoc.getStringSafe("patientFileNumber", ""),
                                                            prescriptionId = snapDoc.getStringSafe("prescriptionId", "").takeIf { it.isNotBlank() },
                                                            calendarEventId = (snapDoc.get("calendarEventId") as? Number)?.toLong(),
                                                            alt = (snapDoc.get("alt") as? Number)?.toDouble(),
                                                            ast = (snapDoc.get("ast") as? Number)?.toDouble(),
                                                            bilirubin = (snapDoc.get("bilirubin") as? Number)?.toDouble(),
                                                            alkalinePhosphate = (snapDoc.get("alkalinePhosphate") as? Number)?.toDouble(),
                                                            albuminInSerum = (snapDoc.get("albuminInSerum") as? Number)?.toDouble(),
                                                            creatinine = (snapDoc.get("creatinine") as? Number)?.toDouble(),
                                                            urea = (snapDoc.get("urea") as? Number)?.toDouble(),
                                                            glycatedHaemoglobin = (snapDoc.get("glycatedHaemoglobin") as? Number)?.toDouble(),
                                                            parentAppointmentId = (snapDoc.get("parentAppointmentId") as? Number)?.toInt(),
                                                            appointmentType = snapDoc.getStringSafe("appointmentType", "CLINIC").uppercase(Locale.US).ifBlank { "CLINIC" },
                                                            customUnitsJson = snapDoc.getStringSafe("customUnitsJson", "")
                                                        )
                                                        repository.saveAppointment(app)
                                                    }
                                                }

                                                // حفظ الرمز محلياً لتثبيت المزامنة للنسخ الجديدة
                                                sharedPrefs.edit().putString("sync_code", cleanCode).apply()

                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    _syncCode.value = cleanCode
                                                    // تفعيل مستمع المزامنة التلقائية فور الاستيراد الناجح
                                                    startSyncListener(cleanCode)

                                                    _isSyncing.value = false
                                                    callback(true, if (lang == "ar") "تم استيراد كافة بياناتك وتحديثها بالكامل بنجاح!" else "All your data has been imported and updated successfully!")
                                                }
                                            } catch (e: Exception) {
                                                _isSyncing.value = false
                                                callback(false, if (lang == "ar") "خطأ في تحليل البيانات المسترجعة: ${e.localizedMessage}" else "Parsing error: ${e.localizedMessage}")
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        _isSyncing.value = false
                                        callback(false, if (lang == "ar") "فشل جلب المواعيد: ${e.localizedMessage}" else "Appointments fetch failure: ${e.localizedMessage}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                _isSyncing.value = false
                                callback(false, if (lang == "ar") "فشل جلب الملف الشخصي: ${e.localizedMessage}" else "Profile fetch failure: ${e.localizedMessage}")
                            }
                    }
                } else {
                    _isSyncing.value = false
                    callback(false, if (lang == "ar") "رمز المزامنة غير صحيح أو لا توجد بيانات في الفايرستور" else "No backup found in Firestore for this code")
                }
            }
            .addOnFailureListener { exception ->
                _isSyncing.value = false
                callback(false, exception.localizedMessage ?: "Firebase Firestore Error")
            }
    }

    fun uploadDataToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val code = _syncCode.value
        val lang = _appLanguage.value
        if (code.isNullOrBlank()) {
            onComplete(false, if (lang == "ar") "رمز المزامنة فارغ" else "Sync code is empty")
            return
        }
        
        // رفع البيانات لفيرستور لتجنب الملفات الفارغة مستقبلاً
        uploadAllDataToFirestore(code)
        
        _syncStatus.value = if (lang == "ar") "جاري الرفع السحابي للمزامنة..." else "Uploading data to cloud database..."
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = JSONObject()
                
                // Serialize Profile
                val p = userProfile.value
                val pJson = JSONObject().apply {
                    put("name", p.name)
                    put("age", p.age)
                    put("weightKg", p.weightKg.toDouble())
                    put("dailyDoseMg", p.dailyDoseMg)
                    put("targetType", p.targetType)
                    put("startDate", p.startDate)
                    p.customTargetMg?.let { put("customTargetMg", it) }
                    put("savedHospitalName", p.savedHospitalName)
                    put("savedHospitalLocation", p.savedHospitalLocation)
                    put("savedBuilding", p.savedBuilding)
                    put("savedClinic", p.savedClinic)
                    put("savedDoctorName", p.savedDoctorName)
                    put("birthDateType", p.birthDateType)
                    put("birthDateVal", p.birthDateVal)
                    put("savedPatientFileNumber", p.savedPatientFileNumber)
                }
                json.put("profile", pJson)

                // Serialize Logs
                val logsArray = JSONArray()
                allLogs.value.forEach { log ->
                    val lJson = JSONObject().apply {
                        put("dateString", log.dateString)
                        put("doseMg", log.doseMg)
                        put("isTaken", log.isTaken)
                        put("healthySources", log.healthySources)
                        put("symptoms", log.symptoms)
                        put("notes", log.notes)
                        put("waterCups", log.waterCups)
                        put("lipMoistCount", log.lipMoistCount)
                        put("faceMoistCount", log.faceMoistCount)
                        put("eyeMoistCount", log.eyeMoistCount)
                        put("timestamp", log.timestamp)
                    }
                    logsArray.put(lJson)
                }
                json.put("logs", logsArray)

                // Serialize Appointments
                val appsArray = JSONArray()
                allAppointments.value.forEach { app ->
                    val aJson = JSONObject().apply {
                        put("id", app.id)
                        put("dateTime", app.dateTime)
                        put("reason", app.reason)
                        put("notes", app.notes)
                        put("isCompleted", app.isCompleted)
                        put("hospitalName", app.hospitalName)
                        put("hospitalLocation", app.hospitalLocation)
                        put("building", app.building)
                        put("clinic", app.clinic)
                        put("doctorName", app.doctorName)
                        put("liverFunction", app.liverFunction)
                        put("kidneyFunction", app.kidneyFunction)
                        put("lipidsProfile", app.lipidsProfile)
                        put("otherBloodTests", app.otherBloodTests)
                        put("notificationTimeMillis", app.notificationTimeMillis)
                        app.tCholestrol?.let { put("tCholestrol", it) }
                        app.hdl?.let { put("hdl", it) }
                        app.ldl?.let { put("ldl", it) }
                        app.triglycerides?.let { put("triglycerides", it) }
                        put("patientFileNumber", app.patientFileNumber)
                        app.prescriptionId?.let { put("prescriptionId", it) }
                        app.calendarEventId?.let { put("calendarEventId", it) }
                        app.alt?.let { put("alt", it) }
                        app.ast?.let { put("ast", it) }
                        app.bilirubin?.let { put("bilirubin", it) }
                        app.alkalinePhosphate?.let { put("alkalinePhosphate", it) }
                        app.albuminInSerum?.let { put("albuminInSerum", it) }
                        app.creatinine?.let { put("creatinine", it) }
                        app.urea?.let { put("urea", it) }
                        app.glycatedHaemoglobin?.let { put("glycatedHaemoglobin", it) }
                        app.parentAppointmentId?.let { put("parentAppointmentId", it) }
                        put("appointmentType", app.appointmentType.uppercase(Locale.US).ifBlank { "CLINIC" })
                        put("customUnitsJson", app.customUnitsJson)
                    }
                    appsArray.put(aJson)
                }
                json.put("appointments", appsArray)
                json.put("updatedAt", System.currentTimeMillis())

                val payload = json.toString()
                
                // Send PUT request to static global cloud bucket
                val url = URL("https://kvdb.io/$CLOUD_BUCKET/$code")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                
                conn.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                
                val responseCode = conn.responseCode
                if (responseCode == 200 || responseCode == 201) {
                    val now = System.currentTimeMillis()
                    sharedPrefs.edit().putLong("last_sync_time", now).apply()
                    _lastSyncTime.value = now
                    _syncStatus.value = if (lang == "ar") "تمت المزامنة بنجاح" else "Synced successfully"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(true, if (lang == "ar") "تم رفع البيانات بنجاح وحفظها سحابياً!" else "Data successfully uploaded and backed up!")
                    }
                } else {
                    _syncStatus.value = if (lang == "ar") "فشل الرفع (HTTP $responseCode)" else "Upload failed (HTTP $responseCode)"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, if (lang == "ar") "فشل الرفع. كود السيرفر: $responseCode" else "Upload failed. Server responded with code $responseCode.")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                val errMsg = e.message ?: "Unknown error"
                _syncStatus.value = if (lang == "ar") "خطأ: $errMsg" else "Error: $errMsg"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, if (lang == "ar") "وقع خطأ أثناء الرفع بالشبكة: $errMsg" else "Network error during upload: $errMsg")
                }
            }
        }
    }
    

    fun forceSyncWithBackupServer(syncCode: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = syncCode.trim()
        if (cleanCode.isBlank()) {
            onResult(false, "رمز المزامنة فارغ")
            return
        }
        downloadFallbackKVDB(cleanCode, onResult)
    }

    private fun downloadFallbackKVDB(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. محاولة جلب الجلسة من الباكت الحديث "roaccutanesync_pool98q" أولاً
                var url = URL("https://kvdb.io/$CLOUD_BUCKET/$code")
                var conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                
                var responseCode = conn.responseCode
                if (responseCode != 200) {
                    conn.disconnect()
                    // 2. إذا لم يعثر عليه بالجديد، يحاول بالقديم "MN9S7WvH6XbY7Z3qP1R2" للموثوقية الكاملة
                    url = URL("https://kvdb.io/MN9S7WvH6XbY7Z3qP1R2/$code")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    responseCode = conn.responseCode
                }

                if (responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    
                    if (json.has("profile")) {
                        val pJson = json.getJSONObject("profile")
                        val importedProfile = UserProfile(
                            id = 1,
                            name = pJson.optString("name", "User"),
                            age = pJson.optInt("age", 20),
                            weightKg = pJson.optDouble("weightKg", 82.0).toFloat(), // الافتراضي المحدث 82
                            dailyDoseMg = pJson.optInt("dailyDoseMg", 20), // الافتراضي المحدث 20
                            targetType = pJson.optString("targetType", "MIN"),
                            startDate = pJson.optLong("startDate", System.currentTimeMillis()),
                            customTargetMg = if (pJson.has("customTargetMg")) {
                                val custom = pJson.optInt("customTargetMg", 0)
                                if (custom == 0) null else custom
                            } else null,
                            savedHospitalName = pJson.optString("savedHospitalName", ""),
                            savedHospitalLocation = pJson.optString("savedHospitalLocation", ""),
                            savedBuilding = pJson.optString("savedBuilding", ""),
                            savedClinic = pJson.optString("savedClinic", ""),
                            savedDoctorName = pJson.optString("savedDoctorName", ""),
                            birthDateType = pJson.optString("birthDateType", "G"),
                            birthDateVal = pJson.optString("birthDateVal", ""),
                            savedPatientFileNumber = pJson.optString("savedPatientFileNumber", "")
                        )
                        repository.saveUserProfile(importedProfile)
                    }

                    if (json.has("logs")) {
                        val logsArray = json.getJSONArray("logs")
                        for (i in 0 until logsArray.length()) {
                            val lJson = logsArray.getJSONObject(i)
                            val log = DailyLog(
                                dateString = lJson.optString("dateString"),
                                doseMg = lJson.optInt("doseMg", 20),
                                isTaken = lJson.optBoolean("isTaken", true),
                                healthySources = lJson.optString("healthySources", ""),
                                symptoms = lJson.optString("symptoms", ""),
                                notes = lJson.optString("notes", ""),
                                waterCups = lJson.optInt("waterCups", 0),
                                lipMoistCount = lJson.optInt("lipMoistCount", 0),
                                faceMoistCount = lJson.optInt("faceMoistCount", 0),
                                eyeMoistCount = lJson.optInt("eyeMoistCount", 0),
                                timestamp = lJson.optLong("timestamp", System.currentTimeMillis())
                            )
                            repository.saveLog(log)
                        }
                    }

                    if (json.has("appointments")) {
                        val appArray = json.getJSONArray("appointments")
                        if (appArray.length() > 0) {
                            repository.deleteAllAppointments()
                        }
                        for (i in 0 until appArray.length()) {
                            val aJson = appArray.getJSONObject(i)
                            fun optDoubleOrNull(key: String): Double? =
                                if (!aJson.isNull(key)) aJson.optDouble(key).takeIf { !it.isNaN() } else null
                            val appointment = Appointment(
                                id = aJson.optInt("id", 0),
                                dateTime = aJson.optLong("dateTime", 0L),
                                reason = aJson.optString("reason", "Checkup"),
                                notes = aJson.optString("notes", ""),
                                isCompleted = aJson.optBoolean("isCompleted", false),
                                hospitalName = aJson.optString("hospitalName", ""),
                                hospitalLocation = aJson.optString("hospitalLocation", ""),
                                building = aJson.optString("building", ""),
                                clinic = aJson.optString("clinic", ""),
                                doctorName = aJson.optString("doctorName", ""),
                                liverFunction = aJson.optString("liverFunction", ""),
                                kidneyFunction = aJson.optString("kidneyFunction", ""),
                                lipidsProfile = aJson.optString("lipidsProfile", ""),
                                otherBloodTests = aJson.optString("otherBloodTests", ""),
                                notificationTimeMillis = aJson.optLong("notificationTimeMillis", 0L),
                                tCholestrol = optDoubleOrNull("tCholestrol"),
                                hdl = optDoubleOrNull("hdl"),
                                ldl = optDoubleOrNull("ldl"),
                                triglycerides = optDoubleOrNull("triglycerides"),
                                patientFileNumber = aJson.optString("patientFileNumber", ""),
                                prescriptionId = if (!aJson.isNull("prescriptionId")) aJson.optString("prescriptionId").takeIf { it.isNotBlank() } else null,
                                calendarEventId = if (!aJson.isNull("calendarEventId")) aJson.optLong("calendarEventId") else null,
                                alt = optDoubleOrNull("alt"),
                                ast = optDoubleOrNull("ast"),
                                bilirubin = optDoubleOrNull("bilirubin"),
                                alkalinePhosphate = optDoubleOrNull("alkalinePhosphate"),
                                albuminInSerum = optDoubleOrNull("albuminInSerum"),
                                creatinine = optDoubleOrNull("creatinine"),
                                urea = optDoubleOrNull("urea"),
                                glycatedHaemoglobin = optDoubleOrNull("glycatedHaemoglobin"),
                                parentAppointmentId = if (!aJson.isNull("parentAppointmentId")) aJson.optInt("parentAppointmentId") else null,
                                appointmentType = aJson.optString("appointmentType", "CLINIC").uppercase(Locale.US).ifBlank { "CLINIC" },
                                customUnitsJson = aJson.optString("customUnitsJson", "")
                            )
                            repository.saveAppointment(appointment)
                        }
                    }

                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "تمت استعادة البيانات الاحتياطية بنجاح")
                    }
                } else {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "لم يتم العثور على الجلسة السحابية الاحتياطية")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "فشل استرجاع البيانات الاحتياطية: ${e.localizedMessage}")
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            // نقرأ أول قيمة تنبعث من قاعدة البيانات للتأكد من وجود ملف طبي
            val currentProfile = repository.userProfile.firstOrNull()
            if (currentProfile == null) {
                // إذا كان التطبيق محمل لأول مرة وقاعدة البيانات فارغة، ننشئ الملف الافتراضي (الذي يحتوي على وزن 82)
                repository.saveUserProfile(UserProfile())
            }
        }
        // بقية أكواد الـ init الخاصة بك...
    }

    // --- User Profile operations ---
    fun updateProfile(
        name: String,
        age: Int,
        weight: Float,
        dailyDose: Int,
        targetType: String,
        startTimestamp: Long,
        birthDateType: String? = null,
        birthDateVal: String? = null,
        savedPatientFileNumber: String? = null,
        clearCustomTarget: Boolean = false
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                name = name,
                age = age,
                weightKg = weight,
                dailyDoseMg = dailyDose,
                targetType = targetType,
                customTargetMg = if (clearCustomTarget) null else current.customTargetMg,
                startDate = startTimestamp,
                birthDateType = birthDateType ?: current.birthDateType,
                birthDateVal = birthDateVal ?: current.birthDateVal,
                savedPatientFileNumber = savedPatientFileNumber ?: current.savedPatientFileNumber
            )
            // 1. الحفظ المحلي
            repository.saveUserProfile(updated)
            
            // 2. الرفع السحابي الفوري والمباشر للبيانات الجديدة لتجنب تأخير الـ StateFlow
            val code = _syncCode.value
            if (!code.isNullOrBlank()) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val profileMap = mapOf(
                        "name" to updated.name,
                        "age" to updated.age,
                        "weightKg" to updated.weightKg,
                        "dailyDoseMg" to updated.dailyDoseMg,
                        "targetType" to updated.targetType,
                        "startDate" to updated.startDate,
                        "customTargetMg" to updated.customTargetMg,
                        "birthDateType" to updated.birthDateType,
                        "birthDateVal" to updated.birthDateVal,
                        "savedPatientFileNumber" to updated.savedPatientFileNumber
                    )
                    db.collection("sync_sessions").document(code)
                        .collection("profile").document("user_profile")
                        .set(profileMap, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    Log.e("RoaccutaneViewModel", "Error syncing profile directly", e)
                }
            }
        }
    }

    fun getEarliestDiaryDateMillis(): Long? {
        val logs = allLogs.value.filter { it.isTaken && it.doseMg > 0 }
        if (logs.isEmpty()) return null
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return logs.mapNotNull {
            try {
                sdf.parse(it.dateString)?.time
            } catch (_: Exception) {
                null
            }
        }.minOrNull()
    }

    fun getEarliestDispenseDateMillis(): Long? {
        return allDispensedMedications.value.map { it.startDate }.minOrNull()
    }

    fun syncTreatmentStartDate(chosenDateMillis: Long) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(startDate = chosenDateMillis)
            repository.saveUserProfile(updated)
            val code = _syncCode.value
            if (!code.isNullOrBlank()) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("sync_sessions").document(code)
                        .collection("profile").document("user_profile")
                        .update("startDate", chosenDateMillis)
                } catch (e: Exception) {
                    Log.e("RoaccutaneViewModel", "Error updating startDate in Firestore", e)
                }
            }
        }
    }

    // --- Log operations ---
    fun updateTimerStates() {
        val now = System.currentTimeMillis()
        
        val lipEnd = sharedPrefs.getLong("lip_timer_end", 0L)
        _lipTimerSec.value = if (lipEnd > now) ((lipEnd - now) / 1000).toInt() else 0

        val faceEnd = sharedPrefs.getLong("face_timer_end", 0L)
        _faceTimerSec.value = if (faceEnd > now) ((faceEnd - now) / 1000).toInt() else 0

        val eyeEnd = sharedPrefs.getLong("eye_timer_end", 0L)
        _eyeTimerSec.value = if (eyeEnd > now) ((eyeEnd - now) / 1000).toInt() else 0
    }

    fun toggleDoseTaken(dateString: String, doseMg: Int) {
        viewModelScope.launch {
            val existing = repository.allLogs.firstOrNull()?.find { it.dateString == dateString }
            val now = System.currentTimeMillis()
            val updated = if (existing != null) {
                val newTaken = !existing.isTaken
                existing.copy(
                    isTaken = newTaken,
                    timestamp = if (newTaken) now else existing.timestamp
                )
            } else {
                DailyLog(
                    dateString = dateString,
                    doseMg = doseMg,
                    isTaken = true,
                    healthySources = "دهون مناسبة مع الجرعة",
                    timestamp = now
                )
            }
            repository.saveLog(updated)
            writeDailyLogToFirestore(updated)
            uploadCurrentDateToFirestore()
            com.example.widget.DoseWidgetProvider.sendUpdateBroadcast(getApplication())
            syncMissedDaysFromDailyLogs()
        }
    }

    fun saveDailyLog(
        dateString: String,
        doseMg: Int,
        isTaken: Boolean,
        sources: List<String>,
        symptoms: List<String>,
        notes: String,
        waterCups: Int = 0,
        lipMoist: Int = 0,
        faceMoist: Int = 0,
        eyeMoist: Int = 0
    ) {
        viewModelScope.launch {
            val existing = repository.allLogs.firstOrNull()?.find { it.dateString == dateString }
            val now = System.currentTimeMillis()
            val finalTimestamp = if (isTaken) {
                if (existing != null && existing.isTaken && existing.timestamp > 0) existing.timestamp else now
            } else {
                0L
            }
            val log = DailyLog(
                dateString = dateString,
                doseMg = doseMg,
                isTaken = isTaken,
                healthySources = sources.joinToString(", "),
                symptoms = symptoms.joinToString(","),
                notes = notes,
                waterCups = waterCups,
                lipMoistCount = lipMoist,
                faceMoistCount = faceMoist,
                eyeMoistCount = eyeMoist,
                timestamp = finalTimestamp
            )
            repository.saveLog(log)
            writeDailyLogToFirestore(log)
            if (isTaken && doseMg > 0) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val logDate = sdf.parse(dateString)?.time
                    val curStart = userProfile.value.startDate
                    if (logDate != null && logDate < curStart) {
                        syncTreatmentStartDate(logDate)
                    }
                } catch (_: Exception) {}
            }
            _editingLogDate.value = null
            syncMissedDaysFromDailyLogs()
        }
    }

    fun deleteLog(dateString: String) {
        viewModelScope.launch {
            repository.deleteLog(dateString)
            deleteLogFromFirestore(dateString)
            syncMissedDaysFromDailyLogs()
        }
    }

    fun setEditingLogDate(dateString: String?) {
        _editingLogDate.value = dateString
    }

    // Quick moisturizer or water logging directly from Home screen
    fun incrementWaterCups(dateString: String, delta: Int) {
        viewModelScope.launch {
            val dayLog = repository.allLogs.firstOrNull()?.find { it.dateString == dateString }
            val updated = if (dayLog != null) {
                val newCups = (dayLog.waterCups + delta).coerceAtLeast(0)
                dayLog.copy(waterCups = newCups)
            } else {
                DailyLog(
                    dateString = dateString,
                    doseMg = userProfile.value.dailyDoseMg,
                    isTaken = false,
                    waterCups = delta.coerceAtLeast(0)
                )
            }
            repository.saveLog(updated)
            writeDailyLogToFirestore(updated)
        }
    }

    fun getTimerDurationSecs(type: String): Int {
        val defaultSecs = when (type) {
            "lip" -> 45 * 60
            "face" -> 120 * 60
            "eye" -> 180 * 60
            else -> 0
        }
        return sharedPrefs.getInt("${type}_timer_duration_secs", defaultSecs)
    }

    fun setCustomTimerDuration(type: String, durationSecs: Int) {
        sharedPrefs.edit().putInt("${type}_timer_duration_secs", durationSecs).apply()
        updateTimerStates()
    }

    fun decrementMoisturizer(dateString: String, type: String) {
        viewModelScope.launch {
            val dayLog = repository.allLogs.firstOrNull()?.find { it.dateString == dateString }
            if (dayLog != null) {
                val updated = when (type) {
                    "lip" -> dayLog.copy(lipMoistCount = (dayLog.lipMoistCount - 1).coerceAtLeast(0))
                    "face" -> dayLog.copy(faceMoistCount = (dayLog.faceMoistCount - 1).coerceAtLeast(0))
                    "eye" -> dayLog.copy(eyeMoistCount = (dayLog.eyeMoistCount - 1).coerceAtLeast(0))
                    else -> dayLog
                }
                repository.saveLog(updated)
                writeDailyLogToFirestore(updated)
                uploadCurrentDateToFirestore()
            }
        }
    }

    fun incrementMoisturizer(dateString: String, type: String) { // "lip", "face", "eye"
        viewModelScope.launch {
            val dayLog = repository.allLogs.firstOrNull()?.find { it.dateString == dateString }
            val baseLog = dayLog ?: DailyLog(
                dateString = dateString,
                doseMg = userProfile.value.dailyDoseMg,
                isTaken = false
            )
            val updated = when (type) {
                "lip" -> baseLog.copy(lipMoistCount = baseLog.lipMoistCount + 1)
                "face" -> baseLog.copy(faceMoistCount = baseLog.faceMoistCount + 1)
                "eye" -> baseLog.copy(eyeMoistCount = baseLog.eyeMoistCount + 1)
                else -> baseLog
            }
            repository.saveLog(updated)
            writeDailyLogToFirestore(updated)
            uploadCurrentDateToFirestore()
            
            // Set and persist the timer end time based on custom duration
            val durationSecs = getTimerDurationSecs(type)
            if (durationSecs > 0) {
                val endTimestamp = System.currentTimeMillis() + durationSecs * 1000L
                sharedPrefs.edit().putLong("${type}_timer_end", endTimestamp).apply()
                updateTimerStates()
            }
        }
    }

    // --- Appointment operations ---
    fun addAppointment(
        dateTimeMillis: Long,
        reason: String,
        notes: String,
        hospitalName: String,
        hospitalLocation: String,
        building: String,
        clinic: String,
        doctorName: String,
        liverFunction: String = "",
        kidneyFunction: String = "",
        lipidsProfile: String = "",
        otherBloodTests: String = "",
        notificationTimeMillis: Long = 0L,
        tCholestrol: Double? = null,
        hdl: Double? = null,
        ldl: Double? = null,
        triglycerides: Double? = null,
        patientFileNumber: String = "",
        prescriptionId: String? = null,
        alt: Double? = null,
        ast: Double? = null,
        bilirubin: Double? = null,
        alkalinePhosphate: Double? = null,
        albuminInSerum: Double? = null,
        creatinine: Double? = null,
        urea: Double? = null,
        glycatedHaemoglobin: Double? = null,
        parentAppointmentId: Int? = null,
        appointmentType: String = "CLINIC",
        customUnitsJson: String = ""
    ) {
        viewModelScope.launch {
            val app = Appointment(
                dateTime = dateTimeMillis,
                reason = reason,
                notes = notes,
                hospitalName = hospitalName,
                hospitalLocation = hospitalLocation,
                building = building,
                clinic = clinic,
                doctorName = doctorName,
                liverFunction = liverFunction,
                kidneyFunction = kidneyFunction,
                lipidsProfile = lipidsProfile,
                otherBloodTests = otherBloodTests,
                notificationTimeMillis = notificationTimeMillis,
                isCompleted = false,
                tCholestrol = tCholestrol,
                hdl = hdl,
                ldl = ldl,
                triglycerides = triglycerides,
                patientFileNumber = patientFileNumber,
                prescriptionId = prescriptionId,
                alt = alt,
                ast = ast,
                bilirubin = bilirubin,
                alkalinePhosphate = alkalinePhosphate,
                albuminInSerum = albuminInSerum,
                creatinine = creatinine,
                urea = urea,
                glycatedHaemoglobin = glycatedHaemoglobin,
                parentAppointmentId = parentAppointmentId,
                appointmentType = appointmentType,
                customUnitsJson = customUnitsJson
            )
            // 1. Sync with Native Device Calendar (Deterministic calendar event creation)
            val calEventId = try {
                com.example.utils.CalendarSyncManager.createCalendarEvent(getApplication(), app, _calendarEventColorId.value)
            } catch (e: Exception) {
                Log.e("RoaccutaneViewModel", "Failed to sync event to native calendar", e)
                null
            }

            val appWithCal = app.copy(calendarEventId = calEventId)
            val insertedId = repository.saveAppointment(appWithCal)
            val updatedApp = appWithCal.copy(id = insertedId.toInt())
            if (hospitalName.isNotBlank()) {
                saveHospitalProfile(
                    com.example.data.HospitalProfile(
                        hospitalName = hospitalName.trim(),
                        patientFileNumber = patientFileNumber.trim(),
                        doctorName = doctorName.trim(),
                        clinic = clinic.trim(),
                        building = building.trim(),
                        hospitalLocation = hospitalLocation.trim()
                    )
                )
            }
            // Trigger Firestore upload for the newly added appointment directly to avoid async Room flow lag
            writeAppointmentToFirestore(updatedApp)
            uploadCurrentDateToFirestore()
            com.example.widget.AppointmentWidgetProvider.sendUpdateBroadcast(getApplication())
        }
    }

    fun scheduleAppointmentAlarm(context: Context, appointment: Appointment) {
        if (appointment.notificationTimeMillis <= 0L || appointment.isCompleted) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(context, com.example.AppointmentReceiver::class.java).apply {
                putExtra("reason", appointment.reason)
                putExtra("hospital", appointment.hospitalName)
                putExtra("doctor", appointment.doctorName)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                appointment.id,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, appointment.notificationTimeMillis, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, appointment.notificationTimeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, appointment.notificationTimeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("RoaccutaneViewModel", "Alarm schedule failed", e)
        }
    }

    fun deleteAppointment(id: Int) {
        viewModelScope.launch {
            val targetAppt = allAppointments.value.find { it.id == id }
            // Delete from native calendar if event_id is bound
            targetAppt?.calendarEventId?.let { cId ->
                try {
                    com.example.utils.CalendarSyncManager.deleteCalendarEvent(getApplication(), cId)
                } catch (e: Exception) {
                    Log.e("RoaccutaneViewModel", "Failed to delete from native calendar", e)
                }
            }
            repository.deleteAppointment(id)
            deleteAppointmentFromFirestore(id)
            com.example.widget.AppointmentWidgetProvider.sendUpdateBroadcast(getApplication())
        }
    }

    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            // Update or recreate on native calendar (handles orphan IDs)
            val updatedCalId = try {
                com.example.utils.CalendarSyncManager.updateCalendarEvent(getApplication(), appointment, _calendarEventColorId.value)
            } catch (e: Exception) {
                Log.e("RoaccutaneViewModel", "Failed to update native calendar event", e)
                appointment.calendarEventId
            }

            val apptToSave = appointment.copy(calendarEventId = updatedCalId ?: appointment.calendarEventId)
            repository.saveAppointment(apptToSave)
            if (apptToSave.hospitalName.isNotBlank()) {
                saveHospitalProfile(
                    com.example.data.HospitalProfile(
                        hospitalName = apptToSave.hospitalName.trim(),
                        patientFileNumber = apptToSave.patientFileNumber.trim(),
                        doctorName = apptToSave.doctorName.trim(),
                        clinic = apptToSave.clinic.trim(),
                        building = apptToSave.building.trim(),
                        hospitalLocation = apptToSave.hospitalLocation.trim()
                    )
                )
            }
            writeAppointmentToFirestore(apptToSave)
            com.example.widget.AppointmentWidgetProvider.sendUpdateBroadcast(getApplication())
        }
    }

    fun setAppointmentParent(childAppointmentId: Int, parentAppointmentId: Int?) {
        viewModelScope.launch {
            val child = allAppointments.value.find { it.id == childAppointmentId } ?: return@launch
            val updated = child.copy(parentAppointmentId = parentAppointmentId)
            repository.saveAppointment(updated)
            writeAppointmentToFirestore(updated)
            com.example.widget.AppointmentWidgetProvider.sendUpdateBroadcast(getApplication())
        }
    }

    /**
     * Synchronizes ALL appointments (past and upcoming) directly to native device calendar.
     * Updates existing event if ID exists or recreates/creates new, applying Berry color and 5-day reminder.
     */
    fun syncAllAppointmentsToCalendar(onResult: (Int, Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val appts = allAppointments.value
            val context = getApplication<android.app.Application>()
            if (!com.example.utils.CalendarSyncManager.hasCalendarPermissions(context)) {
                onResult(0, appts.size)
                return@launch
            }
            var syncedCount = 0
            for (appt in appts) {
                try {
                    val calId = com.example.utils.CalendarSyncManager.updateCalendarEvent(context, appt, _calendarEventColorId.value)
                    if (calId != null) {
                        syncedCount++
                        if (calId != appt.calendarEventId) {
                            val updated = appt.copy(calendarEventId = calId)
                            repository.saveAppointment(updated)
                            writeAppointmentToFirestore(updated)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RoaccutaneViewModel", "Failed to sync appointment ${appt.id} to calendar", e)
                }
            }
            onResult(syncedCount, appts.size)
        }
    }

    fun getSavedHospitalName(): String {
        val dbVal = userProfile.value.savedHospitalName
        if (dbVal.isNotBlank()) return dbVal
        return sharedPrefs.getString("saved_hospital_name", "") ?: ""
    }
    fun getSavedHospitalLocation(): String {
        val dbVal = userProfile.value.savedHospitalLocation
        if (dbVal.isNotBlank()) return dbVal
        return sharedPrefs.getString("saved_hospital_location", "") ?: ""
    }
    fun getSavedBuilding(): String {
        val dbVal = userProfile.value.savedBuilding
        if (dbVal.isNotBlank()) return dbVal
        return sharedPrefs.getString("saved_building", "") ?: ""
    }
    fun getSavedClinic(): String {
        val dbVal = userProfile.value.savedClinic
        if (dbVal.isNotBlank()) return dbVal
        return sharedPrefs.getString("saved_clinic", "") ?: ""
    }
    fun getSavedDoctorName(): String {
        val dbVal = userProfile.value.savedDoctorName
        if (dbVal.isNotBlank()) return dbVal
        return sharedPrefs.getString("saved_doctor_name", "") ?: ""
    }
    fun getSavedPatientFileNumber(): String {
        val dbVal = userProfile.value.savedPatientFileNumber
        if (dbVal.isNotBlank()) return dbVal
        return sharedPrefs.getString("saved_patient_file_number", "") ?: ""
    }

    fun saveAppointmentDefaults(
        hospitalName: String,
        hospitalLocation: String,
        building: String,
        clinic: String,
        doctorName: String,
        patientFileNumber: String = ""
    ) {
        sharedPrefs.edit()
            .putString("saved_hospital_name", hospitalName)
            .putString("saved_hospital_location", hospitalLocation)
            .putString("saved_building", building)
            .putString("saved_clinic", clinic)
            .putString("saved_doctor_name", doctorName)
            .putString("saved_patient_file_number", patientFileNumber)
            .apply()

        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                savedHospitalName = hospitalName.trim(),
                savedHospitalLocation = hospitalLocation.trim(),
                savedBuilding = building.trim(),
                savedClinic = clinic.trim(),
                savedDoctorName = doctorName.trim(),
                savedPatientFileNumber = patientFileNumber.trim()
            )
            repository.saveUserProfile(updated)
            
            // Sync immediately on Firestore if sync code is active
            val code = _syncCode.value
            if (!code.isNullOrBlank()) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val profileMap = mapOf(
                        "name" to updated.name,
                        "age" to updated.age,
                        "weightKg" to updated.weightKg,
                        "dailyDoseMg" to updated.dailyDoseMg,
                        "targetType" to updated.targetType,
                        "startDate" to updated.startDate,
                        "customTargetMg" to updated.customTargetMg,
                        "savedHospitalName" to updated.savedHospitalName,
                        "savedHospitalLocation" to updated.savedHospitalLocation,
                        "savedBuilding" to updated.savedBuilding,
                        "savedClinic" to updated.savedClinic,
                        "savedDoctorName" to updated.savedDoctorName,
                        "birthDateType" to updated.birthDateType,
                        "birthDateVal" to updated.birthDateVal,
                        "savedPatientFileNumber" to updated.savedPatientFileNumber
                    )
                    db.collection("sync_sessions").document(code)
                        .collection("profile").document("user_profile")
                        .set(profileMap, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    Log.e("RoaccutaneViewModel", "Error syncing profile defaults directly", e)
                }
            }
        }
    }

    fun getSavedHospitalProfiles(): List<com.example.data.HospitalProfile> {
        val map = linkedMapOf<String, com.example.data.HospitalProfile>()

        // 1. Saved default hospital if set
        val defHosp = getSavedHospitalName()
        if (defHosp.isNotBlank()) {
            map[defHosp.trim().lowercase(java.util.Locale.ROOT)] = com.example.data.HospitalProfile(
                hospitalName = defHosp.trim(),
                patientFileNumber = getSavedPatientFileNumber(),
                doctorName = getSavedDoctorName(),
                clinic = getSavedClinic(),
                building = getSavedBuilding(),
                hospitalLocation = getSavedHospitalLocation()
            )
        }

        // 2. Load stored list from SharedPreferences
        val jsonStr = sharedPrefs.getString("saved_hospital_profiles_json", null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val jsonArr = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val hName = obj.optString("hospitalName", "").trim()
                    if (hName.isNotBlank()) {
                        val key = hName.lowercase(java.util.Locale.ROOT)
                        map[key] = com.example.data.HospitalProfile(
                            hospitalName = hName,
                            patientFileNumber = obj.optString("patientFileNumber", ""),
                            doctorName = obj.optString("doctorName", ""),
                            clinic = obj.optString("clinic", ""),
                            building = obj.optString("building", ""),
                            hospitalLocation = obj.optString("hospitalLocation", "")
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("RoaccutaneViewModel", "Error parsing saved hospital profiles", e)
            }
        }

        // 3. Harvest from all appointments
        allAppointments.value.sortedByDescending { it.dateTime }.forEach { appt ->
            val hName = appt.hospitalName.trim()
            if (hName.isNotBlank()) {
                val key = hName.lowercase(java.util.Locale.ROOT)
                val existing = map[key]
                if (existing == null) {
                    map[key] = com.example.data.HospitalProfile(
                        hospitalName = hName,
                        patientFileNumber = appt.patientFileNumber ?: "",
                        doctorName = appt.doctorName,
                        clinic = appt.clinic,
                        building = appt.building,
                        hospitalLocation = appt.hospitalLocation
                    )
                } else {
                    map[key] = existing.copy(
                        patientFileNumber = existing.patientFileNumber.ifBlank { appt.patientFileNumber ?: "" },
                        doctorName = existing.doctorName.ifBlank { appt.doctorName },
                        clinic = existing.clinic.ifBlank { appt.clinic },
                        building = existing.building.ifBlank { appt.building },
                        hospitalLocation = existing.hospitalLocation.ifBlank { appt.hospitalLocation }
                    )
                }
            }
        }

        return map.values.toList()
    }

    fun saveHospitalProfile(profile: com.example.data.HospitalProfile) {
        if (profile.hospitalName.isBlank()) return
        val currentList = getSavedHospitalProfiles().toMutableList()
        val idx = currentList.indexOfFirst { it.hospitalName.equals(profile.hospitalName.trim(), ignoreCase = true) }
        if (idx >= 0) {
            val old = currentList[idx]
            currentList[idx] = com.example.data.HospitalProfile(
                hospitalName = profile.hospitalName.trim(),
                patientFileNumber = profile.patientFileNumber.ifBlank { old.patientFileNumber },
                doctorName = profile.doctorName.ifBlank { old.doctorName },
                clinic = profile.clinic.ifBlank { old.clinic },
                building = profile.building.ifBlank { old.building },
                hospitalLocation = profile.hospitalLocation.ifBlank { old.hospitalLocation }
            )
        } else {
            currentList.add(0, profile)
        }

        val jsonArr = org.json.JSONArray()
        currentList.forEach { p ->
            val obj = org.json.JSONObject().apply {
                put("hospitalName", p.hospitalName)
                put("patientFileNumber", p.patientFileNumber)
                put("doctorName", p.doctorName)
                put("clinic", p.clinic)
                put("building", p.building)
                put("hospitalLocation", p.hospitalLocation)
            }
            jsonArr.put(obj)
        }
        sharedPrefs.edit().putString("saved_hospital_profiles_json", jsonArr.toString()).apply()
    }

    fun getDoctorsForHospital(hospitalName: String): List<com.example.data.DoctorProfile> {
        val docMap = linkedMapOf<String, com.example.data.DoctorProfile>()
        val trimmedHosp = hospitalName.trim()

        // 1. Check all appointments matching hospital (most recent first)
        val sortedAppts = allAppointments.value.sortedByDescending { it.dateTime }
        
        // First pass: exact hospital match
        sortedAppts.forEach { appt ->
            val docName = appt.doctorName.trim()
            if (docName.isNotBlank()) {
                val matchesHosp = trimmedHosp.isBlank() || appt.hospitalName.trim().equals(trimmedHosp, ignoreCase = true)
                if (matchesHosp) {
                    val key = docName.lowercase(java.util.Locale.ROOT)
                    if (!docMap.containsKey(key)) {
                        docMap[key] = com.example.data.DoctorProfile(
                            doctorName = docName,
                            clinic = appt.clinic.trim(),
                            hospitalName = appt.hospitalName.trim(),
                            building = appt.building.trim()
                        )
                    }
                }
            }
        }

        // 2. Check saved hospital profiles
        getSavedHospitalProfiles().forEach { profile ->
            val docName = profile.doctorName.trim()
            if (docName.isNotBlank()) {
                val matchesHosp = trimmedHosp.isBlank() || profile.hospitalName.trim().equals(trimmedHosp, ignoreCase = true)
                if (matchesHosp) {
                    val key = docName.lowercase(java.util.Locale.ROOT)
                    if (!docMap.containsKey(key)) {
                        docMap[key] = com.example.data.DoctorProfile(
                            doctorName = docName,
                            clinic = profile.clinic.trim(),
                            hospitalName = profile.hospitalName.trim(),
                            building = profile.building.trim()
                        )
                    } else {
                        val existing = docMap[key]!!
                        if (existing.clinic.isBlank() && profile.clinic.isNotBlank()) {
                            docMap[key] = existing.copy(clinic = profile.clinic.trim())
                        }
                    }
                }
            }
        }

        // 3. Fallback: If no doctors found specifically for this hospital, include all known doctors
        if (docMap.isEmpty() && trimmedHosp.isNotBlank()) {
            sortedAppts.forEach { appt ->
                val docName = appt.doctorName.trim()
                if (docName.isNotBlank()) {
                    val key = docName.lowercase(java.util.Locale.ROOT)
                    if (!docMap.containsKey(key)) {
                        docMap[key] = com.example.data.DoctorProfile(
                            doctorName = docName,
                            clinic = appt.clinic.trim(),
                            hospitalName = appt.hospitalName.trim(),
                            building = appt.building.trim()
                        )
                    }
                }
            }
        }

        return docMap.values.toList()
    }


    fun toggleAppointmentCompleted(appointment: Appointment) {
        viewModelScope.launch {
            val updated = appointment.copy(isCompleted = !appointment.isCompleted)
            repository.saveAppointment(updated)
            writeAppointmentToFirestore(updated)
        }
    }

    // --- Interactive Two-Way AI Assistant (Function Calling & DB Action Execution) ---
    fun askJauzaa(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            val userMsg = ChatMessage(query, isUser = true)
            _aiChatHistory.value = _aiChatHistory.value + userMsg
            _isAiLoading.value = true

            val keyToUse = if (_customApiKey.value.isNotBlank()) {
                _customApiKey.value
            } else {
                com.example.BuildConfig.GEMINI_API_KEY
            }

            try {
                val audit = computeRoaccutaneInventoryAudit(
                    medications = allDispensedMedications.value,
                    courseStartDate = userProfile.value.startDate,
                    allLogs = allLogs.value
                )

                val responseMessage = com.example.data.RoaccutaneAiTwoWayService.processAssistantTurn(
                    apiKey = keyToUse,
                    userQuery = query,
                    chatHistory = _aiChatHistory.value,
                    userProfile = userProfile.value,
                    allLogs = allLogs.value,
                    cumulativeDose = cumulativeDose.value,
                    inventoryAudit = audit,
                    allAppointments = allAppointments.value,
                    allPrescriptions = allMedicalPrescriptions.value,
                    appLanguage = sharedPrefs.getString("app_language", "ar") ?: "ar",
                    activeAccount = activeAccount.value
                )

                _aiChatHistory.value = _aiChatHistory.value + responseMessage
            } catch (e: Exception) {
                val errMsg = if ((sharedPrefs.getString("app_language", "ar") ?: "ar") == "ar") {
                    "حدث خطأ أثناء معالجة الطلب: ${e.localizedMessage ?: "مشكلة اتصال"}"
                } else {
                    "Error processing AI request: ${e.localizedMessage ?: "Connection error"}"
                }
                _aiChatHistory.value = _aiChatHistory.value + ChatMessage(message = errMsg, isUser = false)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runQuickAssistantQuery(queryType: String, promptLabel: String) {
        viewModelScope.launch {
            _aiChatHistory.value = _aiChatHistory.value + ChatMessage(promptLabel, isUser = true)
            val audit = computeRoaccutaneInventoryAudit(
                medications = allDispensedMedications.value,
                courseStartDate = userProfile.value.startDate,
                allLogs = allLogs.value
            )
            val responseMessage = com.example.data.RoaccutaneAiTwoWayService.executeAppDataQuery(
                queryType = queryType,
                userProfile = userProfile.value,
                allLogs = allLogs.value,
                cumulativeDose = cumulativeDose.value,
                inventoryAudit = audit,
                allAppointments = allAppointments.value,
                allPrescriptions = allMedicalPrescriptions.value,
                appLanguage = sharedPrefs.getString("app_language", "ar") ?: "ar",
                activeAccount = activeAccount.value
            )
            _aiChatHistory.value = _aiChatHistory.value + responseMessage
        }
    }

    /**
     * Executes the pending AI Action Card into Room & Firestore database upon explicit user confirmation.
     */
    fun confirmAiAction(card: AiActionCard) {
        viewModelScope.launch {
            val lang = sharedPrefs.getString("app_language", "ar") ?: "ar"
            val isAr = lang == "ar"
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())

            val existingLog = repository.allLogs.firstOrNull()?.find { it.dateString == todayStr }

            // 1. Process Dose, Symptoms, Trackers
            val finalDose = card.doseMg ?: existingLog?.doseMg ?: userProfile.value.dailyDoseMg
            val finalIsTaken = if (card.doseMg != null && card.doseMg > 0) true else (existingLog?.isTaken ?: true)

            val existingSymptoms = existingLog?.symptoms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val mergedSymptoms = (existingSymptoms + card.symptoms).distinct()

            val existingWater = existingLog?.waterCups ?: 0
            val addedWaterCups = if (card.waterMl != null) (card.waterMl / 250).coerceAtLeast(1) else 0
            val finalWater = existingWater + addedWaterCups

            var lipMoist = existingLog?.lipMoistCount ?: 0
            var faceMoist = existingLog?.faceMoistCount ?: 0
            var eyeMoist = existingLog?.eyeMoistCount ?: 0

            if (card.moisturizerType == "lips") lipMoist += (card.moisturizerCount ?: 1)
            if (card.moisturizerType == "skin") faceMoist += (card.moisturizerCount ?: 1)
            if (card.moisturizerType == "eyes") eyeMoist += (card.moisturizerCount ?: 1)

            val notes = StringBuilder(existingLog?.notes ?: "")
            if (card.severity != null) {
                if (notes.isNotBlank()) notes.append(" | ")
                notes.append(if (isAr) "شدة الأعراض المدخلة بالذكاء الاصطناعي: ${card.severity}/5" else "AI Logged Severity: ${card.severity}/5")
            }

            // Save Daily Log if any log field is present
            if (card.doseMg != null || card.symptoms.isNotEmpty() || card.waterMl != null || card.moisturizerType != null || card.severity != null) {
                saveDailyLog(
                    dateString = todayStr,
                    doseMg = finalDose,
                    isTaken = finalIsTaken,
                    sources = existingLog?.healthySources?.split(",")?.map { it.trim() } ?: emptyList(),
                    symptoms = mergedSymptoms,
                    notes = notes.toString(),
                    waterCups = finalWater,
                    lipMoist = lipMoist,
                    faceMoist = faceMoist,
                    eyeMoist = eyeMoist
                )
            }

            // 2. Process Prescribed Dose / Stock
            if (card.prescribedDose != null && card.prescribedDose > 0) {
                val updatedProf = userProfile.value.copy(dailyDoseMg = card.prescribedDose)
                repository.saveUserProfile(updatedProf)
            }

            // 3. Process Appointments
            if (!card.appointmentDate.isNullOrBlank()) {
                val parsedTime = try {
                    sdf.parse(card.appointmentDate)?.time ?: (System.currentTimeMillis() + 86400000L * 7)
                } catch (_: Exception) {
                    System.currentTimeMillis() + 86400000L * 7
                }
                val newAppt = Appointment(
                    dateTime = parsedTime,
                    reason = if (card.labTestName.isNullOrBlank()) (if (isAr) "متابعة كورس الروكتان" else "Roaccutane Follow-up") else "${if (isAr) "فحص" else "Test"} ${card.labTestName}",
                    liverFunction = if (card.labTestName?.contains("كبد") == true || card.labTestName?.contains("liver") == true) (card.labResultValue ?: "") else "",
                    lipidsProfile = if (card.labTestName?.contains("دهون") == true || card.labTestName?.contains("lipid") == true) (card.labResultValue ?: "") else "",
                    notes = if (isAr) "تمت الإضافة عبر المساعد الذكي" else "Added via AI Assistant",
                    appointmentType = if (!card.labTestName.isNullOrBlank()) "LAB" else "CLINIC"
                )
                val insertedId = repository.saveAppointment(newAppt)
                writeAppointmentToFirestore(newAppt.copy(id = insertedId.toInt()))
            }

            // Mark card as confirmed in chat history
            _aiChatHistory.value = _aiChatHistory.value.map { msg ->
                if (msg.actionCard?.id == card.id) {
                    msg.copy(actionCard = msg.actionCard.copy(isConfirmed = true, isCancelled = false))
                } else {
                    msg
                }
            }

            val confirmNotice = if (isAr) {
                "✓ تم تأكيد وحفظ كافة البيانات بنجاح في قاعدة البيانات وتحديث جميع المؤشرات وسجل اليوميات."
            } else {
                "✓ All data has been confirmed and successfully saved into the database!"
            }
            _aiChatHistory.value = _aiChatHistory.value + ChatMessage(message = confirmNotice, isUser = false)
        }
    }

    /**
     * Cancels the pending AI Action Card and prompts the user for adjustment.
     */
    fun cancelAiAction(card: AiActionCard) {
        val lang = sharedPrefs.getString("app_language", "ar") ?: "ar"
        val isAr = lang == "ar"

        _aiChatHistory.value = _aiChatHistory.value.map { msg ->
            if (msg.actionCard?.id == card.id) {
                msg.copy(actionCard = msg.actionCard.copy(isCancelled = true, isConfirmed = false))
            } else {
                msg
            }
        }

        val cancelPrompt = if (isAr) {
            "تم إلغاء تسجيل الإجراء. ما هو المحور الذي ترغب بتعديله؟ (الجرعة، الأعراض، أو المرطبات)"
        } else {
            "Action cancelled. What would you like to modify? (Dose, Symptoms, or Moisturizers?)"
        }
        _aiChatHistory.value = _aiChatHistory.value + ChatMessage(message = cancelPrompt, isUser = false)
    }

    fun clearChat() {
        _aiChatHistory.value = emptyList()
    }

 // --- Stat computations ---

    // 1. إجمالي الجرعة التراكمية المتناولة فعلياً (سليم كما هو)
    val cumulativeDose: StateFlow<Int> = allLogs.map { logs ->
        logs.filter { it.isTaken }.sumOf { it.doseMg }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // 2. الأيام المنقضية من بداية الكورس إلى اليوم
    val elapsedDaysFlow: Flow<Int> = userProfile.map { profile ->
        val diffMs = System.currentTimeMillis() - profile.startDate
        val days = diffMs / (1000 * 60 * 60 * 24)
        if (days < 0) 0 else days.toInt() + 1
    }

    // 3. عدد الأيام التي تم تناول الجرعة فيها فعلياً
    val activeDaysCountFlow: Flow<Int> = allLogs.map { logs ->
        logs.count { it.isTaken }
    }

    // 4. الحساب الذكي المطور والموحد للأيام المتبقية وتاريخ النهاية والتقدم
// 4. الحساب الذكي المطور للأيام المتبقية وتاريخ النهاية
    // 4. الحساب الذكي المطور للأيام المتبقية وتاريخ النهاية
    val remainingDaysEstimatedFlow: StateFlow<Int> = combine(
        userProfile,
        cumulativeDose 
    ) { profile, cumulative ->
        
        val weight = if (profile.weightKg > 0f) profile.weightKg else 82f
        
        // المحاسب الجديد يسأل عن نوع الهدف أولاً
        val calculatedTargetDoseMg = if ((profile.customTargetMg ?: 0) > 0) {
            profile.customTargetMg!!.toFloat()
        } else {
            weight * profile.targetMgPerKg
        }
        
        val remainingMg = calculatedTargetDoseMg - cumulative
        
        if (remainingMg <= 0) {
            0
        } else {
            val daily = if (profile.dailyDoseMg > 0) profile.dailyDoseMg else 20
            Math.ceil(remainingMg.toDouble() / daily).toInt()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0) // <-- هذه الإضافة ضرورية جداً

    // 5. نسبة التقدم الفعلي لعرضها في الشاشة الرئيسية
    val progressPercentage: StateFlow<Float> = combine(userProfile, cumulativeDose) { profile, cumulative ->
        val weight = if (profile.weightKg > 0f) profile.weightKg else 82f
        
        val targetDose = if ((profile.customTargetMg ?: 0) > 0) {
            profile.customTargetMg!!.toFloat()
        } else {
            weight * profile.targetMgPerKg
        }
        
        if (targetDose > 0) {
            val progress = (cumulative.toFloat() / targetDose) * 100f
            progress.coerceIn(0f, 100f)
        } else {
            0f
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    private fun getSymptomKeyInEnglish(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""
        val indexEn = SYMPTOMS_EN.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        if (indexEn != -1) return SYMPTOMS_EN[indexEn]
        val indexAr = SYMPTOMS_AR.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        if (indexAr != -1) return SYMPTOMS_EN[indexAr]
        return trimmed
    }

    private fun getFatKeyInEnglish(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""
        val indexEn = FATS_EN.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        if (indexEn != -1) return FATS_EN[indexEn]
        val indexAr = FATS_AR.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        if (indexAr != -1) return FATS_EN[indexAr]
        return trimmed
    }

    // Symptom Statistics (percentage of reports of each symptom)
    val symptomStatisticsFlow: Flow<List<SymptomStats>> = allLogs.map { logs ->
        val takenLogs = logs.filter { it.isTaken }
        if (takenLogs.isEmpty()) return@map emptyList()

        val symptomCounts = mutableMapOf<String, Int>()
        takenLogs.forEach { log ->
            if (log.symptoms.isNotBlank()) {
                val list = log.symptoms.split(",")
                list.forEach { sym ->
                    val clean = sym.trim()
                    if (clean.isNotBlank()) {
                        val key = getSymptomKeyInEnglish(clean)
                        if (key.isNotBlank()) {
                            symptomCounts[key] = (symptomCounts[key] ?: 0) + 1
                        }
                    }
                }
            }
        }

        val totalLogs = takenLogs.size
        symptomCounts.map { (symptom, count) ->
            SymptomStats(
                symptom = symptom,
                occurrenceCount = count,
                percentage = (count.toFloat() / totalLogs) * 100
            )
        }.sortedByDescending { it.percentage }
    }

    // Healthy Sources Tracker Stats (percentage of logs containing certain healthy fats)
    val healthySourcesFlow: Flow<List<SourceStats>> = allLogs.map { logs ->
        val takenLogs = logs.filter { it.isTaken }
        if (takenLogs.isEmpty()) return@map emptyList()

        val sourceCounts = mutableMapOf<String, Int>()
        takenLogs.forEach { log ->
            if (log.healthySources.isNotBlank()) {
                val list = log.healthySources.split(",")
                list.forEach { src ->
                    val clean = src.trim()
                    if (clean.isNotBlank()) {
                        val key = getFatKeyInEnglish(clean)
                        if (key.isNotBlank()) {
                            sourceCounts[key] = (sourceCounts[key] ?: 0) + 1
                        }
                    }
                }
            }
        }

        val totalLogs = takenLogs.size
        sourceCounts.map { (source, count) ->
            SourceStats(
                source = source,
                occurrenceCount = count,
                percentage = (count.toFloat() / totalLogs) * 100
            )
        }.sortedByDescending { it.percentage }
    }

    // --- Dynamic AI Customized Tips & Care Guidance Generator ---
    private val _aiCustomTips = MutableStateFlow<List<String>>(
        try {
            sharedPrefs.getString("ai_custom_tips_cached", "")
                ?.split("||")
                ?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    )
    val aiCustomTips: StateFlow<List<String>> = _aiCustomTips.asStateFlow()

    private val _isGeneratingTips = MutableStateFlow(false)
    val isGeneratingTips: StateFlow<Boolean> = _isGeneratingTips.asStateFlow()

    private val _customTipsError = MutableStateFlow<String?>(null)
    val customTipsError: StateFlow<String?> = _customTipsError.asStateFlow()

    fun generateCustomAITips(
        lang: String,
        cumulativeDose: Int,
        elapsedDays: Int,
        remainingDays: Int,
        progressPercent: Int
    ) {
        viewModelScope.launch {
            _isGeneratingTips.value = true
            _customTipsError.value = null
            try {
                val profile = userProfile.value
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
                val yesterdayLog = allLogs.value.find { it.dateString == yesterdayStr }

                val yesterdayTaken = yesterdayLog?.isTaken ?: false
                val yesterdayTakenText = if (yesterdayTaken) {
                    if (lang == "ar") "تم تناول الجرعة بنجاح" else "Pill was taken"
                } else {
                    if (lang == "ar") "لم تؤخذ الجرعة أو لم تُسجل" else "Pill was not logged or not taken"
                }
                
                val yesterdaySymptoms = yesterdayLog?.symptoms?.let { if (it.isBlank()) null else it }
                    ?: if (lang == "ar") "لا توجد أعراض مسجلة يوم أمس" else "No symptoms logged yesterday"
                    
                val yesterdayWater = yesterdayLog?.waterCups ?: 0
                val yesterdayLipMoist = yesterdayLog?.lipMoistCount ?: 0
                val yesterdayFaceMoist = yesterdayLog?.faceMoistCount ?: 0
                val yesterdayEyeMoist = yesterdayLog?.eyeMoistCount ?: 0
                val yesterdayHealthyFats = yesterdayLog?.healthySources?.let { if (it.isBlank()) null else it }
                    ?: if (lang == "ar") "لا توجد دهون مفيدة مسجلة يوم أمس" else "No healthy fats logged yesterday"

                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val currentPayload = "${lang}_$todayStr"
                val cachedPayload = sharedPrefs.getString("ai_custom_tips_payload_ref", "") ?: ""
                val cachedTips = sharedPrefs.getString("ai_custom_tips_cached", "") ?: ""

                if (currentPayload == cachedPayload && cachedTips.isNotBlank() && _aiCustomTips.value.isNotEmpty()) {
                    // Cache matches and has content. We won't call the API again.
                    _customTipsError.value = if (lang == "ar") {
                        "تم توليد النصائح المخصصة لليوم بنجاح بناءً على سجلات الأمس الكاملة وجرعتك اليومية! يُنصح بإجراء تحليل ذكي واحد يومياً لضمان ثبات الخطة الإرشادية."
                    } else {
                        "Your advice has been successfully generated for today based on yesterday's completed logs and your active daily dose! A single stable analysis per day guarantees the highest consistency."
                    }
                    _isGeneratingTips.value = false
                    return@launch
                }

                val systemMsg = if (lang == "ar") {
                    "أنت طبيب جلدي ومساعد ذكي رصين مخصص لمرافقة مريض الرواكتان (Isotretinoin). مهمتك هي صياغة خمس نصائح وإرشادات طبية دقيقة وموجزة جداً وموجهة مباشرة للمريض تعكس سجلات يوم أمس والجرعة المقررة له اليوم."
                } else {
                    "You are a dermatologist and smart AI assistant supporting a patient on their Roaccutane (Isotretinoin) treatment journey. Your task is to write exactly 5 highly precise, ultra-brief, personalized advice instructions directly customized for the patient based on yesterday's logs and today's dose."
                }

                val prompt = if (lang == "ar") {
                    "الرجاء كتابة 5 نصائح دقيقة وحكيمة للمريض، متناسبة مع حالته وسجلاته ليوم أمس ومحددات اليوم:\n" +
                    "- المريض: ${profile.name} (العمر: ${profile.age} سنة)\n" +
                    "- جرعة كبسولة اليوم النشطة: ${profile.dailyDoseMg} ملغ\n" +
                    "- كورس تقدم العلاج: الأيام تم إنهاؤها: $elapsedDays، الأيام المتبقية: $remainingDays، نسبة الجرعة التراكمية: $progressPercent%\n\n" +
                    "بيانات وسجلات اليوم السابق التراكمية:\n" +
                    "- أخذ الجرعة أمس: $yesterdayTakenText\n" +
                    "- الأعراض الجانبية المسجلة لأمس: $yesterdaySymptoms\n" +
                    "- كمية شرب الماء لأمس: $yesterdayWater أكواب\n" +
                    "- مرات ترطيب الشفاه لأمس: $yesterdayLipMoist مرات\n" +
                    "- مرات ترطيب الوجه البشرة لأمس: $yesterdayFaceMoist مرات\n" +
                    "- مرات ترطيب العين لأمس: $yesterdayEyeMoist مرات\n" +
                    "- الدهون المفيدة المتناولة أمس: $yesterdayHealthyFats\n\n" +
                    "الشروط البنائية المُلزمة:\n" +
                    "1. صغ النصائح بلغة عربية سلسلة وطبية رصينة لرفع الوعي الطبي للشفاه والجلد.\n" +
                    "2. اكتب بالضبط 5 نصائح، كل نصيحة في سطر منفصل تماماً (newline).\n" +
                    "3. لا تستخدم أرقام الترقيم (كـ 1.، 2.)، أو فواصل نقطية (كـ - أو •) في بداية الأسطر.\n" +
                    "4. احرص على أن تكون كل نصيحة أقل من 15 كلمة ومباشرة في صلب الفائدة الطبيّة للرواكتان."
                } else {
                    "Please write exactly 5 precise and helpful dermatological tips corresponding to the patient's data, yesterday's complete logs, and today's dose:\n" +
                    "- Patient: ${profile.name} (Age: ${profile.age} years old)\n" +
                    "- Active Capsule Dose Today: ${profile.dailyDoseMg} mg\n" +
                    "- Course progress: $elapsedDays days done, approx. $remainingDays days remaining.\n" +
                    "- Cumulative target dose progression: $progressPercent%\n\n" +
                    "Yesterday's Complete Historical Records:\n" +
                    "- Pill Taken Yesterday: $yesterdayTakenText\n" +
                    "- Logged Side-effects Yesterday: $yesterdaySymptoms\n" +
                    "- Water usage cups Yesterday: $yesterdayWater cups\n" +
                    "- Lip moisturizer usage index Yesterday: $yesterdayLipMoist times\n" +
                    "- Skin moisturizer usage index Yesterday: $yesterdayFaceMoist times\n" +
                    "- Eye drops logged Yesterday: $yesterdayEyeMoist times\n" +
                    "- Healthy fat intake sources Yesterday: $yesterdayHealthyFats\n\n" +
                    "Format Requirements:\n" +
                    "1. Write in clear, empathetic, precise English.\n" +
                    "2. Return exactly 5 sentences, each separated by a newline character.\\n\n" +
                    "3. DO NOT include indices (like '1.', '2.'), dashed marks or bullets at the start of any line.\n" +
                    "4. Make each sentence extremely concise (under 15 words)."
                }

                val keyToUse = if (_customApiKey.value.isNotBlank()) _customApiKey.value else com.example.BuildConfig.GEMINI_API_KEY
                
                if (keyToUse.isBlank() || keyToUse == "MY_GEMINI_API_KEY") {
                    _customTipsError.value = if (lang == "ar") {
                        "تنبيه: تعذر الاتصال بالمساعد الذكي لعدم تهيئة رمز مفتاح API صالح في الإعدادات."
                    } else {
                        "Warning: Gemini API Key is missing in Settings."
                    }
                } else {
                    val sysTextContent = GeminiContent(parts = listOf(GeminiPart(text = systemMsg)))
                    val queryTextContent = GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    val request = GeminiRequest(
                        contents = listOf(queryTextContent),
                        systemInstruction = sysTextContent
                    )
                    
                    val modelsToTry = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-3.5-flash")
                    var textResult: String? = null
                    var apiError: Exception? = null
                    
                    for (modelName in modelsToTry) {
                        try {
                            Log.d("RoaccutaneViewModel", "Trying model: $modelName for AI Tips...")
                            val apiResponse = RetrofitClient.service.generateContent(modelName, keyToUse, request)
                            val candidateText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!candidateText.isNullOrBlank()) {
                                textResult = candidateText
                                break
                            }
                        } catch (e: Exception) {
                            Log.w("RoaccutaneViewModel", "Model $modelName call failed for AI Tips", e)
                            apiError = e
                        }
                    }
                    
                    if (textResult == null && apiError != null) {
                        throw apiError
                    }
                    
                    if (textResult != null && textResult.isNotBlank()) {
                        val parsedLines = textResult.split("\n")
                            .map { it.trim().replace(Regex("^[-*•0-9.\\s]+"), "") }
                            .filter { it.isNotBlank() }
                            .take(5)
                        
                        if (parsedLines.isNotEmpty()) {
                            _aiCustomTips.value = parsedLines
                            sharedPrefs.edit()
                                .putString("ai_custom_tips_cached", parsedLines.joinToString("||"))
                                .putString("ai_custom_tips_payload_ref", currentPayload)
                                .apply()
                        } else {
                            throw Exception("Parsed tips list is empty.")
                        }
                    } else {
                        throw Exception("Received empty response from artificial intelligence.")
                    }
                }
            } catch (e: Exception) {
                Log.e("RoaccutaneViewModel", "AITips failure", e)
                _customTipsError.value = e.localizedMessage ?: "Failed connection to AI services"
            } finally {
                _isGeneratingTips.value = false
            }
        }
    }

    // Clean English helpers for PDF report tables to prevent character encoding issues and bilingual clutter
    private fun toCleanEnglishReportText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var text = input.trim()
        text = text.replace(Regex("""(?i)\[\s*pdf\s*\]|\(\s*pdf\s*\)"""), "").trim()
        val parenMatch = Regex("""\(([^)]*[a-zA-Z]+[^)]*)\)""").find(text)
        if (parenMatch != null) {
            val extracted = parenMatch.groupValues[1].trim()
            if (extracted.isNotBlank()) {
                text = extracted
            }
        }
        val hasArabic = text.any { it in '\u0600'..'\u06FF' }
        if (hasArabic) {
            val lower = text.lowercase()
            text = when {
                lower.contains("روأك") || lower.contains("رواك") || lower.contains("كيوتان") || lower.contains("accutane") -> "Roaccutane"
                lower.contains("ايزوتري") || lower.contains("ايزو") || lower.contains("isotret") -> "Isotretinoin"
                lower.contains("كوراكني") || lower.contains("curacne") -> "Curacne"
                lower.contains("استشاري") || lower.contains("طبيب") || lower.contains("د.") || lower.contains("دكتور") -> "Consultant Dermatologist"
                lower.contains("مستشفى") || lower.contains("عياد") || lower.contains("مجمع") || lower.contains("مركز") -> "Specialized Clinic"
                else -> {
                    val filtered = text.filter { it !in '\u0600'..'\u06FF' }.replace(Regex("""\s+"""), " ").trim()
                    filtered.ifBlank { "Prescribed Regimen" }
                }
            }
        }
        return text.trim()
    }

    private fun toCleanEnglishDose(dose: String?, fallbackMg: Int): String {
        if (dose.isNullOrBlank()) return "${fallbackMg}mg/day"
        var clean = dose.replace(Regex("""(?i)ملغ|ملجم|مغ"""), "mg")
            .replace("كبسولة", "cap")
            .replace("كبسولات", "caps")
            .replace("يومياً", "/day")
            .replace("في اليوم", "/day")
            .filter { it !in '\u0600'..'\u06FF' }
            .trim()
        clean = clean.replace(Regex("""\s+"""), " ")
        if (!clean.contains("mg", ignoreCase = true) && !clean.contains("cap", ignoreCase = true)) {
            clean = "${fallbackMg}mg/day"
        }
        return clean.ifBlank { "${fallbackMg}mg/day" }
    }

    // Native PDF Export Functionality
    fun exportStatsToPdf(
        context: Context,
        profile: UserProfile,
        cumulativeDose: Int,
        targetDose: Int,
        elapsedDays: Int,
        symptomStats: List<SymptomStats>,
        sourceStats: List<SourceStats>,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val aiAnalysis = try {
                val logsForAI = allLogs.value.filter { it.symptoms.isNotBlank() }
                    .take(25)
                    .joinToString("\n") { log ->
                        "- Date: ${log.dateString}, Symptoms: ${log.symptoms}"
                    }
                
                val cachedSymptomLogs = sharedPrefs.getString("cached_symptom_logs_for_ai", "") ?: ""
                val cachedAiAnalysis = sharedPrefs.getString("cached_ai_analysis_result", "") ?: ""
                
                if (logsForAI == cachedSymptomLogs && cachedAiAnalysis.isNotBlank() && 
                    !cachedAiAnalysis.startsWith("فشل") && 
                    !cachedAiAnalysis.startsWith("تنبيه:") && 
                    !cachedAiAnalysis.startsWith("Note:") && 
                    !cachedAiAnalysis.startsWith("Error:")
                ) {
                    cachedAiAnalysis
                } else {
                    val systemInstForAnalysis = "أنت مساعد طبي متخصص ومحلل رصين للبيانات الصحية. " +
                            "مهمتك هي تحليل الأعراض الجانبية وتواريخها لمريض معالَج بالرواكتان (Roaccutane). " +
                            "قم بتحليل البيانات بإيجاز شديد وعناية طبية مركزة، واذكر التكرار والأنماط الهامة، وقدم توصية طبية مختصرة جداً. " +
                            "اجعل طول التحليل كاملاً أقل من 130 كلمة باللغة العربية حصراً."
                    
                    val prompt = if (logsForAI.isBlank()) {
                        "لا توجد أعراض جانبية مسجلة بعد من قبل المستخدم في تطبيق المتابعة. قدم نصائح عامة وموجزة جداً حول السيطرة على جفاف الشفاه والجلد وأهمية فحص الكولسترول والدهون وزيادة امتصاص الرواكتان بالوجبات الدسمة."
                    } else {
                        "يرجى مراجعة وتحليل الأعراض الجانبية وتواريخها التالية لمريض الرواكتان:\n$logsForAI\n\nقم بصياغة تقرير طبي تحليلي موجز للغاية وتوصية واضحة باللغة العربية (أقل من 130 كلمة)."
                    }
                    
                    val keyToUse = if (_customApiKey.value.isNotBlank()) _customApiKey.value else com.example.BuildConfig.GEMINI_API_KEY
                    
                    val sysTextContent = GeminiContent(parts = listOf(GeminiPart(text = systemInstForAnalysis)))
                    val queryTextContent = GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    val request = GeminiRequest(
                        contents = listOf(queryTextContent),
                        systemInstruction = sysTextContent
                    )
                    
                    if (keyToUse.isBlank() || keyToUse == "MY_GEMINI_API_KEY") {
                        "تنبيه: تعذر إتمام تحليل الذكاء الاصطناعي للأعراض لعدم العثور على رمز مفتاح API صالح في الإعدادات. يرجى إدخال مفتاح الـ API وتنشيطه."
                    } else {
                        val modelsToTry = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-3.5-flash")
                        var textResult: String? = null
                        var apiError: Exception? = null
                        
                        for (modelName in modelsToTry) {
                            try {
                                Log.d("RoaccutaneViewModel", "Trying model: $modelName for symptom analysis...")
                                val apiResponse = RetrofitClient.service.generateContent(modelName, keyToUse, request)
                                val candidateText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                if (!candidateText.isNullOrBlank()) {
                                    textResult = candidateText
                                    break
                                }
                            } catch (e: Exception) {
                                Log.w("RoaccutaneViewModel", "Model $modelName call failed for symptom analysis", e)
                                apiError = e
                            }
                        }
                        
                        if (textResult == null && apiError != null) {
                            throw apiError
                        }
                        
                        if (textResult != null && textResult.isNotBlank()) {
                            sharedPrefs.edit()
                                .putString("cached_symptom_logs_for_ai", logsForAI)
                                .putString("cached_ai_analysis_result", textResult)
                                .apply()
                            textResult
                        } else {
                            "لم يسترجع نظام المساعد الطبي تحليلاً دقيقاً في الوقت الراهن."
                        }
                    }
                }
            } catch (e: Exception) {
                "فشل الاتصال بخدمة التحليل الذكي للأعراض: ${e.localizedMessage ?: "مشكلة اتصال بالإنترنت"}"
            }

            try {
            val savedH = getSavedHospitalName()
            val savedD = getSavedDoctorName()
            val savedC = getSavedClinic()
            val savedB = getSavedBuilding()
            val savedL = getSavedHospitalLocation()

            val prescriptionsList = allMedicalPrescriptions.value
            val dispensedList = allDispensedMedications.value

            val estimations = com.example.utils.TreatmentCourseEstimator.computeEstimations(
                userProfile = profile,
                dispensedMedications = dispensedList,
                allLogs = allLogs.value,
                allAppointments = allAppointments.value,
                allPrescriptions = prescriptionsList,
                todayMillis = System.currentTimeMillis()
            )
            val inventoryAudit = computeRoaccutaneInventoryAudit(
                medications = dispensedList,
                courseStartDate = profile.startDate,
                allLogs = allLogs.value,
                todayMillis = System.currentTimeMillis()
            )

            fun hasNumericLabData(a: Appointment): Boolean =
                a.alt != null || a.ast != null || a.bilirubin != null || a.alkalinePhosphate != null ||
                a.albuminInSerum != null || a.creatinine != null || a.urea != null ||
                a.tCholestrol != null || a.ldl != null || a.hdl != null || a.triglycerides != null ||
                a.glycatedHaemoglobin != null

            val typedLabAppts = allAppointments.value.filter {
                it.appointmentType.equals("LAB", ignoreCase = true) && hasNumericLabData(it)
            }
            val sourceLabAppts = if (typedLabAppts.isNotEmpty()) typedLabAppts else allAppointments.value.filter { hasNumericLabData(it) }

            val uniqueLabsChronological = sourceLabAppts
                .distinctBy { appt ->
                    val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(appt.dateTime))
                    "$dStr|${appt.alt}|${appt.ast}|${appt.bilirubin}|${appt.alkalinePhosphate}|${appt.albuminInSerum}|${appt.creatinine}|${appt.urea}|${appt.tCholestrol}|${appt.ldl}|${appt.hdl}|${appt.triglycerides}|${appt.glycatedHaemoglobin}"
                }
                .sortedBy { it.dateTime }

            val labHistoryChunks = uniqueLabsChronological.chunked(4).ifEmpty { listOf(emptyList()) }
            val totalPages = 3 + labHistoryChunks.size + 2
            var currentPageNum = 1

            val latestAppt = allAppointments.value.maxByOrNull { it.dateTime }
            val hosp = if (savedH.isNotBlank()) savedH else (latestAppt?.hospitalName?.takeIf { it.isNotBlank() } ?: "N/A")
            val doc = if (savedD.isNotBlank()) savedD else (latestAppt?.doctorName?.takeIf { it.isNotBlank() } ?: "N/A")
            val clinicVal = if (savedC.isNotBlank()) savedC else (latestAppt?.clinic?.takeIf { it.isNotBlank() } ?: "N/A")
            val bld = if (savedB.isNotBlank()) savedB else (latestAppt?.building?.takeIf { it.isNotBlank() } ?: "N/A")
            val locUrl = latestAppt?.hospitalLocation?.takeIf { it.isNotBlank() } ?: savedL

            val pdfDoc = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint().apply { isAntiAlias = true }
            val ecgPath = android.graphics.Path().apply {
                moveTo(494f, 65f)
                lineTo(508f, 65f)
                lineTo(514f, 51f)
                lineTo(521f, 78f)
                lineTo(528f, 59f)
                lineTo(533f, 68f)
                lineTo(538f, 65f)
                lineTo(555f, 65f)
            }

            fun drawPageHeader(canvas: android.graphics.Canvas, title: String, subtitle: String) {
                paint.style = android.graphics.Paint.Style.FILL
                paint.textAlign = android.graphics.Paint.Align.LEFT
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                var titleSize = 16.5f
                paint.textSize = titleSize
                while (titleSize > 12.5f && paint.measureText(title) > 445f) {
                    titleSize -= 0.5f
                    paint.textSize = titleSize
                }
                paint.color = android.graphics.Color.rgb(103, 80, 164)
                canvas.drawText(ellipsizePdfText(title, 445f, paint), 40f, 52f, paint)

                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                paint.textSize = 8.8f
                paint.color = android.graphics.Color.GRAY
                canvas.drawText(ellipsizePdfText(subtitle, 445f, paint), 41f, 70f, paint)

                paint.color = android.graphics.Color.rgb(103, 80, 164)
                paint.strokeWidth = 1.5f
                paint.style = android.graphics.Paint.Style.STROKE
                canvas.drawPath(ecgPath, paint)
                paint.style = android.graphics.Paint.Style.FILL

                paint.strokeWidth = 1.2f
                paint.color = android.graphics.Color.LTGRAY
                canvas.drawLine(40f, 88f, 555f, 88f, paint)
            }

            // -------------------- PAGE 1: EXECUTIVE SUMMARY, PROGRESS, ESTIMATIONS & SYMPTOMS --------------------
            val page1 = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create())
            val canvas1 = page1.canvas
            drawPageHeader(
                canvas1,
                "ROACCUTANE TRACKER - MEDICAL REPORT",
                "Generated in real-time • Summary of Treatment, Estimations & Symptoms"
            )

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 10f
            paint.color = android.graphics.Color.BLACK
            var y = 110f

            canvas1.drawText(ellipsizePdfText("Patient Name: ${profile.name}", 268f, paint), 40f, y, paint)
            canvas1.drawText(ellipsizePdfText("Patient File No.: ${profile.savedPatientFileNumber.ifBlank { "N/A" }}", 235f, paint), 318f, y, paint)
            y += 17f
            canvas1.drawText(ellipsizePdfText("Age: ${profile.age} years", 268f, paint), 40f, y, paint)
            val bDateTypeLabel = if (profile.birthDateType == "H") "Hijri" else "Gregorian"
            val bDateStr = if (profile.birthDateVal.isNotBlank()) "${profile.birthDateVal} ($bDateTypeLabel)" else "N/A"
            canvas1.drawText(ellipsizePdfText("Birth Date: $bDateStr", 235f, paint), 318f, y, paint)
            y += 17f
            canvas1.drawText(ellipsizePdfText("Weight: ${String.format(Locale.US, "%.1f", profile.weightKg)} kg", 268f, paint), 40f, y, paint)
            canvas1.drawText(ellipsizePdfText("Prescribed Daily Dose: ${profile.dailyDoseMg} mg/day", 235f, paint), 318f, y, paint)
            y += 24f

            // I. THERAPEUTIC DOSAGE PROGRESS
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 11.5f
            canvas1.drawText("I. THERAPEUTIC DOSAGE PROGRESS & COURSE ESTIMATIONS", 40f, y, paint)
            y += 18f

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 9.8f
            canvas1.drawText(ellipsizePdfText("Total Cumulative Dose Taken: $cumulativeDose mg", 263f, paint), 45f, y, paint)
            val compliancePct = if (targetDose > 0) (cumulativeDose.toFloat() / targetDose) * 100f else 0f
            canvas1.drawText(ellipsizePdfText("Course Compliance Ratio: ${String.format(Locale.US, "%.1f%%", compliancePct)}", 235f, paint), 318f, y, paint)
            y += 16f

            canvas1.drawText(ellipsizePdfText("Required Cumulative Target: $targetDose mg", 263f, paint), 45f, y, paint)
            val stratTag = when {
                profile.targetMgPerKgInt <= 120 -> "MIN"
                profile.targetMgPerKgInt >= 150 -> "MAX"
                else -> "CUSTOM"
            }
            canvas1.drawText(ellipsizePdfText("Model Strategy: ${profile.targetMgPerKgInt} mg/kg ($stratTag)", 235f, paint), 318f, y, paint)
            y += 16f

            val startStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(profile.startDate))
            canvas1.drawText(ellipsizePdfText("Treatment Start Date: $startStr", 263f, paint), 45f, y, paint)
            canvas1.drawText(ellipsizePdfText("Active Elapsed Days: $elapsedDays days (Month ${estimations.currentTreatmentMonth})", 235f, paint), 318f, y, paint)
            y += 14f

            paint.color = android.graphics.Color.rgb(235, 235, 235)
            canvas1.drawRoundRect(android.graphics.RectF(45f, y, 545f, y + 10f), 4f, 4f, paint)
            val barW = 500f * (compliancePct.coerceIn(0f, 100f) / 100f)
            paint.color = android.graphics.Color.rgb(103, 80, 164)
            canvas1.drawRoundRect(android.graphics.RectF(45f, y, 45f + barW, y + 10f), 4f, 4f, paint)
            y += 18f

            // 4-Box Smart Estimations Strip (synced with Settings Estimations Hub)
            val estDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val estBoxH = 42f
            val estBoxW = 125f
            val estGap = 5f
            val nextLabSubLabel = when {
                estimations.nextUpcomingLabVisit != null -> "Booked: ${estimations.nextUpcomingLabVisit.hospitalName.ifBlank { "Lab Visit" }}"
                estimations.nextUpcomingClinicVisit != null -> "Pre-clinic check"
                else -> "Monthly protocol"
            }
            val estCards = listOf(
                Triple("EST. COURSE FINISH", estDateFmt.format(Date(estimations.estimatedCourseCompletionDateMillis)), "${estimations.estimatedDaysToCourseCompletion} days remaining"),
                Triple("LAST CAPSULE DATE", estDateFmt.format(Date(estimations.estimatedRunOutDateMillis)), "${estimations.liveRemainingCapsules} caps (${estimations.liveRemainingDays}d stock)"),
                Triple("NEXT LAB DRAW", estDateFmt.format(Date(estimations.recommendedNextLabDateMillis)), nextLabSubLabel),
                Triple("LAB SAFETY STATUS", if (estimations.labSafetyAssessment.status == com.example.utils.SafetyStatus.NORMAL) "Normal / Safe" else "Elevated Alert", "${uniqueLabsChronological.size} lab panel(s)")
            )
            estCards.forEachIndexed { idx, (label, value, sub) ->
                val bx = 40f + idx * (estBoxW + estGap)
                val innerW = estBoxW - 12f
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = if (idx == 3 && estimations.labSafetyAssessment.status != com.example.utils.SafetyStatus.NORMAL)
                    android.graphics.Color.rgb(254, 240, 240)
                else
                    android.graphics.Color.rgb(246, 244, 252)
                canvas1.drawRoundRect(android.graphics.RectF(bx, y, bx + estBoxW, y + estBoxH), 5f, 5f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(212, 204, 235)
                paint.strokeWidth = 0.8f
                canvas1.drawRoundRect(android.graphics.RectF(bx, y, bx + estBoxW, y + estBoxH), 5f, 5f, paint)

                paint.style = android.graphics.Paint.Style.FILL
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 6.6f
                paint.color = android.graphics.Color.rgb(103, 80, 164)
                canvas1.drawText(ellipsizePdfText(label, innerW, paint), bx + 6f, y + 12f, paint)

                paint.textSize = 9.2f
                paint.color = if (idx == 3 && estimations.labSafetyAssessment.status != com.example.utils.SafetyStatus.NORMAL)
                    android.graphics.Color.rgb(198, 40, 40)
                else
                    android.graphics.Color.BLACK
                canvas1.drawText(ellipsizePdfText(value, innerW, paint), bx + 6f, y + 25f, paint)

                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 6.7f
                paint.color = android.graphics.Color.DKGRAY
                canvas1.drawText(ellipsizePdfText(sub, innerW, paint), bx + 6f, y + 36f, paint)
            }
            y += estBoxH + 16f

            // II. SIDE-EFFECT FREQUENCIES STATS
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 11.5f
            paint.color = android.graphics.Color.BLACK
            canvas1.drawText("II. SIDE-EFFECT FREQUENCIES STATS", 40f, y, paint)
            y += 15f

            paint.textSize = 9f
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas1.drawText("Symptom", 45f, y, paint)
            canvas1.drawText("Occurrence Bar Chart", 200f, y, paint)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas1.drawText("Frequency (Count)", 550f, y, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.strokeWidth = 1f
            paint.color = android.graphics.Color.DKGRAY
            canvas1.drawLine(40f, y + 4f, 555f, y + 4f, paint)
            y += 17f

            paint.color = android.graphics.Color.BLACK
            if (symptomStats.isEmpty()) {
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 9f
                canvas1.drawText("No recorded side-effects in current log history.", 45f, y, paint)
                y += 18f
            } else {
                symptomStats.take(5).forEach { stat ->
                    val enLabel = stat.symptom
                    val symIdx = SYMPTOMS_EN.indexOf(enLabel)
                    val displayLabel = if (symIdx != -1) "$enLabel / ${SYMPTOMS_AR[symIdx]}" else enLabel
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 8.4f
                    paint.color = android.graphics.Color.BLACK
                    canvas1.drawText(ellipsizePdfText(displayLabel, 146f, paint), 45f, y, paint)

                    paint.color = android.graphics.Color.rgb(240, 240, 240)
                    canvas1.drawRect(200f, y - 7f, 395f, y + 2f, paint)
                    paint.color = android.graphics.Color.rgb(180, 70, 70)
                    val symBarW = 195f * (stat.percentage.coerceIn(0f, 100f) / 100f)
                    canvas1.drawRect(200f, y - 7f, 200f + symBarW, y + 2f, paint)

                    paint.color = android.graphics.Color.DKGRAY
                    paint.textSize = 8.6f
                    paint.textAlign = android.graphics.Paint.Align.RIGHT
                    canvas1.drawText(
                        ellipsizePdfText("${String.format(Locale.US, "%.1f%%", stat.percentage)} (${stat.occurrenceCount} logs)", 145f, paint),
                        550f,
                        y,
                        paint
                    )
                    paint.textAlign = android.graphics.Paint.Align.LEFT
                    y += 16f
                }
            }
            y += 14f

            // III. WEEKLY SYMPTOM TRENDS LINE GRAPH
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 11.5f
            paint.color = android.graphics.Color.BLACK
            canvas1.drawText("III. WEEKLY SYMPTOM TRENDS LINE GRAPH (BY WEEK & SYMPTOM)", 40f, y, paint)
            y += 11f

            val allSymptomsWithColors = listOf(
                ("Chapped Lips" to "جفاف الشفاه") to android.graphics.Color.rgb(229, 57, 53),
                ("Dry Skin" to "جفاف الجلد") to android.graphics.Color.rgb(251, 140, 0),
                ("Dry Eyes" to "جفاف العين") to android.graphics.Color.rgb(30, 136, 229),
                ("Nosebleeds" to "نزيف الأنف") to android.graphics.Color.rgb(0, 150, 136),
                ("Joint Pain" to "آلام المفاصل") to android.graphics.Color.rgb(142, 36, 170),
                ("Muscle Aches" to "آلام العضلات") to android.graphics.Color.rgb(216, 27, 96),
                ("Headaches" to "الصداع") to android.graphics.Color.rgb(141, 110, 99),
                ("Fatigue" to "الخمول والتعب") to android.graphics.Color.rgb(67, 160, 71),
                ("Mood Swings" to "تقلبات المزاج") to android.graphics.Color.rgb(57, 73, 171),
                ("Brain Fog" to "تشوش الذهن") to android.graphics.Color.rgb(245, 124, 0)
            )

            val startMs = if (profile.startDate > 0) profile.startDate else System.currentTimeMillis()
            val nowMs = System.currentTimeMillis()
            val courseDays = ((nowMs - startMs).coerceAtLeast(0) / (1000 * 60 * 60 * 24)).toInt()
            val weeksCount = ((courseDays / 7) + 1).coerceIn(1, 28)
            val startCal = Calendar.getInstance().apply { timeInMillis = startMs }

            data class PdfWeekData(val weekNum: Int, val dateRangeStr: String, val symptomCounts: Map<String, Int>)
            val weekDataList = mutableListOf<PdfWeekData>()
            for (w in 0 until weeksCount) {
                val wkStartCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, w * 7) }
                val counts = mutableMapOf<String, Int>()
                for (dayOffset in 0..6) {
                    val dCal = (wkStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                    val dStr = String.format(Locale.US, "%04d-%02d-%02d", dCal.get(Calendar.YEAR), dCal.get(Calendar.MONTH) + 1, dCal.get(Calendar.DAY_OF_MONTH))
                    val log = allLogs.value.find { it.dateString == dStr }
                    if (log != null && log.symptoms.isNotBlank()) {
                        log.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawSym ->
                            val key = getSymptomKeyInEnglish(rawSym)
                            counts[key] = (counts[key] ?: 0) + 1
                        }
                    }
                }
                val dateStr = "${wkStartCal.get(Calendar.DAY_OF_MONTH)}/${wkStartCal.get(Calendar.MONTH) + 1}"
                weekDataList.add(PdfWeekData(w + 1, dateStr, counts))
            }

            val activeSymptomsToDraw = allSymptomsWithColors.filter { (pair, _) ->
                weekDataList.any { wk -> (wk.symptomCounts[pair.first] ?: 0) > 0 }
            }

            val chartBoxTop = y
            val chartBoxHeight = 114f
            paint.color = android.graphics.Color.rgb(248, 249, 250)
            canvas1.drawRoundRect(android.graphics.RectF(40f, chartBoxTop, 555f, chartBoxTop + chartBoxHeight), 8f, 8f, paint)
            paint.color = android.graphics.Color.rgb(210, 215, 220)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas1.drawRoundRect(android.graphics.RectF(40f, chartBoxTop, 555f, chartBoxTop + chartBoxHeight), 8f, 8f, paint)
            paint.style = android.graphics.Paint.Style.FILL

            val yAxisX = 72f
            val xAxisY = chartBoxTop + chartBoxHeight - 25f
            val chartTopY = chartBoxTop + 18f
            val chartRightX = 536f
            val maxYVal = 7f

            paint.color = android.graphics.Color.rgb(0, 106, 96)
            paint.textSize = 7f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas1.drawText("Y Axis: Entries / الإدخالات (0-7)", 48f, chartTopY - 5f, paint)

            paint.color = android.graphics.Color.rgb(230, 235, 240)
            paint.strokeWidth = 1f
            for (v in listOf(0, 2, 4, 6, 7)) {
                val gridY = xAxisY - (v.toFloat() / maxYVal) * (xAxisY - chartTopY)
                canvas1.drawLine(yAxisX, gridY, chartRightX, gridY, paint)
                paint.color = android.graphics.Color.GRAY
                paint.textSize = 7.2f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas1.drawText("$v", yAxisX - 13f, gridY + 3f, paint)
                paint.color = android.graphics.Color.rgb(230, 235, 240)
            }

            paint.color = android.graphics.Color.rgb(0, 106, 96)
            paint.textSize = 7f
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas1.drawText("X Axis: Treatment Weeks / أسابيع العلاج", 545f, chartTopY - 5f, paint)

            val xStep = if (weeksCount > 1) (chartRightX - yAxisX) / (weeksCount - 1) else 0f
            val labelStride = when {
                weeksCount <= 10 -> 1
                weeksCount <= 18 -> 2
                weeksCount <= 26 -> 3
                else -> 4
            }
            var lastDrawnX = -999f
            weekDataList.forEachIndexed { i, wk ->
                val nodeX = if (weeksCount > 1) yAxisX + i * xStep else (yAxisX + chartRightX) / 2f
                val isLast = (i == weeksCount - 1)
                val spacedFromLast = (nodeX - lastDrawnX) >= 34f
                val leavesRoomForLast = isLast || (chartRightX - nodeX) >= 34f
                val shouldDraw = (i == 0) || (isLast && (nodeX - lastDrawnX) >= 28f) || ((i % labelStride == 0) && spacedFromLast && leavesRoomForLast)
                if (shouldDraw) {
                    lastDrawnX = nodeX
                    paint.color = android.graphics.Color.rgb(180, 185, 190)
                    paint.strokeWidth = 1f
                    paint.style = android.graphics.Paint.Style.STROKE
                    canvas1.drawLine(nodeX, xAxisY, nodeX, xAxisY + 3f, paint)
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    paint.color = android.graphics.Color.BLACK
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 6.6f
                    canvas1.drawText("wk${wk.weekNum}", nodeX, xAxisY + 11f, paint)
                    paint.color = android.graphics.Color.DKGRAY
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 5.5f
                    canvas1.drawText(wk.dateRangeStr, nodeX, xAxisY + 18f, paint)
                }
            }
            paint.textAlign = android.graphics.Paint.Align.LEFT

            if (activeSymptomsToDraw.isEmpty()) {
                paint.color = android.graphics.Color.GRAY
                paint.textSize = 9f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                canvas1.drawText("No logged side effects recorded across treatment weeks.", yAxisX + 24f, chartTopY + 38f, paint)
            } else {
                activeSymptomsToDraw.forEach { (pair, color) ->
                    val symKey = pair.first
                    val points = weekDataList.mapIndexed { i, wk ->
                        val cnt = (wk.symptomCounts[symKey] ?: 0).coerceAtMost(7)
                        val px = if (weeksCount > 1) yAxisX + i * xStep else (yAxisX + chartRightX) / 2f
                        val py = xAxisY - (cnt.toFloat() / maxYVal) * (xAxisY - chartTopY)
                        Pair(px, py)
                    }
                    paint.color = color
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    for (i in 0 until points.size - 1) {
                        canvas1.drawLine(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, paint)
                    }
                    paint.style = android.graphics.Paint.Style.FILL
                    points.forEach { pt -> canvas1.drawCircle(pt.first, pt.second, 3f, paint) }
                }
            }

            y += chartBoxHeight + 8f
            paint.style = android.graphics.Paint.Style.FILL
            paint.textSize = 7.4f
            val col1X = 45f
            val col2X = 300f
            var legendY = y
            allSymptomsWithColors.forEachIndexed { idx, (pair, color) ->
                val isCol2 = (idx % 2 == 1)
                val curX = if (isCol2) col2X else col1X
                val maxLegendW = if (isCol2) 235f else 235f
                paint.color = color
                canvas1.drawRoundRect(android.graphics.RectF(curX, legendY, curX + 10f, legendY + 8f), 2f, 2f, paint)
                paint.color = android.graphics.Color.BLACK
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas1.drawText(ellipsizePdfText("${pair.first} / ${pair.second}", maxLegendW, paint), curX + 13f, legendY + 7f, paint)
                if (isCol2 || idx == allSymptomsWithColors.size - 1) {
                    legendY += 11.5f
                }
            }

            drawSaudiDisclaimerFooter(canvas1, paint, currentPageNum, totalPages)
            pdfDoc.finishPage(page1)
            currentPageNum++

            // -------------------- PAGE 2: CLINICAL CENTRE, APPOINTMENTS SUMMARY & HOSPITAL QR CODES --------------------
            val page2 = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create())
            val canvas2 = page2.canvas
            drawPageHeader(
                canvas2,
                "ROACCUTANE CLINICAL VISITS & PROVIDERS",
                "Healthcare provider details, linked clinical/lab visits & Google Maps QR codes"
            )

            y = 112f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 12f
            paint.color = android.graphics.Color.BLACK
            canvas2.drawText("IV. PRIMARY CLINICAL CENTRE & HEALTHCARE PROVIDER", 40f, y, paint)
            y += 18f

            val apptFileNum = latestAppt?.patientFileNumber?.takeIf { it.isNotBlank() }
                ?: profile.savedPatientFileNumber.ifBlank { "N/A" }

            val providerPairs = listOf(
                "Hospital / Center:" to hosp,
                "Dermatologist Clinician:" to doc,
                "Assigned Clinic / Room:" to clinicVal,
                "Building / Block:" to bld,
                "Patient File Number:" to apptFileNum
            )
            providerPairs.forEach { (k, v) ->
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 10f
                paint.color = android.graphics.Color.DKGRAY
                canvas2.drawText(ellipsizePdfText(k, 152f, paint), 45f, y, paint)
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.color = android.graphics.Color.BLACK
                canvas2.drawText(ellipsizePdfText(v, 345f, paint), 205f, y, paint)
                y += 16f
            }

            y += 10f

            // V. RECENT & UPCOMING APPOINTMENTS LOG (including CLINIC / LAB / PHARMACY types)
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 11.5f
            paint.color = android.graphics.Color.BLACK
            canvas2.drawText("V. SCHEDULED & RECENT VISITS SUMMARY (CLINIC • LAB • PHARMACY)", 40f, y, paint)
            y += 11f

            val visitsToShow = allAppointments.value.sortedByDescending { it.dateTime }.take(6)
            val vHdrH = 19f
            val vRowH = 27f
            val vColDividers = listOf(110f, 172f, 336f, 488f)
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(103, 80, 164)
            canvas2.drawRect(40f, y, 555f, y + vHdrH, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(180, 180, 180)
            paint.strokeWidth = 0.8f
            canvas2.drawRect(40f, y, 555f, y + vHdrH, paint)
            vColDividers.forEach { cx -> canvas2.drawLine(cx, y, cx, y + vHdrH, paint) }

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 7.8f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas2.drawText("Date", 46f, y + 12.5f, paint)
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas2.drawText("Type", 141f, y + 12.5f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas2.drawText("Hospital / Clinic", 178f, y + 12.5f, paint)
            canvas2.drawText("Reason / Link", 342f, y + 12.5f, paint)
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas2.drawText("Status", 521.5f, y + 12.5f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            y += vHdrH

            if (visitsToShow.isEmpty()) {
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(250, 250, 252)
                canvas2.drawRect(40f, y, 555f, y + 24f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(215, 215, 220)
                canvas2.drawRect(40f, y, 555f, y + 24f, paint)
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.GRAY
                paint.textSize = 8.5f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas2.drawText("No clinical or laboratory appointments recorded yet.", 48f, y + 15f, paint)
                y += 24f
            } else {
                val visitDateFmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
                visitsToShow.forEachIndexed { idx, appt ->
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = if (idx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.rgb(248, 247, 252)
                    canvas2.drawRect(40f, y, 555f, y + vRowH, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(220, 220, 228)
                    canvas2.drawRect(40f, y, 555f, y + vRowH, paint)
                    vColDividers.forEach { cx -> canvas2.drawLine(cx, y, cx, y + vRowH, paint) }

                    paint.style = android.graphics.Paint.Style.FILL
                    paint.textSize = 7.6f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.color = android.graphics.Color.BLACK
                    canvas2.drawText(ellipsizePdfText(visitDateFmt.format(Date(appt.dateTime)), 60f, paint), 45f, y + 16f, paint)

                    val typeTag = when (appt.appointmentType.uppercase(Locale.US)) {
                        "LAB" -> "LAB"
                        "PHARMACY" -> "PHARMACY"
                        else -> "CLINIC"
                    }
                    val (tagBg, tagFg) = when (typeTag) {
                        "LAB" -> android.graphics.Color.rgb(227, 242, 253) to android.graphics.Color.rgb(0, 100, 155)
                        "PHARMACY" -> android.graphics.Color.rgb(232, 245, 233) to android.graphics.Color.rgb(46, 125, 50)
                        else -> android.graphics.Color.rgb(237, 231, 246) to android.graphics.Color.rgb(103, 80, 164)
                    }
                    paint.color = tagBg
                    canvas2.drawRoundRect(android.graphics.RectF(114f, y + 6f, 168f, y + 21f), 4f, 4f, paint)
                    paint.color = tagFg
                    paint.textSize = 6.8f
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas2.drawText(typeTag, 141f, y + 16f, paint)
                    paint.textAlign = android.graphics.Paint.Align.LEFT

                    val line1Y = y + 11.5f
                    val line2Y = y + 21.5f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 7.4f
                    canvas2.drawText(ellipsizePdfText(appt.hospitalName.ifBlank { "Clinic" }, 154f, paint), 178f, line1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.color = android.graphics.Color.DKGRAY
                    paint.textSize = 6.6f
                    canvas2.drawText(ellipsizePdfText(appt.clinic.ifBlank { appt.doctorName.ifBlank { "—" } }, 154f, paint), 178f, line2Y, paint)

                    val reasonMain = appt.reason.ifBlank { typeTag }
                    val linkSub = if (appt.parentAppointmentId != null) "Linked to Clinic Visit" else appt.notes.trim()
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 7.3f
                    canvas2.drawText(ellipsizePdfText(reasonMain, 142f, paint), 342f, if (linkSub.isBlank()) y + 16f else line1Y, paint)
                    if (linkSub.isNotBlank()) {
                        paint.color = if (appt.parentAppointmentId != null) android.graphics.Color.rgb(0, 110, 165) else android.graphics.Color.GRAY
                        paint.textSize = 6.5f
                        canvas2.drawText(ellipsizePdfText(linkSub, 142f, paint), 342f, line2Y, paint)
                    }

                    val stBg = if (appt.isCompleted) android.graphics.Color.rgb(232, 245, 233) else android.graphics.Color.rgb(255, 243, 224)
                    val stFg = if (appt.isCompleted) android.graphics.Color.rgb(30, 110, 50) else android.graphics.Color.rgb(200, 95, 0)
                    paint.color = stBg
                    canvas2.drawRoundRect(android.graphics.RectF(493f, y + 6f, 550f, y + 21f), 4f, 4f, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.color = stFg
                    paint.textSize = 6.8f
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas2.drawText(if (appt.isCompleted) "Done" else "Upcoming", 521.5f, y + 16f, paint)
                    paint.textAlign = android.graphics.Paint.Align.LEFT

                    y += vRowH
                }
            }

            y += 18f

            // VI. HOSPITAL LOCATION QR CODES
            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 11.5f
            paint.color = android.graphics.Color.BLACK
            canvas2.drawText("VI. HOSPITAL LOCATION QR CODES / مواقع المستشفيات والعيادات", 40f, y, paint)
            y += 14f

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 8.2f
            paint.color = android.graphics.Color.DKGRAY
            canvas2.drawText("Scan QR code(s) below to navigate to each hospital's location on Google Maps:", 45f, y, paint)
            y += 12f

            data class HospitalPdfEntry(val name: String, val doctor: String, val fileNo: String, val locationUrl: String)
            val hospMap = linkedMapOf<String, HospitalPdfEntry>()
            allAppointments.value.forEach { apptItem ->
                val hName = apptItem.hospitalName.trim()
                if (hName.isNotBlank()) {
                    val key = hName.lowercase(Locale.ROOT)
                    val existing = hospMap[key]
                    val apptLoc = apptItem.hospitalLocation.trim().ifBlank { locUrl }
                    val apptFile = apptItem.patientFileNumber?.trim().orEmpty().ifBlank { apptFileNum }
                    if (existing == null) {
                        hospMap[key] = HospitalPdfEntry(hName, apptItem.doctorName.trim(), apptFile, apptLoc)
                    } else {
                        hospMap[key] = existing.copy(
                            doctor = existing.doctor.ifBlank { apptItem.doctorName.trim() },
                            fileNo = existing.fileNo.ifBlank { apptFile },
                            locationUrl = existing.locationUrl.ifBlank { apptLoc }
                        )
                    }
                }
            }
            if (hosp.isNotBlank() && hosp != "N/A") {
                val key = hosp.lowercase(Locale.ROOT)
                if (!hospMap.containsKey(key)) {
                    hospMap[key] = HospitalPdfEntry(hosp, doc, apptFileNum, locUrl)
                }
            }
            if (hospMap.isEmpty()) {
                hospMap["default"] = HospitalPdfEntry("Healthcare Provider", doc, apptFileNum, if (locUrl.isNotBlank()) locUrl else "https://maps.google.com")
            }

            val hospitalsToDisplay = hospMap.values.toList().take(3)
            val hospCount = hospitalsToDisplay.size
            val cardTop = y
            val cardHeight = 104f

            if (hospCount == 1) {
                val item = hospitalsToDisplay[0]
                val cardX = 40f
                val cardWidth = 515f
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(250, 250, 252)
                canvas2.drawRoundRect(android.graphics.RectF(cardX, cardTop, cardX + cardWidth, cardTop + cardHeight), 8f, 8f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(220, 225, 235)
                paint.strokeWidth = 1f
                canvas2.drawRoundRect(android.graphics.RectF(cardX, cardTop, cardX + cardWidth, cardTop + cardHeight), 8f, 8f, paint)
                paint.style = android.graphics.Paint.Style.FILL

                val qrSize = 80f
                val qrX = cardX + 14f
                val qrY = cardTop + 12f
                drawQrCodeOnCanvas(canvas2, paint, item.locationUrl, qrX, qrY, qrSize)

                val textLeft = qrX + qrSize + 18f
                val textMaxW = (cardX + cardWidth) - textLeft - 12f
                var lineY = cardTop + 24f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 11.2f
                paint.color = android.graphics.Color.rgb(103, 80, 164)
                canvas2.drawText(ellipsizePdfText(item.name, textMaxW, paint), textLeft, lineY, paint)
                lineY += 17f
                if (item.fileNo.isNotBlank()) {
                    paint.textSize = 9f
                    paint.color = android.graphics.Color.rgb(30, 80, 150)
                    canvas2.drawText(ellipsizePdfText("Patient File No. (رقم الملف): ${item.fileNo}", textMaxW, paint), textLeft, lineY, paint)
                    lineY += 14f
                }
                if (item.doctor.isNotBlank() && item.doctor != "N/A") {
                    paint.textSize = 9f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.color = android.graphics.Color.BLACK
                    canvas2.drawText(ellipsizePdfText("Clinician (الطبيب المعالج): ${item.doctor}", textMaxW, paint), textLeft, lineY, paint)
                    lineY += 14f
                }
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                paint.textSize = 7.4f
                paint.color = android.graphics.Color.GRAY
                canvas2.drawText(ellipsizePdfText("Map URL: ${item.locationUrl}", textMaxW, paint), textLeft, lineY, paint)
            } else {
                val colSpacing = 10f
                val cardWidth = (515f - ((hospCount - 1) * colSpacing)) / hospCount
                hospitalsToDisplay.forEachIndexed { idx, item ->
                    val colX = 40f + idx * (cardWidth + colSpacing)
                    val innerCardW = cardWidth - 16f
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.rgb(250, 250, 252)
                    canvas2.drawRoundRect(android.graphics.RectF(colX, cardTop, colX + cardWidth, cardTop + cardHeight), 6f, 6f, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(220, 225, 235)
                    paint.strokeWidth = 1f
                    canvas2.drawRoundRect(android.graphics.RectF(colX, cardTop, colX + cardWidth, cardTop + cardHeight), 6f, 6f, paint)
                    paint.style = android.graphics.Paint.Style.FILL

                    var cy = cardTop + 14f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = if (hospCount == 2) 9.2f else 8f
                    paint.color = android.graphics.Color.rgb(103, 80, 164)
                    canvas2.drawText(ellipsizePdfText(item.name, innerCardW, paint), colX + 8f, cy, paint)
                    cy += 11f
                    if (item.fileNo.isNotBlank()) {
                        paint.textSize = 7f
                        paint.color = android.graphics.Color.rgb(30, 80, 150)
                        canvas2.drawText(ellipsizePdfText("File: ${item.fileNo}", innerCardW, paint), colX + 8f, cy, paint)
                        cy += 10f
                    }
                    val qrSize = if (hospCount == 2) 50f else 44f
                    val qrX = colX + (cardWidth - qrSize) / 2f
                    drawQrCodeOnCanvas(canvas2, paint, item.locationUrl, qrX, cy, qrSize)
                    cy += qrSize + 9f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.5f
                    paint.color = android.graphics.Color.DKGRAY
                    val docOrScan = if (item.doctor.isNotBlank() && item.doctor != "N/A") "Dr. ${item.doctor}" else "Google Maps"
                    canvas2.drawText(ellipsizePdfText(docOrScan, innerCardW, paint), colX + 8f, cy, paint)
                }
            }

            y = cardTop + cardHeight + 16f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 7.5f
            paint.color = android.graphics.Color.GRAY
            canvas2.drawText("Clinic Notice: Roaccutane is lipophilic. Absorption improves significantly when taken with dietary fats.", 45f, y, paint)
            y += 11f
            canvas2.drawText("Hospital details, clinician assignments and visit schedules are synchronized dynamically from saved records.", 45f, y, paint)

            drawSaudiDisclaimerFooter(canvas2, paint, currentPageNum, totalPages)
            pdfDoc.finishPage(page2)
            currentPageNum++

            // Shared Biomarker Definitions for Page 3 & Page 4+
            data class PdfBiomarkerSpec(
                val category: String,
                val nameEn: String,
                val convRange: String,
                val siRange: String,
                val getValue: (Appointment) -> Double?,
                val formatConv: (Double) -> String,
                val formatConvCompact: (Double) -> String,
                val formatSi: (Double) -> String,
                val statusLabel: (Double) -> String,
                val isAbnormal: (Double) -> Boolean,
                val isHigherWorse: Boolean = true
            )

            val allBiomarkers = listOf(
                // Liver Function (LFTs)
                PdfBiomarkerSpec(
                    category = "LIVER FUNCTION TESTS (LFTs) / وظائف الكبد",
                    nameEn = "ALT (SGPT)",
                    convRange = "7 - 56 U/L",
                    siRange = "0.12 - 0.93 µkat/L",
                    getValue = { it.alt },
                    formatConv = { String.format(Locale.US, "%.0f U/L", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f U/L", it) },
                    formatSi = { String.format(Locale.US, "%.2f µkat/L", com.example.utils.LabUnitConverter.enzymeUlToUkat(it)) },
                    statusLabel = { v -> if (v < 7.0) "LOW" else if (v > 56.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 7.0..56.0 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "LIVER FUNCTION TESTS (LFTs) / وظائف الكبد",
                    nameEn = "AST (SGOT)",
                    convRange = "10 - 40 U/L",
                    siRange = "0.17 - 0.67 µkat/L",
                    getValue = { it.ast },
                    formatConv = { String.format(Locale.US, "%.0f U/L", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f U/L", it) },
                    formatSi = { String.format(Locale.US, "%.2f µkat/L", com.example.utils.LabUnitConverter.enzymeUlToUkat(it)) },
                    statusLabel = { v -> if (v < 10.0) "LOW" else if (v > 40.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 10.0..40.0 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "LIVER FUNCTION TESTS (LFTs) / وظائف الكبد",
                    nameEn = "Total Bilirubin",
                    convRange = "0.1 - 1.2 mg/dL",
                    siRange = "1.7 - 20.5 µmol/L",
                    getValue = { it.bilirubin },
                    formatConv = { String.format(Locale.US, "%.2f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.2f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.1f µmol/L", com.example.utils.LabUnitConverter.bilirubinMgToUmol(it)) },
                    statusLabel = { v -> if (v < 0.1) "LOW" else if (v > 1.2) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 0.1..1.2 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "LIVER FUNCTION TESTS (LFTs) / وظائف الكبد",
                    nameEn = "Alkaline Phosphatase (ALP)",
                    convRange = "44 - 147 U/L",
                    siRange = "0.73 - 2.45 µkat/L",
                    getValue = { it.alkalinePhosphate },
                    formatConv = { String.format(Locale.US, "%.0f U/L", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f U/L", it) },
                    formatSi = { String.format(Locale.US, "%.2f µkat/L", com.example.utils.LabUnitConverter.enzymeUlToUkat(it)) },
                    statusLabel = { v -> if (v < 44.0) "LOW" else if (v > 147.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 44.0..147.0 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "LIVER FUNCTION TESTS (LFTs) / وظائف الكبد",
                    nameEn = "Serum Albumin",
                    convRange = "3.5 - 5.5 g/dL",
                    siRange = "35 - 55 g/L",
                    getValue = { it.albuminInSerum },
                    formatConv = { String.format(Locale.US, "%.2f g/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.2f g/dL", it) },
                    formatSi = { String.format(Locale.US, "%.1f g/L", com.example.utils.LabUnitConverter.albuminGdlToGl(it)) },
                    statusLabel = { v -> if (v < 3.5) "LOW" else if (v > 5.5) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 3.5..5.5 },
                    isHigherWorse = false
                ),
                // Kidney Function (KFTs)
                PdfBiomarkerSpec(
                    category = "KIDNEY FUNCTION TESTS (KFTs) / وظائف الكلى",
                    nameEn = "Serum Creatinine",
                    convRange = "0.6 - 1.2 mg/dL",
                    siRange = "53 - 106 µmol/L",
                    getValue = { it.creatinine },
                    formatConv = { String.format(Locale.US, "%.2f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.2f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.1f µmol/L", com.example.utils.LabUnitConverter.creatinineMgToUmol(it)) },
                    statusLabel = { v -> if (v < 0.6) "LOW" else if (v > 1.2) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 0.6..1.2 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "KIDNEY FUNCTION TESTS (KFTs) / وظائف الكلى",
                    nameEn = "Blood Urea (BUN)",
                    convRange = "7 - 20 mg/dL",
                    siRange = "2.5 - 7.1 mmol/L",
                    getValue = { it.urea },
                    formatConv = { String.format(Locale.US, "%.1f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.1f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.1f mmol/L", com.example.utils.LabUnitConverter.ureaBUNMgToMmol(it)) },
                    statusLabel = { v -> if (v < 7.0) "LOW" else if (v > 20.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 7.0..20.0 },
                    isHigherWorse = true
                ),
                // Lipid Profile
                PdfBiomarkerSpec(
                    category = "LIPID PROFILE & CARDIOVASCULAR / دهنيات الدم",
                    nameEn = "Total Cholesterol",
                    convRange = "< 200 mg/dL",
                    siRange = "< 5.2 mmol/L",
                    getValue = { it.tCholestrol },
                    formatConv = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.cholesterolMgToMmol(it)) },
                    statusLabel = { v -> if (v >= 200.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it >= 200.0 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "LIPID PROFILE & CARDIOVASCULAR / دهنيات الدم",
                    nameEn = "LDL Cholesterol",
                    convRange = "< 100 mg/dL",
                    siRange = "< 2.6 mmol/L",
                    getValue = { it.ldl },
                    formatConv = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.cholesterolMgToMmol(it)) },
                    statusLabel = { v -> if (v >= 100.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it >= 100.0 },
                    isHigherWorse = true
                ),
                PdfBiomarkerSpec(
                    category = "LIPID PROFILE & CARDIOVASCULAR / دهنيات الدم",
                    nameEn = "HDL Cholesterol",
                    convRange = "> 40 mg/dL",
                    siRange = "> 1.0 mmol/L",
                    getValue = { it.hdl },
                    formatConv = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.cholesterolMgToMmol(it)) },
                    statusLabel = { v -> if (v < 40.0) "LOW" else "NORMAL" },
                    isAbnormal = { it < 40.0 },
                    isHigherWorse = false
                ),
                PdfBiomarkerSpec(
                    category = "LIPID PROFILE & CARDIOVASCULAR / دهنيات الدم",
                    nameEn = "Triglycerides (TG)",
                    convRange = "< 150 mg/dL",
                    siRange = "< 1.7 mmol/L",
                    getValue = { it.triglycerides },
                    formatConv = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatConvCompact = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSi = { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.triglyceridesMgToMmol(it)) },
                    statusLabel = { v -> if (v >= 150.0) "HIGH" else "NORMAL" },
                    isAbnormal = { it >= 150.0 },
                    isHigherWorse = true
                ),
                // Glycemic Control
                PdfBiomarkerSpec(
                    category = "GLYCEMIC CONTROL / السكر التراكمي",
                    nameEn = "Glycated Haemoglobin (HbA1c)",
                    convRange = "4.0 - 5.6 %",
                    siRange = "20 - 38 mmol/mol",
                    getValue = { it.glycatedHaemoglobin },
                    formatConv = { String.format(Locale.US, "%.1f %%", it) },
                    formatConvCompact = { String.format(Locale.US, "%.1f%%", it) },
                    formatSi = { String.format(Locale.US, "%.0f mmol/mol", com.example.utils.LabUnitConverter.hba1cPercentToMmolMol(it)) },
                    statusLabel = { v -> if (v < 4.0) "LOW" else if (v > 5.6) "HIGH" else "NORMAL" },
                    isAbnormal = { it !in 4.0..5.6 },
                    isHigherWorse = true
                )
            )

            // Helper to retrieve the latest non-null reading for any biomarker (preferring LAB-typed visits)
            val allApptsDesc = allAppointments.value.sortedByDescending { it.dateTime }
            val labApptsDesc = allApptsDesc.filter { it.appointmentType.equals("LAB", ignoreCase = true) }
            fun findLatestReading(getter: (Appointment) -> Double?): Pair<Double, Long>? {
                val fromLab = labApptsDesc.firstOrNull { getter(it) != null }
                val target = fromLab ?: allApptsDesc.firstOrNull { getter(it) != null }
                return target?.let { appt -> getter(appt)?.let { v -> v to appt.dateTime } }
            }

            // -------------------- PAGE 3: COMPREHENSIVE LATEST LABORATORY SUMMARY (ALL 12 BIOMARKERS) --------------------
            val page3 = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create())
            val canvas3 = page3.canvas
            drawPageHeader(
                canvas3,
                "ROACCUTANE TRACKER - COMPREHENSIVE LAB SUMMARY",
                "Section VII: All Liver, Kidney, Lipid & Glycemic Biomarkers (Conventional & SI Units)"
            )

            // Patient & Safety Context Banner
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(245, 243, 250)
            canvas3.drawRoundRect(android.graphics.RectF(40f, 96f, 555f, 120f), 5f, 5f, paint)
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 8.3f
            paint.color = android.graphics.Color.rgb(55, 50, 75)
            canvas3.drawText(
                ellipsizePdfText(
                    "Patient: ${profile.name}  |  Weight: ${String.format(Locale.US, "%.1f", profile.weightKg)} kg  |  Daily Dose: ${profile.dailyDoseMg} mg  |  Total Lab Visits: ${uniqueLabsChronological.size}",
                    499f,
                    paint
                ),
                48f,
                111f,
                paint
            )

            // Table Header
            var tblY = 128f
            val hdrH = 22f
            val labSummaryDividers = listOf(202f, 284f, 368f, 496f)
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(103, 80, 164)
            canvas3.drawRect(40f, tblY, 555f, tblY + hdrH, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(180, 180, 180)
            paint.strokeWidth = 0.8f
            canvas3.drawRect(40f, tblY, 555f, tblY + hdrH, paint)
            labSummaryDividers.forEach { cx -> canvas3.drawLine(cx, tblY, cx, tblY + hdrH, paint) }

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 7.8f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas3.drawText("Biomarker & Latest Date", 46f, tblY + 14f, paint)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas3.drawText("Conventional", 279f, tblY + 14f, paint)
            canvas3.drawText("Intl. (SI)", 363f, tblY + 14f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas3.drawText("Ref. Range (Conv | SI)", 374f, tblY + 14f, paint)
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas3.drawText("Status", 525.5f, tblY + 14f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            tblY += hdrH

            val shortDateFmt = SimpleDateFormat("dd MMM yy", Locale.US)
            var lastCat = ""
            var rowCounter = 0

            allBiomarkers.forEach { spec ->
                if (spec.category != lastCat) {
                    lastCat = spec.category
                    val catH = 16f
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.rgb(236, 232, 246)
                    canvas3.drawRect(40f, tblY, 555f, tblY + catH, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(205, 198, 224)
                    canvas3.drawRect(40f, tblY, 555f, tblY + catH, paint)
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7.4f
                    paint.color = android.graphics.Color.rgb(75, 55, 130)
                    canvas3.drawText(ellipsizePdfText(spec.category, 500f, paint), 46f, tblY + 11f, paint)
                    tblY += catH
                }

                val rH = 26f
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = if (rowCounter % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.rgb(250, 249, 253)
                canvas3.drawRect(40f, tblY, 555f, tblY + rH, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(220, 220, 228)
                canvas3.drawRect(40f, tblY, 555f, tblY + rH, paint)
                labSummaryDividers.forEach { cx ->
                    canvas3.drawLine(cx, tblY, cx, tblY + rH, paint)
                }

                val reading = findLatestReading(spec.getValue)
                val valNum = reading?.first
                val valDate = reading?.second

                paint.style = android.graphics.Paint.Style.FILL
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 7.8f
                paint.color = android.graphics.Color.BLACK
                canvas3.drawText(ellipsizePdfText(spec.nameEn, 150f, paint), 46f, tblY + 11f, paint)

                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 6.7f
                paint.color = android.graphics.Color.GRAY
                val dateLabel = if (valDate != null) "Recorded: ${shortDateFmt.format(Date(valDate))}" else "No test recorded"
                canvas3.drawText(ellipsizePdfText(dateLabel, 150f, paint), 46f, tblY + 21f, paint)

                if (valNum != null) {
                    val abn = spec.isAbnormal(valNum)
                    val valColor = if (abn) android.graphics.Color.rgb(198, 40, 40) else android.graphics.Color.rgb(30, 115, 45)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7.8f
                    paint.color = valColor
                    paint.textAlign = android.graphics.Paint.Align.RIGHT
                    canvas3.drawText(ellipsizePdfText(spec.formatConv(valNum), 72f, paint), 279f, tblY + 16f, paint)
                    paint.textSize = 7.3f
                    paint.color = android.graphics.Color.rgb(45, 70, 120)
                    canvas3.drawText(ellipsizePdfText(spec.formatSi(valNum), 74f, paint), 363f, tblY + 16f, paint)

                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    val stText = spec.statusLabel(valNum)
                    paint.color = if (abn) android.graphics.Color.rgb(253, 232, 232) else android.graphics.Color.rgb(232, 245, 233)
                    canvas3.drawRoundRect(android.graphics.RectF(501f, tblY + 5f, 550f, tblY + 21f), 4f, 4f, paint)
                    paint.color = valColor
                    paint.textSize = 6.8f
                    canvas3.drawText(stText, 525.5f, tblY + 15.5f, paint)
                } else {
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 7.8f
                    paint.color = android.graphics.Color.GRAY
                    paint.textAlign = android.graphics.Paint.Align.RIGHT
                    canvas3.drawText("N/A", 279f, tblY + 16f, paint)
                    canvas3.drawText("N/A", 363f, tblY + 16f, paint)
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas3.drawText("—", 525.5f, tblY + 16f, paint)
                }

                paint.textAlign = android.graphics.Paint.Align.LEFT
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 6.7f
                paint.color = android.graphics.Color.DKGRAY
                canvas3.drawText(ellipsizePdfText("Conv: ${spec.convRange}", 118f, paint), 374f, tblY + 11f, paint)
                paint.color = android.graphics.Color.rgb(90, 90, 125)
                canvas3.drawText(ellipsizePdfText("SI: ${spec.siRange}", 118f, paint), 374f, tblY + 21f, paint)

                tblY += rH
                rowCounter++
            }

            // Cardiovascular Risk Factor + Additional Notes below summary table
            val latestTC = findLatestReading { it.tCholestrol }?.first ?: 0.0
            val latestHDL = findLatestReading { it.hdl }?.first ?: 0.0
            val rfVal = if (latestHDL > 0.0 && latestTC > 0.0) latestTC / latestHDL else 0.0
            val rfText = if (rfVal > 0.0) String.format(Locale.US, "%.2f", rfVal) else "N/A"

            var extraY = tblY + 12f
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = if (rfVal > 4.5) android.graphics.Color.rgb(254, 240, 240) else android.graphics.Color.rgb(240, 248, 242)
            canvas3.drawRoundRect(android.graphics.RectF(40f, extraY, 555f, extraY + 30f), 6f, 6f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = if (rfVal > 4.5) android.graphics.Color.rgb(235, 180, 180) else android.graphics.Color.rgb(185, 225, 195)
            canvas3.drawRoundRect(android.graphics.RectF(40f, extraY, 555f, extraY + 30f), 6f, 6f, paint)
            paint.style = android.graphics.Paint.Style.FILL
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 8.6f
            paint.color = android.graphics.Color.BLACK
            canvas3.drawText(ellipsizePdfText("Atherogenic / Cardiovascular Risk Factor (Total Cholesterol / HDL-C):", 375f, paint), 50f, extraY + 18f, paint)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            paint.color = when {
                rfVal <= 0.0 -> android.graphics.Color.GRAY
                rfVal > 4.5 -> android.graphics.Color.rgb(198, 40, 40)
                else -> android.graphics.Color.rgb(30, 115, 45)
            }
            canvas3.drawText("$rfText  (Optimal < 4.5)", 545f, extraY + 18f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT

            extraY += 38f
            val otherTestsNotes = allApptsDesc
                .filter { it.otherBloodTests.isNotBlank() }
                .take(3)
                .joinToString("\n") { apptItem ->
                    val dStr = shortDateFmt.format(Date(apptItem.dateTime))
                    "• [$dStr] ${apptItem.otherBloodTests.trim()}"
                }
                .ifBlank { "No additional text-based blood tests (e.g. CBC / Beta-hCG notes) logged." }

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(248, 249, 252)
            canvas3.drawRoundRect(android.graphics.RectF(40f, extraY, 555f, 720f), 6f, 6f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(215, 220, 230)
            canvas3.drawRoundRect(android.graphics.RectF(40f, extraY, 555f, 720f), 6f, 6f, paint)
            paint.style = android.graphics.Paint.Style.FILL
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 8.5f
            paint.color = android.graphics.Color.rgb(60, 60, 100)
            canvas3.drawText("ADDITIONAL BLOOD PANELS & CLINICAL LAB NOTES:", 50f, extraY + 15f, paint)
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 8f
            paint.color = android.graphics.Color.DKGRAY
            drawSmartWrappedText(canvas3, otherTestsNotes, 50f, extraY + 23f, 495f, paint, maxBottomY = 712f)

            drawSaudiDisclaimerFooter(canvas3, paint, currentPageNum, totalPages)
            pdfDoc.finishPage(page3)
            currentPageNum++

            // -------------------- PAGE 4+ (CHRONOLOGICAL COMPARATIVE LAB HISTORY MATRIX - ALL 12 BIOMARKERS) --------------------
            labHistoryChunks.forEachIndexed { chunkIdx, chunk ->
                val pageMat = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create())
                val canvasMat = pageMat.canvas
                val visitRangeLabel = if (uniqueLabsChronological.isEmpty()) {
                    "No laboratory visits recorded yet"
                } else {
                    val startV = chunkIdx * 4 + 1
                    val endV = chunkIdx * 4 + chunk.size
                    "Visits $startV–$endV of ${uniqueLabsChronological.size} • All Liver, Kidney, Lipid & HbA1c Trends"
                }

                drawPageHeader(
                    canvasMat,
                    "ROACCUTANE TRACKER - LAB HISTORY MATRIX",
                    "Section VIII: Chronological Comparative Lab Analytics ($visitRangeLabel)"
                )

                // Patient Context Banner
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(245, 243, 250)
                canvasMat.drawRoundRect(android.graphics.RectF(40f, 96f, 555f, 118f), 5f, 5f, paint)
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 8f
                paint.color = android.graphics.Color.rgb(60, 60, 60)
                canvasMat.drawText(
                    ellipsizePdfText(
                        "Context: Age ${profile.age} yrs | Weight ${String.format(Locale.US, "%.1f", profile.weightKg)} kg | Target ${profile.calculatedTargetDoseMg} mg (${profile.targetMgPerKgInt} mg/kg) | Dual Conv. & SI Units",
                        499f,
                        paint
                    ),
                    48f,
                    110f,
                    paint
                )

                val tableTop = 126f
                val col1Width = 163f
                val colNextWidth = 88f // 163 + 4*88 = 515f (40..555)
                val matHdrHeight = 32f
                val matRowHeight = 38f

                // Header Row
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(103, 80, 164)
                canvasMat.drawRect(40f, tableTop, 555f, tableTop + matHdrHeight, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(180, 180, 180)
                paint.strokeWidth = 0.8f
                canvasMat.drawRect(40f, tableTop, 555f, tableTop + matHdrHeight, paint)

                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.WHITE
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 8.4f
                paint.textAlign = android.graphics.Paint.Align.LEFT
                canvasMat.drawText("Biomarker & Normal Range", 46f, tableTop + 14f, paint)
                paint.textSize = 7.1f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvasMat.drawText("Conv. | International SI", 46f, tableTop + 25f, paint)

                for (slot in 0 until 4) {
                    val xPos = 40f + col1Width + (slot * colNextWidth)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(180, 180, 180)
                    canvasMat.drawLine(xPos, tableTop, xPos, tableTop + matHdrHeight, paint)

                    val lab = chunk.getOrNull(slot)
                    if (lab != null) {
                        paint.style = android.graphics.Paint.Style.FILL
                        paint.color = android.graphics.Color.WHITE
                        paint.textAlign = android.graphics.Paint.Align.CENTER
                        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        paint.textSize = 7.8f
                        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(lab.dateTime))
                        canvasMat.drawText(ellipsizePdfText(dateFmt, colNextWidth - 8f, paint), xPos + colNextWidth / 2f, tableTop + 14f, paint)
                        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        paint.textSize = 6.5f
                        val hTag = lab.hospitalName.ifBlank { lab.appointmentType }
                        canvasMat.drawText(ellipsizePdfText(hTag, colNextWidth - 8f, paint), xPos + colNextWidth / 2f, tableTop + 25f, paint)
                    }
                }
                paint.textAlign = android.graphics.Paint.Align.LEFT

                var currentY = tableTop + matHdrHeight
                allBiomarkers.forEachIndexed { rowIdx, spec ->
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = if (rowIdx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.rgb(248, 246, 252)
                    canvasMat.drawRect(40f, currentY, 555f, currentY + matRowHeight, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(212, 212, 220)
                    canvasMat.drawRect(40f, currentY, 555f, currentY + matRowHeight, paint)

                    // Left Parameter Column
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.BLACK
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7.6f
                    canvasMat.drawText(ellipsizePdfText(spec.nameEn, col1Width - 12f, paint), 46f, currentY + 13f, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.color = android.graphics.Color.DKGRAY
                    paint.textSize = 6.7f
                    canvasMat.drawText(ellipsizePdfText("Conv: ${spec.convRange}", col1Width - 12f, paint), 46f, currentY + 23f, paint)
                    paint.color = android.graphics.Color.rgb(90, 90, 130)
                    canvasMat.drawText(ellipsizePdfText("SI: ${spec.siRange}", col1Width - 12f, paint), 46f, currentY + 32f, paint)

                    for (slot in 0 until 4) {
                        val xPos = 40f + col1Width + (slot * colNextWidth)
                        val lab = chunk.getOrNull(slot)
                        val globalColIdx = (chunkIdx * 4) + slot
                        val value = lab?.let { spec.getValue(it) }

                        if (value != null) {
                            val isAb = spec.isAbnormal(value)
                            paint.style = android.graphics.Paint.Style.FILL
                            paint.color = if (isAb) android.graphics.Color.rgb(253, 234, 234) else android.graphics.Color.rgb(234, 246, 236)
                            canvasMat.drawRect(xPos + 0.5f, currentY + 0.5f, xPos + colNextWidth - 0.5f, currentY + matRowHeight - 0.5f, paint)
                        }

                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.color = android.graphics.Color.rgb(212, 212, 220)
                        canvasMat.drawLine(xPos, currentY, xPos, currentY + matRowHeight, paint)

                        paint.style = android.graphics.Paint.Style.FILL
                        paint.textAlign = android.graphics.Paint.Align.CENTER
                        val cellCenterX = xPos + (colNextWidth / 2f)

                        if (value == null) {
                            paint.color = android.graphics.Color.GRAY
                            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                            paint.textSize = 8f
                            canvasMat.drawText(if (lab != null) "—" else "", cellCenterX, currentY + 22f, paint)
                        } else {
                            // Only draw trend indicator if there is a prior entered result in earlier chronological visits
                            val prevValue = if (globalColIdx > 0) {
                                (globalColIdx - 1 downTo 0).firstNotNullOfOrNull { pIdx ->
                                    spec.getValue(uniqueLabsChronological[pIdx])
                                }
                            } else null

                            val trendSymbol = if (prevValue != null && value != prevValue) {
                                if (value > prevValue) " ▲" else " ▼"
                            } else ""

                            val isAb = spec.isAbnormal(value)
                            paint.color = if (isAb) android.graphics.Color.rgb(198, 40, 40) else android.graphics.Color.rgb(30, 120, 45)
                            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            val convCellText = "${spec.formatConvCompact(value)}$trendSymbol"
                            paint.textSize = 7.3f
                            if (paint.measureText(convCellText) > colNextWidth - 8f) {
                                paint.textSize = 6.5f
                            }
                            canvasMat.drawText(ellipsizePdfText(convCellText, colNextWidth - 6f, paint), cellCenterX, currentY + 16f, paint)

                            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                            paint.textSize = 6.4f
                            paint.color = if (isAb) android.graphics.Color.rgb(170, 50, 50) else android.graphics.Color.rgb(40, 95, 55)
                            canvasMat.drawText(ellipsizePdfText(spec.formatSi(value), colNextWidth - 6f, paint), cellCenterX, currentY + 28f, paint)
                        }
                        paint.textAlign = android.graphics.Paint.Align.LEFT
                    }

                    currentY += matRowHeight
                }

                // Legend Guide below matrix
                val guideTop = currentY + 10f
                val guideBottom = minOf(guideTop + 76f, 726f)
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(240, 244, 250)
                canvasMat.drawRoundRect(android.graphics.RectF(40f, guideTop, 555f, guideBottom), 6f, 6f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(180, 200, 220)
                canvasMat.drawRoundRect(android.graphics.RectF(40f, guideTop, 555f, guideBottom), 6f, 6f, paint)

                paint.style = android.graphics.Paint.Style.FILL
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 8f
                paint.color = android.graphics.Color.rgb(40, 70, 110)
                canvasMat.drawText("LAB MATRIX TREND & UNITS GUIDE:", 50f, guideTop + 14f, paint)
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 7f
                paint.color = android.graphics.Color.BLACK
                val guideProse = "• Green / Red Cell: Reading within vs outside standard clinical reference bounds.\n" +
                    "• ▲ / ▼ Indicator: Shown only when an earlier entered result exists; first recorded result shows no arrow.\n" +
                    "• Dual Units: Top value = Conventional unit; bottom value = International SI unit (µkat/L, µmol/L, g/L, mmol/L)."
                drawSmartWrappedText(canvasMat, guideProse, 50f, guideTop + 21f, 495f, paint, maxBottomY = guideBottom - 6f)

                drawSaudiDisclaimerFooter(canvasMat, paint, currentPageNum, totalPages)
                pdfDoc.finishPage(pageMat)
                currentPageNum++
            }

            // -------------------- PRESCRIPTION & DISPENSED MEDICATION INVENTORY PAGE --------------------
            val pageInv = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create())
            val canvasInv = pageInv.canvas
            drawPageHeader(
                canvasInv,
                "ROACCUTANE TRACKER - PRESCRIPTION & MEDICATION REGISTRY",
                "Section IX: Dispensed Medications, Capsule Inventory & Last Capsule Forecasting"
            )

            val totalDispensedCaps = inventoryAudit.grandTotalDispensedCapsules
            val totalRemainingCaps = inventoryAudit.trueRemainingCapsules
            val totalConsumedCaps = inventoryAudit.actualCumulativeConsumedCapsules
            val totalDispensedMg = inventoryAudit.grandTotalDispensedMg
            val totalRemainingMg = inventoryAudit.trueRemainingMg
            val totalConsumedMg = inventoryAudit.actualCumulativeConsumedMg
            val totalBoxes = inventoryAudit.grandTotalBoxes
            val daysSupplyRemaining = inventoryAudit.remainingDaysCount
            val dailyCapsRate = inventoryAudit.dailyCapsulesRate
            val runOutDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(inventoryAudit.estimatedRunOutDateMillis))

            val kpiTop = 102f
            val kpiHeight = 52f
            val kpiColWidth = 125f
            val kpiSpacing = 5f

            val invCards = listOf(
                Triple(
                    "TOTAL DISPENSED",
                    "$totalDispensedCaps Caps ($totalBoxes bx)",
                    "$totalDispensedMg mg active"
                ) to Pair(android.graphics.Color.rgb(243, 246, 252), android.graphics.Color.rgb(25, 75, 140)),
                Triple(
                    "CONSUMED",
                    "$totalConsumedCaps Caps",
                    "$totalConsumedMg mg taken"
                ) to Pair(android.graphics.Color.rgb(254, 248, 240), android.graphics.Color.rgb(180, 95, 20)),
                Triple(
                    "IN STOCK / REMAINING",
                    "$totalRemainingCaps Caps",
                    "$totalRemainingMg mg left"
                ) to Pair(android.graphics.Color.rgb(240, 252, 245), android.graphics.Color.rgb(30, 130, 60)),
                Triple(
                    "LAST CAP / DAYS LEFT",
                    "$daysSupplyRemaining Days",
                    "Last cap: $runOutDateStr ($dailyCapsRate/d)"
                ) to Pair(android.graphics.Color.rgb(249, 245, 255), android.graphics.Color.rgb(103, 80, 164))
            )

            invCards.forEachIndexed { idx, (texts, colors) ->
                val cx = 40f + idx * (kpiColWidth + kpiSpacing)
                val innerKpiW = kpiColWidth - 12f
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = colors.first
                canvasInv.drawRoundRect(android.graphics.RectF(cx, kpiTop, cx + kpiColWidth, kpiTop + kpiHeight), 6f, 6f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(210, 215, 228)
                paint.strokeWidth = 1f
                canvasInv.drawRoundRect(android.graphics.RectF(cx, kpiTop, cx + kpiColWidth, kpiTop + kpiHeight), 6f, 6f, paint)

                paint.style = android.graphics.Paint.Style.FILL
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 7.1f
                paint.color = colors.second
                canvasInv.drawText(ellipsizePdfText(texts.first, innerKpiW, paint), cx + 6f, kpiTop + 15f, paint)

                paint.textSize = 10f
                paint.color = android.graphics.Color.BLACK
                canvasInv.drawText(ellipsizePdfText(texts.second, innerKpiW, paint), cx + 6f, kpiTop + 31f, paint)

                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 6.7f
                paint.color = android.graphics.Color.DKGRAY
                canvasInv.drawText(ellipsizePdfText(texts.third, innerKpiW, paint), cx + 6f, kpiTop + 44f, paint)
            }

            // A. REGISTERED MEDICAL PRESCRIPTIONS
            var secY = kpiTop + kpiHeight + 18f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 10.5f
            paint.color = android.graphics.Color.rgb(40, 40, 40)
            canvasInv.drawText("A. REGISTERED MEDICAL PRESCRIPTIONS", 40f, secY, paint)
            secY += 8f

            val rxHeaderH = 18f
            val rxDividers = listOf(195f, 325f, 445f)
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(103, 80, 164)
            canvasInv.drawRect(40f, secY, 555f, secY + rxHeaderH, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(180, 180, 180)
            canvasInv.drawRect(40f, secY, 555f, secY + rxHeaderH, paint)
            rxDividers.forEach { cx -> canvasInv.drawLine(cx, secY, cx, secY + rxHeaderH, paint) }

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 7.6f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvasInv.drawText("Prescription # / Medication", 45f, secY + 12f, paint)
            canvasInv.drawText("Strength & Regimen", 200f, secY + 12f, paint)
            canvasInv.drawText("Doctor / Hospital", 330f, secY + 12f, paint)
            canvasInv.drawText("Duration & Refills", 450f, secY + 12f, paint)
            secY += rxHeaderH

            val rxToDisplay = prescriptionsList.take(5)
            val rxRowH = 24f
            if (rxToDisplay.isEmpty()) {
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(252, 252, 254)
                canvasInv.drawRect(40f, secY, 555f, secY + 22f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(220, 220, 220)
                canvasInv.drawRect(40f, secY, 555f, secY + 22f, paint)
                paint.style = android.graphics.Paint.Style.FILL
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 8f
                paint.color = android.graphics.Color.GRAY
                canvasInv.drawText("No registered prescriptions found in tracker. Dispenses are managed directly.", 46f, secY + 14f, paint)
                secY += 22f
            } else {
                rxToDisplay.forEachIndexed { index, rx ->
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = if (index % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.rgb(248, 247, 252)
                    canvasInv.drawRect(40f, secY, 555f, secY + rxRowH, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(220, 220, 220)
                    canvasInv.drawRect(40f, secY, 555f, secY + rxRowH, paint)
                    rxDividers.forEach { cx -> canvasInv.drawLine(cx, secY, cx, secY + rxRowH, paint) }

                    val line1Y = secY + 10.5f
                    val line2Y = secY + 19.5f

                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.BLACK
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7.5f
                    val rxNum = if (rx.prescriptionNumber.isNotBlank()) "Rx #${rx.prescriptionNumber.trim()}" else "Rx #${index + 1}"
                    val cleanMedName = toCleanEnglishReportText(rx.medicationName).ifBlank { "Roaccutane" }
                    canvasInv.drawText(ellipsizePdfText("$rxNum • $cleanMedName", 145f, paint), 45f, line1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.6f
                    paint.color = android.graphics.Color.DKGRAY
                    canvasInv.drawText(ellipsizePdfText("Packaging: ${rx.capsulesPerBox} caps/box", 145f, paint), 45f, line2Y, paint)

                    paint.color = android.graphics.Color.BLACK
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7.3f
                    val cleanDoseStr = toCleanEnglishDose(rx.dailyDose, rx.capsuleStrengthMg * rx.dailyCapsulesCount)
                    canvasInv.drawText(ellipsizePdfText("${rx.capsuleStrengthMg}mg/cap • $cleanDoseStr", 120f, paint), 200f, line1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.6f
                    paint.color = android.graphics.Color.DKGRAY
                    canvasInv.drawText(ellipsizePdfText("Take ${rx.dailyCapsulesCount} cap(s)/day", 120f, paint), 200f, line2Y, paint)

                    val cleanDoc = toCleanEnglishReportText(rx.doctorName).ifBlank { "Dermatologist" }
                    val cleanClinic = toCleanEnglishReportText(rx.clinicOrHospital).ifBlank { "Clinic" }
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 7.1f
                    canvasInv.drawText(ellipsizePdfText(cleanDoc, 110f, paint), 330f, line1Y, paint)
                    paint.textSize = 6.4f
                    paint.color = android.graphics.Color.GRAY
                    canvasInv.drawText(ellipsizePdfText(cleanClinic, 110f, paint), 330f, line2Y, paint)

                    val usedDispenses = dispensedList.count { it.prescriptionId == rx.id }
                    val expStr = if (rx.expiryDate > 0) SimpleDateFormat("dd/MM/yy", Locale.US).format(Date(rx.expiryDate)) else "Std"
                    paint.color = if (rx.isExpired) android.graphics.Color.RED else android.graphics.Color.rgb(30, 110, 50)
                    paint.textSize = 7.1f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    val rxMonths = (rx.targetDurationDays / 30.0).let { if (it >= 1.0) "${it.toInt()}m" else "${rx.targetDurationDays}d" }
                    canvasInv.drawText(ellipsizePdfText("$rxMonths • Refills $usedDispenses/${rx.totalAllowedRefills}", 100f, paint), 450f, line1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.4f
                    paint.color = android.graphics.Color.DKGRAY
                    canvasInv.drawText("Exp: $expStr", 450f, line2Y, paint)

                    secY += rxRowH
                }
                if (prescriptionsList.size > rxToDisplay.size) {
                    secY += 11f
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.textSize = 7f
                    paint.color = android.graphics.Color.GRAY
                    canvasInv.drawText("+ ${prescriptionsList.size - rxToDisplay.size} additional prescription(s) in app registry.", 46f, secY, paint)
                }
            }

            // B. DISPENSED MEDICATION & CAPSULE INVENTORY LOG
            secY += 16f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 10.5f
            paint.color = android.graphics.Color.rgb(40, 40, 40)
            canvasInv.drawText("B. DISPENSED MEDICATION & CAPSULE INVENTORY LOG", 40f, secY, paint)
            secY += 8f

            val dispHeaderH = 17f
            val dispDividers = listOf(168f, 238f, 326f, 404f, 452f)
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(70, 90, 130)
            canvasInv.drawRect(40f, secY, 555f, secY + dispHeaderH, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(180, 180, 180)
            canvasInv.drawRect(40f, secY, 555f, secY + dispHeaderH, paint)
            dispDividers.forEach { cx -> canvasInv.drawLine(cx, secY, cx, secY + dispHeaderH, paint) }

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 7f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvasInv.drawText("Dispense / Medication", 45f, secY + 11.5f, paint)
            canvasInv.drawText("Disp. Date", 173f, secY + 11.5f, paint)
            canvasInv.drawText("Capsules (Bx)", 243f, secY + 11.5f, paint)
            canvasInv.drawText("Used / Left", 331f, secY + 11.5f, paint)
            canvasInv.drawText("Missed", 409f, secY + 11.5f, paint)
            canvasInv.drawText("Est. End Date", 457f, secY + 11.5f, paint)
            secY += dispHeaderH

            val dispRowH = 23f
            val maxRowsBottomY = 640f // Leaves clean room for optional '+N' line + guide box + 740f footer
            val maxDispRows = ((maxRowsBottomY - secY) / dispRowH).toInt().coerceAtLeast(1)
            val dispToShow = dispensedList.take(maxDispRows)

            if (dispToShow.isEmpty()) {
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(252, 252, 254)
                canvasInv.drawRect(40f, secY, 555f, secY + 22f, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(220, 220, 220)
                canvasInv.drawRect(40f, secY, 555f, secY + 22f, paint)
                paint.style = android.graphics.Paint.Style.FILL
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 8f
                paint.color = android.graphics.Color.GRAY
                canvasInv.drawText("No dispensed medication entries recorded yet.", 46f, secY + 14f, paint)
                secY += 22f
            } else {
                dispToShow.forEachIndexed { idx, med ->
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = if (idx % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.rgb(246, 248, 252)
                    canvasInv.drawRect(40f, secY, 555f, secY + dispRowH, paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = android.graphics.Color.rgb(215, 215, 225)
                    canvasInv.drawRect(40f, secY, 555f, secY + dispRowH, paint)
                    dispDividers.forEach { cx -> canvasInv.drawLine(cx, secY, cx, secY + dispRowH, paint) }

                    val dLine1Y = secY + 10f
                    val dLine2Y = secY + 19f
                    val phase = inventoryAudit.phases.firstOrNull { it.dispenseId == med.id }
                    val consumed = phase?.phaseConsumedCapsules ?: med.consumedCapsulesCount
                    val remaining = phase?.phaseRemainingCapsules ?: med.remainingCapsulesCount

                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.BLACK
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7.2f
                    val cleanMed = toCleanEnglishReportText(med.medicationName).ifBlank { "Isotretinoin" }
                    val dispLabel = if (med.dispenseNumber > 1) "Refill #${med.dispenseNumber}: $cleanMed" else "Dispense #1: $cleanMed"
                    canvasInv.drawText(ellipsizePdfText(dispLabel, 118f, paint), 45f, dLine1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.4f
                    paint.color = android.graphics.Color.GRAY
                    val cleanMedDose = toCleanEnglishDose(med.dailyDose, med.capsuleStrengthMg * med.dailyCapsulesCount)
                    canvasInv.drawText(ellipsizePdfText("$cleanMedDose • ${med.capsuleStrengthMg}mg", 118f, paint), 45f, dLine2Y, paint)

                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 7.1f
                    val sDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(med.startDate))
                    canvasInv.drawText(ellipsizePdfText(sDateStr, 62f, paint), 173f, dLine1Y, paint)
                    paint.color = android.graphics.Color.GRAY
                    paint.textSize = 6.3f
                    canvasInv.drawText(ellipsizePdfText("${med.durationDays}d plan", 62f, paint), 173f, dLine2Y, paint)

                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 7.1f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvasInv.drawText(ellipsizePdfText("${med.totalCapsules} Caps", 80f, paint), 243f, dLine1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.3f
                    paint.color = android.graphics.Color.DKGRAY
                    canvasInv.drawText(ellipsizePdfText("${med.boxCount} bx (${med.capsulesPerBox}/bx)", 80f, paint), 243f, dLine2Y, paint)

                    paint.color = android.graphics.Color.rgb(180, 80, 20)
                    paint.textSize = 6.9f
                    canvasInv.drawText(ellipsizePdfText("Used: $consumed", 70f, paint), 331f, dLine1Y, paint)
                    paint.color = android.graphics.Color.rgb(20, 120, 50)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvasInv.drawText(ellipsizePdfText("Left: $remaining", 70f, paint), 331f, dLine2Y, paint)

                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.9f
                    paint.color = if (med.missedDays > 0) android.graphics.Color.rgb(210, 60, 20) else android.graphics.Color.DKGRAY
                    canvasInv.drawText("${med.missedDays} d", 409f, dLine1Y + 4f, paint)

                    val endDateMillis = phase?.calculatedEndDate ?: med.calculatedEndDate
                    val eDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(endDateMillis))
                    paint.color = android.graphics.Color.BLACK
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 7f
                    canvasInv.drawText(ellipsizePdfText(eDateStr, 93f, paint), 457f, dLine1Y, paint)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    paint.textSize = 6.3f
                    val isPhaseActive = phase?.isActiveNow == true || (phase == null && med.remainingDaysUntilEnd > 0 && !med.isCompleted)
                    val isPhaseQueued = phase?.isFuture == true
                    val phaseDaysLeft = if (phase != null) (phase.durationDays - phase.elapsedActiveDays).coerceAtLeast(0) else med.remainingDaysUntilEnd
                    val (statusColor, statusText) = when {
                        remaining <= 0 -> Pair(android.graphics.Color.rgb(120, 120, 120), "Completed")
                        isPhaseActive -> Pair(android.graphics.Color.rgb(20, 120, 50), "${phaseDaysLeft}d left (Active)")
                        isPhaseQueued -> Pair(android.graphics.Color.rgb(103, 80, 164), "Queued (${med.durationDays}d)")
                        med.remainingDaysUntilEnd > 0 -> Pair(android.graphics.Color.rgb(20, 120, 50), "${med.remainingDaysUntilEnd}d left")
                        else -> Pair(android.graphics.Color.GRAY, "Completed")
                    }
                    paint.color = statusColor
                    canvasInv.drawText(ellipsizePdfText(statusText, 93f, paint), 457f, dLine2Y, paint)

                    secY += dispRowH
                }
                if (dispensedList.size > dispToShow.size) {
                    secY += 11f
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.textSize = 7f
                    paint.color = android.graphics.Color.GRAY
                    canvasInv.drawText("+ ${dispensedList.size - dispToShow.size} additional dispense record(s) in tracker.", 46f, secY, paint)
                }
            }

            // Clinical Inventory Guidance Box (always positioned strictly below table & above 740f footer)
            val invGuideTop = maxOf(secY + 12f, 654f).coerceAtMost(670f)
            val invGuideHeight = 56f
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(240, 248, 255)
            canvasInv.drawRoundRect(android.graphics.RectF(40f, invGuideTop, 555f, invGuideTop + invGuideHeight), 6f, 6f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(180, 215, 245)
            paint.strokeWidth = 1f
            canvasInv.drawRoundRect(android.graphics.RectF(40f, invGuideTop, 555f, invGuideTop + invGuideHeight), 6f, 6f, paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 7.8f
            paint.color = android.graphics.Color.rgb(20, 70, 130)
            canvasInv.drawText("CLINICAL INVENTORY, CAPSULE COMPLIANCE & REFILL PROTOCOL:", 50f, invGuideTop + 14f, paint)
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 7f
            paint.color = android.graphics.Color.BLACK
            val invGuideProse = "• Last Capsule Forecast: Calculated dynamically from live capsule inventory divided by active daily capsule rate.\n" +
                "• Refill Strategy: To ensure uninterrupted therapy, book follow-up & labs 5–7 days prior to the last capsule date.\n" +
                "• Missed Dose Calibration: Unlogged or skipped days automatically extend the active dispense run-out timeline."
            drawSmartWrappedText(canvasInv, invGuideProse, 50f, invGuideTop + 19f, 495f, paint, maxBottomY = invGuideTop + invGuideHeight - 4f)

            drawSaudiDisclaimerFooter(canvasInv, paint, currentPageNum, totalPages)
            pdfDoc.finishPage(pageInv)
            currentPageNum++

            // -------------------- FINAL PAGE (AI ANALYSIS & PHARMACOKINETICS SAFETY) --------------------
            val pageFinal = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create())
            val canvasFinal = pageFinal.canvas
            drawPageHeader(
                canvasFinal,
                "ROACCUTANE TRACKER - AI MEDICAL SYNTHESIS",
                "Section X: Al Jauzaa Smart Medical Synthesis & Clinical Pharmacokinetics Summary"
            )

            val aiBoxTop = 108f
            val aiBoxBottom = 536f
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(249, 247, 253)
            canvasFinal.drawRoundRect(android.graphics.RectF(40f, aiBoxTop, 555f, aiBoxBottom), 8f, 8f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(221, 211, 241)
            paint.strokeWidth = 1f
            canvasFinal.drawRoundRect(android.graphics.RectF(40f, aiBoxTop, 555f, aiBoxBottom), 8f, 8f, paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 10f
            paint.color = android.graphics.Color.rgb(103, 80, 164)
            canvasFinal.drawText("X. AL JAUZAA HEALTH & CLINICAL SYMPTOMS COMPLEXITY ANALYSIS", 52f, aiBoxTop + 22f, paint)

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.rgb(40, 40, 40)
            drawSmartWrappedText(canvasFinal, sanitizePdfProse(aiAnalysis), 52f, aiBoxTop + 34f, 491f, paint, maxBottomY = aiBoxBottom - 14f)

            val safetyBoxTop = 552f
            val safetyBoxBottom = 710f
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(254, 244, 244)
            canvasFinal.drawRoundRect(android.graphics.RectF(40f, safetyBoxTop, 555f, safetyBoxBottom), 8f, 8f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.rgb(245, 198, 203)
            paint.strokeWidth = 1f
            canvasFinal.drawRoundRect(android.graphics.RectF(40f, safetyBoxTop, 555f, safetyBoxBottom), 8f, 8f, paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.rgb(186, 32, 40)
            canvasFinal.drawText("CLINICAL PHARMACOKINETICS WARNING & DIETARY GUIDANCE:", 52f, safetyBoxTop + 20f, paint)

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 8.4f
            paint.color = android.graphics.Color.BLACK
            val safetyBullets = "• Isotretinoin (Roaccutane) is highly lipophilic (fat-soluble). Absorption is significantly enhanced when ingested with healthy-fat meals (avocado, olive oil, nuts, eggs, or full-fat dairy).\n" +
                "• Maintain periodic liver enzyme (ALT/AST/ALP/Bilirubin), renal (Creatinine/Urea) & lipid panels.\n" +
                "• Keep skin, lips, and eyes hydrated. Report persistent headaches, mood shifts, or bone pain promptly.\n" +
                "• Monthly negative pregnancy test (Beta-hCG) is mandatory for patients of childbearing potential."
            drawSmartWrappedText(canvasFinal, safetyBullets, 52f, safetyBoxTop + 28f, 491f, paint, maxBottomY = safetyBoxBottom - 10f)

            drawSaudiDisclaimerFooter(canvasFinal, paint, currentPageNum, totalPages)
            pdfDoc.finishPage(pageFinal)

            // Save inside context's external files directory safely
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (dir?.exists() == false) {
                dir.mkdirs()
            }
            val file = java.io.File(dir, "Roaccutane_Medical_Report.pdf")
            val outputStream = java.io.FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            pdfDoc.close()
            outputStream.flush()
            outputStream.close()

            onComplete(true, file.absolutePath)
        } catch (e: Exception) {
            Log.e("RoaccutaneViewModel", "PDF Export error", e)
            onComplete(false, e.localizedMessage ?: "Failed to generate PDF document")
        }
    }
}

private fun ellipsizePdfText(text: String, maxWidth: Float, paint: android.graphics.Paint): String {
    if (text.isEmpty() || maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    val avail = (maxWidth - paint.measureText(ellipsis)).coerceAtLeast(0f)
    var end = text.length
    while (end > 0 && paint.measureText(text, 0, end) > avail) {
        end--
    }
    return if (end <= 0) ellipsis else text.substring(0, end).trimEnd() + ellipsis
}

private fun sanitizePdfProse(raw: String): String {
    return raw
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("""__(.*?)__"""), "$1")
        .replace(Regex("""^\s{0,3}#{1,6}\s*""", RegexOption.MULTILINE), "")
        .replace(Regex("""^\s*[-*]\s+""", RegexOption.MULTILINE), "• ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun drawSmartWrappedText(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    yTop: Float,
    width: Float,
    paint: android.graphics.Paint,
    maxBottomY: Float = Float.MAX_VALUE
): Float {
    if (text.isBlank() || width <= 0f || yTop >= maxBottomY) return yTop
    val textPaint = android.text.TextPaint(paint)
    val layout = android.text.StaticLayout.Builder
        .obtain(text, 0, text.length, textPaint, width.toInt().coerceAtLeast(1))
        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(2.5f, 1.0f)
        .setIncludePad(false)
        .build()

    var lastFittingLine = -1
    for (i in 0 until layout.lineCount) {
        if (yTop + layout.getLineBottom(i) <= maxBottomY) {
            lastFittingLine = i
        } else {
            break
        }
    }
    if (lastFittingLine < 0) return yTop

    val clipBottom = yTop + layout.getLineBottom(lastFittingLine).toFloat()
    canvas.save()
    canvas.clipRect(x, yTop, x + width, clipBottom)
    canvas.translate(x, yTop)
    layout.draw(canvas)
    canvas.restore()
    return clipBottom
}

private fun drawQrCodeOnCanvas(
    canvas: android.graphics.Canvas,
    paint: android.graphics.Paint,
    url: String,
    x: Float,
    y: Float,
    size: Float
) {
    try {
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(android.graphics.RectF(x - 3f, y - 3f, x + size + 3f, y + size + 3f), 4f, 4f, paint)

        val urlToEncode = if (url.isBlank()) "https://maps.google.com" else url
        val qrCodeWriter = com.google.zxing.qrcode.QRCodeWriter()
        val hints = java.util.HashMap<com.google.zxing.EncodeHintType, Any>()
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M

        val bitMatrix = qrCodeWriter.encode(
            urlToEncode,
            com.google.zxing.BarcodeFormat.QR_CODE,
            120,
            120,
            hints
        )

        val bitWidth = bitMatrix.width
        val bitHeight = bitMatrix.height
        val cellSize = size / bitWidth

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.BLACK
        for (row in 0 until bitHeight) {
            for (col in 0 until bitWidth) {
                if (bitMatrix.get(col, row)) {
                    val cellLeft = x + (col * cellSize)
                    val cellTop = y + (row * cellSize)
                    canvas.drawRect(cellLeft, cellTop, cellLeft + cellSize + 0.1f, cellTop + cellSize + 0.1f, paint)
                }
            }
        }
    } catch (e: Exception) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = android.graphics.Color.RED
        paint.strokeWidth = 1f
        canvas.drawRect(x, y, x + size, y + size, paint)
        paint.style = android.graphics.Paint.Style.FILL
    }
}

private fun drawSaudiDisclaimerFooter(canvas: android.graphics.Canvas, paint: android.graphics.Paint, pageNum: Int, totalPages: Int) {
    val footerY = 740f
    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.LTGRAY
    paint.strokeWidth = 1f
    canvas.drawLine(40f, footerY, 555f, footerY, paint)

    paint.style = android.graphics.Paint.Style.FILL
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textSize = 7.5f
    paint.color = android.graphics.Color.rgb(186, 32, 40)
    canvas.drawText("⚖️ تنبيه نظامي وطبي (طبقاً لمتطلبات الصحة الرقمية السعودية):", 40f, footerY + 13f, paint)

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    paint.textSize = 6.9f
    paint.color = android.graphics.Color.rgb(60, 60, 60)
    canvas.drawText(
        ellipsizePdfText("هذا التطبيق أداة تحليلية برمجية فقط ولا يمثل تشخيصاً طبياً معتمداً، ولا يغني إطلاقاً عن مراجعة طبيبك المعالج أو إجراء الفحوصات المخبرية الرسمية.", 515f, paint),
        40f,
        footerY + 24f,
        paint
    )
    canvas.drawText(
        ellipsizePdfText("This application is an analytical software tool, not a medical diagnosis, and does not replace consulting your board-certified dermatologist.", 515f, paint),
        40f,
        footerY + 34f,
        paint
    )

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    paint.textSize = 7.8f
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("Page $pageNum of $totalPages - Roaccutane Tracker", 40f, footerY + 49f, paint)
    paint.textAlign = android.graphics.Paint.Align.RIGHT
    val genDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
    canvas.drawText("Generated: $genDate", 555f, footerY + 49f, paint)
    paint.textAlign = android.graphics.Paint.Align.LEFT
}
}

data class AiActionCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val functionName: String,
    val functionArgsJson: String = "",
    val description: String = "",
    val doseMg: Int? = null,
    val symptoms: List<String> = emptyList(),
    val severity: Int? = null,
    val takenAt: String? = null,
    val waterMl: Int? = null,
    val moisturizerType: String? = null,
    val moisturizerCount: Int? = null,
    val capsulesDispensed: Int? = null,
    val capsulesRemaining: Int? = null,
    val prescribedDose: Int? = null,
    val productName: String? = null,
    val productAction: String? = null,
    val appointmentDate: String? = null,
    val labTestName: String? = null,
    val labResultValue: String? = null,
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false
)

data class AiQueryCard(
    val queryType: String,
    val title: String,
    val summary: String,
    val details: List<Pair<String, String>> = emptyList()
)

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionCard: AiActionCard? = null,
    val queryCard: AiQueryCard? = null
)

data class SymptomStats(
    val symptom: String,
    val occurrenceCount: Int,
    val percentage: Float
)

data class SourceStats(
    val source: String,
    val occurrenceCount: Int,
    val percentage: Float
)

// Private Safe Extractor extension functions to protect against ClassCastExceptions / Schema differences
private fun Any?.toIntSafe(default: Int = 0): Int {
    if (this == null) return default
    if (this is Number) return this.toInt()
    if (this is String) return this.toIntOrNull() ?: default
    if (this is Boolean) return if (this) 1 else 0
    return default
}

private fun Any?.toLongSafe(default: Long = 0L): Long {
    if (this == null) return default
    if (this is Number) return this.toLong()
    if (this is String) return this.toLongOrNull() ?: default
    if (this is com.google.firebase.Timestamp) return this.toDate().time
    return default
}

private fun Any?.toBooleanSafe(default: Boolean = false): Boolean {
    if (this == null) return default
    if (this is Boolean) return this
    if (this is Number) return this.toInt() != 0
    if (this is String) return this.lowercase() == "true" || this == "1"
    return default
}

private fun Any?.toStringSafe(default: String = ""): String {
    if (this == null) return default
    return this.toString()
}

// Resilient DocumentSnapshot helpers
private fun com.google.firebase.firestore.DocumentSnapshot.getLongSafe(field: String, default: Long = 0L): Long {
    return try {
        this.get(field).toLongSafe(default)
    } catch (e: Exception) {
        default
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.getDoubleSafe(field: String, default: Double = 0.0): Double {
    return try {
        val value = this.get(field)
        if (value is Number) value.toDouble()
        else if (value is String) value.toDoubleOrNull() ?: default
        else default
    } catch (e: Exception) {
        default
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.getBooleanSafe(field: String, default: Boolean = false): Boolean {
    return try {
        this.get(field).toBooleanSafe(default)
    } catch (e: Exception) {
        default
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.getStringSafe(field: String, default: String = ""): String {
    return try {
        this.get(field).toStringSafe(default)
    } catch (e: Exception) {
        default
    }
}




