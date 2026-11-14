package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AffiliateProduct
import com.example.data.DispensedMedication
import com.example.data.MedicalPrescription
import com.example.data.UserProfile
import com.example.data.DailyLog
import com.example.data.RoaccutaneInventoryAudit
import com.example.data.BoxInventoryItem
import com.example.data.BoxConsumptionStatus
import com.example.data.DispensePhaseDetail
import com.example.data.StrengthInventorySummary
import com.example.data.computeRoaccutaneInventoryAudit
import com.example.ui.RoaccutaneViewModel
import com.example.ui.portals.ProductImageThumbnail
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

val DISTINCT_MED_COLORS = listOf(
    "#1976D2", // Royal Blue
    "#2E7D32", // Emerald Green
    "#6A1B9A", // Royal Purple
    "#C2185B", // Crimson Rose
    "#E65100", // Deep Orange
    "#00838F", // Cyan / Teal
    "#455A64"  // Slate Indigo
)

@Composable
fun DispensedMedicationsTrackerDialog(
    appLanguage: String,
    viewModel: RoaccutaneViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val medications by viewModel.allDispensedMedications.collectAsStateWithLifecycle()
    val prescriptions by viewModel.allMedicalPrescriptions.collectAsStateWithLifecycle()
    val approvedProducts by viewModel.allAffiliateProducts.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    LaunchedEffect(medications, allLogs) {
        viewModel.syncMissedDaysFromDailyLogs()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddPrescriptionDialog by remember { mutableStateOf(false) }
    var prescriptionToEdit by remember { mutableStateOf<MedicalPrescription?>(null) }
    var prescriptionToDispense by remember { mutableStateOf<MedicalPrescription?>(null) }
    var prescriptionToDelete by remember { mutableStateOf<MedicalPrescription?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<DispensedMedication?>(null) }
    var selectedMedForMissedDaysEdit by remember { mutableStateOf<DispensedMedication?>(null) }
    var viewingAttachmentUri by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .imePadding()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1976D2).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (appLanguage == "ar") "تتبع الجرعات والوصفات المصروفة" else "Dispensed Prescriptions Tracker",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "ar") "إدارة الوصفات الطبية، تسجيل مرات الصرف وحساب الجرعات" else "Manage prescriptions, multi-dispenses & dose tracking",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Navigation Tabs: Prescriptions vs Dispenses
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = 0 },
                        color = if (selectedTab == 0) Color(0xFF1976D2) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (appLanguage == "ar") "الوصفات الطبية" else "Prescriptions"} (${prescriptions.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = 1 },
                        color = if (selectedTab == 1) Color(0xFF1976D2) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (appLanguage == "ar") "سجل الصرف" else "Dispenses Log"} (${medications.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tab 0: Prescriptions List & Multi-refill management
                if (selectedTab == 0) {
                    // Action Bar: Prescriptions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (appLanguage == "ar") "الوصفات الطبية المعتمدة: ${prescriptions.size}" else "Registered Prescriptions: ${prescriptions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Button(
                            onClick = { showAddPrescriptionDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (appLanguage == "ar") "إضافة وصفة طبية" else "New Prescription",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (prescriptions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1976D2).copy(alpha = 0.1f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MedicalServices,
                                            contentDescription = null,
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (appLanguage == "ar") "لا توجد وصفات طبية مسجلة بعد" else "No medical prescriptions recorded yet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLanguage == "ar")
                                        "الوصفة الطبية يمكن صرفها لأكثر من مرة! أضف الوصفة أولاً وحدد عدد مرات الصرف المسموحة، ثم قم بإدخال بيانات الصرف بنقرة زر لحساب الجرعة والكبسولات المتبقية وتسهيل الإدارة."
                                    else
                                        "A prescription can be dispensed multiple times! Add the prescription first, set allowed refills, then record each dispense with one click.",
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showAddPrescriptionDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (appLanguage == "ar") "إضافة أول وصفة طبية" else "Add First Prescription", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(prescriptions, key = { it.id }) { rx ->
                                val dispensesForThis = medications.filter { it.prescriptionId == rx.id }
                                MedicalPrescriptionCard(
                                    prescription = rx,
                                    dispenses = dispensesForThis,
                                    appLanguage = appLanguage,
                                    onEdit = { prescriptionToEdit = rx },
                                    onDelete = { prescriptionToDelete = rx },
                                    onDispenseNew = { prescriptionToDispense = rx },
                                    onEditDispense = { medicationToEdit = it },
                                    onDeleteDispense = { viewModel.deleteDispensedMedication(it) },
                                    onAddMissedDay = { viewModel.addMissedDaysToDispensedMedication(it, 1) },
                                    onRemoveMissedDay = { viewModel.addMissedDaysToDispensedMedication(it, -1) },
                                    onEditMissedDays = { selectedMedForMissedDaysEdit = it },
                                    onViewAttachment = { uriStr ->
                                        val isImage = uriStr.startsWith("data:image") || uriStr.endsWith(".jpg", ignoreCase = true) || uriStr.endsWith(".jpeg", ignoreCase = true) || uriStr.endsWith(".png", ignoreCase = true)
                                        if (isImage) {
                                            viewingAttachmentUri = uriStr
                                        } else {
                                            handleOpenAttachment(context, uriStr)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Tab 1: Dispenses Log & Timeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (appLanguage == "ar") "الأدوية المصروفة: ${medications.size}" else "Active entries: ${medications.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Button(
                            onClick = {
                                prescriptionToDispense = null
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (appLanguage == "ar") "صرف مباشر / جديد" else "Direct Dispense",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (medications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    text = if (appLanguage == "ar") "لا توجد أدوية أو عمليات صرف مسجلة بعد" else "No dispensed medications tracked yet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (appLanguage == "ar") "اضغط على زر صرف مباشر أو اختر وصفة من تبويب الوصفات لصرفها" else "Tap Direct Dispense above or select a prescription to dispense",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val inventoryAudit = remember(medications, userProfile.startDate, allLogs) {
                            computeRoaccutaneInventoryAudit(
                                medications = medications,
                                courseStartDate = userProfile.startDate,
                                allLogs = allLogs
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                DispensedMedicationsDashboard(
                                    medications = medications,
                                    userProfile = userProfile,
                                    inventoryAudit = inventoryAudit,
                                    appLanguage = appLanguage
                                )
                            }

                            items(medications, key = { it.id }) { med ->
                                DispensedMedicationCard(
                                    item = med,
                                    inventoryAudit = inventoryAudit,
                                    appLanguage = appLanguage,
                                    onEdit = { medicationToEdit = med },
                                    onAddMissedDay = { viewModel.addMissedDaysToDispensedMedication(med.id, 1) },
                                    onRemoveMissedDay = { viewModel.addMissedDaysToDispensedMedication(med.id, -1) },
                                    onEditMissedDays = { selectedMedForMissedDaysEdit = med },
                                    onDelete = { viewModel.deleteDispensedMedication(med.id) },
                                    onViewAttachment = { uriStr ->
                                        val isImage = uriStr.startsWith("data:image") || uriStr.endsWith(".jpg", ignoreCase = true) || uriStr.endsWith(".jpeg", ignoreCase = true) || uriStr.endsWith(".png", ignoreCase = true)
                                        if (isImage) {
                                            viewingAttachmentUri = uriStr
                                        } else {
                                            handleOpenAttachment(context, uriStr)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (appLanguage == "ar") "إغلاق" else "Close")
                    }
                }
            }
        }
    }

    // Add Prescription Subdialog
    if (showAddPrescriptionDialog) {
        AddEditPrescriptionDialog(
            appLanguage = appLanguage,
            prescriptionToEdit = null,
            onDismiss = { showAddPrescriptionDialog = false },
            onSave = { newRx ->
                viewModel.saveMedicalPrescription(newRx)
                showAddPrescriptionDialog = false
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم حفظ الوصفة الطبية ومزامنتها بنجاح!" else "Medical prescription saved & synced!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Edit Prescription Subdialog
    prescriptionToEdit?.let { targetRx ->
        AddEditPrescriptionDialog(
            appLanguage = appLanguage,
            prescriptionToEdit = targetRx,
            onDismiss = { prescriptionToEdit = null },
            onSave = { updatedRx ->
                viewModel.saveMedicalPrescription(updatedRx)
                prescriptionToEdit = null
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم تعديل الوصفة ومزامنتها بنجاح!" else "Medical prescription updated & synced!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Delete Prescription Confirmation Subdialog
    prescriptionToDelete?.let { rx ->
        AlertDialog(
            onDismissRequest = { prescriptionToDelete = null },
            title = {
                Text(
                    text = if (appLanguage == "ar") "حذف الوصفة الطبية" else "Delete Prescription",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (appLanguage == "ar")
                        "هل أنت متأكد من رغبتك في حذف الوصفة \"${rx.prescriptionNumber.ifBlank { rx.medicationName }}\"؟\nسيتم حذف سجلات الصرفيات التابعة لها أيضاً."
                    else
                        "Are you sure you want to delete prescription \"${rx.prescriptionNumber.ifBlank { rx.medicationName }}\"? Its dispenses will also be removed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rxId = rx.id
                        viewModel.deleteMedicalPrescription(rxId)
                        medications.filter { it.prescriptionId == rxId }.forEach {
                            viewModel.deleteDispensedMedication(it.id)
                        }
                        prescriptionToDelete = null
                        Toast.makeText(
                            context,
                            if (appLanguage == "ar") "تم حذف الوصفة الطبية" else "Prescription deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (appLanguage == "ar") "حذف نهائي" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { prescriptionToDelete = null }) {
                    Text(if (appLanguage == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Add / Dispense for Prescription Subdialog
    if (prescriptionToDispense != null) {
        val targetRx = prescriptionToDispense!!
        AddDispensedMedicationDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            approvedProducts = approvedProducts,
            prescriptionToDispense = targetRx,
            medicationToEdit = null,
            onDismiss = { prescriptionToDispense = null },
            onSave = { newItem ->
                viewModel.saveDispensedMedication(newItem)
                prescriptionToDispense = null
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم تسجيل الصرف للوصفة ومزامنته بنجاح!" else "Dispense recorded for prescription & synced!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Direct Add Dispensed Medication Subdialog
    if (showAddDialog) {
        AddDispensedMedicationDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            approvedProducts = approvedProducts,
            prescriptionToDispense = null,
            medicationToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                viewModel.saveDispensedMedication(newItem)
                showAddDialog = false
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم حفظ الصرف ومزامنته بنجاح!" else "Dispense saved & synced!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Edit Dispensed Medication Subdialog
    medicationToEdit?.let { targetMed ->
        AddDispensedMedicationDialog(
            appLanguage = appLanguage,
            viewModel = viewModel,
            approvedProducts = approvedProducts,
            prescriptionToDispense = null,
            medicationToEdit = targetMed,
            onDismiss = { medicationToEdit = null },
            onSave = { updatedItem ->
                viewModel.saveDispensedMedication(updatedItem)
                medicationToEdit = null
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم تعديل الصرف ومزامنته بنجاح!" else "Dispensed record updated & synced!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // View Attachment Image Dialog
    viewingAttachmentUri?.let { imgUri ->
        PrescriptionImagePreviewDialog(
            imageUri = imgUri,
            appLanguage = appLanguage,
            onDismiss = { viewingAttachmentUri = null },
            onOpenExternally = {
                handleOpenAttachment(context, imgUri)
            }
        )
    }

    // Edit Missed Days Dialog
    selectedMedForMissedDaysEdit?.let { targetMed ->
        EditMissedDaysDialog(
            med = targetMed,
            appLanguage = appLanguage,
            onDismiss = { selectedMedForMissedDaysEdit = null },
            onConfirm = { newDays ->
                viewModel.updateDispensedMedicationMissedDays(targetMed.id, newDays)
                selectedMedForMissedDaysEdit = null
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم تمديد تاريخ النهاية بـ $newDays يوم" else "End date extended by $newDays days",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
fun DispensedMedicationsDashboard(
    medications: List<DispensedMedication>,
    userProfile: UserProfile? = null,
    inventoryAudit: RoaccutaneInventoryAudit = remember(medications, userProfile?.startDate) {
        computeRoaccutaneInventoryAudit(
            medications = medications,
            courseStartDate = userProfile?.startDate ?: 0L
        )
    },
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    if (medications.isEmpty()) return

    val audit = inventoryAudit

    val dateFormatter = remember(appLanguage) {
        SimpleDateFormat("EEEE، d MMMM yyyy", if (appLanguage == "ar") Locale("ar") else Locale.US)
    }
    val shortDateFormatter = remember(appLanguage) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    // الأيام المتبقية وتاريخ النفاذ مستخرجة مباشرة من رصيد الكبسولات المتبقية الفعلي وطابور الاستهلاك الزمني (FIFO)
    val daysUntilLatestEnd = audit.remainingDaysCount
    val latestEndDate = audit.estimatedRunOutDateMillis
    val refillReminderDateStr = remember(latestEndDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = latestEndDate
            add(Calendar.DAY_OF_YEAR, -7)
        }
        shortDateFormatter.format(cal.time)
    }

    var isExpanded by rememberSaveable(key = "inventory_audit_expanded") { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dashboard Header & Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (appLanguage == "ar") "تدقيق مخزون كبسولات الروأكتان (Isotretinoin Audit)" else "Roaccutane Capsule Inventory & Dosage Audit",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (appLanguage == "ar") "حساب دقيق للمخزون، الاستهلاك الزمني التراكمي وتوزيع العلب" else "Precise inventory, chronological cumulative intake & box tracking",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = Color(0xFF1976D2)
                    )
                }
            }

            // Top-Level KPI Summary (Quick Glance Bar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Total Dispensed
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1976D2).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "إجمالي المصروف" else "Total Dispensed",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = "${audit.grandTotalDispensedMg} ملغ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = "${audit.grandTotalDispensedCapsules} كبسولة (${audit.grandTotalBoxes} علب)",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Consumed
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE65100).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "المستهلك التراكمي" else "Cumulative Intake",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "${audit.actualCumulativeConsumedMg} ملغ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "${audit.actualCumulativeConsumedCapsules} كبسولة (${audit.consumptionPercent}%)",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Remaining
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "المتبقي الحقيقي" else "True Remaining",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "${audit.trueRemainingMg} ملغ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "${audit.trueRemainingCapsules} كبسولة",
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // ==========================================
                    // STEP 1: حصر المخزون المصروف بالمليغرام وتفصيل التركيزات
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1976D2).copy(alpha = 0.15f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "1",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1976D2)
                                        )
                                    }
                                }
                                Text(
                                    text = if (appLanguage == "ar") "إجمالي الكبسولات المصروفة والمخزون الكلي بالمليغرام:" else "Total Dispensed Capsules & Mg Inventory:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 20mg vs 10mg Strength Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 20mg Card
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1976D2).copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (appLanguage == "ar") "تركيز 20 ملغ" else "Strength 20 mg",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1976D2)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF1976D2).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "${audit.strength20Mg.totalBoxesDispensed} ${if (appLanguage == "ar") "علب" else "boxes"}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1976D2),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${audit.strength20Mg.totalMgDispensed} ملغ",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1976D2)
                                        )

                                        Text(
                                            text = if (appLanguage == "ar")
                                                "${audit.strength20Mg.totalCapsulesDispensed} كبسولة (كل علبة = 600 ملغ)"
                                            else
                                                "${audit.strength20Mg.totalCapsulesDispensed} caps (600 mg / box)",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 10mg Card
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF00838F).copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color(0xFF00838F).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (appLanguage == "ar") "تركيز 10 ملغ" else "Strength 10 mg",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00838F)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF00838F).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "${audit.strength10Mg.totalBoxesDispensed} ${if (appLanguage == "ar") "علب" else "boxes"}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00838F),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${audit.strength10Mg.totalMgDispensed} ملغ",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00838F)
                                        )

                                        Text(
                                            text = if (appLanguage == "ar")
                                                "${audit.strength10Mg.totalCapsulesDispensed} كبسولة (كل علبة = 300 ملغ)"
                                            else
                                                "${audit.strength10Mg.totalCapsulesDispensed} caps (300 mg / box)",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Grand Total Banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "الإجمالي الكلي للمليغرامات المصروفة:" else "Grand Total Dispensed Mg:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${audit.grandTotalDispensedMg} ملغ (${audit.grandTotalDispensedCapsules} كبسولة)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // STEP 2: إجمالي الجرعة الاستهلاكية (التراكمية) الفعلي حتى اليوم
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE65100).copy(alpha = 0.15f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "2",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                                Text(
                                    text = if (appLanguage == "ar") "الجرعة الاستهلاكية التراكمية الفعلية حتى تاريخ اليوم:" else "Actual Cumulative Intake to Date:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Daily Log Confirmation Intake Card (سجل التأكيد اليومي الفعلي)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (audit.hasDailyLogs) Color(0xFF1B5E20).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (audit.hasDailyLogs) Color(0xFF2E7D32).copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = if (audit.hasDailyLogs) Icons.Default.CheckCircle else Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = if (audit.hasDailyLogs) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (audit.hasDailyLogs)
                                                    (if (appLanguage == "ar") "الاستهلاك الفعلي المعتمد على سجل التأكيد اليومي:" else "Actual Intake from Daily Confirmation Log:")
                                                else
                                                    (if (appLanguage == "ar") "الاستهلاك المعتمد حالياً (جدولة زمنية خطية):" else "Intake Basis (Scheduled Timeline):"),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (audit.hasDailyLogs) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (audit.hasDailyLogs) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFE65100).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (audit.hasDailyLogs) (if (appLanguage == "ar") "سجل مؤكد" else "Confirmed Log") else (if (appLanguage == "ar") "حساب مجدول" else "Scheduled"),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (audit.hasDailyLogs) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (audit.hasDailyLogs)
                                                "${if (appLanguage == "ar") "أيام تناول الجرعة المؤكدة بالسجل:" else "Confirmed Intake Days:"} ${audit.confirmedDaysCount} ${if (appLanguage == "ar") "يوماً" else "days"}"
                                            else
                                                "${if (appLanguage == "ar") "الأيام المنقضية منذ البدء:" else "Elapsed Days:"} ${audit.elapsedDaysFromStart} ${if (appLanguage == "ar") "يوماً" else "days"}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${audit.actualCumulativeConsumedMg} ملغ (${audit.actualCumulativeConsumedCapsules} كبسولة)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (audit.hasDailyLogs) Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                    }

                                    if (audit.hasDailyLogs) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${if (appLanguage == "ar") "المجدول زمنياً:" else "Scheduled:"} ${audit.scheduledCumulativeConsumedMg} ملغ",
                                                fontSize = 8.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${if (appLanguage == "ar") "فارق المطابقة (الالتزام):" else "Variance:"} ${if (audit.reconciliationVarianceMg > 0) "+${audit.reconciliationVarianceMg} ملغ (وفر)" else if (audit.reconciliationVarianceMg < 0) "${audit.reconciliationVarianceMg} ملغ (متقدم)" else "0 ملغ (مطابقة تامة)"}",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (audit.reconciliationVarianceMg >= 0) Color(0xFF1976D2) else Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }

                            // Elapsed Days & Start Date Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (appLanguage == "ar") "تاريخ بدء الكورس:" else "Course Start Date:",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = shortDateFormatter.format(Date(audit.courseStartDate)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE65100).copy(alpha = 0.08f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (appLanguage == "ar") "الأيام المنقضية حتى اليوم:" else "Elapsed Days to Date:",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${audit.elapsedDaysFromStart} ${if (appLanguage == "ar") "يوماً" else "days"}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            // Variable Dose Phases Timeline
                            Text(
                                text = if (appLanguage == "ar") "تفصيل الاستهلاك حسب الجرعات اليومية والمراحل الزمنية:" else "Chronological Consumption by Regimen Phase:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            audit.phases.forEach { phase ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (phase.isActiveNow) Color(0xFF1976D2).copy(alpha = 0.08f)
                                    else if (phase.isPast) Color(0xFF2E7D32).copy(alpha = 0.06f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (phase.isActiveNow) Color(0xFF1976D2).copy(alpha = 0.35f)
                                        else if (phase.isPast) Color(0xFF2E7D32).copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (phase.isActiveNow) Color(0xFF1976D2) else if (phase.isPast) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                                ) {
                                                    Text(
                                                        text = "${if (appLanguage == "ar") "المرحلة" else "Phase"} #${phase.phaseNumber}",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${phase.dailyDoseMg} ملغ/يوم (${phase.dailyCapsulesCount} كبسولة بتركيز ${phase.capsuleStrengthMg} ملغ)",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (phase.isActiveNow) Color(0xFF1976D2).copy(alpha = 0.15f)
                                                else if (phase.isPast) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = if (phase.isPast) (if (appLanguage == "ar") "مكتملة بالكامل" else "Completed")
                                                    else if (phase.isActiveNow) (if (appLanguage == "ar") "جارية حالياً" else "Active Now")
                                                    else (if (appLanguage == "ar") "مجدولة قادمة" else "Upcoming"),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (phase.isPast) Color(0xFF2E7D32)
                                                    else if (phase.isActiveNow) Color(0xFF1976D2)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${if (appLanguage == "ar") "المدة:" else "Duration:"} ${phase.durationDays} ${if (appLanguage == "ar") "يوماً" else "days"} • ${if (appLanguage == "ar") "المنقضي:" else "Elapsed:"} ${phase.elapsedActiveDays} ${if (appLanguage == "ar") "يوم" else "days"}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${if (appLanguage == "ar") "المستهلك:" else "Consumed:"} ${phase.phaseConsumedMg} ملغ (${phase.phaseConsumedCapsules} كبسولة)",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (phase.isActiveNow) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // STEP 3: معادلة المتبقي الحقيقي
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, Color(0xFF2E7D32).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "3",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Text(
                                    text = if (appLanguage == "ar") "معادلة المتبقي الحقيقي بالمغ:" else "True Remaining Dose Formula:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Math Formula Display Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (audit.hasDailyLogs) {
                                            if (appLanguage == "ar")
                                                "معادلة المطابقة: (إجمالي المليغرامات المصروفة − إجمالي المليغرامات المؤكد استهلاكها في السجل) = المتبقي الفعلي بالمليغرام"
                                            else
                                                "Formula: (Total Dispensed Mg − Confirmed Daily Log Mg) = True Remaining Mg"
                                        } else {
                                            if (appLanguage == "ar")
                                                "معادلة المتبقي: (إجمالي المليغرامات المصروفة − إجمالي المستهلك المجدول) = المتبقي الحقيقي بالمليغرام"
                                            else
                                                "Formula: (Total Dispensed Mg − Scheduled Consumed Mg) = True Remaining Mg"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (audit.hasDailyLogs) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${audit.grandTotalDispensedMg} ملغ",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1976D2)
                                        )
                                        Text(
                                            text = "−",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "${audit.actualCumulativeConsumedMg} ملغ",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "=",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "${audit.trueRemainingMg} ملغ",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }

                                    Text(
                                        text = "${if (appLanguage == "ar") "المتبقي الفعلي الموزع:" else "True Remaining:"} ${audit.trueRemainingCapsules} ${if (appLanguage == "ar") "كبسولة موزعة على العلب (FIFO)" else "capsules (FIFO)"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            // Progress Bar
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${audit.consumptionPercent}% ${if (appLanguage == "ar") "مستهلك" else "consumed"} (${audit.actualCumulativeConsumedMg} ملغ)",
                                        fontSize = 9.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${100 - audit.consumptionPercent}% ${if (appLanguage == "ar") "متبقي" else "remaining"} (${audit.trueRemainingMg} ملغ)",
                                        fontSize = 9.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (audit.consumptionPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (audit.consumptionPercent >= 90) Color(0xFFE65100) else Color(0xFF1976D2),
                                    trackColor = Color(0xFF2E7D32).copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // ==========================================
                    // STEP 4: توزيع المتبقي على العلب والكبسولات المفردة وترتيب الاستهلاك
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color(0xFF6A1B9A).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF6A1B9A).copy(alpha = 0.15f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "4",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF6A1B9A)
                                        )
                                    }
                                }
                                Text(
                                    text = if (appLanguage == "ar") "توزيع المتبقي على العلب الكاملة والمفتوحة والكبسولات المفردة:" else "Box Distribution & Remaining Capsules Breakdown:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Remaining per strength detail (20mg vs 10mg)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 20mg Distribution
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (appLanguage == "ar") "تركيز 20 ملغ المتبقي:" else "Remaining 20 mg:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1976D2)
                                        )
                                        Text(
                                            text = "${audit.strength20Mg.totalCapsulesRemaining} كبسولة (${audit.strength20Mg.totalMgRemaining} ملغ)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "• ${audit.strength20Mg.fullSealedBoxesRemaining} ${if (appLanguage == "ar") "علب كاملة مغلقة" else "sealed boxes"}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (audit.strength20Mg.hasOpenBox) {
                                            Text(
                                                text = "• ${audit.strength20Mg.openBoxCapsulesRemaining} ${if (appLanguage == "ar") "كبسولة في العلبة المفتوحة (استهلك ${audit.strength20Mg.openBoxCapsulesConsumed})" else "caps in open box (${audit.strength20Mg.openBoxCapsulesConsumed} used)"}",
                                                fontSize = 9.sp,
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = if (appLanguage == "ar") "• لا توجد علبة مفتوحة حالياً" else "• No open box",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 10mg Distribution
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFF00838F).copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (appLanguage == "ar") "تركيز 10 ملغ المتبقي:" else "Remaining 10 mg:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00838F)
                                        )
                                        Text(
                                            text = "${audit.strength10Mg.totalCapsulesRemaining} كبسولة (${audit.strength10Mg.totalMgRemaining} ملغ)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "• ${audit.strength10Mg.fullSealedBoxesRemaining} ${if (appLanguage == "ar") "علب كاملة مغلقة" else "sealed boxes"}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (audit.strength10Mg.hasOpenBox) {
                                            Text(
                                                text = "• ${audit.strength10Mg.openBoxCapsulesRemaining} ${if (appLanguage == "ar") "كبسولة في العلبة المفتوحة (استهلك ${audit.strength10Mg.openBoxCapsulesConsumed})" else "caps in open box (${audit.strength10Mg.openBoxCapsulesConsumed} used)"}",
                                                fontSize = 9.sp,
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = if (appLanguage == "ar") "• لا توجد علبة مفتوحة حالياً" else "• No open box",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Chronological Box Order (FIFO)
                            Text(
                                text = if (appLanguage == "ar") "ترتيب الاستهلاك الزمني بالعلبة (من الأولى للأخيرة FIFO):" else "Chronological Consumption Order by Box (FIFO):",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                audit.boxes.forEach { box ->
                                    val isFullyConsumed = box.status == BoxConsumptionStatus.FULLY_CONSUMED
                                    val isCurrentlyOpen = box.status == BoxConsumptionStatus.CURRENTLY_OPEN
                                    val isSealed = box.status == BoxConsumptionStatus.SEALED_IN_STOCK

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrentlyOpen) Color(0xFFFFF3E0)
                                        else if (isFullyConsumed) Color(0xFFE8F5E9).copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrentlyOpen) Color(0xFFFFB74D)
                                            else if (isFullyConsumed) Color(0xFFA5D6A7).copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFullyConsumed) Icons.Default.CheckCircle
                                                    else if (isCurrentlyOpen) Icons.Default.HourglassTop
                                                    else Icons.Default.Inventory2,
                                                    contentDescription = null,
                                                    tint = if (isFullyConsumed) Color(0xFF2E7D32)
                                                    else if (isCurrentlyOpen) Color(0xFFE65100)
                                                    else Color(0xFF1976D2),
                                                    modifier = Modifier.size(16.dp)
                                                )

                                                Column {
                                                    Text(
                                                        text = "${if (appLanguage == "ar") "علبة" else "Box"} #${box.globalBoxIndex} • ${box.capsuleStrengthMg} ملغ (${box.dispenseName})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${if (appLanguage == "ar") "سعة العلبة:" else "Capacity:"} ${box.totalCapsulesInBox} كبسولة (${box.totalMgInBox} ملغ) • ${shortDateFormatter.format(Date(box.dispenseDate))}",
                                                        fontSize = 8.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isFullyConsumed) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                else if (isCurrentlyOpen) Color(0xFFE65100).copy(alpha = 0.15f)
                                                else Color(0xFF1976D2).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = if (isFullyConsumed) (if (appLanguage == "ar") "مستهلكة بالكامل (30/30)" else "Fully Consumed (30/30)")
                                                    else if (isCurrentlyOpen) (if (appLanguage == "ar") "مفتوحة: متبقي ${box.remainingCapsules} كبسولة" else "Open: ${box.remainingCapsules} caps left")
                                                    else (if (appLanguage == "ar") "مغلقة في المخزون (30/30)" else "Sealed in Stock (30/30)"),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFullyConsumed) Color(0xFF2E7D32)
                                                    else if (isCurrentlyOpen) Color(0xFFE65100)
                                                    else Color(0xFF1976D2),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // Estimated Date of Last Capsule & Refill Alert
                    // ==========================================
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (appLanguage == "ar") "تاريخ آخر كبسولة متوقع (تاريخ النفاذ):" else "Estimated Last Capsule Run-Out Date:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (daysUntilLatestEnd > 7) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color(0xFFE65100).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (daysUntilLatestEnd > 0)
                                            (if (appLanguage == "ar") "متبقي $daysUntilLatestEnd يوم" else "$daysUntilLatestEnd days left")
                                        else
                                            (if (appLanguage == "ar") "نفذت الكمية" else "Finished"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysUntilLatestEnd > 7) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = dateFormatter.format(Date(latestEndDate)),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Detailed Calculation Card for Run-Out & Days Remaining
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (appLanguage == "ar")
                                            "معادلة الأيام المتبقية: إجمالي الكبسولات المتبقية الفعلية (${audit.trueRemainingCapsules} كبسولة) ÷ معدل الاستهلاك اليومي (${audit.dailyCapsulesRate} كبسولات/يوم) = ${audit.remainingDaysCount} يوماً"
                                        else
                                            "Remaining Days Formula: ${audit.trueRemainingCapsules} caps ÷ ${audit.dailyCapsulesRate} caps/day = ${audit.remainingDaysCount} days",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (appLanguage == "ar")
                                            "تاريخ النفاذ المتوقع: تاريخ اليوم (${shortDateFormatter.format(Date(System.currentTimeMillis()))}) + ${audit.remainingDaysCount} يوماً = ${shortDateFormatter.format(Date(latestEndDate))}"
                                        else
                                            "Run-Out Date: Today (${shortDateFormatter.format(Date(System.currentTimeMillis()))}) + ${audit.remainingDaysCount} days = ${shortDateFormatter.format(Date(latestEndDate))}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "ar")
                                            "• مرتبطة تلقائياً بوصفة الصرف الأخيرة في طابور الاستهلاك الزمني (FIFO) لتعكس مدة الـ 60 يوماً كاملة في رصيد الأيام."
                                        else
                                            "• Sequenced in FIFO queue so the latest refill automatically reflects its full 60 days duration in remaining days.",
                                        fontSize = 8.5.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Refill Alert Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (daysUntilLatestEnd <= 7) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, if (daysUntilLatestEnd <= 7) Color(0xFFFFB74D) else Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (daysUntilLatestEnd <= 7) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (daysUntilLatestEnd <= 7) Color(0xFFE65100) else Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (daysUntilLatestEnd <= 7) {
                                    if (appLanguage == "ar")
                                        "تنبيه: متبقي $daysUntilLatestEnd أيام فقط! يرجى حجز موعد مع الطبيب وإجراء التحاليل لصرف الوصفة الجديدة."
                                    else "Alert: Only $daysUntilLatestEnd days left! Book clinic appointment for new refill."
                                } else {
                                    if (appLanguage == "ar")
                                        "المخزون كافٍ — موعد الفحوصات وطلب الوصفة القادمة المقترح: $refillReminderDateStr"
                                    else "Supply is sufficient — Recommended next refill date: $refillReminderDateStr"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (daysUntilLatestEnd <= 7) Color(0xFFE65100) else Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DispensedMedicationCard(
    item: DispensedMedication,
    inventoryAudit: RoaccutaneInventoryAudit? = null,
    appLanguage: String,
    onEdit: () -> Unit,
    onAddMissedDay: () -> Unit,
    onRemoveMissedDay: () -> Unit,
    onEditMissedDays: () -> Unit,
    onDelete: () -> Unit,
    onViewAttachment: (String) -> Unit
) {
    val themeColor = remember(item.badgeColorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.badgeColorHex))
        } catch (_: Exception) {
            Color(0xFF1976D2)
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val startDateStr = remember(item.startDate) { dateFormatter.format(Date(item.startDate)) }
    val endDateStr = remember(item.calculatedEndDate) { dateFormatter.format(Date(item.calculatedEndDate)) }

    val phaseDetail = remember(inventoryAudit, item.id) {
        inventoryAudit?.phases?.firstOrNull { it.dispenseId == item.id }
    }

    val effectiveEndMillis = phaseDetail?.calculatedEndDate ?: item.calculatedEndDate
    val daysRemaining = remember(effectiveEndMillis) {
        val now = System.currentTimeMillis()
        val diff = effectiveEndMillis - now
        if (diff > 0) ((diff / (24L * 3600L * 1000L)).toInt() + 1) else 0
    }

    val fifoBadgeText = when {
        phaseDetail == null -> if (daysRemaining >= 0) (if (appLanguage == "ar") "متبقي $daysRemaining يوم" else "$daysRemaining d left") else (if (appLanguage == "ar") "منتهي" else "Ended")
        phaseDetail.isPast -> if (appLanguage == "ar") "مكتملة بالكامل" else "Fully Consumed"
        phaseDetail.isActiveNow -> {
            val rem = (phaseDetail.durationDays - phaseDetail.elapsedActiveDays).coerceAtLeast(0)
            if (appLanguage == "ar") "جارية: متبقي $rem يوم" else "Active: $rem d left"
        }
        else -> {
            if (appLanguage == "ar") "طابور FIFO (${item.durationDays} يوماً)" else "FIFO Queue (${item.durationDays} d)"
        }
    }
    val fifoBadgeColor = when {
        phaseDetail == null -> if (daysRemaining >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
        phaseDetail.isPast -> Color(0xFF2E7D32)
        phaseDetail.isActiveNow -> Color(0xFFE65100)
        else -> Color(0xFF1976D2)
    }

    // Parse attachments JSON
    val attachmentsList = remember(item.attachedFileUris) {
        parseJsonFileList(item.attachedFileUris)
    }

    var isExpanded by rememberSaveable(item.id, key = "disp_card_${item.id}") { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, themeColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Color Accent Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(themeColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Title, Dose Badge, Action Buttons & Expand Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = themeColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.medicationName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.dispenseNumber > 1 || item.prescriptionId != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = themeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${if (appLanguage == "ar") "صرف #" else "Dispense #"}${item.dispenseNumber}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${item.boxCount} ${if (appLanguage == "ar") "علب" else "packs"} (${item.totalCapsules} كبسولة) • ${item.dailyDose}",
                                fontSize = 11.sp,
                                color = themeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (appLanguage == "ar") "تعديل الوصفة/الدواء" else "Edit Prescription",
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Basic Compact Summary Row (Always Visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.copy(alpha = 0.08f))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${if (appLanguage == "ar") "تاريخ الصرف:" else "Dispensed:"} $startDateStr (${item.durationDays} ${if (appLanguage == "ar") "يوماً" else "days"})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = fifoBadgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = fifoBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = fifoBadgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (isExpanded) (if (appLanguage == "ar") "طي ▲" else "Less ▲") else (if (appLanguage == "ar") "تفاصيل ▼" else "More ▼"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }
                }

                // Collapsible Full Details Section
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                // Concentration, Daily Capsules & Total Active Mg Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = themeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${if (appLanguage == "ar") "تركيز" else "Strength"} ${item.capsuleStrengthMg} ملغ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${item.dailyCapsulesCount} ${if (appLanguage == "ar") "كبسولة/يوم" else "caps/day"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${item.totalDispensedMg} ملغ إجمالي",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Row 2: Duration, Start & Calculated End Date Card
                Surface(
                    color = themeColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (appLanguage == "ar") "تاريخ البداية:" else "Start Date:",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = startDateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.durationDays} ${if (appLanguage == "ar") "يوم مدة الصرف" else "days duration"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (appLanguage == "ar") "تاريخ النهاية المحسوب:" else "Calculated End Date:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            val effectiveEndStr = if (phaseDetail != null) dateFormatter.format(Date(phaseDetail.calculatedEndDate)) else endDateStr
                            Text(
                                text = effectiveEndStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = themeColor
                            )
                            Text(
                                text = when {
                                    phaseDetail == null -> if (daysRemaining >= 0) (if (appLanguage == "ar") "متبقي $daysRemaining يوم حتى النفاذ" else "$daysRemaining days left") else (if (appLanguage == "ar") "انتهت فترة الصرف" else "Period completed")
                                    phaseDetail.isPast -> if (appLanguage == "ar") "اكتمل استهلاكها بالكامل" else "Fully consumed"
                                    phaseDetail.isActiveNow -> {
                                        val rem = (phaseDetail.durationDays - phaseDetail.elapsedActiveDays).coerceAtLeast(0)
                                        if (appLanguage == "ar") "جارية حالياً: متبقي $rem يوم" else "Active: $rem days left"
                                    }
                                    else -> {
                                        if (appLanguage == "ar") "في طابور FIFO: مدة ${item.durationDays} يوماً" else "In FIFO queue: ${item.durationDays} days"
                                    }
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phaseDetail?.isPast == true) Color(0xFF2E7D32) else if (phaseDetail?.isActiveNow == true) Color(0xFFE65100) else themeColor
                            )
                            val itemRemainingMg = if (phaseDetail != null) phaseDetail.phaseRemainingMg else (item.remainingCapsulesCount * (if (item.capsuleStrengthMg > 0) item.capsuleStrengthMg else 20))
                            val itemRemainingCaps = if (phaseDetail != null) phaseDetail.phaseRemainingCapsules else item.remainingCapsulesCount
                            Text(
                                text = if (appLanguage == "ar") "الجرعة المتبقية: $itemRemainingMg ملغ ($itemRemainingCaps كبسولة)" else "Remaining dose: $itemRemainingMg mg ($itemRemainingCaps caps)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                    }
                }

                // Capsule Consumption & Remaining Inventory Progress Tracker
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val totalCaps = item.totalCapsules
                        val myBoxes = remember(inventoryAudit, item.id) {
                            inventoryAudit?.boxes?.filter { it.dispenseId == item.id } ?: emptyList()
                        }
                        val consumedCaps = phaseDetail?.phaseConsumedCapsules 
                            ?: if (myBoxes.isNotEmpty()) myBoxes.sumOf { it.consumedCapsules } 
                            else item.consumedCapsulesCount
                        val remainingCaps = phaseDetail?.phaseRemainingCapsules 
                            ?: if (myBoxes.isNotEmpty()) myBoxes.sumOf { it.remainingCapsules } 
                            else item.remainingCapsulesCount
                        val activeStr = if (item.capsuleStrengthMg > 0) item.capsuleStrengthMg else 20
                        val consumedMg = phaseDetail?.phaseConsumedMg 
                            ?: if (myBoxes.isNotEmpty()) myBoxes.sumOf { it.consumedMg } 
                            else (consumedCaps * activeStr)
                        val remainingMg = phaseDetail?.phaseRemainingMg 
                            ?: if (myBoxes.isNotEmpty()) myBoxes.sumOf { it.remainingMg } 
                            else (remainingCaps * activeStr)
                        val percent = if (totalCaps > 0) ((consumedCaps.toFloat() / totalCaps.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                        val progressVal = if (totalCaps > 0) (consumedCaps.toFloat() / totalCaps.toFloat()).coerceIn(0f, 1f) else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = themeColor, modifier = Modifier.size(15.dp))
                                Text(
                                    text = if (appLanguage == "ar") "حالة استهلاك الكبسولات والمخزون:" else "Capsules Consumption & Inventory:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$percent% ${if (appLanguage == "ar") "مستهلك" else "consumed"}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themeColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressVal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = themeColor,
                            trackColor = themeColor.copy(alpha = 0.15f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Consumed so far
                            Column {
                                Text(
                                    text = if (appLanguage == "ar") "المستهلك الفعلي:" else "Consumed:",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$consumedCaps ${if (appLanguage == "ar") "كبسولة" else "caps"} ($consumedMg ملغ)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Remaining in box
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (appLanguage == "ar") "المتبقي في العلبة:" else "Remaining in Box:",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$remainingCaps ${if (appLanguage == "ar") "كبسولة" else "caps"} ($remainingMg ملغ)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }

                        // Individual Box Breakdown for this dispense
                        if (myBoxes.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (appLanguage == "ar") "حالة علب هذا الصرف (بالترتيب الزمني FIFO):" else "Boxes Status for this Dispense (FIFO):",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                myBoxes.forEach { bItem ->
                                    val bConsumed = bItem.status == BoxConsumptionStatus.FULLY_CONSUMED
                                    val bOpen = bItem.status == BoxConsumptionStatus.CURRENTLY_OPEN
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (bOpen) Color(0xFFFFF3E0)
                                        else if (bConsumed) Color(0xFFE8F5E9).copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (bOpen) Color(0xFFFFB74D)
                                            else if (bConsumed) Color(0xFFA5D6A7).copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${if (appLanguage == "ar") "علبة" else "Box"} #${bItem.boxIndexInDispense} (${bItem.totalCapsulesInBox} كبسولة • ${bItem.totalMgInBox} ملغ)",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (bConsumed) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                else if (bOpen) Color(0xFFE65100).copy(alpha = 0.15f)
                                                else Color(0xFF1976D2).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = if (bConsumed) (if (appLanguage == "ar") "مستهلكة بالكامل (30/30)" else "Fully Consumed (30/30)")
                                                    else if (bOpen) (if (appLanguage == "ar") "مفتوحة: متبقي ${bItem.remainingCapsules} كبسولة" else "Open: ${bItem.remainingCapsules} caps left")
                                                    else (if (appLanguage == "ar") "مغلقة في المخزون (30/30)" else "Sealed in Stock (30/30)"),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (bConsumed) Color(0xFF2E7D32)
                                                    else if (bOpen) Color(0xFFE65100)
                                                    else Color(0xFF1976D2),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Row 3: Missed Doses / Forgotten Compensation Controls
                val effectiveMissedDays = phaseDetail?.missedDays ?: item.missedDays
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = if (effectiveMissedDays > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (appLanguage == "ar") "الجرعات المنسية (تلقائي): $effectiveMissedDays يوم" else "Missed doses (auto): $effectiveMissedDays days",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (effectiveMissedDays > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (appLanguage == "ar") "محسوبة تلقائياً من سجل الجرعات اليومية وتمدد التاريخ لتعويضها" else "Auto-calculated from daily dose history & extends duration",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (effectiveMissedDays > 0) {
                                IconButton(
                                    onClick = onRemoveMissedDay,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                }
                            }

                            Button(
                                onClick = onAddMissedDay,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Day", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (appLanguage == "ar") "+1 يوم منسي" else "+1 Day",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onEditMissedDays,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Days", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Interactive PDF Document Card (Inherited from Master Prescription)
                if (item.prescriptionUrl.isNotBlank()) {
                    val cleanRxLabel = item.prescriptionId?.removePrefix("rx_")?.ifBlank { null }
                        ?: item.medicationName
                    InteractivePrescriptionPdfCard(
                        prescriptionUrl = item.prescriptionUrl,
                        rxNumber = cleanRxLabel,
                        appLanguage = appLanguage
                    )
                }

                // Row 4: Attached Prescription Files (Images/PDFs)
                if (attachmentsList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "الملفات والمستندات المرفقة للوصفة:" else "Attached Prescription Files:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachmentsList) { filePath ->
                                val fileName = getAttachmentDisplayName(filePath, appLanguage)
                                val isPdf = filePath.contains("application/pdf") || filePath.endsWith(".pdf", ignoreCase = true)
                                val isImage = filePath.startsWith("data:image") || filePath.endsWith(".jpg", ignoreCase = true) || filePath.endsWith(".jpeg", ignoreCase = true) || filePath.endsWith(".png", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPdf) Color(0xFFD32F2F).copy(alpha = 0.1f) else themeColor.copy(alpha = 0.1f),
                                    border = BorderStroke(0.8.dp, if (isPdf) Color(0xFFD32F2F).copy(alpha = 0.3f) else themeColor.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { onViewAttachment(filePath) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isImage) {
                                            ProductImageThumbnail(
                                                imageUri = filePath,
                                                contentDescription = fileName,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = if (isPdf) Color(0xFFD32F2F) else themeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = fileName.take(20),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dispense Message / Note & Dispense Photo
                if (item.notes.isNotBlank() || item.dispenseImageUri.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.notes.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(15.dp).padding(top = 2.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (appLanguage == "ar") "رسالة وملاحظة الصرف:" else "Dispense Note / Message:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = item.notes,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (item.dispenseImageUri.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onViewAttachment(item.dispenseImageUri) }
                                        .background(Color.White.copy(alpha = 0.6f))
                                        .padding(6.dp)
                                ) {
                                    ProductImageThumbnail(
                                        imageUri = item.dispenseImageUri,
                                        contentDescription = "Dispense Photo",
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text(
                                            text = if (appLanguage == "ar") "صورة إثبات الصرف (انقر للتكبير)" else "Dispense Photo Proof (Tap to view)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColor
                                        )
                                        Text(
                                            text = if (appLanguage == "ar") "تم حفظها ومزامنتها سحابياً" else "Saved & Synced to Cloud",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
fun AddDispensedMedicationDialog(
    appLanguage: String,
    viewModel: RoaccutaneViewModel,
    approvedProducts: List<AffiliateProduct>,
    prescriptionToDispense: MedicalPrescription? = null,
    medicationToEdit: DispensedMedication? = null,
    onDismiss: () -> Unit,
    onSave: (DispensedMedication) -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val allPrescriptions by viewModel.allMedicalPrescriptions.collectAsState()
    val allDispensedMedications by viewModel.allDispensedMedications.collectAsState()
    val isFirstDispense = remember(medicationToEdit, allDispensedMedications) {
        medicationToEdit == null && allDispensedMedications.isEmpty()
    }
    val lastDefaults = remember { viewModel.getLastDispensedMedicationDefaults() }

    var selectedPrescriptionId by remember {
        mutableStateOf<String?>(prescriptionToDispense?.id ?: medicationToEdit?.prescriptionId)
    }
    var syncWithCourseStart by remember { mutableStateOf(isFirstDispense) }

    // Smart baseline resolution: from Prescription, last defaults, or user profile
    val defaultStrength = remember {
        medicationToEdit?.capsuleStrengthMg
            ?: prescriptionToDispense?.capsuleStrengthMg
            ?: (if (lastDefaults.capsuleStrengthMg > 0) lastDefaults.capsuleStrengthMg
                else if (userProfile.dailyDoseMg > 0 && userProfile.dailyDoseMg % 20 == 0) 20 else 10)
    }
    val defaultDailyCaps = remember {
        medicationToEdit?.dailyCapsulesCount
            ?: prescriptionToDispense?.dailyCapsulesCount
            ?: (if (lastDefaults.dailyCapsulesCount > 0) lastDefaults.dailyCapsulesCount
                else if (userProfile.dailyDoseMg > 0) (userProfile.dailyDoseMg / defaultStrength).coerceAtLeast(1) else 1)
    }
    val defaultDose = remember {
        medicationToEdit?.dailyDose
            ?: prescriptionToDispense?.dailyDose
            ?: (if (lastDefaults.dailyDose.isNotBlank()) lastDefaults.dailyDose
                else if (userProfile.dailyDoseMg > 0) "${userProfile.dailyDoseMg} ملغ" else "20 ملغ")
    }

    var medName by remember {
        mutableStateOf(
            medicationToEdit?.medicationName
                ?: prescriptionToDispense?.medicationName
                ?: lastDefaults.medName.ifBlank { if (appLanguage == "ar") "رواكتان (ايزوتريتينوين)" else "Roaccutane (Isotretinoin)" }
        )
    }
    var selectedProductId by remember { mutableStateOf<String?>(medicationToEdit?.sourceProductId) }
    var boxCount by remember {
        mutableStateOf(
            medicationToEdit?.boxCount
                ?: if (lastDefaults.boxCount > 0) lastDefaults.boxCount else 1
        )
    }
    var capsulesPerBoxInput by remember {
        mutableStateOf(
            (medicationToEdit?.capsulesPerBox
                ?: prescriptionToDispense?.capsulesPerBox
                ?: if (lastDefaults.capsulesPerBox > 0) lastDefaults.capsulesPerBox else 30).toString()
        )
    }
    var capsuleStrengthMgInput by remember {
        mutableStateOf(defaultStrength.toString())
    }
    var dailyCapsulesCountInput by remember {
        mutableStateOf(defaultDailyCaps.toString())
    }
    var dailyDose by remember {
        mutableStateOf(defaultDose)
    }
    var durationDaysInput by remember {
        val defDays = if (prescriptionToDispense != null) {
            val caps = prescriptionToDispense.capsulesPerBox
            val daily = prescriptionToDispense.dailyCapsulesCount.coerceAtLeast(1)
            caps / daily
        } else if (lastDefaults.durationDays > 0) {
            lastDefaults.durationDays
        } else {
            val caps = if (lastDefaults.capsulesPerBox > 0) lastDefaults.capsulesPerBox else 30
            val daily = defaultDailyCaps.coerceAtLeast(1)
            caps / daily
        }
        mutableStateOf((medicationToEdit?.durationDays ?: defDays).toString())
    }
    var startDateMillis by remember {
        mutableStateOf(
            medicationToEdit?.startDate
                ?: if (isFirstDispense && userProfile.startDate > 0L) userProfile.startDate
                else System.currentTimeMillis()
        )
    }
    var selectedColorHex by remember {
        mutableStateOf(
            medicationToEdit?.badgeColorHex
                ?: prescriptionToDispense?.badgeColorHex
                ?: lastDefaults.colorHex.ifBlank { DISTINCT_MED_COLORS.first() }
        )
    }
    var notes by remember { mutableStateOf(medicationToEdit?.notes ?: lastDefaults.notes) }
    var dispenseImageUri by remember { mutableStateOf(medicationToEdit?.dispenseImageUri ?: "") }
    val attachedUris = remember {
        val list = mutableStateListOf<String>()
        if (medicationToEdit != null) {
            list.addAll(parseJsonFileList(medicationToEdit.attachedFileUris))
        }
        list
    }

    var showApprovedProductPicker by remember { mutableStateOf(false) }

    // Visual photo picker launcher for dispense photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val syncable = convertUriToSyncableDataUri(context, uri)
            dispenseImageUri = syncable ?: uri.toString()
        }
    }

    // File picker launcher for images and PDFs
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val syncable = convertUriToSyncableDataUri(context, uri)
            if (syncable != null) {
                attachedUris.add(syncable)
            } else {
                val path = copyUriToInternalStorage(context, uri, "prescriptions")
                if (path != null) {
                    attachedUris.add(path)
                } else {
                    attachedUris.add(uri.toString())
                }
            }
        }
    }

    // Date Picker launcher for start date
    val showStartDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        DatePickerDialog(
            context,
            { _, yr, mo, dy ->
                val chosenCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, yr)
                    set(Calendar.MONTH, mo)
                    set(Calendar.DAY_OF_MONTH, dy)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startDateMillis = chosenCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Calculations based on inputs
    val parsedStrength = (capsuleStrengthMgInput.toIntOrNull() ?: 20).coerceAtLeast(1)
    val parsedDailyCapsules = (dailyCapsulesCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val parsedDuration = durationDaysInput.toIntOrNull() ?: 0
    val parsedCapsulesPerBox = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
    val totalCapsules = (boxCount * parsedCapsulesPerBox).coerceAtLeast(0)
    val totalDispensedMg = totalCapsules * parsedStrength

    // Expected treatment duration from total capsules and daily intake
    val expectedCoverageDays = if (parsedDailyCapsules > 0 && totalCapsules > 0) totalCapsules / parsedDailyCapsules else 0

    // Boxes required if duration is fixed
    val neededBoxesForDuration = if (parsedDuration > 0 && parsedDailyCapsules > 0 && parsedCapsulesPerBox > 0) {
        val totalNeededCapsules = parsedDuration * parsedDailyCapsules
        (totalNeededCapsules + parsedCapsulesPerBox - 1) / parsedCapsulesPerBox
    } else 1

    val calculatedEnd = remember(startDateMillis, parsedDuration) {
        startDateMillis + (parsedDuration.toLong() * 24L * 3600L * 1000L)
    }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startMidnight = remember(startDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val daysPassed = remember(todayMidnight, startMidnight) {
        if (todayMidnight >= startMidnight) {
            ((todayMidnight - startMidnight) / (24L * 3600L * 1000L)).toInt()
        } else {
            0
        }
    }

    val remainingDaysUntilEnd = remember(parsedDuration, daysPassed) {
        if (parsedDuration > 0) maxOf(0, parsedDuration - daysPassed) else 0
    }

    val remainingCapsules = remember(totalCapsules, parsedDuration, daysPassed) {
        if (parsedDuration > 0 && totalCapsules > 0) {
            val dailyRate = totalCapsules.toDouble() / parsedDuration
            val consumed = (daysPassed * dailyRate).toInt().coerceAtMost(totalCapsules)
            maxOf(0, totalCapsules - consumed)
        } else {
            totalCapsules
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember(appLanguage) {
        SimpleDateFormat("EEEE، d MMMM yyyy", if (appLanguage == "ar") Locale("ar") else Locale.US)
    }

    val doseSuggestions = listOf("10 ملغ", "20 ملغ", "30 ملغ", "40 ملغ", "حبة واحدة يومياً", "مرتين يومياً")
    val durationSuggestions = listOf(14, 30, 60, 90, 120)
    val capsulesPerBoxSuggestions = listOf(10, 20, 30, 60, 100)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .imePadding()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (medicationToEdit != null) {
                                if (appLanguage == "ar") "تعديل بيانات الصرف / الدواء" else "Edit Dispensed Record"
                            } else if (prescriptionToDispense != null) {
                                "${if (appLanguage == "ar") "صرف الوصفة:" else "Dispensing:"} ${prescriptionToDispense.prescriptionNumber.ifBlank { prescriptionToDispense.medicationName }}"
                            } else {
                                if (appLanguage == "ar") "صرف وتسجيل دواء جديد" else "Dispense New Medication"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Prescription Linked Info Banner
                if (prescriptionToDispense != null) {
                    val existingForRx = viewModel.allDispensedMedications.value.filter { it.prescriptionId == prescriptionToDispense.id }
                    val nextDispNumber = existingForRx.size + 1
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1976D2).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "${if (appLanguage == "ar") "صرف تالٍ للوصفة الطبية:" else "Refill for Prescription:"} ${prescriptionToDispense.prescriptionNumber.ifBlank { prescriptionToDispense.medicationName }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = if (appLanguage == "ar")
                                        "تسجيل عملية الصرف رقم $nextDispNumber من أصل ${prescriptionToDispense.totalAllowedRefills} مرات مسموحة • ${prescriptionToDispense.dailyDose}"
                                    else
                                        "Recording dispense #$nextDispNumber of ${prescriptionToDispense.totalAllowedRefills} allowed refills • ${prescriptionToDispense.dailyDose}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Auto-fill from Registered Prescription
                if (prescriptionToDispense == null && medicationToEdit == null && allPrescriptions.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1976D2).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (appLanguage == "ar") "تعبئة سريعة من وصفة طبية مسجلة:" else "Fast Auto-fill from Registered Prescription:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            }
                            Text(
                                text = if (appLanguage == "ar")
                                    "انقر على وصفتك لتعبئة اسم الدواء، التركيز، كبسولات العلبة، والجرعة اليومية تلقائياً:"
                                else
                                    "Tap your prescription to automatically populate medication name, strength, capsules, and daily dose:",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allPrescriptions) { rx ->
                                    val isSelected = selectedPrescriptionId == rx.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable {
                                            selectedPrescriptionId = rx.id
                                            medName = rx.medicationName
                                            capsuleStrengthMgInput = rx.capsuleStrengthMg.toString()
                                            dailyCapsulesCountInput = rx.dailyCapsulesCount.toString()
                                            capsulesPerBoxInput = rx.capsulesPerBox.toString()
                                            dailyDose = rx.dailyDose.ifBlank { "${rx.dailyCapsulesCount * rx.capsuleStrengthMg} ملغ" }
                                            selectedColorHex = rx.badgeColorHex
                                            val curBox = boxCount.coerceAtLeast(1)
                                            val curDaily = rx.dailyCapsulesCount.coerceAtLeast(1)
                                            val days = (curBox * rx.capsulesPerBox) / curDaily
                                            durationDaysInput = days.toString()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                            Column {
                                                Text(
                                                    text = rx.prescriptionNumber.ifBlank { rx.medicationName },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${rx.capsuleStrengthMg}mg • ${rx.dailyCapsulesCount} cap/day • ${rx.capsulesPerBox} caps",
                                                    fontSize = 9.sp,
                                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fast Pick from Approved Products Button
                if (approvedProducts.isNotEmpty()) {
                    Button(
                        onClick = { showApprovedProductPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            contentColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appLanguage == "ar") "اختر من مستلزمات العناية المعتمدة بالصفحة الرئيسية" else "Select from Approved Care Products",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Medication Name Field
                OutlinedTextField(
                    value = medName,
                    onValueChange = {
                        medName = it
                        selectedProductId = null
                    },
                    label = { Text(if (appLanguage == "ar") "اسم الدواء / المستحضر *" else "Medication Name *") },
                    placeholder = { Text(if (appLanguage == "ar") "مثال: رواكتان، زيرولوتان، كوراكني..." else "e.g., Roaccutane, Curacne...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Distinct Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (appLanguage == "ar") "لون تمييز الدواء في القائمة:" else "Badge Color Distinction:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DISTINCT_MED_COLORS) { colorHex ->
                            val c = Color(android.graphics.Color.parseColor(colorHex))
                            val isSelected = selectedColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedColorHex = colorHex }
                                    .padding(4.dp),
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
                        }
                    }
                }

                // Start Date Picker Section
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (appLanguage == "ar") "تاريخ الصرف *" else "Dispense Date *",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = { showStartDatePicker() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (appLanguage == "ar") "تعديل التاريخ" else "Change",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isFirstDispense && userProfile.startDate > 0) {
                            val courseStartStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(userProfile.startDate))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(15.dp))
                                    Text(
                                        text = if (appLanguage == "ar")
                                            "أول صرفية في الكورس: تم اعتماد تاريخ البداية تلقائياً من الإعدادات ($courseStartStr)"
                                        else
                                            "First dispense: Adopted course start date from Settings ($courseStartStr)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Text(
                            text = displayDateFormatter.format(Date(startDateMillis)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Quick date selection chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                            val todayCal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }
                            SuggestionChip(
                                onClick = { startDateMillis = todayCal.timeInMillis },
                                label = { Text(if (appLanguage == "ar") "اليوم" else "Today", fontSize = 10.sp) }
                            )
                            if (userProfile.startDate > 0) {
                                val courseStartStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(userProfile.startDate))
                                SuggestionChip(
                                    onClick = { startDateMillis = userProfile.startDate },
                                    label = { Text(if (appLanguage == "ar") "بداية الكورس ($courseStartStr)" else "Course Start ($courseStartStr)", fontSize = 10.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                )
                            }
                            SuggestionChip(
                                onClick = {
                                    val tomCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                    startDateMillis = tomCal.timeInMillis
                                },
                                label = { Text(if (appLanguage == "ar") "غداً" else "Tomorrow", fontSize = 10.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    val yestCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                        add(Calendar.DAY_OF_YEAR, -1)
                                    }
                                    startDateMillis = yestCal.timeInMillis
                                },
                                label = { Text(if (appLanguage == "ar") "أمس" else "Yesterday", fontSize = 10.sp) }
                            )
                            SuggestionChip(
                                onClick = { showStartDatePicker() },
                                label = { Text(if (appLanguage == "ar") "تاريخ مخصص 📅" else "Custom 📅", fontSize = 10.sp) }
                            )
                        }

                        // Course start harmonization switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { syncWithCourseStart = !syncWithCourseStart }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = syncWithCourseStart,
                                onCheckedChange = { syncWithCourseStart = it },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (appLanguage == "ar")
                                    "مزامنة واعتماد هذا التاريخ كبداية رسمية لكورس العلاج في الإعدادات وسجل اليوميات"
                                else
                                    "Sync & set this date as official course start date in Settings & Diary",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 1. Drug Concentration & Daily Capsules Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vaccines,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (appLanguage == "ar") "تركيز الكبسولة والجرعة اليومية (حساب آلي دون تعارض)" else "Capsule Strength & Daily Dose Calculation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Concentration (Strength) & Daily Capsules Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Concentration / Strength Field
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = capsuleStrengthMgInput,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() }
                                        capsuleStrengthMgInput = filtered
                                        val strVal = filtered.toIntOrNull() ?: 0
                                        if (strVal > 0) {
                                            val newDose = parsedDailyCapsules * strVal
                                            dailyDose = "$newDose ملغ"
                                        }
                                    },
                                    label = { Text(if (appLanguage == "ar") "تركيز الكبسولة (ملغ) *" else "Strength (mg) *") },
                                    placeholder = { Text("20") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val strengthChips = listOf(10, 20, 30, 40)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(strengthChips) { st ->
                                        SuggestionChip(
                                            onClick = {
                                                capsuleStrengthMgInput = st.toString()
                                                val newDose = parsedDailyCapsules * st
                                                dailyDose = "$newDose ملغ"
                                            },
                                            label = { Text("$st ملغ", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            // Daily Capsules Count Stepper & Input
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (appLanguage == "ar") "الكبسولات اليومية: *" else "Daily Capsules: *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (parsedDailyCapsules > 1) {
                                                val newCount = parsedDailyCapsules - 1
                                                dailyCapsulesCountInput = newCount.toString()
                                                dailyDose = "${newCount * parsedStrength} ملغ"
                                                val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                                durationDaysInput = ((boxCount * curCaps) / newCount).toString()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(15.dp))
                                    }

                                    OutlinedTextField(
                                        value = dailyCapsulesCountInput,
                                        onValueChange = {
                                            val filtered = it.filter { ch -> ch.isDigit() }
                                            dailyCapsulesCountInput = filtered
                                            val cnt = filtered.toIntOrNull() ?: 1
                                            if (cnt > 0) {
                                                dailyDose = "${cnt * parsedStrength} ملغ"
                                                val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                                durationDaysInput = ((boxCount * curCaps) / cnt).toString()
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            val newCount = parsedDailyCapsules + 1
                                            dailyCapsulesCountInput = newCount.toString()
                                            dailyDose = "${newCount * parsedStrength} ملغ"
                                            val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                            durationDaysInput = ((boxCount * curCaps) / newCount).toString()
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }

                                val dailyCapsuleChips = listOf(1, 2, 3, 4)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(dailyCapsuleChips) { c ->
                                        SuggestionChip(
                                            onClick = {
                                                dailyCapsulesCountInput = c.toString()
                                                dailyDose = "${c * parsedStrength} ملغ"
                                                val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                                durationDaysInput = ((boxCount * curCaps) / c).toString()
                                            },
                                            label = { Text(if (appLanguage == "ar") "$c كبسولة" else "$c caps", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // Daily Dose Input & Synchronizer
                        OutlinedTextField(
                            value = dailyDose,
                            onValueChange = {
                                dailyDose = it
                                val digits = it.filter { ch -> ch.isDigit() }.toIntOrNull()
                                if (digits != null && parsedStrength > 0) {
                                    val calcCount = (digits / parsedStrength).coerceAtLeast(1)
                                    dailyCapsulesCountInput = calcCount.toString()
                                    val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                    durationDaysInput = ((boxCount * curCaps) / calcCount).toString()
                                }
                            },
                            label = { Text(if (appLanguage == "ar") "إجمالي الجرعة اليومية (ملغ) *" else "Total Daily Dose (mg) *") },
                            placeholder = { Text("مثال: 20 ملغ أو 40 ملغ") },
                            supportingText = {
                                Text(
                                    text = if (appLanguage == "ar")
                                        "محسوبة تلقائياً: $parsedDailyCapsules كبسولة × $parsedStrength ملغ = ${parsedDailyCapsules * parsedStrength} ملغ/يوم"
                                    else "Auto-calculated: $parsedDailyCapsules cap × $parsedStrength mg = ${parsedDailyCapsules * parsedStrength} mg/day",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Dose Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(doseSuggestions) { d ->
                                SuggestionChip(
                                    onClick = {
                                        dailyDose = d
                                        val digits = d.filter { ch -> ch.isDigit() }.toIntOrNull()
                                        if (digits != null && parsedStrength > 0) {
                                            val calcCount = (digits / parsedStrength).coerceAtLeast(1)
                                            dailyCapsulesCountInput = calcCount.toString()
                                            val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                            durationDaysInput = ((boxCount * curCaps) / calcCount).toString()
                                        }
                                    },
                                    label = { Text(d, fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }
                }

                // 2. Boxes & Capsules per Box Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Box Count Stepper
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "عدد العلب المصروفة: *" else "Box count: *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (boxCount > 1) {
                                        boxCount--
                                        val daily = (dailyCapsulesCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
                                        val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                        durationDaysInput = ((boxCount * curCaps) / daily).toString()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$boxCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    boxCount++
                                    val daily = (dailyCapsulesCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
                                    val curCaps = (capsulesPerBoxInput.toIntOrNull() ?: 30).coerceAtLeast(1)
                                    durationDaysInput = ((boxCount * curCaps) / daily).toString()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Capsules per Box
                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = capsulesPerBoxInput,
                            onValueChange = {
                                val filtered = it.filter { ch -> ch.isDigit() }
                                capsulesPerBoxInput = filtered
                                val caps = filtered.toIntOrNull() ?: 30
                                if (caps > 0) {
                                    val daily = (dailyCapsulesCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
                                    durationDaysInput = ((boxCount * caps) / daily).toString()
                                }
                            },
                            label = { Text(if (appLanguage == "ar") "الكبسولات في كل علبة *" else "Capsules per Box *") },
                            placeholder = { Text("30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(capsulesPerBoxSuggestions) { cCount ->
                                SuggestionChip(
                                    onClick = {
                                        capsulesPerBoxInput = cCount.toString()
                                        val daily = (dailyCapsulesCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
                                        durationDaysInput = ((boxCount * cCount) / daily).toString()
                                    },
                                    label = { Text(if (appLanguage == "ar") "$cCount كبسولة" else "$cCount caps", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // 3. Duration in Days
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = durationDaysInput,
                        onValueChange = { durationDaysInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (appLanguage == "ar") "المدة بالأيام (Duration Days) *" else "Duration (Days) *") },
                        placeholder = { Text(if (appLanguage == "ar") "أدخل عدد الأيام..." else "Enter days...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Duration Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(durationSuggestions) { days ->
                            SuggestionChip(
                                onClick = { durationDaysInput = days.toString() },
                                label = { Text(if (appLanguage == "ar") "$days يوم" else "$days days", fontSize = 10.5.sp) }
                            )
                        }
                    }
                }

                // 4. Zero Data Conflict Smart Calculator Card (منع تعارض البيانات)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (expectedCoverageDays == parsedDuration) Color(0xFF2E7D32).copy(alpha = 0.08f) else Color(0xFFE65100).copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, if (expectedCoverageDays == parsedDuration) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFE65100).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (expectedCoverageDays == parsedDuration) Icons.Default.CheckCircle else Icons.Default.SyncProblem,
                                contentDescription = null,
                                tint = if (expectedCoverageDays == parsedDuration) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (appLanguage == "ar") "ميزان التناسق الطبي ومنع تعارض البيانات:" else "Medical Consistency & Zero-Conflict Check:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (expectedCoverageDays == parsedDuration) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }

                        Text(
                            text = if (appLanguage == "ar")
                                "• إجمالي الكبسولات المصروفة: $totalCapsules كبسولة ($boxCount علبة × $parsedCapsulesPerBox كبسولة) بإجمالي $totalDispensedMg ملغ.\n• بمعدل $parsedDailyCapsules كبسولة/يوم (${parsedDailyCapsules * parsedStrength} ملغ)، تكفي الكمية المصروفة لمدة $expectedCoverageDays يوماً."
                            else
                                "• Total dispensed: $totalCapsules caps ($boxCount boxes × $parsedCapsulesPerBox caps) = $totalDispensedMg mg total.\n• At $parsedDailyCapsules caps/day (${parsedDailyCapsules * parsedStrength} mg), this supply covers $expectedCoverageDays days.",
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (expectedCoverageDays != parsedDuration && parsedDuration > 0) {
                            HorizontalDivider(color = Color(0xFFE65100).copy(alpha = 0.2f))
                            Text(
                                text = if (appLanguage == "ar")
                                    "⚠️ تنبيه: المدة المدخلة ($parsedDuration يوم) لا تتطابق مع كمية الكبسولات المصروفة ($expectedCoverageDays يوم). اختر إجراءً للمزامنة الفورية:"
                                else
                                    "⚠️ Duration mismatch: $parsedDuration days vs $expectedCoverageDays days from capsule supply. Tap to sync:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { durationDaysInput = expectedCoverageDays.toString() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "مزامنة المدة ($expectedCoverageDays يوم)" else "Sync Duration ($expectedCoverageDays d)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { boxCount = neededBoxesForDuration },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "مزامنة العلب ($neededBoxesForDuration علب)" else "Sync Boxes ($neededBoxesForDuration)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (appLanguage == "ar") "✅ البيانات متوافقة تماماً بين الجرعة اليومية، عدد الكبسولات، ومدة الصرف." else "✅ Capsule count, daily dose, and duration are perfectly aligned.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // Live Preview & Calculations Card
                Surface(
                    color = Color(android.graphics.Color.parseColor(selectedColorHex)).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(android.graphics.Color.parseColor(selectedColorHex)).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Calculation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (appLanguage == "ar") "تاريخ البداية:" else "Start Date:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(Date(startDateMillis)),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (appLanguage == "ar") "تاريخ النهاية المحسوب:" else "Calculated End:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(android.graphics.Color.parseColor(selectedColorHex))
                                )
                                Text(
                                    text = dateFormatter.format(Date(calculatedEnd)),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(android.graphics.Color.parseColor(selectedColorHex))
                                )
                            }
                        }

                        HorizontalDivider(color = Color(android.graphics.Color.parseColor(selectedColorHex)).copy(alpha = 0.2f))

                        // Live Capsules & Days Remaining Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Remaining Days
                            Column {
                                Text(
                                    text = if (appLanguage == "ar") "الأيام المتبقية حتى النفاذ:" else "Days until empty:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (remainingDaysUntilEnd > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (remainingDaysUntilEnd > 0) {
                                            if (appLanguage == "ar") "$remainingDaysUntilEnd يوم متبقٍ" else "$remainingDaysUntilEnd days left"
                                        } else {
                                            if (appLanguage == "ar") "انتهت فترة الصرف" else "Completed"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (remainingDaysUntilEnd > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                if (daysPassed > 0) {
                                    Text(
                                        text = if (appLanguage == "ar") "انقضى منها $daysPassed يوم" else "$daysPassed days elapsed",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Remaining Capsules
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (appLanguage == "ar") "عدد الكبسولات المتبقية:" else "Remaining capsules:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(android.graphics.Color.parseColor(selectedColorHex))
                                    )
                                    Text(
                                        text = if (appLanguage == "ar") "$remainingCapsules من أصل $totalCapsules كبسولة" else "$remainingCapsules of $totalCapsules caps",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(android.graphics.Color.parseColor(selectedColorHex))
                                    )
                                }
                                Text(
                                    text = if (appLanguage == "ar") "($boxCount علبة × $parsedCapsulesPerBox كبسولة)" else "($boxCount box × $parsedCapsulesPerBox)",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // File and PDF Attachment Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (appLanguage == "ar") "المرفقات (صور الوصفة أو ملفات PDF):" else "Attachments (Photos & PDFs):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (appLanguage == "ar") "إرفاق ملف" else "Attach", fontSize = 11.sp)
                            }
                        }

                        if (attachedUris.isEmpty()) {
                            Text(
                                text = if (appLanguage == "ar") "لم يتم إرفاق صور أو تقارير PDF بعد (اختياري)" else "No files attached yet (optional)",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                attachedUris.forEachIndexed { index, uriStr ->
                                    val fileName = getAttachmentDisplayName(uriStr, appLanguage)
                                    val isPdf = uriStr.contains("application/pdf") || uriStr.endsWith(".pdf", ignoreCase = true)
                                    val isImage = uriStr.startsWith("data:image") || uriStr.endsWith(".jpg", ignoreCase = true) || uriStr.endsWith(".jpeg", ignoreCase = true) || uriStr.endsWith(".png", ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (isImage) {
                                                    ProductImageThumbnail(
                                                        imageUri = uriStr,
                                                        contentDescription = fileName,
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(RoundedCornerShape(4.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.AttachFile,
                                                        contentDescription = null,
                                                        tint = if (isPdf) Color(0xFFD32F2F) else Color(0xFF1976D2),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = fileName,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { attachedUris.removeAt(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dedicated Dispense Message & Photo Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Text(
                                text = if (appLanguage == "ar") "رسالة وصورة الصرف (مزامنة سحابية):" else "Dispense Note & Photo Proof (Cloud Synced):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (appLanguage == "ar") "رسالة وملاحظة الصرف (تظهر في السجل وتتزامن سحابياً)" else "Dispense note / message (synced)") },
                            placeholder = { Text(if (appLanguage == "ar") "مثال: تم صرف العلب من صيدلية النهدي..." else "e.g. Dispensed from pharmacy...") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Dispense Photo Proof Picker & Preview
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (appLanguage == "ar") "صورة الصرف (فاتورة / علب الدواء):" else "Dispense Photo Proof:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (dispenseImageUri.isNotBlank()) {
                                            if (appLanguage == "ar") "تغيير الصورة" else "Change Photo"
                                        } else {
                                            if (appLanguage == "ar") "إضافة صورة للصرف" else "Add Dispense Photo"
                                        },
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            if (dispenseImageUri.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            ProductImageThumbnail(
                                                imageUri = dispenseImageUri,
                                                contentDescription = "Dispense Preview",
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column {
                                                Text(
                                                    text = if (appLanguage == "ar") "صورة الصرف المرفقة" else "Attached Dispense Photo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (appLanguage == "ar") "جاهزة للحفظ والمزامنة السحابية" else "Ready to save & sync to cloud",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        IconButton(onClick = { dispenseImageUri = "" }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (appLanguage == "ar") "إلغاء" else "Cancel")
                    }

                    Button(
                        onClick = {
                            // Incomplete data validation
                            val missingFields = mutableListOf<String>()
                            if (medName.trim().isBlank()) {
                                missingFields.add(if (appLanguage == "ar") "اسم الدواء / المستحضر" else "Medication Name")
                            }
                            if (dailyDose.trim().isBlank()) {
                                missingFields.add(if (appLanguage == "ar") "الجرعة اليومية" else "Daily Dose")
                            }
                            if (parsedDuration <= 0) {
                                missingFields.add(if (appLanguage == "ar") "المدة بالأيام (يجب إدخال عدد أكبر من صفر)" else "Duration in Days (must be > 0)")
                            }
                            if (boxCount <= 0) {
                                missingFields.add(if (appLanguage == "ar") "عدد العلب (يجب أن يكون 1 على الأقل)" else "Box Count")
                            }
                            if (parsedCapsulesPerBox <= 0) {
                                missingFields.add(if (appLanguage == "ar") "عدد الكبسولات بالعلبة" else "Capsules per Box")
                            }

                            if (missingFields.isNotEmpty()) {
                                val errorMsg = if (appLanguage == "ar") {
                                    "يرجى إكمال البيانات المطلوبة:\n• " + missingFields.joinToString("\n• ")
                                } else {
                                    "Please provide required info:\n• " + missingFields.joinToString("\n• ")
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            // Save and persist defaults for future entries
                            viewModel.saveLastDispensedMedicationDefaults(
                                medName = medName.trim(),
                                dailyDose = dailyDose.trim(),
                                durationDays = parsedDuration,
                                boxCount = boxCount,
                                capsulesPerBox = parsedCapsulesPerBox,
                                colorHex = selectedColorHex,
                                notes = notes.trim(),
                                capsuleStrengthMg = parsedStrength,
                                dailyCapsulesCount = parsedDailyCapsules
                            )

                            val syncableList = attachedUris.map { uriOrPath ->
                                if (uriOrPath.startsWith("data:")) uriOrPath
                                else convertLocalFileToSyncableDataUri(uriOrPath) ?: uriOrPath
                            }
                            val filesJson = JSONArray(syncableList).toString()

                            val pId = selectedPrescriptionId ?: prescriptionToDispense?.id ?: medicationToEdit?.prescriptionId
                            val parentRx = if (pId != null) allPrescriptions.firstOrNull { it.id == pId } ?: prescriptionToDispense else prescriptionToDispense
                            val inheritedUrl = parentRx?.prescriptionUrl?.ifBlank { null }
                                ?: medicationToEdit?.prescriptionUrl?.ifBlank { null }
                                ?: ""
                            val dispNum = if (medicationToEdit != null) {
                                medicationToEdit.dispenseNumber
                            } else if (pId != null) {
                                val existingForRx = viewModel.allDispensedMedications.value.filter { it.prescriptionId == pId }
                                existingForRx.size + 1
                            } else 1

                            if (syncWithCourseStart) {
                                viewModel.syncTreatmentStartDate(startDateMillis)
                            }

                            val item = DispensedMedication(
                                id = medicationToEdit?.id ?: "med_${UUID.randomUUID().toString().take(8)}",
                                medicationName = medName.trim(),
                                sourceProductId = selectedProductId,
                                boxCount = boxCount,
                                dailyDose = dailyDose.trim(),
                                durationDays = parsedDuration,
                                startDate = startDateMillis,
                                missedDays = medicationToEdit?.missedDays ?: 0,
                                badgeColorHex = selectedColorHex,
                                notes = notes.trim(),
                                attachedFileUris = filesJson,
                                prescriptionUrl = inheritedUrl,
                                dispenseImageUri = dispenseImageUri,
                                capsuleStrengthMg = parsedStrength,
                                dailyCapsulesCount = parsedDailyCapsules,
                                capsulesPerBox = parsedCapsulesPerBox,
                                prescriptionId = pId,
                                dispenseNumber = dispNum,
                                createdAt = medicationToEdit?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(item)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (medicationToEdit != null) (if (appLanguage == "ar") "حفظ التعديلات" else "Save Changes") else (if (appLanguage == "ar") "حفظ وصرف" else "Save & Dispense"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Approved Product Picker Dialog
    if (showApprovedProductPicker) {
        Dialog(
            onDismissRequest = { showApprovedProductPicker = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (appLanguage == "ar") "اختر من مستلزمات العناية المعتمدة" else "Select Approved Care Product",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(approvedProducts) { prod ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        medName = prod.productNameAr
                                        selectedProductId = prod.id
                                        notes = prod.descriptionAr
                                        showApprovedProductPicker = false
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (prod.imageUri.isNotBlank()) {
                                        ProductImageThumbnail(
                                            imageUri = prod.imageUri,
                                            contentDescription = prod.productNameAr,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.productNameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("${prod.categoryAr} • ${prod.partnerName}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showApprovedProductPicker = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (appLanguage == "ar") "إلغاء" else "Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun EditMissedDaysDialog(
    med: DispensedMedication,
    appLanguage: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputDaysText by remember { mutableStateOf(med.missedDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (appLanguage == "ar") "تعديل عدد الأيام المنسية" else "Edit Missed Doses Days",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (appLanguage == "ar")
                        "أدخل عدد الأيام التي تم نسيان تناول دواء \"${med.medicationName}\" فيها لتمديد تاريخ النهاية:"
                    else "Enter missed dose days count to extend the end date:"
                )
                OutlinedTextField(
                    value = inputDaysText,
                    onValueChange = { inputDaysText = it },
                    label = { Text(if (appLanguage == "ar") "عدد الأيام" else "Days Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputDaysText.toIntOrNull() ?: med.missedDays
                    onConfirm(parsed.coerceAtLeast(0))
                }
            ) {
                Text(if (appLanguage == "ar") "تأكيد وتمديد" else "Confirm & Extend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (appLanguage == "ar") "إلغاء" else "Cancel")
            }
        }
    )
}

fun parseJsonFileList(jsonStr: String): List<String> {
    if (jsonStr.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        list
    } catch (_: Exception) {
        if (jsonStr.contains(",")) {
            jsonStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(jsonStr.trim())
        }
    }
}

fun getAttachmentDisplayName(attachmentStr: String, appLanguage: String): String {
    return if (attachmentStr.contains(";name=")) {
        attachmentStr.substringAfter(";name=").substringBefore(";base64,")
    } else if (attachmentStr.startsWith("data:application/pdf")) {
        if (appLanguage == "ar") "وصفة_طبية.pdf" else "prescription.pdf"
    } else if (attachmentStr.startsWith("data:image")) {
        if (appLanguage == "ar") "صورة_الوصفة.jpg" else "prescription_photo.jpg"
    } else if (attachmentStr.contains("/")) {
        File(attachmentStr).name
    } else {
        if (appLanguage == "ar") "مرفق_الوصفة" else "attachment"
    }
}

fun convertUriToSyncableDataUri(context: Context, uri: Uri): String? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: when {
            uri.toString().endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            uri.toString().endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "image/jpeg"
        }

        var fileName = "prescription"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val rawName = cursor.getString(nameIndex)
                    if (!rawName.isNullOrBlank()) {
                        fileName = rawName
                    }
                }
            }
        } catch (_: Exception) {}

        if (mimeType.startsWith("image")) {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            val maxDim = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDim || height > maxDim) {
                maxDim.toFloat() / maxOf(width, height)
            } else 1f

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else originalBitmap

            val outStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
            val byteArray = outStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val safeName = fileName.replace(';', '_').replace('=', '_')
            "data:image/jpeg;name=$safeName;base64,$base64"
        } else {
            // PDF or other document
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            if (bytes.size <= 750_000) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val safeName = fileName.replace(';', '_').replace('=', '_')
                "data:application/pdf;name=$safeName;base64,$base64"
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}

fun convertLocalFileToSyncableDataUri(filePath: String): String? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null
        val fileName = file.name
        val safeName = fileName.replace(';', '_').replace('=', '_')
        if (filePath.endsWith(".pdf", ignoreCase = true)) {
            if (file.length() <= 750_000) {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:application/pdf;name=$safeName;base64,$base64"
            } else null
        } else {
            // Image
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val maxDim = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDim || height > maxDim) {
                maxDim.toFloat() / maxOf(width, height)
            } else 1f

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else originalBitmap

            val outStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
            val byteArray = outStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;name=$safeName;base64,$base64"
        }
    } catch (_: Exception) {
        null
    }
}

fun handleOpenAttachment(context: Context, filePathOrUri: String) {
    try {
        if (filePathOrUri.startsWith("data:")) {
            val isPdf = filePathOrUri.contains("application/pdf")
            val base64Part = filePathOrUri.substringAfter("base64,")
            val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
            val extension = if (isPdf) ".pdf" else ".jpg"
            val mimeType = if (isPdf) "application/pdf" else "image/jpeg"

            val cacheDir = File(context.cacheDir, "prescriptions_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val fileName = if (filePathOrUri.contains(";name=")) {
                filePathOrUri.substringAfter(";name=").substringBefore(";base64,")
            } else {
                "prescription_${System.currentTimeMillis()}$extension"
            }

            val targetFile = File(cacheDir, fileName.ifBlank { "prescription_${System.currentTimeMillis()}$extension" })
            FileOutputStream(targetFile).use { fos ->
                fos.write(decodedBytes)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.roaccutanetracking.ndsqpx.fileprovider",
                targetFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        val file = File(filePathOrUri)
        val uri: Uri = if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                "com.aistudio.roaccutanetracking.ndsqpx.fileprovider",
                file
            )
        } else {
            Uri.parse(filePathOrUri)
        }

        val isPdf = filePathOrUri.endsWith(".pdf", ignoreCase = true)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (isPdf) "application/pdf" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح الملف مباشرة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PrescriptionImagePreviewDialog(
    imageUri: String,
    appLanguage: String,
    onDismiss: () -> Unit,
    onOpenExternally: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "ar") "معاينة مستند الوصفة" else "Prescription Document Preview",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Image Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    ProductImageThumbnail(
                        imageUri = imageUri,
                        contentDescription = "Prescription Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (appLanguage == "ar") "إغلاق" else "Close")
                    }

                    Button(
                        onClick = onOpenExternally,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (appLanguage == "ar") "فتح في المعرض" else "Open in Viewer")
                    }
                }
            }
        }
    }
}
