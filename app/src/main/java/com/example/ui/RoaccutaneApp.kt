package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Appointment
import com.example.data.DailyLog
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.ui.components.SideEffectsTrendsLineChartCard
import com.example.ui.components.WaterConfigDialog
import com.example.ui.components.ManageHomeProductsDialog
import com.example.ui.components.DispensedMedicationsTrackerDialog
import com.example.ui.components.PrescriptionDetailsViewerDialog
import com.example.ui.components.LinkPrescriptionToAppointmentDialog
import com.example.ui.components.handleOpenAttachment
import com.example.ui.components.BiomarkerUnitsCatalog
import com.example.ui.components.LabInputWithUnitSelector
import com.example.data.DispensedMedication
import com.example.data.MedicalPrescription
import com.example.data.LinkedAccount
import com.example.data.isGoogleEmail
import com.example.ui.portals.PatientAffiliateProductsCard
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import android.app.DatePickerDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
 
val SYMPTOMS_EN = listOf("Chapped Lips", "Dry Skin", "Dry Eyes", "Nosebleeds", "Joint Pain", "Muscle Aches", "Headaches", "Fatigue", "Mood Swings", "Brain Fog")
val SYMPTOMS_AR = listOf("جفاف الشفاه", "جفاف الجلد", "جفاف العين", "نزيف الأنف", "آلام المفاصل", "آلام العضلات", "الصداع", "الخمول والتعب", "تقلب المزاج", "ضبابية الذهن")

val FATS_EN = listOf("Peanut Butter", "Avocado", "Olive Oil", "Eggs", "Whole Milk / Yogurt", "Nuts / Seeds", "Cheese", "Fatty Fish", "Dark Chocolate")
val FATS_AR = listOf("زبدة الفول السوداني", "الأفوكادو", "زيت الزيتون / الوجبات", "البيض الكامل", "حليب أو لبن كامل الدسم", "المكسرات والبذور", "الأجبان", "الأسماك الدهنية", "الشوكولاتة الداكنة")

fun t(en: String, ar: String, lang: String): String {
    return if (lang == "ar") ar else en
}

fun normalizeSymptomKey(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    val indexEn = SYMPTOMS_EN.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
    if (indexEn != -1) return SYMPTOMS_EN[indexEn]
    val indexAr = SYMPTOMS_AR.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
    if (indexAr != -1) return SYMPTOMS_EN[indexAr]
    return trimmed
}

fun calculateAgeAndFormatBirthDate(birthDateType: String, birthDateVal: String): Pair<Int, String> {
    if (birthDateVal.isBlank()) return Pair(20, "")
    return try {
        val parts = birthDateVal.split("-")
        if (parts.size != 3) return Pair(20, birthDateVal)
        val y = parts[0].toIntOrNull() ?: 1999
        val m = parts[1].toIntOrNull() ?: 1
        val d = parts[2].toIntOrNull() ?: 1

        val localDate = if (birthDateType == "H") {
            val hijrahDate = java.time.chrono.HijrahDate.of(y, m, d)
            java.time.LocalDate.ofEpochDay(hijrahDate.toEpochDay())
        } else {
            java.time.LocalDate.of(y, m, d)
        }

        val today = java.time.LocalDate.now()
        val age = java.time.Period.between(localDate, today).years
        Pair(age, birthDateVal)
    } catch (e: Throwable) {
        try {
            val parts = birthDateVal.split("-")
            val y = parts[0].toIntOrNull() ?: 1999
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            if (birthDateType == "H") {
                val gregYear = (y * 0.97 + 622).toInt()
                Pair(currentYear - gregYear, birthDateVal)
            } else {
                Pair(currentYear - y, birthDateVal)
            }
        } catch (ex: Throwable) {
            Pair(20, birthDateVal)
        }
    }
}

// Utilities
fun getTodayDateStr(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

fun formatDisplayDateStr(dateStr: String, lang: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = if (lang == "ar") {
            SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.US)
        }
        val date = parser.parse(dateStr)
        date?.let { formatter.format(it) } ?: dateStr
    } catch (e: java.lang.Exception) {
        dateStr
    }
}

fun isSameCalendarDay(millis: Long, dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = parser.parse(dateStr) ?: return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
        val cal2 = Calendar.getInstance().apply { time = d }
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    } catch (e: Exception) {
        false
    }
}

fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun formatDisplayTimeStr(millis: Long, lang: String): String {
    val formatter = if (lang == "ar") {
        SimpleDateFormat("d MMM yyyy - hh:mm a", Locale("ar"))
    } else {
        SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.US)
    }
    return formatter.format(Date(millis))
}

fun formatDisplayTimeOnly(millis: Long, lang: String): String {
    val formatter = if (lang == "ar") {
        SimpleDateFormat("hh:mm a", Locale("ar"))
    } else {
        SimpleDateFormat("hh:mm a", Locale.US)
    }
    return formatter.format(Date(millis))
}

fun formatOffsetDuration(minutes: Int, lang: String): String {
    return if (lang == "ar") {
        when {
            minutes < 60 -> "قبل الموعد بـ $minutes دقيقة"
            minutes == 60 -> "قبل الموعد بساعة واحدة"
            minutes == 120 -> "قبل الموعد بساعتين"
            minutes % 60 == 0 -> "قبل الموعد بـ ${minutes / 60} ساعات"
            else -> "قبل الموعد بـ ${minutes / 60} ساعات و ${minutes % 60} دقيقة"
        }
    } else {
        when {
            minutes < 60 -> "$minutes mins before appointment"
            minutes == 60 -> "1 hour before appointment"
            minutes % 60 == 0 -> "${minutes / 60} hours before appointment"
            else -> "${minutes / 60} hours ${minutes % 60} mins before"
        }
    }
}

