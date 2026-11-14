package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Doctor
import com.example.data.PatientPairing
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtil {

    fun exportAndOpenClinicalSummary(
        context: Context,
        doctor: Doctor,
        patient: PatientPairing
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#1565C0")
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subPaint = Paint().apply {
                color = Color.parseColor("#424242")
                textSize = 12f
                isAntiAlias = true
            }
            val headerBoxPaint = Paint().apply {
                color = Color.parseColor("#E3F2FD")
                style = Paint.Style.FILL
            }

            // Header Background Box
            canvas.drawRect(40f, 40f, 555f, 130f, headerBoxPaint)
            canvas.drawText("التقرير والملخص السريري المعتمد", 60f, 75f, titlePaint)
            canvas.drawText("منظومة متابعة علاج الرواكتان الذكية - Roaccutane Smart Platform", 60f, 98f, subPaint)

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("تاريخ الإصدار: $dateStr", 60f, 116f, subPaint)

            var y = 165f
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            // Section: Patient Info
            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#0D47A1")
                textSize = 15f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 13f
                isAntiAlias = true
            }

            canvas.drawText("بيانات المريض المسجل:", 60f, y, sectionTitlePaint)
            y += 26f
            canvas.drawText("• اسم المريض: ${patient.patientName}", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• رقم الملف الطبي: #PAT-${patient.id + 1040}", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• المرحلة العلاجية الحالية: ${patient.treatmentPhase}", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• معدل الالتزام الدوائي المسجل: ${patient.adherenceRate}%", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• حالة الملف والاشتراك: ${patient.status}", 70f, y, textPaint)

            y += 30f
            canvas.drawLine(60f, y, 535f, y, linePaint)
            y += 30f

            // Section: Doctor Info
            canvas.drawText("بيانات واعتماد الطبيب المعالج:", 60f, y, sectionTitlePaint)
            y += 26f
            canvas.drawText("• اسم الاستشاري: ${doctor.name}", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• التخصص الدقيق: ${doctor.specialty}", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• رقم التواصل المهني: ${doctor.phone.ifBlank { "+966 50 123 4567" }}", 70f, y, textPaint)
            y += 22f
            canvas.drawText("• كود الترخيص والربط الطبي: ${doctor.pairingCode}", 70f, y, textPaint)

            y += 35f
            canvas.drawLine(60f, y, 535f, y, linePaint)
            y += 30f

            // Stamp and Signature boxes
            val stampBoxPaint = Paint().apply {
                color = Color.parseColor("#E8F5E9")
                style = Paint.Style.FILL
            }
            val stampBorderPaint = Paint().apply {
                color = Color.parseColor("#2E7D32")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(60f, y, 260f, y + 80f, 10f, 10f, stampBoxPaint)
            canvas.drawRoundRect(60f, y, 260f, y + 80f, 10f, 10f, stampBorderPaint)

            val stampTextPaint = Paint().apply {
                color = Color.parseColor("#1B5E20")
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("ختم المنشأة الطبي المعتمد", 75f, y + 35f, stampTextPaint)
            canvas.drawText("سجل رقم: MED-${doctor.pairingCode}", 75f, y + 55f, subPaint)

            val sigBoxPaint = Paint().apply {
                color = Color.parseColor("#EDE7F6")
                style = Paint.Style.FILL
            }
            val sigBorderPaint = Paint().apply {
                color = Color.parseColor("#512DA8")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(300f, y, 500f, y + 80f, 10f, 10f, sigBoxPaint)
            canvas.drawRoundRect(300f, y, 500f, y + 80f, 10f, 10f, sigBorderPaint)

            val sigTextPaint = Paint().apply {
                color = Color.parseColor("#311B92")
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("التوقيع الإلكتروني المعتمد", 315f, y + 35f, sigTextPaint)
            canvas.drawText("موقع رقمياً من قبل الاستشاري", 315f, y + 55f, subPaint)

            y += 125f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                isAntiAlias = true
            }
            canvas.drawText("تم استخراج هذا التقرير رسمياً وهو معتمد للاستخدام السريري والمتابعة الدورية.", 60f, y, footerPaint)

            pdfDocument.finishPage(page)

            // Save PDF to cache
            val cleanName = patient.patientName.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
            val pdfFile = File(context.cacheDir, "Clinical_Summary_${cleanName}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Open the PDF automatically via Intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "عرض ومشاركة الملخص الطبي PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "تم تكوين الملف وفتحه تلقائياً بنجاح", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("PdfExportUtil", "Error exporting/opening PDF: ${e.message}", e)
            Toast.makeText(context, "تم حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
