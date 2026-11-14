package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyLog
import com.example.data.UserProfile
import com.example.ui.SYMPTOMS_AR
import com.example.ui.SYMPTOMS_EN
import com.example.ui.normalizeSymptomKey
import com.example.ui.t
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

enum class SideEffectCategory(val labelEn: String, val labelAr: String, val color: Color) {
    PHYSICAL("Physical Effects", "الأعراض الجسدية", Color(0xFF00897B)),
    PSYCHOLOGICAL("Psychological Effects", "الأعراض النفسية", Color(0xFF8E24AA))
}

enum class ChartFilterMode(val labelEn: String, val labelAr: String) {
    BOTH("Both Trends", "مقارنة المسارين"),
    PHYSICAL_ONLY("Physical Only", "الأعراض الجسدية"),
    PSYCHOLOGICAL_ONLY("Psychological Only", "الأعراض النفسية")
}

enum class ChartTimeGrouping(val labelEn: String, val labelAr: String) {
    WEEKLY("Weekly", "أسبوعي"),
    MONTHLY("Monthly", "شهري")
}

data class CourseTimelinePoint(
    val index: Int,
    val label: String,
    val fullDateRange: String,
    val physicalCount: Int,
    val psychologicalCount: Int,
    val physicalBreakdown: Map<String, Int>,
    val psychologicalBreakdown: Map<String, Int>,
    val logCount: Int
)

fun classifySymptomCategory(symptomKey: String): SideEffectCategory {
    val lower = symptomKey.trim().lowercase(Locale.ROOT)
    return when {
        lower.contains("mood") || lower.contains("مزاج") ||
        lower.contains("fog") || lower.contains("ذهن") || lower.contains("تشوش") ||
        lower.contains("depress") || lower.contains("كآبة") || lower.contains("اكتئاب") ||
        lower.contains("anxiety") || lower.contains("قلق") || lower.contains("توتر") ||
        lower.contains("irrit") || lower.contains("عصبية") ||
        lower.contains("psych") || lower.contains("نفس") -> {
            SideEffectCategory.PSYCHOLOGICAL
        }
        else -> SideEffectCategory.PHYSICAL
    }
}

