package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AppointmentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = intent.getStringExtra("reason") ?: "Hospital consultation"
        val hospital = intent.getStringExtra("hospital") ?: "Medical Center"
        val doctor = intent.getStringExtra("doctor") ?: "Dermatologist"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "appointments_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Roaccutane Appointment Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notify users of upcoming dermatology and laboratory tests checkups"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val titleLocal = "🏥 تذكير موعد الرواكتان | Roaccutane Appointment"
        val msgLocal = "لديك موعد في $hospital مع د. $doctor بخصوص: $reason"
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleLocal)
            .setContentText(msgLocal)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msgLocal))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
