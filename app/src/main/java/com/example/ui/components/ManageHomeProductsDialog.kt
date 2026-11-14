package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AffiliateProduct
import com.example.ui.RoaccutaneViewModel
import com.example.ui.portals.ProductDetailsPopupDialog
import com.example.ui.portals.ProductImageThumbnail
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

@Composable
fun ManageHomeProductsDialog(
    appLanguage: String,
    viewModel: RoaccutaneViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allAffiliateProducts.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<AffiliateProduct?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<AffiliateProduct?>(null) }
    var productToPreview by remember { mutableStateOf<AffiliateProduct?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(12.dp)
                .imePadding()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (appLanguage == "ar") "إدارة مستلزمات العناية والمنتجات" else "Manage Care Products",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "ar") "المنتجات المعروضة في الصفحة الرئيسية" else "Items displayed on Home Screen",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Action Bar (Add Product Button & Count)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "ar") "إجمالي المنتجات: ${products.size}" else "Total products: ${products.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Button(
                        onClick = {
                            productToEdit = null
                            showEditDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (appLanguage == "ar") "إضافة منتج جديد" else "Add New Product",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Products List
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (appLanguage == "ar") "لا توجد منتجات مسجلة حالياً" else "No products found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (appLanguage == "ar") "اضغط على زر إضافة منتج جديد بالاعلى لإدراج مستلزمات العناية" else "Tap add new product to start adding items",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products, key = { it.id }) { item ->
                            ProductManageItemCard(
                                item = item,
                                appLanguage = appLanguage,
                                onClick = {
                                    productToPreview = item
                                },
                                onEdit = {
                                    productToEdit = item
                                    showEditDialog = true
                                },
                                onDelete = {
                                    showDeleteConfirmDialog = item
                                }
                            )
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (appLanguage == "ar") "تم الانتهاء" else "Done")
                    }
                }
            }
        }
    }

    // Edit/Add Subdialog
    if (showEditDialog) {
        ProductEditorDialog(
            product = productToEdit,
            appLanguage = appLanguage,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProduct ->
                viewModel.saveAffiliateProduct(updatedProduct)
                showEditDialog = false
                Toast.makeText(
                    context,
                    if (appLanguage == "ar") "تم حفظ المنتج بنجاح!" else "Product saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { target ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = if (appLanguage == "ar") "تأكيد حذف المنتج" else "Confirm Delete",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (appLanguage == "ar")
                        "هل أنت متأكد من حذف المنتج \"${target.productNameAr}\"؟ سيتم حذفه من الصفحة الرئيسية والمزامنة السحابية."
                    else "Are you sure you want to delete \"${target.productNameAr}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAffiliateProduct(target.id, target.productNameAr)
                        showDeleteConfirmDialog = null
                        Toast.makeText(
                            context,
                            if (appLanguage == "ar") "تم حذف المنتج" else "Product deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (appLanguage == "ar") "نعم، حذف" else "Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(if (appLanguage == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    productToPreview?.let { prod ->
        ProductDetailsPopupDialog(
            product = prod,
            appLanguage = appLanguage,
            onDismiss = { productToPreview = null }
        )
    }
}

@Composable
private fun ProductManageItemCard(
    item: AffiliateProduct,
    appLanguage: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUri.isNotBlank()) {
                    ProductImageThumbnail(
                        imageUri = item.imageUri,
                        contentDescription = item.productNameAr,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Info Details
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.productNameAr,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.categoryAr,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.partnerName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.partnerName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (item.descriptionAr.isNotBlank()) {
                    Text(
                        text = item.descriptionAr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.2f", item.priceSAR)} ${if (appLanguage == "ar") "ر.س" else "SAR"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                // Manufacturer & Pharmacy URLs
                if (item.manufacturerUrl.isNotBlank() || item.pharmacyUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        if (item.manufacturerUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    try {
                                        val u = if (item.manufacturerUrl.startsWith("http://") || item.manufacturerUrl.startsWith("https://")) item.manufacturerUrl else "https://${item.manufacturerUrl}"
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                    } catch (_: Exception) {
                                        Toast.makeText(ctx, if (appLanguage == "ar") "تعذر فتح رابط الشركة المنتجة" else "Cannot open manufacturer URL", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(
                                        text = if (appLanguage == "ar") "الشركة المنتجة" else "Manufacturer",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        if (item.pharmacyUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1976D2).copy(alpha = 0.12f),
                                modifier = Modifier.clickable {
                                    try {
                                        val u = if (item.pharmacyUrl.startsWith("http://") || item.pharmacyUrl.startsWith("https://")) item.pharmacyUrl else "https://${item.pharmacyUrl}"
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                    } catch (_: Exception) {
                                        Toast.makeText(ctx, if (appLanguage == "ar") "تعذر فتح رابط الصيدلية" else "Cannot open pharmacy URL", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.LocalPharmacy, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF1976D2))
                                    Text(
                                        text = if (appLanguage == "ar") "رابط الصيدلية" else "Pharmacy",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductEditorDialog(
    product: AffiliateProduct?,
    appLanguage: String,
    onDismiss: () -> Unit,
    onSave: (AffiliateProduct) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(product?.productNameAr ?: "") }
    var partnerName by remember { mutableStateOf(product?.partnerName ?: "صيدلية النهدي") }
    var category by remember { mutableStateOf(product?.categoryAr ?: "ترطيب الشفاه") }
    var priceText by remember { mutableStateOf(product?.priceSAR?.toString() ?: "30.0") }
    var description by remember { mutableStateOf(product?.descriptionAr ?: "") }
    var url by remember { mutableStateOf(product?.url ?: "https://example.com") }
    var imageUri by remember { mutableStateOf(product?.imageUri ?: "") }
    var manufacturerUrl by remember { mutableStateOf(product?.manufacturerUrl ?: "") }
    var pharmacyUrl by remember { mutableStateOf(product?.pharmacyUrl ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64Data = convertImageUriToBase64(context, uri)
            if (base64Data != null) {
                imageUri = base64Data
            } else {
                val copiedPath = copyUriToInternalStorage(context, uri, "product_images")
                imageUri = copiedPath ?: uri.toString()
            }
        }
    }

    val predefinedCategories = listOf(
        "ترطيب الشفاه",
        "واقي شمس",
        "ترطيب الوجه",
        "قطرات العين",
        "مكملات غذائية",
        "غسول لطيف",
        "عناية بالجسم"
    )

    val predefinedStores = listOf(
        "صيدلية النهدي",
        "صيدلية الدواء",
        "أمازون السعودية",
        "إيهيرب",
        "نون",
        "صيدلية محلية"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(12.dp)
                .imePadding()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product == null)
                            (if (appLanguage == "ar") "إضافة منتج جديد" else "Add New Product")
                        else
                            (if (appLanguage == "ar") "تعديل تفاصيل المنتج" else "Edit Product Details"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (appLanguage == "ar") "اسم المنتج *" else "Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (appLanguage == "ar") "التصنيف:" else "Category:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(predefinedCategories) { cat ->
                            val isSelected = category == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Store / Partner Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (appLanguage == "ar") "المتجر / الصيدلية:" else "Store / Pharmacy:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(predefinedStores) { store ->
                            val isSelected = partnerName == store
                            FilterChip(
                                selected = isSelected,
                                onClick = { partnerName = store },
                                label = { Text(store, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Price (SAR)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (appLanguage == "ar") "القيمة / السعر (ر.س) *" else "Price (SAR) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (appLanguage == "ar") "وصف المنتج وأهميته للعلاج" else "Description & Benefits") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Image Selection Block
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "صورة المنتج (تُحفظ في قاعدة البيانات للمزامنة مع كل الأجهزة):" else "Product Image (Saved to DB & Synced):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (imageUri.isNotBlank()) {
                                ProductImageThumbnail(
                                    imageUri = imageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Image", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "ar") "اختيار صورة من المعرض" else "Pick from Gallery", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = imageUri,
                            onValueChange = { imageUri = it },
                            placeholder = { Text(if (appLanguage == "ar") "أو أدخل رابط الصورة (URL)..." else "Or paste Image URL...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Web Links & URLs Group
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "عناوين وروابط المنتج (URLs):" else "Product Web Links & URLs:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Manufacturer URL
                        OutlinedTextField(
                            value = manufacturerUrl,
                            onValueChange = { manufacturerUrl = it },
                            label = { Text(if (appLanguage == "ar") "عنوان URL الخاص بالشركة المنتجة" else "Manufacturer URL") },
                            placeholder = { Text("https://brand.com/product") },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Pharmacy URL
                        OutlinedTextField(
                            value = pharmacyUrl,
                            onValueChange = { pharmacyUrl = it },
                            label = { Text(if (appLanguage == "ar") "عنوان URL الخاص بالصيدلية" else "Pharmacy URL") },
                            placeholder = { Text("https://nahdi.sa/product") },
                            leadingIcon = {
                                Icon(Icons.Default.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2E7D32))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Store / Purchase Link
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(if (appLanguage == "ar") "رابط الشراء أو المتجر العام (URL)" else "General Product / Store URL") },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action buttons
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
                            if (name.isBlank()) {
                                Toast.makeText(context, if (appLanguage == "ar") "يرجى كتابة اسم المنتج" else "Please enter product name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val parsedPrice = priceText.toDoubleOrNull() ?: 0.0

                            // Convert local image uri/file to base64 so it syncs across all devices in the database
                            val cleanImageUri = imageUri.trim()
                            val persistentImageUri = when {
                                cleanImageUri.startsWith("data:image") -> cleanImageUri
                                cleanImageUri.startsWith("http://") || cleanImageUri.startsWith("https://") -> cleanImageUri
                                cleanImageUri.startsWith("content://") -> convertImageUriToBase64(context, Uri.parse(cleanImageUri)) ?: cleanImageUri
                                cleanImageUri.startsWith("/") -> convertLocalFileToBase64(cleanImageUri) ?: cleanImageUri
                                else -> cleanImageUri
                            }

                            val finalItem = (product ?: AffiliateProduct(
                                id = "prod_${UUID.randomUUID().toString().take(8)}",
                                partnerName = partnerName,
                                productNameAr = name,
                                descriptionAr = description,
                                categoryAr = category,
                                priceSAR = parsedPrice,
                                url = url,
                                imageUri = persistentImageUri,
                                manufacturerUrl = manufacturerUrl.trim(),
                                pharmacyUrl = pharmacyUrl.trim()
                            )).copy(
                                productNameAr = name,
                                partnerName = partnerName,
                                categoryAr = category,
                                priceSAR = parsedPrice,
                                descriptionAr = description,
                                url = url,
                                imageUri = persistentImageUri,
                                manufacturerUrl = manufacturerUrl.trim(),
                                pharmacyUrl = pharmacyUrl.trim()
                            )
                            onSave(finalItem)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (appLanguage == "ar") "حفظ المنتج" else "Save Product", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun copyUriToInternalStorage(context: Context, uri: Uri, subFolder: String): String? {
    return try {
        val targetDir = File(context.filesDir, subFolder)
        if (!targetDir.exists()) targetDir.mkdirs()

        val extension = context.contentResolver.getType(uri)?.let { type ->
            if (type.contains("pdf")) ".pdf" else if (type.contains("png")) ".png" else ".jpg"
        } ?: ".bin"

        val outFile = File(targetDir, "file_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        outFile.absolutePath
    } catch (_: Exception) {
        null
    }
}

fun convertImageUriToBase64(context: Context, uri: Uri, maxDim: Int = 512): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = if (width > maxDim || height > maxDim) {
            maxDim.toFloat() / maxOf(width, height)
        } else {
            1f
        }
        val scaledBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (_: Exception) {
        null
    }
}

fun convertLocalFileToBase64(filePath: String, maxDim: Int = 512): String? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null
        val originalBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = if (width > maxDim || height > maxDim) {
            maxDim.toFloat() / maxOf(width, height)
        } else {
            1f
        }
        val scaledBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            originalBitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (_: Exception) {
        null
    }
}