fun getLocalizedSymptomName(symKey: String, appLanguage: String): String {
    val normalized = normalizeSymptomKey(symKey)
    val idx = SYMPTOMS_EN.indexOfFirst { it.equals(normalized, ignoreCase = true) }
    return if (appLanguage == "ar" && idx != -1) SYMPTOMS_AR[idx] else normalized
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SideEffectsTrendsLineChartCard(
    allLogs: List<DailyLog>,
    userProfile: UserProfile?,
    appLanguage: String,
    modifier: Modifier = Modifier,
    onExpandFullView: (() -> Unit)? = null
) {
    var filterMode by remember { mutableStateOf(ChartFilterMode.BOTH) }
    var timeGrouping by remember { mutableStateOf(ChartTimeGrouping.WEEKLY) }

    // Start of course computation
    val startMs = remember(userProfile, allLogs) {
        if ((userProfile?.startDate ?: 0L) > 0L) {
            userProfile!!.startDate
        } else {
            allLogs.mapNotNull { it.timestamp }.minOrNull() ?: System.currentTimeMillis()
        }
    }

    val nowMs = remember { System.currentTimeMillis() }
    val courseDays = remember(startMs, nowMs) {
        ((nowMs - startMs).coerceAtLeast(0) / (1000L * 60 * 60 * 24)).toInt()
    }

    val timelinePoints = remember(allLogs, startMs, timeGrouping, appLanguage) {
        val points = mutableListOf<CourseTimelinePoint>()
        val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
        val dateFmt = SimpleDateFormat("d MMM", if (appLanguage == "ar") Locale("ar") else Locale.US)

        if (timeGrouping == ChartTimeGrouping.WEEKLY) {
            val weeksCount = ((courseDays / 7) + 1).coerceIn(1, 36)
            for (w in 0 until weeksCount) {
                val wkStartCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, w * 7) }
                val wkEndCal = (wkStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }

                val physicalMap = mutableMapOf<String, Int>()
                val psychMap = mutableMapOf<String, Int>()
                var logsCount = 0

                for (dayOffset in 0..6) {
                    val dCal = (wkStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                    val dStr = String.format(Locale.US, "%04d-%02d-%02d", dCal.get(Calendar.YEAR), dCal.get(Calendar.MONTH) + 1, dCal.get(Calendar.DAY_OF_MONTH))
                    val log = allLogs.find { it.dateString == dStr }
                    if (log != null) {
                        logsCount++
                        if (log.symptoms.isNotBlank()) {
                            log.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawSym ->
                                val key = normalizeSymptomKey(rawSym)
                                if (key.isNotBlank()) {
                                    val cat = classifySymptomCategory(key)
                                    if (cat == SideEffectCategory.PHYSICAL) {
                                        physicalMap[key] = (physicalMap[key] ?: 0) + 1
                                    } else {
                                        psychMap[key] = (psychMap[key] ?: 0) + 1
                                    }
                                }
                            }
                        }
                    }
                }

                val rangeStr = "${dateFmt.format(wkStartCal.time)} - ${dateFmt.format(wkEndCal.time)}"
                val labelStr = if (appLanguage == "ar") "أسبوع ${w + 1}" else "W${w + 1}"

                points.add(
                    CourseTimelinePoint(
                        index = w,
                        label = labelStr,
                        fullDateRange = rangeStr,
                        physicalCount = physicalMap.values.sum(),
                        psychologicalCount = psychMap.values.sum(),
                        physicalBreakdown = physicalMap,
                        psychologicalBreakdown = psychMap,
                        logCount = logsCount
                    )
                )
            }
        } else {
            // Monthly grouping
            val monthsCount = ((courseDays / 30) + 1).coerceIn(1, 12)
            for (m in 0 until monthsCount) {
                val mStartCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, m * 30) }
                val mEndCal = (mStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 29) }

                val physicalMap = mutableMapOf<String, Int>()
                val psychMap = mutableMapOf<String, Int>()
                var logsCount = 0

                for (dayOffset in 0..29) {
                    val dCal = (mStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                    val dStr = String.format(Locale.US, "%04d-%02d-%02d", dCal.get(Calendar.YEAR), dCal.get(Calendar.MONTH) + 1, dCal.get(Calendar.DAY_OF_MONTH))
                    val log = allLogs.find { it.dateString == dStr }
                    if (log != null) {
                        logsCount++
                        if (log.symptoms.isNotBlank()) {
                            log.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawSym ->
                                val key = normalizeSymptomKey(rawSym)
                                if (key.isNotBlank()) {
                                    val cat = classifySymptomCategory(key)
                                    if (cat == SideEffectCategory.PHYSICAL) {
                                        physicalMap[key] = (physicalMap[key] ?: 0) + 1
                                    } else {
                                        psychMap[key] = (psychMap[key] ?: 0) + 1
                                    }
                                }
                            }
                        }
                    }
                }

                val rangeStr = "${dateFmt.format(mStartCal.time)} - ${dateFmt.format(mEndCal.time)}"
                val labelStr = if (appLanguage == "ar") "شهر ${m + 1}" else "M${m + 1}"

                points.add(
                    CourseTimelinePoint(
                        index = m,
                        label = labelStr,
                        fullDateRange = rangeStr,
                        physicalCount = physicalMap.values.sum(),
                        psychologicalCount = psychMap.values.sum(),
                        physicalBreakdown = physicalMap,
                        psychologicalBreakdown = psychMap,
                        logCount = logsCount
                    )
                )
            }
        }
        points
    }

    var selectedIndex by remember(timelinePoints) {
        mutableStateOf(if (timelinePoints.isNotEmpty()) timelinePoints.size - 1 else 0)
    }

    val selectedPoint = remember(selectedIndex, timelinePoints) {
        timelinePoints.getOrNull(selectedIndex) ?: timelinePoints.lastOrNull()
    }

    val totalPhysicalCount = remember(timelinePoints) { timelinePoints.sumOf { it.physicalCount } }
    val totalPsychCount = remember(timelinePoints) { timelinePoints.sumOf { it.psychologicalCount } }
    val maxSingleCount = remember(timelinePoints) {
        timelinePoints.maxOfOrNull { max(it.physicalCount, it.psychologicalCount) }?.coerceAtLeast(4) ?: 4
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("side_effects_trends_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Title and Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = t(
                                "Side Effects Course Trends",
                                "مسار الأعراض الجانبية طوال الكورس",
                                appLanguage
                            ),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = t(
                                "Physical vs. Psychological timeline",
                                "تتبع الأعراض الجسدية والنفسية عبر خط زمني",
                                appLanguage
                            ),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onExpandFullView != null) {
                    IconButton(
                        onClick = onExpandFullView,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expand Full Screen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick Stats Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Physical Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SideEffectCategory.PHYSICAL.color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SideEffectCategory.PHYSICAL.color.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SideEffectCategory.PHYSICAL.color)
                        )
                        Column {
                            Text(
                                text = t("Physical", "أعراض جسدية", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalPhysicalCount ${t("Events", "تسجيل", appLanguage)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SideEffectCategory.PHYSICAL.color
                            )
                        }
                    }
                }

                // Psychological Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SideEffectCategory.PSYCHOLOGICAL.color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SideEffectCategory.PSYCHOLOGICAL.color.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SideEffectCategory.PSYCHOLOGICAL.color)
                        )
                        Column {
                            Text(
                                text = t("Psychological", "أعراض نفسية", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalPsychCount ${t("Events", "تسجيل", appLanguage)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SideEffectCategory.PSYCHOLOGICAL.color
                            )
                        }
                    }
                }
            }

            // Filter Chips Bar (Both, Physical, Psychological) & Time Grouping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter Categories
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartFilterMode.values().forEach { mode ->
                        val isSelected = filterMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterMode = mode },
                            label = {
                                Text(
                                    text = if (appLanguage == "ar") mode.labelAr else mode.labelEn,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (mode) {
                                    ChartFilterMode.BOTH -> MaterialTheme.colorScheme.primaryContainer
                                    ChartFilterMode.PHYSICAL_ONLY -> SideEffectCategory.PHYSICAL.color.copy(alpha = 0.2f)
                                    ChartFilterMode.PSYCHOLOGICAL_ONLY -> SideEffectCategory.PSYCHOLOGICAL.color.copy(alpha = 0.2f)
                                },
                                selectedLabelColor = when (mode) {
                                    ChartFilterMode.BOTH -> MaterialTheme.colorScheme.onPrimaryContainer
                                    ChartFilterMode.PHYSICAL_ONLY -> SideEffectCategory.PHYSICAL.color
                                    ChartFilterMode.PSYCHOLOGICAL_ONLY -> SideEffectCategory.PSYCHOLOGICAL.color
                                }
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                // Time Unit (Weekly vs Monthly)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        ChartTimeGrouping.values().forEach { grouping ->
                            val isSelected = timeGrouping == grouping
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { timeGrouping = grouping }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (appLanguage == "ar") grouping.labelAr else grouping.labelEn,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // The Canvas Line Chart
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            val pointsCount = timelinePoints.size

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                if (timelinePoints.isEmpty() || (totalPhysicalCount == 0 && totalPsychCount == 0)) {
                    // Empty state fallback
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = t(
                                "No symptoms recorded along your course yet.",
                                "لم يتم تسجيل أعراض جانبية خلال فترات العلاج حتى الآن.",
                                appLanguage
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = t(
                                "Log your symptoms in the 'Daily' tab to see trends automatically plotted here.",
                                "سجل أعراضك اليومية من تبويب 'اليوميات' لتظهر منحنيات التتبع تلقائياً.",
                                appLanguage
                            ),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(timelinePoints) {
                                detectTapGestures { offset ->
                                    val leftPad = 36.dp.toPx()
                                    val rightPad = 20.dp.toPx()
                                    val chartW = size.width - leftPad - rightPad
                                    if (pointsCount > 1) {
                                        val step = chartW / (pointsCount - 1)
                                        val relativeX = offset.x - leftPad
                                        val tappedIndex = ((relativeX + step / 2f) / step)
                                            .toInt()
                                            .coerceIn(0, pointsCount - 1)
                                        selectedIndex = tappedIndex
                                    } else if (pointsCount == 1) {
                                        selectedIndex = 0
                                    }
                                }
                            }
                    ) {
                        val leftPadding = 36.dp.toPx()
                        val rightPadding = 20.dp.toPx()
                        val topPadding = 20.dp.toPx()
                        val bottomPadding = 34.dp.toPx()

                        val chartWidth = size.width - leftPadding - rightPadding
                        val chartHeight = size.height - topPadding - bottomPadding

                        val paint = android.graphics.Paint().apply {
                            color = textColor
                            textSize = 9.sp.toPx()
                            typeface = android.graphics.Typeface.DEFAULT
                        }

                        // Determine Y axis levels based on max count
                        val yMax = ((maxSingleCount + 1) / 2 * 2).coerceAtLeast(4)
                        val ySteps = 4
                        for (i in 0..ySteps) {
                            val levelVal = (yMax * (i.toFloat() / ySteps)).toInt()
                            val yPos = topPadding + chartHeight * (1f - (i.toFloat() / ySteps))

                            // Grid line
                            drawLine(
                                color = gridLineColor,
                                start = Offset(leftPadding, yPos),
                                end = Offset(size.width - rightPadding, yPos),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            // Y axis label
                            drawContext.canvas.nativeCanvas.drawText(
                                "$levelVal",
                                leftPadding - 20.dp.toPx(),
                                yPos + 3.dp.toPx(),
                                paint
                            )
                        }

                        val xStep = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth / 2f

                        // X axis ticks and labels
                        val stride = when {
                            pointsCount <= 6 -> 1
                            pointsCount <= 12 -> 2
                            pointsCount <= 20 -> 3
                            else -> 4
                        }

                        timelinePoints.forEachIndexed { idx, point ->
                            if (idx % stride == 0 || idx == pointsCount - 1) {
                                val xPos = if (pointsCount == 1) leftPadding + chartWidth / 2f else leftPadding + idx * xStep
                                drawContext.canvas.nativeCanvas.drawText(
                                    point.label,
                                    xPos - 12.dp.toPx(),
                                    topPadding + chartHeight + 20.dp.toPx(),
                                    paint
                                )
                            }
                        }

                        // Selected Index Hairline Guide
                        if (selectedIndex in 0 until pointsCount) {
                            val selX = if (pointsCount == 1) leftPadding + chartWidth / 2f else leftPadding + selectedIndex * xStep
                            drawLine(
                                color = Color(0xFF64B5F6),
                                start = Offset(selX, topPadding),
                                end = Offset(selX, topPadding + chartHeight),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // Helper function to plot a trend line with area gradient
                        fun plotCurve(
                            countExtractor: (CourseTimelinePoint) -> Int,
                            lineColor: Color
                        ) {
                            val points = mutableListOf<Offset>()
                            timelinePoints.forEachIndexed { idx, point ->
                                val count = countExtractor(point)
                                val x = if (pointsCount == 1) leftPadding + chartWidth / 2f else leftPadding + idx * xStep
                                val y = topPadding + chartHeight * (1f - (count.toFloat() / yMax).coerceIn(0f, 1f))
                                points.add(Offset(x, y))
                            }

                            if (points.isEmpty()) return

                            // Shaded Gradient Area under Curve
                            val areaPath = Path().apply {
                                moveTo(points.first().x, topPadding + chartHeight)
                                points.forEach { pt -> lineTo(pt.x, pt.y) }
                                lineTo(points.last().x, topPadding + chartHeight)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lineColor.copy(alpha = 0.22f),
                                        lineColor.copy(alpha = 0.02f)
                                    ),
                                    startY = topPadding,
                                    endY = topPadding + chartHeight
                                )
                            )

                            // Stroke Path
                            val linePath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    // Smooth bezier curve between points
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }

                            drawPath(
                                path = linePath,
                                color = lineColor,
                                style = Stroke(
                                    width = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Data Nodes
                            points.forEachIndexed { idx, pt ->
                                val isNodeSelected = idx == selectedIndex
                                val dotRadius = if (isNodeSelected) 6.dp.toPx() else 4.dp.toPx()

                                if (isNodeSelected) {
                                    drawCircle(
                                        color = lineColor.copy(alpha = 0.35f),
                                        radius = dotRadius + 4.dp.toPx(),
                                        center = pt
                                    )
                                }

                                drawCircle(
                                    color = Color.White,
                                    radius = dotRadius,
                                    center = pt
                                )
                                drawCircle(
                                    color = lineColor,
                                    radius = dotRadius - 1.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // Plot according to active filter
                        if (filterMode == ChartFilterMode.BOTH || filterMode == ChartFilterMode.PHYSICAL_ONLY) {
                            plotCurve(
                                countExtractor = { it.physicalCount },
                                lineColor = SideEffectCategory.PHYSICAL.color
                            )
                        }

                        if (filterMode == ChartFilterMode.BOTH || filterMode == ChartFilterMode.PSYCHOLOGICAL_ONLY) {
                            plotCurve(
                                countExtractor = { it.psychologicalCount },
                                lineColor = SideEffectCategory.PSYCHOLOGICAL.color
                            )
                        }
                    }
                }
            }

            // Interactive Point Details Inspector Card
            AnimatedVisibility(
                visible = selectedPoint != null && (totalPhysicalCount > 0 || totalPsychCount > 0),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedPoint?.let { pt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${pt.label} (${pt.fullDateRange})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${pt.logCount} ${t("daily entries", "أيام مسجلة", appLanguage)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Physical Side Effects Breakdown in Selected Period
                            if (pt.physicalBreakdown.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(SideEffectCategory.PHYSICAL.color)
                                        )
                                        Text(
                                            text = "${t("Physical Side Effects", "الأعراض الجسدية", appLanguage)} (${pt.physicalCount}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SideEffectCategory.PHYSICAL.color
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        pt.physicalBreakdown.forEach { (sym, count) ->
                                            val locName = getLocalizedSymptomName(sym, appLanguage)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SideEffectCategory.PHYSICAL.color.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "$locName × $count",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SideEffectCategory.PHYSICAL.color,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = t("No physical symptoms logged during this period.", "لا توجد أعراض جسدية مسجلة في هذه الفترة.", appLanguage),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Psychological Side Effects Breakdown in Selected Period
                            if (pt.psychologicalBreakdown.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(SideEffectCategory.PSYCHOLOGICAL.color)
                                        )
                                        Text(
                                            text = "${t("Psychological & Mood", "الأعراض النفسية والمزاجية", appLanguage)} (${pt.psychologicalCount}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SideEffectCategory.PSYCHOLOGICAL.color
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        pt.psychologicalBreakdown.forEach { (sym, count) ->
                                            val locName = getLocalizedSymptomName(sym, appLanguage)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SideEffectCategory.PSYCHOLOGICAL.color.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "$locName × $count",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SideEffectCategory.PSYCHOLOGICAL.color,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = t("No psychological or mood symptoms reported during this period.", "لا توجد أعراض نفسية أو تقلبات مزاجية في هذه الفترة.", appLanguage),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Clinical Advisory Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = t(
                                "Clinical Note on Roaccutane Trends:",
                                "إرشاد سريري حول مسارات الرواكتان:",
                                appLanguage
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = t(
                                "Mucocutaneous physical dryness is normal and expected. However, psychological mood changes or severe brain fog should be reported to your treating physician.",
                                "الأعراض الجسدية كالجفاف متوقعة وتتطلب استمرار الترطيب، بينما تتطلب التغيرات المزاجية الحادة أو ضبابية الذهن مناقشتها مع طبيبك المعالج.",
                                appLanguage
                            ),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
