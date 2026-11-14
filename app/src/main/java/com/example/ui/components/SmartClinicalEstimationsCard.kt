package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.RefillUrgency
import com.example.utils.SafetyStatus
import com.example.utils.TreatmentEstimations
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SmartClinicalEstimationsCard(
    estimations: TreatmentEstimations,
    appLanguage: String,
    onScheduleLabClick: () -> Unit,
    onDispenseMedicationClick: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDetails by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val courseEndFormatted = remember(estimations.estimatedCourseCompletionDateMillis) {
        sdf.format(Date(estimations.estimatedCourseCompletionDateMillis))
    }
    val lastCapsuleDateFormatted = remember(estimations.estimatedRunOutDateMillis) {
        sdf.format(Date(estimations.estimatedRunOutDateMillis))
    }
    val labDateFormatted = remember(estimations.nextUpcomingLabVisit, estimations.recommendedNextLabDateMillis) {
        val targetMillis = estimations.nextUpcomingLabVisit?.dateTime ?: estimations.recommendedNextLabDateMillis
        sdf.format(Date(targetMillis))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (appLanguage == "ar") "لوحة التقديرات والتكامل السريري الذكي" else "Smart Clinical Estimations Hub",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (appLanguage == "ar") "ربط حي بين الجرعات، الوصفات، ومواعيد التحاليل" else "Live cross-linking: doses, prescriptions & lab tests",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (appLanguage == "ar") "الشهر ${estimations.currentTreatmentMonth}" else "Month ${estimations.currentTreatmentMonth}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 4-Card Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Course Finish Estimation
                EstimationMetricTile(
                    icon = Icons.Default.Flag,
                    title = if (appLanguage == "ar") "إنهاء الكورس" else "Course Finish",
                    mainValue = courseEndFormatted,
                    subValue = if (appLanguage == "ar") "متبقي ${estimations.estimatedDaysToCourseCompletion} يوماً" else "${estimations.estimatedDaysToCourseCompletion} days left",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Last Capsule in Stock Date (synced with Dispensed Medications Tracker)
                val refillColor = when (estimations.refillDueUrgency) {
                    RefillUrgency.CRITICAL -> MaterialTheme.colorScheme.error
                    RefillUrgency.REFILL_SOON -> Color(0xFFE65100)
                    RefillUrgency.OK -> Color(0xFF2E7D32)
                }
                EstimationMetricTile(
                    icon = Icons.Default.LocalPharmacy,
                    title = if (appLanguage == "ar") "تاريخ آخر كبسولة" else "Last Capsule Date",
                    mainValue = lastCapsuleDateFormatted,
                    subValue = if (estimations.liveRemainingDays > 0) {
                        if (appLanguage == "ar") {
                            "متبقي ${estimations.liveRemainingDays} يوم • ${estimations.liveRemainingCapsules} كبسولة"
                        } else {
                            "Stock: ${estimations.liveRemainingDays} d • ${estimations.liveRemainingCapsules} caps"
                        }
                    } else {
                        if (appLanguage == "ar") "نفذت الكمية (0 كبسولة)" else "Depleted (0 caps)"
                    },
                    color = refillColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 3: Next Lab Test (Linked directly with upcoming LAB appointments)
                val nextLab = estimations.nextUpcomingLabVisit
                val nextLabSubValue = when {
                    nextLab != null -> {
                        val loc = nextLab.hospitalName.ifBlank { nextLab.reason }
                        if (appLanguage == "ar") "موعد مختبر قادم • $loc" else "Booked Lab • $loc"
                    }
                    estimations.nextUpcomingClinicVisit != null -> {
                        if (appLanguage == "ar") "مقترح قبل العيادة (غير مجدول)" else "Pre-clinic (not booked)"
                    }
                    else -> {
                        if (appLanguage == "ar") "فحص دوري مقترح (غير مجدول)" else "Monthly protocol (unbooked)"
                    }
                }
                EstimationMetricTile(
                    icon = Icons.Default.Science,
                    title = if (appLanguage == "ar") "الفحص القادم" else "Next Lab Draw",
                    mainValue = labDateFormatted,
                    subValue = nextLabSubValue,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToAppointments,
                    modifier = Modifier.weight(1f)
                )

                // Metric 4: Biomarker Safety Indicator
                val safetyColor = when (estimations.labSafetyAssessment.status) {
                    SafetyStatus.NORMAL -> Color(0xFF2E7D32)
                    SafetyStatus.WARNING -> Color(0xFFE65100)
                    SafetyStatus.ELEVATED -> MaterialTheme.colorScheme.error
                }
                val safetyLabel = when (estimations.labSafetyAssessment.status) {
                    SafetyStatus.NORMAL -> if (appLanguage == "ar") "آمن وطبيعي ✓" else "Normal ✓"
                    SafetyStatus.WARNING -> if (appLanguage == "ar") "قريب من الحد ⚠️" else "Borderline ⚠️"
                    SafetyStatus.ELEVATED -> if (appLanguage == "ar") "مؤشرات مرتفعة ⚠️" else "Elevated ⚠️"
                }
                EstimationMetricTile(
                    icon = Icons.Default.HealthAndSafety,
                    title = if (appLanguage == "ar") "الأمان المخبري" else "Lab Safety",
                    mainValue = safetyLabel,
                    subValue = if (appLanguage == "ar") "الكبد والدهنيات" else "LFTs & Lipids",
                    color = safetyColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Clinical Safety Details Banner (if elevated or clicked)
            if (estimations.labSafetyAssessment.status != SafetyStatus.NORMAL || expandedDetails) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (estimations.labSafetyAssessment.status == SafetyStatus.NORMAL)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(
                        0.8.dp,
                        if (estimations.labSafetyAssessment.status == SafetyStatus.NORMAL)
                            MaterialTheme.colorScheme.outlineVariant
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (estimations.labSafetyAssessment.status == SafetyStatus.NORMAL) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (estimations.labSafetyAssessment.status == SafetyStatus.NORMAL) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (appLanguage == "ar") estimations.labSafetyAssessment.summaryAr else estimations.labSafetyAssessment.summaryEn,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estimations.labSafetyAssessment.status == SafetyStatus.NORMAL) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                        estimations.labSafetyAssessment.details.forEach { detail ->
                            Text(text = "• $detail", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Quick Interactive Shortcuts Row for Seamless Accessibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Schedule Lab
                OutlinedButton(
                    onClick = onScheduleLabClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (appLanguage == "ar") "+ موعد تحاليل" else "+ Lab Test", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                // Quick Log Dispense
                OutlinedButton(
                    onClick = onDispenseMedicationClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (appLanguage == "ar") "+ تسجيل صرف" else "+ Dispense", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                // Toggle Details
                IconButton(
                    onClick = { expandedDetails = !expandedDetails },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EstimationMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    mainValue: String,
    subValue: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = if (onClick != null) modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() } else modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Text(
                text = mainValue,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
