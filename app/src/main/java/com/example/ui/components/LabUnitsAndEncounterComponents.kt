package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Appointment
import com.example.data.DailyLog
import com.example.data.DispensedMedication
import com.example.data.MedicalPrescription
import com.example.utils.LabUnitConverter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit option definition with conversion functions to/from standard representation.
 */
data class BiomarkerUnitOption(
    val unit: String,
    val labelAr: String,
    val rangeAr: String,
    val rangeEn: String,
    val toStandard: (Double) -> Double,   // Converts input in this unit to standard storage
    val fromStandard: (Double) -> Double // Converts standard storage value to this unit
)

object BiomarkerUnitsCatalog {
    val ALBUMIN = listOf(
        BiomarkerUnitOption("g/dL", "غرام/ديسيلتر", "3.5 - 5.5 g/dL", "3.5 - 5.5 g/dL", { it }, { it }),
        BiomarkerUnitOption("g/L", "غرام/لتر (SI)", "35 - 55 g/L", "35 - 55 g/L", { it / 10.0 }, { it * 10.0 })
    )

    val BILIRUBIN = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر", "0.1 - 1.2 mg/dL", "0.1 - 1.2 mg/dL", { it }, { it }),
        BiomarkerUnitOption("µmol/L", "ميكرومول/لتر (SI)", "1.7 - 20.5 µmol/L", "1.7 - 20.5 µmol/L", { it / 17.1 }, { it * 17.1 })
    )

    val CREATININE = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر", "0.6 - 1.2 mg/dL", "0.6 - 1.2 mg/dL", { it }, { it }),
        BiomarkerUnitOption("µmol/L", "ميكرومول/لتر (SI)", "53 - 106 µmol/L", "53 - 106 µmol/L", { it / 88.4 }, { it * 88.4 })
    )

    val UREA = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر (BUN)", "7 - 20 mg/dL", "7 - 20 mg/dL", { it }, { it }),
        BiomarkerUnitOption("mmol/L", "مليمول/لتر (SI)", "2.5 - 7.1 mmol/L", "2.5 - 7.1 mmol/L", { it * 2.80 }, { it / 2.80 })
    )

    val CHOLESTEROL = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر", "< 200 mg/dL", "< 200 mg/dL", { it }, { it }),
        BiomarkerUnitOption("mmol/L", "مليمول/لتر (SI)", "< 5.2 mmol/L", "< 5.2 mmol/L", { it * 38.67 }, { it / 38.67 })
    )

    val LDL = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر", "< 100 mg/dL", "< 100 mg/dL", { it }, { it }),
        BiomarkerUnitOption("mmol/L", "مليمول/لتر (SI)", "< 2.6 mmol/L", "< 2.6 mmol/L", { it * 38.67 }, { it / 38.67 })
    )

    val HDL = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر", "> 40 mg/dL", "> 40 mg/dL", { it }, { it }),
        BiomarkerUnitOption("mmol/L", "مليمول/لتر (SI)", "> 1.0 mmol/L", "> 1.0 mmol/L", { it * 38.67 }, { it / 38.67 })
    )

    val TRIGLYCERIDES = listOf(
        BiomarkerUnitOption("mg/dL", "ملغ/ديسيلتر", "< 150 mg/dL", "< 150 mg/dL", { it }, { it }),
        BiomarkerUnitOption("mmol/L", "مليمول/لتر (SI)", "< 1.7 mmol/L", "< 1.7 mmol/L", { it * 88.57 }, { it / 88.57 })
    )

    val ENZYMES = listOf(
        BiomarkerUnitOption("U/L", "وحدة/لتر", "طبيعي", "Normal", { it }, { it }),
        BiomarkerUnitOption("µkat/L", "ميكروكاتال (SI)", "طبيعي", "Normal", { it / 0.0167 }, { it * 0.0167 })
    )

    val HBA1C = listOf(
        BiomarkerUnitOption("%", "نسبة مئوية (%)", "4.0% - 5.6%", "4.0% - 5.6%", { it }, { it }),
        BiomarkerUnitOption("mmol/mol", "مليمول/مول (IFCC)", "20 - 38 mmol/mol", "20 - 38 mmol/mol", { LabUnitConverter.hba1cMmolMolToPercent(it) }, { LabUnitConverter.hba1cPercentToMmolMol(it) })
    )
}

/**
 * Text field with interactive Unit selection dropdown.
 * Updates reference range label dynamically and converts entered values automatically on unit change.
 */
