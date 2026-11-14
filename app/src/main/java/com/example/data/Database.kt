package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "User",
    val age: Int = 20,
    val weightKg: Float = 82.0f,
    val dailyDoseMg: Int = 20,
    val targetType: String = "MIN", // "MIN" (120 mg/kg), "MAX" (150 mg/kg), or numeric string e.g. "135"
    val startDate: Long = System.currentTimeMillis(),
    val customTargetMg: Int? = null,
    val savedHospitalName: String = "",
    val savedHospitalLocation: String = "",
    val savedBuilding: String = "",
    val savedClinic: String = "",
    val savedDoctorName: String = "",
    val birthDateType: String = "G", // "G" for Gregorian, "H" for Hijri
    val birthDateVal: String = "", // e.g. YYYY-MM-DD
    val savedPatientFileNumber: String = ""
) {
    // Target Dosage calculations
    val targetMgPerKg: Float
        get() = when {
            targetType == "MAX" -> 150f
            targetType == "MIN" -> 120f
            else -> targetType.toFloatOrNull()?.coerceIn(120f, 150f) ?: 120f
        }

    val targetMgPerKgInt: Int
        get() = kotlin.math.round(targetMgPerKg).toInt()

    val calculatedTargetDoseMg: Int
        get() = customTargetMg ?: kotlin.math.round(weightKg * targetMgPerKg).toInt()
}

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val dateString: String, // YYYY-MM-DD
    val doseMg: Int,
    val isTaken: Boolean,
    val healthySources: String = "", // e.g., "Peanut Butter, Milk, Olive Oil"
    val symptoms: String = "",       // e.g., "Chapped Lips,Dry Skin" comma separated
    val notes: String = "",
    val waterCups: Int = 0,          // Calculated daily water quantity (cups)
    val lipMoistCount: Int = 0,      // Counting times lips lip-balm is applied
    val faceMoistCount: Int = 0,     // Counting face moisturizer applications
    val eyeMoistCount: Int = 0,      // Eye drops application count
    val timestamp: Long = System.currentTimeMillis()
)


@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateTime: Long, // Epoch millis
    val reason: String = "Checkup", // Clinic consultation reason
    val notes: String = "",
    val isCompleted: Boolean = false,
    
    // Hospital and doctor information
    val hospitalName: String = "",
    val hospitalLocation: String = "",
    val building: String = "",
    val clinic: String = "",
    val doctorName: String = "",
    
    // Blood analysis tracking metrics
    val liverFunction: String = "", // e.g., "ALT: 25, AST: 22" (طبيعي)
    val kidneyFunction: String = "", // e.g., "Creatinine: 0.8"
    val lipidsProfile: String = "", // cholesterol / triglycerides (مهم للرواكتان)
    val otherBloodTests: String = "",
    
    // Scheduled reminder
    val notificationTimeMillis: Long = 0L, // absolute timestamp for reminder

    // Lipid detailed numbers
    val tCholestrol: Double? = null,
    val hdl: Double? = null,
    val ldl: Double? = null,
    val triglycerides: Double? = null,
    val patientFileNumber: String = "",
    val prescriptionId: String? = null,
    val calendarEventId: Long? = null,

    // Specific Liver Function Tests (LFTs)
    val alt: Double? = null,
    val ast: Double? = null,
    val bilirubin: Double? = null,
    val alkalinePhosphate: Double? = null,
    val albuminInSerum: Double? = null,

    // Kidney Function Tests (KFTs)
    val creatinine: Double? = null,
    val urea: Double? = null,

    // Blood Sugar & Roaccutane Safety Monitoring
    val glycatedHaemoglobin: Double? = null,

    // Hierarchy & Encounter Bundling (Secondary Lab Visits & Multi-visit linking)
    val parentAppointmentId: Int? = null,
    val appointmentType: String = "CLINIC", // "CLINIC", "LAB", "PHARMACY", "OTHER"
    val customUnitsJson: String = ""
)

// --- DAO ---

