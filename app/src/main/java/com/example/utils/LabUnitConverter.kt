package com.example.utils

import java.util.Locale

/**
 * Standard International Units (SI) and Conventional Laboratory Units Converter.
 * Supports cross-hospital conversions between:
 * - Conventional US/Private Hospital units (mg/dL, g/dL, %)
 * - International SI / University Hospital (e.g., KFUH, MOH, Europe) units (mmol/L, µmol/L, g/L, mmol/mol, µkat/L)
 */
object LabUnitConverter {

    // --- Cholesterol (Total, LDL, HDL) ---
    // Conversion factor: 1 mmol/L = 38.67 mg/dL
    fun cholesterolMgToMmol(mg: Double): Double = mg * 0.02586
    fun cholesterolMmolToMg(mmol: Double): Double = mmol * 38.67

    fun formatCholesterolDual(mg: Double?, appLanguage: String = "ar"): String {
        if (mg == null) return "–"
        val mmol = cholesterolMgToMmol(mg)
        val mmolStr = String.format(Locale.US, "%.2f", mmol)
        val mgStr = String.format(Locale.US, "%.0f", mg)
        return if (appLanguage == "ar") "$mgStr mg/dL ($mmolStr mmol/L)" else "$mgStr mg/dL ($mmolStr mmol/L)"
    }

    // --- Triglycerides ---
    // Conversion factor: 1 mmol/L = 88.57 mg/dL
    fun triglyceridesMgToMmol(mg: Double): Double = mg * 0.01129
    fun triglyceridesMmolToMg(mmol: Double): Double = mmol * 88.57

    fun formatTriglyceridesDual(mg: Double?, appLanguage: String = "ar"): String {
        if (mg == null) return "–"
        val mmol = triglyceridesMgToMmol(mg)
        val mmolStr = String.format(Locale.US, "%.2f", mmol)
        val mgStr = String.format(Locale.US, "%.0f", mg)
        return "$mgStr mg/dL ($mmolStr mmol/L)"
    }

    // --- Serum Creatinine ---
    // Conversion factor: 1 mg/dL ≈ 88.4 µmol/L
    fun creatinineMgToUmol(mg: Double): Double = mg * 88.4
    fun creatinineUmolToMg(umol: Double): Double = umol / 88.4

    fun formatCreatinineDual(mg: Double?, appLanguage: String = "ar"): String {
        if (mg == null) return "–"
        val umol = creatinineMgToUmol(mg)
        val umolStr = String.format(Locale.US, "%.1f", umol)
        val mgStr = String.format(Locale.US, "%.2f", mg)
        return "$mgStr mg/dL ($umolStr µmol/L)"
    }

    // --- Blood Urea / BUN ---
    // Conversion factor: 1 mmol/L Blood Urea ≈ 2.80 mg/dL BUN
    fun ureaBUNMgToMmol(mgBUN: Double): Double = mgBUN / 2.80
    fun ureaMmolToBUNMg(mmol: Double): Double = mmol * 2.80

    fun formatUreaDual(mg: Double?, appLanguage: String = "ar"): String {
        if (mg == null) return "–"
        val mmol = ureaBUNMgToMmol(mg)
        val mmolStr = String.format(Locale.US, "%.1f", mmol)
        val mgStr = String.format(Locale.US, "%.1f", mg)
        return "$mgStr mg/dL ($mmolStr mmol/L)"
    }

    // --- Total Bilirubin ---
    // Conversion factor: 1 mg/dL ≈ 17.1 µmol/L
    fun bilirubinMgToUmol(mg: Double): Double = mg * 17.1
    fun bilirubinUmolToMg(umol: Double): Double = umol / 17.1

    fun formatBilirubinDual(mg: Double?, appLanguage: String = "ar"): String {
        if (mg == null) return "–"
        val umol = bilirubinMgToUmol(mg)
        val umolStr = String.format(Locale.US, "%.1f", umol)
        val mgStr = String.format(Locale.US, "%.2f", mg)
        return "$mgStr mg/dL ($umolStr µmol/L)"
    }

    // --- Serum Albumin ---
    // Conversion factor: 1 g/dL = 10 g/L
    fun albuminGdlToGl(gdl: Double): Double = gdl * 10.0
    fun albuminGlToGdl(gl: Double): Double = gl / 10.0

    fun formatAlbuminDual(gdl: Double?, appLanguage: String = "ar"): String {
        if (gdl == null) return "–"
        val gl = albuminGdlToGl(gdl)
        val glStr = String.format(Locale.US, "%.1f", gl)
        val gdlStr = String.format(Locale.US, "%.2f", gdl)
        return "$gdlStr g/dL ($glStr g/L)"
    }

    // --- Glycated Haemoglobin (HbA1c) ---
    // NGSP % to IFCC mmol/mol: (percent - 2.15) * 10.929
    fun hba1cPercentToMmolMol(percent: Double): Double = ((percent - 2.15) * 10.929).coerceAtLeast(0.0)
    fun hba1cMmolMolToPercent(mmolMol: Double): Double = (mmolMol / 10.929) + 2.15

    fun formatHbA1cDual(percent: Double?, appLanguage: String = "ar"): String {
        if (percent == null) return "–"
        val mmolMol = hba1cPercentToMmolMol(percent)
        val mmolMolStr = String.format(Locale.US, "%.0f", mmolMol)
        val percentStr = String.format(Locale.US, "%.1f", percent)
        return "$percentStr% ($mmolMolStr mmol/mol)"
    }

    // --- Liver Enzymes & ALP (U/L to µkat/L) ---
    fun enzymeUlToUkat(ul: Double): Double = ul * 0.0167

    fun formatEnzymeDual(ul: Double?, appLanguage: String = "ar"): String {
        if (ul == null) return "–"
        val ukat = enzymeUlToUkat(ul)
        val ukatStr = String.format(Locale.US, "%.2f", ukat)
        val ulStr = String.format(Locale.US, "%.0f", ul)
        return "$ulStr U/L ($ukatStr µkat/L)"
    }
}
