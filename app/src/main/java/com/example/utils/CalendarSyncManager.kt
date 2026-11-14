package com.example.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.Appointment
import java.util.TimeZone

/**
 * Deterministic Native Android Calendar Synchronizer.
 * Interacts directly with CalendarContract to create, update, and delete events
 * using persistent native calendar event IDs (calendarEventId).
 * 
 * Features:
 * - Specific Title format: Hospital + Clinic + Reason (e.g. "KFUH Derma Follow-up")
 * - Specific Location: Direct Geographic Map URL Link or Hospital/Building
 * - Comprehensive Description: Doctor name, patient file number, lab analysis, notes, status
 * - Color: "توتي" (Mulberry / Berry ARGB: 0xFF88124C)
 * - Automatic Reminders: 5 days before (7200 minutes) automatically for all events, + 1 day and custom
 * - Orphan event recovery and bulk sync of all appointments (past & upcoming)
 */
object CalendarSyncManager {
    private const val TAG = "CalendarSyncManager"

    data class CalendarColorOption(
        val id: String, // Google Calendar color_id "1" to "11"
        val nameAr: String,
        val nameEn: String,
        val argb: Int,
        val hexColor: String
    )

    // Berry / Mulberry / توتي default color
    val BERRY_COLOR_ARGB: Int = 0xFF88124C.toInt()
    const val GOOGLE_CALENDAR_COLOR_ID_BERRY: String = "3" // Grape / Mulberry / توتي in Google 1-11

    val GOOGLE_CALENDAR_COLORS = listOf(
        CalendarColorOption("1", "خزامي (لافندر)", "Lavender", 0xFF7986CB.toInt(), "#7986CB"),
        CalendarColorOption("2", "مرمية (أخضر ناعم)", "Sage", 0xFF33B679.toInt(), "#33B679"),
        CalendarColorOption("3", "توتي (عنبي / Berry)", "Grape / Berry", 0xFF88124C.toInt(), "#88124C"),
        CalendarColorOption("4", "فلامنغو (وردي مرجاني)", "Flamingo", 0xFFE67C73.toInt(), "#E67C73"),
        CalendarColorOption("5", "موزي (أصفر مشرق)", "Banana", 0xFFF6BF26.toInt(), "#F6BF26"),
        CalendarColorOption("6", "يوسفي (برتقالي)", "Tangerine", 0xFFF4511E.toInt(), "#F4511E"),
        CalendarColorOption("7", "طاووسي (أزرق سماوي)", "Peacock", 0xFF039BE5.toInt(), "#039BE5"),
        CalendarColorOption("8", "جرافيت (رمادي)", "Graphite", 0xFF616161.toInt(), "#616161"),
        CalendarColorOption("9", "توت أزرق (كحلي)", "Blueberry", 0xFF3F51B5.toInt(), "#3F51B5"),
        CalendarColorOption("10", "ريحان (أخضر داكن)", "Basil", 0xFF0B8043.toInt(), "#0B8043"),
        CalendarColorOption("11", "طماطمي (أحمر)", "Tomato", 0xFFD50000.toInt(), "#D50000")
    )

    fun getColorOptionById(id: String): CalendarColorOption {
        return GOOGLE_CALENDAR_COLORS.find { it.id == id } ?: GOOGLE_CALENDAR_COLORS[2] // default to 3 (Berry)
    }

    fun getSelectedCalendarColorId(context: Context): String {
        val prefs = context.getSharedPreferences("roaccutane_prefs", Context.MODE_PRIVATE)
        return prefs.getString("calendar_event_color_id", "3") ?: "3"
    }

