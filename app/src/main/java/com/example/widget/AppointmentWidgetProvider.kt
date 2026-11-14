package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateWidgetStateSync(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        suspend fun updateWidgetStateSync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_appointment_layout)

                // Click opens main app
                val openAppIntent = Intent(context, MainActivity::class.java)
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_appt_root, openAppPendingIntent)

                val db = AppDatabase.getDatabase(context)
                val allAppointments = db.roaccutaneDao().getAllAppointmentsDirect()
                val now = System.currentTimeMillis()
                val nextAppt = allAppointments
                    .filter { !it.isCompleted && it.dateTime >= now - (2 * 3600 * 1000) }
                    .minByOrNull { it.dateTime }

                if (nextAppt != null) {
                    val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale("ar"))
                    val formattedDate = dateFmt.format(Date(nextAppt.dateTime))
                    views.setTextViewText(R.id.tv_appt_date, formattedDate)
                    val hospitalInfo = if (nextAppt.hospitalName.isNotBlank()) "في ${nextAppt.hospitalName}" else ""
                    views.setTextViewText(R.id.tv_appt_reason, "${nextAppt.reason} $hospitalInfo")

                    val hasLabs = nextAppt.tCholestrol != null || nextAppt.liverFunction.isNotBlank() || nextAppt.lipidsProfile.isNotBlank()
                    if (hasLabs) {
                        views.setTextViewText(R.id.tv_lab_status, "فحوصات الشهر مسجلة")
                        views.setTextViewText(R.id.tv_lab_details, "الدهون والكبد والكلى")
                    } else {
                        views.setTextViewText(R.id.tv_lab_status, "تحاليل الموعد القادم")
                        views.setTextViewText(R.id.tv_lab_details, "صورة دم وخمائر الكبد")
                    }
                } else {
                    views.setTextViewText(R.id.tv_appt_date, "لا يوجد موعد قادم")
                    views.setTextViewText(R.id.tv_appt_reason, "انقر لجدولة موعد مستشفى")
                    views.setTextViewText(R.id.tv_lab_status, "الفحوصات الطبية")
                    views.setTextViewText(R.id.tv_lab_details, "جدولة الفحوصات الدورية")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateWidgetState(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                updateWidgetStateSync(context, appWidgetManager, appWidgetId)
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, AppointmentWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, AppointmentWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}

