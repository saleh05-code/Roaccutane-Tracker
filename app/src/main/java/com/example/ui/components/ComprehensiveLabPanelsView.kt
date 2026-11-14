package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Appointment
import com.example.utils.LabUnitConverter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Categorized Comprehensive Lab Panels View for Roaccutane treatment monitoring.
 * Fully supports Standard International Units (SI) alongside Conventional Units
 * across different hospitals (e.g. KFUH, MOH, Private Clinics, International standards).
 *
 * Includes:
 * 1. Dedicated Tabs: Liver (LFTs), Kidney (KFTs), Lipids, HbA1c & Others, plus Comprehensive Master Table.
 * 2. Visual Metric Cards with dual units (e.g., mg/dL and mmol/L or µmol/L).
 * 3. Historical Results Tables following the exact double-scrollable model from the Lab Analysis page.
 */
@Composable
fun ComprehensiveLabPanelsView(
    allAppointments: List<Appointment>,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    var selectedLabTab by remember { mutableIntStateOf(0) }

    val tabs = remember(appLanguage) {
        listOf(
            Triple(0, if (appLanguage == "ar") "وظائف الكبد" else "Liver (LFTs)", Icons.Default.Biotech),
            Triple(1, if (appLanguage == "ar") "وظائف الكلى" else "Kidney (KFTs)", Icons.Default.WaterDrop),
            Triple(2, if (appLanguage == "ar") "دهنيات الدم" else "Lipids Profile", Icons.Default.Bloodtype),
            Triple(3, if (appLanguage == "ar") "السكر والتحاليل الأخرى" else "HbA1c & Others", Icons.Default.MedicalInformation),
            Triple(4, if (appLanguage == "ar") "جدول التحاليل الشامل" else "All Tests Table", Icons.Default.TableChart)
        )
    }

    // Rely strictly on appointments classified as Lab / Analysis ("LAB")
    val labAppointments = remember(allAppointments) {
        allAppointments.filter { it.appointmentType.equals("LAB", ignoreCase = true) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Selector Row
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tabs) { (index, title, icon) ->
                    val isSelected = selectedLabTab == index
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedLabTab = index },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Active Lab Panel Content
        AnimatedContent(targetState = selectedLabTab, label = "LabPanelTab") { currentTab ->
            when (currentTab) {
                0 -> LiverFunctionPanel(allAppointments = labAppointments, appLanguage = appLanguage)
                1 -> KidneyFunctionPanel(allAppointments = labAppointments, appLanguage = appLanguage)
                2 -> LipidProfilePanel(allAppointments = labAppointments, appLanguage = appLanguage)
                3 -> OtherTestsAndHbA1cPanel(allAppointments = labAppointments, appLanguage = appLanguage)
                else -> AllLabsMasterTablePanel(allAppointments = labAppointments, appLanguage = appLanguage)
            }
        }
    }
}

