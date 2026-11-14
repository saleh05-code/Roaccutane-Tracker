package com.example.data

import android.util.Log
import com.example.ui.AiActionCard
import com.example.ui.AiQueryCard
import com.example.ui.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Interactive Two-Way AI Assistant Service for Roaccutane Tracker.
 * Implements bidirectional Function Calling with Gemini API:
 * 1. Understands natural language queries & dialect.
 * 2. Emits JSON Function Calls.
 * 3. Halts critical DB actions behind an interactive confirmation card.
 * 4. Executes app data queries immediately with clinical precision.
 */
object RoaccutaneAiTwoWayService {
    private const val TAG = "RoaccutaneAiTwoWay"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    const val SYSTEM_INSTRUCTION = """
أنت مساعد شخصي ذكي مدمج داخل تطبيق تتبع علاج الروكتان (Roaccutane Tracker).
مهامك الأساسية:
1. فهم كلام المستخدم بأي أسلوب أو لهجة (عامية، فصحى، إنجليزية) وتحديد الإجراء المطلوب بدقة.
2. استخدام الـ Tools / Functions المتاحة لتوليد الأوامر البرمجية المناسبة بصيغة JSON.
3. التحدث دائماً بنبرة داعمة ومباشرة، وإيقاف تنفيذ أي إجراء حرج حتى يعاين المستخدم البيانات ويؤكدها في بطاقة التأكيد.
4. بالنسبة لطلبات الاستعلام (عرض بيانات)، قم باستدعاء الدالة get_app_data فوراً بالـ query_type المناسب لجلب البيانات وعرضها.
5. إذا ذكر المستخدم في جملة واحدة تسجيل جرعة + أعراض + مرطب + ماء، استخرج جميع هذه المعطيات بدقة في استدعاءات الوظائف المناسبة.
6. إذا لم يكن الطلب إجراءً تسجيلياً أو استعلامياً، أجب على المستخدم مباشرة بنصيحة طبية مختصرة وداعمة مع ذكر المصادر الطبية المعتمدة.
"""

