package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetEntity
import com.example.model.SopConstants
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.util.QrCodeUtils
import java.util.Locale
import kotlin.math.ceil

@Composable
fun QrLabelsScreen(
    allAssets: List<AssetEntity>,
    qrBaseAddress: String,
    lanServerUrl: String,
    isSyncingLan: Boolean,
    lanSyncMessage: String?,
    isExportingPdf: Boolean = false,
    lastExportMessage: String? = null,
    suggestAssetId: (String) -> String = { sys ->
        val code = SopConstants.SYSTEM_CODES[sys] ?: "GEN"
        "AKA-HYD-$code-001"
    },
    buildAssetQrUrl: (String) -> String,
    onSaveQrAddress: (String) -> Unit,
    onSyncLanServer: (String) -> Unit,
    onCreateNewAssetWithQr: (
        id: String,
        name: String,
        system: String,
        location: String,
        criticality: String,
        status: String,
        owner: String,
        notes: String,
        make: String,
        model: String,
        capacity: String,
        exportPdfImmediately: Boolean,
        onError: (String) -> Unit,
        onCreated: (AssetEntity) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onExportQrLabelsPdf: (assets: List<AssetEntity>, fileName: String) -> Unit = { _, _ -> },
    onOpenAsset: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val akah = LocalAkahColors.current
    var addressInput by remember(qrBaseAddress) { mutableStateOf(qrBaseAddress) }
    var lanUrlInput by remember(lanServerUrl) { mutableStateOf(lanServerUrl) }
    var selectedSystem by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlySelected by remember { mutableStateOf(false) }
    var selectedAssetIds by remember { mutableStateOf(setOf<String>()) }

    // New Asset QR Generator Studio State
    var showNewAssetStudio by remember { mutableStateOf(true) }
    var showServerConfig by remember { mutableStateOf(false) }
    var newSystem by remember { mutableStateOf("Electrical") }
    var newAssetId by remember(allAssets.size) { mutableStateOf(suggestAssetId(newSystem)) }
    var newName by remember { mutableStateOf("") }
    var newLocation by remember { mutableStateOf("") }
    var newCriticality by remember { mutableStateOf("Critical") }
    var newStatus by remember { mutableStateOf("In service") }
    var newOwner by remember { mutableStateOf("Facilities / MEP") }
    var newMake by remember { mutableStateOf("") }
    var newModel by remember { mutableStateOf("") }
    var newCapacity by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var newAssetError by remember { mutableStateOf("") }
    var recentlyCreatedAsset by remember { mutableStateOf<AssetEntity?>(null) }
    var previewModalAsset by remember { mutableStateOf<AssetEntity?>(null) }

    val shownAssets = remember(allAssets, selectedSystem, searchQuery, showOnlySelected, selectedAssetIds) {
        val q = searchQuery.trim().lowercase(Locale.US)
        allAssets
            .sortedBy { it.id }
            .filter { asset ->
                val sysMatch = selectedSystem == "All" || asset.system == selectedSystem
                val selMatch = !showOnlySelected || asset.id in selectedAssetIds
                val queryMatch = q.isEmpty() ||
                    asset.id.lowercase(Locale.US).contains(q) ||
                    asset.name.lowercase(Locale.US).contains(q) ||
                    asset.location.lowercase(Locale.US).contains(q) ||
                    asset.system.lowercase(Locale.US).contains(q)
                sysMatch && selMatch && queryMatch
            }
    }

    val pdfTargetAssets = remember(selectedAssetIds, shownAssets, allAssets) {
        if (selectedAssetIds.isNotEmpty()) {
            allAssets.filter { it.id in selectedAssetIds }.sortedBy { it.id }
        } else {
            shownAssets
        }
    }

    val previewDraftAsset = remember(
        newAssetId,
        newName,
        newSystem,
        newLocation,
        newCriticality,
        newStatus,
        newOwner,
        newMake,
        newModel,
        newCapacity,
        newNotes
    ) {
        AssetEntity(
            id = newAssetId.trim().ifBlank { suggestAssetId(newSystem) }.uppercase(Locale.US),
            sr = allAssets.size + 1,
            cat = SopConstants.SYSTEM_CODES[newSystem] ?: "GEN",
            name = newName.trim().ifBlank { "New $newSystem Asset Preview" },
            system = newSystem,
            category = newSystem,
            criticality = newCriticality,
            status = newStatus,
            location = newLocation.trim().ifBlank { "Campus Location" },
            make = newMake.trim(),
            model = newModel.trim(),
            serial = "",
            capacity = newCapacity.trim(),
            installed = "",
            amcVendor = "",
            amcExpiry = "",
            owner = newOwner.trim(),
            strategy = "Preventive + Condition Based",
            routine = "D: Visual & status inspection",
            ppm = "Q: Preventive service",
            notes = newNotes.trim(),
            lastCheckJson = "",
            rev = 1,
            pendingOfflineSync = true
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 175.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_labels_grid"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Navigation & Printable QR PDF Action Bar
        item(span = { GridItemSpan(maxLineSpan) }, key = "top_qr_pdf_bar") {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
                tonalElevation = 2.dp,
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
                        TextButton(
                            onClick = onBack,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("qr_screen_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Register",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Asset Register", fontWeight = FontWeight.SemiBold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showServerConfig = !showServerConfig },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("toggle_qr_server_config_button")
                            ) {
                                Text(
                                    text = if (showServerConfig) "Hide URL Setup" else "QR URL & Sync",
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { showNewAssetStudio = !showNewAssetStudio },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("toggle_new_asset_qr_studio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "New Asset QR Generator",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showNewAssetStudio) "Hide New Asset QR" else "+ New Asset QR",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = akah.line)

                    // Primary PDF Export Header for All Available & Selected Assets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ASSET QR CODE GENERATOR & PRINTABLE PDF SHEETS",
                                    fontFamily = BarlowDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.6.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            val totalPages = ceil(allAssets.size / 12.0).toInt().coerceAtLeast(1)
                            Text(
                                text = "${allAssets.size} available assets ready · A4 Portrait PDF (12 physical labels per page · $totalPages page${if (totalPages == 1) "" else "s"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.muted
                            )
                        }
                    }

                    // Prominent Export to PDF Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onExportQrLabelsPdf(
                                    allAssets,
                                    "AKAH_All_Asset_QR_Labels_${allAssets.size}.pdf"
                                )
                            },
                            enabled = !isExportingPdf && allAssets.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("export_all_qr_pdf_button")
                        ) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Download All QR Codes in PDF",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get All Available QRs in PDF (${allAssets.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (selectedAssetIds.isNotEmpty() || shownAssets.size != allAssets.size) {
                            OutlinedButton(
                                onClick = {
                                    val labelScope = if (selectedAssetIds.isNotEmpty()) {
                                        "Selected_${selectedAssetIds.size}"
                                    } else {
                                        "${selectedSystem}_${shownAssets.size}"
                                    }
                                    onExportQrLabelsPdf(
                                        pdfTargetAssets,
                                        "AKAH_QR_Labels_$labelScope.pdf"
                                    )
                                },
                                enabled = !isExportingPdf && pdfTargetAssets.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("export_filtered_qr_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export Selected/Filtered QRs as PDF",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedAssetIds.isNotEmpty()) {
                                        "Export Selected QRs PDF (${selectedAssetIds.size})"
                                    } else {
                                        "Export Filtered QRs PDF (${shownAssets.size})"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (!lastExportMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = akah.okSoft,
                            border = BorderStroke(1.dp, akah.ok),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_pdf_export_status_banner")
                        ) {
                            Text(
                                text = lastExportMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = akah.ok,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Create New Asset & Generate Physical QR Code Label Studio
        if (showNewAssetStudio) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "new_asset_qr_studio") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_asset_qr_generator_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GENERATE QR CODE FOR NEW ASSET & ADD TO TRACKING SYSTEM",
                                    fontFamily = BarlowDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.6.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Enter new asset details below to preview its physical QR label in real time, add it to the campus register, and export its printable PDF label.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = akah.muted
                                )
                            }
                            IconButton(onClick = { showNewAssetStudio = false }) {
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "Collapse New Asset QR Studio"
                                )
                            }
                        }

                        // MEP System Chips (Auto-suggests next Asset ID)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "1. Select MEP System (auto-assigns SOP Asset ID):",
                                style = MaterialTheme.typography.labelSmall,
                                color = akah.muted
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SopConstants.SYSTEMS.forEach { sys ->
                                    val selected = newSystem == sys
                                    Surface(
                                        onClick = {
                                            newSystem = sys
                                            newAssetId = suggestAssetId(sys)
                                        },
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else akah.line
                                        ),
                                        modifier = Modifier.testTag("new_qr_sys_chip_$sys")
                                    ) {
                                        Text(
                                            text = sys,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Responsive Form + Live Physical Label Preview
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 560.dp
                            if (isWide) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        NewAssetQrFormFields(
                                            assetId = newAssetId,
                                            onAssetIdChange = { newAssetId = it.uppercase(Locale.US) },
                                            name = newName,
                                            onNameChange = { newName = it },
                                            location = newLocation,
                                            onLocationChange = { newLocation = it },
                                            criticality = newCriticality,
                                            onCriticalityChange = { newCriticality = it },
                                            status = newStatus,
                                            onStatusChange = { newStatus = it },
                                            make = newMake,
                                            onMakeChange = { newMake = it },
                                            model = newModel,
                                            onModelChange = { newModel = it },
                                            capacity = newCapacity,
                                            onCapacityChange = { newCapacity = it }
                                        )
                                    }

                                    // Live Physical QR Label Card Preview on Right
                                    Column(
                                        modifier = Modifier.width(210.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "LIVE PHYSICAL LABEL PREVIEW",
                                            fontFamily = IbmPlexMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        PrintableQrLabelCard(
                                            asset = previewDraftAsset,
                                            qrUrl = buildAssetQrUrl(previewDraftAsset.id),
                                            isSelected = false,
                                            showSelectionBadge = false,
                                            onToggleSelect = {},
                                            onExportSinglePdf = {},
                                            onEnlarge = { previewModalAsset = previewDraftAsset },
                                            onClick = { previewModalAsset = previewDraftAsset }
                                        )
                                        Text(
                                            text = "Encodes: ${buildAssetQrUrl(previewDraftAsset.id)}",
                                            fontFamily = IbmPlexMonoFontFamily,
                                            fontSize = 9.sp,
                                            color = akah.muted,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    NewAssetQrFormFields(
                                        assetId = newAssetId,
                                        onAssetIdChange = { newAssetId = it.uppercase(Locale.US) },
                                        name = newName,
                                        onNameChange = { newName = it },
                                        location = newLocation,
                                        onLocationChange = { newLocation = it },
                                        criticality = newCriticality,
                                        onCriticalityChange = { newCriticality = it },
                                        status = newStatus,
                                        onStatusChange = { newStatus = it },
                                        make = newMake,
                                        onMakeChange = { newMake = it },
                                        model = newModel,
                                        onModelChange = { newModel = it },
                                        capacity = newCapacity,
                                        onCapacityChange = { newCapacity = it }
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "LIVE PHYSICAL QR LABEL PREVIEW",
                                            fontFamily = IbmPlexMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(modifier = Modifier.width(210.dp)) {
                                            PrintableQrLabelCard(
                                                asset = previewDraftAsset,
                                                qrUrl = buildAssetQrUrl(previewDraftAsset.id),
                                                isSelected = false,
                                                showSelectionBadge = false,
                                                onToggleSelect = {},
                                                onExportSinglePdf = {},
                                                onEnlarge = { previewModalAsset = previewDraftAsset },
                                                onClick = { previewModalAsset = previewDraftAsset }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (newAssetError.isNotEmpty()) {
                            Text(
                                text = newAssetError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Action Buttons to Save Asset & Generate / Export QR Label PDF
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    newAssetError = ""
                                    onCreateNewAssetWithQr(
                                        newAssetId,
                                        newName,
                                        newSystem,
                                        newLocation,
                                        newCriticality,
                                        newStatus,
                                        newOwner,
                                        newNotes,
                                        newMake,
                                        newModel,
                                        newCapacity,
                                        false,
                                        { err -> newAssetError = err },
                                        { created ->
                                            recentlyCreatedAsset = created
                                            selectedAssetIds = selectedAssetIds + created.id
                                            newName = ""
                                            newLocation = ""
                                            newMake = ""
                                            newModel = ""
                                            newCapacity = ""
                                            newNotes = ""
                                            newAssetId = suggestAssetId(newSystem)
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("create_asset_and_qr_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Asset & Generate QR Label", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    newAssetError = ""
                                    onCreateNewAssetWithQr(
                                        newAssetId,
                                        newName,
                                        newSystem,
                                        newLocation,
                                        newCriticality,
                                        newStatus,
                                        newOwner,
                                        newNotes,
                                        newMake,
                                        newModel,
                                        newCapacity,
                                        true,
                                        { err -> newAssetError = err },
                                        { created ->
                                            recentlyCreatedAsset = created
                                            selectedAssetIds = selectedAssetIds + created.id
                                            newName = ""
                                            newLocation = ""
                                            newMake = ""
                                            newModel = ""
                                            newCapacity = ""
                                            newNotes = ""
                                            newAssetId = suggestAssetId(newSystem)
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("create_asset_and_export_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Asset & Get QR PDF", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Newly Created Asset Callout Banner with Immediate QR PDF & Inspection
                        AnimatedVisibility(visible = recentlyCreatedAsset != null) {
                            recentlyCreatedAsset?.let { created ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = akah.okSoft,
                                    border = BorderStroke(1.5.dp, akah.ok),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("newly_created_qr_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = akah.ok,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "New Asset Added & QR Label Generated: ${created.id}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${created.name} · ${created.location} · Added to tracking system & selected for PDF printing",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = akah.muted
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { previewModalAsset = created },
                                                shape = CircleShape,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text("View Label", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    onExportQrLabelsPdf(
                                                        listOf(created),
                                                        "AKAH_QR_Label_${created.id}.pdf"
                                                    )
                                                },
                                                shape = CircleShape,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                                modifier = Modifier.testTag("export_new_asset_qr_pdf_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Get PDF", fontSize = 11.sp)
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

        // Collapsible QR Base URL & Campus Server Sync Card
        if (showServerConfig) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "qr_config_card") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, akah.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "QR BASE URL & CAMPUS SERVER SYNC",
                            style = MaterialTheme.typography.titleSmall,
                            color = akah.muted
                        )
                        Text(
                            text = "Each label encodes its asset URL (${SopConstants.APP_NAME}). Scanning any label in the app or with a phone opens its asset sheet directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted
                        )

                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("Address printed in the QR codes") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qr_address_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onSaveQrAddress(addressInput) },
                                modifier = Modifier.testTag("use_qr_address_button")
                            ) {
                                Text("Use this address")
                            }
                            Text(
                                text = "A4 · 12 labels per page (63mm × 68mm) · ${allAssets.size} assets",
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.muted
                            )
                        }

                        OutlinedTextField(
                            value = lanUrlInput,
                            onValueChange = { lanUrlInput = it },
                            label = { Text("Optional AKAH_SOP_Server PC address (e.g. http://192.168.1.50:8090)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lan_server_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSyncLanServer(lanUrlInput) },
                                enabled = !isSyncingLan,
                                modifier = Modifier.testTag("sync_lan_button")
                            ) {
                                if (isSyncingLan) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync with Campus PC")
                            }
                            if (!lanSyncMessage.isNullOrBlank()) {
                                Text(
                                    text = lanSyncMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = akah.muted,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Available Assets Search, System Filter & Batch Selection Toolbar
        item(span = { GridItemSpan(maxLineSpan) }, key = "available_assets_filter_bar") {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
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
                            text = "AVAILABLE ASSET QR LABELS (${shownAssets.size} OF ${allAssets.size})",
                            fontFamily = BarlowDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.7.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    selectedAssetIds = selectedAssetIds + shownAssets.map { it.id }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("select_all_qr_labels_button")
                            ) {
                                Text("Select Shown (${shownAssets.size})", fontSize = 11.sp)
                            }
                            if (selectedAssetIds.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        selectedAssetIds = emptySet()
                                        showOnlySelected = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("clear_selected_qr_labels_button")
                                ) {
                                    Text("Clear (${selectedAssetIds.size})", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Search Available Assets for QR Generation
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter QR labels by asset ID, name, or location…", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search QR labels",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_labels_search_input")
                    )

                    // System filter chips for labels
                    val systems = remember { listOf("All") + SopConstants.SYSTEMS }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedAssetIds.isNotEmpty()) {
                            Surface(
                                onClick = { showOnlySelected = !showOnlySelected },
                                shape = CircleShape,
                                color = if (showOnlySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "Selected (${selectedAssetIds.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (showOnlySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }

                        systems.forEach { sys ->
                            val isSelected = selectedSystem == sys
                            Surface(
                                onClick = { selectedSystem = sys },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                                ),
                                modifier = Modifier.testTag("qr_filter_system_$sys")
                            ) {
                                Text(
                                    text = sys,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else akah.muted,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Printable-Style Asset QR Label Cards for All Available Assets
        items(
            items = shownAssets,
            key = { "qr_label_${it.id}" }
        ) { asset ->
            val isSelected = asset.id in selectedAssetIds
            PrintableQrLabelCard(
                asset = asset,
                qrUrl = buildAssetQrUrl(asset.id),
                isSelected = isSelected,
                showSelectionBadge = true,
                onToggleSelect = {
                    selectedAssetIds = if (isSelected) {
                        selectedAssetIds - asset.id
                    } else {
                        selectedAssetIds + asset.id
                    }
                },
                onExportSinglePdf = {
                    onExportQrLabelsPdf(
                        listOf(asset),
                        "AKAH_QR_Label_${asset.id}.pdf"
                    )
                },
                onEnlarge = { previewModalAsset = asset },
                onClick = { onOpenAsset(asset.id) }
            )
        }
    }

    // Full-Size Physical QR Label Print & Scan Modal
    previewModalAsset?.let { modalAsset ->
        val modalQrUrl = buildAssetQrUrl(modalAsset.id)
        var modalBmp by remember(modalQrUrl) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(modalQrUrl) {
            modalBmp = QrCodeUtils.generateQrImageBitmap(modalQrUrl, 512)
        }

        AlertDialog(
            onDismissRequest = { previewModalAsset = null },
            title = {
                Column {
                    Text(
                        text = "Physical Asset QR Label · ${modalAsset.id}",
                        fontFamily = BarlowDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "${modalAsset.name} · ${modalAsset.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = akah.muted
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFF00804A)),
                        modifier = Modifier.width(240.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF00804A))
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AGA KHAN ACADEMY · HYDERABAD",
                                    fontFamily = BarlowDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                modalBmp?.let { bmp ->
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = "Large QR Code ${modalAsset.id}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Text(
                                text = modalAsset.id,
                                fontFamily = IbmPlexMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF15201A)
                            )
                            Text(
                                text = modalAsset.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF15201A),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${modalAsset.location} · ${modalAsset.system} (${modalAsset.criticality})",
                                fontSize = 10.sp,
                                color = Color(0xFF56645C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = modalQrUrl,
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val id = modalAsset.id
                            previewModalAsset = null
                            onOpenAsset(id)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Sheet")
                    }
                    Button(
                        onClick = {
                            onExportQrLabelsPdf(
                                listOf(modalAsset),
                                "AKAH_QR_Label_${modalAsset.id}.pdf"
                            )
                            previewModalAsset = null
                        },
                        modifier = Modifier.testTag("modal_export_single_qr_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download QR PDF")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { previewModalAsset = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun NewAssetQrFormFields(
    assetId: String,
    onAssetIdChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    criticality: String,
    onCriticalityChange: (String) -> Unit,
    status: String,
    onStatusChange: (String) -> Unit,
    make: String,
    onMakeChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    capacity: String,
    onCapacityChange: (String) -> Unit
) {
    val akah = LocalAkahColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = assetId,
            onValueChange = onAssetIdChange,
            label = { Text("Asset ID (QR Code Key)") },
            singleLine = true,
            modifier = Modifier
                .weight(0.95f)
                .testTag("new_qr_asset_id_input")
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("New Asset Name *") },
            placeholder = { Text("e.g. Chiller Pump #3") },
            singleLine = true,
            modifier = Modifier
                .weight(1.35f)
                .testTag("new_qr_asset_name_input")
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Physical Location *") },
            placeholder = { Text("e.g. Utility Plant Room") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("new_qr_asset_location_input")
        )
        OutlinedTextField(
            value = capacity,
            onValueChange = onCapacityChange,
            label = { Text("Capacity / Rating") },
            placeholder = { Text("e.g. 15 kW / 250 KVA") },
            singleLine = true,
            modifier = Modifier
                .weight(0.9f)
                .testTag("new_qr_asset_capacity_input")
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = make,
            onValueChange = onMakeChange,
            label = { Text("Make / Manufacturer") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }

    // Criticality & Initial Status selectors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Criticality:",
            style = MaterialTheme.typography.labelSmall,
            color = akah.muted
        )
        SopConstants.CRITICALITIES.forEach { crit ->
            val selected = criticality == crit
            Surface(
                onClick = { onCriticalityChange(crit) },
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else akah.line
                )
            ) {
                Text(
                    text = crit,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else akah.muted,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Status:",
            style = MaterialTheme.typography.labelSmall,
            color = akah.muted
        )
        SopConstants.STATUSES.forEach { st ->
            val selected = status == st
            Surface(
                onClick = { onStatusChange(st) },
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else akah.line
                )
            ) {
                Text(
                    text = st,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else akah.muted,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PrintableQrLabelCard(
    asset: AssetEntity,
    qrUrl: String,
    isSelected: Boolean,
    showSelectionBadge: Boolean,
    onToggleSelect: () -> Unit,
    onExportSinglePdf: () -> Unit,
    onEnlarge: () -> Unit,
    onClick: () -> Unit
) {
    var bmp by remember(qrUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(qrUrl) {
        bmp = QrCodeUtils.generateQrImageBitmap(qrUrl, 320)
    }

    val critColor = when (asset.criticality) {
        "Critical" -> Color(0xFFB3261E)
        "Medium" -> Color(0xFF9A6200)
        else -> Color(0xFF1E7B4F)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00804A) else Color(0xFFB9C6BF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qr_label_card_${asset.id}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Green top band: AGA KHAN ACADEMY · HYDERABAD + optional batch selection checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF00804A), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AGA KHAN ACADEMY · HYD",
                    fontFamily = BarlowDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.3.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showSelectionBadge) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (isSelected) "Deselect QR Label" else "Select QR Label for PDF",
                        tint = Color.White,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onToggleSelect() }
                            .testTag("select_qr_${asset.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // QR Code
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .background(Color.White)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                bmp?.let { imageBitmap ->
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "QR Label ${asset.id}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Monospace Asset ID
            Text(
                text = asset.id,
                fontFamily = IbmPlexMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = Color(0xFF15201A),
                textAlign = TextAlign.Center
            )

            // Asset Name (2 lines max)
            Text(
                text = asset.name,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                lineHeight = 12.5.sp,
                color = Color(0xFF15201A),
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )

            // Asset Location
            Text(
                text = asset.location.ifBlank { "Campus" },
                fontSize = 9.sp,
                color = Color(0xFF56645C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom System + Criticality
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = asset.system,
                    fontSize = 9.sp,
                    color = Color(0xFF56645C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = asset.criticality.uppercase(Locale.US),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = critColor
                )
            }

            if (showSelectionBadge) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onEnlarge,
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFFF1F5F3),
                        border = BorderStroke(1.dp, Color(0xFFD0DDD6)),
                        modifier = Modifier.testTag("enlarge_qr_${asset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Preview QR label",
                                tint = Color(0xFF15201A),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Label",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15201A)
                            )
                        }
                    }

                    Surface(
                        onClick = onExportSinglePdf,
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFFE1F3EA),
                        border = BorderStroke(1.dp, Color(0xFF00804A)),
                        modifier = Modifier.testTag("single_qr_pdf_${asset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export ${asset.id} QR PDF",
                                tint = Color(0xFF00804A),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "QR PDF",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00804A)
                            )
                        }
                    }
                }
            }
        }
    }
}