// -------------------------------------------------------------
// Page 1: Liver Function Tests (وظائف الكبد)
// -------------------------------------------------------------
@Composable
fun LiverFunctionPanel(
    allAppointments: List<Appointment>,
    appLanguage: String
) {
    val appointmentsWithLiver = remember(allAppointments) {
        allAppointments
            .filter { it.alt != null || it.ast != null || it.bilirubin != null || it.alkalinePhosphate != null || it.albuminInSerum != null || it.liverFunction.isNotBlank() }
            .sortedBy { it.dateTime } // Chronological for table
    }

    val latestAppt = appointmentsWithLiver.lastOrNull()
    var viewMode by remember { mutableStateOf("TABLE") } // "TABLE" or "CARDS"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Clinical Information Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE65100).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        text = if (appLanguage == "ar") "متابعة سلامة الكبد ووحدات القياس العالمية (LFTs)" else "Liver Safety & International Units (LFTs)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = if (appLanguage == "ar")
                            "يتم استقلاب الرواكتان في الكبد. يتم عرض نتائج الإنزيمات بوحدات U/L مع وحدات النظام العالمي (SI) كالبيليروبين (µmol/L) والألبومين (g/L) المعتمدة بمستشفيات KFUH ووزارة الصحة."
                        else
                            "Isotretinoin is metabolized primarily by liver. Displays standard U/L along with International SI units (µmol/L and g/L) used in hospital labs.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5 Specific Liver Tests with Dual Units
        val liverParams = listOf(
            LabTestItemDef(
                code = "ALT",
                name = if (appLanguage == "ar") "إنزيم ناقلة أمين الألانين (ALT / SGPT)" else "Alanine Aminotransferase (ALT)",
                normalRange = if (appLanguage == "ar") "7 - 56 وحدة/لتر (U/L)" else "7 - 56 U/L",
                internationalRange = if (appLanguage == "ar") "النظام الدولي: 0.12 - 0.93 ميكروكاتال (µkat/L)" else "SI: 0.12 - 0.93 µkat/L",
                latestValue = latestAppt?.alt,
                status = evaluateRange(latestAppt?.alt, 7.0, 56.0),
                unit = "U/L",
                internationalUnit = "µkat/L",
                internationalValue = latestAppt?.alt?.let { String.format(Locale.US, "%.2f µkat/L", LabUnitConverter.enzymeUlToUkat(it)) }
            ),
            LabTestItemDef(
                code = "AST",
                name = if (appLanguage == "ar") "إنزيم ناقلة أمين الأسبارتات (AST / SGOT)" else "Aspartate Aminotransferase (AST)",
                normalRange = if (appLanguage == "ar") "10 - 40 وحدة/لتر (U/L)" else "10 - 40 U/L",
                internationalRange = if (appLanguage == "ar") "النظام الدولي: 0.17 - 0.67 ميكروكاتال (µkat/L)" else "SI: 0.17 - 0.67 µkat/L",
                latestValue = latestAppt?.ast,
                status = evaluateRange(latestAppt?.ast, 10.0, 40.0),
                unit = "U/L",
                internationalUnit = "µkat/L",
                internationalValue = latestAppt?.ast?.let { String.format(Locale.US, "%.2f µkat/L", LabUnitConverter.enzymeUlToUkat(it)) }
            ),
            LabTestItemDef(
                code = "Bilirubin",
                name = if (appLanguage == "ar") "البيليروبين الكلي (Total Bilirubin)" else "Total Bilirubin",
                normalRange = if (appLanguage == "ar") "0.1 - 1.2 ملغ/ديسيلتر (mg/dL)" else "0.1 - 1.2 mg/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH): 1.7 - 20.5 ميكرومول/لتر (µmol/L)" else "SI (KFUH): 1.7 - 20.5 µmol/L",
                latestValue = latestAppt?.bilirubin,
                status = evaluateRange(latestAppt?.bilirubin, 0.1, 1.2),
                unit = "mg/dL",
                internationalUnit = "µmol/L",
                internationalValue = latestAppt?.bilirubin?.let { String.format(Locale.US, "%.1f µmol/L", LabUnitConverter.bilirubinMgToUmol(it)) }
            ),
            LabTestItemDef(
                code = "ALP",
                name = if (appLanguage == "ar") "الفوسفاتاز القلوي (Alkaline Phosphatase)" else "Alkaline Phosphatase (ALP)",
                normalRange = if (appLanguage == "ar") "44 - 147 وحدة/لتر (U/L)" else "44 - 147 U/L",
                internationalRange = if (appLanguage == "ar") "النظام الدولي: 0.73 - 2.45 ميكروكاتال (µkat/L)" else "SI: 0.73 - 2.45 µkat/L",
                latestValue = latestAppt?.alkalinePhosphate,
                status = evaluateRange(latestAppt?.alkalinePhosphate, 44.0, 147.0),
                unit = "U/L",
                internationalUnit = "µkat/L",
                internationalValue = latestAppt?.alkalinePhosphate?.let { String.format(Locale.US, "%.2f µkat/L", LabUnitConverter.enzymeUlToUkat(it)) }
            ),
            LabTestItemDef(
                code = "Albumin",
                name = if (appLanguage == "ar") "ألبومين مصل الدم (Albumin in Serum)" else "Serum Albumin",
                normalRange = if (appLanguage == "ar") "3.5 - 5.5 غرام/ديسيلتر (g/dL)" else "3.5 - 5.5 g/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH/MOH): 35 - 55 غرام/لتر (g/L)" else "SI (KFUH/MOH): 35 - 55 g/L",
                latestValue = latestAppt?.albuminInSerum,
                status = evaluateRange(latestAppt?.albuminInSerum, 3.5, 5.5),
                unit = "g/dL",
                internationalUnit = "g/L",
                internationalValue = latestAppt?.albuminInSerum?.let { String.format(Locale.US, "%.1f g/L", LabUnitConverter.albuminGdlToGl(it)) }
            )
        )

        liverParams.forEach { item ->
            LabMetricCard(item = item, appLanguage = appLanguage)
        }

        // Section Header with View Switcher (Table vs Cards)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (appLanguage == "ar") "سجل نتائج الكبد السابقة:" else "Previous Liver Test Logs:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = viewMode == "TABLE",
                    onClick = { viewMode = "TABLE" },
                    label = { Text(if (appLanguage == "ar") "جدول مقارن" else "Table", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = viewMode == "CARDS",
                    onClick = { viewMode = "CARDS" },
                    label = { Text(if (appLanguage == "ar") "بطاقات" else "Cards", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        if (appointmentsWithLiver.isNotEmpty()) {
            if (viewMode == "TABLE") {
                // Table model matching the Analytics Screen
                val liverTableRows = listOf(
                    LabRowDef(
                        title = if (appLanguage == "ar") "ALT (SGPT)" else "ALT",
                        code = "ALT",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 7 - 56 U/L" else "Normal: 7-56 U/L",
                        internationalRange = "0.12 - 0.93 µkat/L",
                        getValue = { it.alt },
                        formatValue = { String.format(Locale.US, "%.0f U/L", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f µkat/L", LabUnitConverter.enzymeUlToUkat(it)) },
                        isAbnormal = { it !in 7.0..56.0 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "AST (SGOT)" else "AST",
                        code = "AST",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 10 - 40 U/L" else "Normal: 10-40 U/L",
                        internationalRange = "0.17 - 0.67 µkat/L",
                        getValue = { it.ast },
                        formatValue = { String.format(Locale.US, "%.0f U/L", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f µkat/L", LabUnitConverter.enzymeUlToUkat(it)) },
                        isAbnormal = { it !in 10.0..40.0 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "البيليروبين الكلي" else "Total Bilirubin",
                        code = "Bilirubin",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 0.1 - 1.2 mg/dL" else "Normal: 0.1-1.2 mg/dL",
                        internationalRange = "SI: 1.7 - 20.5 µmol/L",
                        getValue = { it.bilirubin },
                        formatValue = { String.format(Locale.US, "%.2f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.1f µmol/L", LabUnitConverter.bilirubinMgToUmol(it)) },
                        isAbnormal = { it !in 0.1..1.2 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "الفوسفاتاز القلوي" else "ALP",
                        code = "ALP",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 44 - 147 U/L" else "Normal: 44-147 U/L",
                        internationalRange = "0.73 - 2.45 µkat/L",
                        getValue = { it.alkalinePhosphate },
                        formatValue = { String.format(Locale.US, "%.0f U/L", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f µkat/L", LabUnitConverter.enzymeUlToUkat(it)) },
                        isAbnormal = { it !in 44.0..147.0 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "ألبومين المصل" else "Albumin",
                        code = "Albumin",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 3.5 - 5.5 g/dL" else "Normal: 3.5-5.5 g/dL",
                        internationalRange = "SI: 35 - 55 g/L",
                        getValue = { it.albuminInSerum },
                        formatValue = { String.format(Locale.US, "%.2f g/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.1f g/L", LabUnitConverter.albuminGdlToGl(it)) },
                        isAbnormal = { it !in 3.5..5.5 },
                        isHigherWorse = false
                    )
                )

                GenericLabHistoryTable(
                    appointments = appointmentsWithLiver,
                    rows = liverTableRows,
                    appLanguage = appLanguage
                )
            } else {
                // Card View fallback
                appointmentsWithLiver.reversed().forEach { appt ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(appt.dateTime))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "📅 $dateStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = appt.hospitalName.ifBlank { "العيادة" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val items = mutableListOf<String>()
                            if (appt.alt != null) items.add("ALT: ${appt.alt} U/L")
                            if (appt.ast != null) items.add("AST: ${appt.ast} U/L")
                            if (appt.bilirubin != null) items.add("Bilirubin: ${LabUnitConverter.formatBilirubinDual(appt.bilirubin, appLanguage)}")
                            if (appt.alkalinePhosphate != null) items.add("ALP: ${appt.alkalinePhosphate} U/L")
                            if (appt.albuminInSerum != null) items.add("Albumin: ${LabUnitConverter.formatAlbuminDual(appt.albuminInSerum, appLanguage)}")
                            if (items.isEmpty() && appt.liverFunction.isNotBlank()) items.add(appt.liverFunction)
                            Text(text = items.joinToString(" • "), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            EmptyLabStateCard(
                message = if (appLanguage == "ar") "لم يتم تسجيل أي نتائج لوظائف الكبد بعد. يمكنك إضافتها عند جدولة أو تعديل موعد."
                else "No liver function test results recorded yet."
            )
        }
    }
}

// -------------------------------------------------------------
// Page 2: Kidney Function Tests (وظائف الكلى)
// -------------------------------------------------------------
@Composable
fun KidneyFunctionPanel(
    allAppointments: List<Appointment>,
    appLanguage: String
) {
    val appointmentsWithKidney = remember(allAppointments) {
        allAppointments
            .filter { it.creatinine != null || it.urea != null || it.kidneyFunction.isNotBlank() }
            .sortedBy { it.dateTime } // Chronological
    }
    val latestAppt = appointmentsWithKidney.lastOrNull()
    var viewMode by remember { mutableStateOf("TABLE") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1976D2).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        text = if (appLanguage == "ar") "متابعة كفاءة الكلى ووحدات القياس العالمية (KFTs)" else "Renal Health & International SI Units (KFTs)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = if (appLanguage == "ar")
                            "يتم عرض الكرياتينين بالوحدتين: التقليدية (mg/dL) والدولية (µmol/L) المعتمدة في مستشفيات KFUH والصحة العالمية، مع يوريا الدم (mmol/L)."
                        else
                            "Displays Serum Creatinine in both mg/dL and International SI units (µmol/L) as reported in KFUH and global hospitals.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val kidneyParams = listOf(
            LabTestItemDef(
                code = "Creatinine",
                name = if (appLanguage == "ar") "الكرياتينين في الدم (Serum Creatinine)" else "Serum Creatinine",
                normalRange = if (appLanguage == "ar") "0.6 - 1.2 ملغ/ديسيلتر (mg/dL)" else "0.6 - 1.2 mg/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH/MOH): 53 - 106 ميكرومول/لتر (µmol/L)" else "SI (KFUH/MOH): 53 - 106 µmol/L",
                latestValue = latestAppt?.creatinine,
                status = evaluateRange(latestAppt?.creatinine, 0.6, 1.2),
                unit = "mg/dL",
                internationalUnit = "µmol/L",
                internationalValue = latestAppt?.creatinine?.let { String.format(Locale.US, "%.1f µmol/L", LabUnitConverter.creatinineMgToUmol(it)) }
            ),
            LabTestItemDef(
                code = "Urea / BUN",
                name = if (appLanguage == "ar") "نيتروجين يوريا الدم (BUN / Urea)" else "Blood Urea Nitrogen (BUN)",
                normalRange = if (appLanguage == "ar") "7 - 20 ملغ/ديسيلتر (mg/dL BUN)" else "7 - 20 mg/dL BUN",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH): 2.5 - 7.1 مليمول/لتر (mmol/L)" else "SI (KFUH): 2.5 - 7.1 mmol/L",
                latestValue = latestAppt?.urea,
                status = evaluateRange(latestAppt?.urea, 7.0, 20.0),
                unit = "mg/dL",
                internationalUnit = "mmol/L",
                internationalValue = latestAppt?.urea?.let { String.format(Locale.US, "%.1f mmol/L", LabUnitConverter.ureaBUNMgToMmol(it)) }
            )
        )

        kidneyParams.forEach { item ->
            LabMetricCard(item = item, appLanguage = appLanguage)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (appLanguage == "ar") "سجل قراءات الكلى السابقة:" else "Previous Renal Records:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = viewMode == "TABLE",
                    onClick = { viewMode = "TABLE" },
                    label = { Text(if (appLanguage == "ar") "جدول مقارن" else "Table", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = viewMode == "CARDS",
                    onClick = { viewMode = "CARDS" },
                    label = { Text(if (appLanguage == "ar") "بطاقات" else "Cards", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        if (appointmentsWithKidney.isNotEmpty()) {
            if (viewMode == "TABLE") {
                val kidneyTableRows = listOf(
                    LabRowDef(
                        title = if (appLanguage == "ar") "الكرياتينين" else "Creatinine",
                        code = "Creatinine",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 0.6 - 1.2 mg/dL" else "Normal: 0.6-1.2 mg/dL",
                        internationalRange = "SI: 53 - 106 µmol/L",
                        getValue = { it.creatinine },
                        formatValue = { String.format(Locale.US, "%.2f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.1f µmol/L", LabUnitConverter.creatinineMgToUmol(it)) },
                        isAbnormal = { it !in 0.6..1.2 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "يوريا الدم (BUN)" else "Urea / BUN",
                        code = "Urea",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: 7 - 20 mg/dL" else "Normal: 7-20 mg/dL",
                        internationalRange = "SI: 2.5 - 7.1 mmol/L",
                        getValue = { it.urea },
                        formatValue = { String.format(Locale.US, "%.1f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.1f mmol/L", LabUnitConverter.ureaBUNMgToMmol(it)) },
                        isAbnormal = { it !in 7.0..20.0 },
                        isHigherWorse = true
                    )
                )

                GenericLabHistoryTable(
                    appointments = appointmentsWithKidney,
                    rows = kidneyTableRows,
                    appLanguage = appLanguage
                )
            } else {
                appointmentsWithKidney.reversed().forEach { appt ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(appt.dateTime))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "📅 $dateStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = appt.hospitalName.ifBlank { "العيادة" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val items = mutableListOf<String>()
                            if (appt.creatinine != null) items.add("Creatinine: ${LabUnitConverter.formatCreatinineDual(appt.creatinine, appLanguage)}")
                            if (appt.urea != null) items.add("Urea: ${LabUnitConverter.formatUreaDual(appt.urea, appLanguage)}")
                            if (items.isEmpty() && appt.kidneyFunction.isNotBlank()) items.add(appt.kidneyFunction)
                            Text(text = items.joinToString(" • "), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            EmptyLabStateCard(
                message = if (appLanguage == "ar") "لم يتم تسجيل أي نتائج لوظائف الكلى بعد."
                else "No kidney function records logged yet."
            )
        }
    }
}

// -------------------------------------------------------------
// Page 3: Lipid Profile (دهنيات الدم والكوليسترول)
// -------------------------------------------------------------
@Composable
fun LipidProfilePanel(
    allAppointments: List<Appointment>,
    appLanguage: String
) {
    val appointmentsWithLipids = remember(allAppointments) {
        allAppointments
            .filter { it.tCholestrol != null || it.triglycerides != null || it.hdl != null || it.ldl != null || it.lipidsProfile.isNotBlank() }
            .sortedBy { it.dateTime } // Chronological
    }
    val latestAppt = appointmentsWithLipids.lastOrNull()
    var viewMode by remember { mutableStateOf("TABLE") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFC2185B).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFFC2185B).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Bloodtype, contentDescription = null, tint = Color(0xFFC2185B), modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        text = if (appLanguage == "ar") "مراقبة دهنيات الدم بالوحدات التقليدية والدولية (mg/dL & mmol/L)" else "Lipid Profile & International Units (mg/dL & mmol/L)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC2185B)
                    )
                    Text(
                        text = if (appLanguage == "ar")
                            "تعرض دهنيات الدم بوحدات ملغ/ديسيلتر (mg/dL) مع التحويل التلقائي لنظام مليمول/لتر (mmol/L) المتبع في تقارير مستشفى الملك فهد الجامعي (KFUH) والمراكز العالمية."
                        else
                            "Displays Lipids in mg/dL with live automatic conversion to mmol/L as reported by KFUH and international health labs.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val tcVal = latestAppt?.tCholestrol
        val tgVal = latestAppt?.triglycerides
        val hdlVal = latestAppt?.hdl
        val ldlVal = latestAppt?.ldl

        val calculatedRiskFactor = if (tcVal != null && hdlVal != null && hdlVal > 0.0) {
            String.format(Locale.US, "%.2f", tcVal / hdlVal)
        } else null

        val lipidParams = listOf(
            LabTestItemDef(
                code = "T.Chol",
                name = if (appLanguage == "ar") "الكوليسترول الكلي (Total Cholesterol)" else "Total Cholesterol",
                normalRange = if (appLanguage == "ar") "أقل من 200 ملغ/ديسيلتر (< 200 mg/dL)" else "< 200 mg/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH): أقل من 5.2 مليمول/لتر (< 5.2 mmol/L)" else "SI (KFUH): < 5.2 mmol/L",
                latestValue = tcVal,
                status = evaluateUpperLimit(tcVal, 200.0),
                unit = "mg/dL",
                internationalUnit = "mmol/L",
                internationalValue = tcVal?.let { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) }
            ),
            LabTestItemDef(
                code = "Triglycerides",
                name = if (appLanguage == "ar") "الدهون الثلاثية (Triglycerides)" else "Triglycerides",
                normalRange = if (appLanguage == "ar") "أقل من 150 ملغ/ديسيلتر (< 150 mg/dL)" else "< 150 mg/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH): أقل من 1.7 مليمول/لتر (< 1.7 mmol/L)" else "SI (KFUH): < 1.7 mmol/L",
                latestValue = tgVal,
                status = evaluateUpperLimit(tgVal, 150.0),
                unit = "mg/dL",
                internationalUnit = "mmol/L",
                internationalValue = tgVal?.let { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.triglyceridesMgToMmol(it)) }
            ),
            LabTestItemDef(
                code = "HDL",
                name = if (appLanguage == "ar") "الكوليسترول النافع (HDL - High Density)" else "HDL Cholesterol",
                normalRange = if (appLanguage == "ar") "أعلى من 40 ملغ/ديسيلتر (> 40 mg/dL)" else "> 40 mg/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH): أعلى من 1.0 مليمول/لتر (> 1.0 mmol/L)" else "SI (KFUH): > 1.0 mmol/L",
                latestValue = hdlVal,
                status = evaluateLowerLimit(hdlVal, 40.0),
                unit = "mg/dL",
                internationalUnit = "mmol/L",
                internationalValue = hdlVal?.let { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) }
            ),
            LabTestItemDef(
                code = "LDL",
                name = if (appLanguage == "ar") "الكوليسترول الضار (LDL - Low Density)" else "LDL Cholesterol",
                normalRange = if (appLanguage == "ar") "أقل من 100 ملغ/ديسيلتر (< 100 mg/dL)" else "< 100 mg/dL",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (KFUH): أقل من 2.6 مليمول/لتر (< 2.6 mmol/L)" else "SI (KFUH): < 2.6 mmol/L",
                latestValue = ldlVal,
                status = evaluateUpperLimit(ldlVal, 100.0),
                unit = "mg/dL",
                internationalUnit = "mmol/L",
                internationalValue = ldlVal?.let { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) }
            )
        )

        lipidParams.forEach { item ->
            LabMetricCard(item = item, appLanguage = appLanguage)
        }

        // Risk Factor Card
        if (calculatedRiskFactor != null) {
            val rfNum = calculatedRiskFactor.toDoubleOrNull() ?: 0.0
            val isSafe = rfNum < 4.5
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSafe) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFD32F2F).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, if (isSafe) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFD32F2F).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (appLanguage == "ar") "معدل خطورة القلب والأوعية (RF = TC / HDL):" else "Atherogenic Risk Factor (TC / HDL):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (appLanguage == "ar") "المعدل المثالي الصحي: أقل من 4.5 (نسبة لا تتأثر باختلاف وحدات القياس)" else "Optimal safe ratio: < 4.5 (Unitless ratio)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$calculatedRiskFactor ${if (isSafe) "✓" else "⚠"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSafe) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (appLanguage == "ar") "سجل دهنيات الدم السابقة:" else "Previous Lipid Records:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = viewMode == "TABLE",
                    onClick = { viewMode = "TABLE" },
                    label = { Text(if (appLanguage == "ar") "جدول مقارن" else "Table", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = viewMode == "CARDS",
                    onClick = { viewMode = "CARDS" },
                    label = { Text(if (appLanguage == "ar") "بطاقات" else "Cards", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        if (appointmentsWithLipids.isNotEmpty()) {
            if (viewMode == "TABLE") {
                val lipidTableRows = listOf(
                    LabRowDef(
                        title = if (appLanguage == "ar") "الكوليسترول الكلي" else "Total Chol",
                        code = "T.Chol",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: < 200 mg/dL" else "Normal: <200 mg/dL",
                        internationalRange = "SI: < 5.2 mmol/L",
                        getValue = { it.tCholestrol },
                        formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) },
                        isAbnormal = { it >= 200.0 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "الكوليسترول الضار" else "LDL Chol",
                        code = "LDL",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: < 100 mg/dL" else "Normal: <100 mg/dL",
                        internationalRange = "SI: < 2.6 mmol/L",
                        getValue = { it.ldl },
                        formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) },
                        isAbnormal = { it >= 100.0 },
                        isHigherWorse = true
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "الكوليسترول النافع" else "HDL Chol",
                        code = "HDL",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: > 40 mg/dL" else "Normal: >40 mg/dL",
                        internationalRange = "SI: > 1.0 mmol/L",
                        getValue = { it.hdl },
                        formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) },
                        isAbnormal = { it < 40.0 },
                        isHigherWorse = false
                    ),
                    LabRowDef(
                        title = if (appLanguage == "ar") "الدهون الثلاثية" else "Triglycerides",
                        code = "TG",
                        conventionalRange = if (appLanguage == "ar") "طبيعي: < 150 mg/dL" else "Normal: <150 mg/dL",
                        internationalRange = "SI: < 1.7 mmol/L",
                        getValue = { it.triglycerides },
                        formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                        formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.triglyceridesMgToMmol(it)) },
                        isAbnormal = { it >= 150.0 },
                        isHigherWorse = true
                    )
                )

                GenericLabHistoryTable(
                    appointments = appointmentsWithLipids,
                    rows = lipidTableRows,
                    appLanguage = appLanguage
                )
            } else {
                appointmentsWithLipids.reversed().forEach { appt ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(appt.dateTime))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "📅 $dateStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = appt.hospitalName.ifBlank { "العيادة" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val items = mutableListOf<String>()
                            if (appt.tCholestrol != null) items.add("TC: ${LabUnitConverter.formatCholesterolDual(appt.tCholestrol, appLanguage)}")
                            if (appt.triglycerides != null) items.add("TG: ${LabUnitConverter.formatTriglyceridesDual(appt.triglycerides, appLanguage)}")
                            if (appt.hdl != null) items.add("HDL: ${LabUnitConverter.formatCholesterolDual(appt.hdl, appLanguage)}")
                            if (appt.ldl != null) items.add("LDL: ${LabUnitConverter.formatCholesterolDual(appt.ldl, appLanguage)}")
                            if (items.isEmpty() && appt.lipidsProfile.isNotBlank()) items.add(appt.lipidsProfile)
                            Text(text = items.joinToString(" • "), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            EmptyLabStateCard(
                message = if (appLanguage == "ar") "لم يتم تسجيل أي قراءات لدهنيات الدم بعد."
                else "No lipid readings recorded yet."
            )
        }
    }
}

// -------------------------------------------------------------
// Page 4: Glycated Haemoglobin (HbA1c) & Other Tests
// -------------------------------------------------------------
@Composable
fun OtherTestsAndHbA1cPanel(
    allAppointments: List<Appointment>,
    appLanguage: String
) {
    val appointmentsWithOther = remember(allAppointments) {
        allAppointments
            .filter { it.glycatedHaemoglobin != null || it.otherBloodTests.isNotBlank() }
            .sortedBy { it.dateTime }
    }
    val latestAppt = appointmentsWithOther.lastOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF6A1B9A).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFF6A1B9A).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        text = if (appLanguage == "ar") "السكر التراكمي (HbA1c) بالنظامين NGSP (%) و IFCC (mmol/mol)" else "HbA1c in NGSP (%) & IFCC (mmol/mol)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A1B9A)
                    )
                    Text(
                        text = if (appLanguage == "ar")
                            "يتم عرض السكر التراكمي بالنسبة المئوية (%) ونظام الاتحاد الدولي للكيمياء السريرية (mmol/mol) المعتمد عالمياً."
                        else
                            "Displays Glycated Hemoglobin in both NGSP % and International IFCC mmol/mol units.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val hba1cVal = latestAppt?.glycatedHaemoglobin
        val hba1cStatus = evaluateRange(hba1cVal, 4.0, 5.6)

        LabMetricCard(
            item = LabTestItemDef(
                code = "HbA1c",
                name = if (appLanguage == "ar") "الهيموغلوبين السكري / التراكمي (Glycated Haemoglobin)" else "Glycated Hemoglobin (HbA1c)",
                normalRange = if (appLanguage == "ar") "4.0% - 5.6% (طبيعي) | 5.7%-6.4% (ما قبل السكري)" else "4.0% - 5.6% (Normal)",
                internationalRange = if (appLanguage == "ar") "النظام الدولي (IFCC): 20 - 38 مليمول/مول (mmol/mol)" else "SI (IFCC): 20 - 38 mmol/mol",
                latestValue = hba1cVal,
                status = hba1cStatus,
                unit = "%",
                internationalUnit = "mmol/mol",
                internationalValue = hba1cVal?.let { String.format(Locale.US, "%.0f mmol/mol", LabUnitConverter.hba1cPercentToMmolMol(it)) }
            ),
            appLanguage = appLanguage
        )

        if (appointmentsWithOther.isNotEmpty()) {
            val hba1cTableRows = listOf(
                LabRowDef(
                    title = if (appLanguage == "ar") "السكر التراكمي" else "HbA1c",
                    code = "HbA1c",
                    conventionalRange = if (appLanguage == "ar") "طبيعي: 4.0% - 5.6%" else "Normal: 4.0-5.6%",
                    internationalRange = "SI: 20 - 38 mmol/mol",
                    getValue = { it.glycatedHaemoglobin },
                    formatValue = { String.format(Locale.US, "%.1f%%", it) },
                    formatSiValue = { String.format(Locale.US, "%.0f mmol/mol", LabUnitConverter.hba1cPercentToMmolMol(it)) },
                    isAbnormal = { it !in 4.0..5.6 },
                    isHigherWorse = true
                )
            )

            Text(
                text = if (appLanguage == "ar") "سجل قراءات السكر التراكمي السابقة:" else "Previous HbA1c Logs:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            GenericLabHistoryTable(
                appointments = appointmentsWithOther,
                rows = hba1cTableRows,
                appLanguage = appLanguage
            )
        }

        // General Roaccutane Safety Protocol Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (appLanguage == "ar") "إرشادات الفحوصات الإلزامية خلال كورس الرواكتان:" else "Standard Recommended Testing Protocol:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (appLanguage == "ar")
                        "• فحص وظائف الكبد (ALT / AST) ودهنيات الدم قبل بدء الكورس ثم بعد 4-8 أسابيع.\n• فحص الحمل المخبري (Beta-hCG) سلبي إلزامي شهرياً للإناث في سن الإنجاب.\n• صورة الدم الكاملة (CBC) للتأكد من سلامة كريات الدم البيضاء والصفائح الدموية."
                    else
                        "• LFTs & Lipids at baseline and 4-8 weeks.\n• Monthly pregnancy test for females.\n• CBC check periodically for white cells & platelets count.",
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Page 5: All Labs Master Table (جدول التحاليل الشامل لجميع القراءات)
// -------------------------------------------------------------
@Composable
fun AllLabsMasterTablePanel(
    allAppointments: List<Appointment>,
    appLanguage: String
) {
    val appointmentsWithAnyLab = remember(allAppointments) {
        allAppointments
            .filter {
                it.alt != null || it.ast != null || it.bilirubin != null || it.alkalinePhosphate != null ||
                it.albuminInSerum != null || it.creatinine != null || it.urea != null ||
                it.tCholestrol != null || it.ldl != null || it.hdl != null || it.triglycerides != null ||
                it.glycatedHaemoglobin != null
            }
            .sortedBy { it.dateTime } // Chronological
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Column {
                    Text(
                        text = if (appLanguage == "ar") "جدول التحاليل الشامل لجميع الزيارات" else "Comprehensive Master Lab Table",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (appLanguage == "ar")
                            "يعرض جميع فحوصات الكبد، الكلى، دهنيات الدم، والسكر التراكمي في جدول مقارن واحد بوحدات القياس العالمية."
                        else
                            "Consolidated comparative table displaying all liver, kidney, lipid, and glycemic biomarkers across visits.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (appointmentsWithAnyLab.isNotEmpty()) {
            val allRows = listOf(
                // Liver
                LabRowDef(
                    title = if (appLanguage == "ar") "ALT (SGPT)" else "ALT",
                    code = "ALT",
                    conventionalRange = "7 - 56 U/L",
                    internationalRange = "0.12 - 0.93 µkat/L",
                    getValue = { it.alt },
                    formatValue = { String.format(Locale.US, "%.0f U/L", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f µkat", LabUnitConverter.enzymeUlToUkat(it)) },
                    isAbnormal = { it !in 7.0..56.0 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "AST (SGOT)" else "AST",
                    code = "AST",
                    conventionalRange = "10 - 40 U/L",
                    internationalRange = "0.17 - 0.67 µkat/L",
                    getValue = { it.ast },
                    formatValue = { String.format(Locale.US, "%.0f U/L", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f µkat", LabUnitConverter.enzymeUlToUkat(it)) },
                    isAbnormal = { it !in 10.0..40.0 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "البيليروبين" else "Bilirubin",
                    code = "Bilirubin",
                    conventionalRange = "0.1 - 1.2 mg/dL",
                    internationalRange = "1.7 - 20.5 µmol/L",
                    getValue = { it.bilirubin },
                    formatValue = { String.format(Locale.US, "%.2f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.1f µmol/L", LabUnitConverter.bilirubinMgToUmol(it)) },
                    isAbnormal = { it !in 0.1..1.2 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "الفوسفاتاز القلوي" else "ALP",
                    code = "ALP",
                    conventionalRange = "44 - 147 U/L",
                    internationalRange = "0.73 - 2.45 µkat/L",
                    getValue = { it.alkalinePhosphate },
                    formatValue = { String.format(Locale.US, "%.0f U/L", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f µkat", LabUnitConverter.enzymeUlToUkat(it)) },
                    isAbnormal = { it !in 44.0..147.0 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "ألبومين المصل" else "Albumin",
                    code = "Albumin",
                    conventionalRange = "3.5 - 5.5 g/dL",
                    internationalRange = "35 - 55 g/L",
                    getValue = { it.albuminInSerum },
                    formatValue = { String.format(Locale.US, "%.2f g/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.1f g/L", LabUnitConverter.albuminGdlToGl(it)) },
                    isAbnormal = { it !in 3.5..5.5 },
                    isHigherWorse = false
                ),
                // Kidney
                LabRowDef(
                    title = if (appLanguage == "ar") "الكرياتينين" else "Creatinine",
                    code = "Creatinine",
                    conventionalRange = "0.6 - 1.2 mg/dL",
                    internationalRange = "53 - 106 µmol/L",
                    getValue = { it.creatinine },
                    formatValue = { String.format(Locale.US, "%.2f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.1f µmol/L", LabUnitConverter.creatinineMgToUmol(it)) },
                    isAbnormal = { it !in 0.6..1.2 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "يوريا الدم (BUN)" else "Urea",
                    code = "Urea",
                    conventionalRange = "7 - 20 mg/dL",
                    internationalRange = "2.5 - 7.1 mmol/L",
                    getValue = { it.urea },
                    formatValue = { String.format(Locale.US, "%.1f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.1f mmol/L", LabUnitConverter.ureaBUNMgToMmol(it)) },
                    isAbnormal = { it !in 7.0..20.0 }
                ),
                // Lipids
                LabRowDef(
                    title = if (appLanguage == "ar") "الكوليسترول الكلي" else "Total Chol",
                    code = "T.Chol",
                    conventionalRange = "< 200 mg/dL",
                    internationalRange = "< 5.2 mmol/L",
                    getValue = { it.tCholestrol },
                    formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) },
                    isAbnormal = { it >= 200.0 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "الكوليسترول الضار" else "LDL",
                    code = "LDL",
                    conventionalRange = "< 100 mg/dL",
                    internationalRange = "< 2.6 mmol/L",
                    getValue = { it.ldl },
                    formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) },
                    isAbnormal = { it >= 100.0 }
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "الكوليسترول النافع" else "HDL",
                    code = "HDL",
                    conventionalRange = "> 40 mg/dL",
                    internationalRange = "> 1.0 mmol/L",
                    getValue = { it.hdl },
                    formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.cholesterolMgToMmol(it)) },
                    isAbnormal = { it < 40.0 },
                    isHigherWorse = false
                ),
                LabRowDef(
                    title = if (appLanguage == "ar") "الدهون الثلاثية" else "Triglycerides",
                    code = "TG",
                    conventionalRange = "< 150 mg/dL",
                    internationalRange = "< 1.7 mmol/L",
                    getValue = { it.triglycerides },
                    formatValue = { String.format(Locale.US, "%.0f mg/dL", it) },
                    formatSiValue = { String.format(Locale.US, "%.2f mmol/L", LabUnitConverter.triglyceridesMgToMmol(it)) },
                    isAbnormal = { it >= 150.0 }
                ),
                // HbA1c
                LabRowDef(
                    title = if (appLanguage == "ar") "السكر التراكمي" else "HbA1c",
                    code = "HbA1c",
                    conventionalRange = "4.0% - 5.6%",
                    internationalRange = "20 - 38 mmol/mol",
                    getValue = { it.glycatedHaemoglobin },
                    formatValue = { String.format(Locale.US, "%.1f%%", it) },
                    formatSiValue = { String.format(Locale.US, "%.0f mmol/mol", LabUnitConverter.hba1cPercentToMmolMol(it)) },
                    isAbnormal = { it !in 4.0..5.6 }
                )
            )

            GenericLabHistoryTable(
                appointments = appointmentsWithAnyLab,
                rows = allRows,
                appLanguage = appLanguage
            )
        } else {
            EmptyLabStateCard(
                message = if (appLanguage == "ar") "لا توجد أي نتائج مخبرية مسجلة بعد لعرض الجدول الشامل."
                else "No lab records available yet to display the comprehensive table."
            )
        }
    }
}

// -------------------------------------------------------------
// Shared Data Definitions & Table Composables
// -------------------------------------------------------------
data class LabTestItemDef(
    val code: String,
    val name: String,
    val normalRange: String,
    val internationalRange: String = "",
    val latestValue: Double?,
    val status: LabStatus,
    val unit: String,
    val internationalUnit: String = "",
    val internationalValue: String? = null
)

enum class LabStatus {
    NORMAL, HIGH, LOW, UNRECORDED
}

fun evaluateRange(value: Double?, min: Double, max: Double): LabStatus {
    if (value == null) return LabStatus.UNRECORDED
    return when {
        value < min -> LabStatus.LOW
        value > max -> LabStatus.HIGH
        else -> LabStatus.NORMAL
    }
}

fun evaluateUpperLimit(value: Double?, max: Double): LabStatus {
    if (value == null) return LabStatus.UNRECORDED
    return if (value > max) LabStatus.HIGH else LabStatus.NORMAL
}

fun evaluateLowerLimit(value: Double?, min: Double): LabStatus {
    if (value == null) return LabStatus.UNRECORDED
    return if (value < min) LabStatus.LOW else LabStatus.NORMAL
}

@Composable
fun LabMetricCard(
    item: LabTestItemDef,
    appLanguage: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (appLanguage == "ar") "المعدل الطبيعي:" else "Normal:"} ${item.normalRange}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.internationalRange.isNotBlank()) {
                        Text(
                            text = item.internationalRange,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (item.status) {
                        LabStatus.NORMAL -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                        LabStatus.HIGH -> Color(0xFFD32F2F).copy(alpha = 0.12f)
                        LabStatus.LOW -> Color(0xFFFFA000).copy(alpha = 0.12f)
                        LabStatus.UNRECORDED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    val statusText = when (item.status) {
                        LabStatus.NORMAL -> if (appLanguage == "ar") "طبيعي ✓" else "Normal ✓"
                        LabStatus.HIGH -> if (appLanguage == "ar") "مرتفع ⚠" else "High ⚠"
                        LabStatus.LOW -> if (appLanguage == "ar") "منخفض ⚠" else "Low ⚠"
                        LabStatus.UNRECORDED -> if (appLanguage == "ar") "غير مسجل" else "Not Logged"
                    }
                    val statusColor = when (item.status) {
                        LabStatus.NORMAL -> Color(0xFF2E7D32)
                        LabStatus.HIGH -> Color(0xFFD32F2F)
                        LabStatus.LOW -> Color(0xFFFFA000)
                        LabStatus.UNRECORDED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (appLanguage == "ar") "آخر قراءة مسجلة (بالوحدتين):" else "Latest Dual Reading:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.latestValue != null) "${item.latestValue} ${item.unit}" else if (appLanguage == "ar") "لا توجد قراءة" else "No Data",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (item.status) {
                            LabStatus.NORMAL -> Color(0xFF2E7D32)
                            LabStatus.HIGH -> Color(0xFFD32F2F)
                            LabStatus.LOW -> Color(0xFFFFA000)
                            LabStatus.UNRECORDED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (item.internationalValue != null) {
                        Text(
                            text = "≈ ${item.internationalValue}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Definition of a parameter row in the Lab Results History Table.
 */
data class LabRowDef(
    val title: String,
    val code: String,
    val conventionalRange: String,
    val internationalRange: String,
    val getValue: (Appointment) -> Double?,
    val formatValue: (Double) -> String,
    val formatSiValue: (Double) -> String,
    val isAbnormal: (Double) -> Boolean,
    val isHigherWorse: Boolean = true
)

/**
 * Reusable Double-Scrollable Lab History Table replicating the exact model from the Analytics page.
 */
@Composable
fun GenericLabHistoryTable(
    appointments: List<Appointment>,
    rows: List<LabRowDef>,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Parameter & Norms Column (Fixed on Start side)
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .width(185.dp)
            ) {
                // Header Cell
                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = if (appLanguage == "ar") "التحليل والمعدل المرجعي" else "Parameter & Norms",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (appLanguage == "ar") "الوحدة التقليدية • النظام الدولي" else "Conv. • SI Units",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Rows
                rows.forEachIndexed { idx, rowDef ->
                    Box(
                        modifier = Modifier
                            .height(68.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = rowDef.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = rowDef.conventionalRange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = rowDef.internationalRange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (idx < rows.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            // Columns for each recorded appointment
            appointments.forEachIndexed { colIdx, appt ->
                val dateStr = try {
                    val loc = if (appLanguage == "ar") Locale("ar") else Locale.US
                    SimpleDateFormat("dd MMM yyyy", loc).format(Date(appt.dateTime))
                } catch (e: Exception) {
                    "–"
                }

                val hospitalLabel = appt.hospitalName.takeIf { it.isNotBlank() } ?: (if (appLanguage == "ar") "زيارة" else "Visit")

                Column(
                    modifier = Modifier
                        .width(115.dp)
                        .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    // Column Header Cell (Date & Hospital)
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dateStr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = hospitalLabel.take(14),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Value cells for each parameter
                    rows.forEachIndexed { rowIdx, rowDef ->
                        val value = rowDef.getValue(appt)
                        // Scan backwards for the most recent prior entered result; if none exists,
                        // this reading is treated as the first result and gets NO trend indicator.
                        val prevValue = if (colIdx > 0) {
                            (colIdx - 1 downTo 0).firstNotNullOfOrNull { prevIdx ->
                                rowDef.getValue(appointments[prevIdx])
                            }
                        } else null

                        val trend = if (value != null && prevValue != null) {
                            if (value > prevValue) {
                                if (rowDef.isHigherWorse) "🔺" else "💚"
                            } else if (value < prevValue) {
                                if (rowDef.isHigherWorse) "💚" else "🔺"
                            } else ""
                        } else ""

                        val isAbn = value != null && rowDef.isAbnormal(value)

                        val cellBg = when {
                            value == null -> Color.Transparent
                            isAbn -> Color(0xFFFDE8E8) // Soft Red
                            else -> Color(0xFFE8F5E9) // Soft Green
                        }

                        val cellTextColor = when {
                            value == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isAbn -> Color(0xFFE53935) // Deep Red
                            else -> Color(0xFF2E7D32) // Deep Green
                        }

                        Box(
                            modifier = Modifier
                                .height(68.dp)
                                .fillMaxWidth()
                                .background(cellBg)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = rowDef.formatValue(value),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = cellTextColor
                                        )
                                        if (trend.isNotEmpty()) {
                                            Text(text = trend, fontSize = 9.sp)
                                        }
                                    }
                                    Text(
                                        text = rowDef.formatSiValue(value),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cellTextColor.copy(alpha = 0.85f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "–",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (rowIdx < rows.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Table Legend & Units Reference Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (appLanguage == "ar") "💡 دليل الوحدات العالمية ومؤشرات التغير:" else "💡 Units Legend & Clinical Indicators:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (appLanguage == "ar") "• 🟢 خلفية خضراء: طبيعي" else "• 🟢 Green: Normal",
                        fontSize = 10.5.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (appLanguage == "ar") "• 🔴 خلفية حمراء: خارج المعدل" else "• 🔴 Red: Abnormal",
                        fontSize = 10.5.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (appLanguage == "ar") "• 🔺/💚: مؤشر التغير" else "• 🔺/💚: Trend",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (appLanguage == "ar")
                        "• النظام الدولي (SI): يشمل مليمول/لتر (mmol/L) وميكرومول/لتر (µmol/L) المعتمد في مستشفى الملك فهد (KFUH) والمختبرات العالمية."
                    else
                        "• SI Units: Includes mmol/L and µmol/L used in KFUH and international healthcare networks.",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyLabStateCard(message: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