@Composable
fun LabInputWithUnitSelector(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    unitOptions: List<BiomarkerUnitOption>,
    modifier: Modifier = Modifier,
    appLanguage: String = "ar",
    testTag: String = ""
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentOption = unitOptions.find { it.unit == selectedUnit } ?: unitOptions.first()
    val activeRange = if (appLanguage == "ar") currentOption.rangeAr else currentOption.rangeEn

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = "$label ($activeRange)",
                fontSize = 11.5.sp,
                maxLines = 1
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = currentOption.unit,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select unit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    unitOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${opt.unit} - ${opt.labelAr}",
                                        fontSize = 12.sp,
                                        fontWeight = if (opt.unit == selectedUnit) FontWeight.Bold else FontWeight.Normal,
                                        color = if (opt.unit == selectedUnit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "المعدل: ${opt.rangeAr}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                if (opt.unit != selectedUnit) {
                                    // Automatic live conversion if value is present
                                    val currentNum = value.toDoubleOrNull()
                                    if (currentNum != null) {
                                        val stdVal = currentOption.toStandard(currentNum)
                                        val converted = opt.fromStandard(stdVal)
                                        onValueChange(String.format(Locale.US, if (converted % 1.0 == 0.0) "%.0f" else "%.1f", converted))
                                    }
                                    onUnitSelected(opt.unit)
                                }
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog to link/unlink secondary visits (e.g. Lab Draw on Dec 1 linked to Parent Clinic Visit on Dec 2).
 */
@Composable
fun LinkSecondaryVisitsDialog(
    parentAppointment: Appointment,
    allAppointments: List<Appointment>,
    appLanguage: String,
    onLinkVisit: (Appointment, Boolean) -> Unit, // (childVisit, isLinked)
    onDismiss: () -> Unit
) {
    // Eligible child visits: other appointments not equal to this parent and not having a different parent
    val candidates = remember(allAppointments, parentAppointment) {
        allAppointments.filter { it.id != parentAppointment.id && (it.parentAppointmentId == null || it.parentAppointmentId == parentAppointment.id) }
            .sortedBy { it.dateTime }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = if (appLanguage == "ar") "ربط الزيارات والتحاليل التابعة" else "Link Secondary Visits",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val parentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(parentAppointment.dateTime))
                            Text(
                                text = "${parentAppointment.hospitalName} ($parentDateStr)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = if (appLanguage == "ar")
                        "يمكنك ربط زيارة التحاليل وسحب الدم (مثلاً قبل يوم من الموعد) لتظهر كقائمة تابعة وموحدة مع هذا الموعد الطبي."
                    else
                        "Link laboratory test draws or secondary visits to this primary clinical encounter.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (candidates.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (appLanguage == "ar") "لا توجد مواعيد أخرى متاحة للربط حالياً." else "No other visits available to link.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.id }) { cand ->
                            val isLinked = cand.parentAppointmentId == parentAppointment.id
                            val candDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(cand.dateTime))
                            val isLab = cand.appointmentType == "LAB" || cand.alt != null || cand.tCholestrol != null || cand.creatinine != null

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isLinked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(if (isLinked) 1.5.dp else 0.5.dp, if (isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Checkbox(
                                            checked = isLinked,
                                            onCheckedChange = { checked ->
                                                onLinkVisit(cand, checked)
                                            }
                                        )
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = cand.reason,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isLab) Color(0xFFE65100).copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (isLab) (if (appLanguage == "ar") "🧪 تحاليل" else "LAB") else (if (appLanguage == "ar") "🏥 عيادة" else "CLINIC"),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isLab) Color(0xFFE65100) else MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "📅 $candDateStr • ${cand.hospitalName}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (appLanguage == "ar") "إغلاق وحفظ الربط" else "Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Unified Patient Encounter View displaying:
 * 1. Primary Clinic Consultation details
 * 2. Secondary Linked Lab Visits & Blood Tests
 * 3. Linked Medical Prescription & Dispenses
 * 4. Same-Day Daily Symptoms & Doses Logs
 */
@Composable
fun UnifiedEncounterDetailsDialog(
    parentAppointment: Appointment,
    linkedSecondaryVisits: List<Appointment>,
    linkedPrescription: MedicalPrescription?,
    dispensesForLinkedRx: List<DispensedMedication>,
    sameDayLog: DailyLog?,
    appLanguage: String,
    onDismiss: () -> Unit,
    onManageSecondaryLinks: () -> Unit
) {
    val parentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(parentAppointment.dateTime))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(22.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = if (appLanguage == "ar") "الملف السريري الشامل للموعد" else "Comprehensive Encounter File",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📅 $parentDateStr • ${parentAppointment.hospitalName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section 1: Primary Clinical Consultation
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "🏥 موعد العيادة والاستشارة الرئيسي" else "🏥 Primary Clinic Consultation",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (parentAppointment.isCompleted) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32).copy(alpha = 0.15f)) {
                                            Text(
                                                text = if (appLanguage == "ar") "مكتمل ✓" else "Completed ✓",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(text = "${parentAppointment.hospitalName} • ${parentAppointment.reason}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (parentAppointment.doctorName.isNotBlank() || parentAppointment.clinic.isNotBlank()) {
                                    Text(text = "الطبيب: ${parentAppointment.doctorName.ifBlank { "غير محدد" }} | عيادة: ${parentAppointment.clinic.ifBlank { "–" }} | مبنى: ${parentAppointment.building.ifBlank { "–" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (parentAppointment.notes.isNotBlank()) {
                                    Text(text = "الملاحظات: ${parentAppointment.notes}", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                    }

                    // Section 2: Linked Secondary Lab Visits (قائمة مواعيد التحاليل التابعة)
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (appLanguage == "ar") "🧪 زيارات ومواعيد التحاليل المرتبطة" else "🧪 Linked Lab Visits",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                    TextButton(onClick = onManageSecondaryLinks, contentPadding = PaddingValues(0.dp)) {
                                        Text(if (appLanguage == "ar") "إدارة الربط" else "Manage", fontSize = 11.sp)
                                    }
                                }

                                if (linkedSecondaryVisits.isEmpty()) {
                                    Text(
                                        text = if (appLanguage == "ar") "لم يتم ربط زيارات تحاليل ثانوية بهذا الموعد بعد. اضغط على 'إدارة الربط' لربط فحص دم سابق أو قادم."
                                        else "No secondary lab visits linked. Tap 'Manage' to link blood draws.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    linkedSecondaryVisits.forEach { childAppt ->
                                        val childDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(childAppt.dateTime))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE65100).copy(alpha = 0.06f),
                                            border = BorderStroke(0.8.dp, Color(0xFFE65100).copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(text = "📅 $childDateStr • ${childAppt.hospitalName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                                    Text(text = childAppt.reason, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                // Biomarkers summary
                                                val labs = mutableListOf<String>()
                                                if (childAppt.alt != null) labs.add("ALT: ${childAppt.alt} U/L")
                                                if (childAppt.ast != null) labs.add("AST: ${childAppt.ast} U/L")
                                                if (childAppt.bilirubin != null) labs.add("Bilirubin: ${childAppt.bilirubin} mg/dL")
                                                if (childAppt.creatinine != null) labs.add("Creatinine: ${childAppt.creatinine} mg/dL")
                                                if (childAppt.tCholestrol != null) labs.add("T.Chol: ${childAppt.tCholestrol} mg/dL")
                                                if (childAppt.triglycerides != null) labs.add("TG: ${childAppt.triglycerides} mg/dL")
                                                if (labs.isNotEmpty()) {
                                                    Text(text = labs.joinToString(" • "), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Linked Medical Prescription (Only if user linked or exact date matched)
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (appLanguage == "ar") "📋 الوصفة الطبية المعتمدة" else "📋 Medical Prescription",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (linkedPrescription != null) {
                                    Text(text = "${linkedPrescription.medicationName} (${linkedPrescription.dailyDose})", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "رقم الوصفة: #${linkedPrescription.prescriptionNumber} • الطبيب: ${linkedPrescription.doctorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Text(
                                        text = if (appLanguage == "ar") "لا توجد وصفة مرتبطة بهذا الموعد (لم يتم ربطها يدوياً أو بتاريخ مطابق)."
                                        else "No prescription linked manually or on the exact same date.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Same-Day Symptoms & Dose Log
                    if (sameDayLog != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (appLanguage == "ar") "📖 سجل اليوميات والأعراض لنفس اليوم" else "📖 Same-Day Daily Log",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(text = "الجرعة المستهلكة: ${sameDayLog.doseMg} ملغ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    if (sameDayLog.symptoms.isNotBlank()) {
                                        Text(text = "الأعراض المدونة: ${sameDayLog.symptoms}", fontSize = 11.sp)
                                    }
                                    if (sameDayLog.notes.isNotBlank()) {
                                        Text(text = "الملاحظات: ${sameDayLog.notes}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (appLanguage == "ar") "إغلاق" else "Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