    fun getToolsDeclarationJson(): JSONArray {
        val toolsArray = JSONArray()

        // 1. log_daily_dose_and_symptoms
        val logDoseFunc = JSONObject().apply {
            put("name", "log_daily_dose_and_symptoms")
            put("description", "تسجيل الجرعة اليومية والأعراض المصاحبة")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("dose_taken_mg", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "الجرعة بالملجم مثل 20 أو 40")
                    })
                    put("taken_at", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "وقت تناول الجرعة ISO format أو HH:MM")
                    })
                    put("symptoms", JSONObject().apply {
                        put("type", "ARRAY")
                        put("items", JSONObject().apply { put("type", "STRING") })
                        put("description", "قائمة الأعراض مثل: جفاف شفايف، صداع، خمول")
                    })
                    put("severity_1_to_5", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "شدة الأعراض من 1 إلى 5")
                    })
                })
                put("required", JSONArray().apply { put("dose_taken_mg") })
            })
        }

        // 2. update_trackers
        val updateTrackersFunc = JSONObject().apply {
            put("name", "update_trackers")
            put("description", "تحديث عداد الماء أو المرطبات (بشرة، شفايف، عينين)")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("water_ml", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "كمية الماء المضافة بالمليلتر")
                    })
                    put("moisturizer_type", JSONObject().apply {
                        put("type", "STRING")
                        put("enum", JSONArray().apply {
                            put("lips")
                            put("skin")
                            put("eyes")
                        })
                        put("description", "نوع المرطب")
                    })
                    put("count_increment", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "عدد مرات استخدام المرطب")
                    })
                })
            })
        }

        // 3. manage_prescription_and_stock
        val manageStockFunc = JSONObject().apply {
            put("name", "manage_prescription_and_stock")
            put("description", "تحديث الوصفات والكبسولات المتبقية والمنتجات")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("prescribed_dose", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "الجرعة اليومية الموصوفة بالملجم")
                    })
                    put("capsules_dispensed", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "عدد الكبسولات المصروفة")
                    })
                    put("capsules_remaining", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "عدد الكبسولات المتبقية")
                    })
                    put("product_action", JSONObject().apply {
                        put("type", "STRING")
                        put("enum", JSONArray().apply {
                            put("add")
                            put("remove")
                        })
                    })
                    put("product_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "اسم المنتج")
                    })
                })
            })
        }

        // 4. manage_appointments_and_labs
        val manageApptsFunc = JSONObject().apply {
            put("name", "manage_appointments_and_labs")
            put("description", "إدارة المواعيد والتحاليل المخبرية")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appointment_date", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "تاريخ الموعد YYYY-MM-DD")
                    })
                    put("lab_test_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "اسم الفحص مثل إنزيمات كبد أو دهون")
                    })
                    put("lab_result_value", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "قيمة نتيجة التحليل")
                    })
                })
            })
        }

        // 5. get_app_data
        val getAppDataFunc = JSONObject().apply {
            put("name", "get_app_data")
            put("description", "استعلام وجلب بيانات من التطبيق لعرضها للمستخدم")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query_type", JSONObject().apply {
                        put("type", "STRING")
                        put("enum", JSONArray().apply {
                            put("next_appointment")
                            put("next_lab_appointment")
                            put("past_appointments")
                            put("lab_results")
                            put("remaining_days_cumulative_dose")
                            put("stock_status")
                            put("daily_summary")
                            put("linked_account_status")
                        })
                        put("description", "نوع البيانات المطلوب عرضها")
                    })
                })
                put("required", JSONArray().apply { put("query_type") })
            })
        }

        val funcsArray = JSONArray().apply {
            put(logDoseFunc)
            put(updateTrackersFunc)
            put(manageStockFunc)
            put(manageApptsFunc)
            put(getAppDataFunc)
        }

        val toolObj = JSONObject().apply {
            put("functionDeclarations", funcsArray)
        }

        toolsArray.put(toolObj)
        return toolsArray
    }

    suspend fun processAssistantTurn(
        apiKey: String,
        userQuery: String,
        chatHistory: List<ChatMessage>,
        userProfile: UserProfile,
        allLogs: List<DailyLog>,
        cumulativeDose: Int,
        inventoryAudit: RoaccutaneInventoryAudit?,
        allAppointments: List<Appointment>,
        allPrescriptions: List<MedicalPrescription>,
        appLanguage: String,
        activeAccount: LinkedAccount? = null
    ): ChatMessage {
        // Deterministic fast-path for exact read-only clinical queries (ensures 100% accurate live DB metrics & lab trends)
        val deterministicQuery = matchDeterministicQueryType(userQuery)
        if (deterministicQuery != null) {
            return executeAppDataQuery(
                queryType = deterministicQuery,
                userProfile = userProfile,
                allLogs = allLogs,
                cumulativeDose = cumulativeDose,
                inventoryAudit = inventoryAudit,
                allAppointments = allAppointments,
                allPrescriptions = allPrescriptions,
                appLanguage = appLanguage,
                activeAccount = activeAccount
            )
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return processOfflineIntent(
                userQuery = userQuery,
                userProfile = userProfile,
                allLogs = allLogs,
                cumulativeDose = cumulativeDose,
                inventoryAudit = inventoryAudit,
                allAppointments = allAppointments,
                allPrescriptions = allPrescriptions,
                appLanguage = appLanguage,
                activeAccount = activeAccount
            )
        }

        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-3.5-flash")

        // Build Gemini Request Payload
        val payload = JSONObject()

        // 1. System Instruction with Active Linked Account Awareness
        val accountContext = if (activeAccount != null) {
            "\n[حساب المستخدم الموثق حالياً: البريد الإلكتروني=${activeAccount.email}، نوع الحساب=${if (activeAccount.providerType == "GOOGLE") "حساب جوجل (Google Account)" else "بريد إلكتروني مخصص"}]. عندما يسألك المستخدم عن حسابه أو بياناته المرتبطة أخبره بهذه التفاصيل."
        } else ""

        val sysInstObj = JSONObject().apply {
            val parts = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", SYSTEM_INSTRUCTION + accountContext)
                })
            }
            put("parts", parts)
        }
        payload.put("systemInstruction", sysInstObj)

        // 2. Tools
        payload.put("tools", getToolsDeclarationJson())

        // 3. Contents
        val contentsArray = JSONArray()
        // Include latest turns for context
        val recentHistory = chatHistory.takeLast(6)
        recentHistory.forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            val turnObj = JSONObject().apply {
                put("role", role)
                val parts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", msg.message)
                    })
                }
                put("parts", parts)
            }
            contentsArray.put(turnObj)
        }

        // Add current user prompt
        val currentPromptObj = JSONObject().apply {
            put("role", "user")
            val parts = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", userQuery)
                })
            }
            put("parts", parts)
        }
        contentsArray.put(currentPromptObj)
        payload.put("contents", contentsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)

        var lastException: Exception? = null

        for (model in models) {
            try {
                Log.d(TAG, "Calling Gemini ($model) with Function Calling...")
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseStr = response.body?.string()

                if (!response.isSuccessful || responseStr.isNullOrBlank()) {
                    Log.w(TAG, "Model $model returned error ${response.code}: $responseStr")
                    lastException = Exception("HTTP ${response.code}: $responseStr")
                    continue
                }

                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    continue
                }

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue

                // Analyze parts for functionCall or text
                var functionCallObj: JSONObject? = null
                val combinedText = StringBuilder()

                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("functionCall")) {
                        functionCallObj = p.getJSONObject("functionCall")
                    }
                    if (p.has("text")) {
                        combinedText.append(p.getString("text")).append("\n")
                    }
                }

                // If no native functionCall found, check if text has embedded JSON function call
                if (functionCallObj == null && combinedText.isNotBlank()) {
                    functionCallObj = extractEmbeddedFunctionCall(combinedText.toString())
                }

                // If functionCall detected:
                if (functionCallObj != null) {
                    val funcName = functionCallObj.optString("name", "")
                    val argsObj = functionCallObj.optJSONObject("args") ?: JSONObject()

                    if (funcName == "get_app_data") {
                        val queryType = argsObj.optString("query_type", "daily_summary")
                        return executeAppDataQuery(
                            queryType = queryType,
                            userProfile = userProfile,
                            allLogs = allLogs,
                            cumulativeDose = cumulativeDose,
                            inventoryAudit = inventoryAudit,
                            allAppointments = allAppointments,
                            allPrescriptions = allPrescriptions,
                            appLanguage = appLanguage,
                            activeAccount = activeAccount
                        )
                    } else {
                        // Action function: Requires Confirmation Card!
                        return buildActionConfirmationCard(
                            funcName = funcName,
                            args = argsObj,
                            aiNote = combinedText.toString().trim(),
                            appLanguage = appLanguage
                        )
                    }
                }

                // Plain text / guidance response
                val finalAnswer = combinedText.toString().trim()
                if (finalAnswer.isNotBlank()) {
                    return ChatMessage(message = finalAnswer, isUser = false)
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error invoking model $model: ${e.message}", e)
                lastException = e
            }
        }

        // Try offline intent parser before showing error
        return try {
            processOfflineIntent(
                userQuery = userQuery,
                userProfile = userProfile,
                allLogs = allLogs,
                cumulativeDose = cumulativeDose,
                inventoryAudit = inventoryAudit,
                allAppointments = allAppointments,
                allPrescriptions = allPrescriptions,
                appLanguage = appLanguage,
                activeAccount = activeAccount
            )
        } catch (_: Exception) {
            val errNotice = if (appLanguage == "ar") {
                "عذراً، تعذر الاتصال بالمساعد الذكي في الوقت الحالي (${lastException?.localizedMessage ?: "تأكد من إدخال مفتاح API صالح في الإعدادات"})."
            } else {
                "Sorry, unable to reach the AI assistant at the moment (${lastException?.localizedMessage ?: "Check your API key in Settings"})."
            }
            ChatMessage(message = errNotice, isUser = false)
        }
    }

    /**
     * Executes read-only queries with high clinical accuracy.
     */
    fun executeAppDataQuery(
        queryType: String,
        userProfile: UserProfile,
        allLogs: List<DailyLog>,
        cumulativeDose: Int,
        inventoryAudit: RoaccutaneInventoryAudit?,
        allAppointments: List<Appointment>,
        allPrescriptions: List<MedicalPrescription>,
        appLanguage: String,
        activeAccount: LinkedAccount? = null
    ): ChatMessage {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val isAr = appLanguage == "ar"

        val startOfTodayMillis = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val estimations = com.example.utils.TreatmentCourseEstimator.computeEstimations(
            userProfile = userProfile,
            dispensedMedications = emptyList(),
            allLogs = allLogs,
            allAppointments = allAppointments,
            allPrescriptions = allPrescriptions
        )

        when (queryType) {
            "remaining_days_cumulative_dose" -> {
                val weight = if (userProfile.weightKg > 0f) userProfile.weightKg else 80f
                val targetPerKg = userProfile.targetMgPerKgInt
                val targetDoseMg = userProfile.calculatedTargetDoseMg
                val remainingMg = (targetDoseMg - cumulativeDose).coerceAtLeast(0)
                val dailyRate = inventoryAudit?.dailyCapsulesRate ?: 0
                val dailyDose = if (userProfile.dailyDoseMg > 0) userProfile.dailyDoseMg else if (dailyRate > 0) dailyRate * 20 else 20
                val remainingDays = if (dailyDose > 0 && remainingMg > 0) kotlin.math.ceil(remainingMg.toDouble() / dailyDose.toDouble()).toInt() else 0

                val completionCal = java.util.Calendar.getInstance()
                completionCal.add(java.util.Calendar.DAY_OF_YEAR, remainingDays)
                val completionDateStr = SimpleDateFormat("dd MMMM yyyy", if (isAr) Locale("ar") else Locale.US).format(completionCal.time)

                val progressPercent = if (targetDoseMg > 0) ((cumulativeDose.toFloat() / targetDoseMg.toFloat()) * 100f).coerceIn(0f, 100f) else 0f
                val progressPctStr = String.format(Locale.US, "%.1f%%", progressPercent)

                val summaryText = if (isAr) {
                    "بناءً على وزنك الحالي ($weight كغ) وإستراتيجية الجرعة المستهدفة ($targetPerKg ملغ/كغ = $targetDoseMg ملغ):\n" +
                            "• الجرعة التراكمية المستهلكة: $cumulativeDose ملغ ($progressPctStr من الكورس).\n" +
                            "• المتبقي للوصول للهدف: $remainingMg ملغ.\n" +
                            "• بالجرعة اليومية النشطة ($dailyDose ملغ/يوم): متبقي لك $remainingDays يوماً تقريباً.\n" +
                            "• تاريخ الانتهاء المتوقع: $completionDateStr."
                } else {
                    "Based on your weight ($weight kg) and target strategy ($targetPerKg mg/kg = $targetDoseMg mg):\n" +
                            "• Consumed so far: $cumulativeDose mg ($progressPctStr completed).\n" +
                            "• Remaining to target: $remainingMg mg.\n" +
                            "• At your active daily dose ($dailyDose mg/day): ~$remainingDays days remaining.\n" +
                            "• Expected completion date: $completionDateStr."
                }

                val detailsList = listOf(
                    (if (isAr) "الوزن الحالي" else "Current Weight") to "$weight kg",
                    (if (isAr) "إستراتيجية الهدف" else "Target Strategy") to "$targetPerKg mg/kg",
                    (if (isAr) "الجرعة التراكمية المستهدفة" else "Target Cumulative Dose") to "$targetDoseMg mg",
                    (if (isAr) "الجرعة المتناولة فعلياً" else "Actual Consumed Dose") to "$cumulativeDose mg ($progressPctStr)",
                    (if (isAr) "المتبقي بالملجم" else "Remaining (mg)") to "$remainingMg mg",
                    (if (isAr) "الجرعة اليومية المعتمدة" else "Daily Dose") to "$dailyDose mg/day",
                    (if (isAr) "الأيام المتبقية" else "Remaining Days") to "$remainingDays ${if (isAr) "يوماً" else "days"}",
                    (if (isAr) "تاريخ الإنجاز المتوقع" else "Estimated End Date") to completionDateStr
                )

                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "حساب الأيام المتبقية والجرعة التراكمية" else "Cumulative Dose & Remaining Days",
                    summary = summaryText,
                    details = detailsList
                )

                return ChatMessage(
                    message = summaryText,
                    isUser = false,
                    queryCard = queryCard
                )
            }

            "stock_status" -> {
                val audit = inventoryAudit
                val totalCaps = audit?.grandTotalDispensedCapsules ?: 0
                if (audit == null || totalCaps <= 0) {
                    val emptyMsg = if (isAr) {
                        "لا توجد صرفيات كبسولات مسجلة في متتبع الجرعات والوصفات المصروفة حتى الآن. يمكنك إضافة صرفية من قسم «الوصفات والصرفيات»."
                    } else {
                        "No dispensed capsule packs are logged yet. Add a dispense record in the Prescriptions & Dispenses tracker."
                    }
                    return ChatMessage(message = emptyMsg, isUser = false)
                }

                val consumedCaps = audit.actualCumulativeConsumedCapsules
                val remainingCaps = audit.trueRemainingCapsules
                val fullBoxes = audit.fullSealedBoxesCount
                val openBoxes = audit.currentOpenBoxes.size
                val dailyRate = audit.dailyCapsulesRate
                val remainingDays = audit.remainingDaysCount
                val runOutDateStr = SimpleDateFormat("yyyy-MM-dd (dd MMM)", if (isAr) Locale("ar") else Locale.US).format(Date(audit.estimatedRunOutDateMillis))

                val summaryText = if (isAr) {
                    "حالة مخزون كبسولات الروكتان المحدثة:\n" +
                            "• إجمالي الكبسولات المتبقية: $remainingCaps كبسولة (من أصل $totalCaps مصروفة).\n" +
                            "• توزيع العلب: $fullBoxes علبة مقفلة بالكامل، و $openBoxes علبة مفتوحة قيد الاستخدام.\n" +
                            "• بمعدل استهلاكك الحالي ($dailyRate كبسولة/يوم): يكفي المخزون لمدة $remainingDays يوماً.\n" +
                            "• تاريخ آخر كبسولة في المخزون: $runOutDateStr."
                } else {
                    "Current Roaccutane capsule stock status:\n" +
                            "• Remaining capsules: $remainingCaps caps (out of $totalCaps dispensed).\n" +
                            "• Packs breakdown: $fullBoxes sealed boxes, $openBoxes currently open.\n" +
                            "• At daily consumption rate ($dailyRate caps/day): supply lasts $remainingDays days.\n" +
                            "• Last capsule in stock date: $runOutDateStr."
                }

                val detailsList = listOf(
                    (if (isAr) "الكبسولات المتبقية الفعلية" else "Remaining Capsules") to "$remainingCaps caps",
                    (if (isAr) "الكبسولات المستهلكة" else "Consumed Capsules") to "$consumedCaps caps",
                    (if (isAr) "العلب المقفلة الكاملة" else "Sealed Full Packs") to "$fullBoxes",
                    (if (isAr) "العلب المفتوحة حالياً" else "Currently Open Packs") to "$openBoxes",
                    (if (isAr) "معدل الاستهلاك اليومي" else "Daily Consumption") to "$dailyRate caps/day",
                    (if (isAr) "مدة كفاية المخزون" else "Days of Supply") to "$remainingDays ${if (isAr) "يوماً" else "days"}",
                    (if (isAr) "تاريخ آخر كبسولة" else "Last Capsule Date") to runOutDateStr
                )

                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "تدقيق مخزون الكبسولات (FIFO)" else "Capsule Inventory Audit (FIFO)",
                    summary = summaryText,
                    details = detailsList
                )

                return ChatMessage(message = summaryText, isUser = false, queryCard = queryCard)
            }

            "next_appointment" -> {
                val next = allAppointments
                    .filter { it.dateTime >= startOfTodayMillis && !it.isCompleted }
                    .minByOrNull { it.dateTime }
                if (next == null) {
                    val noMsg = if (isAr) "لا توجد مواعيد قادمة مجدولة حالياً في سجل المواعيد." else "No upcoming appointments scheduled."
                    return ChatMessage(message = noMsg, isUser = false)
                }

                val typeLabel = when (next.appointmentType.uppercase(Locale.US)) {
                    "LAB" -> if (isAr) "🧪 سحب عينات وتحاليل" else "🧪 Lab Test"
                    "PHARMACY" -> if (isAr) "💊 صرف صيدلية" else "💊 Pharmacy"
                    else -> if (isAr) "🏥 عيادة واستشارة" else "🏥 Clinic Visit"
                }
                val dateFormatted = SimpleDateFormat("EEEE, dd MMMM yyyy - hh:mm a", if (isAr) Locale("ar") else Locale.US).format(Date(next.dateTime))
                val diffDays = ((next.dateTime - System.currentTimeMillis()) / (24L * 3600L * 1000L)).toInt()
                val countdownStr = when {
                    diffDays > 1 -> if (isAr) "بعد $diffDays يوماً" else "In $diffDays days"
                    diffDays == 1 -> if (isAr) "غداً" else "Tomorrow"
                    diffDays == 0 -> if (isAr) "اليوم" else "Today"
                    else -> if (isAr) "اليوم (مر وقته)" else "Earlier today"
                }

                val summaryText = if (isAr) {
                    "تفاصيل موعدك القادم:\n" +
                            "• التصنيف: $typeLabel\n" +
                            "• التاريخ والوقت: $dateFormatted ($countdownStr)\n" +
                            "• المستشفى: ${next.hospitalName.ifBlank { "غير محدد" }}\n" +
                            "• الطبيب / العيادة: ${next.doctorName.ifBlank { "غير محدد" }}${if (next.clinic.isNotBlank()) " (${next.clinic})" else ""}\n" +
                            "• سبب الموعد: ${next.reason}" +
                            (if (next.notes.isNotBlank()) "\n• ملاحظات: ${next.notes}" else "")
                } else {
                    "Your next upcoming appointment:\n" +
                            "• Type: $typeLabel\n" +
                            "• Date & Time: $dateFormatted ($countdownStr)\n" +
                            "• Hospital: ${next.hospitalName.ifBlank { "Unspecified" }}\n" +
                            "• Doctor/Clinic: ${next.doctorName.ifBlank { "Unspecified" }}${if (next.clinic.isNotBlank()) " (${next.clinic})" else ""}\n" +
                            "• Reason: ${next.reason}" +
                            (if (next.notes.isNotBlank()) "\n• Notes: ${next.notes}" else "")
                }

                val detailsList = mutableListOf(
                    (if (isAr) "نوع الموعد" else "Type") to typeLabel,
                    (if (isAr) "التاريخ والوقت" else "Date & Time") to "$dateFormatted ($countdownStr)",
                    (if (isAr) "المستشفى" else "Hospital") to next.hospitalName.ifBlank { "N/A" },
                    (if (isAr) "الطبيب" else "Doctor") to next.doctorName.ifBlank { "N/A" },
                    (if (isAr) "العيادة / المبنى" else "Clinic / Building") to listOf(next.clinic, next.building).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "N/A" },
                    (if (isAr) "سبب الزيارة" else "Reason") to next.reason
                )

                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "تفاصيل الموعد القادم" else "Next Upcoming Appointment",
                    summary = summaryText,
                    details = detailsList
                )

                return ChatMessage(message = summaryText, isUser = false, queryCard = queryCard)
            }

            "next_lab_appointment" -> {
                val nextLab = allAppointments
                    .filter { it.dateTime >= startOfTodayMillis && it.appointmentType.equals("LAB", ignoreCase = true) && !it.isCompleted }
                    .minByOrNull { it.dateTime }

                if (nextLab != null) {
                    val dateFormatted = SimpleDateFormat("EEEE, dd MMMM yyyy - hh:mm a", if (isAr) Locale("ar") else Locale.US).format(Date(nextLab.dateTime))
                    val summaryText = if (isAr) {
                        "موعد تحاليلك المخبرية القادم:\n" +
                                "• التاريخ والوقت: $dateFormatted\n" +
                                "• المستشفى / المختبر: ${nextLab.hospitalName.ifBlank { "غير محدد" }}\n" +
                                "• السبب: ${nextLab.reason}"
                    } else {
                        "Your next scheduled lab appointment:\n" +
                                "• Date & Time: $dateFormatted\n" +
                                "• Hospital / Lab: ${nextLab.hospitalName.ifBlank { "Unspecified" }}\n" +
                                "• Reason: ${nextLab.reason}"
                    }
                    val queryCard = AiQueryCard(
                        queryType = queryType,
                        title = if (isAr) "موعد التحاليل القادم" else "Next Lab Appointment",
                        summary = summaryText,
                        details = listOf(
                            (if (isAr) "التاريخ والوقت" else "Date & Time") to dateFormatted,
                            (if (isAr) "المستشفى" else "Hospital") to nextLab.hospitalName.ifBlank { "N/A" },
                            (if (isAr) "السبب" else "Reason") to nextLab.reason
                        )
                    )
                    return ChatMessage(message = summaryText, isUser = false, queryCard = queryCard)
                } else {
                    val suggestedStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(estimations.recommendedNextLabDateMillis))
                    val msg = if (isAr) {
                        "لا يوجد موعد مصنف كـ «تحاليل (LAB)» مجدول حالياً.\n• التاريخ المقترح للفحص القادم وفق خطة العلاج: $suggestedStr."
                    } else {
                        "No upcoming LAB-classified appointment is scheduled.\n• Recommended next lab date: $suggestedStr."
                    }
                    val queryCard = AiQueryCard(
                        queryType = queryType,
                        title = if (isAr) "تقدير الفحص المخبري القادم" else "Next Lab Estimate",
                        summary = msg,
                        details = listOf(
                            (if (isAr) "الحالة" else "Status") to (if (isAr) "لا يوجد موعد مختبر مجدول" else "Not yet scheduled"),
                            (if (isAr) "الموعد المقترح" else "Recommended Date") to suggestedStr
                        )
                    )
                    return ChatMessage(message = msg, isUser = false, queryCard = queryCard)
                }
            }

            "past_appointments" -> {
                val now = System.currentTimeMillis()
                val past = allAppointments.filter { it.dateTime < now || it.isCompleted }.sortedByDescending { it.dateTime }
                if (past.isEmpty()) {
                    val noMsg = if (isAr) "لا توجد مواعيد سابقة مسجلة في الأرشيف." else "No past appointments recorded."
                    return ChatMessage(message = noMsg, isUser = false)
                }

                val sb = StringBuilder()
                if (isAr) sb.append("المواعيد السابقة (${past.size} موعد):\n")
                else sb.append("Past Appointments (${past.size} visits):\n")

                val details = mutableListOf<Pair<String, String>>()
                past.take(6).forEach { appt ->
                    val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(appt.dateTime))
                    val typeTag = when (appt.appointmentType.uppercase(Locale.US)) {
                        "LAB" -> if (isAr) "تحاليل" else "Lab"
                        "PHARMACY" -> if (isAr) "صيدلية" else "Pharmacy"
                        else -> if (isAr) "عيادة" else "Clinic"
                    }
                    sb.append("• $dStr [$typeTag]: ${appt.reason} - ${appt.hospitalName.ifBlank { appt.doctorName }}\n")
                    details.add("${appt.reason} ($dStr • $typeTag)" to listOf(appt.hospitalName, appt.doctorName).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "N/A" })
                }

                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "سجل المواعيد السابقة" else "Past Appointments Log",
                    summary = sb.toString().trim(),
                    details = details
                )

                return ChatMessage(message = sb.toString().trim(), isUser = false, queryCard = queryCard)
            }

            "lab_results" -> {
                fun hasAnyLabData(a: Appointment): Boolean =
                    a.alt != null || a.ast != null || a.bilirubin != null || a.alkalinePhosphate != null ||
                    a.albuminInSerum != null || a.creatinine != null || a.urea != null ||
                    a.tCholestrol != null || a.ldl != null || a.hdl != null || a.triglycerides != null ||
                    a.glycatedHaemoglobin != null ||
                    a.liverFunction.isNotBlank() || a.lipidsProfile.isNotBlank() ||
                    a.kidneyFunction.isNotBlank() || a.otherBloodTests.isNotBlank()

                val labApptsChronological = allAppointments
                    .filter { it.appointmentType.equals("LAB", ignoreCase = true) && hasAnyLabData(it) }
                    .sortedBy { it.dateTime }

                if (labApptsChronological.isEmpty()) {
                    val noMsg = if (isAr) {
                        "لا توجد نتائج تحاليل مخبرية في مواعيد مصنفة كـ «سحب عينات وتحاليل (LAB)» حتى الآن. يمكنك تصنيف أي موعد من صفحة المواعيد لربطه بسجل التحاليل."
                    } else {
                        "No lab results recorded under LAB-classified appointments yet. Classify an appointment as Lab to link its results."
                    }
                    return ChatMessage(message = noMsg, isUser = false)
                }

                val latestIdx = labApptsChronological.lastIndex
                val latest = labApptsChronological[latestIdx]
                val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(latest.dateTime))

                data class BiomarkerCheck(
                    val labelAr: String,
                    val labelEn: String,
                    val getter: (Appointment) -> Double?,
                    val formatDual: (Double) -> String,
                    val formatCompact: (Double) -> String,
                    val isElevated: (Double) -> Boolean
                )

                val markers = listOf(
                    BiomarkerCheck("إنزيم الكبد ALT", "Liver ALT", { it.alt },
                        { com.example.utils.LabUnitConverter.formatEnzymeDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it > 56.0 }),
                    BiomarkerCheck("إنزيم الكبد AST", "Liver AST", { it.ast },
                        { com.example.utils.LabUnitConverter.formatEnzymeDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it > 40.0 }),
                    BiomarkerCheck("البيليروبين الكلي", "Total Bilirubin", { it.bilirubin },
                        { com.example.utils.LabUnitConverter.formatBilirubinDual(it, appLanguage) },
                        { String.format(Locale.US, "%.2f", it) }, { it > 1.2 }),
                    BiomarkerCheck("الفوسفاتاز القلوي ALP", "Alkaline Phosphatase", { it.alkalinePhosphate },
                        { com.example.utils.LabUnitConverter.formatEnzymeDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it > 147.0 }),
                    BiomarkerCheck("ألبومين المصل", "Serum Albumin", { it.albuminInSerum },
                        { com.example.utils.LabUnitConverter.formatAlbuminDual(it, appLanguage) },
                        { String.format(Locale.US, "%.2f", it) }, { it < 3.4 || it > 5.4 }),
                    BiomarkerCheck("كرياتينين الكلى", "Creatinine", { it.creatinine },
                        { com.example.utils.LabUnitConverter.formatCreatinineDual(it, appLanguage) },
                        { String.format(Locale.US, "%.2f", it) }, { it > 1.2 }),
                    BiomarkerCheck("اليوريا (BUN)", "Urea / BUN", { it.urea },
                        { com.example.utils.LabUnitConverter.formatUreaDual(it, appLanguage) },
                        { String.format(Locale.US, "%.1f", it) }, { it > 20.0 }),
                    BiomarkerCheck("الكوليسترول الكلي", "Total Cholesterol", { it.tCholestrol },
                        { com.example.utils.LabUnitConverter.formatCholesterolDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it >= 200.0 }),
                    BiomarkerCheck("الضار LDL", "LDL Cholesterol", { it.ldl },
                        { com.example.utils.LabUnitConverter.formatCholesterolDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it >= 100.0 }),
                    BiomarkerCheck("النافع HDL", "HDL Cholesterol", { it.hdl },
                        { com.example.utils.LabUnitConverter.formatCholesterolDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it < 40.0 }),
                    BiomarkerCheck("الدهون الثلاثية TG", "Triglycerides", { it.triglycerides },
                        { com.example.utils.LabUnitConverter.formatTriglyceridesDual(it, appLanguage) },
                        { String.format(Locale.US, "%.0f", it) }, { it >= 150.0 }),
                    BiomarkerCheck("السكر التراكمي HbA1c", "HbA1c", { it.glycatedHaemoglobin },
                        { com.example.utils.LabUnitConverter.formatHbA1cDual(it, appLanguage) },
                        { String.format(Locale.US, "%.1f", it) }, { it >= 5.7 })
                )

                val sb = StringBuilder()
                if (isAr) {
                    sb.append("نتائج آخر الفحوصات والتحاليل المخبرية ($dStr):\n")
                    if (latest.hospitalName.isNotBlank()) sb.append("المستشفى/المختبر: ${latest.hospitalName}\n")
                } else {
                    sb.append("Latest Laboratory Test Results ($dStr):\n")
                    if (latest.hospitalName.isNotBlank()) sb.append("Hospital/Lab: ${latest.hospitalName}\n")
                }

                val details = mutableListOf<Pair<String, String>>()
                details.add((if (isAr) "تاريخ التحليل" else "Test Date") to dStr)

                var numericCount = 0
                markers.forEach { marker ->
                    val currentVal = marker.getter(latest) ?: return@forEach
                    numericCount++
                    val prevVal = (latestIdx - 1 downTo 0).firstNotNullOfOrNull { prevIdx ->
                        marker.getter(labApptsChronological[prevIdx])
                    }
                    // Strictly omit trend indicator if there is no prior entered result for this biomarker
                    val trendStr = if (prevVal == null) {
                        ""
                    } else {
                        val diff = currentVal - prevVal
                        when {
                            diff > 0.001 -> if (isAr) " • ↑ مرتفع عن السابق (${marker.formatCompact(prevVal)})" else " • ↑ Up vs prev (${marker.formatCompact(prevVal)})"
                            diff < -0.001 -> if (isAr) " • ↓ منخفض عن السابق (${marker.formatCompact(prevVal)})" else " • ↓ Down vs prev (${marker.formatCompact(prevVal)})"
                            else -> if (isAr) " • → ثابت عن السابق" else " • → Unchanged"
                        }
                    }
                    val safetyTag = if (marker.isElevated(currentVal)) {
                        if (isAr) " ⚠️" else " ⚠️"
                    } else {
                        " ✓"
                    }
                    val name = if (isAr) marker.labelAr else marker.labelEn
                    val formattedVal = "${marker.formatDual(currentVal)}$safetyTag$trendStr"
                    sb.append("• $name: $formattedVal\n")
                    details.add(name to formattedVal)
                }

                // Fallback legacy text fields when numeric fields weren't filled
                if (numericCount == 0) {
                    if (latest.liverFunction.isNotBlank()) {
                        val lVal = latest.liverFunction
                        sb.append("• ${if (isAr) "وظائف الكبد" else "Liver Function"}: $lVal\n")
                        details.add((if (isAr) "وظائف الكبد" else "Liver Function") to lVal)
                    }
                    if (latest.lipidsProfile.isNotBlank()) {
                        val pVal = latest.lipidsProfile
                        sb.append("• ${if (isAr) "ملف الدهون" else "Lipids"}: $pVal\n")
                        details.add((if (isAr) "ملف الدهون" else "Lipids") to pVal)
                    }
                    if (latest.kidneyFunction.isNotBlank()) {
                        val kVal = latest.kidneyFunction
                        sb.append("• ${if (isAr) "وظائف الكلى" else "Kidney Function"}: $kVal\n")
                        details.add((if (isAr) "وظائف الكلى" else "Kidney Function") to kVal)
                    }
                }

                if (latest.otherBloodTests.isNotBlank()) {
                    val oVal = latest.otherBloodTests
                    sb.append("• ${if (isAr) "تحاليل أخرى" else "Other Tests"}: $oVal\n")
                    details.add((if (isAr) "تحاليل أخرى" else "Other Tests") to oVal)
                }

                val finalSummary = sb.toString().trim()
                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "آخر التحاليل المخبرية ومقارنة التغير" else "Latest Lab Results & Trends",
                    summary = finalSummary,
                    details = details
                )

                return ChatMessage(message = finalSummary, isUser = false, queryCard = queryCard)
            }

            "linked_account_status" -> {
                if (activeAccount == null) {
                    val noMsg = if (isAr) "لا يوجد حساب بريد إلكتروني مرتبط حالياً في إعدادات التطبيق." else "No email account currently linked in Settings."
                    return ChatMessage(message = noMsg, isUser = false)
                }
                val providerLabel = if (activeAccount.isGoogle) (if (isAr) "حساب جوجل الموثق (Google Account)" else "Verified Google Account") else (if (isAr) "بريد إلكتروني موثق (Email)" else "Verified Email")
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(activeAccount.linkedTimestamp))
                val summaryText = if (isAr) {
                    "بيانات الحساب المرتبط والمصادق به حالياً:\n" +
                            "• البريد الإلكتروني: ${activeAccount.email}\n" +
                            "• نوع الحساب: $providerLabel\n" +
                            "• حالة الحساب: نشط ومطابق لخدمات Gemini والمزامنة السحابية ✓\n" +
                            "• تاريخ الربط والتحقق: $dateStr"
                } else {
                    "Currently linked and verified account details:\n" +
                            "• Email: ${activeAccount.email}\n" +
                            "• Provider: $providerLabel\n" +
                            "• Status: Active & synced with Gemini AI services ✓\n" +
                            "• Verified at: $dateStr"
                }

                val details = listOf(
                    (if (isAr) "البريد الإلكتروني" else "Email") to activeAccount.email,
                    (if (isAr) "نوع المزود" else "Provider") to providerLabel,
                    (if (isAr) "المصادقة والمزامنة" else "Auth & Sync") to (if (isAr) "تم التحقق عبر رمز OTP ✓" else "Verified via OTP ✓"),
                    (if (isAr) "تاريخ الربط" else "Linked Date") to dateStr
                )

                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "تفاصيل الحساب المرتبط" else "Linked Account Details",
                    summary = summaryText,
                    details = details
                )

                return ChatMessage(message = summaryText, isUser = false, queryCard = queryCard)
            }

            else -> {
                // daily_summary
                val todayLog = allLogs.find { it.dateString == todayStr }
                val takenStr = if (todayLog?.isTaken == true) (if (isAr) "تم أخذ الجرعة (${todayLog.doseMg} ملغ) ✓" else "Dose taken (${todayLog.doseMg} mg) ✓") else (if (isAr) "لم تسجل الجرعة بعد" else "Not logged yet")
                val waterStr = "${todayLog?.waterCups ?: 0} ${if (isAr) "أكواب (~${(todayLog?.waterCups ?: 0) * 250} مل)" else "cups"}"
                val moistSummary = if (isAr) {
                    "شفاه: ${todayLog?.lipMoistCount ?: 0} | بشرة: ${todayLog?.faceMoistCount ?: 0} | عينين: ${todayLog?.eyeMoistCount ?: 0}"
                } else {
                    "Lips: ${todayLog?.lipMoistCount ?: 0} | Face: ${todayLog?.faceMoistCount ?: 0} | Eyes: ${todayLog?.eyeMoistCount ?: 0}"
                }
                val sympStr = todayLog?.symptoms?.ifBlank { null } ?: (if (isAr) "لا توجد أعراض مسجلة" else "No symptoms logged")

                val summaryText = if (isAr) {
                    "ملخص يوميات اليوم ($todayStr):\n" +
                            "• الجرعة: $takenStr\n" +
                            "• الماء المستهلك: $waterStr\n" +
                            "• استخدام المرطبات: $moistSummary\n" +
                            "• الأعراض: $sympStr"
                } else {
                    "Today's Daily Summary ($todayStr):\n" +
                            "• Dose: $takenStr\n" +
                            "• Water: $waterStr\n" +
                            "• Moisturizers: $moistSummary\n" +
                            "• Symptoms: $sympStr"
                }

                val details = listOf(
                    (if (isAr) "حالة الجرعة" else "Dose Status") to takenStr,
                    (if (isAr) "شرب الماء" else "Water") to waterStr,
                    (if (isAr) "المرطبات" else "Moisturizers") to moistSummary,
                    (if (isAr) "الأعراض" else "Symptoms") to sympStr
                )

                val queryCard = AiQueryCard(
                    queryType = queryType,
                    title = if (isAr) "ملخص اليوميات لليوم" else "Today's Daily Log Summary",
                    summary = summaryText,
                    details = details
                )

                return ChatMessage(message = summaryText, isUser = false, queryCard = queryCard)
            }
        }
    }

    /**
     * Builds an unconfirmed interactive confirmation card for user inspection.
     */
    private fun buildActionConfirmationCard(
        funcName: String,
        args: JSONObject,
        aiNote: String,
        appLanguage: String
    ): ChatMessage {
        val isAr = appLanguage == "ar"

        val doseMg = if (args.has("dose_taken_mg")) args.getInt("dose_taken_mg") else null
        val takenAt = args.optString("taken_at", null)
        val symptomsList = mutableListOf<String>()
        if (args.has("symptoms")) {
            val arr = args.getJSONArray("symptoms")
            for (i in 0 until arr.length()) {
                symptomsList.add(arr.getString(i))
            }
        }
        val severity = if (args.has("severity_1_to_5")) args.getInt("severity_1_to_5") else null

        val waterMl = if (args.has("water_ml")) args.getInt("water_ml") else null
        val moistType = args.optString("moisturizer_type", null)
        val moistCount = if (args.has("count_increment")) args.getInt("count_increment") else null

        val prescribedDose = if (args.has("prescribed_dose")) args.getInt("prescribed_dose") else null
        val capsulesDispensed = if (args.has("capsules_dispensed")) args.getInt("capsules_dispensed") else null
        val capsulesRemaining = if (args.has("capsules_remaining")) args.getInt("capsules_remaining") else null
        val productAction = args.optString("product_action", null)
        val productName = args.optString("product_name", null)

        val apptDate = args.optString("appointment_date", null)
        val labTestName = args.optString("lab_test_name", null)
        val labResultValue = args.optString("lab_result_value", null)

        val actionCard = AiActionCard(
            functionName = funcName,
            functionArgsJson = args.toString(),
            description = aiNote,
            doseMg = doseMg,
            symptoms = symptomsList,
            severity = severity,
            takenAt = takenAt,
            waterMl = waterMl,
            moisturizerType = moistType,
            moisturizerCount = moistCount,
            capsulesDispensed = capsulesDispensed,
            capsulesRemaining = capsulesRemaining,
            prescribedDose = prescribedDose,
            productName = productName,
            productAction = productAction,
            appointmentDate = apptDate,
            labTestName = labTestName,
            labResultValue = labResultValue,
            isConfirmed = false,
            isCancelled = false
        )

        val preamble = if (aiNote.isNotBlank()) {
            aiNote
        } else {
            if (isAr) "فهمت رغبتك بتسجيل هذه البيانات في التطبيق. يرجى معاينتها والضغط على تأكيد لحفظها في قاعدة البيانات:"
            else "I understood your request to log this data. Please review and confirm to save into the database:"
        }

        return ChatMessage(
            message = preamble,
            isUser = false,
            actionCard = actionCard
        )
    }

    private fun extractEmbeddedFunctionCall(text: String): JSONObject? {
        try {
            val jsonStart = text.indexOf('{')
            val jsonEnd = text.lastIndexOf('}')
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val candidateJson = text.substring(jsonStart, jsonEnd + 1)
                val obj = JSONObject(candidateJson)
                if (obj.has("functionCall")) {
                    return obj.getJSONObject("functionCall")
                }
                if (obj.has("name") && (obj.has("args") || obj.has("parameters"))) {
                    val converted = JSONObject()
                    converted.put("name", obj.getString("name"))
                    converted.put("args", obj.optJSONObject("args") ?: obj.optJSONObject("parameters") ?: JSONObject())
                    return converted
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun matchDeterministicQueryType(userQuery: String): String? {
        val q = userQuery.lowercase().trim()
        val isLoggingIntent = q.contains("أخذت") || q.contains("اخذت") || q.contains("شربت") ||
            q.contains("وضعت") || q.contains("سجل لي") || q.contains("took ") || q.contains("drank ")
        if (isLoggingIntent) return null

        if (q.contains("حساب") || q.contains("ايميل") || q.contains("بريد") || q.contains("من أنا") || q.contains("مين أنا") || q.contains("linked account") || q.contains("who am i")) {
            return "linked_account_status"
        }
        if (q.contains("كم باقي") || q.contains("كم بقي") || q.contains("باقي لي يوم") || q.contains("بقي لي يوم") || q.contains("نهاية الكورس") || q.contains("days left") || q.contains("remaining days")) {
            return "remaining_days_cumulative_dose"
        }
        if ((q.contains("تحاليل") || q.contains("تحليل") || q.contains("فحص") || q.contains("مختبر") || q.contains("lab")) &&
            (q.contains("موعد") || q.contains("متى") || q.contains("appointment") || q.contains("when")) &&
            !q.contains("نتائج") && !q.contains("result")
        ) {
            return "next_lab_appointment"
        }
        if ((q.contains("موعد") || q.contains("مواعيد") || q.contains("appointment")) &&
            (q.contains("سابق") || q.contains("ماضي") || q.contains("past"))
        ) {
            return "past_appointments"
        }
        if (q.contains("موعدي القادم") || q.contains("الموعد القادم") || q.contains("تفاصيل موعدي") || q.contains("next appointment")) {
            return "next_appointment"
        }
        if (q.contains("نتائج") && (q.contains("تحاليل") || q.contains("فحص") || q.contains("فحوصات") || q.contains("مختبر")) ||
            q.contains("latest lab") || q.contains("lab results")
        ) {
            return "lab_results"
        }
        if (q.contains("مخزون الكبسولات") || q.contains("حالة مخزون") || q.contains("capsule inventory") || q.contains("stock status")) {
            return "stock_status"
        }
        if (q.contains("ملخص يوميات") || q.contains("ملخص اليوم") || q.contains("daily summary")) {
            return "daily_summary"
        }
        return null
    }

    fun processOfflineIntent(
        userQuery: String,
        userProfile: UserProfile,
        allLogs: List<DailyLog>,
        cumulativeDose: Int,
        inventoryAudit: RoaccutaneInventoryAudit?,
        allAppointments: List<Appointment>,
        allPrescriptions: List<MedicalPrescription>,
        appLanguage: String,
        activeAccount: LinkedAccount? = null
    ): ChatMessage {
        val q = userQuery.lowercase()
        val isAr = appLanguage == "ar"

        // 1. Check queries
        matchDeterministicQueryType(userQuery)?.let { matchedType ->
            return executeAppDataQuery(matchedType, userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
        }
        if (q.contains("باقي") || q.contains("بقي") || q.contains("تراكم") || q.contains("الجرعة الدنيا") || q.contains("أخلص") || q.contains("remaining") || q.contains("cumulative")) {
            return executeAppDataQuery("remaining_days_cumulative_dose", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
        }
        if (q.contains("موعد") || q.contains("عيادة") || q.contains("طبيب") || q.contains("دكتور") || q.contains("appointment") || q.contains("clinic")) {
            if (q.contains("سابق") || q.contains("ماضي") || q.contains("past")) {
                return executeAppDataQuery("past_appointments", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
            }
            if (q.contains("تحاليل") || q.contains("فحص") || q.contains("lab")) {
                return executeAppDataQuery("next_lab_appointment", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
            }
            return executeAppDataQuery("next_appointment", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
        }
        if (q.contains("تحاليل") || q.contains("فحص") || q.contains("فحوصات") || q.contains("دم") || q.contains("كبد") || q.contains("دهون") || q.contains("lab") || q.contains("test")) {
            return executeAppDataQuery("lab_results", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
        }
        if (q.contains("مخزون") || q.contains("كبسول") || q.contains("علب") || q.contains("stock") || q.contains("capsule")) {
            return executeAppDataQuery("stock_status", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
        }
        if (q.contains("ملخص") || q.contains("اليوم") || q.contains("summary") || q.contains("today")) {
            return executeAppDataQuery("daily_summary", userProfile, allLogs, cumulativeDose, inventoryAudit, allAppointments, allPrescriptions, appLanguage, activeAccount)
        }

        // 2. Check dose / symptoms / tracker logging actions
        val hasDose = q.contains("أخذت") || q.contains("اخذت") || q.contains("حبة") || q.contains("جرعة") || q.contains("took") || q.contains("dose") || q.contains("20") || q.contains("40") || q.contains("10")
        val hasSymptoms = q.contains("جفاف") || q.contains("صداع") || q.contains("خمول") || q.contains("مفاصل") || q.contains("شفايف") || q.contains("dryness") || q.contains("headache")
        val hasTracker = q.contains("مرطب") || q.contains("ترطيب") || q.contains("ماء") || q.contains("شربت") || q.contains("water") || q.contains("moistur")

        if (hasDose || hasSymptoms || hasTracker) {
            val args = JSONObject()

            // Dose
            val doseMatch = Regex("(\\d+)\\s*(?:mg|ملغ|ملجم|حبة)").find(userQuery)
                ?: Regex("حبة الـ\\s*(\\d+)").find(userQuery)
                ?: Regex("(\\d+)").find(userQuery)
            val detectedDose = doseMatch?.groupValues?.get(1)?.toIntOrNull() ?: if (hasDose) userProfile.dailyDoseMg else null
            if (detectedDose != null) {
                args.put("dose_taken_mg", detectedDose)
            }

            // Symptoms
            val symptoms = JSONArray()
            if (q.contains("جفاف") || q.contains("dryness")) {
                if (q.contains("شفايف") || q.contains("lip")) symptoms.put(if (isAr) "جفاف شفايف" else "Chapped Lips")
                else if (q.contains("عين") || q.contains("eye")) symptoms.put(if (isAr) "جفاف عينين" else "Dry Eyes")
                else symptoms.put(if (isAr) "جفاف بشرة" else "Dry Skin")
            }
            if (q.contains("صداع") || q.contains("headache")) symptoms.put(if (isAr) "صداع" else "Headache")
            if (q.contains("خمول") || q.contains("تعب") || q.contains("fatigue")) symptoms.put(if (isAr) "خمول وإرهاق" else "Fatigue")
            if (q.contains("مفاصل") || q.contains("ظهر") || q.contains("joint")) symptoms.put(if (isAr) "آلام مفاصل" else "Joint Pain")
            if (symptoms.length() > 0) {
                args.put("symptoms", symptoms)
            }

            // Severity
            val severity = if (q.contains("شديد") || q.contains("قوي") || q.contains("severe")) 4
            else if (q.contains("خفيف") || q.contains("mild")) 2
            else 3
            args.put("severity_1_to_5", severity)

            // Moisturizer
            if (q.contains("مرطب") || q.contains("ترطيب") || q.contains("شفايف") || q.contains("moistur")) {
                val countMatch = Regex("(\\d+)\\s*(?:مرات|مرة|times)?").find(userQuery)
                val count = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                args.put("moisturizer_type", if (q.contains("شفايف") || q.contains("lip")) "lips" else if (q.contains("عين") || q.contains("eye")) "eyes" else "skin")
                args.put("count_increment", count)
            }

            // Water
            if (q.contains("ماء") || q.contains("water") || q.contains("شربت")) {
                val waterMatch = Regex("(\\d+)\\s*(?:مل|ml)").find(userQuery)
                val ml = waterMatch?.groupValues?.get(1)?.toIntOrNull() ?: 500
                args.put("water_ml", ml)
            }

            return buildActionConfirmationCard(
                funcName = "log_daily_dose_and_symptoms",
                args = args,
                aiNote = if (isAr) "فهمت ما ذكرته باللغة الطبيعية. قمت بتجهيز بطاقة التأكيد التالية لمعاينتها واعتمادها:"
                else "I understood your request. Here is the confirmation card to review before saving:",
                appLanguage = appLanguage
            )
        }

        // Generic friendly medical reply
        val defaultAdvice = if (isAr) {
            "أهلاً بك! أنا مساعدك الذكي المتخصص في كورس الروكتان. يمكنك إخباري بجرعتك اليومية، أعراضك، شرب الماء، أو الاستفسار عن كورس الدواء وتاريخ الانتهاء المتوقع ومواعيدك ومخزون الكبسولات."
        } else {
            "Welcome! I am your intelligent Roaccutane assistant. You can log your daily dose, symptoms, water intake, or ask about remaining days, appointments, and capsule inventory."
        }
        return ChatMessage(message = defaultAdvice, isUser = false)
    }
}