    fun setSelectedCalendarColorId(context: Context, colorId: String) {
        val prefs = context.getSharedPreferences("roaccutane_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("calendar_event_color_id", colorId).apply()
    }

    fun hasCalendarPermissions(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
        val write = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Resolves an available writable calendar ID on the user's device.
     */
    fun getPrimaryCalendarId(context: Context): Long? {
        if (!hasCalendarPermissions(context)) return null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val isPrimaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                var firstId: Long? = null
                do {
                    val calId = cursor.getLong(idIdx)
                    if (firstId == null) firstId = calId

                    val isPrimary = if (isPrimaryIdx != -1) cursor.getInt(isPrimaryIdx) == 1 else false
                    if (isPrimary) {
                        return calId
                    }
                } while (cursor.moveToNext())

                return firstId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving calendar ID", e)
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Constructs the event title according to user requested format:
     * Hospital Name + Clinic + Reason (e.g. "KFUH Derma Follow-up")
     */
    fun buildEventTitle(appt: Appointment): String {
        val parts = listOf(
            appt.hospitalName.trim(),
            appt.clinic.trim(),
            appt.reason.trim()
        ).filter { it.isNotBlank() }

        return if (parts.isNotEmpty()) {
            parts.joinToString(" ")
        } else {
            "موعد عيادة الجلدية"
        }
    }

    /**
     * Constructs the event location:
     * Puts the geographic map link if present, or physical location info.
     */
    fun buildEventLocation(appt: Appointment): String {
        if (appt.hospitalLocation.isNotBlank()) {
            return appt.hospitalLocation.trim()
        }
        val physical = listOf(
            appt.hospitalName.trim(),
            appt.building.trim(),
            appt.clinic.trim()
        ).filter { it.isNotBlank() }

        return if (physical.isNotEmpty()) physical.joinToString(" - ") else "المستشفى / العيادة"
    }

    /**
     * Constructs comprehensive description mapping all appointment data
     * into their designated sections.
     */
    fun buildEventDescription(appt: Appointment): String {
        val builder = StringBuilder()
        
        if (appt.hospitalName.isNotBlank()) builder.append("🏥 المستشفى: ${appt.hospitalName.trim()}\n")
        if (appt.clinic.isNotBlank()) builder.append("🚪 العيادة / الغرفة: ${appt.clinic.trim()}\n")
        if (appt.building.isNotBlank()) builder.append("🏢 المبنى: ${appt.building.trim()}\n")
        if (appt.doctorName.isNotBlank()) builder.append("👨‍⚕️ الطبيب المعالج: ${appt.doctorName.trim()}\n")
        if (!appt.patientFileNumber.isNullOrBlank()) builder.append("📋 رقم الملف الطبي: ${appt.patientFileNumber!!.trim()}\n")
        if (appt.reason.isNotBlank()) builder.append("🎯 سبب الموعد: ${appt.reason.trim()}\n")
        if (appt.hospitalLocation.isNotBlank()) builder.append("📍 رابط الموقع الجغرافي: ${appt.hospitalLocation.trim()}\n")

        val labs = mutableListOf<String>()
        // Liver Function Tests (LFTs)
        val lftList = mutableListOf<String>()
        if (appt.alt != null) lftList.add("ALT: ${appt.alt} U/L")
        if (appt.ast != null) lftList.add("AST: ${appt.ast} U/L")
        if (appt.bilirubin != null) lftList.add("Bilirubin: ${appt.bilirubin} mg/dL")
        if (appt.alkalinePhosphate != null) lftList.add("ALP: ${appt.alkalinePhosphate} U/L")
        if (appt.albuminInSerum != null) lftList.add("Albumin: ${appt.albuminInSerum} g/dL")
        if (lftList.isNotEmpty()) {
            labs.add("وظائف الكبد: ${lftList.joinToString(" • ")}")
        } else if (appt.liverFunction.isNotBlank()) {
            labs.add("وظائف الكبد: ${appt.liverFunction.trim()}")
        }

        // Kidney Function Tests (KFTs)
        val kftList = mutableListOf<String>()
        if (appt.creatinine != null) kftList.add("Creatinine: ${appt.creatinine} mg/dL")
        if (appt.urea != null) kftList.add("Urea: ${appt.urea} mg/dL")
        if (kftList.isNotEmpty()) {
            labs.add("وظائف الكلى: ${kftList.joinToString(" • ")}")
        } else if (appt.kidneyFunction.isNotBlank()) {
            labs.add("وظائف الكلى: ${appt.kidneyFunction.trim()}")
        }

        // Lipid Profile
        val lipidList = mutableListOf<String>()
        if (appt.tCholestrol != null) lipidList.add("الكوليسترول الكلي: ${appt.tCholestrol}")
        if (appt.triglycerides != null) lipidList.add("الدهون الثلاثية: ${appt.triglycerides}")
        if (appt.hdl != null) lipidList.add("HDL: ${appt.hdl}")
        if (appt.ldl != null) lipidList.add("LDL: ${appt.ldl}")
        if (lipidList.isNotEmpty()) {
            labs.add("دهنيات الدم: ${lipidList.joinToString(" • ")}")
        } else if (appt.lipidsProfile.isNotBlank()) {
            labs.add("دهنيات الدم: ${appt.lipidsProfile.trim()}")
        }

        // Other & Roaccutane Monitoring
        if (appt.glycatedHaemoglobin != null) labs.add("السكر التراكمي (HbA1c): ${appt.glycatedHaemoglobin}%")
        if (appt.otherBloodTests.isNotBlank()) labs.add("تحاليل أخرى: ${appt.otherBloodTests.trim()}")

        if (labs.isNotEmpty()) {
            builder.append("\n🔬 نتائج التحاليل والفحوصات المخبرية:\n")
            labs.forEach { builder.append("• $it\n") }
        }

        if (appt.notes.isNotBlank()) {
            builder.append("\n📝 ملاحظات الموعد:\n${appt.notes.trim()}\n")
        }

        if (appt.isCompleted) {
            builder.append("\n✓ حالة الموعد: مكتمل\n")
        }

        return builder.toString().trim()
    }

    /**
     * Creates an event in the device calendar and returns the generated event_id.
     */
    fun createCalendarEvent(context: Context, appt: Appointment, overrideColorId: String? = null): Long? {
        if (!hasCalendarPermissions(context)) {
            Log.w(TAG, "Cannot create calendar event: missing permissions")
            return null
        }

        val calId = getPrimaryCalendarId(context) ?: return null

        val startMillis = appt.dateTime
        val endMillis = startMillis + (60 * 60 * 1000L) // Default 1 hour duration

        val title = buildEventTitle(appt)
        val location = buildEventLocation(appt)
        val description = buildEventDescription(appt)

        val chosenColorId = overrideColorId ?: getSelectedCalendarColorId(context)
        val colorOpt = getColorOptionById(chosenColorId)

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            // Color_ID: Google Calendar format 1-11
            put(CalendarContract.Events.EVENT_COLOR_KEY, colorOpt.id)
            // Event Color: Exact matching ARGB representation
            put(CalendarContract.Events.EVENT_COLOR, colorOpt.argb)
        }

        return try {
            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()
            Log.d(TAG, "Calendar event created with ID: $eventId, Color: ${colorOpt.nameEn} (ID: ${colorOpt.id})")

            if (eventId != null) {
                configureEventReminders(context, eventId, appt)
            }

            eventId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert calendar event", e)
            null
        }
    }

    /**
     * Updates an existing event by event_id. If the event was deleted externally (orphan event),
     * it recreates the event in the calendar and returns the new event_id.
     */
    fun updateCalendarEvent(context: Context, appt: Appointment, overrideColorId: String? = null): Long? {
        if (!hasCalendarPermissions(context)) return null

        val currentEventId = appt.calendarEventId
        if (currentEventId == null || currentEventId <= 0L) {
            return createCalendarEvent(context, appt, overrideColorId)
        }

        val startMillis = appt.dateTime
        val endMillis = startMillis + (60 * 60 * 1000L)
        val title = buildEventTitle(appt)
        val location = buildEventLocation(appt)
        val description = buildEventDescription(appt)

        val chosenColorId = overrideColorId ?: getSelectedCalendarColorId(context)
        val colorOpt = getColorOptionById(chosenColorId)

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            // Color_ID: Google Calendar format 1-11
            put(CalendarContract.Events.EVENT_COLOR_KEY, colorOpt.id)
            // Event Color: Exact matching ARGB representation
            put(CalendarContract.Events.EVENT_COLOR, colorOpt.argb)
        }

        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, currentEventId)

        return try {
            val rows = context.contentResolver.update(updateUri, values, null, null)
            if (rows > 0) {
                Log.d(TAG, "Calendar event $currentEventId updated successfully with Color: ${colorOpt.nameEn} (ID: ${colorOpt.id})")
                configureEventReminders(context, currentEventId, appt)
                currentEventId
            } else {
                Log.w(TAG, "Event $currentEventId not found on calendar (orphan). Recreating...")
                createCalendarEvent(context, appt, overrideColorId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update calendar event $currentEventId, recreating...", e)
            createCalendarEvent(context, appt, overrideColorId)
        }
    }

    /**
     * Deletes an event from the device calendar using event_id.
     */
    fun deleteCalendarEvent(context: Context, eventId: Long?) {
        if (eventId == null || eventId <= 0L || !hasCalendarPermissions(context)) return

        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            Log.d(TAG, "Deleted calendar event $eventId (rows affected: $rows)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete calendar event $eventId", e)
        }
    }

    /**
     * Configures automatic reminders for the appointment:
     * - Automatic 5 days before reminder (7200 minutes) as requested
     * - 1 day before reminder (1440 minutes)
     * - 2 hours before reminder (120 minutes)
     * - Custom notification time if set
     */
    fun configureEventReminders(context: Context, eventId: Long, appt: Appointment) {
        try {
            // Delete previous reminders for this event to avoid duplicates
            val reminderUri = CalendarContract.Reminders.CONTENT_URI
            context.contentResolver.delete(
                reminderUri,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString())
            )

            // 1. Automatic 5 days reminder (7200 minutes)
            addReminderToEvent(context, eventId, 5 * 24 * 60)

            // 2. 1 day reminder (1440 minutes)
            addReminderToEvent(context, eventId, 24 * 60)

            // 3. 2 hours reminder (120 minutes)
            addReminderToEvent(context, eventId, 120)

            // 4. Custom notification time if configured in app
            if (appt.notificationTimeMillis > 0 && appt.dateTime > appt.notificationTimeMillis) {
                val customMinutes = ((appt.dateTime - appt.notificationTimeMillis) / (60 * 1000L)).toInt()
                if (customMinutes !in listOf(7200, 1440, 120) && customMinutes > 0) {
                    addReminderToEvent(context, eventId, customMinutes)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting reminders for event $eventId", e)
        }
    }

    private fun addReminderToEvent(context: Context, eventId: Long, minutesBefore: Int) {
        try {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.MINUTES, minutesBefore)
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set reminder on event $eventId for $minutesBefore minutes", e)
        }
    }
}
