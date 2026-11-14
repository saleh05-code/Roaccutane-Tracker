package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class RoaccutaneRepository(private val dao: RoaccutaneDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfile().flowOn(Dispatchers.IO)
    val allLogs: Flow<List<DailyLog>> = dao.getAllLogs().flowOn(Dispatchers.IO)
    val allAppointments: Flow<List<Appointment>> = dao.getAllAppointments().flowOn(Dispatchers.IO)

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        dao.insertUserProfile(profile)
    }

    suspend fun saveLog(log: DailyLog) = withContext(Dispatchers.IO) {
        dao.insertLog(log)
    }

    suspend fun deleteLog(dateString: String) = withContext(Dispatchers.IO) {
        dao.deleteLogByDate(dateString)
    }

    suspend fun saveAppointment(appointment: Appointment): Long = withContext(Dispatchers.IO) {
        dao.insertAppointment(appointment)
    }

    suspend fun deleteAppointment(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteAppointment(id)
    }

    suspend fun deleteAllAppointments() = withContext(Dispatchers.IO) {
        dao.deleteAllAppointments()
    }

    // --- Vital Signs ---
    val allVitalSigns: Flow<List<VitalSign>> = dao.getAllVitalSigns().flowOn(Dispatchers.IO)
    suspend fun saveVitalSign(vital: VitalSign) = withContext(Dispatchers.IO) {
        dao.insertVitalSign(vital)
    }

    // --- Clinics ---
    val allClinics: Flow<List<Clinic>> = dao.getAllClinics().flowOn(Dispatchers.IO)
    suspend fun saveClinic(clinic: Clinic) = withContext(Dispatchers.IO) {
        dao.insertClinic(clinic)
    }
    suspend fun updateClinicFrozen(clinicId: String, isFrozen: Boolean) = withContext(Dispatchers.IO) {
        dao.updateClinicFrozen(clinicId, isFrozen)
    }
    suspend fun updateClinicTrial(clinicId: String, trialEndDate: Long) = withContext(Dispatchers.IO) {
        dao.updateClinicTrial(clinicId, trialEndDate)
    }
    suspend fun updateClinicLogo(clinicId: String, logoResName: String) = withContext(Dispatchers.IO) {
        dao.updateClinicLogo(clinicId, logoResName)
    }

    // --- Doctors ---
    val allDoctors: Flow<List<Doctor>> = dao.getAllDoctors().flowOn(Dispatchers.IO)
    fun getDoctorsByClinic(clinicId: String): Flow<List<Doctor>> = dao.getDoctorsByClinic(clinicId).flowOn(Dispatchers.IO)
    suspend fun saveDoctor(doctor: Doctor) = withContext(Dispatchers.IO) {
        dao.insertDoctor(doctor)
    }
    suspend fun updateDoctorBlocked(doctorId: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateDoctorBlocked(doctorId, isBlocked)
    }
    suspend fun reassignClinicPatients(clinicId: String, oldDoctorId: String, newDoctorId: String) = withContext(Dispatchers.IO) {
        dao.reassignClinicPatients(clinicId, oldDoctorId, newDoctorId)
    }

    // --- Patient Pairings ---
    val allPairings: Flow<List<PatientPairing>> = dao.getAllPairings().flowOn(Dispatchers.IO)
    fun getPairingsForDoctor(doctorId: String): Flow<List<PatientPairing>> = dao.getPairingsForDoctor(doctorId).flowOn(Dispatchers.IO)
    fun getPairingsForClinic(clinicId: String): Flow<List<PatientPairing>> = dao.getPairingsForClinic(clinicId).flowOn(Dispatchers.IO)
    suspend fun savePairing(pairing: PatientPairing) = withContext(Dispatchers.IO) {
        dao.insertPairing(pairing)
    }
    suspend fun updatePairingStatus(id: Int, status: String) = withContext(Dispatchers.IO) {
        dao.updatePairingStatus(id, status)
    }

    // --- Doctor Notes ---
    val allDoctorNotes: Flow<List<DoctorNote>> = dao.getAllDoctorNotes().flowOn(Dispatchers.IO)
    fun getNotesForPatient(patientId: String): Flow<List<DoctorNote>> = dao.getNotesForPatient(patientId).flowOn(Dispatchers.IO)
    suspend fun saveDoctorNote(note: DoctorNote) = withContext(Dispatchers.IO) {
        dao.insertDoctorNote(note)
    }
    suspend fun deleteDoctorNote(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteDoctorNote(id)
    }

    // --- Financial Records ---
    val allFinancialRecords: Flow<List<FinancialRecord>> = dao.getAllFinancialRecords().flowOn(Dispatchers.IO)
    suspend fun saveFinancialRecord(record: FinancialRecord) = withContext(Dispatchers.IO) {
        dao.insertFinancialRecord(record)
    }
    suspend fun deleteFinancialRecord(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteFinancialRecord(id)
    }

    // --- Feature Usage ---
    val allFeatureUsage: Flow<List<FeatureUsageRecord>> = dao.getAllFeatureUsage().flowOn(Dispatchers.IO)
    suspend fun logFeatureUsage(featureKey: String, estimatedCostSAR: Double = 0.05) = withContext(Dispatchers.IO) {
        val curMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
        dao.insertFeatureUsage(
            FeatureUsageRecord(
                featureKey = featureKey,
                count = 1,
                estimatedCostSAR = estimatedCostSAR,
                periodMonthYear = curMonth
            )
        )
    }

    // --- Subscription Plans ---
    val allSubscriptionPlans: Flow<List<SubscriptionPlanConfig>> = dao.getAllSubscriptionPlans().flowOn(Dispatchers.IO)
    suspend fun saveSubscriptionPlan(plan: SubscriptionPlanConfig) = withContext(Dispatchers.IO) {
        dao.insertSubscriptionPlan(plan)
    }
    suspend fun updatePlanPrices(planId: String, priceMonthly: Double, priceAnnual: Double) = withContext(Dispatchers.IO) {
        dao.updatePlanPrices(planId, priceMonthly, priceAnnual)
    }
    suspend fun updatePlanFeatures(planId: String, isAiEnabled: Boolean, dailyAiLimit: Int, maxPdfExports: Int) = withContext(Dispatchers.IO) {
        dao.updatePlanFeatures(planId, isAiEnabled, dailyAiLimit, maxPdfExports)
    }

    // --- Audit Logs ---
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs().flowOn(Dispatchers.IO)
    suspend fun logAudit(actorRole: String, actorName: String, action: String, details: String) = withContext(Dispatchers.IO) {
        dao.insertAuditLog(
            AuditLog(
                actorRole = actorRole,
                actorName = actorName,
                action = action,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // --- Affiliate Products ---
    val allAffiliateProducts: Flow<List<AffiliateProduct>> = dao.getAllAffiliateProducts().flowOn(Dispatchers.IO)
    suspend fun saveAffiliateProduct(product: AffiliateProduct) = withContext(Dispatchers.IO) {
        dao.insertAffiliateProduct(product)
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "ADD_PRODUCT",
                title = "إضافة منتج موصى به",
                details = "تمت إضافة المنتج: ${product.productNameAr} بسعر ${product.priceSAR} ر.س"
            )
        )
    }
    suspend fun deleteAffiliateProduct(id: String, name: String = "") = withContext(Dispatchers.IO) {
        dao.deleteAffiliateProduct(id)
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "DELETE_PRODUCT",
                title = "حذف منتج",
                details = "تم حذف المنتج (معرف: $id) ${if (name.isNotBlank()) "- $name" else ""}"
            )
        )
    }
    suspend fun updateAffiliateRank(id: String, rankOrder: Int) = withContext(Dispatchers.IO) {
        dao.updateAffiliateRank(id, rankOrder)
    }

    // --- Owner Changes ---
    val allOwnerChanges: Flow<List<OwnerChangeRecord>> = dao.getAllOwnerChanges().flowOn(Dispatchers.IO)
    suspend fun logOwnerChange(actionType: String, title: String, details: String) = withContext(Dispatchers.IO) {
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = actionType,
                title = title,
                details = details
            )
        )
    }

    // --- Dispensed Medications ---
    val allDispensedMedications: Flow<List<DispensedMedication>> = dao.getAllDispensedMedications().flowOn(Dispatchers.IO)

    suspend fun saveDispensedMedication(item: DispensedMedication) = withContext(Dispatchers.IO) {
        // Inherit prescriptionUrl from parent prescription if available and blank on dispense
        val resolvedItem = if (item.prescriptionUrl.isBlank() && !item.prescriptionId.isNullOrBlank()) {
            val parentRx = dao.getMedicalPrescriptionById(item.prescriptionId)
            if (parentRx != null && parentRx.prescriptionUrl.isNotBlank()) {
                item.copy(prescriptionUrl = parentRx.prescriptionUrl)
            } else {
                item
            }
        } else {
            item
        }
        dao.insertDispensedMedication(resolvedItem)
    }

    suspend fun deleteDispensedMedication(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDispensedMedication(id)
    }

    suspend fun updateDispensedMedicationMissedDays(id: String, missedDays: Int) = withContext(Dispatchers.IO) {
        dao.updateDispensedMedicationMissedDays(id, missedDays)
    }

    suspend fun deleteAllDispensedMedications() = withContext(Dispatchers.IO) {
        dao.deleteAllDispensedMedications()
    }

    // --- Medical Prescriptions ---
    val allMedicalPrescriptions: Flow<List<MedicalPrescription>> = dao.getAllMedicalPrescriptions().flowOn(Dispatchers.IO)

    suspend fun saveMedicalPrescription(item: MedicalPrescription) = withContext(Dispatchers.IO) {
        dao.insertMedicalPrescription(item)
        // Cascading update: All dispense records belonging to this prescription inherit prescription_url
        if (item.prescriptionUrl.isNotBlank()) {
            dao.updateDispensedMedicationsPrescriptionUrl(item.id, item.prescriptionUrl.trim())
        }
    }

    suspend fun deleteMedicalPrescription(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMedicalPrescription(id)
    }

    suspend fun deleteAllMedicalPrescriptions() = withContext(Dispatchers.IO) {
        dao.deleteAllMedicalPrescriptions()
    }

    /**
     * Strict Scope Constraint: Purges legacy direct blob/attachment files strictly and exclusively
     * belonging to medical prescriptions folder to free device storage.
     * NEVER touches or deletes any files or records from other tables/features.
     */
    suspend fun purgeLegacyPrescriptionFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            val internalRxDir = java.io.File(context.filesDir, "prescriptions")
            if (internalRxDir.exists() && internalRxDir.isDirectory) {
                internalRxDir.listFiles()?.forEach { file ->
                    try { file.delete() } catch (_: Exception) {}
                }
            }
            val extRxDir = java.io.File(context.getExternalFilesDir(null), "prescriptions")
            if (extRxDir.exists() && extRxDir.isDirectory) {
                extRxDir.listFiles()?.forEach { file ->
                    try { file.delete() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun updateDoctorProfile(
        id: String,
        name: String,
        specialty: String,
        phone: String,
        bio: String,
        fee: Double,
        signatureUri: String,
        stampUri: String,
        licenseUri: String
    ) = withContext(Dispatchers.IO) {
        dao.updateDoctorProfile(id, name, specialty, phone, bio, fee, signatureUri, stampUri, licenseUri)
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "UPDATE_DOCTOR_PROFILE",
                title = "تحديث الملف المهني للطبيب",
                details = "تم تحديث بيانات الطبيب $name والمستندات المرفقة (التوقيع، الختم، والترخيص)"
            )
        )
    }

    suspend fun updateClinicDetails(id: String, name: String, email: String, phone: String, logo: String) = withContext(Dispatchers.IO) {
        val currentClinic = dao.getAllClinics().firstOrNull()?.firstOrNull { it.id == id }
        dao.updateClinicDetails(id, name, email, phone, logo)
        val changedFields = mutableListOf<String>()
        if (currentClinic != null) {
            if (currentClinic.name != name) changedFields.add("الاسم: $name")
            if (currentClinic.contactPhone != phone) changedFields.add("الهاتف: $phone")
            if (currentClinic.contactEmail != email) changedFields.add("البريد: $email")
            if (currentClinic.logoResName != logo) changedFields.add("شعار المنشأة")
        }
        val detailsStr = if (changedFields.isNotEmpty()) {
            "العيادة: ${currentClinic?.name ?: name} - التعديلات: " + changedFields.joinToString("، ")
        } else {
            "العيادة: $name - تم تأكيد البيانات دون تعديلات إضافية"
        }
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "UPDATE_CLINIC_DETAILS",
                title = "تحديث بيانات عيادة",
                details = detailsStr
            )
        )
    }

    suspend fun renewClinicSubscription(clinicId: String, daysToAdd: Int = 30) = withContext(Dispatchers.IO) {
        val clinic = dao.getAllClinics().firstOrNull()?.firstOrNull { it.id == clinicId }
        val clinicName = clinic?.name ?: clinicId
        val currentTrial = clinic?.trialEndDate ?: System.currentTimeMillis()
        val baseTime = maxOf(System.currentTimeMillis(), currentTrial)
        val extendedTrial = baseTime + (daysToAdd.toLong() * 24 * 3600 * 1000)
        dao.updateClinicFrozen(clinicId, false)
        dao.updateClinicTrial(clinicId, extendedTrial)
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "RENEW_CLINIC_SUBSCRIPTION",
                title = "تجديد اشتراك عيادة",
                details = "العيادة: $clinicName - تم تمديد الاشتراك بنجاح لمدة $daysToAdd يوماً إضافية"
            )
        )
    }

    suspend fun reactivateClinicSubscription(clinicId: String) = withContext(Dispatchers.IO) {
        renewClinicSubscription(clinicId, 30)
    }

    suspend fun updatePlanFeaturesList(planId: String, featuresList: String) = withContext(Dispatchers.IO) {
        dao.updatePlanFeaturesList(planId, featuresList)
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "UPDATE_PLAN_FEATURES_LIST",
                title = "تحديث مزايا باقة الاشتراك",
                details = "تم تحديث قائمة مزايا الباقة $planId لتشمل: $featuresList"
            )
        )
    }

    suspend fun deletePairing(id: Int, patientName: String) = withContext(Dispatchers.IO) {
        dao.deletePairing(id)
        dao.insertOwnerChange(
            OwnerChangeRecord(
                actionType = "UNLINK_PATIENT",
                title = "فك ربط مريض",
                details = "تم فك ربط المريض $patientName (معرف: $id)"
            )
        )
    }

    // --- Pre-populate Seed Data If Empty ---
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        try {
            // Clean up any legacy template/mock records so only real user-entered data remains
            dao.deleteTemplateDoctorNotes()

            val existingPlans = dao.getAllSubscriptionPlans().firstOrNull() ?: emptyList()
            if (existingPlans.isEmpty()) {
                // Only provide standard tier configuration schemas
                dao.insertSubscriptionPlan(
                    SubscriptionPlanConfig(
                        id = "FREE",
                        nameAr = "الباقة المجانية للأفراد",
                        nameEn = "Free Individual",
                        priceMonthlySAR = 0.0,
                        priceAnnualSAR = 0.0,
                        isAiEnabled = true,
                        dailyAiLimit = 5,
                        maxPdfExports = 2,
                        trialDays = 0,
                        isPublished = true
                    )
                )
                dao.insertSubscriptionPlan(
                    SubscriptionPlanConfig(
                        id = "PATIENT_PRO",
                        nameAr = "باقة المريض الذهبية (Pro)",
                        nameEn = "Patient Pro",
                        priceMonthlySAR = 29.0,
                        priceAnnualSAR = 249.0,
                        isAiEnabled = true,
                        dailyAiLimit = 50,
                        maxPdfExports = 999,
                        trialDays = 7,
                        isPublished = true
                    )
                )
                dao.insertSubscriptionPlan(
                    SubscriptionPlanConfig(
                        id = "CLINIC_STANDARD",
                        nameAr = "باقة العيادة الأساسية",
                        nameEn = "Clinic Standard",
                        priceMonthlySAR = 499.0,
                        priceAnnualSAR = 4990.0,
                        isAiEnabled = true,
                        dailyAiLimit = 200,
                        maxPdfExports = 999,
                        trialDays = 14,
                        isPublished = true
                    )
                )
                dao.insertSubscriptionPlan(
                    SubscriptionPlanConfig(
                        id = "CLINIC_ENTERPRISE",
                        nameAr = "باقة المراكز والمستشفيات المتقدمة",
                        nameEn = "Clinic Enterprise",
                        priceMonthlySAR = 899.0,
                        priceAnnualSAR = 8990.0,
                        isAiEnabled = true,
                        dailyAiLimit = 1000,
                        maxPdfExports = 999,
                        trialDays = 30,
                        isPublished = true
                    )
                )
            }

            val existingAffiliates = dao.getAllAffiliateProducts().firstOrNull() ?: emptyList()
            if (existingAffiliates.isEmpty()) {
                dao.insertAffiliateProduct(
                    AffiliateProduct(
                        id = "prod_cerave_cream",
                        partnerName = "iHerb",
                        productNameAr = "كريم مرطب سيرافي للبشرة الجافة إلى شديدة الجفاف",
                        descriptionAr = "غني بالسيراميدات وحمض الهيالورونيك لدعم الحاجز الواقي للبشرة أثناء علاج الرواكتان.",
                        categoryAr = "ترطيب الوجه",
                        priceSAR = 85.50,
                        commissionPercent = 12.0,
                        url = "https://sa.iherb.com/search?kw=cerave%20moisturizing%20cream",
                        imageUri = "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&q=80",
                        manufacturerUrl = "https://www.cerave.com/skincare/moisturizers/moisturizing-cream",
                        pharmacyUrl = "https://www.nahdionline.com/ar/cerave-moisturising-cream"
                    )
                )
                dao.insertAffiliateProduct(
                    AffiliateProduct(
                        id = "prod_bepanthen_lips",
                        partnerName = "نهدي",
                        productNameAr = "مرطب الشفاه بيبانثين ديرما المرمم",
                        descriptionAr = "تركيبة مركزة بالبرو فيتامين B5 لتغذية وتهدئة الشفاه شديدة التشقق والجفاف.",
                        categoryAr = "ترطيب الشفاه",
                        priceSAR = 28.75,
                        commissionPercent = 10.0,
                        url = "https://www.nahdionline.com/ar/bepanthen-derma-lip-cream",
                        imageUri = "https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=500&q=80",
                        manufacturerUrl = "https://www.bepanthen.me/en/products/bepanthen-derma-restoring-lip-cream",
                        pharmacyUrl = "https://www.nahdionline.com/ar/bepanthen-derma-lip-cream"
                    )
                )
                dao.insertAffiliateProduct(
                    AffiliateProduct(
                        id = "prod_laroche_sunscreen",
                        partnerName = "أمازون السعودية",
                        productNameAr = "واقي شمس لاروش بوزيه أنثيليوس SPF 50+",
                        descriptionAr = "حماية فائقة غير دهنية ضد الأشعة فوق البنفسجية، أساسي لمنع التصبغات أثناء العلاج.",
                        categoryAr = "حماية من الشمس",
                        priceSAR = 124.00,
                        commissionPercent = 8.0,
                        url = "https://www.amazon.sa/s?k=la+roche+posay+anthelios",
                        imageUri = "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500&q=80",
                        manufacturerUrl = "https://www.laroche-posay.me/en-sa/anthelios",
                        pharmacyUrl = "https://www.al-dawaa.com/arabic/la-roche-posay-anthelios"
                    )
                )
                dao.insertAffiliateProduct(
                    AffiliateProduct(
                        id = "prod_systane_drops",
                        partnerName = "الدواء",
                        productNameAr = "قطرة مرطبة للعين سيستان ألترا خالية من المواد الحافظة",
                        descriptionAr = "لتخفيف جفاف وحرقة العين المصاحبة لتناول دواء الرواكتان طوال اليوم.",
                        categoryAr = "ترطيب العين",
                        priceSAR = 42.00,
                        commissionPercent = 10.0,
                        url = "https://www.al-dawaa.com/arabic/systane-ultra-eye-drops",
                        imageUri = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&q=80",
                        manufacturerUrl = "https://systane.myalcon.com",
                        pharmacyUrl = "https://www.al-dawaa.com/arabic/systane-ultra-eye-drops"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("RoaccutaneRepository", "Error setting up platform config: ${e.message}", e)
        }
    }

    // --- Gemini Jauzaa AI API Call ---
    suspend fun askJauzaaAI(
        apiKey: String,
        userQuery: String,
        contextInfo: String,
        previousChat: List<GeminiContent> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val systemMsg = "أنت مساعد طبي ذكي متخصص في علاج الرواكتان (Roaccutane). " +
                "يجب أن تكون إجاباتك مختصرة جداً، علمية، ومبنية على المصادر الطبية الموثقة والحديثة فقط. " +
                "لا تقم بالإسهاب، ولا تقدم نصائح خارج نطاق متابعة الكورس. " +
                "إذا كان السؤال غير متعلق بالطب، جاوب عليه باخلاقية ومسؤولية واختصار شديد ومباشر بعد تأكيد عدم صلة السؤال بالجانب الطبي. " +
                "في نهاية الإجابة، اذكر اسم المصادر الطبية الحديثة التي اعتمدت عليها في الإجابة فقط دون أي وصف أو جمل إضافية، " +
                "وضمن رابطاً تشعبياً بنفس اسم المصدر تماماً للذهاب إلى صفحته الرسمية. " +
                "مثال للمصادر: [PubMed](https://pubmed.ncbi.nlm.nih.gov) أو [Mayo Clinic](https://www.mayoclinic.org) أو [NHS](https://www.nhs.uk) أو [FDA](https://www.fda.gov) أو [WebMD](https://www.webmd.com)."

        try {
            Log.d("RoaccutaneRepository", "Attempting Firebase Vertex AI call...")
            val firebaseApp = com.google.firebase.FirebaseApp.getInstance()
            val vertexAI = com.google.firebase.vertexai.FirebaseVertexAI.getInstance(firebaseApp)
            val model = vertexAI.generativeModel(
                modelName = "gemini-1.5-flash",
                systemInstruction = com.google.firebase.vertexai.type.content {
                    text(systemMsg)
                }
            )
            
            val promptBuilder = StringBuilder()
            if (contextInfo.isNotBlank()) {
                promptBuilder.append("سياق حالة المستخدم الحالية: $contextInfo\n\n")
            }
            previousChat.forEach { chat ->
                val partText = chat.parts.firstOrNull()?.text ?: ""
                promptBuilder.append("- $partText\n")
            }
            promptBuilder.append("السؤال المستعلم عنه: $userQuery")
            
            val response = model.generateContent(promptBuilder.toString())
            val resText = response.text
            if (!resText.isNullOrBlank()) {
                return@withContext resText
            }
        } catch (e: Throwable) {
            Log.w("RoaccutaneRepository", "Firebase Vertex AI call failed, falling back to direct REST API. Error: ${e.message}", e)
        }

        try {
            val systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemMsg))
            )

            // Dynamic inclusion of local user context inside the query context injection
            val fullyInjectedQuery = if (contextInfo.isNotBlank()) {
                "سياق حالة المستخدم الحالية: $contextInfo\n\nالسؤال المستعلم عنه: $userQuery"
            } else {
                userQuery
            }

            // Compile the whole message history structure
            val contentsList = mutableListOf<GeminiContent>()
            // Add previous dialog history if any
            contentsList.addAll(previousChat)
            // Add current question
            contentsList.add(GeminiContent(parts = listOf(GeminiPart(text = fullyInjectedQuery))))

            val request = GeminiRequest(
                contents = contentsList,
                systemInstruction = systemInstruction
            )

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext "خطأ: لم يتم ضبط مفتاح الـ API لـ Gemini في الإعدادات. يرجى إدخاله وحفظه أولاً لإتمام الاتصال الاحتياطي في حال تعذر الاتصال المباشر بـ Firebase Vertex AI.\n\nAPI Key is missing for Direct REST fallback."
            }

            val modelsToTry = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-3.5-flash")
            var lastError: Throwable? = null
            var responseText: String? = null

            for (modelName in modelsToTry) {
                try {
                    Log.d("RoaccutaneRepository", "Trying Gemini model: $modelName via REST...")
                    val apiResponse = RetrofitClient.service.generateContent(modelName, apiKey, request)
                    val textResult = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!textResult.isNullOrBlank()) {
                        responseText = textResult
                        break
                    }
                } catch (e: Throwable) {
                    Log.w("RoaccutaneRepository", "Failed calling model: $modelName. Error: ${e.message}", e)
                    lastError = e
                }
            }

            if (responseText != null) {
                responseText
            } else {
                val errMsg = lastError?.localizedMessage ?: "مشكلة اتصال بالإنترنت"
                "وقع خطأ أثناء ربط واستدعاء المساعد الطبي الذكي: $errMsg. تأكد من تفعيل الاتصال وصلاحية الرمز المدخل في الإعدادات."
            }
        } catch (e: Throwable) {
            Log.e("RoaccutaneRepository", "Gemini API error", e)
            "وقع خطأ أثناء ربط واستدعاء المساعد الطبي الذكي: ${e.localizedMessage ?: "مشكلة اتصال بالإنترنت"}. تأكد من تفعيل الاتصال وصلاحية الرمز المدخل في الإعدادات."
        }
    }
}