@Dao
interface RoaccutaneDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM daily_logs ORDER BY dateString DESC")
    fun getAllLogs(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE dateString = :dateString")
    suspend fun getLogByDate(dateString: String): DailyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLog)

    @Query("DELETE FROM daily_logs WHERE dateString = :dateString")
    suspend fun deleteLogByDate(dateString: String)

    @Query("SELECT * FROM appointments ORDER BY dateTime ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments ORDER BY dateTime ASC")
    suspend fun getAllAppointmentsDirect(): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointment(id: Int)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    // --- Vital Signs ---
    @Query("SELECT * FROM vital_signs ORDER BY timestamp DESC")
    fun getAllVitalSigns(): Flow<List<VitalSign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitalSign(vital: VitalSign)

    // --- Clinics ---
    @Query("SELECT * FROM clinics ORDER BY createdAt ASC")
    fun getAllClinics(): Flow<List<Clinic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinic(clinic: Clinic)

    @Query("UPDATE clinics SET isFrozen = :isFrozen WHERE id = :clinicId")
    suspend fun updateClinicFrozen(clinicId: String, isFrozen: Boolean)

    @Query("UPDATE clinics SET trialEndDate = :trialEndDate WHERE id = :clinicId")
    suspend fun updateClinicTrial(clinicId: String, trialEndDate: Long)

    @Query("UPDATE clinics SET logoResName = :logoResName WHERE id = :clinicId")
    suspend fun updateClinicLogo(clinicId: String, logoResName: String)

    // --- Doctors ---
    @Query("SELECT * FROM doctors ORDER BY createdAt ASC")
    fun getAllDoctors(): Flow<List<Doctor>>

    @Query("SELECT * FROM doctors WHERE clinicId = :clinicId")
    fun getDoctorsByClinic(clinicId: String): Flow<List<Doctor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor)

    @Query("UPDATE doctors SET isBlocked = :isBlocked WHERE id = :doctorId")
    suspend fun updateDoctorBlocked(doctorId: String, isBlocked: Boolean)

    @Query("UPDATE patient_pairings SET doctorId = :newDoctorId WHERE doctorId = :oldDoctorId AND clinicId = :clinicId")
    suspend fun reassignClinicPatients(clinicId: String, oldDoctorId: String, newDoctorId: String)

    // --- Patient Pairings ---
    @Query("SELECT * FROM patient_pairings ORDER BY pairedAt DESC")
    fun getAllPairings(): Flow<List<PatientPairing>>

    @Query("SELECT * FROM patient_pairings WHERE doctorId = :doctorId")
    fun getPairingsForDoctor(doctorId: String): Flow<List<PatientPairing>>

    @Query("SELECT * FROM patient_pairings WHERE clinicId = :clinicId")
    fun getPairingsForClinic(clinicId: String): Flow<List<PatientPairing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairing(pairing: PatientPairing)

    @Query("UPDATE patient_pairings SET status = :status WHERE id = :id")
    suspend fun updatePairingStatus(id: Int, status: String)

    // --- Doctor Notes ---
    @Query("SELECT * FROM doctor_notes ORDER BY createdAt DESC")
    fun getAllDoctorNotes(): Flow<List<DoctorNote>>

    @Query("SELECT * FROM doctor_notes WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getNotesForPatient(patientId: String): Flow<List<DoctorNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorNote(note: DoctorNote)

    @Query("DELETE FROM doctor_notes WHERE id = :id")
    suspend fun deleteDoctorNote(id: Int)

    // --- Financial Records ---
    @Query("SELECT * FROM financial_records ORDER BY timestamp DESC")
    fun getAllFinancialRecords(): Flow<List<FinancialRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecord)

    @Query("DELETE FROM financial_records WHERE id = :id")
    suspend fun deleteFinancialRecord(id: Int)

    // --- Feature Usage Records ---
    @Query("SELECT * FROM feature_usage_records ORDER BY id DESC")
    fun getAllFeatureUsage(): Flow<List<FeatureUsageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatureUsage(usage: FeatureUsageRecord)

    // --- Subscription Plans ---
    @Query("SELECT * FROM subscription_plans")
    fun getAllSubscriptionPlans(): Flow<List<SubscriptionPlanConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionPlan(plan: SubscriptionPlanConfig)

    @Query("UPDATE subscription_plans SET priceMonthlySAR = :priceMonthly, priceAnnualSAR = :priceAnnual WHERE id = :planId")
    suspend fun updatePlanPrices(planId: String, priceMonthly: Double, priceAnnual: Double)

    @Query("UPDATE subscription_plans SET isAiEnabled = :isAiEnabled, dailyAiLimit = :dailyAiLimit, maxPdfExports = :maxPdfExports WHERE id = :planId")
    suspend fun updatePlanFeatures(planId: String, isAiEnabled: Boolean, dailyAiLimit: Int, maxPdfExports: Int)

    @Query("UPDATE subscription_plans SET featuresList = :featuresList WHERE id = :planId")
    suspend fun updatePlanFeaturesList(planId: String, featuresList: String)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    // --- Affiliate Products ---
    @Query("SELECT * FROM affiliate_products ORDER BY rankOrder ASC")
    fun getAllAffiliateProducts(): Flow<List<AffiliateProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffiliateProduct(product: AffiliateProduct)

    @Query("DELETE FROM affiliate_products WHERE id = :id")
    suspend fun deleteAffiliateProduct(id: String)

    @Query("DELETE FROM affiliate_products")
    suspend fun deleteAllAffiliateProducts()

    @Query("UPDATE affiliate_products SET rankOrder = :rankOrder WHERE id = :id")
    suspend fun updateAffiliateRank(id: String, rankOrder: Int)

    // --- Owner Change Logs ---
    @Query("SELECT * FROM owner_change_logs ORDER BY timestamp DESC")
    fun getAllOwnerChanges(): Flow<List<OwnerChangeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerChange(record: OwnerChangeRecord)

    // --- Additional Clinic & Doctor management queries ---
    @Query("UPDATE doctors SET name = :name, specialty = :specialty, phone = :phone, bio = :bio, consultationFee = :fee, signatureUri = :signatureUri, stampUri = :stampUri, licenseDocUri = :licenseUri WHERE id = :id")
    suspend fun updateDoctorProfile(id: String, name: String, specialty: String, phone: String, bio: String, fee: Double, signatureUri: String, stampUri: String, licenseUri: String)

    @Query("UPDATE clinics SET name = :name, contactEmail = :email, contactPhone = :phone, logoResName = :logo WHERE id = :id")
    suspend fun updateClinicDetails(id: String, name: String, email: String, phone: String, logo: String)

    @Query("DELETE FROM clinics WHERE id = :id")
    suspend fun deleteClinic(id: String)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctor(id: String)

    @Query("DELETE FROM patient_pairings WHERE id = :id")
    suspend fun deletePairing(id: Int)

    @Query("DELETE FROM financial_records")
    suspend fun deleteAllFinancialRecords()

    @Query("DELETE FROM feature_usage_records")
    suspend fun deleteAllFeatureUsage()

    @Query("DELETE FROM doctor_notes WHERE isTemplate = 1")
    suspend fun deleteTemplateDoctorNotes()

    // --- Dispensed Medications ---
    @Query("SELECT * FROM dispensed_medications ORDER BY createdAt DESC")
    fun getAllDispensedMedications(): Flow<List<DispensedMedication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispensedMedication(item: DispensedMedication)

    @Query("DELETE FROM dispensed_medications WHERE id = :id")
    suspend fun deleteDispensedMedication(id: String)

    @Query("UPDATE dispensed_medications SET missedDays = :missedDays WHERE id = :id")
    suspend fun updateDispensedMedicationMissedDays(id: String, missedDays: Int)

    @Query("DELETE FROM dispensed_medications")
    suspend fun deleteAllDispensedMedications()

    // --- Medical Prescriptions ---
    @Query("SELECT * FROM medical_prescriptions ORDER BY createdAt DESC")
    fun getAllMedicalPrescriptions(): Flow<List<MedicalPrescription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalPrescription(item: MedicalPrescription)

    @Query("UPDATE dispensed_medications SET prescriptionUrl = :url WHERE prescriptionId = :prescriptionId")
    suspend fun updateDispensedMedicationsPrescriptionUrl(prescriptionId: String, url: String)

    @Query("SELECT * FROM medical_prescriptions WHERE id = :id LIMIT 1")
    suspend fun getMedicalPrescriptionById(id: String): MedicalPrescription?

    @Query("DELETE FROM medical_prescriptions WHERE id = :id")
    suspend fun deleteMedicalPrescription(id: String)

    @Query("DELETE FROM medical_prescriptions")
    suspend fun deleteAllMedicalPrescriptions()
}

