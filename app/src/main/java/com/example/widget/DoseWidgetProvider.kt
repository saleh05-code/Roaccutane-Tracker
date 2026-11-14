package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.R
import com.example.data.AppDatabase
import com.example.data.DailyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DoseWidgetProvider : AppWidgetProvider() {

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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_DOSE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.roaccutaneDao()
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                    val existingLog = dao.getLogByDate(todayStr)
                    val isNowTaken = !(existingLog?.isTaken ?: false)

                    // 1. Immediately reflect the visual state change on all widget instances
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, DoseWidgetProvider::class.java)
                    val views = createDoseRemoteViews(context, isNowTaken)
                    appWidgetManager.updateAppWidget(componentName, views)

                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    for (id in appWidgetIds) {
                        appWidgetManager.updateAppWidget(id, views)
                    }

                    // 2. Persist to database directly
                    val profile = dao.getUserProfileDirect()
                    val defaultDose = profile?.dailyDoseMg ?: 20

                    val updatedLog = existingLog?.copy(
                        isTaken = isNowTaken,
                        timestamp = System.currentTimeMillis()
                    ) ?: DailyLog(
                        dateString = todayStr,
                        doseMg = defaultDose,
                        isTaken = isNowTaken,
                        timestamp = System.currentTimeMillis()
                    )

                    dao.insertLog(updatedLog)

                    withContext(Dispatchers.Main) {
                        val msg = if (isNowTaken) "تم تأكيد أخذ الجرعة اليومية بنجاح 💊" else "تم إلغاء تأكيد الجرعة"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_DOSE = "com.example.ACTION_TOGGLE_DOSE"

        fun createDoseRemoteViews(context: Context, isTaken: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_dose_layout)

            val toggleIntent = Intent(context, DoseWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_DOSE
                setPackage(context.packageName)
                component = ComponentName(context, DoseWidgetProvider::class.java)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_dose_root, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.tv_dose_icon, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.tv_dose_title, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.tv_dose_status, togglePendingIntent)

            if (isTaken) {
                views.setTextViewText(R.id.tv_dose_title, "تم أخذ الجرعة")
                views.setTextViewText(R.id.tv_dose_status, "جرعة اليوم مكتملة ✓")
                views.setTextViewText(R.id.tv_dose_icon, "✅")
            } else {
                views.setTextViewText(R.id.tv_dose_title, "تأكيد الجرعة")
                views.setTextViewText(R.id.tv_dose_status, "انقر للتأكيد الآن")
                views.setTextViewText(R.id.tv_dose_icon, "💊")
            }
            return views
        }

        suspend fun updateWidgetStateSync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val db = AppDatabase.getDatabase(context)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val log = db.roaccutaneDao().getLogByDate(todayStr)
                val isTaken = log?.isTaken ?: false

                val views = createDoseRemoteViews(context, isTaken)
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
            val intent = Intent(context, DoseWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, DoseWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}

