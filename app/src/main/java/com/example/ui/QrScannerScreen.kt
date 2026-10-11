package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AssetEntity
import com.example.model.SopScheduleParser
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.util.QrCodeUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    allAssets: List<AssetEntity>,
    recorderName: String,
    isDesktopMode: Boolean = false,
    onBack: () -> Unit,
    onOpenAssetSheet: (String) -> Unit,
    onQuickUpdateStatus: (assetId: String, newStatus: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val akah = LocalAkahColors.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var scannedAssetId by remember { mutableStateOf<String?>(null) }
    var rawScanError by remember { mutableStateOf<String?>(null) }
    var manualQuery by remember { mutableStateOf("") }
    var quickNote by remember { mutableStateOf("") }
    var statusUpdateFeedback by remember { mutableStateOf<String?>(null) }

    val scannedAsset = remember(scannedAssetId, allAssets) {
        allAssets.find { it.id.equals(scannedAssetId, ignoreCase = true) }
    }

    fun handleRawDecodedText(raw: String) {
        val extracted = SopScheduleParser.extractAssetIdFromQr(raw)
        if (extracted != null) {
            scannedAssetId = extracted
            rawScanError = null
            statusUpdateFeedback = null
        } else {
            rawScanError = "Scanned code \"$raw\" does not contain a valid AKAH Asset ID (AKA-HYD-XXX-000)."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val results = QrCodeUtils.decodeQrFromUri(context, uri)
                if (results.isNotEmpty()) {
                    handleRawDecodedText(results.first())
                } else {
                    rawScanError = "No QR code detected in the selected image."
                }
            }
        }
    }

    val matchingAssets = remember(allAssets, manualQuery) {
        val q = manualQuery.trim().lowercase()
        if (q.isEmpty()) allAssets else allAssets.filter {
            "${it.id} ${it.name} ${it.system} ${it.location}".lowercase().contains(q)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_scanner_screen")
    ) {
        val useTwoColumnDesktop = isDesktopMode || maxWidth >= 680.dp

        if (useTwoColumnDesktop) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Camera Viewfinder + Scanned Asset Result Card
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScannerTopActionRow(
                        onBack = onBack,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    if (scannedAsset != null) {
                        ScannedAssetActionCard(
                            scannedAsset = scannedAsset,
                            recorderName = recorderName,
                            quickNote = quickNote,
                            onQuickNoteChange = { quickNote = it },
                            statusUpdateFeedback = statusUpdateFeedback,
                            onQuickUpdateStatus = { st ->
                                onQuickUpdateStatus(scannedAsset.id, st, quickNote)
                                statusUpdateFeedback =
                                    "Updated ${scannedAsset.id} to \"$st\" by ${recorderName.ifBlank { "Duty Operator" }}"
                                quickNote = ""
                            },
                            onOpenAssetSheet = { onOpenAssetSheet(scannedAsset.id) },
                            onScanAnother = { scannedAssetId = null }
                        )
                    }

                    if (!rawScanError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = akah.critSoft,
                            border = BorderStroke(1.dp, akah.crit),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rawScanError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.crit,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    CameraViewportCard(
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onQrDetected = { raw -> handleRawDecodedText(raw) }
                    )
                }

                VerticalDivider(color = akah.line, thickness = 1.dp)

                // Right Pane: Campus QR Label Simulator & Quick Search List
                LazyColumn(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "quick_qr_simulator_header") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TAP ANY CAMPUS QR LABEL TO SCAN / UPDATE STATUS",
                                style = MaterialTheme.typography.titleSmall,
                                color = akah.muted
                            )
                            OutlinedTextField(
                                value = manualQuery,
                                onValueChange = { manualQuery = it },
                                placeholder = { Text("Filter QR labels by ID, name or location…") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scanner_filter_input")
                            )
                        }
                    }

                    items(
                        items = matchingAssets,
                        key = { "scan_pick_${it.id}" }
                    ) { asset ->
                        CampusQrSimulatorRow(
                            asset = asset,
                            isSelected = scannedAssetId == asset.id,
                            onClick = {
                                handleRawDecodedText("https://script.google.com/macros/s/AKAH/exec?asset=${asset.id}")
                            }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "scanner_header") {
                    ScannerTopActionRow(
                        onBack = onBack,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                if (scannedAsset != null) {
                    item(key = "scanned_asset_result") {
                        ScannedAssetActionCard(
                            scannedAsset = scannedAsset,
                            recorderName = recorderName,
                            quickNote = quickNote,
                            onQuickNoteChange = { quickNote = it },
                            statusUpdateFeedback = statusUpdateFeedback,
                            onQuickUpdateStatus = { st ->
                                onQuickUpdateStatus(scannedAsset.id, st, quickNote)
                                statusUpdateFeedback =
                                    "Updated ${scannedAsset.id} to \"$st\" by ${recorderName.ifBlank { "Duty Operator" }}"
                                quickNote = ""
                            },
                            onOpenAssetSheet = { onOpenAssetSheet(scannedAsset.id) },
                            onScanAnother = { scannedAssetId = null }
                        )
                    }
                }

                if (!rawScanError.isNullOrBlank()) {
                    item(key = "scan_error") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = akah.critSoft,
                            border = BorderStroke(1.dp, akah.crit),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rawScanError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.crit,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item(key = "camera_viewport") {
                    CameraViewportCard(
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onQrDetected = { raw -> handleRawDecodedText(raw) }
                    )
                }

                item(key = "quick_qr_simulator_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TAP ANY CAMPUS QR LABEL TO SCAN / UPDATE STATUS",
                            style = MaterialTheme.typography.titleSmall,
                            color = akah.muted
                        )
                        OutlinedTextField(
                            value = manualQuery,
                            onValueChange = { manualQuery = it },
                            placeholder = { Text("Filter QR labels by ID, name or location…") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scanner_filter_input")
                        )
                    }
                }

                items(
                    items = matchingAssets,
                    key = { "scan_pick_${it.id}" }
                ) { asset ->
                    CampusQrSimulatorRow(
                        asset = asset,
                        isSelected = scannedAssetId == asset.id,
                        onClick = {
                            handleRawDecodedText("https://script.google.com/macros/s/AKAH/exec?asset=${asset.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerTopActionRow(
    onBack: () -> Unit,
    onPickPhoto: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "All assets",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("All assets", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onPickPhoto,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("scan_from_photo_button")
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Scan QR from image",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scan from Photo", fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScannedAssetActionCard(
    scannedAsset: AssetEntity,
    recorderName: String,
    quickNote: String,
    onQuickNoteChange: (String) -> Unit,
    statusUpdateFeedback: String?,
    onQuickUpdateStatus: (String) -> Unit,
    onOpenAssetSheet: () -> Unit,
    onScanAnother: () -> Unit
) {
    val akah = LocalAkahColors.current
    val critColor = CriticalityColor(scannedAsset.criticality)
    val isOverdue = SopScheduleParser.isLate(scannedAsset)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_asset_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "QR SCANNED · ${scannedAsset.id}",
                        fontFamily = IbmPlexMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusPill(status = scannedAsset.status)
            }

            Text(
                text = scannedAsset.name,
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Text(
                text = "${scannedAsset.criticality.uppercase()} · ${scannedAsset.system}${if (scannedAsset.location.isNotBlank()) " · ${scannedAsset.location}" else ""}",
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = critColor
            )

            if (isOverdue) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = akah.warnSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Routine SOP check is currently due for this asset.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = akah.warn,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = akah.line)

            Text(
                text = "INDIVIDUAL STATUS UPDATE (1-TAP)",
                style = MaterialTheme.typography.labelSmall,
                color = akah.muted
            )

            OutlinedTextField(
                value = quickNote,
                onValueChange = onQuickNoteChange,
                label = { Text("Optional inspection note / reading") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_quick_note_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickButtons = listOf(
                    Triple("In service", akah.ok, Icons.Default.CheckCircle),
                    Triple("Needs attention", akah.warn, Icons.Default.Warning),
                    Triple("Out of service", akah.crit, Icons.Default.Error)
                )
                quickButtons.forEach { (st, color, icon) ->
                    val isCurrent = scannedAsset.status == st
                    OutlinedButton(
                        onClick = { onQuickUpdateStatus(st) },
                        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, color),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isCurrent) color.copy(alpha = 0.14f) else Color.Transparent,
                            contentColor = color
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_set_status_$st")
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = st,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = st,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!statusUpdateFeedback.isNullOrBlank()) {
                Text(
                    text = statusUpdateFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = akah.ok
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenAssetSheet,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_scanned_asset_sheet_button")
                ) {
                    Text("Open Full SOP Checklist & Sheet")
                }
                TextButton(onClick = onScanAnother) {
                    Text("Scan Another")
                }
            }
        }
    }
}

@Composable
private fun CameraViewportCard(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrDetected: (String) -> Unit
) {
    val akah = LocalAkahColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, akah.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "LIVE CAMERA QR SCANNER",
                style = MaterialTheme.typography.titleSmall,
                color = akah.muted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (hasCameraPermission) {
                CameraQrPreviewBox(
                    onQrDetected = onQrDetected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(akah.sunken),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Permission",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Point your camera at any AKAH Asset QR label to open its SOP checklist or update status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted
                        )
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.testTag("grant_camera_permission_button")
                        ) {
                            Text("Enable Camera Scanner")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampusQrSimulatorRow(
    asset: AssetEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val akah = LocalAkahColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else akah.line
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulate_scan_${asset.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan ${asset.id}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = asset.id,
                        fontFamily = IbmPlexMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${asset.system} · ${asset.location.ifBlank { asset.criticality }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = akah.muted
                    )
                }
            }
            StatusPill(status = asset.status)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreviewBox(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControlRef by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val barcodeScanner = BarcodeScanning.getClient(options)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val raw = barcode.rawValue
                                            if (!raw.isNullOrBlank()) {
                                                onQrDetected(raw)
                                                break
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControlRef = camera.cameraControl
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Targeting Reticle Overlay
        val accentColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = size.minDimension * 0.62f
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2f
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 6f)
            )
        }

        // Torch button
        IconButton(
            onClick = {
                torchEnabled = !torchEnabled
                cameraControlRef?.enableTorch(torchEnabled)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
        ) {
            Icon(
                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle Flashlight",
                tint = Color.White
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Text(
                text = "Align AKA-HYD-* QR Label inside frame",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}