// --- AppDatabase ---

@Database(
    entities = [
        UserProfile::class,
        DailyLog::class,
        Appointment::class,
        VitalSign::class,
        Clinic::class,
        Doctor::class,
        PatientPairing::class,
        DoctorNote::class,
        FinancialRecord::class,
        FeatureUsageRecord::class,
        SubscriptionPlanConfig::class,
        AuditLog::class,
        AffiliateProduct::class,
        OwnerChangeRecord::class,
        DispensedMedication::class,
        MedicalPrescription::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roaccutaneDao(): RoaccutaneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS owner_change_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, actionType TEXT NOT NULL, title TEXT NOT NULL, details TEXT NOT NULL, timestamp INTEGER NOT NULL)")
                try {
                    db.execSQL("ALTER TABLE affiliate_products ADD COLUMN descriptionAr TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE doctors ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE doctors ADD COLUMN bio TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE doctors ADD COLUMN consultationFee REAL NOT NULL DEFAULT 150.0")
                    db.execSQL("ALTER TABLE doctors ADD COLUMN signatureUri TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE doctors ADD COLUMN stampUri TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE doctors ADD COLUMN licenseDocUri TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE clinics ADD COLUMN contactPhone TEXT NOT NULL DEFAULT '+966 11 234 5678'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE subscription_plans ADD COLUMN featuresList TEXT NOT NULL DEFAULT 'ذكاء اصطناعي طبي, تصدير تقارير PDF, تحليلات سريرية, دعم على مدار الساعة'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE affiliate_products ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dispensed_medications (
                        id TEXT PRIMARY KEY NOT NULL,
                        medicationName TEXT NOT NULL,
                        sourceProductId TEXT,
                        boxCount INTEGER NOT NULL DEFAULT 1,
                        dailyDose TEXT NOT NULL DEFAULT '20 ملغ',
                        durationDays INTEGER NOT NULL DEFAULT 30,
                        startDate INTEGER NOT NULL,
                        missedDays INTEGER NOT NULL DEFAULT 0,
                        badgeColorHex TEXT NOT NULL DEFAULT '#1976D2',
                        notes TEXT NOT NULL DEFAULT '',
                        attachedFileUris TEXT NOT NULL DEFAULT '',
                        attachedFileNames TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE affiliate_products ADD COLUMN manufacturerUrl TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE affiliate_products ADD COLUMN pharmacyUrl TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN capsuleStrengthMg INTEGER NOT NULL DEFAULT 20")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN dailyCapsulesCount INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN capsulesPerBox INTEGER NOT NULL DEFAULT 30")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_prescriptions (
                        id TEXT PRIMARY KEY NOT NULL,
                        prescriptionNumber TEXT NOT NULL DEFAULT '',
                        medicationName TEXT NOT NULL,
                        doctorName TEXT NOT NULL DEFAULT '',
                        clinicOrHospital TEXT NOT NULL DEFAULT '',
                        dailyDose TEXT NOT NULL DEFAULT '20 ملغ',
                        capsuleStrengthMg INTEGER NOT NULL DEFAULT 20,
                        dailyCapsulesCount INTEGER NOT NULL DEFAULT 1,
                        capsulesPerBox INTEGER NOT NULL DEFAULT 30,
                        totalAllowedRefills INTEGER NOT NULL DEFAULT 3,
                        prescriptionDate INTEGER NOT NULL,
                        expiryDate INTEGER NOT NULL,
                        badgeColorHex TEXT NOT NULL DEFAULT '#1976D2',
                        notes TEXT NOT NULL DEFAULT '',
                        attachedFileUris TEXT NOT NULL DEFAULT '',
                        attachedFileNames TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN prescriptionId TEXT")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN dispenseNumber INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE appointments ADD COLUMN prescriptionId TEXT")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE medical_prescriptions ADD COLUMN prescriptionUrl TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN prescriptionUrl TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                // Strict Scope Constraint: Clear legacy attachment data strictly in medical_prescriptions and linked dispensed_medications
                try {
                    db.execSQL("UPDATE medical_prescriptions SET attachedFileUris = '', attachedFileNames = ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("UPDATE dispensed_medications SET attachedFileUris = '', attachedFileNames = '' WHERE prescriptionId IS NOT NULL")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE medical_prescriptions ADD COLUMN targetDurationDays INTEGER NOT NULL DEFAULT 90")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE dispensed_medications ADD COLUMN dispenseImageUri TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE appointments ADD COLUMN calendarEventId INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN alt REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN ast REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN bilirubin REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN alkalinePhosphate REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN albuminInSerum REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN creatinine REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN urea REAL") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN glycatedHaemoglobin REAL") } catch (_: Exception) {}
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN parentAppointmentId INTEGER") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'CLINIC'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE appointments ADD COLUMN customUnitsJson TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roaccutane_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