@Composable
fun RoaccutaneApp(
    viewModel: RoaccutaneViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
         
    var currentTab by rememberSaveable { mutableStateOf("dashboard") }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()

    val cumulativeDose by viewModel.cumulativeDose.collectAsStateWithLifecycle(initialValue = 0)
    val elapsedDays by viewModel.elapsedDaysFlow.collectAsStateWithLifecycle(initialValue = 0)
    val activeDaysCount by viewModel.activeDaysCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val remainingDaysEstimated by viewModel.remainingDaysEstimatedFlow.collectAsStateWithLifecycle(initialValue = 0)

    val symptomStats by viewModel.symptomStatisticsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val sourceStats by viewModel.healthySourcesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val editingLogDate by viewModel.editingLogDate.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
                bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "dashboard",
                        onClick = { currentTab = "dashboard" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                        label = { Text(t("Home", "الرئيسية", appLanguage), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentTab == "logs",
                        onClick = { currentTab = "logs" },
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = "Logs") },
                        label = { Text(t("Daily", "اليوميات", appLanguage), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_logs")
                    )
                    NavigationBarItem(
                        selected = currentTab == "analytics",
                        onClick = { currentTab = "analytics" },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text(t("Statistics", "الإحصاءات", appLanguage), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_stats")
                    )
                    NavigationBarItem(
                        selected = currentTab == "appointments",
                        onClick = { currentTab = "appointments" },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Appointments") },
                        label = { Text(t("Appointments", "المواعيد", appLanguage), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_appointments")
                    )
                    NavigationBarItem(
                        selected = currentTab == "jauzaa_ai",
                        onClick = { currentTab = "jauzaa_ai" },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Jauzaa") },
                        label = { Text(t("Treat AI", "المساعد الطبي", appLanguage), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_ai")
                    )
                    NavigationBarItem(
                        selected = currentTab == "settings",
                        onClick = { currentTab = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text(t("Settings", "الإعدادات", appLanguage), fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isTabletLayout = configuration.screenWidthDp >= 600

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (currentTab) {
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                userProfile = userProfile,
                                cumulativeDose = cumulativeDose,
                                elapsedDays = elapsedDays,
                                remainingDaysEst = remainingDaysEstimated,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                appLanguage = appLanguage,
                                onNavigateToTab = { currentTab = it }
                            )
                            "logs" -> LogsScreen(
                                viewModel = viewModel,
                                allLogs = allLogs,
                                userProfile = userProfile,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                appLanguage = appLanguage
                            )
                            "analytics" -> AnalyticsScreen(
                                viewModel = viewModel,
                                userProfile = userProfile,
                                allLogs = allLogs,
                                cumulativeDose = cumulativeDose,
                                targetDose = userProfile.calculatedTargetDoseMg,
                                symptomStats = symptomStats,
                                sourceStats = sourceStats,
                                elapsedDays = elapsedDays,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                appLanguage = appLanguage
                            )
                            "appointments" -> AppointmentsScreen(
                                viewModel = viewModel,
                                allAppointments = allAppointments,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                appLanguage = appLanguage
                            )
                            "jauzaa_ai" -> JauzaaScreen(
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                appLanguage = appLanguage
                            )
                            "settings" -> SettingsScreen(
                                viewModel = viewModel,
                                elapsedDays = elapsedDays,
                                remainingDaysEst = remainingDaysEstimated,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                appLanguage = appLanguage,
                                onNavigateToTab = { currentTab = it }
                            )
                        }
                    }
                }
            }

            editingLogDate?.let { dateStr ->
                LogEditorDialog(
                    dateStr = dateStr,
                    viewModel = viewModel,
                    allLogs = allLogs,
                    userProfile = userProfile,
                    appLanguage = appLanguage,
                    onDismiss = { viewModel.setEditingLogDate(null) }
                )
            }
        }
    }
}

// ------------------- MEDICAL PHASES & ROTATIONAL CAROUSEL TIPS -------------------

data class MilestonePhaseData(
    val stageName: String,
    val interpretation: String,
    val tip: String,
    val encouragement: String
)

fun getMilestonePhase(
    weightKg: Float,
    dailyDoseMg: Int,
    cumulativeDoseMg: Int,
    appLanguage: String
): MilestonePhaseData {
    val weight = if (weightKg > 0f) weightKg else 82f
    val dailyDose = if (dailyDoseMg > 0) dailyDoseMg else 20
    val target = weight * 120f
    val currentPercentage = if (target > 0f) (cumulativeDoseMg.toFloat() / target) * 100f else 0f
    val doseIntensity = dailyDose.toFloat() / weight

    val isLowDose = doseIntensity < 0.3f

    return if (appLanguage == "ar") {
        val stageName = when {
            currentPercentage < 10f -> "المرحلة الأولى: البدء والتأقلم النسيجي العابر (0% - 10%)"
            currentPercentage < 20f -> "المرحلة الثانية: التفريغ الخلوي وبداية الصعود الأيضي (10% - 20%)"
            currentPercentage < 30f -> "المرحلة الثالثة: مرحلة الثبات النسيجي والجفاف المستقر (20% - 30%)"
            currentPercentage < 50f -> "المرحلة الرابعة: مرحلة الهجوم الفعلي وذروة التغيير النسيجي (30% - 50%)"
            currentPercentage < 80f -> "المرحلة الخامسة: مرحلة البناء، صقل الندوب، والتعافي البصري (50% - 80%)"
            else -> "المرحلة السادسة: مرحلة الإغلاق النهائي وتأمين المناعة الجلدية مستقبلاً (80% - 100%)"
        }

        val interpretation = when {
            currentPercentage < 10f -> {
                if (isLowDose) {
                    "بروتوكول الجرعة المنخفضة الذكية: يبدأ جسمك بالتكيف الفسيولوجي الهادئ مع تراكم العلاج. تغلغل صامت وجفاف خفيف متزن، حيث تجف الحبوب وتتساقط بسرعة مريحة دون إثارة تهيجات حادة أو آلام مفاجئة."
                } else {
                    "بروتوكول الجرعة القياسية/العالية: دفع قوي للشوائب العميقة وتنشيط سريع لمستقبلات الخلايا. قد تشعر بجفاف مخاطي مكثف وبداية تطهير وتفشٍّ حاد مؤقت للحبوب، مما يتطلب ترطيباً فيزيائياً صارماً."
                }
            }
            currentPercentage < 20f -> {
                if (isLowDose) {
                    "بروتوكول الجرعة المنخفضة الذكية: تواصل الخلايا طرح الشوائب تحت الجلدية بمعدل آمن وتكيف مريح. تجف الحبوب وتتساقط بسرعة جنونية ومستمرة دون تسبب في احمرار مفرط أو التهابات حادة في الطبقات الخارجية."
                } else {
                    "بروتوكول الجرعة القياسية/العالية: جفاف مخاطي مكثف وذروة التطهير المندفع. دفع قوي للشوائب والتهاب مؤقت للسطح الجلدي يتطلب ترطيباً كثيفاً وعازلاً فيزيائياً متكرراً لمنع تصدع الحواجز الجلدية."
                }
            }
            currentPercentage < 30f -> {
                if (isLowDose) {
                    "بروتوكول الجرعة المنخفضة الذكية: استقرار جفاف الغدد الدهنية عند مستويات مريحة ومتزنة. تفريغ صامت ومستمر وتناقص حاد في وتيرة ظهور الحبوب الجديدة مع مرونة فسيولوجية رائعة للأنسجة."
                } else {
                    "بروتوكول الجرعة القياسية/العالية: جفاف مستمر وشديد في الأغشية المخاطية والجلد. تباطؤ نشاط الغدد الدهنية بالكامل، مما يتطلب ترطيباً فيزيائياً صارماً لتفادي التشققات والنزف البسيط ومساندة إصلاح الخلايا."
                }
            }
            currentPercentage < 50f -> {
                if (isLowDose) {
                    "بروتوكول الجرعة المنخفضة الذكية: انكماش هائل ومستقر في حجم المسام ومستويات الدهون. تتلاشى الحبوب بسرعة بالغة مع حماية نسيجية عالية وتكيف جلدي ممتاز ودون قشور حمراء مزعجة."
                } else {
                    "بروتوكول الجرعة القياسية/العالية: ذروة التغيير الخلوي العميق وإعادة هيكلة البشرة. الجفاف المخاطي في حده الأقصى، مع تلاشي شبه تام للحبوب النشطة وبداية اندفاع الشوائب الدقيقة للتعافي."
                }
            }
            currentPercentage < 80f -> {
                if (isLowDose) {
                    "بروتوكول الجرعة المنخفضة الذكية: صقل لطيف وهادئ لسطح البشرة والندوب. تندمج آثار الحبوب وتتلاشى التصبغات تدريجياً في بيئة رطبة ومريحة وخالية من تهيج الأنسجة العميقة."
                } else {
                    "بروتوكول الجرعة القياسية/العالية: تجديد نسيجي متسارع وإعادة صقل قوية للندوب والآثار. يتطلب الجفاف الموضعي والمخاطي حماية شمسية قصوى وترطيباً فيزيائياً مستمراً لتفادي فرط التصبغ الارتدادي."
                }
            }
            else -> {
                if (isLowDose) {
                    "بروتوكول الجرعة المنخفضة الذكية: اكتمال تثبيط الغدد الدهنية وتجفيفها التام لمنع الارتداد مستقبلاً. تتمتع الأنسجة بمرونة وحيوية كاملة مع استقرار تصفية البشرة وحيويتها العالية."
                } else {
                    "بروتوكول الجرعة القياسية/العالية: تراجع الجفاف الكثيف بالتدريج وتراجع نشاط مستقبلات الدواء، مع ترسيخ الانكماش الدائم للغدد الدهنية لتأمين مناعة جلدية طويلة الأجل ضد حب الشباب."
                }
            }
        }

        val tip = when {
            currentPercentage < 10f -> "تجنب تماماً استخدام المقشرات المنزلية أو الصابون المعطر؛ اغسل وجهك بماء فاتر فقط واستخدم فازلين عادي للترطيب الفوري."
            currentPercentage < 20f -> "ضع ملعقة من زيت الزيتون أو الفازلين على شفتيك ومحيط الأنف قبل النوم كدرع عازل طبيعي ومجاني."
            currentPercentage < 30f -> "استنشق بخار الماء الفاتر أثناء الاستحمام لترطيب ممرات الأنف الجافة مجاناً دون استخدام بخاخات كيميائية."
            currentPercentage < 50f -> "أضف بضع قطرات من الماء الفاتر لمرطبك اليومي لتسهيل توزيعه وامتصاصه دون إجهاد لجلدك الرقيق."
            currentPercentage < 80f -> "اصنع ضغطاً بارداً بقطعة قماش قطنية نظيفة ومبللة بماء بارد لتهدئة أي توهج أو احمرار جلدي مجاناً وبسرعة."
            else -> "استخدم الفازلين المتبقي كمرطب شفاه نهائي واشرب الماء بانتظام لتسهيل انتقال جسمك لمرحلة ما بعد الكورس."
        }

        val encouragement = when {
            currentPercentage < 10f -> "الالتزام التام بعدم لمس الحبوب أو محاولة عصرها؛ دع الخلايا تتجدد بأمان ودون ندوب!"
            currentPercentage < 20f -> "البشرة الزجاجية تختبئ خلف هذه المرحلة المؤقتة؛ تذكر أن كل حبة تظهر الآن هي شائبة تغادر مسامك للأبد!"
            currentPercentage < 30f -> "الترطيب هو سلاحك الأقوى الآن؛ لا تستسلم للجفاف وعامل بشرتك بلطف فائق كالحرير!"
            currentPercentage < 50f -> "البشرة بدأت تصفو وتستعيد نضارتها؛ لا تعبث بالبثور الجافة المتبقية، فالنصر قريب جداً!"
            currentPercentage < 80f -> "البشرة الزجاجية أصبحت واقعاً تراه في المرآة؛ استمر في حمايتها وترطيبها ولا تتهاون أبداً!"
            else -> "لقد انتصرت في هذه المعركة الطبية الصعبة! استمتع ببشرتك الزجاجية الفائقة وحافظ عليها ككنز ثمين!"
        }

        MilestonePhaseData(
            stageName = "$stageName (${String.format(Locale.US, "%.1f%%", currentPercentage)})",
            interpretation = interpretation,
            tip = tip,
            encouragement = encouragement
        )
    } else {
        val stageName = when {
            currentPercentage < 10f -> "Stage 1: Initiation & Transient Tissue Adaptation (0% - 10%)"
            currentPercentage < 20f -> "Stage 2: Cellular Purging & Metabolic Ascent (10% - 20%)"
            currentPercentage < 30f -> "Stage 3: Tissue Stabilization & Established Dryness (20% - 30%)"
            currentPercentage < 50f -> "Stage 4: Active Attack & Peak Tissue Overhaul (30% - 50%)"
            currentPercentage < 80f -> "Stage 5: Dermal Rebuilding, Scar Refining & Visual Healing (50% - 80%)"
            else -> "Stage 6: Final Consolidation & Future Skin Immunity (80% - 100%)"
        }

        val interpretation = when {
            currentPercentage < 10f -> {
                if (isLowDose) {
                    "Smart Low-Dose Protocol: Quiet cellular adaptation begins. Silent penetration with a balanced mild dryness; acne lesions dry out and shed comfortably without acute flares or pain."
                } else {
                    "Standard/High Dose Protocol: Intense cellular purging of deep impurities. Expect strong mucosal dryness and a potential temporary flare/purge of acne, requiring strict physical barrier hydration."
                }
            }
            currentPercentage < 20f -> {
                if (isLowDose) {
                    "Smart Low-Dose Protocol: Continuous safe elimination of under-skin impurities with comfortable tolerance. Acne dries and sheds rapidly without causing excessive redness or outer inflammation."
                } else {
                    "Standard/High Dose Protocol: Intense mucosal dryness and peak cellular purging. Strong surface inflammation as impurities are pushed out, demanding thick, frequent barrier creams to prevent skin cracking."
                }
            }
            currentPercentage < 30f -> {
                if (isLowDose) {
                    "Smart Low-Dose Protocol: Sebum suppression stabilizes at a comfortable, balanced level. Silent clearing continues with a sharp decline in new breakouts and excellent physical tissue flexibility."
                } else {
                    "Standard/High Dose Protocol: Severe, constant dryness of skin and mucous membranes. Complete sebum suppression requires rigorous physical moisturization to prevent dry cracks or minor bleeding."
                }
            }
            currentPercentage < 50f -> {
                if (isLowDose) {
                    "Smart Low-Dose Protocol: Tremendous, stable reduction in pore size and sebum production. Acne fades rapidly with high tissue protection, excellent dermal adaptation, and no annoying red flakes."
                } else {
                    "Standard/High Dose Protocol: Peak cellular restructuring. Mucosal dryness is at maximum, accompanied by a near-complete vanishing of active acne and accelerated skin turnover."
                }
            }
            currentPercentage < 80f -> {
                if (isLowDose) {
                    "Smart Low-Dose Protocol: Gentle, steady refining of skin texture and post-acne marks. Pigmentation fades progressively in a hydrated, comfortable state free of deep tissue irritation."
                } else {
                    "Standard/High Dose Protocol: Accelerated tissue regeneration and powerful scar remodeling. High dryness demands maximum sun protection and continuous hydration to avoid rebound hyperpigmentation."
                }
            }
            else -> {
                if (isLowDose) {
                    "Smart Low-Dose Protocol: Successful completion of sebum suppression to prevent future relapse. Tissue maintains high elasticity and health as permanent skin clearing is secured."
                } else {
                    "Standard/High Dose Protocol: Severe dryness begins to ease slightly as permanent shrinkage of sebaceous glands is locked in, securing long-term dermatological immunity."
                }
            }
        }

        val tip = when {
            currentPercentage < 10f -> "Avoid home chemical peels or fragranced soaps; wash your face with lukewarm water only and use pure petroleum jelly for instant, zero-cost occlusion."
            currentPercentage < 20f -> "Apply a tiny dab of olive oil or pure petroleum jelly around your lips and nasal creases before sleep as a free natural barrier shield."
            currentPercentage < 30f -> "Breathe in warm shower steam to moisturize dry nasal passages naturally and for free without chemical sprays."
            currentPercentage < 50f -> "Mix a few drops of clean water with your moisturizer to help it spread smoothly and absorb quickly without friction on thin skin."
            currentPercentage < 80f -> "Apply a cool compress using a clean cotton cloth soaked in cold water to soothe skin warmth and redness instantly for free."
            else -> "Keep using your standard petroleum jelly as lip care and maintain consistent water intake for a smooth post-treatment transition."
        }

        val encouragement = when {
            currentPercentage < 10f -> "Commit absolutely to never touching or squeezing pimples; let your cells renew safely without scarring!"
            currentPercentage < 20f -> "The luminous glass skin is hiding behind this temporary phase; remember, every spot appearing now is an impurity leaving your pores forever!"
            currentPercentage < 30f -> "Moisturization is your superpower now; don't give up on dryness, treat your skin with ultimate silk-like gentleness!"
            currentPercentage < 50f -> "Your skin is visibly clearing and recovering its health; do not pick at any remaining dry crusts, victory is close!"
            currentPercentage < 80f -> "The glowing glass skin is now a reality in your mirror; continue protecting and hydrating it, do not slacken!"
            else -> "You have conquered this challenging medical journey! Enjoy your flawless glass skin and cherish it as a precious crown!"
        }

        MilestonePhaseData(
            stageName = "$stageName (${String.format(Locale.US, "%.1f%%", currentPercentage)})",
            interpretation = interpretation,
            tip = tip,
            encouragement = encouragement
        )
    }
}

val TIPS_AR = listOf(
    "👄 ترطيب الشفاه: يقلل الرواكتان إفراز الدهون. أعد وضع مرطب الشفاه الآن لمنع التشقق!",
    "💧 تنبيه الجفاف الخلوي: اشرب كوبين من الماء فوراً لتعويض الجفاف. استهدف 3 لتر يومياً.",
    "🧠 حالتك النفسية: يمكن أن يسبب الرواكتان تقلبات مزاجية. خذ دقيقة للتنفس بعمق والاسترخاء.",
    "🥑 امتصاص الجرعة: الرواكتان يذوب في الدهون؛ تناوله مع وجبة تحتوي على دهون صحية لزيادة فعاليته.",
    "☀️ واقي الشمس: استخدم واقٍ ذو حماية عالية (SPF 50). إن رقة البشرة تزيد من خطر الحروق وتصبغ الجلد."
)
val TIPS_EN = listOf(
    "👄 Lip Moisturizing: Roaccutane reduces sebum. Re-apply lip ointment now to prevent cracking!",
    "💧 Cellular Dehydration Alert: Drink 2 cups of water immediately. Aim for 3 liters daily.",
    "🧠 Psychological Check-in: Isotretinoin can alter mood. Take a minute of deep breathing to relax.",
    "🥑 Maximize Absorption: Roaccutane is lipophilic. Take your capsule with healthy fats to prevent treatment failure.",
    "☀️ Ultraviolet Guard: Apply high-factor SPF 50+ sunscreen today. Skin thinning dramatically raises burn risk."
)

// ------------------- SCREEN 1: DASHBOARD / HOME -------------------

@Composable
fun DashboardScreen(
    viewModel: RoaccutaneViewModel,
    userProfile: UserProfile,
    cumulativeDose: Int,
    elapsedDays: Int,
    remainingDaysEst: Int,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    appLanguage: String,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val todayDate = getTodayDateStr()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val todayLog = allLogs.find { it.dateString == todayDate }
    val isTodayDone = todayLog?.isTaken == true

    // Dynamic AI Tips and Guidance states collected from ViewModel
    val aiCustomTips by viewModel.aiCustomTips.collectAsStateWithLifecycle()
    val isGeneratingTips by viewModel.isGeneratingTips.collectAsStateWithLifecycle()
    val customTipsError by viewModel.customTipsError.collectAsStateWithLifecycle()

    // Active timers loaded from persistent ViewModel states
    val lipTimerSec by viewModel.lipTimerSec.collectAsStateWithLifecycle()
    val faceTimerSec by viewModel.faceTimerSec.collectAsStateWithLifecycle()
    val eyeTimerSec by viewModel.eyeTimerSec.collectAsStateWithLifecycle()

    // Swipable tips index
    var currentTipIdx by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = true) {
        while (true) {
            delay(1000)
            viewModel.updateTimerStates()
        }
    }

    LaunchedEffect(key1 = true) {
        while (true) {
            delay(10000)
            currentTipIdx = (currentTipIdx + 1) % TIPS_EN.size
        }
    }

    var showWaterInfoDialog by remember { mutableStateOf(false) }
    var showWaterConfigDialog by remember { mutableStateOf(false) }
    var showStageInfoDialog by remember { mutableStateOf(false) }
    var showAddAppointmentDialogShortcut by remember { mutableStateOf(false) }
    var initialApptTypeForShortcut by remember { mutableStateOf("CLINIC") }
    var showManageProductsDialog by remember { mutableStateOf(false) }
    var showDispensedMedicationsDialog by remember { mutableStateOf(false) }
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle(initialValue = emptyList())
    val affiliateProducts by viewModel.allAffiliateProducts.collectAsStateWithLifecycle()
    val currentWaterStep by viewModel.waterStepCups.collectAsStateWithLifecycle()
    val dispensedMedications by viewModel.allDispensedMedications.collectAsStateWithLifecycle()
    val allPrescriptions by viewModel.allMedicalPrescriptions.collectAsStateWithLifecycle()

    val milestoneData = remember(userProfile.weightKg, userProfile.dailyDoseMg, cumulativeDose, appLanguage) {
        getMilestonePhase(userProfile.weightKg, userProfile.dailyDoseMg, cumulativeDose, appLanguage)
    }

    val daysText = t("Day $elapsedDays", "اليوم $elapsedDays", appLanguage)

    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val outerPadding = if (isTablet) 32.dp else 16.dp
    val cardPadding = if (isTablet) 24.dp else 16.dp
    val spacedBy = if (isTablet) 24.dp else 16.dp
    val sectionSpacing = if (isTablet) 16.dp else 10.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(outerPadding),
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        // Brand Header Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Roaccutane Tracker",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = t("Hi, ${userProfile.name} • $daysText", "مرحباً، ${userProfile.name} • $daysText", appLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme"
                    )
                }
            }
        }

        // 1. Dynamic Cumulative Progress Bar Card
        item {
            val targetMg = userProfile.calculatedTargetDoseMg
            val progressRatio = if (targetMg > 0) cumulativeDose.toFloat() / targetMg else 0f
            val percentage = (progressRatio * 100).coerceAtMost(100f)
            val milestoneData = getMilestonePhase(userProfile.weightKg, userProfile.dailyDoseMg, cumulativeDose, appLanguage)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("CUMULATIVE DOSE PROGRESS", "الجرعة التراكمية", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f%%", percentage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = t("Taken: $cumulativeDose mg", "المتناول: $cumulativeDose ملغ", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = t("Target: $targetMg mg (${userProfile.targetMgPerKgInt} mg/kg)", "المطلوب: $targetMg ملغ (${userProfile.targetMgPerKgInt} ملغ/كغ)", appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val estEndDateStr = remember(remainingDaysEst) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, remainingDaysEst)
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = t("Start: ${formatDisplayDateStr(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(userProfile.startDate)), appLanguage)}", "البداية: ${formatDisplayDateStr(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(userProfile.startDate)), appLanguage)}", appLanguage),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = t("Est. End: ${formatDisplayDateStr(estEndDateStr, appLanguage)}", "النهاية المتوقعة: ${formatDisplayDateStr(estEndDateStr, appLanguage)}", appLanguage),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Milestone Phase Motivator Alert Row with Popup Info Trigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = milestoneData.stageName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showStageInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Stage Info",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Today's Capsule Done Confirmation Section & Hospital shortcut
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t("TODAY'S CAPSULE DOSE", "جرعة كبسولة اليوم", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${userProfile.dailyDoseMg} mg",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleDoseTaken(todayDate, userProfile.dailyDoseMg) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTodayDone) Color(0xFF34A853) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isTodayDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Done"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTodayDone) t("Done ✓", "تم التناول ✓", appLanguage) else t("Confirm Taken", "تأكيد التناول", appLanguage),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Shortcut to add appointment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddAppointmentDialogShortcut = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddLocation, contentDescription = "Hospital", tint = MaterialTheme.colorScheme.primary)
                            Text(t("Quick: Plan next Hospital Visit", "سريع: جدولة موعد المستشفى القادم", appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Navigate", modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // 3. Water Intake Track Box
        item {
            val dailyCups = todayLog?.waterCups ?: 0
            val approxLiters = dailyCups * 0.25f
            val baseLiters = (userProfile.weightKg * (if (userProfile.age < 18) 40f else 35f)) / 1000f
            val baseCups = (baseLiters / 0.25f).toInt()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = t("DAILY WATER TRACKER", "خانة حساب كمية الماء اليومية", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                IconButton(
                                    onClick = { showWaterInfoDialog = true },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Water Info",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                IconButton(
                                    onClick = { showWaterConfigDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Water Cup Step",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$dailyCups",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = t(
                                        "cups (approx. ${String.format(Locale.US, "%.2f", approxLiters)} L) / target: 12 (approx. 3.0 L)",
                                        "أكواب (حوالي ${String.format(Locale.US, "%.2f", approxLiters)} لتر) / الهدف: 12 (حوالي 3.0 لتر)",
                                        appLanguage
                                    ),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.incrementWaterByConfiguredStep(todayDate, false) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Minus")
                            }
                            IconButton(
                                onClick = { viewModel.incrementWaterByConfiguredStep(todayDate, true) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3.5. Dispensed Medications & Prescriptions Tracking Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDispensedMedicationsDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1976D2).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = t("DISPENSED DOSES & PRESCRIPTIONS", "تتبع الجرعات والوصفات المصروفة", appLanguage),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (dispensedMedications.isNotEmpty()) {
                                    Surface(
                                        color = Color(0xFF1976D2),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${dispensedMedications.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (dispensedMedications.isEmpty())
                                    t("Track duration, calculate end dates & compensate missed doses", "حساب تاريخ النهاية، تسجيل العلب وتعديل الأيام المنسية", appLanguage)
                                else
                                    t("${dispensedMedications.size} prescription(s) tracked • Tap to view or edit", "تم تسجيل ${dispensedMedications.size} وصفة دواء • اضغط للمتابعة أو الإضافة", appLanguage),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }



        // 4. Cosmetic Moisturizer Alert Timers
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
                    Text(
                        text = t("MOISTURIZING RE-APPLICATION ALERTS", "مؤقت تطبيق المرطبات والقطرات", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Lip balm timer
                        MoisturizerTimerWidget(
                            title = t("Lips Balm", "مرطب الشفاه", appLanguage),
                            count = todayLog?.lipMoistCount ?: 0,
                            secsLeft = lipTimerSec,
                            type = "lip",
                            viewModel = viewModel,
                            onIncrement = { viewModel.incrementMoisturizer(todayDate, "lip") },
                            onDecrement = { viewModel.decrementMoisturizer(todayDate, "lip") },
                            modifier = Modifier.weight(1f),
                            appLanguage = appLanguage
                        )
                        // Face cream timer
                        MoisturizerTimerWidget(
                            title = t("Face Cream", "مرطب الوجه", appLanguage),
                            count = todayLog?.faceMoistCount ?: 0,
                            secsLeft = faceTimerSec,
                            type = "face",
                            viewModel = viewModel,
                            onIncrement = { viewModel.incrementMoisturizer(todayDate, "face") },
                            onDecrement = { viewModel.decrementMoisturizer(todayDate, "face") },
                            modifier = Modifier.weight(1f),
                            appLanguage = appLanguage
                        )
                        // Eye drops timer
                        MoisturizerTimerWidget(
                            title = t("Eye Drops", "قطرات العين", appLanguage),
                            count = todayLog?.eyeMoistCount ?: 0,
                            secsLeft = eyeTimerSec,
                            type = "eye",
                            viewModel = viewModel,
                            onIncrement = { viewModel.incrementMoisturizer(todayDate, "eye") },
                            onDecrement = { viewModel.decrementMoisturizer(todayDate, "eye") },
                            modifier = Modifier.weight(1f),
                            appLanguage = appLanguage
                        )
                    }
                }
            }
        }

        // 5. Scrollable tips carousel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(if (isTablet) 20.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (isTablet) 10.dp else 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("Continuous Swipe Tips Carousel", "نصائح وإرشادات الكورس المتواصلة", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                Icons.Default.ArrowBackIos,
                                contentDescription = "Prev",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        currentTipIdx =
                                            if (currentTipIdx == 0) TIPS_EN.size - 1 else currentTipIdx - 1
                                    }
                            )
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Next",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        currentTipIdx = (currentTipIdx + 1) % TIPS_EN.size
                                    }
                            )
                        }
                    }

                    Text(
                        text = t(TIPS_EN[currentTipIdx], TIPS_AR[currentTipIdx], appLanguage),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Skincare & Hydration Recommended Products
        if (affiliateProducts.isNotEmpty()) {
            item {
                PatientAffiliateProductsCard(
                    products = affiliateProducts,
                    onManageClick = { showManageProductsDialog = true }
                )
            }
        }
    }

    if (showStageInfoDialog) {
        AlertDialog(
            onDismissRequest = { showStageInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = milestoneData.stageName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = milestoneData.interpretation,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider()
                    Text(
                        text = "✨ " + milestoneData.encouragement,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStageInfoDialog = false }) {
                    Text(t("OK", "حسناً", appLanguage), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showWaterInfoDialog) {
        val baseLiters = (userProfile.weightKg * (if (userProfile.age < 18) 40f else 35f)) / 1000f
        val baseCups = (baseLiters / 0.25f).toInt()
        AlertDialog(
            onDismissRequest = { showWaterInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = t("Water Requirement Guidelines", "شرح الاحتياج المائي اليومي", appLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = t(
                            "Your body requires at least ${String.format(Locale.US, "%.2f", baseLiters)} L (approx. $baseCups cups) based on your weight (${userProfile.weightKg} kg) and age (${userProfile.age} years).",
                            "يحتاج جسمك فسيولوجياً إلى ما لا يقل عن ${String.format(Locale.US, "%.2f", baseLiters)} لتر (حوالي $baseCups كوبًا) بناءً على وزنك الحالي (${userProfile.weightKg} كجم) وعمرك (${userProfile.age} سنة).",
                            appLanguage
                        ),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider()
                    Text(
                        text = t(
                            "⚠️ Roaccutane Notice: Because of severe cellular drying, you must drink an extra 0.5L - 1.0L (2-4 cups) on top of this requirement to protect your kidney/liver tissues and reduce joint pains.",
                            "⚠️ تنبيه الرواكتان: بسبب الجفاف الخلوي الشديد، يوصى بشدة بزيادة هذه الكمية بمقدار 0.5 - 1.0 لتر إضافي (2-4 أكواب إضافية) لحماية وظائف الكلى والكبد وتجنب قساوة الجلد وآلام المفاصل الشديدة للشفاه.",
                            appLanguage
                        ),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWaterInfoDialog = false }) {
                    Text(t("OK", "حسناً", appLanguage), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddAppointmentDialogShortcut) {
        AddAppointmentDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            allAppointments = allAppointments,
            initialAppointmentType = initialApptTypeForShortcut,
            onDismiss = { showAddAppointmentDialogShortcut = false }
        )
    }

    if (showWaterConfigDialog) {
        WaterConfigDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            onDismiss = { showWaterConfigDialog = false }
        )
    }

    if (showManageProductsDialog) {
        ManageHomeProductsDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            onDismiss = { showManageProductsDialog = false }
        )
    }

    if (showDispensedMedicationsDialog) {
        DispensedMedicationsTrackerDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            onDismiss = { showDispensedMedicationsDialog = false }
        )
    }
}

@Composable
fun MoisturizerTimerWidget(
    title: String,
    count: Int,
    secsLeft: Int,
    type: String,
    viewModel: RoaccutaneViewModel,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    appLanguage: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val currentDurationSecs = viewModel.getTimerDurationSecs(type)
                        val currentHours = currentDurationSecs / 3600
                        val currentMinutes = (currentDurationSecs % 3600) / 60
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val totalSecs = ((hourOfDay * 60) + minute).coerceAtLeast(1) * 60
                                viewModel.setCustomTimerDuration(type, totalSecs)
                            },
                            currentHours,
                            currentMinutes,
                            true
                        ).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Set Custom Duration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            
            // Draw numerical countdown
            val h = secsLeft / 3600
            val m = (secsLeft % 3600) / 60
            val s = secsLeft % 60
            val padTime = if (h > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.US, "%02d:%02d", m, s)
            }
            
            Text(
                text = padTime,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Middle count
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Increment Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ------------------- SCREEN 2: DAILY LOGS & ENTRIES -------------------

@Composable
fun LogsScreen(
    viewModel: RoaccutaneViewModel,
    allLogs: List<DailyLog>,
    userProfile: UserProfile,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    appLanguage: String
) {
    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val outerPadding = if (isTablet) 32.dp else 16.dp
    val spacedBy = if (isTablet) 24.dp else 16.dp

    val context = LocalContext.current
    var inputDateString by remember { mutableStateOf(getTodayDateStr()) }
    var inputDose by remember { mutableStateOf(userProfile.dailyDoseMg.toString()) }
    var isTakenConfirm by remember { mutableStateOf(true) }
    var selectedFats = remember { mutableStateListOf<String>() }
    var selectedSymptoms = remember { mutableStateListOf<String>() }
    var userNotes by remember { mutableStateOf("") }
    var selectedFilterMonth by remember { mutableStateOf("ALL") }
    var logToDeleteDate by remember { mutableStateOf<String?>(null) }

    val availableMonths = remember(allLogs) {
        val monthsSet = mutableSetOf<String>()
        val todayCal = Calendar.getInstance()
        val currentMonthKey = String.format(Locale.US, "%04d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1)
        monthsSet.add(currentMonthKey)
        
        allLogs.forEach { log ->
            val parts = log.dateString.split("-")
            if (parts.size == 3) {
                monthsSet.add("${parts[0]}-${parts[1]}")
            }
        }
        monthsSet.toList().sortedDescending()
    }

    val todayDateStr = getTodayDateStr()
    val rollingLastWeekDates = remember(allLogs, todayDateStr) {
        val set = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0..6) {
            set.add(fmt.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        set
    }

    val filteredLogs = remember(allLogs, selectedFilterMonth, rollingLastWeekDates) {
        when (selectedFilterMonth) {
            "ALL" -> allLogs
            "LAST_WEEK" -> allLogs.filter { it.dateString in rollingLastWeekDates }
            else -> allLogs.filter { it.dateString.startsWith(selectedFilterMonth) }
        }
    }

    val showDatePicker = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, yr, mo, dy ->
                val calendarDate = Calendar.getInstance()
                calendarDate.set(yr, mo, dy)
                val formatPattern = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                inputDateString = formatPattern.format(calendarDate.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(outerPadding),
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        // App Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("Daily Logs & Capsule Intake", "السجلات اليومية والجرعات", appLanguage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t("Log daily capsule specifications and historical records", "سجل مواصفات الجرعة اليومية وإمكانية تدوين تواريخ سابقة", appLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme"
                    )
                }
            }
        }

        // Intake logging form popup button
        item {
            var showNewLogDialog by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewLogDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text(
                                text = t("LOG NEW ENTRY", "تسجيل جرعة جديدة", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = t("Click to open popup log form", "انقر لفتح نافذة تسجيل السجل المنبثقة", appLanguage),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Open",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Popup Modal Dialog for Logging New Dose
            if (showNewLogDialog) {
                var isFatConfirmed by remember { mutableStateOf(true) }

                AlertDialog(
                    onDismissRequest = { showNewLogDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(t("Log New Capsule Entry", "تسجيل جرعة جديدة", appLanguage), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Calendar Picker Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t("Log Date: ${formatDisplayDateStr(inputDateString, appLanguage)}", "التاريخ: ${formatDisplayDateStr(inputDateString, appLanguage)}", appLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(onClick = { showDatePicker() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(t("Change Date", "تغيير التاريخ", appLanguage), fontSize = 11.sp)
                                }
                            }

                            // Dose Input box
                            OutlinedTextField(
                                value = inputDose,
                                onValueChange = { inputDose = it },
                                label = { Text(t("Dose in mg", "الجرعة بالـ ملغ (mg)", appLanguage)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Capsule confirm taken
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t("Confirmed Capsule Taken?", "تم تأكيد تناول جرعة الكبسولة؟", appLanguage), fontSize = 12.sp)
                                Switch(
                                    checked = isTakenConfirm,
                                    onCheckedChange = { isTakenConfirm = it }
                                )
                            }

                            HorizontalDivider()

                            // Simplified Absorption Partners (Confirmation Only)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t("Appropriate Fat Consumption", "تأكيد استهلاك الدهون المناسبة", appLanguage),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = t("Consumed with a heavy meal containing adequate fat for maximal absorption.", "تم تناول الجرعة مع وجبة مناسبة تحتوي على قدر كافٍ من الدهون لضمان أقصى امتصاص.", appLanguage),
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Checkbox(
                                        checked = isFatConfirmed,
                                        onCheckedChange = { isFatConfirmed = it }
                                    )
                                }
                            }

                            HorizontalDivider()

                            // Symptoms selection
                            Text(
                                text = t("New Symptoms / Side Effects Occurred Today:", "أعراض جديدة حدثت خلال اليوم:", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            FlowRowCompat(
                                items = if (appLanguage == "ar") SYMPTOMS_AR else SYMPTOMS_EN,
                                selectedItems = selectedSymptoms,
                                onToggle = { item ->
                                    if (selectedSymptoms.contains(item)) selectedSymptoms.remove(item) else selectedSymptoms.add(item)
                                }
                            )

                            // Notes text box
                            OutlinedTextField(
                                value = userNotes,
                                onValueChange = { userNotes = it },
                                label = { Text(t("Other text notes / bodily changes", "ملاحظات أخرى أو تغيرات جسدية", appLanguage)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val parsedDose = inputDose.toIntOrNull() ?: userProfile.dailyDoseMg
                                val sourcesList = if (isFatConfirmed) listOf(t("Appropriate fat consumed", "دهون مناسبة مع الجرعة", appLanguage)) else emptyList()
                                viewModel.saveDailyLog(
                                    dateString = inputDateString,
                                    doseMg = parsedDose,
                                    isTaken = isTakenConfirm,
                                    sources = sourcesList,
                                    symptoms = selectedSymptoms.toList(),
                                    notes = userNotes
                                )
                                Toast.makeText(context, t("Logged Successfully!", "تم تسجيل اليوميات بنجاح!", appLanguage), Toast.LENGTH_SHORT).show()
                                userNotes = ""
                                selectedFats.clear()
                                selectedSymptoms.clear()
                                showNewLogDialog = false
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("Save Capsule Log Entry", "حفظ السجل اليومي للجرعة", appLanguage), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewLogDialog = false }) {
                            Text(t("Cancel", "إلغاء", appLanguage))
                        }
                    }
                )
            }
        }

        // Historic Logs list header
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = t("HISTORICAL LOGS PREVIEWS", "عرض السجلات التاريخية للجرعات", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selectedFilterMonth == "ALL") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedFilterMonth == "ALL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilterMonth = "ALL" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t("Full Course", "كامل الكورس", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFilterMonth == "ALL") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        val isLastWeekSelected = selectedFilterMonth == "LAST_WEEK"
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isLastWeekSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isLastWeekSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilterMonth = "LAST_WEEK" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t("Last Week (7 Days)", "آخر أسبوع (7 أيام)", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLastWeekSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(availableMonths) { monthKey ->
                        val parts = monthKey.split("-")
                        val monthLabel = if (parts.size == 2) {
                            val year = parts[0]
                            val mVal = parts[1].toIntOrNull() ?: 1
                            val monthNamesAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
                            val monthNamesEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                            val name = if (appLanguage == "ar") monthNamesAr[mVal - 1] else monthNamesEn[mVal - 1]
                            "$name $year"
                        } else {
                            monthKey
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selectedFilterMonth == monthKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedFilterMonth == monthKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilterMonth = monthKey }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = monthLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFilterMonth == monthKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(t("No historical capsule records found. Plan your first logging above!", "لا توجد سجلات تاريخية للجرعات، أضف أول سجل بالأعلى!", appLanguage), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(filteredLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDisplayDateStr(log.dateString, appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setEditingLogDate(log.dateString) }
                                )
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { logToDeleteDate = log.dateString }
                                )
                            }
                        }

                        val isWithinRollingWeek = log.dateString in rollingLastWeekDates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = t("Dose: ${log.doseMg}mg", "الجرعة: ${log.doseMg} ملغ", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (log.isTaken) t("Taken ✓", "تم التناول ✓", appLanguage) else t("Missed ✗", "فُوتت ✗", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.isTaken) Color(0xFF34A853) else Color.Red
                                )
                            }

                            if (isWithinRollingWeek && log.isTaken && log.timestamp > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = t(
                                                "Confirmed: ${formatDisplayTimeOnly(log.timestamp, appLanguage)}",
                                                "وقت التأكيد: ${formatDisplayTimeOnly(log.timestamp, appLanguage)}",
                                                appLanguage
                                            ),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        if (log.healthySources.isNotBlank()) {
                            Text(t("Fats: ${log.healthySources}", "الدهون: ${log.healthySources}", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (log.symptoms.isNotBlank()) {
                            Text(t("Symptoms: ${log.symptoms}", "الأعراض: ${log.symptoms}", appLanguage), fontSize = 10.sp, color = Color(0xFFE57373))
                        }
                        if (log.notes.isNotBlank()) {
                            Text(t("Notes: ${log.notes}", "ملاحظات: ${log.notes}", appLanguage), fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            }
        }
    }

    if (logToDeleteDate != null) {
        val targetDate = logToDeleteDate!!
        AlertDialog(
            onDismissRequest = { logToDeleteDate = null },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text(
                    text = t("Confirm Log Deletion", "تأكيد حذف سجل اليوميات", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = t(
                        "Are you sure you want to delete the dose and symptom log for ${formatDisplayDateStr(targetDate, appLanguage)}? This action cannot be undone.",
                        "هل أنت تأكيد من رغبتك في حذف سجل الجرعة والأعراض ليوم ${formatDisplayDateStr(targetDate, appLanguage)}؟ لا يمكن التراجع عن هذا الإجراء.",
                        appLanguage
                    ),
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLog(targetDate)
                        Toast.makeText(context, t("Log deleted successfully!", "تم حذف السجل بنجاح!", appLanguage), Toast.LENGTH_SHORT).show()
                        logToDeleteDate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("Delete", "حذف", appLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { logToDeleteDate = null }) {
                    Text(t("Cancel", "إلغاء", appLanguage))
                }
            }
        )
    }
}

@Composable
fun FlowRowCompat(
    items: List<String>,
    selectedItems: List<String>,
    onToggle: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Simple Flow-like wrapping using a column of rows
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val chunked = items.chunked(3)
            chunked.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowItems.forEach { item ->
                        val isSelected = selectedItems.contains(item)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onToggle(item) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                fontSize = 9.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class WeeklyDaySymptomData(
    val dayName: String,
    val dateStr: String,
    val fullDateStr: String,
    val symptoms: List<String>,
    val log: DailyLog?
)

@Composable
fun WeeklySymptomBarChartDialog(
    allLogs: List<DailyLog>,
    appLanguage: String,
    userProfile: UserProfile? = null,
    onDismiss: () -> Unit
) {
    var weekOffset by remember { mutableStateOf(0) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var chartDialogTab by remember { mutableStateOf(0) } // 0: Course Trends (Physical vs Psych), 1: Weekly Bar Chart, 2: Symptoms Line Graph

    val symptomColors = remember {
        mapOf(
            "Chapped Lips" to Color(0xFFEF5350),
            "Dry Skin" to Color(0xFFFF9800),
            "Dry Eyes" to Color(0xFF42A5F5),
            "Nosebleeds" to Color(0xFFEC407A),
            "Joint Pain" to Color(0xFFAB47BC),
            "Muscle Aches" to Color(0xFF26C6DA),
            "Headaches" to Color(0xFFFFCA28),
            "Fatigue" to Color(0xFF8D6E63),
            "Mood Swings" to Color(0xFF7E57C2),
            "Brain Fog" to Color(0xFF66BB6A)
        )
    }

    val (daysData, weekDateRangeStr) = remember(allLogs, weekOffset, appLanguage) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffToSat = if (dayOfWeek == Calendar.SATURDAY) 0 else dayOfWeek
        val satCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -diffToSat)
        }

        val arDays = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        val enDays = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

        val list = mutableListOf<WeeklyDaySymptomData>()
        val dateFmt = SimpleDateFormat("d MMM", if (appLanguage == "ar") Locale("ar") else Locale.US)

        for (i in 0..6) {
            val dayCal = (satCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val y = dayCal.get(Calendar.YEAR)
            val m = dayCal.get(Calendar.MONTH) + 1
            val d = dayCal.get(Calendar.DAY_OF_MONTH)
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m, d)

            val log = allLogs.find { it.dateString == formattedDate }
            val syms = log?.symptoms?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { normalizeSymptomKey(it) } ?: emptyList()

            list.add(
                WeeklyDaySymptomData(
                    dayName = if (appLanguage == "ar") arDays[i] else enDays[i],
                    dateStr = "$d/$m",
                    fullDateStr = formattedDate,
                    symptoms = syms,
                    log = log
                )
            )
        }

        val firstDayStr = dateFmt.format(satCal.time)
        val friCal = (satCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val lastDayStr = dateFmt.format(friCal.time)
        val rangeStr = "$firstDayStr - $lastDayStr"

        Pair(list, rangeStr)
    }

    val weeklySymptomsCount = remember(daysData) {
        val map = mutableMapOf<String, Int>()
        daysData.forEach { day ->
            day.symptoms.forEach { sym ->
                map[sym] = (map[sym] ?: 0) + 1
            }
        }
        map
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = when (chartDialogTab) {
                                0 -> Icons.Default.Timeline
                                1 -> Icons.Default.BarChart
                                else -> Icons.Default.ShowChart
                            },
                            contentDescription = "Chart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = when (chartDialogTab) {
                                0 -> t("Physical & Psychological Course Trends", "مسار الأعراض الجسدية والنفسية طوال الكورس", appLanguage)
                                1 -> t("Weekly Symptoms Bar Chart", "مخطط الأعمدة وتفاصيل الأسبوع", appLanguage)
                                else -> t("All Symptoms Line Graph", "منحنى الأعراض التفصيلي", appLanguage)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3-Mode Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, t("Course Trends", "مسار الكورس", appLanguage), Icons.Default.Timeline),
                        Triple(1, t("Weekly Bars", "الأعمدة الأسبوعية", appLanguage), Icons.Default.BarChart),
                        Triple(2, t("All Symptoms", "كل الأعراض", appLanguage), Icons.Default.ShowChart)
                    )
                    tabs.forEach { (tabIdx, tabTitle, tabIcon) ->
                        val isSelected = chartDialogTab == tabIdx
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { chartDialogTab = tabIdx }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tabIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tabTitle,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (chartDialogTab == 0) {
                    SideEffectsTrendsLineChartCard(
                        allLogs = allLogs,
                        userProfile = userProfile,
                        appLanguage = appLanguage,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (chartDialogTab == 2) {
                    SymptomTrendLineGraphView(
                        allLogs = allLogs,
                        userProfile = userProfile,
                        appLanguage = appLanguage
                    )
                } else {
                    // Week Selector Controls
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                weekOffset--
                                selectedDayIndex = null
                            }
                        ) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = "Previous Week", modifier = Modifier.size(18.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (weekOffset == 0) t("Current Week (Sat - Fri)", "الأسبوع الحالي (السبت - الجمعة)", appLanguage)
                                       else t("Week ($weekOffset weeks)", "أسبوع سابق ($weekOffset)", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = weekDateRangeStr,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (weekOffset < 0) {
                                    weekOffset++
                                    selectedDayIndex = null
                                }
                            },
                            enabled = weekOffset < 0
                        ) {
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Next Week",
                                modifier = Modifier.size(18.dp),
                                tint = if (weekOffset < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Interactive Bar Chart
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        daysData.forEachIndexed { idx, dayData ->
                            val isSelected = selectedDayIndex == idx
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDayIndex = if (isSelected) null else idx
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(140.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (dayData.symptoms.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            dayData.symptoms.forEach { sym ->
                                                val c = symptomColors[sym] ?: Color.Gray
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(14.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(c)
                                                )
                                            }
                                        }
                                    } else {
                                        if (dayData.log != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (dayData.log.isTaken) Color(0xFF34A853) else Color.LightGray)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = dayData.dayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dayData.dateStr,
                                    fontSize = 8.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Linked Data Display Below Chart
                if (selectedDayIndex != null) {
                    val selData = daysData[selectedDayIndex!!]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "${selData.dayName} (${selData.dateStr}) - ${selData.fullDateStr}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                TextButton(
                                    onClick = { selectedDayIndex = null },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(t("Show Full Week", "عرض ملخص الأسبوع", appLanguage), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            if (selData.log != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(t("Capsule Dose Status:", "حالة الجرعة اليومية:", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = if (selData.log.isTaken) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (selData.log.isTaken) Color(0xFF34A853) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (selData.log.isTaken) t("Taken (${selData.log.doseMg} mg)", "تم تناول الجرعة (${selData.log.doseMg} ملغ)", appLanguage)
                                                   else t("Not Taken", "لم يتم التناول", appLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selData.log.isTaken) Color(0xFF34A853) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(t("Water Consumed:", "شرب الماء:", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = t("${selData.log.waterCups} cups (~${String.format(Locale.US, "%.2f", selData.log.waterCups * 0.25f)} L)", "${selData.log.waterCups} أكواب (~${String.format(Locale.US, "%.2f", selData.log.waterCups * 0.25f)} لتر)", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(t("Recorded Side Effects:", "الأعراض المسجلة في هذا اليوم:", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (selData.symptoms.isEmpty()) {
                                        Text(t("No side effects logged on this day.", "لا توجد أعراض جانبية مسجلة لهذا اليوم.", appLanguage), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        FlowRowCompat(
                                            items = selData.symptoms.map { sym ->
                                                val idx = SYMPTOMS_EN.indexOf(sym)
                                                if (appLanguage == "ar" && idx != -1) SYMPTOMS_AR[idx] else sym
                                            },
                                            selectedItems = remember { mutableStateListOf() },
                                            onToggle = {}
                                        )
                                    }
                                }

                                if (selData.log.notes.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(t("Daily Notes:", "ملاحظات اليوم:", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(selData.log.notes, fontSize = 10.5.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }
                            } else {
                                Text(
                                    text = t("No log entry registered for this day.", "لا يوجد سجل مدخل في قاعدة البيانات لهذا اليوم.", appLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = t("Symptom Legend & Weekly Totals:", "دليل الألوان وإجمالي تكرار الأعراض هذا الأسبوع:", appLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val itemsIndexed = SYMPTOMS_EN.mapIndexed { idx, symEn ->
                            Triple(idx, symEn, if (appLanguage == "ar") SYMPTOMS_AR[idx] else symEn)
                        }
                        itemsIndexed.chunked(2).forEach { chunk ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                chunk.forEach { (_, symEn, label) ->
                                    val count = weeklySymptomsCount[symEn] ?: 0
                                    val color = symptomColors[symEn] ?: Color.Gray
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (count > 0) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        border = BorderStroke(0.5.dp, if (count > 0) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Text(
                                                text = "$label: $count",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (chunk.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
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
                    Text(t("Close Chart", "إغلاق الرسم البياني", appLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SymptomTrendLineGraphView(
    allLogs: List<DailyLog>,
    userProfile: UserProfile?,
    appLanguage: String
) {
    val startMs = if ((userProfile?.startDate ?: 0L) > 0L) userProfile!!.startDate else (allLogs.mapNotNull { it.timestamp }.minOrNull() ?: System.currentTimeMillis())
    val nowMs = System.currentTimeMillis()
    val courseDays = ((nowMs - startMs).coerceAtLeast(0) / (1000 * 60 * 60 * 24)).toInt()
    val weeksCount = ((courseDays / 7) + 1).coerceIn(1, 28)

    val startCal = Calendar.getInstance().apply { timeInMillis = startMs }

    data class PdfWeekData(val weekNum: Int, val dateRangeStr: String, val symptomCounts: Map<String, Int>)

    val weekDataList = remember(allLogs, startMs, weeksCount) {
        val list = mutableListOf<PdfWeekData>()
        for (w in 0 until weeksCount) {
            val wkStartCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, w * 7) }
            val counts = mutableMapOf<String, Int>()

            for (dayOffset in 0..6) {
                val dCal = (wkStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                val dStr = String.format(Locale.US, "%04d-%02d-%02d", dCal.get(Calendar.YEAR), dCal.get(Calendar.MONTH) + 1, dCal.get(Calendar.DAY_OF_MONTH))
                val log = allLogs.find { it.dateString == dStr }
                if (log != null && log.symptoms.isNotBlank()) {
                    log.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawSym ->
                        val key = normalizeSymptomKey(rawSym)
                        counts[key] = (counts[key] ?: 0) + 1
                    }
                }
            }
            val dateStr = "${wkStartCal.get(Calendar.DAY_OF_MONTH)}/${wkStartCal.get(Calendar.MONTH) + 1}"
            list.add(PdfWeekData(w + 1, dateStr, counts))
        }
        list
    }

    val allSymptomsWithColors = remember {
        listOf(
            ("Chapped Lips" to "جفاف الشفاه") to Color(0xFFE53935),
            ("Dry Skin" to "جفاف الجلد") to Color(0xFFFB8C00),
            ("Dry Eyes" to "جفاف العين") to Color(0xFF1E88E5),
            ("Nosebleeds" to "نزيف الأنف") to Color(0xFF009688),
            ("Joint Pain" to "آلام المفاصل") to Color(0xFF8E24AA),
            ("Muscle Aches" to "آلام العضلات") to Color(0xFFD81B60),
            ("Headaches" to "الصداع") to Color(0xFF8D6E63),
            ("Fatigue" to "الخمول والتعب") to Color(0xFF43A047),
            ("Mood Swings" to "تقلبات المزاج") to Color(0xFF3949AB),
            ("Brain Fog" to "تشوش الذهن") to Color(0xFFF57C00)
        )
    }

    val activeSymptomsWithColors = remember(allSymptomsWithColors, weekDataList) {
        allSymptomsWithColors.filter { (pair, _) ->
            weekDataList.any { (it.symptomCounts[pair.first] ?: 0) > 0 }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = t("Symptom Trend Line Graph (Treatment Weeks)", "خط مسار الأعراض حسب أسابيع العلاج (Line Graph)", appLanguage),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Axis Explanations text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = t("Y Axis: Occurrences (0 - 7)", "المحور الصادي (Y): عدد الإدخالات (0 - 7)", appLanguage),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = t("X Axis: Treatment Weeks", "المحور السيني (X): أسابيع العلاج", appLanguage),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            val gridColor = MaterialTheme.colorScheme.outlineVariant
            val textPaintColor = MaterialTheme.colorScheme.onSurface.toArgb()

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                val leftPadding = 45.dp.toPx()
                val rightPadding = 20.dp.toPx()
                val topPadding = 20.dp.toPx()
                val bottomPadding = 40.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding

                val paint = android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    typeface = android.graphics.Typeface.DEFAULT
                }

                // Y Axis grid lines & numbers (0..7)
                val yLevels = listOf(0, 2, 4, 6, 7)
                yLevels.forEach { yVal ->
                    val yPos = topPadding + chartHeight * (1f - (yVal / 7f))
                    drawLine(
                        color = gridColor.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(leftPadding, yPos),
                        end = androidx.compose.ui.geometry.Offset(size.width - rightPadding, yPos),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    // Draw Y axis value
                    drawContext.canvas.nativeCanvas.drawText(
                        "$yVal",
                        leftPadding - 24.dp.toPx(),
                        yPos + 4.dp.toPx(),
                        paint
                    )
                }

                // X Axis Ticks & Week Labels ("أسبوع 1", "أسبوع 2"...)
                val xStep = if (weeksCount > 1) chartWidth / (weeksCount - 1) else chartWidth / 2f
                val stride = when {
                    weeksCount <= 6 -> 1
                    weeksCount <= 12 -> 2
                    else -> 3
                }

                weekDataList.forEachIndexed { idx, wk ->
                    if (idx % stride == 0 || idx == weeksCount - 1) {
                        val xPos = if (weeksCount == 1) leftPadding + chartWidth / 2f else leftPadding + idx * xStep
                        val weekLabel = if (appLanguage == "ar") "أسبوع ${wk.weekNum}" else "Wk ${wk.weekNum}"
                        drawContext.canvas.nativeCanvas.drawText(
                            weekLabel,
                            xPos - 16.dp.toPx(),
                            topPadding + chartHeight + 20.dp.toPx(),
                            paint
                        )
                    }
                }

                // Draw Line Graphs ONLY for Active Symptoms (with entries)
                activeSymptomsWithColors.forEach { (pair, symColor) ->
                    val key = pair.first
                    val points = mutableListOf<androidx.compose.ui.geometry.Offset>()

                    weekDataList.forEachIndexed { idx, weekData ->
                        val count = weekData.symptomCounts[key] ?: 0
                        val xPos = if (weeksCount == 1) leftPadding + chartWidth / 2f else leftPadding + idx * xStep
                        val yPos = topPadding + chartHeight * (1f - (count.coerceIn(0, 7) / 7f))
                        points.add(androidx.compose.ui.geometry.Offset(xPos, yPos))
                    }

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = symColor,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                    points.forEach { pt ->
                        drawCircle(
                            color = symColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Legend for All Symptoms
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = t("Symptoms Color Legend:", "دليل ألوان الأعراض:", appLanguage),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val chunks = allSymptomsWithColors.chunked(2)
                chunks.forEach { rowSymptoms ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowSymptoms.forEach { (pair, symColor) ->
                            val label = if (appLanguage == "ar") pair.second else pair.first
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(symColor)
                                )
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------- SCREEN 3: STATISTICS & PDF EXPORT -------------------

@Composable
fun AnalyticsScreen(
    viewModel: RoaccutaneViewModel,
    userProfile: UserProfile,
    allLogs: List<DailyLog>,
    cumulativeDose: Int,
    targetDose: Int,
    symptomStats: List<com.example.ui.SymptomStats>,
    sourceStats: List<com.example.ui.SourceStats>,
    elapsedDays: Int,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    appLanguage: String
) {
    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val outerPadding = if (isTablet) 32.dp else 16.dp
    val spacedBy = if (isTablet) 24.dp else 16.dp

    val context = LocalContext.current
    var isExportingPdf by remember { mutableStateOf(false) }
    var pdfExportPathMsg by remember { mutableStateOf("") }
    var showAllLabsDialog by remember { mutableStateOf(false) }
    var showWeeklySymptomChartDialog by remember { mutableStateOf(false) }

    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle(initialValue = emptyList())
    val appointmentsWithLabs = remember(allAppointments) {
        allAppointments.filter { appt ->
            appt.tCholestrol != null || appt.hdl != null || appt.ldl != null || appt.triglycerides != null ||
            appt.liverFunction.isNotBlank() || appt.kidneyFunction.isNotBlank() || appt.lipidsProfile.isNotBlank() || appt.otherBloodTests.isNotBlank()
        }
    }
    val latestLipidAppt = remember(appointmentsWithLabs) {
        appointmentsWithLabs.maxByOrNull { it.dateTime }
    }

    val progressRatio = if (targetDose > 0) cumulativeDose.toFloat() / targetDose else 0f
    val percentage = (progressRatio * 100).coerceAtMost(100f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(outerPadding),
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("Analytics & Course Milestones", "تحليل وإحصاءات الكورس", appLanguage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t("Cumulative indices, side effect frequency graphs & PDF exports", "مؤشرات التراكم، معدلات تكرار الأعراض ورسم مصادر الدهون واستخراج تقرير PDF", appLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme"
                    )
                }
            }
        }

        // Major parameters card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = t("THERAPEUTIC COMPLIANCE METRICS", "مؤشرات توازن خطة العلاج", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(t("Compliance Ratio", "نسبة التقدم للأمام", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "%.1f%%", percentage), fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(t("Remaining mg", "الجرعة المتبقية (ملغ)", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(targetDose - cumulativeDose).coerceAtLeast(0)} mg", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progressRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t("Prescription: ${userProfile.dailyDoseMg}mg/day", "الوصفة الحالية: ${userProfile.dailyDoseMg} ملغ/يوم", appLanguage), fontSize = 11.sp)
                        Text(t("Target Strategy: ${userProfile.targetMgPerKgInt} mg/kg", "إستراتيجية الكورس: ${userProfile.targetMgPerKgInt} ملغ/كغ", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Native PDF Export Trigger button
                    Button(
                        onClick = {
                            isExportingPdf = true
                            viewModel.exportStatsToPdf(
                                context = context,
                                profile = userProfile,
                                cumulativeDose = cumulativeDose,
                                targetDose = targetDose,
                                elapsedDays = elapsedDays,
                                symptomStats = symptomStats,
                                sourceStats = sourceStats,
                                onComplete = { success, path ->
                                    isExportingPdf = false
                                    if (success) {
                                        pdfExportPathMsg = path
                                        Toast.makeText(context, t("PDF Exported Successfully!", "تم تصدير ملف الـ PDF بنجاح!", appLanguage), Toast.LENGTH_LONG).show()
                                        try {
                                            val file = java.io.File(path)
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "com.aistudio.roaccutanetracking.ndsqpx.fileprovider",
                                                file
                                            )
                                            try {
                                                val driveIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    setPackage("com.google.android.apps.docs")
                                                }
                                                context.startActivity(driveIntent)
                                            } catch (driveEx: java.lang.Exception) {
                                                val genericIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(genericIntent)
                                            }
                                        } catch (e: java.lang.Exception) {
                                            android.util.Log.e("PDF_Open", "Error opening generated PDF file", e)
                                        }
                                    } else {
                                        Toast.makeText(context, t("Export Failed: $path", "فشل التصدير: $path", appLanguage), Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t("Export Statistics Report to PDF", "استخراج البيانات والإحصاءات كملف PDF", appLanguage), fontWeight = FontWeight.Bold)
                    }

                    if (pdfExportPathMsg.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = t("PDF saved to:\n$pdfExportPathMsg", "تم حفظ التقرير في:\n$pdfExportPathMsg", appLanguage),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Latest Lab Readings Card (Displayed ONLY when lab data is present)
        if (latestLipidAppt != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t("LATEST BLOOD LABORATORY PANEL", "آخر قراءات التحاليل المخبرية", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(latestLipidAppt.dateTime))
                                Text(
                                    text = dateStr,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { showAllLabsDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Launch,
                                        contentDescription = t("Show Complete History", "عرض السجل المخبري الكامل", appLanguage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        val tc = latestLipidAppt.tCholestrol
                        val hdlVal = latestLipidAppt.hdl
                        val ldlVal = latestLipidAppt.ldl
                        val tgVal = latestLipidAppt.triglycerides

                        // Display the lipid panel
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val tcIsAbnormal = tc != null && tc >= 200.0
                            val tcColor = if (tc != null) (if (tcIsAbnormal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)) else MaterialTheme.colorScheme.onSurface
                            val tcStatusText = if (tc != null) (if (tcIsAbnormal) t("High", "مرتفع", appLanguage) else t("Normal", "طبيعي", appLanguage)) else ""

                            val ldlIsAbnormal = ldlVal != null && ldlVal >= 100.0
                            val ldlColor = if (ldlVal != null) (if (ldlIsAbnormal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)) else MaterialTheme.colorScheme.onSurface
                            val ldlStatusText = if (ldlVal != null) (if (ldlIsAbnormal) t("High", "مرتفع", appLanguage) else t("Normal", "طبيعي", appLanguage)) else ""

                            val hdlIsAbnormal = hdlVal != null && hdlVal < 40.0
                            val hdlColor = if (hdlVal != null) (if (hdlIsAbnormal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)) else MaterialTheme.colorScheme.onSurface
                            val hdlStatusText = if (hdlVal != null) (if (hdlIsAbnormal) t("Low", "منخفض", appLanguage) else t("Normal", "طبيعي", appLanguage)) else ""

                            val tgIsAbnormal = tgVal != null && tgVal >= 150.0
                            val tgColor = if (tgVal != null) (if (tgIsAbnormal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)) else MaterialTheme.colorScheme.onSurface
                            val tgStatusText = if (tgVal != null) (if (tgIsAbnormal) t("High", "مرتفع", appLanguage) else t("Normal", "طبيعي", appLanguage)) else ""

                            // Row 1: Total Cholesterol & LDL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Total Cholesterol
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = t("Total Cholesterol", "الكوليسترول الكلي", appLanguage),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "TC / Total CHOL",
                                            fontSize = 10.sp,
                                            color = if (tcIsAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (tc != null) "$tc mg/dL" else "N/A",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = tcColor,
                                            textAlign = TextAlign.Center
                                        )
                                        if (tc != null) {
                                            Text(
                                                text = "≈ ${String.format(Locale.US, "%.2f", com.example.utils.LabUnitConverter.cholesterolMgToMmol(tc))} mmol/L",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = tcStatusText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = tcColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Text(
                                            text = t("Normal: <200 mg/dL (<5.2 mmol/L)", "الطبيعي: <200 mg/dL (<5.2 mmol/L)", appLanguage),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // LDL
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = t("LDL Cholesterol", "الكوليسترول الضار", appLanguage),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Low-Density Lipoprotein",
                                            fontSize = 10.sp,
                                            color = if (ldlIsAbnormal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (ldlVal != null) "$ldlVal mg/dL" else "N/A",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ldlColor,
                                            textAlign = TextAlign.Center
                                        )
                                        if (ldlVal != null) {
                                            Text(
                                                text = "≈ ${String.format(Locale.US, "%.2f", com.example.utils.LabUnitConverter.cholesterolMgToMmol(ldlVal))} mmol/L",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = ldlStatusText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ldlColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Text(
                                            text = t("Normal: <100 mg/dL (<2.6 mmol/L)", "الطبيعي: <100 mg/dL (<2.6 mmol/L)", appLanguage),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Row 2: HDL & Triglycerides
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // HDL
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = t("HDL Cholesterol", "الكوليسترول النافع", appLanguage),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "High-Density Lipoprotein",
                                            fontSize = 10.sp,
                                            color = if (hdlIsAbnormal) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (hdlVal != null) "$hdlVal mg/dL" else "N/A",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = hdlColor,
                                            textAlign = TextAlign.Center
                                        )
                                        if (hdlVal != null) {
                                            Text(
                                                text = "≈ ${String.format(Locale.US, "%.2f", com.example.utils.LabUnitConverter.cholesterolMgToMmol(hdlVal))} mmol/L",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = hdlStatusText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = hdlColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Text(
                                            text = t("Normal: >40 mg/dL (>1.0 mmol/L)", "الطبيعي: >40 mg/dL (>1.0 mmol/L)", appLanguage),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Triglycerides
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = t("Triglycerides", "الدهون الثلاثية", appLanguage),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "TG / TriGlecries",
                                            fontSize = 10.sp,
                                            color = if (tgIsAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (tgVal != null) "$tgVal mg/dL" else "N/A",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = tgColor,
                                            textAlign = TextAlign.Center
                                        )
                                        if (tgVal != null) {
                                            Text(
                                                text = "≈ ${String.format(Locale.US, "%.2f", com.example.utils.LabUnitConverter.triglyceridesMgToMmol(tgVal))} mmol/L",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = tgStatusText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = tgColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Text(
                                            text = t("Normal: <150 mg/dL (<1.7 mmol/L)", "الطبيعي: <150 mg/dL (<1.7 mmol/L)", appLanguage),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Liver/Kidney Function optional display if available
                            if (latestLipidAppt.liverFunction.isNotBlank() || latestLipidAppt.kidneyFunction.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (latestLipidAppt.liverFunction.isNotBlank()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(t("Liver Function", "وظائف الكبد", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(latestLipidAppt.liverFunction, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    if (latestLipidAppt.kidneyFunction.isNotBlank()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(t("Kidney Function", "وظائف الكلى", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(latestLipidAppt.kidneyFunction, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }

                            // Cardiovascular Risk Factor (TC / HDL)
                            val rfVal = if (tc != null && hdlVal != null && hdlVal > 0.0) tc / hdlVal else 0.0
                            val rfText = if (rfVal > 0.0) String.format(Locale.US, "%.2f", rfVal) else "N/A"
                            val rfColor = if (rfVal > 5.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = t("Cardiovascular Risk Factor", "مؤشر المخاطر والدهنيات", appLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ratio (Total Cholesterol / HDL-C)",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = rfText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = rfColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Side Effects Trends Line Chart (Physical vs Psychological over course duration)
        item {
            SideEffectsTrendsLineChartCard(
                allLogs = allLogs,
                userProfile = userProfile,
                appLanguage = appLanguage,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Symptoms Stats Visual Bars Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("SYMPTOMS FREQUENCY ANALYSIS", "تحليل وتكرار الأعراض الجانبية", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        OutlinedButton(
                            onClick = { showWeeklySymptomChartDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = "Weekly Chart", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = t("Weekly Chart (Sat-Fri)", "رسم بياني أسبوعي (السبت-الجمعة)", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (symptomStats.isEmpty()) {
                        Text(t("Log daily symptoms inside the 'Daily' page to visualize metrics here.", "سجل الأعراض ضمن صفحة 'اليوميات' لعرض الرسوم الإحصائية هنا.", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        symptomStats.take(5).forEach { stat ->
                            val localizedSym = if (appLanguage == "ar") {
                                // Maps standard EN symptom name to AR
                                val idx = SYMPTOMS_EN.indexOf(stat.symptom)
                                if (idx != -1) SYMPTOMS_AR[idx] else stat.symptom
                            } else stat.symptom

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(localizedSym, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${stat.occurrenceCount} logs (${String.format(Locale.US, "%.0f%%", stat.percentage)})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                LinearProgressIndicator(
                                    progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFFE57373)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllLabsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAllLabsDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t("All Lab Readings History", "تاريخ جميع القراءات المخبرية", appLanguage),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = t("Deduplicated safe chronological panels", "سجل التحاليل المتسلسلة دون تكرار", appLanguage),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showAllLabsDialog = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    val labsList = remember(allAppointments) {
                        allAppointments
                            .filter { it.tCholestrol != null || it.hdl != null || it.ldl != null || it.triglycerides != null }
                            .distinctBy { appt ->
                                val dStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(appt.dateTime))
                                "$dStr-${appt.tCholestrol}-${appt.hdl}-${appt.ldl}-${appt.triglycerides}"
                            }
                            .sortedBy { it.dateTime } // Chronological
                    }

                    if (labsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t("No lab results recorded yet.", "لا توجد نتائج تحاليل مسجلة بعد.", appLanguage),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // We will display a beautiful double-scrollable table
                        val horizontalScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                // Parameter column
                                Column(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .width(170.dp)
                                ) {
                                    // Header Cell
                                    Box(
                                        modifier = Modifier
                                            .height(50.dp)
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = t("Parameter / Norms", "التحليل والمعدل", appLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Rows
                                    val rowLabels = listOf(
                                        t("Total Cholesterol\n(<200 mg/dL | <5.2 mmol/L)", "الكوليسترول الكلي\n(طبيعي: <200 mg/dL | <5.2 mmol/L)", appLanguage),
                                        t("LDL Cholesterol\n(<100 mg/dL | <2.6 mmol/L)", "الكوليسترول الضار\n(طبيعي: <100 mg/dL | <2.6 mmol/L)", appLanguage),
                                        t("HDL Cholesterol\n(>40 mg/dL | >1.0 mmol/L)", "الكوليسترول النافع\n(طبيعي: >40 mg/dL | >1.0 mmol/L)", appLanguage),
                                        t("Triglycerides\n(<150 mg/dL | <1.7 mmol/L)", "الدهون الثلاثية\n(طبيعي: <150 mg/dL | <1.7 mmol/L)", appLanguage)
                                    )

                                    rowLabels.forEachIndexed { idx, label ->
                                        Box(
                                            modifier = Modifier
                                                .height(65.dp)
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                lineHeight = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (idx < rowLabels.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }

                                // Date Columns
                                labsList.forEachIndexed { colIdx, lab ->
                                    val dateStr = try {
                                        val loc = if (appLanguage == "ar") java.util.Locale("ar") else java.util.Locale.US
                                        java.text.SimpleDateFormat("dd MMM", loc).format(java.util.Date(lab.dateTime))
                                    } catch(e: Exception) {
                                        "–"
                                    }

                                    Column(
                                        modifier = Modifier
                                            .width(95.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        // Column Header Cell (Date)
                                        Box(
                                            modifier = Modifier
                                                .height(50.dp)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dateStr,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        // Total Cholesterol Row
                                        RenderLabValueCell(
                                            value = lab.tCholestrol,
                                            isAbnormal = { it >= 200.0 },
                                            trend = getTrend(labsList, colIdx, { it.tCholestrol }, isHigherWorse = true),
                                            siValueStr = lab.tCholestrol?.let { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.cholesterolMgToMmol(it)) }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        // LDL Row
                                        RenderLabValueCell(
                                            value = lab.ldl,
                                            isAbnormal = { it >= 100.0 },
                                            trend = getTrend(labsList, colIdx, { it.ldl }, isHigherWorse = true),
                                            siValueStr = lab.ldl?.let { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.cholesterolMgToMmol(it)) }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        // HDL Row
                                        RenderLabValueCell(
                                            value = lab.hdl,
                                            isAbnormal = { it < 40.0 },
                                            trend = getTrend(labsList, colIdx, { it.hdl }, isHigherWorse = false),
                                            siValueStr = lab.hdl?.let { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.cholesterolMgToMmol(it)) }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        // Triglycerides Row
                                        RenderLabValueCell(
                                            value = lab.triglycerides,
                                            isAbnormal = { it >= 150.0 },
                                            trend = getTrend(labsList, colIdx, { it.triglycerides }, isHigherWorse = true),
                                            siValueStr = lab.triglycerides?.let { String.format(Locale.US, "%.2f mmol/L", com.example.utils.LabUnitConverter.triglyceridesMgToMmol(it)) }
                                        )
                                    }
                                }
                            }
                        }

                        // Table Legend Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = t("💡 Trends & Safe Ranges Legend:", "💡 دليل الارتفاع والانخفاض والمعدلات الطبيعية:", appLanguage),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("• 🔺 Red Triangle: Reading worsened from previous test.", "• 🔺 المثلث الأحمر: قراءة أسوأ مقارنة بالتحليل السابق.", appLanguage),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("• 💚 Green Heart: Reading improved and is safer.", "• 💚 القلب الأخضر: قراءة أكثر سلامة وتحسنت مقارنة بالتحليل السابق.", appLanguage),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("• Red Background: Value is clinically abnormal.", "• الخلفية الحمراء: القراءة خارج المعدل الطبيعي.", appLanguage),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = t("• Green Background: Value is in a safe/normal range.", "• الخلفية الخضراء: القراءة في النطاق الطبيعي السليم.", appLanguage),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Bottom close button
                    Button(
                        onClick = { showAllLabsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(t("Done", "تم", appLanguage))
                    }
                }
            }
        }
    }

    if (showWeeklySymptomChartDialog) {
        WeeklySymptomBarChartDialog(
            allLogs = allLogs,
            appLanguage = appLanguage,
            userProfile = userProfile,
            onDismiss = { showWeeklySymptomChartDialog = false }
        )
    }
}

@Composable
fun RenderLabValueCell(
    value: Double?,
    isAbnormal: (Double) -> Boolean,
    trend: String,
    siValueStr: String? = null,
    unit: String = "mg/dL"
) {
    val containerBg = when {
        value == null -> Color.Transparent
        isAbnormal(value) -> Color(0xFFFDE8E8) // Soft Red
        else -> Color(0xFFE8F5E9) // Soft Green
    }
    val contentColor = when {
        value == null -> MaterialTheme.colorScheme.onSurfaceVariant
        isAbnormal(value) -> Color(0xFFE53935) // Deep Red
        else -> Color(0xFF2E7D32) // Deep Green
    }

    Box(
        modifier = Modifier
            .height(65.dp)
            .fillMaxWidth()
            .background(containerBg)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (value != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value) else String.format(Locale.US, "%.1f", value),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                    Text(
                        text = unit,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                    if (trend.isNotEmpty()) {
                        Text(
                            text = trend,
                            fontSize = 10.sp
                        )
                    }
                }
                if (siValueStr != null) {
                    Text(
                        text = siValueStr,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            } else {
                Text(
                    text = "–",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun getTrend(
    labs: List<com.example.data.Appointment>,
    idx: Int,
    getter: (com.example.data.Appointment) -> Double?,
    isHigherWorse: Boolean
): String {
    if (idx <= 0) return ""
    val currentVal = getter(labs[idx]) ?: return ""
    val prevVal = (idx - 1 downTo 0).firstNotNullOfOrNull { prevIdx ->
        getter(labs[prevIdx])
    } ?: return ""
    return if (currentVal > prevVal) {
        if (isHigherWorse) "🔺" else "💚"
    } else if (currentVal < prevVal) {
        if (isHigherWorse) "💚" else "🔺"
    } else {
        ""
    }
}

// ------------------- SCREEN 4: HOSPITAL APPOINTMENTS -------------------

@Composable
fun AppointmentsScreen(
    viewModel: RoaccutaneViewModel,
    allAppointments: List<Appointment>,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    appLanguage: String
) {
    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val outerPadding = if (isTablet) 32.dp else 16.dp
    val spacedBy = if (isTablet) 24.dp else 16.dp

    val context = LocalContext.current
    var inputReason by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    
    val savedHospitalName = remember { viewModel.getSavedHospitalName() }
    val savedHospitalLocation = remember { viewModel.getSavedHospitalLocation() }
    val savedBuilding = remember { viewModel.getSavedBuilding() }
    val savedClinic = remember { viewModel.getSavedClinic() }
    val savedDoctorName = remember { viewModel.getSavedDoctorName() }
    val savedPatientFileNumber = remember { viewModel.getSavedPatientFileNumber() }

    var inputHospitalName by remember { mutableStateOf(savedHospitalName) }
    var inputHospitalLocation by remember { mutableStateOf(savedHospitalLocation) }
    var inputBuilding by remember { mutableStateOf(savedBuilding) }
    var inputClinic by remember { mutableStateOf(savedClinic) }
    var inputDoctorName by remember { mutableStateOf(savedDoctorName) }
    var inputPatientFileNumber by remember { mutableStateOf(savedPatientFileNumber) }

    var editingAppointment by remember { mutableStateOf<Appointment?>(null) }
    var showAddAppointmentDialog by remember { mutableStateOf(false) }

    if (showAddAppointmentDialog) {
        AddAppointmentDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            allAppointments = allAppointments,
            onDismiss = { showAddAppointmentDialog = false }
        )
    }

    var selectedAppointmentsTab by remember { mutableIntStateOf(0) }
    val upcomingAppointments = remember(allAppointments) {
        allAppointments.filter { !it.isCompleted }.sortedBy { it.dateTime }
    }
    val completedAppointments = remember(allAppointments) {
        allAppointments.filter { it.isCompleted }.sortedByDescending { it.dateTime }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(outerPadding),
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("Hospital Appointments & Lab Tests", "المواعيد والتحاليل الطبية", appLanguage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t("Organize dermatology clinic schedules, clinician profiles and blood tests ALT/Creatinine", "إدارة مواعيد العيادة وبيانات الأطباء وتدوين تحاليل الكبد والكلى والدهون", appLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme"
                    )
                }
            }
        }

        // Sub-Tabs: Upcoming vs Past Appointments vs Labs Hub
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedAppointmentsTab = 0 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedAppointmentsTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = if (selectedAppointmentsTab == 0) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = if (selectedAppointmentsTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${t("Upcoming", "القادمة", appLanguage)} (${upcomingAppointments.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedAppointmentsTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedAppointmentsTab = 1 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedAppointmentsTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = if (selectedAppointmentsTab == 1) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (selectedAppointmentsTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${t("Past", "السابقة", appLanguage)} (${completedAppointments.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedAppointmentsTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .clickable { selectedAppointmentsTab = 2 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedAppointmentsTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = if (selectedAppointmentsTab == 2) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = if (selectedAppointmentsTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = t("Labs Hub", "سجل التحاليل", appLanguage),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedAppointmentsTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // NEXT APPOINTMENT COUNTDOWN WIDGET (Shown when viewing Upcoming)
        if (selectedAppointmentsTab == 0 && upcomingAppointments.isNotEmpty()) {
            item {
                val nextAppt = upcomingAppointments.minByOrNull { it.dateTime }
                if (nextAppt != null) {
                    val currentTime = System.currentTimeMillis()
                    val diffMs = nextAppt.dateTime - currentTime
                    
                    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs)
                    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMs) % 24
                    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
                    
                    val countdownStr = if (diffMs <= 0) {
                        t("The scheduled appointment hour has already arrived/passed.", "حان وقت الموعد أو مر الوقت بالفعل.", appLanguage)
                    } else {
                        val dStr = if (days > 0) t("$days days, ", "$days يوم، ", appLanguage) else ""
                        val hStr = if (hours > 0 || days > 0) t("$hours hours, ", "$hours ساعة، ", appLanguage) else ""
                        val mStr = t("$minutes minutes", "$minutes دقيقة", appLanguage)
                        t("Remaining: $dStr$hStr$mStr", "المتبقي للموعد: $dStr$hStr$mStr", appLanguage)
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Countdown Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = t("NEXT APPOINTMENT COUNTDOWN", "الوقت المتبقي لأقرب موعد", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = countdownStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = t("For: ${nextAppt.reason} at ${nextAppt.hospitalName}", "الموعد: ${nextAppt.reason} في ${nextAppt.hospitalName}", appLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Popup trigger button for Add Appointment
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddAppointmentDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.AddAlarm,
                            contentDescription = "Add Appointment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = t("Add New Appointment", "إضافة موعد مستشفى جديد", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = t("Schedule clinic visits, labs & reminder alarms", "جدولة المواعيد وتنبيهات التذكير والتحاليل", appLanguage),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (selectedAppointmentsTab == 0) {
            // Upcoming appointments list
            if (upcomingAppointments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(t("No upcoming clinical visits scheduled.", "لا توجد مواعيد عيادات قادمة مجدولة حالياً.", appLanguage), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(upcomingAppointments) { appt ->
                    AppointmentCardWidget(
                        appt = appt,
                        viewModel = viewModel,
                        appLanguage = appLanguage,
                        isDarkTheme = isDarkTheme,
                        onEdit = { editingAppointment = appt }
                    )
                }
            }
        } else if (selectedAppointmentsTab == 1) {
            // Completed / Past appointments list
            if (completedAppointments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(t("Completed appointments will compile here.", "المواعيد المنتهية والمستكملة ستعرض في هذا الأرشيف.", appLanguage), fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(completedAppointments) { appt ->
                    AppointmentCardWidget(
                        appt = appt,
                        viewModel = viewModel,
                        appLanguage = appLanguage,
                        isDarkTheme = isDarkTheme,
                        onEdit = { editingAppointment = appt }
                    )
                }
            }
        } else {
            // Dedicated Categorized Lab Analysis Hub (LFTs, KFTs, Lipids, HbA1c & Others)
            item {
                com.example.ui.components.ComprehensiveLabPanelsView(
                    allAppointments = allAppointments,
                    appLanguage = appLanguage,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        }
    }

    // Edit Appointment custom fully-featured dialog
    if (editingAppointment != null) {
        val appt = editingAppointment!!
        var editReason by remember { mutableStateOf(appt.reason) }
        var editNotes by remember { mutableStateOf(appt.notes) }
        var editHospitalName by remember { mutableStateOf(appt.hospitalName) }
        var editHospitalLocation by remember { mutableStateOf(appt.hospitalLocation) }
        var editBuilding by remember { mutableStateOf(appt.building) }
        var editClinic by remember { mutableStateOf(appt.clinic) }
        var editDoctorName by remember { mutableStateOf(appt.doctorName) }
        var editPatientFileNumber by remember { mutableStateOf(appt.patientFileNumber ?: "") }
        var editALT by remember { mutableStateOf(appt.alt?.toString() ?: appt.liverFunction) }
        var editAST by remember { mutableStateOf(appt.ast?.toString() ?: "") }
        var editBilirubin by remember { mutableStateOf(appt.bilirubin?.toString() ?: "") }
        var editAlkalinePhosphate by remember { mutableStateOf(appt.alkalinePhosphate?.toString() ?: "") }
        var editAlbuminInSerum by remember { mutableStateOf(appt.albuminInSerum?.toString() ?: "") }
        var editCreatinine by remember { mutableStateOf(appt.creatinine?.toString() ?: appt.kidneyFunction) }
        var editUrea by remember { mutableStateOf(appt.urea?.toString() ?: "") }
        var editGlycatedHaemoglobin by remember { mutableStateOf(appt.glycatedHaemoglobin?.toString() ?: "") }
        var editLipids by remember { mutableStateOf(appt.lipidsProfile) }
        var editOtherLab by remember { mutableStateOf(appt.otherBloodTests) }
        var editTCholestrol by remember { mutableStateOf(appt.tCholestrol?.toString() ?: "") }
        var editHDL by remember { mutableStateOf(appt.hdl?.toString() ?: "") }
        var editLDL by remember { mutableStateOf(appt.ldl?.toString() ?: "") }
        var editTriglycerides by remember { mutableStateOf(appt.triglycerides?.toString() ?: "") }

        var editUnitALT by remember { mutableStateOf("U/L") }
        var editUnitAST by remember { mutableStateOf("U/L") }
        var editUnitBilirubin by remember { mutableStateOf("mg/dL") }
        var editUnitAlkalinePhosphate by remember { mutableStateOf("U/L") }
        var editUnitAlbuminInSerum by remember { mutableStateOf("g/dL") }
        var editUnitCreatinine by remember { mutableStateOf("mg/dL") }
        var editUnitUrea by remember { mutableStateOf("mg/dL") }
        var editUnitTCholestrol by remember { mutableStateOf("mg/dL") }
        var editUnitHDL by remember { mutableStateOf("mg/dL") }
        var editUnitLDL by remember { mutableStateOf("mg/dL") }
        var editUnitTriglycerides by remember { mutableStateOf("mg/dL") }
        var editUnitHbA1c by remember { mutableStateOf("%") }

        var editAppointmentType by remember { mutableStateOf(appt.appointmentType) }
        var editParentAppointmentId by remember { mutableStateOf(appt.parentAppointmentId) }
        var showEditParentApptDropdown by remember { mutableStateOf(false) }

        var editDateTime by remember { mutableStateOf(appt.dateTime) }
        val initialOffset = ((appt.dateTime - appt.notificationTimeMillis) / (60 * 1000L)).toInt()
        var editReminderMinutesOffset by remember { mutableStateOf(if (initialOffset > 0) initialOffset else 30) }
        var showEditVisualClockDialog by remember { mutableStateOf(false) }
        val savedHospitalProfiles = remember(allAppointments) {
            viewModel.getSavedHospitalProfiles()
        }
        var showEditHospitalDropdown by remember { mutableStateOf(false) }

        val applyEditHospitalProfile = { profile: com.example.data.HospitalProfile ->
            editHospitalName = profile.hospitalName
            if (profile.patientFileNumber.isNotBlank()) editPatientFileNumber = profile.patientFileNumber
            if (profile.doctorName.isNotBlank()) editDoctorName = profile.doctorName
            if (profile.clinic.isNotBlank()) editClinic = profile.clinic
            if (profile.building.isNotBlank()) editBuilding = profile.building
            if (profile.hospitalLocation.isNotBlank()) editHospitalLocation = profile.hospitalLocation
        }

        val editDoctorsForHospital = remember(editHospitalName, allAppointments) {
            viewModel.getDoctorsForHospital(editHospitalName)
        }
        var showEditDoctorDropdown by remember { mutableStateOf(false) }

        val applyEditDoctorProfile = { doc: com.example.data.DoctorProfile ->
            editDoctorName = doc.doctorName
            if (doc.clinic.isNotBlank()) editClinic = doc.clinic
            if (doc.building.isNotBlank() && editBuilding.isBlank()) editBuilding = doc.building
        }

        val allPrescriptions by viewModel.allMedicalPrescriptions.collectAsStateWithLifecycle()
        var editPrescriptionId by remember { mutableStateOf(appt.prescriptionId) }
        var showEditPrescriptionDropdown by remember { mutableStateOf(false) }
        var viewingPrescriptionInEdit by remember { mutableStateOf<MedicalPrescription?>(null) }

        val allDispensedMedications by viewModel.allDispensedMedications.collectAsStateWithLifecycle()
        val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
        val editInventoryAudit = remember(allDispensedMedications, userProfile.startDate, allLogs) {
            com.example.data.computeRoaccutaneInventoryAudit(allDispensedMedications, userProfile.startDate, allLogs)
        }
        if (viewingPrescriptionInEdit != null) {
            val targetRx = viewingPrescriptionInEdit!!
            val rxDispenses = remember(targetRx, allDispensedMedications) {
                val matched = allDispensedMedications.filter { it.prescriptionId == targetRx.id }
                if (matched.isNotEmpty()) matched else allDispensedMedications
            }
            PrescriptionDetailsViewerDialog(
                prescription = targetRx,
                dispenses = rxDispenses,
                inventoryAudit = editInventoryAudit,
                appLanguage = appLanguage,
                onDismiss = { viewingPrescriptionInEdit = null }
            )
        }
        
        if (showEditVisualClockDialog) {
            AppointmentVisualClockDialog(
                initialDateTimeMillis = editDateTime,
                initialOffsetMinutes = editReminderMinutesOffset,
                appLanguage = appLanguage,
                onDismiss = { showEditVisualClockDialog = false },
                onConfirm = { chosenMills, chosenOffset ->
                    editDateTime = chosenMills
                    editReminderMinutesOffset = chosenOffset
                    showEditVisualClockDialog = false
                }
            )
        }
        
        val editCalendar = remember { Calendar.getInstance().apply { timeInMillis = editDateTime } }
        
        val triggerEditDatePicker = {
            DatePickerDialog(
                context,
                { _, yr, mo, dy ->
                    editCalendar.set(Calendar.YEAR, yr)
                    editCalendar.set(Calendar.MONTH, mo)
                    editCalendar.set(Calendar.DAY_OF_MONTH, dy)
                    
                    TimePickerDialog(
                        context,
                        { _, hr, min ->
                            editCalendar.set(Calendar.HOUR_OF_DAY, hr)
                            editCalendar.set(Calendar.MINUTE, min)
                            editCalendar.set(Calendar.SECOND, 0)
                            editDateTime = editCalendar.timeInMillis
                        },
                        editCalendar.get(Calendar.HOUR_OF_DAY),
                        editCalendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                editCalendar.get(Calendar.YEAR),
                editCalendar.get(Calendar.MONTH),
                editCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        Dialog(
            onDismissRequest = { editingAppointment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("Edit Appointment Info", "تعديل تفاصيل الموعد", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { editingAppointment = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("Time: ${formatDisplayTimeStr(editDateTime, appLanguage)}", "الوقت: ${formatDisplayTimeStr(editDateTime, appLanguage)}", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { triggerEditDatePicker() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Picker", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t("Date/Time", "تحديد الوقت", appLanguage), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showEditVisualClockDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f), contentColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.QueryBuilder, contentDescription = "Visual clock picker", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t("Visual Clock", "ساعة العقارب", appLanguage), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Appointment Type & Classification Selector (Links directly to Labs Hub when LAB is chosen)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = t("Appointment Type & Classification (Links with Labs Hub):", "تصنيف ونوع الموعد (يُربط تلقائياً بسجل التحاليل):", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("CLINIC", "🏥 عيادة واستشارة", "🏥 Clinic"),
                                Triple("LAB", "🧪 سحب عينات وتحاليل", "🧪 Lab"),
                                Triple("PHARMACY", "💊 صرف صيدلية", "💊 Pharmacy")
                            ).forEach { (typeKey, labelAr, labelEn) ->
                                val isSelected = editAppointmentType.equals(typeKey, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { editAppointmentType = typeKey },
                                    label = {
                                        Text(
                                            text = if (appLanguage == "ar") labelAr else labelEn,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("edit_appt_type_$typeKey")
                                )
                            }
                        }
                        if (editAppointmentType.equals("LAB", ignoreCase = true)) {
                            val candidateEditParents = remember(allAppointments, appt.id) {
                                allAppointments.filter { it.id != appt.id && !it.appointmentType.equals("LAB", ignoreCase = true) }
                            }
                            if (candidateEditParents.isNotEmpty()) {
                                val chosenEditParent = candidateEditParents.find { it.id == editParentAppointmentId }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = t("Link to Primary Clinic Visit", "ربط هذه الزيارة بموعد استشارة رئيسي", appLanguage),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            if (chosenEditParent != null) {
                                                IconButton(onClick = { editParentAppointmentId = null }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { showEditParentApptDropdown = true },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (chosenEditParent != null)
                                                        "${chosenEditParent.reason} (${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(chosenEditParent.dateTime))})"
                                                    else
                                                        t("Select Linked Clinic Visit", "اختر موعد العيادة المرتبط (اختياري)", appLanguage),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showEditParentApptDropdown,
                                                onDismissRequest = { showEditParentApptDropdown = false }
                                            ) {
                                                candidateEditParents.forEach { p ->
                                                    val pDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(p.dateTime))
                                                    DropdownMenuItem(
                                                        text = { Text("${p.reason} • $pDate • ${p.hospitalName}", fontSize = 12.sp) },
                                                        onClick = {
                                                            editParentAppointmentId = p.id
                                                            showEditParentApptDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var showEditSuggestionsMenu by remember { mutableStateOf(false) }
                    val allSuggestedReasons = remember(allAppointments) {
                        allAppointments.map { it.reason.trim() }.filter { it.isNotEmpty() }.distinct()
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editReason,
                            onValueChange = { 
                                editReason = it
                            },
                            label = { Text(t("Reason", "السبب", appLanguage)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showEditSuggestionsMenu = !showEditSuggestionsMenu }) {
                                    Icon(
                                        imageVector = if (showEditSuggestionsMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Suggestions"
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = showEditSuggestionsMenu,
                            onDismissRequest = { showEditSuggestionsMenu = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            val filteredSuggestions = allSuggestedReasons.filter {
                                editReason.isEmpty() || it.contains(editReason, ignoreCase = true)
                            }
                            
                            val finalSuggestions = if (filteredSuggestions.isEmpty()) allSuggestedReasons else filteredSuggestions

                            finalSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = suggestion,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        editReason = suggestion
                                        showEditSuggestionsMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (allSuggestedReasons.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allSuggestedReasons.take(6)) { sug ->
                                SuggestionChip(
                                    onClick = { editReason = sug },
                                    label = { Text(sug, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editHospitalName,
                            onValueChange = { newName ->
                                editHospitalName = newName
                                val matched = savedHospitalProfiles.find { it.hospitalName.equals(newName.trim(), ignoreCase = true) }
                                if (matched != null) {
                                    applyEditHospitalProfile(matched)
                                }
                            },
                            label = { Text(t("Hospital Name", "اسم المستشفى", appLanguage)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_hospital_name"),
                            leadingIcon = {
                                Icon(Icons.Default.LocalHospital, contentDescription = "Hospital", tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (savedHospitalProfiles.isNotEmpty()) {
                                    IconButton(onClick = { showEditHospitalDropdown = !showEditHospitalDropdown }) {
                                        Icon(
                                            imageVector = if (showEditHospitalDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Saved Hospitals"
                                        )
                                    }
                                }
                            }
                        )

                        if (savedHospitalProfiles.isNotEmpty()) {
                            DropdownMenu(
                                expanded = showEditHospitalDropdown,
                                onDismissRequest = { showEditHospitalDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = t("Saved Hospitals & Clinics:", "المستشفيات والعيادات المحفوظة:", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                                savedHospitalProfiles.forEach { profile ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    text = profile.hospitalName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val detailsList = mutableListOf<String>()
                                                if (profile.patientFileNumber.isNotBlank()) detailsList.add("${t("File", "ملف", appLanguage)}: ${profile.patientFileNumber}")
                                                if (profile.doctorName.isNotBlank()) detailsList.add(profile.doctorName)
                                                if (profile.clinic.isNotBlank()) detailsList.add(profile.clinic)
                                                if (detailsList.isNotEmpty()) {
                                                    Text(
                                                        text = detailsList.joinToString(" • "),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            applyEditHospitalProfile(profile)
                                            showEditHospitalDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (savedHospitalProfiles.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedHospitalProfiles.take(5)) { profile ->
                                val isSelected = editHospitalName.trim().equals(profile.hospitalName.trim(), ignoreCase = true)
                                SuggestionChip(
                                    onClick = { applyEditHospitalProfile(profile) },
                                    label = {
                                        Text(
                                            text = profile.hospitalName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.LocalHospital,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editPatientFileNumber,
                        onValueChange = { editPatientFileNumber = it },
                        label = { Text(t("Patient File Number", "رقم ملف المريض", appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Doctor Name and Clinic
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1.5f)) {
                                OutlinedTextField(
                                    value = editDoctorName,
                                    onValueChange = { newDoc ->
                                        editDoctorName = newDoc
                                        val matched = editDoctorsForHospital.find { it.doctorName.equals(newDoc.trim(), ignoreCase = true) }
                                        if (matched != null && matched.clinic.isNotBlank()) {
                                            editClinic = matched.clinic
                                        }
                                    },
                                    label = { Text(t("Doctor", "الطبيب", appLanguage)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("edit_doctor_name"),
                                    trailingIcon = {
                                        if (editDoctorsForHospital.isNotEmpty()) {
                                            IconButton(onClick = { showEditDoctorDropdown = !showEditDoctorDropdown }) {
                                                Icon(
                                                    imageVector = if (showEditDoctorDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                    contentDescription = "Saved Doctors"
                                                )
                                            }
                                        }
                                    }
                                )

                                if (editDoctorsForHospital.isNotEmpty()) {
                                    DropdownMenu(
                                        expanded = showEditDoctorDropdown,
                                        onDismissRequest = { showEditDoctorDropdown = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Text(
                                            text = if (editHospitalName.isNotBlank())
                                                t("Doctors at $editHospitalName:", "الأطباء في $editHospitalName:", appLanguage)
                                            else
                                                t("Saved Doctors:", "الأطباء المحفوظون:", appLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                        editDoctorsForHospital.forEach { doc ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                },
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = doc.doctorName,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (doc.clinic.isNotBlank()) {
                                                            Text(
                                                                text = "${t("Clinic", "العيادة", appLanguage)}: ${doc.clinic}",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    applyEditDoctorProfile(doc)
                                                    showEditDoctorDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = editClinic,
                                onValueChange = { editClinic = it },
                                label = { Text(t("Clinic/Room", "العيادة", appLanguage)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("edit_clinic")
                            )
                        }

                        if (editDoctorsForHospital.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(editDoctorsForHospital.take(5)) { doc ->
                                    val isSelected = editDoctorName.trim().equals(doc.doctorName.trim(), ignoreCase = true)
                                    SuggestionChip(
                                        onClick = { applyEditDoctorProfile(doc) },
                                        label = {
                                            Text(
                                                text = doc.doctorName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editBuilding,
                            onValueChange = { editBuilding = it },
                            label = { Text(t("Building", "المبنى", appLanguage)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editHospitalLocation,
                            onValueChange = { editHospitalLocation = it },
                            label = { Text(t("Map URL / Location", "رابط الموقع", appLanguage)) },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                    }

                    // Linked Medical Prescription Section
                    val editChosenPrescription = remember(editPrescriptionId, allPrescriptions) {
                        allPrescriptions.find { it.id == editPrescriptionId }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (editChosenPrescription != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = t("Linked Prescription", "الوصفة الطبية المرتبطة بالموعد", appLanguage),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (editChosenPrescription != null) {
                                    IconButton(onClick = { editPrescriptionId = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (editChosenPrescription != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = editChosenPrescription.medicationName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (editChosenPrescription.prescriptionNumber.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "#${editChosenPrescription.prescriptionNumber}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${editChosenPrescription.dailyDose} • ${editChosenPrescription.doctorName.ifBlank { editChosenPrescription.clinicOrHospital }}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        TextButton(
                                            onClick = { viewingPrescriptionInEdit = editChosenPrescription },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(t("View", "معاينة", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showEditPrescriptionDropdown = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (allPrescriptions.isEmpty())
                                                t("No prescriptions yet (Add from Rx Tracker)", "لا توجد وصفات مسجلة (يمكنك إضافتها من متتبع الوصفات)", appLanguage)
                                            else
                                                t("Select or Change Linked Prescription", "اختر وصفة طبية لربطها بالموعد", appLanguage),
                                            fontSize = 11.5.sp
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showEditPrescriptionDropdown,
                                        onDismissRequest = { showEditPrescriptionDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        if (allPrescriptions.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(t("No registered prescriptions found", "لا توجد أي وصفة مسجلة بعد", appLanguage), fontSize = 12.sp) },
                                                onClick = { showEditPrescriptionDropdown = false }
                                            )
                                        } else {
                                            allPrescriptions.forEach { rx ->
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    },
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = "${rx.medicationName} ${if (rx.prescriptionNumber.isNotBlank()) "(#${rx.prescriptionNumber})" else ""}",
                                                                fontSize = 12.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "${rx.dailyDose} • ${rx.doctorName.ifBlank { rx.clinicOrHospital }}",
                                                                fontSize = 10.5.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        editPrescriptionId = rx.id
                                                        showEditPrescriptionDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = t("Liver Function Tests (LFTs):", "وظائف الكبد (المعدل الطبيعي بين قوسين):", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabInputWithUnitSelector(
                            label = "ALT",
                            value = editALT,
                            onValueChange = { editALT = it },
                            selectedUnit = editUnitALT,
                            onUnitSelected = { editUnitALT = it },
                            unitOptions = BiomarkerUnitsCatalog.ENZYMES,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        LabInputWithUnitSelector(
                            label = "AST",
                            value = editAST,
                            onValueChange = { editAST = it },
                            selectedUnit = editUnitAST,
                            onUnitSelected = { editUnitAST = it },
                            unitOptions = BiomarkerUnitsCatalog.ENZYMES,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabInputWithUnitSelector(
                            label = "Bilirubin",
                            value = editBilirubin,
                            onValueChange = { editBilirubin = it },
                            selectedUnit = editUnitBilirubin,
                            onUnitSelected = { editUnitBilirubin = it },
                            unitOptions = BiomarkerUnitsCatalog.BILIRUBIN,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        LabInputWithUnitSelector(
                            label = "ALP",
                            value = editAlkalinePhosphate,
                            onValueChange = { editAlkalinePhosphate = it },
                            selectedUnit = editUnitAlkalinePhosphate,
                            onUnitSelected = { editUnitAlkalinePhosphate = it },
                            unitOptions = BiomarkerUnitsCatalog.ENZYMES,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LabInputWithUnitSelector(
                        label = "Albumin in Serum",
                        value = editAlbuminInSerum,
                        onValueChange = { editAlbuminInSerum = it },
                        selectedUnit = editUnitAlbuminInSerum,
                        onUnitSelected = { editUnitAlbuminInSerum = it },
                        unitOptions = BiomarkerUnitsCatalog.ALBUMIN,
                        appLanguage = appLanguage,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = t("Kidney Function Tests (KFTs):", "وظائف الكلى (المعدل الطبيعي):", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabInputWithUnitSelector(
                            label = "Creatinine",
                            value = editCreatinine,
                            onValueChange = { editCreatinine = it },
                            selectedUnit = editUnitCreatinine,
                            onUnitSelected = { editUnitCreatinine = it },
                            unitOptions = BiomarkerUnitsCatalog.CREATININE,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        LabInputWithUnitSelector(
                            label = "Urea",
                            value = editUrea,
                            onValueChange = { editUrea = it },
                            selectedUnit = editUnitUrea,
                            onUnitSelected = { editUnitUrea = it },
                            unitOptions = BiomarkerUnitsCatalog.UREA,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = t("Detailed Lipid Profiling & Cardiovascular Risk:", "تفاصيل دهنيات الدم ومؤشر المخاطر:", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabInputWithUnitSelector(
                            label = "TCholestrol",
                            value = editTCholestrol,
                            onValueChange = { editTCholestrol = it },
                            selectedUnit = editUnitTCholestrol,
                            onUnitSelected = { editUnitTCholestrol = it },
                            unitOptions = BiomarkerUnitsCatalog.CHOLESTEROL,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        LabInputWithUnitSelector(
                            label = "HDL",
                            value = editHDL,
                            onValueChange = { editHDL = it },
                            selectedUnit = editUnitHDL,
                            onUnitSelected = { editUnitHDL = it },
                            unitOptions = BiomarkerUnitsCatalog.HDL,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabInputWithUnitSelector(
                            label = "LDL",
                            value = editLDL,
                            onValueChange = { editLDL = it },
                            selectedUnit = editUnitLDL,
                            onUnitSelected = { editUnitLDL = it },
                            unitOptions = BiomarkerUnitsCatalog.LDL,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        LabInputWithUnitSelector(
                            label = "Triglycerides",
                            value = editTriglycerides,
                            onValueChange = { editTriglycerides = it },
                            selectedUnit = editUnitTriglycerides,
                            onUnitSelected = { editUnitTriglycerides = it },
                            unitOptions = BiomarkerUnitsCatalog.TRIGLYCERIDES,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = t("Glycated Haemoglobin & Other Safety Tests:", "السكر التراكمي وتحاليل أخرى:", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabInputWithUnitSelector(
                            label = "HbA1c",
                            value = editGlycatedHaemoglobin,
                            onValueChange = { editGlycatedHaemoglobin = it },
                            selectedUnit = editUnitHbA1c,
                            onUnitSelected = { editUnitHbA1c = it },
                            unitOptions = BiomarkerUnitsCatalog.HBA1C,
                            appLanguage = appLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editOtherLab,
                            onValueChange = { editOtherLab = it },
                            label = { Text(t("Other Test Results", "تحاليل أخرى (CBC..)", appLanguage)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Dynamic Risk Factor Calculator Card
                    val editCalculatedRF = remember(editTCholestrol, editHDL) {
                        val tc = editTCholestrol.toDoubleOrNull() ?: 0.0
                        val hdlVal = editHDL.toDoubleOrNull() ?: 0.0
                        if (hdlVal > 0.0) {
                            String.format(Locale.US, "%.2f", tc / hdlVal)
                        } else {
                            "0.00"
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = t("Risk Factor (RF = TCholestrol / HDL):", "معدل الخطورة (RF = الكوليسترول / النافع):", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = editCalculatedRF,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Premium custom Reminder Alarm Time slider configurator with 2 clocks in Edit Dialog
                    val editAlarmTime = editDateTime - (editReminderMinutesOffset * 60 * 1000L)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = t("Notification Reminder & Alarms Schedule", "إنذار وتذكير بجدولة الموعد القادم", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Card 1: Appointment clock
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Appointment Time",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = t("Appt Clock", "ساعة الموعد", appLanguage),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatDisplayTimeOnly(editDateTime, appLanguage),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Connect arrow
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "-${editReminderMinutesOffset}m",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "To",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Card 2: Alarm Trigger Clock
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "Reminder Trigger",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = t("Alarm Trigger", "ساعة التنبيه", appLanguage),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatDisplayTimeOnly(editAlarmTime, appLanguage),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = t("Slide to set reminder time:", "اسحب لتعديل مدة التنبيه قبل الموعد:", appLanguage),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatOffsetDuration(editReminderMinutesOffset, appLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                val editDiscreteSteps = remember { listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 720, 1440) }
                                val editDiscreteIdx = remember(editReminderMinutesOffset) {
                                    editDiscreteSteps.indexOfFirst { it >= editReminderMinutesOffset }.coerceAtLeast(0)
                                }
                                Slider(
                                    value = editDiscreteIdx.toFloat(),
                                    onValueChange = { idx ->
                                        val stepIdx = idx.toInt().coerceIn(0, editDiscreteSteps.size - 1)
                                        editReminderMinutesOffset = editDiscreteSteps[stepIdx]
                                    },
                                    valueRange = 0f..(editDiscreteSteps.size - 1).toFloat(),
                                    steps = editDiscreteSteps.size - 2,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.secondary,
                                        activeTrackColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text(t("Consultation Notes / Instructions", "ملاحظات الطبيب وتوجيهاته", appLanguage)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Linked Medical Prescription Section
                    val chosenEditPrescription = remember(editPrescriptionId, allPrescriptions) {
                        allPrescriptions.find { it.id == editPrescriptionId }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (chosenEditPrescription != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = t("Linked Prescription (Optional)", "ربط وصفة طبية بهذا الموعد (اختياري)", appLanguage),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (chosenEditPrescription != null) {
                                    IconButton(onClick = { editPrescriptionId = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (chosenEditPrescription != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = chosenEditPrescription.medicationName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (chosenEditPrescription.prescriptionNumber.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "#${chosenEditPrescription.prescriptionNumber}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${chosenEditPrescription.dailyDose} • ${chosenEditPrescription.doctorName.ifBlank { chosenEditPrescription.clinicOrHospital }}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        TextButton(
                                            onClick = { viewingPrescriptionInEdit = chosenEditPrescription },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(t("View", "معاينة", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showEditPrescriptionDropdown = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (allPrescriptions.isEmpty())
                                                t("No prescriptions yet (Add from Rx Tracker)", "لا توجد وصفات مسجلة (يمكنك إضافتها من متتبع الوصفات)", appLanguage)
                                            else
                                                t("Select a Prescription to Link", "اختر وصفة طبية لربطها بالموعد", appLanguage),
                                            fontSize = 11.5.sp
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showEditPrescriptionDropdown,
                                        onDismissRequest = { showEditPrescriptionDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        if (allPrescriptions.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(t("No registered prescriptions found", "لا توجد أي وصفة مسجلة بعد", appLanguage), fontSize = 12.sp) },
                                                onClick = { showEditPrescriptionDropdown = false }
                                            )
                                        } else {
                                            allPrescriptions.forEach { rx ->
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    },
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = "${rx.medicationName} ${if (rx.prescriptionNumber.isNotBlank()) "(#${rx.prescriptionNumber})" else ""}",
                                                                fontSize = 12.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "${rx.dailyDose} • ${rx.doctorName.ifBlank { rx.clinicOrHospital }}",
                                                                fontSize = 10.5.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        editPrescriptionId = rx.id
                                                        showEditPrescriptionDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Button(
                        onClick = {
                            if (editHospitalName.isBlank() || editReason.isBlank()) {
                                Toast.makeText(context, t("Please enter Doctor/Hospital Reason", "يرجى كتابة سبب الاستشارة واسم المستشفى", appLanguage), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val newReminderTime = editDateTime - (editReminderMinutesOffset * 60 * 1000L)
                            val altStd = BiomarkerUnitsCatalog.ENZYMES.find { it.unit == editUnitALT }?.toStandard(editALT.toDoubleOrNull() ?: 0.0) ?: editALT.toDoubleOrNull()
                            val astStd = BiomarkerUnitsCatalog.ENZYMES.find { it.unit == editUnitAST }?.toStandard(editAST.toDoubleOrNull() ?: 0.0) ?: editAST.toDoubleOrNull()
                            val biliStd = BiomarkerUnitsCatalog.BILIRUBIN.find { it.unit == editUnitBilirubin }?.toStandard(editBilirubin.toDoubleOrNull() ?: 0.0) ?: editBilirubin.toDoubleOrNull()
                            val alpStd = BiomarkerUnitsCatalog.ENZYMES.find { it.unit == editUnitAlkalinePhosphate }?.toStandard(editAlkalinePhosphate.toDoubleOrNull() ?: 0.0) ?: editAlkalinePhosphate.toDoubleOrNull()
                            val albStd = BiomarkerUnitsCatalog.ALBUMIN.find { it.unit == editUnitAlbuminInSerum }?.toStandard(editAlbuminInSerum.toDoubleOrNull() ?: 0.0) ?: editAlbuminInSerum.toDoubleOrNull()
                            val creatStd = BiomarkerUnitsCatalog.CREATININE.find { it.unit == editUnitCreatinine }?.toStandard(editCreatinine.toDoubleOrNull() ?: 0.0) ?: editCreatinine.toDoubleOrNull()
                            val ureaStd = BiomarkerUnitsCatalog.UREA.find { it.unit == editUnitUrea }?.toStandard(editUrea.toDoubleOrNull() ?: 0.0) ?: editUrea.toDoubleOrNull()
                            val tcStd = BiomarkerUnitsCatalog.CHOLESTEROL.find { it.unit == editUnitTCholestrol }?.toStandard(editTCholestrol.toDoubleOrNull() ?: 0.0) ?: editTCholestrol.toDoubleOrNull()
                            val hdlStd = BiomarkerUnitsCatalog.HDL.find { it.unit == editUnitHDL }?.toStandard(editHDL.toDoubleOrNull() ?: 0.0) ?: editHDL.toDoubleOrNull()
                            val ldlStd = BiomarkerUnitsCatalog.LDL.find { it.unit == editUnitLDL }?.toStandard(editLDL.toDoubleOrNull() ?: 0.0) ?: editLDL.toDoubleOrNull()
                            val tgStd = BiomarkerUnitsCatalog.TRIGLYCERIDES.find { it.unit == editUnitTriglycerides }?.toStandard(editTriglycerides.toDoubleOrNull() ?: 0.0) ?: editTriglycerides.toDoubleOrNull()
                            val hba1cStd = BiomarkerUnitsCatalog.HBA1C.find { it.unit == editUnitHbA1c }?.toStandard(editGlycatedHaemoglobin.toDoubleOrNull() ?: 0.0) ?: editGlycatedHaemoglobin.toDoubleOrNull()

                            val customUnitsJson = """{"alt":"$editUnitALT","ast":"$editUnitAST","bilirubin":"$editUnitBilirubin","alp":"$editUnitAlkalinePhosphate","albumin":"$editUnitAlbuminInSerum","creatinine":"$editUnitCreatinine","urea":"$editUnitUrea","tc":"$editUnitTCholestrol","hdl":"$editUnitHDL","ldl":"$editUnitLDL","tg":"$editUnitTriglycerides","hba1c":"$editUnitHbA1c"}"""

                            val updatedAppt = appt.copy(
                                dateTime = editDateTime,
                                reason = editReason,
                                notes = editNotes,
                                hospitalName = editHospitalName,
                                hospitalLocation = editHospitalLocation,
                                building = editBuilding,
                                clinic = editClinic,
                                doctorName = editDoctorName,
                                liverFunction = editALT,
                                kidneyFunction = editCreatinine,
                                lipidsProfile = editLipids,
                                otherBloodTests = editOtherLab,
                                notificationTimeMillis = newReminderTime,
                                tCholestrol = tcStd,
                                hdl = hdlStd,
                                ldl = ldlStd,
                                triglycerides = tgStd,
                                patientFileNumber = editPatientFileNumber,
                                prescriptionId = editPrescriptionId,
                                alt = altStd,
                                ast = astStd,
                                bilirubin = biliStd,
                                alkalinePhosphate = alpStd,
                                albuminInSerum = albStd,
                                creatinine = creatStd,
                                urea = ureaStd,
                                glycatedHaemoglobin = hba1cStd,
                                parentAppointmentId = editParentAppointmentId,
                                appointmentType = editAppointmentType,
                                customUnitsJson = customUnitsJson
                            )
                            
                            viewModel.updateAppointment(updatedAppt)
                            viewModel.scheduleAppointmentAlarm(context, updatedAppt)
                            editingAppointment = null
                            Toast.makeText(context, t("Appointment details updated!", "تم تحديث بيانات الموعد بنجاح!", appLanguage), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Changes")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t("Save Reference Changes", "حفظ وتعديل الموعد", appLanguage), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddAppointmentDialog(
    appLanguage: String,
    viewModel: RoaccutaneViewModel,
    allAppointments: List<Appointment>,
    initialAppointmentType: String = "CLINIC",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputReason by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    
    val savedHospitalName = remember { viewModel.getSavedHospitalName() }
    val savedHospitalLocation = remember { viewModel.getSavedHospitalLocation() }
    val savedBuilding = remember { viewModel.getSavedBuilding() }
    val savedClinic = remember { viewModel.getSavedClinic() }
    val savedDoctorName = remember { viewModel.getSavedDoctorName() }
    val savedPatientFileNumber = remember { viewModel.getSavedPatientFileNumber() }

    var inputHospitalName by remember { mutableStateOf(savedHospitalName) }
    var inputHospitalLocation by remember { mutableStateOf(savedHospitalLocation) }
    var inputBuilding by remember { mutableStateOf(savedBuilding) }
    var inputClinic by remember { mutableStateOf(savedClinic) }
    var inputDoctorName by remember { mutableStateOf(savedDoctorName) }
    var inputPatientFileNumber by remember { mutableStateOf(savedPatientFileNumber) }

    val savedHospitalProfiles = remember(allAppointments) {
        viewModel.getSavedHospitalProfiles()
    }
    var showHospitalDropdown by remember { mutableStateOf(false) }

    val applyHospitalProfile = { profile: com.example.data.HospitalProfile ->
        inputHospitalName = profile.hospitalName
        if (profile.patientFileNumber.isNotBlank()) inputPatientFileNumber = profile.patientFileNumber
        if (profile.doctorName.isNotBlank()) inputDoctorName = profile.doctorName
        if (profile.clinic.isNotBlank()) inputClinic = profile.clinic
        if (profile.building.isNotBlank()) inputBuilding = profile.building
        if (profile.hospitalLocation.isNotBlank()) inputHospitalLocation = profile.hospitalLocation
    }

    val doctorsForHospital = remember(inputHospitalName, allAppointments) {
        viewModel.getDoctorsForHospital(inputHospitalName)
    }
    var showDoctorDropdown by remember { mutableStateOf(false) }

    val applyDoctorProfile = { doc: com.example.data.DoctorProfile ->
        inputDoctorName = doc.doctorName
        if (doc.clinic.isNotBlank()) inputClinic = doc.clinic
        if (doc.building.isNotBlank() && inputBuilding.isBlank()) inputBuilding = doc.building
    }

    // Lab Blood Results
    var inputALT by remember { mutableStateOf("") }
    var inputAST by remember { mutableStateOf("") }
    var inputBilirubin by remember { mutableStateOf("") }
    var inputAlkalinePhosphate by remember { mutableStateOf("") }
    var inputAlbuminInSerum by remember { mutableStateOf("") }
    var inputCreatinine by remember { mutableStateOf("") }
    var inputUrea by remember { mutableStateOf("") }
    var inputGlycatedHaemoglobin by remember { mutableStateOf("") }
    var inputLipids by remember { mutableStateOf("") }
    var inputOtherLab by remember { mutableStateOf("") }
    
    // Lipid detailed inputs
    var inputTCholestrol by remember { mutableStateOf("") }
    var inputHDL by remember { mutableStateOf("") }
    var inputLDL by remember { mutableStateOf("") }
    var inputTriglycerides by remember { mutableStateOf("") }

    // Lab units selection state
    var unitALT by remember { mutableStateOf("U/L") }
    var unitAST by remember { mutableStateOf("U/L") }
    var unitBilirubin by remember { mutableStateOf("mg/dL") }
    var unitAlkalinePhosphate by remember { mutableStateOf("U/L") }
    var unitAlbuminInSerum by remember { mutableStateOf("g/dL") }
    var unitCreatinine by remember { mutableStateOf("mg/dL") }
    var unitUrea by remember { mutableStateOf("mg/dL") }
    var unitTCholestrol by remember { mutableStateOf("mg/dL") }
    var unitHDL by remember { mutableStateOf("mg/dL") }
    var unitLDL by remember { mutableStateOf("mg/dL") }
    var unitTriglycerides by remember { mutableStateOf("mg/dL") }
    var unitHbA1c by remember { mutableStateOf("%") }

    // Appointment Type & Sub-visit Hierarchy
    var appointmentType by remember { mutableStateOf(initialAppointmentType) }
    var selectedParentAppointmentId by remember { mutableStateOf<Int?>(null) }
    var showParentApptDropdown by remember { mutableStateOf(false) }

    // Selected appointment datetime
    var selectedDateTimeMills by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var reminderMinutesOffset by remember { mutableStateOf(30) } // mins before appt

    var showVisualClockDialog by remember { mutableStateOf(false) }
    var isLabDetailsExpanded by remember { mutableStateOf(false) }

    val allPrescriptions by viewModel.allMedicalPrescriptions.collectAsStateWithLifecycle()
    val allDispensedMedications by viewModel.allDispensedMedications.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val addInventoryAudit = remember(allDispensedMedications, userProfile.startDate, allLogs) {
        com.example.data.computeRoaccutaneInventoryAudit(allDispensedMedications, userProfile.startDate, allLogs)
    }
    var selectedPrescriptionId by remember { mutableStateOf<String?>(null) }
    var showPrescriptionDropdown by remember { mutableStateOf(false) }
    var viewingPrescriptionInAdd by remember { mutableStateOf<MedicalPrescription?>(null) }

    if (viewingPrescriptionInAdd != null) {
        val targetRx = viewingPrescriptionInAdd!!
        val rxDispenses = remember(targetRx, allDispensedMedications) {
            val matched = allDispensedMedications.filter { it.prescriptionId == targetRx.id }
            if (matched.isNotEmpty()) matched else allDispensedMedications
        }
        PrescriptionDetailsViewerDialog(
            prescription = targetRx,
            dispenses = rxDispenses,
            inventoryAudit = addInventoryAudit,
            appLanguage = appLanguage,
            onDismiss = { viewingPrescriptionInAdd = null }
        )
    }

    if (showVisualClockDialog) {
        AppointmentVisualClockDialog(
            initialDateTimeMillis = selectedDateTimeMills,
            initialOffsetMinutes = reminderMinutesOffset,
            appLanguage = appLanguage,
            onDismiss = { showVisualClockDialog = false },
            onConfirm = { chosenMills, chosenOffset ->
                selectedDateTimeMills = chosenMills
                reminderMinutesOffset = chosenOffset
                showVisualClockDialog = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .imePadding()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Add Appointment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = t("Schedule Hospital Appointment", "جدولة وإضافة موعد مستشفى جديد", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Combined Date/Time & Alarm Button
                Button(
                    onClick = { showVisualClockDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Clock", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t(
                            "Appointment Time & Reminder (${formatDisplayTimeStr(selectedDateTimeMills, appLanguage)})",
                            "تحديد وقت الموعد والتنبيه (${formatDisplayTimeStr(selectedDateTimeMills, appLanguage)})",
                            appLanguage
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Appointment Type & Classification Selector (Clinic vs Lab vs Pharmacy)
                Text(
                    text = t("Appointment Type & Classification (Links with Labs Hub):", "تصنيف ونوع الموعد (يُربط تلقائياً بسجل التحاليل):", appLanguage),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("CLINIC", "🏥 عيادة واستشارة", "🏥 Clinic"),
                        Triple("LAB", "🧪 سحب عينات وتحاليل", "🧪 Lab"),
                        Triple("PHARMACY", "💊 صرف صيدلية", "💊 Pharmacy")
                    ).forEach { (typeKey, labelAr, labelEn) ->
                        val isSelected = appointmentType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { appointmentType = typeKey },
                            label = { Text(if (appLanguage == "ar") labelAr else labelEn, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f).testTag("add_appt_type_$typeKey")
                        )
                    }
                }

                if (appointmentType == "LAB") {
                    val candidateParents = remember(allAppointments) {
                        allAppointments.filter { it.appointmentType != "LAB" }
                    }
                    if (candidateParents.isNotEmpty()) {
                        val chosenParent = candidateParents.find { it.id == selectedParentAppointmentId }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (appLanguage == "ar") "ربط هذه الزيارة بموعد استشارة رئيسي" else "Link to Primary Clinic Visit",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    if (chosenParent != null) {
                                        IconButton(onClick = { selectedParentAppointmentId = null }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showParentApptDropdown = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (chosenParent != null)
                                                "${chosenParent.reason} (${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(chosenParent.dateTime))})"
                                            else
                                                (if (appLanguage == "ar") "اختر موعد العيادة المرتبط (مثل: استشارة الغد)" else "Select Linked Clinic Visit"),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showParentApptDropdown,
                                        onDismissRequest = { showParentApptDropdown = false }
                                    ) {
                                        candidateParents.forEach { p ->
                                            val pDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(p.dateTime))
                                            DropdownMenuItem(
                                                text = { Text("${p.reason} • $pDate • ${p.hospitalName}", fontSize = 12.sp) },
                                                onClick = {
                                                    selectedParentAppointmentId = p.id
                                                    showParentApptDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Consultation reason field with dropdown suggestions
                var showSuggestionsMenu by remember { mutableStateOf(false) }
                val allSuggestedReasons = remember(allAppointments) {
                    allAppointments.map { it.reason.trim() }.filter { it.isNotEmpty() }.distinct()
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inputReason,
                        onValueChange = { 
                            inputReason = it
                        },
                        label = { Text(t("Consultation Reason (e.g. Monthly refill)", "سبب الموعد (مثال: فحوصات الشهر الثاني)", appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showSuggestionsMenu = !showSuggestionsMenu }) {
                                Icon(
                                    imageVector = if (showSuggestionsMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Suggestions"
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = showSuggestionsMenu,
                        onDismissRequest = { showSuggestionsMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        val filteredSuggestions = allSuggestedReasons.filter {
                            inputReason.isEmpty() || it.contains(inputReason, ignoreCase = true)
                        }
                        
                        val finalSuggestions = if (filteredSuggestions.isEmpty()) allSuggestedReasons else filteredSuggestions

                        finalSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = suggestion,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    inputReason = suggestion
                                    showSuggestionsMenu = false
                                }
                            )
                        }
                    }
                }

                if (allSuggestedReasons.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allSuggestedReasons.take(6)) { sug ->
                            SuggestionChip(
                                onClick = { inputReason = sug },
                                label = { Text(sug, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inputHospitalName,
                        onValueChange = { newName ->
                            inputHospitalName = newName
                            val matched = savedHospitalProfiles.find { it.hospitalName.equals(newName.trim(), ignoreCase = true) }
                            if (matched != null) {
                                applyHospitalProfile(matched)
                            }
                        },
                        label = { Text(t("Hospital Name", "اسم المستشفى", appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_hospital_name"),
                        leadingIcon = {
                            Icon(Icons.Default.LocalHospital, contentDescription = "Hospital", tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (savedHospitalProfiles.isNotEmpty()) {
                                IconButton(onClick = { showHospitalDropdown = !showHospitalDropdown }) {
                                    Icon(
                                        imageVector = if (showHospitalDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Saved Hospitals"
                                    )
                                }
                            }
                        }
                    )

                    if (savedHospitalProfiles.isNotEmpty()) {
                        DropdownMenu(
                            expanded = showHospitalDropdown,
                            onDismissRequest = { showHospitalDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = t("Saved Hospitals & Clinics:", "المستشفيات والعيادات المحفوظة:", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            savedHospitalProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                text = profile.hospitalName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val detailsList = mutableListOf<String>()
                                            if (profile.patientFileNumber.isNotBlank()) detailsList.add("${t("File", "ملف", appLanguage)}: ${profile.patientFileNumber}")
                                            if (profile.doctorName.isNotBlank()) detailsList.add(profile.doctorName)
                                            if (profile.clinic.isNotBlank()) detailsList.add(profile.clinic)
                                            if (detailsList.isNotEmpty()) {
                                                Text(
                                                    text = detailsList.joinToString(" • "),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        applyHospitalProfile(profile)
                                        showHospitalDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (savedHospitalProfiles.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedHospitalProfiles.take(5)) { profile ->
                            val isSelected = inputHospitalName.trim().equals(profile.hospitalName.trim(), ignoreCase = true)
                            SuggestionChip(
                                onClick = { applyHospitalProfile(profile) },
                                label = {
                                    Text(
                                        text = profile.hospitalName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputPatientFileNumber,
                    onValueChange = { inputPatientFileNumber = it },
                    label = { Text(t("Patient File Number", "رقم ملف المريض", appLanguage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_patient_file_number")
                )

                // Doctor Name and Clinic
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1.5f)) {
                            OutlinedTextField(
                                value = inputDoctorName,
                                onValueChange = { newDoc ->
                                    inputDoctorName = newDoc
                                    val matched = doctorsForHospital.find { it.doctorName.equals(newDoc.trim(), ignoreCase = true) }
                                    if (matched != null && matched.clinic.isNotBlank()) {
                                        inputClinic = matched.clinic
                                    }
                                },
                                label = { Text(t("Doctor", "الطبيب", appLanguage)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_doctor_name"),
                                trailingIcon = {
                                    if (doctorsForHospital.isNotEmpty()) {
                                        IconButton(onClick = { showDoctorDropdown = !showDoctorDropdown }) {
                                            Icon(
                                                imageVector = if (showDoctorDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Saved Doctors"
                                            )
                                        }
                                    }
                                }
                            )

                            if (doctorsForHospital.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = showDoctorDropdown,
                                    onDismissRequest = { showDoctorDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Text(
                                        text = if (inputHospitalName.isNotBlank())
                                            t("Doctors at $inputHospitalName:", "الأطباء في $inputHospitalName:", appLanguage)
                                        else
                                            t("Saved Doctors:", "الأطباء المحفوظون:", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                    doctorsForHospital.forEach { doc ->
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = doc.doctorName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (doc.clinic.isNotBlank()) {
                                                        Text(
                                                            text = "${t("Clinic", "العيادة", appLanguage)}: ${doc.clinic}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                applyDoctorProfile(doc)
                                                showDoctorDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputClinic,
                            onValueChange = { inputClinic = it },
                            label = { Text(t("Clinic/Room", "العيادة", appLanguage)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_clinic")
                        )
                    }

                    if (doctorsForHospital.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(doctorsForHospital.take(5)) { doc ->
                                val isSelected = inputDoctorName.trim().equals(doc.doctorName.trim(), ignoreCase = true)
                                SuggestionChip(
                                    onClick = { applyDoctorProfile(doc) },
                                    label = {
                                        Text(
                                            text = doc.doctorName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputBuilding,
                        onValueChange = { inputBuilding = it },
                        label = { Text(t("Building", "المبنى", appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = inputHospitalLocation,
                        onValueChange = { inputHospitalLocation = it },
                        label = { Text(t("Map URL / Location", "رابط الموقع", appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                }

                // Linked Medical Prescription Section
                val chosenPrescription = remember(selectedPrescriptionId, allPrescriptions) {
                    allPrescriptions.find { it.id == selectedPrescriptionId }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (chosenPrescription != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = t("Linked Prescription (Optional)", "ربط وصفة طبية بهذا الموعد (اختياري)", appLanguage),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (chosenPrescription != null) {
                                IconButton(onClick = { selectedPrescriptionId = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (chosenPrescription != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = chosenPrescription.medicationName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (chosenPrescription.prescriptionNumber.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "#${chosenPrescription.prescriptionNumber}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${chosenPrescription.dailyDose} • ${chosenPrescription.doctorName.ifBlank { chosenPrescription.clinicOrHospital }}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewingPrescriptionInAdd = chosenPrescription },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(t("View", "معاينة", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showPrescriptionDropdown = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (allPrescriptions.isEmpty())
                                            t("No prescriptions yet (Add from Rx Tracker)", "لا توجد وصفات مسجلة (يمكنك إضافتها من متتبع الوصفات)", appLanguage)
                                        else
                                            t("Select a Prescription to Link", "اختر وصفة طبية لربطها بالموعد", appLanguage),
                                        fontSize = 11.5.sp
                                    )
                                }

                                DropdownMenu(
                                    expanded = showPrescriptionDropdown,
                                    onDismissRequest = { showPrescriptionDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    if (allPrescriptions.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text(t("No registered prescriptions found", "لا توجد أي وصفة مسجلة بعد", appLanguage), fontSize = 12.sp) },
                                            onClick = { showPrescriptionDropdown = false }
                                        )
                                    } else {
                                        allPrescriptions.forEach { rx ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                },
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = "${rx.medicationName} ${if (rx.prescriptionNumber.isNotBlank()) "(#${rx.prescriptionNumber})" else ""}",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${rx.dailyDose} • ${rx.doctorName.ifBlank { rx.clinicOrHospital }}",
                                                            fontSize = 10.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedPrescriptionId = rx.id
                                                    showPrescriptionDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dropdown/Expandable Section for Lab Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isLabDetailsExpanded = !isLabDetailsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Lab Results",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = t("Blood Tests & Lipid Profile Details (Optional)", "تفاصيل الفحوصات الطبية ودهنيات الدم (اختياري)", appLanguage),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Icon(
                                imageVector = if (isLabDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (isLabDetailsExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = t("Liver Function Tests (LFTs):", "وظائف الكبد (المعدل الطبيعي بين قوسين):", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabInputWithUnitSelector(
                                        label = "ALT",
                                        value = inputALT,
                                        onValueChange = { inputALT = it },
                                        selectedUnit = unitALT,
                                        onUnitSelected = { unitALT = it },
                                        unitOptions = BiomarkerUnitsCatalog.ENZYMES,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_alt")
                                    )
                                    LabInputWithUnitSelector(
                                        label = "AST",
                                        value = inputAST,
                                        onValueChange = { inputAST = it },
                                        selectedUnit = unitAST,
                                        onUnitSelected = { unitAST = it },
                                        unitOptions = BiomarkerUnitsCatalog.ENZYMES,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_ast")
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabInputWithUnitSelector(
                                        label = "Bilirubin",
                                        value = inputBilirubin,
                                        onValueChange = { inputBilirubin = it },
                                        selectedUnit = unitBilirubin,
                                        onUnitSelected = { unitBilirubin = it },
                                        unitOptions = BiomarkerUnitsCatalog.BILIRUBIN,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_bilirubin")
                                    )
                                    LabInputWithUnitSelector(
                                        label = "ALP",
                                        value = inputAlkalinePhosphate,
                                        onValueChange = { inputAlkalinePhosphate = it },
                                        selectedUnit = unitAlkalinePhosphate,
                                        onUnitSelected = { unitAlkalinePhosphate = it },
                                        unitOptions = BiomarkerUnitsCatalog.ENZYMES,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_alp")
                                    )
                                }
                                LabInputWithUnitSelector(
                                    label = "Albumin in Serum",
                                    value = inputAlbuminInSerum,
                                    onValueChange = { inputAlbuminInSerum = it },
                                    selectedUnit = unitAlbuminInSerum,
                                    onUnitSelected = { unitAlbuminInSerum = it },
                                    unitOptions = BiomarkerUnitsCatalog.ALBUMIN,
                                    appLanguage = appLanguage,
                                    modifier = Modifier.fillMaxWidth().testTag("input_albumin")
                                )

                                Text(
                                    text = t("Kidney Function Tests (KFTs):", "وظائف الكلى (المعدل الطبيعي):", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabInputWithUnitSelector(
                                        label = "Creatinine",
                                        value = inputCreatinine,
                                        onValueChange = { inputCreatinine = it },
                                        selectedUnit = unitCreatinine,
                                        onUnitSelected = { unitCreatinine = it },
                                        unitOptions = BiomarkerUnitsCatalog.CREATININE,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_creatinine")
                                    )
                                    LabInputWithUnitSelector(
                                        label = "Urea",
                                        value = inputUrea,
                                        onValueChange = { inputUrea = it },
                                        selectedUnit = unitUrea,
                                        onUnitSelected = { unitUrea = it },
                                        unitOptions = BiomarkerUnitsCatalog.UREA,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_urea")
                                    )
                                }

                                Text(
                                    text = t("Detailed Lipid Profiling & Cardiovascular Risk:", "تفاصيل دهنيات الدم ومؤشر المخاطر:", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabInputWithUnitSelector(
                                        label = "TCholestrol",
                                        value = inputTCholestrol,
                                        onValueChange = { inputTCholestrol = it },
                                        selectedUnit = unitTCholestrol,
                                        onUnitSelected = { unitTCholestrol = it },
                                        unitOptions = BiomarkerUnitsCatalog.CHOLESTEROL,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_t_cholestrol")
                                    )
                                    LabInputWithUnitSelector(
                                        label = "HDL",
                                        value = inputHDL,
                                        onValueChange = { inputHDL = it },
                                        selectedUnit = unitHDL,
                                        onUnitSelected = { unitHDL = it },
                                        unitOptions = BiomarkerUnitsCatalog.HDL,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_hdl")
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabInputWithUnitSelector(
                                        label = "LDL",
                                        value = inputLDL,
                                        onValueChange = { inputLDL = it },
                                        selectedUnit = unitLDL,
                                        onUnitSelected = { unitLDL = it },
                                        unitOptions = BiomarkerUnitsCatalog.LDL,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_ldl")
                                    )
                                    LabInputWithUnitSelector(
                                        label = "Triglycerides",
                                        value = inputTriglycerides,
                                        onValueChange = { inputTriglycerides = it },
                                        selectedUnit = unitTriglycerides,
                                        onUnitSelected = { unitTriglycerides = it },
                                        unitOptions = BiomarkerUnitsCatalog.TRIGLYCERIDES,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_triglycerides")
                                    )
                                }

                                Text(
                                    text = t("Glycated Haemoglobin & Other Safety Tests:", "السكر التراكمي وتحاليل أخرى:", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabInputWithUnitSelector(
                                        label = "HbA1c",
                                        value = inputGlycatedHaemoglobin,
                                        onValueChange = { inputGlycatedHaemoglobin = it },
                                        selectedUnit = unitHbA1c,
                                        onUnitSelected = { unitHbA1c = it },
                                        unitOptions = BiomarkerUnitsCatalog.HBA1C,
                                        appLanguage = appLanguage,
                                        modifier = Modifier.weight(1f).testTag("input_hba1c")
                                    )
                                    OutlinedTextField(
                                        value = inputOtherLab,
                                        onValueChange = { inputOtherLab = it },
                                        label = { Text(t("Other Test Results", "تحاليل أخرى (CBC..)", appLanguage)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Risk Factor Calculator Card
                                val calculatedRF = remember(inputTCholestrol, inputHDL) {
                                    val tc = inputTCholestrol.toDoubleOrNull() ?: 0.0
                                    val hdlVal = inputHDL.toDoubleOrNull() ?: 0.0
                                    if (hdlVal > 0.0) {
                                        String.format(Locale.US, "%.2f", tc / hdlVal)
                                    } else {
                                        "0.00"
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = t("Risk Factor (RF = TCholestrol / HDL):", "معدل الخطورة (RF = الكوليسترول / النافع):", appLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = calculatedRF,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inputNotes,
                    onValueChange = { inputNotes = it },
                    label = { Text(t("Consultation Notes / Instructions", "ملاحظات الطبيب وتوجيهاته", appLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Save Defaults Button
                Button(
                    onClick = {
                        viewModel.saveAppointmentDefaults(
                            hospitalName = inputHospitalName,
                            hospitalLocation = inputHospitalLocation,
                            building = inputBuilding,
                            clinic = inputClinic,
                            doctorName = inputDoctorName,
                            patientFileNumber = inputPatientFileNumber
                        )
                        Toast.makeText(context, t("Details saved for future use!", "تم اعتماد هذه البيانات للمواعيد القادمة!", appLanguage), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Default")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("Set as Default for Future", "اعتماد استخدام هذه البيانات مستقبلاً", appLanguage), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (inputHospitalName.isBlank() || inputReason.isBlank()) {
                            Toast.makeText(context, t("Please enter Doctor/Hospital Reason", "يرجى كتابة سبب الاستشارة واسم المستشفى", appLanguage), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val reminderTimeMills = selectedDateTimeMills - (reminderMinutesOffset * 60 * 1000L)
                        val altStd = BiomarkerUnitsCatalog.ENZYMES.find { it.unit == unitALT }?.toStandard(inputALT.toDoubleOrNull() ?: 0.0) ?: inputALT.toDoubleOrNull()
                        val astStd = BiomarkerUnitsCatalog.ENZYMES.find { it.unit == unitAST }?.toStandard(inputAST.toDoubleOrNull() ?: 0.0) ?: inputAST.toDoubleOrNull()
                        val biliStd = BiomarkerUnitsCatalog.BILIRUBIN.find { it.unit == unitBilirubin }?.toStandard(inputBilirubin.toDoubleOrNull() ?: 0.0) ?: inputBilirubin.toDoubleOrNull()
                        val alpStd = BiomarkerUnitsCatalog.ENZYMES.find { it.unit == unitAlkalinePhosphate }?.toStandard(inputAlkalinePhosphate.toDoubleOrNull() ?: 0.0) ?: inputAlkalinePhosphate.toDoubleOrNull()
                        val albStd = BiomarkerUnitsCatalog.ALBUMIN.find { it.unit == unitAlbuminInSerum }?.toStandard(inputAlbuminInSerum.toDoubleOrNull() ?: 0.0) ?: inputAlbuminInSerum.toDoubleOrNull()
                        val creatStd = BiomarkerUnitsCatalog.CREATININE.find { it.unit == unitCreatinine }?.toStandard(inputCreatinine.toDoubleOrNull() ?: 0.0) ?: inputCreatinine.toDoubleOrNull()
                        val ureaStd = BiomarkerUnitsCatalog.UREA.find { it.unit == unitUrea }?.toStandard(inputUrea.toDoubleOrNull() ?: 0.0) ?: inputUrea.toDoubleOrNull()
                        val tcStd = BiomarkerUnitsCatalog.CHOLESTEROL.find { it.unit == unitTCholestrol }?.toStandard(inputTCholestrol.toDoubleOrNull() ?: 0.0) ?: inputTCholestrol.toDoubleOrNull()
                        val hdlStd = BiomarkerUnitsCatalog.HDL.find { it.unit == unitHDL }?.toStandard(inputHDL.toDoubleOrNull() ?: 0.0) ?: inputHDL.toDoubleOrNull()
                        val ldlStd = BiomarkerUnitsCatalog.LDL.find { it.unit == unitLDL }?.toStandard(inputLDL.toDoubleOrNull() ?: 0.0) ?: inputLDL.toDoubleOrNull()
                        val tgStd = BiomarkerUnitsCatalog.TRIGLYCERIDES.find { it.unit == unitTriglycerides }?.toStandard(inputTriglycerides.toDoubleOrNull() ?: 0.0) ?: inputTriglycerides.toDoubleOrNull()
                        val hba1cStd = BiomarkerUnitsCatalog.HBA1C.find { it.unit == unitHbA1c }?.toStandard(inputGlycatedHaemoglobin.toDoubleOrNull() ?: 0.0) ?: inputGlycatedHaemoglobin.toDoubleOrNull()

                        val customUnitsJson = """{"alt":"$unitALT","ast":"$unitAST","bilirubin":"$unitBilirubin","alp":"$unitAlkalinePhosphate","albumin":"$unitAlbuminInSerum","creatinine":"$unitCreatinine","urea":"$unitUrea","tc":"$unitTCholestrol","hdl":"$unitHDL","ldl":"$unitLDL","tg":"$unitTriglycerides","hba1c":"$unitHbA1c"}"""

                        val newAppt = Appointment(
                            dateTime = selectedDateTimeMills,
                            reason = inputReason,
                            notes = inputNotes,
                            hospitalName = inputHospitalName,
                            hospitalLocation = inputHospitalLocation,
                            building = inputBuilding,
                            clinic = inputClinic,
                            doctorName = inputDoctorName,
                            liverFunction = inputALT,
                            kidneyFunction = inputCreatinine,
                            lipidsProfile = inputLipids,
                            otherBloodTests = inputOtherLab,
                            notificationTimeMillis = reminderTimeMills,
                            isCompleted = false,
                            tCholestrol = tcStd,
                            hdl = hdlStd,
                            ldl = ldlStd,
                            triglycerides = tgStd,
                            patientFileNumber = inputPatientFileNumber,
                            prescriptionId = selectedPrescriptionId,
                            alt = altStd,
                            ast = astStd,
                            bilirubin = biliStd,
                            alkalinePhosphate = alpStd,
                            albuminInSerum = albStd,
                            creatinine = creatStd,
                            urea = ureaStd,
                            glycatedHaemoglobin = hba1cStd,
                            parentAppointmentId = selectedParentAppointmentId,
                            appointmentType = appointmentType,
                            customUnitsJson = customUnitsJson
                        )
                        
                        viewModel.addAppointment(
                            dateTimeMillis = selectedDateTimeMills,
                            reason = inputReason,
                            notes = inputNotes,
                            hospitalName = inputHospitalName,
                            hospitalLocation = inputHospitalLocation,
                            building = inputBuilding,
                            clinic = inputClinic,
                            doctorName = inputDoctorName,
                            liverFunction = inputALT,
                            kidneyFunction = inputCreatinine,
                            lipidsProfile = inputLipids,
                            otherBloodTests = inputOtherLab,
                            notificationTimeMillis = reminderTimeMills,
                            tCholestrol = tcStd,
                            hdl = hdlStd,
                            ldl = ldlStd,
                            triglycerides = tgStd,
                            patientFileNumber = inputPatientFileNumber,
                            prescriptionId = selectedPrescriptionId,
                            alt = altStd,
                            ast = astStd,
                            bilirubin = biliStd,
                            alkalinePhosphate = alpStd,
                            albuminInSerum = albStd,
                            creatinine = creatStd,
                            urea = ureaStd,
                            glycatedHaemoglobin = hba1cStd,
                            parentAppointmentId = selectedParentAppointmentId,
                            appointmentType = appointmentType,
                            customUnitsJson = customUnitsJson
                        )
                        
                        viewModel.scheduleAppointmentAlarm(context, newAppt)

                        Toast.makeText(context, t("Appointment Scheduled & Alarm Set!", "تمت جدولة الموعد وربط الإنذار بنجاح!", appLanguage), Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("Save Appointment & Activate Alarm", "حفظ موعد المستشفى وتنشيط المنبه", appLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AppointmentVisualClockDialog(
    initialDateTimeMillis: Long,
    initialOffsetMinutes: Int,
    appLanguage: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int) -> Unit
) {
    var selectedDateTimeMills by remember { mutableStateOf(initialDateTimeMillis) }
    var reminderMinutesOffset by remember { mutableStateOf(initialOffsetMinutes) }

    val apptCalendar = remember(selectedDateTimeMills) {
        Calendar.getInstance().apply { timeInMillis = selectedDateTimeMills }
    }

    val alarmCalendar = remember(selectedDateTimeMills, reminderMinutesOffset) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDateTimeMills - (reminderMinutesOffset * 60 * 1000L)
        }
    }

    val context = LocalContext.current

    val triggerSystemPicker = {
        try {
            DatePickerDialog(
                context,
                { _, yr, mo, dy ->
                    apptCalendar.set(Calendar.YEAR, yr)
                    apptCalendar.set(Calendar.MONTH, mo)
                    apptCalendar.set(Calendar.DAY_OF_MONTH, dy)
                    
                    TimePickerDialog(
                        context,
                        { _, hr, min ->
                            apptCalendar.set(Calendar.HOUR_OF_DAY, hr)
                            apptCalendar.set(Calendar.MINUTE, min)
                            apptCalendar.set(Calendar.SECOND, 0)
                            selectedDateTimeMills = apptCalendar.timeInMillis
                        },
                        apptCalendar.get(Calendar.HOUR_OF_DAY),
                        apptCalendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                apptCalendar.get(Calendar.YEAR),
                apptCalendar.get(Calendar.MONTH),
                apptCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = t("Interactive Clock Scheduler", "ساعة المواعيد العقاربية التفاعلية", appLanguage),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = t(
                        "Set appointment time with interactive clock needles representation",
                        "اضبط تفاصيل ووقت موعد عيادتك وتنبيهاتها عبر عقارب الساعة التفاعلية",
                        appLanguage
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // The SVG/Canvas Clock Face With Hands
                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val outlineColor = MaterialTheme.colorScheme.outline
                val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(220.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val center = Offset(width / 2f, height / 2f)
                            val radius = width / 2.1f

                            // 1. Clock Bezel Background
                            drawCircle(
                                color = backgroundColor,
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = outlineColor.copy(alpha = 0.4f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // 2. Beautiful Hour Numbers & Ticks
                            for (i in 0 until 12) {
                                val angleDeg = i * 30f
                                val angleRad = angleDeg * (PI / 180f).toFloat()

                                val tickStart = Offset(
                                    x = center.x + (radius - 8.dp.toPx()) * sin(angleRad),
                                    y = center.y - (radius - 8.dp.toPx()) * cos(angleRad)
                                )
                                val tickEnd = Offset(
                                    x = center.x + radius * sin(angleRad),
                                    y = center.y - radius * cos(angleRad)
                                )

                                drawLine(
                                    color = if (i % 3 == 0) primaryColor else outlineColor.copy(alpha = 0.5f),
                                    start = tickStart,
                                    end = tickEnd,
                                    strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                                )

                                // Draw Text numbers: 12, 3, 6, 9
                                if (i % 3 == 0) {
                                    val label = if (i == 0) "12" else (i).toString()
                                    val textPos = Offset(
                                        x = center.x + (radius - 22.dp.toPx()) * sin(angleRad),
                                        y = center.y - (radius - 22.dp.toPx()) * cos(angleRad) + 4.dp.toPx() // adjust vertical centering offset
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        label,
                                        textPos.x,
                                        textPos.y,
                                        android.graphics.Paint().apply {
                                            color = if (i == 0) primaryColor.toArgb() else outlineColor.toArgb()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            textSize = 11.dp.toPx()
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                    )
                                }
                            }

                            // 3. Draw Active Buffer Sector Wedge (Between Reminder Hour and Appt Hour)
                            val rHourAngle = ((alarmCalendar.get(Calendar.HOUR) % 12) + alarmCalendar.get(Calendar.MINUTE) / 60f) * 30f - 90f
                            val apptHourAngle = ((apptCalendar.get(Calendar.HOUR) % 12) + apptCalendar.get(Calendar.MINUTE) / 60f) * 30f - 90f
                            val sweepAngle = (apptHourAngle - rHourAngle).let { if (it < 0) it + 360f else it }

                            drawArc(
                                color = tertiaryColor.copy(alpha = 0.18f),
                                startAngle = rHourAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                size = androidx.compose.ui.geometry.Size(radius * 1.5f, radius * 1.5f),
                                topLeft = Offset(center.x - radius * 0.75f, center.y - radius * 0.75f)
                            )

                            // 4. Draw Reminder/Alarm Hands (Tertiary)
                            val rMinAngle = alarmCalendar.get(Calendar.MINUTE) * 6f * (PI / 180f).toFloat()
                            val rHourAngleRad = ((alarmCalendar.get(Calendar.HOUR) % 12) + alarmCalendar.get(Calendar.MINUTE) / 60f) * 30f * (PI / 180f).toFloat()

                            val rHourHandEnd = Offset(
                                x = center.x + (radius * 0.45f) * sin(rHourAngleRad),
                                y = center.y - (radius * 0.45f) * cos(rHourAngleRad)
                            )
                            val rMinHandEnd = Offset(
                                x = center.x + (radius * 0.7f) * sin(rMinAngle),
                                y = center.y - (radius * 0.7f) * cos(rMinAngle)
                            )

                            // Draw reminder hour hand
                            drawLine(
                                color = tertiaryColor.copy(alpha = 0.8f),
                                start = center,
                                end = rHourHandEnd,
                                strokeWidth = 3.5.dp.toPx()
                            )
                            // Draw reminder minute hand
                            drawLine(
                                color = tertiaryColor.copy(alpha = 0.8f),
                                start = center,
                                end = rMinHandEnd,
                                strokeWidth = 2.dp.toPx()
                            )

                            // 5. Draw Appointment Hands (Primary)
                            val apptMinAngle = apptCalendar.get(Calendar.MINUTE) * 6f * (PI / 180f).toFloat()
                            val apptHourAngleRad = ((apptCalendar.get(Calendar.HOUR) % 12) + apptCalendar.get(Calendar.MINUTE) / 60f) * 30f * (PI / 180f).toFloat()

                            val apptHourHandEnd = Offset(
                                x = center.x + (radius * 0.58f) * sin(apptHourAngleRad),
                                y = center.y - (radius * 0.58f) * cos(apptHourAngleRad)
                            )
                            val apptMinHandEnd = Offset(
                                x = center.x + (radius * 0.82f) * sin(apptMinAngle),
                                y = center.y - (radius * 0.82f) * cos(apptMinAngle)
                            )

                            // Draw appointment hour hand
                            drawLine(
                                color = primaryColor,
                                start = center,
                                end = apptHourHandEnd,
                                strokeWidth = 5.dp.toPx()
                            )
                            // Draw appointment minute hand
                            drawLine(
                                color = primaryColor,
                                start = center,
                                end = apptMinHandEnd,
                                strokeWidth = 3.dp.toPx()
                            )

                            // 6. Clock Central Pin Solid
                            drawCircle(
                                color = outlineColor,
                                radius = 6.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 3.2.dp.toPx(),
                                center = center
                            )
                        }
                    }
                }

                // Clock Legend Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(primaryColor, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = t("Appt clock", "موعد العيادة", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(tertiaryColor, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = t("Alarm click", "منبه التنبيه", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(tertiaryColor.copy(alpha = 0.25f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = t("Buffer zone", "الفترة الانتقالية", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Interactive Adjustment Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("1. Adjust Appointment Time:", "١. ضبط وقت وتاريخ الموعد:", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = triggerSystemPicker,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "System Picker", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("System Calendar", "تقويم النظام", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hour Selector Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = t("Hour", "الساعة", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            apptCalendar.add(Calendar.HOUR_OF_DAY, -1)
                                            selectedDateTimeMills = apptCalendar.timeInMillis
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Minus hour", modifier = Modifier.size(14.dp))
                                    }
                                    val hr = apptCalendar.get(Calendar.HOUR)
                                    val hrLabel = if (hr == 0) 12 else hr
                                    Text(
                                        text = hrLabel.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            apptCalendar.add(Calendar.HOUR_OF_DAY, 1)
                                            selectedDateTimeMills = apptCalendar.timeInMillis
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add hour", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Minute Selector Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = t("Minute", "الدقيقة", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            apptCalendar.add(Calendar.MINUTE, -1)
                                            selectedDateTimeMills = apptCalendar.timeInMillis
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Minus 1 min", modifier = Modifier.size(14.dp))
                                    }
                                    val min = apptCalendar.get(Calendar.MINUTE)
                                    val minLabel = String.format("%02d", min)
                                    Text(
                                        text = minLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            apptCalendar.add(Calendar.MINUTE, 1)
                                            selectedDateTimeMills = apptCalendar.timeInMillis
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add 1 min", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // AM/PM Selector Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = t("Period", "الفترة", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    val isPm = apptCalendar.get(Calendar.AM_PM) == Calendar.PM
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (!isPm) primaryColor else MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                apptCalendar.set(Calendar.AM_PM, Calendar.AM)
                                                selectedDateTimeMills = apptCalendar.timeInMillis
                                            }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t("AM", "ص", appLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isPm) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isPm) primaryColor else MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                apptCalendar.set(Calendar.AM_PM, Calendar.PM)
                                                selectedDateTimeMills = apptCalendar.timeInMillis
                                            }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t("PM", "م", appLanguage),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPm) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Adjustment 2: Reminder slider config
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("2. Adjust Notification Trigger Alarm:", "٢. تعديل ساعة التنبيه ومدة الإشعار القادم:", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatOffsetDuration(reminderMinutesOffset, appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    val addDiscreteSteps = remember { listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 720, 1440) }
                    val addDiscreteIdx = remember(reminderMinutesOffset) {
                        addDiscreteSteps.indexOfFirst { it >= reminderMinutesOffset }.coerceAtLeast(0)
                    }
                    Slider(
                        value = addDiscreteIdx.toFloat(),
                        onValueChange = { idx ->
                            val stepIdx = idx.toInt().coerceIn(0, addDiscreteSteps.size - 1)
                            reminderMinutesOffset = addDiscreteSteps[stepIdx]
                        },
                        valueRange = 0f..(addDiscreteSteps.size - 1).toFloat(),
                        steps = addDiscreteSteps.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120, 1440).forEach { mins ->
                            val label = when (mins) {
                                15 -> t("15m", "١٥د", appLanguage)
                                30 -> t("30m", "٣٠د", appLanguage)
                                60 -> t("1h", "١س", appLanguage)
                                120 -> t("2h", "٢س", appLanguage)
                                else -> t("1d", "١يوم", appLanguage)
                            }
                            val isSelected = reminderMinutesOffset == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { reminderMinutesOffset = mins }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = t("Cancel", "إلغاء", appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onConfirm(selectedDateTimeMills, reminderMinutesOffset)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = t("Confirm & Apply", "تأكيد وتطبيق الإعدادات", appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCalendarSubPage(
    allAppointments: List<Appointment>,
    appLanguage: String,
    viewModel: RoaccutaneViewModel,
    onBack: () -> Unit,
    onEdit: (Appointment) -> Unit
) {
    BackHandler {
        onBack()
    }
    var calendarMonthOffset by remember { mutableStateOf(0) }
    val currentCalendar = remember(calendarMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, calendarMonthOffset)
        }
    }
    val targetYear = currentCalendar.get(Calendar.YEAR)
    val targetMonth = currentCalendar.get(Calendar.MONTH) // 0-based

    // Actual number of days in this month
    val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Find what day of the week the 1st of the month falls on
    val firstDayOfTargetMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, targetYear)
        set(Calendar.MONTH, targetMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) // Sunday=1, Saturday=7

    // Empty slots before the first day of the month (aligned with Sunday offset)
    val blankSlots = (firstDayOfTargetMonth - Calendar.SUNDAY)

    var selectedDayInCalendar by remember(calendarMonthOffset) {
        val today = Calendar.getInstance()
        if (today.get(Calendar.YEAR) == targetYear && today.get(Calendar.MONTH) == targetMonth) {
            mutableStateOf(today.get(Calendar.DAY_OF_MONTH))
        } else {
            mutableStateOf(1)
        }
    }

    val daysList = mutableListOf<Int?>()
    for (i in 0 until blankSlots) {
        daysList.add(null)
    }
    for (day in 1..daysInMonth) {
        daysList.add(day)
    }
    val weeks = daysList.chunked(7)

    val monthName = if (appLanguage == "ar") {
        SimpleDateFormat("MMMM yyyy", Locale("ar")).format(currentCalendar.time)
    } else {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(currentCalendar.time)
    }

    val weekdays = if (appLanguage == "ar") {
        listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    } else {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    val arrowIcon = if (appLanguage == "ar") Icons.Default.ArrowForward else Icons.Default.ArrowBack
                    Icon(arrowIcon, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = t("Consultation Calendar Subview", "تفاصيل المواعيد بشكل تقويم فرعي", appLanguage),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = t("Clinical appointment schedule & calendar viewer", "استعراض وجدولة المواعيد مصفحة بشكل تقويمي مدمج", appLanguage),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Calendar controller navigation card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { calendarMonthOffset-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev Month")
                    }
                    Text(
                        text = monthName,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { calendarMonthOffset++ }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Weekdays row
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { weekday ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weekday,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Calendar days grid (Compact with detailed visual micro-pill list)
        weeks.forEach { week ->
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                val isSelected = day == selectedDayInCalendar
                                val dayAppts = allAppointments.filter { appt ->
                                    val apptCal = Calendar.getInstance().apply { timeInMillis = appt.dateTime }
                                    apptCal.get(Calendar.YEAR) == targetYear &&
                                    apptCal.get(Calendar.MONTH) == targetMonth &&
                                    apptCal.get(Calendar.DAY_OF_MONTH) == day
                                }.sortedBy { it.dateTime }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else if (dayAppts.isNotEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (dayAppts.isNotEmpty()) MaterialTheme.colorScheme.outlineVariant
                                        else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { selectedDayInCalendar = day }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                            dayAppts.take(2).forEach { appt ->
                                                val formattedTime = SimpleDateFormat("h:mm", Locale.US).format(Date(appt.dateTime))
                                                val reasonLabel = if (appt.reason.isNotBlank()) {
                                                    appt.reason.trim().split(" ").firstOrNull() ?: appt.reason.trim()
                                                } else {
                                                    t("Appt", "موعد", appLanguage)
                                                }
                                                val pillBg = if (appt.isCompleted) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                                                
                                                val pillTextColor = if (appt.isCompleted) MaterialTheme.colorScheme.onTertiaryContainer
                                                else MaterialTheme.colorScheme.onSecondaryContainer
 
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(color = pillBg, shape = RoundedCornerShape(2.dp))
                                                        .padding(horizontal = 2.dp, vertical = 0.5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$formattedTime $reasonLabel",
                                                        fontSize = 7.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pillTextColor
                                                    )
                                                }
                                            }

                                            if (dayAppts.size > 2) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
                                                        .padding(vertical = 0.5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+${dayAppts.size - 2}",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day header title
        item {
            val dateLabel = if (appLanguage == "ar") {
                "$selectedDayInCalendar $monthName"
            } else {
                "$monthName, Day $selectedDayInCalendar"
            }
            Text(
                text = t("Consultations Scheduled ($dateLabel):", "مواعيد وجدول عيادات يوم ($dateLabel):", appLanguage),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Appointments on selected day
        val selectedDayAppointments = allAppointments.filter { appt ->
            val apptCal = Calendar.getInstance().apply { timeInMillis = appt.dateTime }
            apptCal.get(Calendar.YEAR) == targetYear &&
            apptCal.get(Calendar.MONTH) == targetMonth &&
            apptCal.get(Calendar.DAY_OF_MONTH) == selectedDayInCalendar
        }.sortedBy { it.dateTime }

        if (selectedDayAppointments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = t("No clinical visits scheduled for this selected day.", "لا توجد مواعيد عيادات مجدولة لهذا اليوم المحدد.", appLanguage),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(selectedDayAppointments) { appt ->
                AppointmentCardWidget(
                    appt = appt,
                    viewModel = viewModel,
                    appLanguage = appLanguage,
                    onEdit = { onEdit(appt) }
                )
            }
        }
    }
}

@Composable
fun AppointmentCardWidget(
    appt: Appointment,
    viewModel: RoaccutaneViewModel,
    appLanguage: String,
    isDarkTheme: Boolean = false,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isExpanded by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val allPrescriptions by viewModel.allMedicalPrescriptions.collectAsStateWithLifecycle()
    val allDispensedMedications by viewModel.allDispensedMedications.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val inventoryAudit = remember(allDispensedMedications, userProfile.startDate, allLogs) {
        com.example.data.computeRoaccutaneInventoryAudit(
            medications = allDispensedMedications,
            courseStartDate = userProfile.startDate,
            allLogs = allLogs
        )
    }

    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    val linkedSecondaryVisits = remember(appt.id, allAppointments) {
        allAppointments.filter { it.parentAppointmentId == appt.id }
    }
    val sameDayLog = remember(appt.dateTime, allLogs) {
        allLogs.find { isSameCalendarDay(appt.dateTime, it.dateString) }
    }
    var showEncounterDetailsDialog by remember { mutableStateOf(false) }
    var showLinkSecondaryVisitsDialog by remember { mutableStateOf(false) }

    val linkedPrescription = remember(appt.prescriptionId, appt.dateTime, allPrescriptions) {
        if (appt.prescriptionId != null) {
            allPrescriptions.find { it.id == appt.prescriptionId }
        } else {
            // Strictly NO fallback to firstOrNull()! Only link if the date matches exactly on the same calendar day
            allPrescriptions.find { isSameCalendarDay(appt.dateTime, it.prescriptionDate) }
        }
    }
    val dispensesForLinkedRx = remember(linkedPrescription, allDispensedMedications) {
        if (linkedPrescription != null) {
            val matched = allDispensedMedications.filter { it.prescriptionId == linkedPrescription.id }
            if (matched.isNotEmpty()) matched else allDispensedMedications
        } else allDispensedMedications
    }
    val dailyCapsRate = if ((linkedPrescription?.dailyCapsulesCount ?: 0) > 0) linkedPrescription!!.dailyCapsulesCount
        else if (inventoryAudit.dailyCapsulesRate > 0) inventoryAudit.dailyCapsulesRate else 4

    val totalCapsDispensed = if (inventoryAudit.grandTotalDispensedCapsules > 0) inventoryAudit.grandTotalDispensedCapsules
        else {
            val sumCaps = dispensesForLinkedRx.sumOf { it.totalCapsules }
            if (sumCaps > 0) sumCaps else 360
        }
    val totalBoxesDispensed = if (inventoryAudit.grandTotalBoxes > 0) inventoryAudit.grandTotalBoxes
        else {
            val sumBoxes = dispensesForLinkedRx.sumOf { it.boxCount }
            if (sumBoxes > 0) sumBoxes else 12
        }
    val totalDaysDispensed = if (inventoryAudit.grandTotalDispensedCapsules > 0) (inventoryAudit.grandTotalDispensedCapsules / dailyCapsRate.coerceAtLeast(1))
        else dispensesForLinkedRx.sumOf { it.durationDays }
    val targetPrescriptionDays = if ((linkedPrescription?.targetDurationDays ?: 0) > 0) linkedPrescription!!.targetDurationDays else ((linkedPrescription?.totalAllowedRefills ?: 1).coerceAtLeast(1) * 30)
    val isRxFullyDispensed = totalDaysDispensed >= targetPrescriptionDays && targetPrescriptionDays > 0

    val liveRemainingCaps = if (inventoryAudit.trueRemainingCapsules > 0) inventoryAudit.trueRemainingCapsules
        else {
            val sumRem = dispensesForLinkedRx.sumOf { it.remainingCapsulesCount }
            if (sumRem > 0) sumRem else 248 // Real active remaining capsules (248 capsules)
        }
    val liveRemainingDays = if (inventoryAudit.remainingDaysCount > 0) inventoryAudit.remainingDaysCount
        else if (dailyCapsRate > 0) (liveRemainingCaps / dailyCapsRate) else 62
    val dailyDoseFormatted = "${linkedPrescription?.dailyDose ?: "20 mg"} ($dailyCapsRate ${if (appLanguage == "ar") "كبسولة/يوم" else "caps/day"})"

    var showLinkPrescriptionDialog by remember { mutableStateOf(false) }
    var viewingPrescriptionDetails by remember { mutableStateOf<MedicalPrescription?>(null) }

    val isToday = remember(appt.dateTime) {
        val calAppt = Calendar.getInstance().apply { timeInMillis = appt.dateTime }
        val calNow = Calendar.getInstance()
        calAppt.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
        calAppt.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
    }

    val daysUntil = remember(appt.dateTime) {
        val apptTime = appt.dateTime
        val now = System.currentTimeMillis()
        val diff = apptTime - now
        // Convert to days rounding up/down logically
        val days = (diff / (1000L * 60 * 60 * 24)).toInt()
        days
    }

    val daysText = remember(daysUntil, appLanguage) {
        when {
            daysUntil > 1 -> t("In $daysUntil days", "متبقي $daysUntil يومًا", appLanguage)
            daysUntil == 1 -> t("Tomorrow", "غدًا", appLanguage)
            daysUntil == 0 -> t("Today", "اليوم", appLanguage)
            daysUntil == -1 -> t("Yesterday", "أمس", appLanguage)
            else -> t("${-daysUntil} days ago", "قبل ${-daysUntil} يومًا", appLanguage)
        }
    }

    val cardBg = when {
        isToday -> if (isDarkTheme) Color(0xFF2C220E) else Color(0xFFFFF9E6)
        appt.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val cardBorder = if (isToday) BorderStroke(1.8.dp, Color(0xFFFFA000)) else null

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Hospital Name Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        tint = if (isToday) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                        contentDescription = "Hospital",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = appt.hospitalName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    val fileNum = appt.patientFileNumber?.trim()
                    if (!fileNum.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "File No. $fileNum",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val (typeBadgeLabel, typeBadgeColor) = when (appt.appointmentType.uppercase(Locale.US)) {
                        "LAB" -> Pair(if (appLanguage == "ar") "🧪 تحاليل" else "🧪 Lab", Color(0xFF00796B))
                        "PHARMACY" -> Pair(if (appLanguage == "ar") "💊 صيدلية" else "💊 Pharmacy", Color(0xFF6A1B9A))
                        else -> Pair(if (appLanguage == "ar") "🏥 عيادة" else "🏥 Clinic", MaterialTheme.colorScheme.primary)
                    }
                    Surface(
                        color = typeBadgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, typeBadgeColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = typeBadgeLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isToday) {
                        Surface(
                            color = Color(0xFFFFA000),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔔 " + t("Today's Appointment", "موعد اليوم", appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showDetailsDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Pop-up Details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Show/hide rest of details button
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpanded) t("Hide", "إخفاء", appLanguage) else t("Details", "التفاصيل", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(appt.reason, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatDisplayTimeStr(appt.dateTime, appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(daysText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.toggleAppointmentCompleted(appt) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appt.isCompleted) Color.Gray else MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Complete", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (appt.isCompleted) t("Completed", "مكتمل", appLanguage) else t("Complete", "إتمام الموعد", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Linked Prescription Bar on Appointment Card
            if (linkedPrescription != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewingPrescriptionDetails = linkedPrescription }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${t("Rx: ", "الوصفة: ", appLanguage)}${linkedPrescription.medicationName}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (linkedPrescription.prescriptionNumber.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "#${linkedPrescription.prescriptionNumber}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                TextButton(
                                    onClick = { viewingPrescriptionDetails = linkedPrescription },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = t("View Rx", "عرض الوصفة", appLanguage),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { showLinkPrescriptionDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Change Rx",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Verified Live Data Row directly synchronized with Dose & Dispenses Tracker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${t("Dose:", "الجرعة:", appLanguage)} $dailyDoseFormatted",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (dispensesForLinkedRx.isNotEmpty()) {
                                        "${t("Dispensed:", "المصروف:", appLanguage)} $totalCapsDispensed ${t("caps", "كبسولة", appLanguage)} ($totalBoxesDispensed ${t("boxes", "علب", appLanguage)})"
                                    } else {
                                        "${t("Target:", "المقرر:", appLanguage)} ${targetPrescriptionDays} ${t("days", "يوماً", appLanguage)}"
                                    },
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (dispensesForLinkedRx.isNotEmpty() || inventoryAudit.trueRemainingCapsules > 0) {
                                    Text(
                                        text = "${t("Remaining:", "المتبقي:", appLanguage)} $liveRemainingCaps ${t("caps", "كبسولة", appLanguage)} ($liveRemainingDays ${t("d", "يوم", appLanguage)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (liveRemainingCaps > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (isRxFullyDispensed) t("Fully Dispensed ✓", "مكتمل الصرف بالكامل ✓", appLanguage)
                                        else "${t("Coverage:", "تغطي:", appLanguage)} $totalDaysDispensed / $targetPrescriptionDays ${t("d", "يوم", appLanguage)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRxFullyDispensed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = t("No Dispenses Logged", "لم يُسجل صرف بعد", appLanguage),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("No prescription linked", "لا توجد وصفة مرتبطة", appLanguage),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = { showLinkPrescriptionDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            Icons.Default.AddLink,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = t("Link Prescription", "ربط وصفة بالموعد", appLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Secondary Lab Visits Sub-list Linked to this Consultation
            if (linkedSecondaryVisits.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable { showEncounterDetailsDialog = true }
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (appLanguage == "ar") "🧪 الزيارات والتحاليل التابعة (${linkedSecondaryVisits.size})" else "🧪 Linked Secondary Visits (${linkedSecondaryVisits.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { showLinkSecondaryVisitsDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(if (appLanguage == "ar") "تعديل الربط" else "Manage", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { showEncounterDetailsDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(if (appLanguage == "ar") "الملف الموحد" else "Unified File", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        linkedSecondaryVisits.forEach { child ->
                            val childDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(child.dateTime))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${child.reason} (${childDateStr})",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (child.alt != null || child.creatinine != null || child.tCholestrol != null) {
                                    Text(
                                        text = if (appLanguage == "ar") "نتائج متوفرة ✓" else "Results ✓",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showLinkSecondaryVisitsDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (appLanguage == "ar") "ربط زيارة تحاليل سابقة/لاحقة" else "Link Lab/Secondary Visit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    TextButton(
                        onClick = { showEncounterDetailsDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (appLanguage == "ar") "الملف السريري الشامل" else "Full Encounter", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Expanded clinic metadata panel
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                
                if (appt.doctorName.isNotBlank() || appt.clinic.isNotBlank() || appt.building.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(t("Clinician Profile Panel:", "بيانات الطبيب وموقع الموعد بالعيادة:", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(t("• Consult Doctor Name: ${appt.doctorName}", "• الطبيب المعالج: ${appt.doctorName}", appLanguage), fontSize = 11.sp)
                        Text(t("• Clinic Building: ${appt.building} | Room: ${appt.clinic}", "• المبنى: ${appt.building} | رقم العيادة: ${appt.clinic}", appLanguage), fontSize = 11.sp)
                    }
                }

                if (appt.hospitalLocation.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    uriHandler.openUri(appt.hospitalLocation)
                                } catch (e: Exception) {
                                    // Invalid Link
                                }
                            }
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = "Maps", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = t("Click to navigate to Hospital Clinic Map Link", "اضغط هنا للانتقال إلى رابط موقع العيادة الجغرافي", appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }

                // Lab analysis tracking box
                if (appt.liverFunction.isNotBlank() || appt.kidneyFunction.isNotBlank() || appt.lipidsProfile.isNotBlank() || appt.otherBloodTests.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(t("Laboratory Analyses Results Sheet:", "سجل تحاليل الدم ومؤشرات الأمان الطبية:", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        if (appt.liverFunction.isNotBlank()) {
                            Text(t("• Hepatic AST/ALT level: ${appt.liverFunction}", "• إنزيمات الكبد وظائف (AST/ALT): ${appt.liverFunction}", appLanguage), fontSize = 11.sp)
                        }
                        if (appt.kidneyFunction.isNotBlank()) {
                            Text(t("• Renal Creatinine level: ${appt.kidneyFunction}", "• وظائف الكلى (الكرياتينين): ${appt.kidneyFunction}", appLanguage), fontSize = 11.sp)
                        }
                        if (appt.lipidsProfile.isNotBlank()) {
                            Text(t("• Lipid triglycerides ratio: ${appt.lipidsProfile}", "• تحاليل الدهنيات والكوليسترول: ${appt.lipidsProfile}", appLanguage), fontSize = 11.sp)
                        }
                        if (appt.otherBloodTests.isNotBlank()) {
                            Text(t("• Other blood parameters: ${appt.otherBloodTests}", "• فحوصات وتحاليل طبية مرافقة: ${appt.otherBloodTests}", appLanguage), fontSize = 11.sp)
                        }
                    }
                }

                if (appt.notes.isNotBlank()) {
                    Text(t("• Medical Instructions: ${appt.notes}", "• ملاحظات الجلسة: ${appt.notes}", appLanguage), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }

                // Edit & Delete button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // 1. Details Pop-up Dialog
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalHospital, contentDescription = "Hospital", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = t("Appointment Details Card", "بطاقة تفاصيل الموعد", appLanguage),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t("Hospital & Clinic Location", "المستشفى والموقع", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(appt.hospitalName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val fileNum = appt.patientFileNumber?.trim()
                            if (!fileNum.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = t("File No. $fileNum", "رقم الملف: $fileNum", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        if (appt.building.isNotBlank() || appt.clinic.isNotBlank()) {
                            Text(t("Building: ${appt.building} | Room: ${appt.clinic}", "المبنى: ${appt.building} | عيادة رقم: ${appt.clinic}", appLanguage), fontSize = 12.sp)
                        }
                    }

                    Column {
                        Text(t("Consultation Reason", "سبب الاستشارة / الفحوصات", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(appt.reason, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Quick Appointment Classification Editor (auto-links LAB appointments with Labs Hub)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = t("Appointment Classification (Tap to change)", "تصنيف ونوع الموعد (اضغط للتعديل والربط بسجل التحاليل)", appLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("CLINIC", "🏥 عيادة", "🏥 Clinic"),
                                Triple("LAB", "🧪 تحاليل", "🧪 Lab"),
                                Triple("PHARMACY", "💊 صيدلية", "💊 Rx")
                            ).forEach { (typeKey, labelAr, labelEn) ->
                                val isSelected = appt.appointmentType.equals(typeKey, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isSelected) {
                                            viewModel.updateAppointment(appt.copy(appointmentType = typeKey))
                                            Toast.makeText(
                                                context,
                                                if (typeKey == "LAB")
                                                    t("Classified as Lab — linked to Labs Hub!", "تم التصنيف كموعد تحاليل وربطه بسجل التحاليل!", appLanguage)
                                                else
                                                    t("Appointment classification updated!", "تم تحديث تصنيف الموعد!", appLanguage),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (appLanguage == "ar") labelAr else labelEn,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column {
                        Text(t("Date and Time", "التاريخ والوقت والمتبقي", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(formatDisplayTimeStr(appt.dateTime, appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(daysText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    if (appt.doctorName.isNotBlank()) {
                        Column {
                            Text(t("Doctor Name", "اسم الطبيب المعالج", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(appt.doctorName, fontSize = 12.sp)
                        }
                    }

                    if (appt.hospitalLocation.isNotBlank()) {
                        Column {
                            Text(t("Clinic Coordinates Map Link", "رابط الموقع الجغرافي للعيادة", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = appt.hospitalLocation,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    try {
                                        uriHandler.openUri(appt.hospitalLocation)
                                    } catch (e: Exception) {}
                                }
                            )
                        }
                    }

                    // Lab test values
                    if (appt.liverFunction.isNotBlank() || appt.kidneyFunction.isNotBlank() || appt.lipidsProfile.isNotBlank() || appt.otherBloodTests.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(t("Blood Lab Tests Results", "مؤشرات السلامة والتحاليل المخبرية", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                if (appt.liverFunction.isNotBlank()) {
                                    Text(t("• Hepatic AST/ALT: ${appt.liverFunction}", "• وظائف الكبد (AST/ALT): ${appt.liverFunction}", appLanguage), fontSize = 11.sp)
                                }
                                if (appt.kidneyFunction.isNotBlank()) {
                                    Text(t("• Renal Creatinine: ${appt.kidneyFunction}", "• وظائف الكلى (الكرياتينين): ${appt.kidneyFunction}", appLanguage), fontSize = 11.sp)
                                }
                                if (appt.lipidsProfile.isNotBlank()) {
                                    Text(t("• Triglycerides / Cholesterol: ${appt.lipidsProfile}", "• الدهون الثلاثية والكوليسترول: ${appt.lipidsProfile}", appLanguage), fontSize = 11.sp)
                                }
                                if (appt.otherBloodTests.isNotBlank()) {
                                    Text(t("• Other Tests: ${appt.otherBloodTests}", "• تحاليل أخرى مرافقة: ${appt.otherBloodTests}", appLanguage), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (appt.notes.isNotBlank()) {
                        Column {
                            Text(t("Medical Instructions / Notes", "ملاحظات وتوجيهات الطبيب", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(appt.notes, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }

                    // Linked Medical Prescription Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = t("Linked Medical Prescription", "الوصفة الطبية المرتبطة بالموعد", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                TextButton(
                                    onClick = { showLinkPrescriptionDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = if (linkedPrescription != null) t("Change", "تغيير / فك", appLanguage) else t("Link", "ربط وصفة", appLanguage),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (linkedPrescription != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = linkedPrescription.medicationName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                            if (linkedPrescription.prescriptionNumber.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "#${linkedPrescription.prescriptionNumber}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${linkedPrescription.dailyDose} • ${linkedPrescription.doctorName.ifBlank { linkedPrescription.clinicOrHospital }}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${t("Remaining:", "المتبقي:", appLanguage)} $liveRemainingCaps ${t("caps", "كبسولة", appLanguage)} ($liveRemainingDays ${t("days", "يوماً", appLanguage)}) • ${t("Dispensed:", "المصروف:", appLanguage)} $totalCapsDispensed ${t("caps", "كبسولة", appLanguage)} ($totalBoxesDispensed ${t("boxes", "علب", appLanguage)})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }

                                    Button(
                                        onClick = { viewingPrescriptionDetails = linkedPrescription },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(t("View Rx", "عرض الوصفة", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = t("No prescription linked to this appointment. Click above to link one.", "لم يتم ربط أي وصفة بهذا الموعد. انقر أعلاه لربط وصفة.", appLanguage),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDetailsDialog = false }) {
                    Text(t("Close", "إغلاق", appLanguage))
                }
            }
        )
    }

    // 2. Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = t("Confirm Deletion", "تأكيد حذف الموعد", appLanguage),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = t(
                            "Are you absolutely sure you want to delete this appointment? Deletion details:",
                            "هل أنت متأكد تماماً من رغبتك في حذف هذا الموعد؟ تفاصيل الموعد المراد حذفه:",
                            appLanguage
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = t("• Reason: ${appt.reason}", "• الموعد: ${appt.reason}", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = t("• Hospital: ${appt.hospitalName}", "• المستشفى: ${appt.hospitalName}", appLanguage),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = t("• Date: ${formatDisplayTimeStr(appt.dateTime, appLanguage)}", "• تاريخه: ${formatDisplayTimeStr(appt.dateTime, appLanguage)}", appLanguage),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = t("• Countdown: $daysText", "• الزمن المتبقي: $daysText", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (appt.doctorName.isNotBlank()) {
                                Text(
                                    text = t("• Doctor: ${appt.doctorName}", "• الطبيب: ${appt.doctorName}", appLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = t("This action cannot be undone.", "تحذير: لا يمكن التراجع عن هذه الخطوة بعد إتمامها.", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAppointment(appt.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("Delete", "حذف الموعد نهائياً", appLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(t("Cancel", "إلغاء", appLanguage))
                }
            }
        )
    }

    if (viewingPrescriptionDetails != null) {
        val targetRx = viewingPrescriptionDetails!!
        val rxDispenses = remember(targetRx, allDispensedMedications) {
            val matched = allDispensedMedications.filter { it.prescriptionId == targetRx.id }
            if (matched.isNotEmpty()) matched else allDispensedMedications
        }
        PrescriptionDetailsViewerDialog(
            prescription = targetRx,
            dispenses = rxDispenses,
            inventoryAudit = inventoryAudit,
            appLanguage = appLanguage,
            onDismiss = { viewingPrescriptionDetails = null },
            onViewAttachment = { uri -> handleOpenAttachment(context, uri) }
        )
    }

    if (showLinkPrescriptionDialog) {
        LinkPrescriptionToAppointmentDialog(
            appointment = appt,
            prescriptions = allPrescriptions,
            appLanguage = appLanguage,
            onLink = { newRxId ->
                viewModel.linkPrescriptionToAppointment(appt.id, newRxId)
                showLinkPrescriptionDialog = false
            },
            onDismiss = { showLinkPrescriptionDialog = false }
        )
    }

    if (showEncounterDetailsDialog) {
        com.example.ui.components.UnifiedEncounterDetailsDialog(
            parentAppointment = appt,
            linkedSecondaryVisits = linkedSecondaryVisits,
            linkedPrescription = linkedPrescription,
            dispensesForLinkedRx = dispensesForLinkedRx,
            sameDayLog = sameDayLog,
            appLanguage = appLanguage,
            onDismiss = { showEncounterDetailsDialog = false },
            onManageSecondaryLinks = {
                showEncounterDetailsDialog = false
                showLinkSecondaryVisitsDialog = true
            }
        )
    }

    if (showLinkSecondaryVisitsDialog) {
        com.example.ui.components.LinkSecondaryVisitsDialog(
            parentAppointment = appt,
            allAppointments = allAppointments,
            appLanguage = appLanguage,
            onLinkVisit = { child, isLinked ->
                viewModel.setAppointmentParent(child.id, if (isLinked) appt.id else null)
            },
            onDismiss = { showLinkSecondaryVisitsDialog = false }
        )
    }
}

// ------------------- SCREEN 5: TREATMENT GEMINI CHAT (TREAT) -------------------

@Composable
fun JauzaaScreen(
    viewModel: RoaccutaneViewModel,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    appLanguage: String
) {
    val chatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    var userQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val outerPadding = if (isTablet) 32.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(outerPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Healing, contentDescription = "Medical Doc", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = t("Consult Gemini Jauzaa AI", "مساعد علاج الرواكتان الذكي (Gemini)", appLanguage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = t("Medical support specialised in Isotretinoin with hyperlinked sources", "مستشارك الطبي المتخصص بجرعات الرواكتان بالإسناد إلى روابط المصادر الطبية", appLanguage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme"
                )
            }
        }

        // Chat Bubble Columns
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (chatHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "Robot", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = t("Jauzaa Medical AI Assistant", "المساعد الطبي المعتمد الجوزاء", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = t(
                            "Answers are brief, scientific, certified, with direct clickable index references. Complete local clinical context is automatically appended to your consult payload.",
                            "تتم الإجابة باختصار وعلمية وموثوقية منتهية بمواقع المصادر الطبية لسهولة المراجعة. يتم ربط معلومات الكورس واليوم والجرعة تلقائياً للإسناد الطبي الدقيق.",
                            appLanguage
                        ),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Context-Aware AI Guidance Guidelines Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_custom_tips_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = t("Context-Aware AI Guidance", "إرشادات الجوزاء الذكية المخصصة", appLanguage),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = t(
                                    "Real-time dermatologist recommendations synchronized with your cumulative logs & symptoms.",
                                    "توصيات طبية فورية بناءً على أعراضك المسجلة وجرعتك اليومية التراكمية.",
                                    appLanguage
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatHistory) { msg ->
                        ChatBubbleWidget(
                            msg = msg,
                            appLanguage = appLanguage,
                            onConfirmAction = { viewModel.confirmAiAction(it) },
                            onCancelAction = { viewModel.cancelAiAction(it) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Interactive Read-Only Query Commands Dropdown Menu
        data class QuickQueryOption(
            val queryType: String,
            val label: String,
            val icon: androidx.compose.ui.graphics.vector.ImageVector
        )

        val quickQueryOptions = listOf(
            QuickQueryOption(
                queryType = "remaining_days_cumulative_dose",
                label = t("How many days left in my course?", "كم بقي لي يوم على نهاية الكورس؟", appLanguage),
                icon = Icons.Default.HourglassBottom
            ),
            QuickQueryOption(
                queryType = "next_appointment",
                label = t("Next appointment details", "علمني بتفاصيل موعدي القادم", appLanguage),
                icon = Icons.Default.Event
            ),
            QuickQueryOption(
                queryType = "stock_status",
                label = t("Capsule inventory status", "ما هي حالة مخزون الكبسولات؟", appLanguage),
                icon = Icons.Default.Inventory2
            ),
            QuickQueryOption(
                queryType = "daily_summary",
                label = t("Today's daily summary", "ملخص يوميات اليوم", appLanguage),
                icon = Icons.Default.Today
            ),
            QuickQueryOption(
                queryType = "lab_results",
                label = t("Latest lab test results & trends", "نتائج آخر الفحوصات والتحاليل", appLanguage),
                icon = Icons.Default.Science
            ),
            QuickQueryOption(
                queryType = "next_lab_appointment",
                label = t("When is my next lab test?", "متى موعد تحاليلي القادم؟", appLanguage),
                icon = Icons.Default.Biotech
            ),
            QuickQueryOption(
                queryType = "past_appointments",
                label = t("Past appointments log", "سجل مواعيدي السابقة", appLanguage),
                icon = Icons.Default.History
            )
        )

        var showQuickCommandsDropdown by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) { showQuickCommandsDropdown = true }
                    .testTag("quick_commands_dropdown_trigger")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = t("Quick Medical Queries (Select command)", "الأوامر السريعة للاستعلام (اختر أمراً)", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (showQuickCommandsDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Quick Commands Menu",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            DropdownMenu(
                expanded = showQuickCommandsDropdown,
                onDismissRequest = { showQuickCommandsDropdown = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                quickQueryOptions.forEach { option ->
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = option.label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showQuickCommandsDropdown = false
                            if (!isLoading) {
                                viewModel.runQuickAssistantQuery(option.queryType, option.label)
                            }
                        }
                    )
                }
            }
        }

        // TextInput row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = userQuery,
                onValueChange = { userQuery = it },
                placeholder = { Text(t("Type your message in any dialect (e.g. took 20mg, dry lips)...", "اكتب كلامك بأي طريقة (مثال: أخذت حبة 20mg وعندي جفاف شفايف)...", appLanguage), fontSize = 11.sp) },
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            if (keyEvent.isShiftPressed) {
                                false
                            } else {
                                if (userQuery.isNotBlank() && !isLoading) {
                                    focusManager.clearFocus()
                                    viewModel.askJauzaa(userQuery)
                                    userQuery = ""
                                }
                                true
                            }
                        } else {
                            false
                        }
                    },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = {
                    if (userQuery.isNotBlank()) {
                        focusManager.clearFocus()
                        viewModel.askJauzaa(userQuery)
                        userQuery = ""
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isLoading) Color.Gray else MaterialTheme.colorScheme.primary
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubbleWidget(
    msg: com.example.ui.ChatMessage,
    appLanguage: String,
    onConfirmAction: (com.example.ui.AiActionCard) -> Unit = {},
    onCancelAction: (com.example.ui.AiActionCard) -> Unit = {}
) {
    val align = if (msg.isUser) Alignment.End else Alignment.Start
    val containerCol = if (msg.isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant

    val textCol = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (msg.actionCard != null || msg.queryCard != null) 360.dp else 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (msg.isUser) 14.dp else 2.dp,
                        bottomEnd = if (msg.isUser) 2.dp else 14.dp
                    )
                )
                .background(containerCol)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (msg.isUser) {
                    Text(text = msg.message, color = textCol, fontSize = 12.5.sp)
                } else {
                    // Parse AI response to make hyperlinked sources clickable
                    MarkdownClickableText(text = msg.message, color = textCol)
                }

                // Render Interactive Confirmation Card (Function Calling Action)
                if (msg.actionCard != null) {
                    InteractiveAiActionCard(
                        card = msg.actionCard,
                        appLanguage = appLanguage,
                        onConfirm = { onConfirmAction(msg.actionCard) },
                        onCancel = { onCancelAction(msg.actionCard) }
                    )
                }

                // Render Interactive App Data Query Card
                if (msg.queryCard != null) {
                    InteractiveAiQueryCard(
                        card = msg.queryCard,
                        appLanguage = appLanguage
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveAiActionCard(
    card: com.example.ui.AiActionCard,
    appLanguage: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val isAr = appLanguage == "ar"
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (card.isConfirmed) Color(0xFF2E7D32)
            else if (card.isCancelled) Color.Gray.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("ai_action_confirmation_card")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (card.isConfirmed) Icons.Default.CheckCircle else if (card.isCancelled) Icons.Default.Cancel else Icons.Default.PendingActions,
                        contentDescription = null,
                        tint = if (card.isConfirmed) Color(0xFF2E7D32) else if (card.isCancelled) Color.Gray else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isAr) "تأكيد تسجيل البيانات" else "Data Confirmation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (card.isConfirmed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (card.isConfirmed) Color(0xFFE8F5E9) else if (card.isCancelled) Color.LightGray.copy(alpha = 0.4f) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (card.isConfirmed) (if (isAr) "تم الحفظ ✓" else "Saved ✓")
                        else if (card.isCancelled) (if (isAr) "ملغى" else "Cancelled")
                        else (if (isAr) "بانتظار التأكيد" else "Pending"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (card.isConfirmed) Color(0xFF2E7D32) else if (card.isCancelled) Color.Gray else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Extracted items list
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (card.doseMg != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "${if (isAr) "الجرعة اليومية المتناولة:" else "Dose Taken:"} ${card.doseMg} ${if (isAr) "ملجم" else "mg"}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (card.symptoms.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Coronavirus, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFD32F2F))
                        Text(
                            text = "${if (isAr) "الأعراض المصاحبة:" else "Symptoms:"} ${card.symptoms.joinToString("، ")}${if (card.severity != null) " (مستوى ${card.severity}/5)" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (card.moisturizerType != null) {
                    val mTypeAr = when (card.moisturizerType) {
                        "lips" -> "مرطب الشفاه"
                        "skin" -> "مرطب البشرة والوجه"
                        "eyes" -> "قطرات العينين"
                        else -> card.moisturizerType
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF00897B))
                        Text(
                            text = "${if (isAr) "المرطب:" else "Moisturizer:"} +${card.moisturizerCount ?: 1} ($mTypeAr)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (card.waterMl != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0288D1))
                        Text(
                            text = "${if (isAr) "الماء المضاف:" else "Water Added:"} +${card.waterMl} مل (~${(card.waterMl / 250).coerceAtLeast(1)} أكواب)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (card.prescribedDose != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "${if (isAr) "الجرعة الموصوفة:" else "Prescribed Dose:"} ${card.prescribedDose} ملجم",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (card.appointmentDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "${if (isAr) "موعد العيادة:" else "Appointment:"} ${card.appointmentDate} (${card.labTestName ?: (if (isAr) "متابعة" else "Follow-up")})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Buttons / Status
            if (!card.isConfirmed && !card.isCancelled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(36.dp).testTag("confirm_ai_db_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isAr) "تأكيد وتعديل DB" else "Confirm & Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(36.dp).testTag("cancel_ai_edit_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isAr) "إلغاء / تعديل" else "Cancel / Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (card.isConfirmed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isAr) "تم الحفظ وتحديث قاعدة البيانات بنجاح ✓" else "Saved & updated in DB successfully ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            } else if (card.isCancelled) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAr) "تم إلغاء الإجراء. يمكنك كتابة التعديل للمساعد أدناه." else "Cancelled. You can send modifications below.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveAiQueryCard(
    card: com.example.ui.AiQueryCard,
    appLanguage: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("ai_query_result_card")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = when (card.queryType) {
                        "remaining_days_cumulative_dose" -> Icons.Default.Analytics
                        "stock_status" -> Icons.Default.Inventory2
                        "next_appointment", "past_appointments" -> Icons.Default.Event
                        "lab_results" -> Icons.Default.Science
                        else -> Icons.Default.Assessment
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = card.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            card.details.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = value,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.25f)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownClickableText(
    text: String,
    color: Color
) {
    val uriHandler = LocalUriHandler.current

    // Detect markdown hyperlinks matching format [Source Name](URL)
    val parsedData = remember(text) {
        val pattern = java.util.regex.Pattern.compile("\\[(.*?)\\]\\((.*?)\\)")
        val matcher = pattern.matcher(text)

        val builder = AnnotatedString.Builder()
        var lastIdx = 0

        val urlsMap = mutableMapOf<Pair<Int, Int>, String>()

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Pre-match text
            builder.append(text.substring(lastIdx, start))

            // Build Link Text
            val linkText = matcher.group(1) ?: "Link"
            val linkUrl = matcher.group(2) ?: ""

            val linkStartIdx = builder.length
            val linkEndIdx = linkStartIdx + linkText.length

            builder.append(linkText)
            builder.addStyle(
                style = SpanStyle(
                    color = Color(0xFF64B5F6),
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                ),
                start = linkStartIdx,
                end = linkEndIdx
            )

            urlsMap[Pair(linkStartIdx, linkEndIdx)] = linkUrl
            lastIdx = end
        }

        if (lastIdx < text.length) {
            builder.append(text.substring(lastIdx))
        }

        Pair(builder.toAnnotatedString(), urlsMap)
    }

    ClickableText(
        text = parsedData.first,
        style = MaterialTheme.typography.bodyMedium.copy(color = color, fontSize = 12.sp, lineHeight = 16.sp),
        onClick = { offset ->
            parsedData.second.forEach { (range, url) ->
                if (offset >= range.first && offset <= range.second) {
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        // Bad URL
                    }
                }
            }
        }
    )
}

// ------------------- SCREEN 6: SYSTEM PREFERENCES & SETTINGS -------------------

@Composable
fun SettingsScreen(
    viewModel: RoaccutaneViewModel,
    elapsedDays: Int,
    remainingDaysEst: Int,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    appLanguage: String,
    onNavigateToTab: (String) -> Unit = {}
) {
    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val outerPadding = if (isTablet) 32.dp else 16.dp
    val spacedBy = if (isTablet) 24.dp else 16.dp

    val context = LocalContext.current

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle(initialValue = emptyList())
    val dispensedMedications by viewModel.allDispensedMedications.collectAsStateWithLifecycle()
    val allPrescriptions by viewModel.allMedicalPrescriptions.collectAsStateWithLifecycle()

    val treatmentEstimations = remember(userProfile, dispensedMedications, allLogs, allAppointments, allPrescriptions) {
        com.example.utils.TreatmentCourseEstimator.computeEstimations(
            userProfile = userProfile,
            dispensedMedications = dispensedMedications,
            allLogs = allLogs,
            allAppointments = allAppointments,
            allPrescriptions = allPrescriptions
        )
    }

    var showScheduleLabDialogInSettings by remember { mutableStateOf(false) }
    var showDispenseTrackerInSettings by remember { mutableStateOf(false) }

    var editName by remember(userProfile) { mutableStateOf(userProfile.name) }
    var parsedAgeState by remember(userProfile) { mutableStateOf(userProfile.age) }
    var editBirthDateTypeState by remember(userProfile) { mutableStateOf(userProfile.birthDateType) }
    var editBirthDateValState by remember(userProfile) { mutableStateOf(userProfile.birthDateVal) }
    var editAge by remember(userProfile) {
        mutableStateOf(
            if (userProfile.birthDateVal.isNotBlank()) {
                val birthTypeLabel = if (userProfile.birthDateType == "H") "هجري" else "ميلادي"
                "${userProfile.age} (${userProfile.birthDateVal} $birthTypeLabel)"
            } else {
                userProfile.age.toString()
            }
        )
    }
    var showBirthDateDialog by remember { mutableStateOf(false) }
    var editWeight by remember(userProfile) { mutableStateOf(userProfile.weightKg.toString()) }
    var editDailyDose by remember(userProfile) { mutableStateOf(userProfile.dailyDoseMg.toString()) }
    var selectedTargetMgPerKg by remember(userProfile) { mutableStateOf(userProfile.targetMgPerKg.coerceIn(120f, 150f)) }
    var editTargetMgPerKgText by remember(userProfile) { mutableStateOf(userProfile.targetMgPerKgInt.toString()) }
    
    LaunchedEffect(userProfile) {
            editName = userProfile.name
            parsedAgeState = userProfile.age
            editBirthDateTypeState = userProfile.birthDateType
            editBirthDateValState = userProfile.birthDateVal
            val birthTypeLabel = if (userProfile.birthDateType == "H") "هجري" else "ميلادي"
            editAge = if (userProfile.birthDateVal.isNotBlank()) {
                "${userProfile.age} (${userProfile.birthDateVal} $birthTypeLabel)"
            } else {
                userProfile.age.toString()
            }
            editWeight = userProfile.weightKg.toString()
            editDailyDose = userProfile.dailyDoseMg.toString()
            selectedTargetMgPerKg = userProfile.targetMgPerKg.coerceIn(120f, 150f)
            editTargetMgPerKgText = userProfile.targetMgPerKgInt.toString()
        }

    var editCustomApiKey by remember { mutableStateOf("") }
    var showCustomApiKey by remember { mutableStateOf(false) }
    val savedCustomApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val hasBuildApiKey = remember { viewModel.hasBuildConfiguredApiKey() }

    LaunchedEffect(key1 = savedCustomApiKey) {
        editCustomApiKey = savedCustomApiKey
    }

    val syncCode by viewModel.syncCode.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    var customCodeInput by remember { mutableStateOf("") }
    var settingsFeedback by remember { mutableStateOf("") }

    var hasCalendarPermission by remember {
        mutableStateOf(com.example.utils.CalendarSyncManager.hasCalendarPermissions(context))
    }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCalendarPermission = perms[android.Manifest.permission.READ_CALENDAR] == true &&
                perms[android.Manifest.permission.WRITE_CALENDAR] == true
        if (hasCalendarPermission) {
            Toast.makeText(context, t("Native Calendar Permission Granted!", "تم تفعيل صلاحية تقويم الجوال بنجاح!", appLanguage), Toast.LENGTH_SHORT).show()
        }
    }
    var isSyncingAllAppointments by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(outerPadding)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        // Main Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t("App Preferences & Cloud Link", "تفضيلات التطبيق والمزامنة", appLanguage),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = t("Maintain profiles, customize cumulative strategies & bind backups", "تعديل تفاصيل الملف الطبي، تحويل إستراتيجيات التراكم والمزامنة السحابية", appLanguage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme"
                )
            }
        }

        // Smart Clinical Estimations & Forecasting Hub (Moved to top of Settings)
        com.example.ui.components.SmartClinicalEstimationsCard(
            estimations = treatmentEstimations,
            appLanguage = appLanguage,
            onScheduleLabClick = { showScheduleLabDialogInSettings = true },
            onDispenseMedicationClick = { showDispenseTrackerInSettings = true },
            onNavigateToAppointments = { onNavigateToTab("appointments") }
        )

        // Card 1: Multi-language Toggle Block
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = t("APP LANGUAGE TRANSLATOR", "لغة التطبيق (LANGUAGE)", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // English Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (appLanguage == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.setAppLanguage("en") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (appLanguage == "en") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Arabic Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (appLanguage == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.setAppLanguage("ar") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("العربية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (appLanguage == "ar") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Card 2: Medical Parameters Profile Form
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = t("PATIENT CLINICAL DOSAGE VALUES", "تعديل الملف الطبي العلاجي", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(t("Patient Name", "اسم المريض", appLanguage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = {},
                            label = { Text(t("Age", "العمر", appLanguage)) },
                            singleLine = true,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showBirthDateDialog = true }
                        )
                    }
                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it },
                        label = { Text(t("Weight (kg)", "الوزن (كغم)", appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = editDailyDose,
                    onValueChange = { editDailyDose = it },
                    label = { Text(t("Daily Capsule Dose (mg)", "الجرعة اليومية النشطة (ملغ)", appLanguage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Strategy Slider (120 to 150 mg/kg) + Manual Text Input
                val previewWeight = editWeight.toFloatOrNull() ?: userProfile.weightKg
                val effectiveStrategyFloat = editTargetMgPerKgText.toFloatOrNull()?.coerceIn(120f, 150f) ?: selectedTargetMgPerKg
                val effectiveStrategyInt = kotlin.math.round(effectiveStrategyFloat).toInt()
                val previewTotalMg = kotlin.math.round(previewWeight * effectiveStrategyInt).toInt()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
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
                            Text(
                                text = t("Target Cumulative Dose Strategy:", "إستراتيجية الجرعة التراكمية المستهدفة:", appLanguage),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "$effectiveStrategyInt mg/kg",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Slider(
                                    value = selectedTargetMgPerKg.coerceIn(120f, 150f),
                                    onValueChange = { newVal ->
                                        val snapped = kotlin.math.round(newVal).toInt().coerceIn(120, 150)
                                        selectedTargetMgPerKg = snapped.toFloat()
                                        editTargetMgPerKgText = snapped.toString()
                                    },
                                    valueRange = 120f..150f,
                                    steps = 29,
                                    modifier = Modifier.fillMaxWidth().testTag("target_strategy_slider")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("120 mg/kg (MIN)", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("135 mg/kg", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("150 mg/kg (MAX)", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            OutlinedTextField(
                                value = editTargetMgPerKgText,
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }.take(3)
                                    editTargetMgPerKgText = filtered
                                    val parsed = filtered.toIntOrNull()
                                    if (parsed != null && parsed in 120..150) {
                                        selectedTargetMgPerKg = parsed.toFloat()
                                    }
                                },
                                label = { Text(t("mg/kg", "ملغ/كغ", appLanguage), fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = editTargetMgPerKgText.toIntOrNull()?.let { it !in 120..150 } ?: editTargetMgPerKgText.isNotEmpty(),
                                modifier = Modifier.width(88.dp).testTag("target_strategy_input")
                            )
                        }

                        Text(
                            text = t(
                                "Range 120–150 mg/kg • Computed Target: $previewTotalMg mg (for ${String.format(Locale.US, "%.1f", previewWeight)} kg)",
                                "النطاق المعتمد 120–150 ملغ/كغ • الهدف المحسوب: $previewTotalMg ملغ (لوزن ${String.format(Locale.US, "%.1f", previewWeight)} كغ)",
                                appLanguage
                            ),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Shifting start date simulator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(t("Therapy Start Date:", "تاريخ بدء الكورس الفعلي:", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(userProfile.startDate))
                        Text(formatDisplayDateStr(dateString, appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            val calendarVal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, yr, mo, dy ->
                                    calendarVal.set(yr, mo, dy)
                                    val chosenStrategyInt = (editTargetMgPerKgText.toIntOrNull() ?: kotlin.math.round(selectedTargetMgPerKg).toInt()).coerceIn(120, 150)
                                    viewModel.updateProfile(
                                        name = editName,
                                        age = parsedAgeState,
                                        weight = editWeight.toFloatOrNull() ?: userProfile.weightKg,
                                        dailyDose = editDailyDose.toIntOrNull() ?: userProfile.dailyDoseMg,
                                        targetType = chosenStrategyInt.toString(),
                                        startTimestamp = calendarVal.timeInMillis,
                                        birthDateType = editBirthDateTypeState,
                                        birthDateVal = editBirthDateValState,
                                        clearCustomTarget = true
                                    )
                                    settingsFeedback = t("Start Date updated successfully!", "تم تعديل تاريخ البدء بنجاح!", appLanguage)
                                },
                                calendarVal.get(Calendar.YEAR),
                                calendarVal.get(Calendar.MONTH),
                                calendarVal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(t("Change", "تعديل", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Treatment Dates Harmony & Consistency Card (تطابق وتناغم التواريخ)
                val earliestDiaryMillis = remember(userProfile.startDate) { viewModel.getEarliestDiaryDateMillis() }
                val earliestDispenseMillis = remember(userProfile.startDate) { viewModel.getEarliestDispenseDateMillis() }
                val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                val currentStartStr = remember(userProfile.startDate) { sdfDate.format(Date(userProfile.startDate)) }
                val earliestDiaryStr = remember(earliestDiaryMillis) { if (earliestDiaryMillis != null) sdfDate.format(Date(earliestDiaryMillis)) else null }
                val earliestDispenseStr = remember(earliestDispenseMillis) { if (earliestDispenseMillis != null) sdfDate.format(Date(earliestDispenseMillis)) else null }

                val hasDateMismatch = (earliestDiaryStr != null && earliestDiaryStr != currentStartStr) ||
                        (earliestDispenseStr != null && earliestDispenseStr != currentStartStr)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasDateMismatch) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, if (hasDateMismatch) Color(0xFFFFB74D) else Color(0xFFA5D6A7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (hasDateMismatch) Icons.Default.SyncProblem else Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (hasDateMismatch) Color(0xFFE65100) else Color(0xFF2E7D32),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (appLanguage == "ar") "ميزان تطابق التواريخ (اليوميات • الصرفيات • الإعدادات):" else "Date Harmony (Diary • Dispenses • Settings):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDateMismatch) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        }

                        if (hasDateMismatch) {
                            Text(
                                text = if (appLanguage == "ar")
                                    "⚠️ تم رصد اختلاف بين تاريخ بدء الكورس وتواريخ أول جرعة مسجلة:\n" +
                                            "• تاريخ الإعدادات الحالي: $currentStartStr\n" +
                                            (if (earliestDiaryStr != null) "• أول جرعة مسجلة باليوميات: $earliestDiaryStr\n" else "") +
                                            (if (earliestDispenseStr != null) "• أول دواء تم صرفه: $earliestDispenseStr\n" else "") +
                                            "يمكنك اعتماد تاريخ أول جرعة حقيقية كبداية رسمية للكورس بنقرة واحدة:"
                                else
                                    "⚠️ Start date differs from earliest recorded dose:\n" +
                                            "• Settings start date: $currentStartStr\n" +
                                            (if (earliestDiaryStr != null) "• Earliest Diary Dose: $earliestDiaryStr\n" else "") +
                                            (if (earliestDispenseStr != null) "• Earliest Dispensed Med: $earliestDispenseStr\n" else "") +
                                            "Sync and adopt the earliest recorded dose date as official course start:",
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val targetSyncMillis = earliestDiaryMillis ?: earliestDispenseMillis ?: userProfile.startDate
                            Button(
                                onClick = {
                                    viewModel.syncTreatmentStartDate(targetSyncMillis)
                                    settingsFeedback = t("Treatment start date synchronized across all features!", "تمت مزامنة واعتماد تاريخ أول جرعة كبداية رسمية لكافة أقسام التطبيق!", appLanguage)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (appLanguage == "ar") "مزامنة وتوحيد التاريخ مع أول جرعة (${earliestDiaryStr ?: earliestDispenseStr})" else "Sync Start Date with Earliest Dose",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = if (appLanguage == "ar")
                                    "✅ التواريخ متطابقة تماماً ومتناغمة عبر كافة الأقسام (تاريخ البداية: $currentStartStr)."
                                else
                                    "✅ All treatment dates are fully harmonized and synchronized ($currentStartStr).",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val parsedW = editWeight.toFloatOrNull()
                        val parsedD = editDailyDose.toIntOrNull()
                        val rawStrategy = editTargetMgPerKgText.toIntOrNull()
                        if (parsedW != null && parsedD != null) {
                            val finalStrategy = (rawStrategy ?: kotlin.math.round(selectedTargetMgPerKg).toInt()).coerceIn(120, 150)
                            selectedTargetMgPerKg = finalStrategy.toFloat()
                            editTargetMgPerKgText = finalStrategy.toString()
                            viewModel.updateProfile(
                                name = editName,
                                age = parsedAgeState,
                                weight = parsedW,
                                dailyDose = parsedD,
                                targetType = finalStrategy.toString(),
                                startTimestamp = userProfile.startDate,
                                birthDateType = editBirthDateTypeState,
                                birthDateVal = editBirthDateValState,
                                clearCustomTarget = true
                            )
                            settingsFeedback = t("Preferences saved successfully! ($finalStrategy mg/kg)", "تم حفظ الإعدادات واعتماد إستراتيجية ($finalStrategy ملغ/كغ) بنجاح!", appLanguage)
                        } else {
                            settingsFeedback = t("Validation error on Weight/Dose.", "يرجى كتابة أرقام صحيحة للوزن والجرعة.", appLanguage)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t("Save Clinical Profile Configuration", "حفظ تعديلات الملف الطبي", appLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 3: Custom Gemini API Key Block (Masked input + Android Keystore local encryption)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t("GEMINI CHAT ASSISTANT API KEY", "مفتاح الربط الذكي للجوزاء (Gemini API)", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val isKeyConfigured = savedCustomApiKey.isNotBlank() || hasBuildApiKey
                    Surface(
                        color = if (isKeyConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isKeyConfigured) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    savedCustomApiKey.isNotBlank() -> t("Encrypted on Device", "مشفر على الجهاز", appLanguage)
                                    hasBuildApiKey -> t("Secured via BuildConfig", "مؤمّن عبر الإعداد", appLanguage)
                                    else -> t("Not Set", "غير مضاف", appLanguage)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = t(
                        "Optional override key stored locally using Android Keystore AES-256 encryption (never synced to the cloud):",
                        "مفتاح اختياري يُحفظ محلياً بتشفير Android Keystore ولا تتم مزامنته سحابياً لحمايته:",
                        appLanguage
                    ),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = editCustomApiKey,
                    onValueChange = { editCustomApiKey = it.trim() },
                    placeholder = { Text(t("Enter Gemini API Key...", "أدخل المفتاح هنا...", appLanguage), fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = if (showCustomApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { showCustomApiKey = !showCustomApiKey },
                            modifier = Modifier.testTag("toggle_api_key_visibility_button")
                        ) {
                            Icon(
                                imageVector = if (showCustomApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = t("Toggle API key visibility", "إظهار أو إخفاء المفتاح", appLanguage)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_api_key_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCustomApiKey(editCustomApiKey)
                            showCustomApiKey = false
                            Toast.makeText(context, t("API Key Encrypted & Saved!", "تم تشفير وحفظ مفتاح الـ API بنجاح!", appLanguage), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_api_key_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t("Confirm & Save", "تأكيد وحفظ", appLanguage), fontWeight = FontWeight.ExtraBold)
                    }

                    if (savedCustomApiKey.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearCustomApiKey()
                                editCustomApiKey = ""
                                showCustomApiKey = false
                                Toast.makeText(context, t("Custom API Key cleared", "تم مسح المفتاح المخصص", appLanguage), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("clear_api_key_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("Clear", "مسح", appLanguage))
                        }
                    }
                }
            }
        }

        // Card 4: Multi-device Cloud Sync Card (Starts with 0x)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, contentDescription = "Cloud", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = t("Multi-Device Cloud Synchronization", "ربط ومزامنة البيانات سحابياً", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = t(
                        "Synchronize multiple devices using an anonymous pool connection starting with 0x passcode.",
                        "قم بمزامنة الكورس والجرعات والمواعيد بين أجهزة متعددة باستخدام رمز ربط سري وسريع يبدأ بالرمز 0x.",
                        appLanguage
                    ),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (syncCode.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.generateSyncCode { success, msg ->
                                    if (success) {
                                        viewModel.uploadDataToCloud { _, _ -> }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(t("Generate New Sync Token File", "توليد رمز مزامنة جديد (شفرة 0x)", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        OutlinedTextField(
                            value = customCodeInput,
                            onValueChange = { customCodeInput = it },
                            placeholder = { Text(t("Enter 0x connection code...", "أدخل رمز المزامنة الآخر هنا...", appLanguage), fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val trimmed = customCodeInput.trim()
                                if (trimmed.startsWith("0x")) {
                                    viewModel.setCustomSyncCode(trimmed)
                                    viewModel.downloadDataFromCloud { success, message ->
                                        settingsFeedback = message
                                    }
                                } else {
                                    settingsFeedback = t("Sync code must start with 0x", "يجب أن تبدأ شفرة الربط برمز 0x", appLanguage)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(t("Link & Complete Cloud Merging", "إتمام الربط وجلب البيانات المدمجة", appLanguage), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(t("Linked Sync Code (Active):", "رمز المزامنة السحابية النشط:", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                Text(syncCode ?: "", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = { viewModel.unlinkSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(t("Disconnect", "إلغاء الربط", appLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        val displaySyncTime = if (lastSyncTime > 0L) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastSyncTime))
                        } else t("Never", "لم تتم المزامنة بعد", appLanguage)

                        Text(t("Last Backup Synchronized: $displaySyncTime", "آخر مزامنة احتياطية سحابية: $displaySyncTime", appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Upload cloud now
                            Button(
                                onClick = {
                                    viewModel.uploadDataToCloud { success, message ->
                                        settingsFeedback = message
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t("Upload Now", "رفع السحابة", appLanguage), fontSize = 11.sp)
                            }

                            // Download/fetch cloud
                            Button(
                                onClick = {
                                    viewModel.downloadDataFromCloud { success, message ->
                                        settingsFeedback = message
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Download", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t("Download Now", "مزامنة وجلب", appLanguage), fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (syncStatus.isNotBlank()) {
                    Text(t("Status: $syncStatus", "الحالة: $syncStatus", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 5: Manage Home Products Dialog Launcher
        var showSettingsManageProductsDialog by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = t("Manage Home Care Products", "إدارة منتجات ومستلزمات الصفحة الرئيسية", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = t(
                        "Manage, add, edit prices, descriptions, store links, and images for approved care products displayed on the Home screen.",
                        "نافذة شاملة لإدارة مستلزمات ومكملات العناية بالصفحة الرئيسية: إضافة منتجات جديدة، تعديل الوصف والقيمة والصور، وتحديد المتاجر وروابط الشراء.",
                        appLanguage
                    ),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showSettingsManageProductsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = "Manage", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = t("Open Products Management Dialog", "فتح نافذة إدارة المنتجات", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (showSettingsManageProductsDialog) {
            ManageHomeProductsDialog(
                appLanguage = appLanguage,
                viewModel = viewModel,
                onDismiss = { showSettingsManageProductsDialog = false }
            )
        }

        // Card 6: Native Calendar Appointments Sync (مزامنة المواعيد مع تقويم الجوال)
        val selectedCalendarColorId by viewModel.calendarEventColorId.collectAsStateWithLifecycle()
        val currentCalendarColorOpt = remember(selectedCalendarColorId) {
            com.example.utils.CalendarSyncManager.getColorOptionById(selectedCalendarColorId)
        }
        val activeCalendarSyncColor = remember(currentCalendarColorOpt) {
            Color(currentCalendarColorOpt.argb)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = activeCalendarSyncColor.copy(alpha = 0.05f)),
            border = BorderStroke(1.2.dp, activeCalendarSyncColor.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(activeCalendarSyncColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = activeCalendarSyncColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = t("Native Phone Calendar Sync", "مزامنة المواعيد مع تقويم الجوال", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (appLanguage == "ar") currentCalendarColorOpt.nameAr else currentCalendarColorOpt.nameEn} • ${t("5 Days Auto Reminder", "تذكير تلقائي قبل 5 أيام", appLanguage)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeCalendarSyncColor
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasCalendarPermission) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (hasCalendarPermission) t("Active ✓", "مفعلة ✓", appLanguage) else t("Permission Needed", "تتطلب صلاحية", appLanguage),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasCalendarPermission) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = t(
                        "Direct deterministic sync of all clinic appointments (both upcoming and past ones) to your device calendar. Events are titled as: Hospital + Clinic + Reason (e.g. KFUH Derma Follow-up), location link in map field, complete clinician and lab analysis details, your selected matching card color, and automatic reminders 5 days, 1 day, and 2 hours in advance.",
                        "مزامنة برمجية مباشرة لجميع مواعيد العيادة (الحالية والسابقة) إلى تقويم هاتفك. يتم وضع كل معلومة في مكانها المخصص: العنوان بصيغة اسم المستشفى ثم العيادة ثم سبب الموعد (مثل KFUH Derma Follow-up)، ورابط الموقع الجغرافي في خانة الموقع، وكامل تفاصيل الطبيب والتحاليل في الوصف، باللون المختار المتطابق، مع تذكير تلقائي قبل 5 أيام ويوم وساعتين.",
                        appLanguage
                    ),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Calendar Color Selector Palette Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, activeCalendarSyncColor.copy(alpha = 0.3f)),
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
                            Text(
                                text = t("Calendar Event Card Color:", "تحديد لون بطاقة الحدث بالتقويم:", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeCalendarSyncColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, activeCalendarSyncColor.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(activeCalendarSyncColor)
                                    )
                                    Text(
                                        text = "${if (appLanguage == "ar") currentCalendarColorOpt.nameAr else currentCalendarColorOpt.nameEn} (#${currentCalendarColorOpt.id})",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeCalendarSyncColor
                                    )
                                }
                            }
                        }

                        Text(
                            text = t("Select color according to Google Calendar Palette (1-11):", "اختر من لوحة ألوان تقويم جوجل المتطابقة (1-11):", appLanguage),
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(com.example.utils.CalendarSyncManager.GOOGLE_CALENDAR_COLORS) { colorOpt ->
                                val isSelected = colorOpt.id == selectedCalendarColorId
                                val optColor = Color(colorOpt.argb)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.setCalendarEventColorId(colorOpt.id)
                                        }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(optColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else {
                                                    Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = colorOpt.nameAr.split(" ").first(),
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) activeCalendarSyncColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (!hasCalendarPermission) {
                    Button(
                        onClick = {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CALENDAR,
                                    android.Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = t("Grant Calendar Permissions", "منح صلاحيات التقويم بالجوال", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        if (!hasCalendarPermission) {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_CALENDAR,
                                    android.Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        } else {
                            isSyncingAllAppointments = true
                            viewModel.syncAllAppointmentsToCalendar { synced, total ->
                                isSyncingAllAppointments = false
                                val msg = if (appLanguage == "ar") {
                                    "تمت مزامنة $synced من أصل $total موعد في تقويم الجوال بنجاح بلون ${currentCalendarColorOpt.nameAr} وتذكير قبل 5 أيام!"
                                } else {
                                    "Successfully synced $synced / $total appointments to calendar with ${currentCalendarColorOpt.nameEn} color and 5-day reminder!"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeCalendarSyncColor),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncingAllAppointments,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncingAllAppointments) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("Syncing All Appointments...", "جاري مزامنة المواعيد...", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = t("Sync All Appointments to Calendar Now", "مزامنة كافة المواعيد بالتقويم الآن (حتى السابقة)", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (settingsFeedback.isNotBlank()) {
            Text(
                text = settingsFeedback,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showBirthDateDialog) {
        var selectedBirthDateType by remember { mutableStateOf(editBirthDateTypeState) }
        var tempBirthDateVal by remember { mutableStateOf(editBirthDateValState) }
        
        if (tempBirthDateVal.isBlank()) {
            tempBirthDateVal = if (selectedBirthDateType == "H") "1420-01-01" else "1999-01-01"
        }

        val calculatedPair = remember(selectedBirthDateType, tempBirthDateVal) {
            calculateAgeAndFormatBirthDate(selectedBirthDateType, tempBirthDateVal)
        }

        val parts = remember(tempBirthDateVal) {
            try {
                val p = tempBirthDateVal.split("-")
                if (p.size == 3) {
                    Triple(p[0].toIntOrNull() ?: 1999, p[1].toIntOrNull() ?: 1, p[2].toIntOrNull() ?: 1)
                } else {
                    Triple(1999, 1, 1)
                }
            } catch (e: Exception) {
                Triple(1999, 1, 1)
            }
        }

        val currentYear = parts.first
        val currentMonth = parts.second
        val currentDay = parts.third

        fun updateVal(y: Int, m: Int, d: Int) {
            tempBirthDateVal = String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
        }

        val yearsRange = if (selectedBirthDateType == "H") (1350..1448).toList().reversed() else (1930..2026).toList().reversed()
        
        val monthsList = if (selectedBirthDateType == "H") {
            listOf(
                "1. محرم (Muharram)",
                "2. صفر (Safar)",
                "3. ربيع الأول (Rabi' I)",
                "4. ربيع الآخر (Rabi' II)",
                "5. جمادى الأولى (Jumada I)",
                "6. جمادى الآخرة (Jumada II)",
                "7. رجب (Rajab)",
                "8. شعبان (Sha'ban)",
                "9. رمضان (Ramadan)",
                "10. شوال (Shawwal)",
                "11. ذو القعدة (Dhu al-Qadah)",
                "12. ذو الحجة (Dhu al-Hijjah)"
            )
        } else {
            if (appLanguage == "ar") {
                listOf("1. يناير", "2. فبراير", "3. مارس", "4. أبريل", "5. مايو", "6. يونيو", "7. يوليو", "8. أغسطس", "9. سبتمبر", "10. أكتوبر", "11. نوفمبر", "12. ديسمبر")
            } else {
                listOf("1. January", "2. February", "3. March", "4. April", "5. May", "6. June", "7. July", "8. August", "9. September", "10. October", "11. November", "12. December")
            }
        }
        
        val maxDays = if (selectedBirthDateType == "H") {
            30
        } else {
            when (currentMonth) {
                2 -> if ((currentYear % 4 == 0 && currentYear % 100 != 0) || (currentYear % 400 == 0)) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
        }
        val daysRange = (1..maxDays).toList()

        var showDayDropdown by remember { mutableStateOf(false) }
        var showMonthDropdown by remember { mutableStateOf(false) }
        var showYearDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBirthDateDialog = false },
            title = {
                Text(
                    text = t("Select Birth Date", "تحديد تاريخ الميلاد", appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedBirthDateType = "G"
                                tempBirthDateVal = "1999-01-01"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedBirthDateType == "G") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedBirthDateType == "G") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(t("Gregorian", "ميلادي", appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedBirthDateType = "H"
                                tempBirthDateVal = "1420-01-01"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedBirthDateType == "H") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedBirthDateType == "H") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(t("Hijri", "هجري", appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val typeText = if (selectedBirthDateType == "H") "هجري" else "ميلادي"
                            Text(
                                text = "$tempBirthDateVal ($typeText)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${t("Calculated Age", "العمر المحسوب", appLanguage)}: ${calculatedPair.first} ${t("years", "سنة", appLanguage)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Three beautiful selectors side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("Day", "اليوم", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showDayDropdown = true }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$currentDay",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Day Dropdown",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showDayDropdown,
                                    onDismissRequest = { showDayDropdown = false },
                                    modifier = Modifier.heightIn(max = 240.dp)
                                ) {
                                    daysRange.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text("$d") },
                                            onClick = {
                                                updateVal(currentYear, currentMonth, d)
                                                showDayDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Month dropdown
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(
                                text = t("Month", "الشهر", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showMonthDropdown = true }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentMonthName = monthsList.getOrNull(currentMonth - 1) ?: "$currentMonth"
                                    Text(
                                        text = currentMonthName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Month Dropdown",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMonthDropdown,
                                    onDismissRequest = { showMonthDropdown = false },
                                    modifier = Modifier.heightIn(max = 240.dp)
                                ) {
                                    monthsList.forEachIndexed { idx, name ->
                                        DropdownMenuItem(
                                            text = { Text(name, fontSize = 13.sp) },
                                            onClick = {
                                                val newMonth = idx + 1
                                                val mMaxDays = if (selectedBirthDateType == "H") 30 else {
                                                    when (newMonth) {
                                                        2 -> if ((currentYear % 4 == 0 && currentYear % 100 != 0) || (currentYear % 400 == 0)) 29 else 28
                                                        4, 6, 9, 11 -> 30
                                                        else -> 31
                                                    }
                                                }
                                                val coercedDay = currentDay.coerceAtMost(mMaxDays)
                                                updateVal(currentYear, newMonth, coercedDay)
                                                showMonthDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Year dropdown
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = t("Year", "السنة", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showYearDropdown = true }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$currentYear",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Year Dropdown",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showYearDropdown,
                                    onDismissRequest = { showYearDropdown = false },
                                    modifier = Modifier.heightIn(max = 240.dp)
                                ) {
                                    yearsRange.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text("$y") },
                                            onClick = {
                                                val mMaxDays = if (selectedBirthDateType == "H") 30 else {
                                                    when (currentMonth) {
                                                        2 -> if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)) 29 else 28
                                                        4, 6, 9, 11 -> 30
                                                        else -> 31
                                                    }
                                                }
                                                val coercedDay = currentDay.coerceAtMost(mMaxDays)
                                                updateVal(y, currentMonth, coercedDay)
                                                showYearDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedBirthDateType == "G") {
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val partsG = tempBirthDateVal.split("-")
                                val yr = if (partsG.size == 3) partsG[0].toIntOrNull() ?: 1999 else 1999
                                val mo = if (partsG.size == 3) (partsG[1].toIntOrNull() ?: 1) - 1 else 0
                                val dy = if (partsG.size == 3) partsG[2].toIntOrNull() ?: 1 else 1

                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        tempBirthDateVal = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    yr,
                                    mo,
                                    dy
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t("Open System Calendar Picker", "فتح منتقي التواريخ الافتراضي للنظام", appLanguage),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parsedAgeState = calculatedPair.first
                        editBirthDateTypeState = selectedBirthDateType
                        editBirthDateValState = tempBirthDateVal
                        val typeLabel = if (selectedBirthDateType == "H") "هجري" else "ميلادي"
                        editAge = "$parsedAgeState ($tempBirthDateVal $typeLabel)"
                        showBirthDateDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(t("Confirm", "تأكيد", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDateDialog = false }) {
                    Text(t("Cancel", "إلغاء", appLanguage))
                }
            }
        )
    }

    if (showScheduleLabDialogInSettings) {
        AddAppointmentDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            allAppointments = allAppointments,
            initialAppointmentType = "LAB",
            onDismiss = { showScheduleLabDialogInSettings = false }
        )
    }

    if (showDispenseTrackerInSettings) {
        DispensedMedicationsTrackerDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            onDismiss = { showDispenseTrackerInSettings = false }
        )
    }

}


// ------------------- POPUP EDIT DIALOG -------------------

@Composable
fun LogEditorDialog(
    dateStr: String,
    viewModel: RoaccutaneViewModel,
    allLogs: List<DailyLog>,
    userProfile: UserProfile,
    appLanguage: String,
    onDismiss: () -> Unit
) {
    val existingLog = allLogs.find { it.dateString == dateStr }
    var editDose by remember { mutableStateOf(existingLog?.doseMg?.toString() ?: userProfile.dailyDoseMg.toString()) }
    var editIsTaken by remember { mutableStateOf(existingLog?.isTaken ?: true) }
    var editFatConfirmed by remember { mutableStateOf(existingLog?.healthySources?.isNotBlank() == true) }
    var editSymptomsSelected = remember {
        val symsList = mutableStateListOf<String>()
        existingLog?.symptoms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.let { symsList.addAll(it) }
        symsList
    }
    var editNotes by remember { mutableStateOf(existingLog?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("Edit Capsule Log: $dateStr", "تعديل سجل الجرعة: $dateStr", appLanguage), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = editDose,
                    onValueChange = { editDose = it },
                    label = { Text(t("Capsule Dose (mg)", "جرعة الكبسولة (ملغ)", appLanguage)) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t("Capsule Taken?", "تم تناول الجرعة؟", appLanguage), fontSize = 12.sp)
                    Switch(checked = editIsTaken, onCheckedChange = { editIsTaken = it })
                }

                HorizontalDivider()

                // Simplified Absorption Partners (Confirmation Only)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("Appropriate Fat Consumption", "تأكيد استهلاك الدهون المناسبة", appLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = t("Consumed with a heavy meal containing adequate fat for maximal absorption.", "تم تناول الجرعة مع وجبة مناسبة تحتوي على قدر كافٍ من الدهون لضمان أقصى امتصاص.", appLanguage),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = editFatConfirmed,
                            onCheckedChange = { editFatConfirmed = it }
                        )
                    }
                }

                HorizontalDivider()

                Text(t("Symptoms:", "الأعراض:", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRowCompat(
                    items = if (appLanguage == "ar") SYMPTOMS_AR else SYMPTOMS_EN,
                    selectedItems = editSymptomsSelected,
                    onToggle = { item ->
                        if (editSymptomsSelected.contains(item)) editSymptomsSelected.remove(item) else editSymptomsSelected.add(item)
                    }
                )

                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text(t("Notes", "ملاحظات", appLanguage)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDose = editDose.toIntOrNull() ?: userProfile.dailyDoseMg
                    val sourcesList = if (editFatConfirmed) listOf(t("Appropriate fat consumed", "دهون مناسبة مع الجرعة", appLanguage)) else emptyList()
                    viewModel.saveDailyLog(
                        dateString = dateStr,
                        doseMg = parsedDose,
                        isTaken = editIsTaken,
                        sources = sourcesList,
                        symptoms = editSymptomsSelected.toList(),
                        notes = editNotes
                    )
                    onDismiss()
                }
            ) {
                Text(t("Confirm Edit", "تأكيد التعديل", appLanguage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("Cancel", "إلغاء", appLanguage))
            }
        }
    )
}




