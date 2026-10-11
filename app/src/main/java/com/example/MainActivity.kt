package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AddAssetOfflineDialog
import com.example.ui.AkahMasthead
import com.example.ui.AssetAuditLogDialog
import com.example.ui.AssetDetailScreen
import com.example.ui.BulkCsvImportDialog
import com.example.ui.DashboardScreen
import com.example.ui.DesktopEmptyDetailPane
import com.example.ui.ExportReportDialog
import com.example.ui.LayoutViewportMode
import com.example.ui.QrLabelsScreen
import com.example.ui.QrScannerScreen
import com.example.ui.QuickStatusUpdateDialog
import com.example.ui.SetMaintenanceDueDateDialog
import com.example.ui.WhoIsRecordingDialog
import com.example.ui.theme.LocalAkahColors
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SopNotificationHelper
import com.example.viewmodel.SopViewModel

enum class MainTab {
    REGISTER,
    SCANNER,
    LABELS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialOpenAssetId = intent?.getStringExtra(SopNotificationHelper.EXTRA_OPEN_ASSET_ID)
        setContent {
            MyApplicationTheme {
                AkahSopRegisterApp(initialOpenAssetId = initialOpenAssetId)
            }
        }
    }
}

@Composable
fun AkahSopRegisterApp(
    initialOpenAssetId: String? = null,
    sopViewModel: SopViewModel = viewModel()
) {
    val context = LocalContext.current
    val akah = LocalAkahColors.current
    val uiState by sopViewModel.uiState.collectAsStateWithLifecycle()
    val allAssets by sopViewModel.allAssets.collectAsStateWithLifecycle()
    val filteredAssets by sopViewModel.filteredAssets.collectAsStateWithLifecycle()
    val allLogs by sopViewModel.allLogs.collectAsStateWithLifecycle()
    val pendingOfflineSyncCount by sopViewModel.pendingOfflineSyncCount.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.REGISTER) }
    var layoutMode by rememberSaveable { mutableStateOf(LayoutViewportMode.AUTO) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        sopViewModel.notifyApproachingDeadlines(context)
    }

    fun ensureNotificationPermissionAndRun(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        action()
    }

    LaunchedEffect(initialOpenAssetId) {
        if (!initialOpenAssetId.isNullOrBlank()) {
            sopViewModel.openAssetDetail(initialOpenAssetId)
            currentTab = MainTab.REGISTER
        }
    }

    val activeAsset = remember(uiState.selectedAssetId, allAssets) {
        allAssets.find { it.id == uiState.selectedAssetId }
    }
    val activeAssetLogs = remember(uiState.selectedAssetId, allLogs) {
        if (uiState.selectedAssetId == null) emptyList()
        else allLogs.filter { it.assetId == uiState.selectedAssetId }
    }

    val quickStatusDialogAsset = remember(uiState.showQuickStatusDialogForAssetId, allAssets) {
        allAssets.find { it.id == uiState.showQuickStatusDialogForAssetId }
    }

    val dueDateDialogAsset = remember(uiState.showDueDateDialogForAssetId, allAssets) {
        allAssets.find { it.id == uiState.showDueDateDialogForAssetId }
    }

    val auditLogDialogAsset = remember(uiState.showAuditLogForAssetId, allAssets) {
        allAssets.find { it.id == uiState.showAuditLogForAssetId }
    }
    val auditLogDialogLogs = remember(uiState.showAuditLogForAssetId, allLogs) {
        if (uiState.showAuditLogForAssetId == null) emptyList()
        else allLogs.filter { it.assetId == uiState.showAuditLogForAssetId }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isEffectiveDesktop = when (layoutMode) {
            LayoutViewportMode.DESKTOP -> true
            LayoutViewportMode.MOBILE -> false
            LayoutViewportMode.AUTO -> maxWidth >= 600.dp
        }
        val isEffectiveTwoPane = when (layoutMode) {
            LayoutViewportMode.DESKTOP -> true
            LayoutViewportMode.MOBILE -> false
            LayoutViewportMode.AUTO -> maxWidth >= 700.dp
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AkahMasthead(
                    meName = uiState.meName,
                    layoutMode = layoutMode,
                    isEffectiveDesktop = isEffectiveDesktop,
                    onToggleLayoutMode = { layoutMode = layoutMode.next() },
                    onOpenWhoDialog = { sopViewModel.setShowWhoDialog(true) },
                    onImportCsv = { sopViewModel.setShowBulkCsvImportDialog(true) },
                    onExportCsv = { sopViewModel.setShowExportReportDialog(true) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
            },
            floatingActionButton = {
                if (!isEffectiveDesktop && currentTab == MainTab.REGISTER && activeAsset == null) {
                    ExtendedFloatingActionButton(
                        onClick = { sopViewModel.setShowAddAssetDialog(true) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Asset"
                            )
                        },
                        text = { Text("Add Asset") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_asset")
                    )
                }
            },
            bottomBar = {
                if (!isEffectiveDesktop) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("mobile_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.REGISTER,
                            onClick = { currentTab = MainTab.REGISTER },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "SOP Register"
                                )
                            },
                            label = { Text("Register") },
                            modifier = Modifier.testTag("nav_tab_register")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.SCANNER,
                            onClick = { currentTab = MainTab.SCANNER },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR Code"
                                )
                            },
                            label = { Text("Scan QR") },
                            modifier = Modifier.testTag("nav_tab_scanner")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.LABELS,
                            onClick = { currentTab = MainTab.LABELS },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "QR Labels"
                                )
                            },
                            label = { Text("QR Labels") },
                            modifier = Modifier.testTag("nav_tab_labels")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isEffectiveDesktop) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Spacer(modifier = Modifier.height(6.dp))
                            FloatingActionButton(
                                onClick = { sopViewModel.setShowAddAssetDialog(true) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("rail_add_asset_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Asset"
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("desktop_nav_rail")
                    ) {
                        NavigationRailItem(
                            selected = currentTab == MainTab.REGISTER,
                            onClick = { currentTab = MainTab.REGISTER },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "SOP Register"
                                )
                            },
                            label = { Text("Register") },
                            modifier = Modifier.testTag("nav_tab_register")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.SCANNER,
                            onClick = { currentTab = MainTab.SCANNER },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR Code"
                                )
                            },
                            label = { Text("Scan QR") },
                            modifier = Modifier.testTag("nav_tab_scanner")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.LABELS,
                            onClick = { currentTab = MainTab.LABELS },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "QR Labels"
                                )
                            },
                            label = { Text("QR Labels") },
                            modifier = Modifier.testTag("nav_tab_labels")
                        )
                    }
                    VerticalDivider(color = akah.line, thickness = 1.dp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentTab) {
                        MainTab.REGISTER -> {
                            if (isEffectiveTwoPane) {
                                // Desktop / Wide Two-Pane List-Detail Split View
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("desktop_split_pane_register")
                                ) {
                                    DashboardScreen(
                                        allAssets = allAssets,
                                        filteredAssets = filteredAssets,
                                        pendingOfflineSyncCount = pendingOfflineSyncCount,
                                        uiState = uiState,
                                        selectedAssetId = uiState.selectedAssetId,
                                        onSearchChange = { sopViewModel.setSearchQuery(it) },
                                        onSearchScopeChange = { sopViewModel.setSearchScope(it) },
                                        onSelectLocation = { sopViewModel.setSelectedLocation(it) },
                                        onSelectSystem = { sopViewModel.setSelectedSystem(it) },
                                        onToggleQuickFilter = { sopViewModel.toggleQuickFilter(it) },
                                        onResetFilters = { sopViewModel.resetAllFilters() },
                                        onOpenAsset = { id -> sopViewModel.openAssetDetail(id) },
                                        onOpenQuickStatus = { id -> sopViewModel.openQuickStatusDialog(id) },
                                        onOpenDueDateDialog = { id -> sopViewModel.openDueDateDialog(id) },
                                        onOpenAuditLog = { id -> sopViewModel.openAuditLogView(id) },
                                        onNotifyApproachingDeadlines = {
                                            ensureNotificationPermissionAndRun {
                                                sopViewModel.notifyApproachingDeadlines(context)
                                            }
                                        },
                                        onOpenAddAssetDialog = { sopViewModel.setShowAddAssetDialog(true) },
                                        onOpenBulkCsvImportDialog = { sopViewModel.setShowBulkCsvImportDialog(true) },
                                        onOpenExportReportDialog = { sopViewModel.setShowExportReportDialog(true) },
                                        onQuickExportCsv = {
                                            sopViewModel.exportAndShareReport(
                                                context = context,
                                                format = "CSV",
                                                useFilteredList = true,
                                                includeHistory = true
                                            )
                                        },
                                        onQuickExportPdf = {
                                            sopViewModel.exportAndShareReport(
                                                context = context,
                                                format = "PDF",
                                                useFilteredList = true,
                                                includeHistory = true
                                            )
                                        },
                                        onOpenScanner = { currentTab = MainTab.SCANNER },
                                        onOpenQrLabels = { currentTab = MainTab.LABELS },
                                        onExportAllQrsPdf = {
                                            sopViewModel.exportAndShareQrLabelsPdf(
                                                context = context,
                                                assetsToExport = filteredAssets.ifEmpty { allAssets }
                                            )
                                        },
                                        onOpenWhoDialog = { sopViewModel.setShowWhoDialog(true) },
                                        onRegisterAiQueryChange = { sopViewModel.updateRegisterAiQuery(it) },
                                        onToggleRegisterAiHighThinking = { sopViewModel.toggleRegisterAiHighThinking(it) },
                                        onAskRegisterAi = { q -> sopViewModel.askRegisterAi(q) },
                                        modifier = Modifier
                                            .weight(0.46f)
                                            .fillMaxHeight()
                                    )

                                    VerticalDivider(color = akah.line, thickness = 1.dp)

                                    Box(
                                        modifier = Modifier
                                            .weight(0.54f)
                                            .fillMaxHeight()
                                    ) {
                                        if (activeAsset != null) {
                                            AssetDetailScreen(
                                                asset = activeAsset,
                                                logs = activeAssetLogs,
                                                meName = uiState.meName,
                                                qrPayloadUrl = sopViewModel.buildAssetQrPayload(activeAsset.id),
                                                flashMessage = uiState.flashMessage,
                                                isFlashError = uiState.isFlashError,
                                                isSaving = uiState.isSaving,
                                                assetAiState = uiState.assetAi,
                                                onBack = { sopViewModel.openAssetDetail(null) },
                                                onOpenWhoDialog = { sopViewModel.setShowWhoDialog(true) },
                                                onOpenQuickStatus = { sopViewModel.openQuickStatusDialog(activeAsset.id) },
                                                onOpenDueDateDialog = { sopViewModel.openDueDateDialog(activeAsset.id) },
                                                onOpenAuditLogView = { sopViewModel.openAuditLogView(activeAsset.id) },
                                                onSaveCheck = { freqCode, resultStatus, marks, readings, notes, onDone ->
                                                    sopViewModel.recordCheck(
                                                        assetId = activeAsset.id,
                                                        freqCode = freqCode,
                                                        resultStatus = resultStatus,
                                                        marks = marks,
                                                        readings = readings,
                                                        notes = notes,
                                                        onComplete = onDone
                                                    )
                                                },
                                                onSaveDetails = { status, crit, loc, make, model, serial, cap, inst, vendor, exp, owner, notes, onDone ->
                                                    sopViewModel.saveAssetDetails(
                                                        assetId = activeAsset.id,
                                                        status = status,
                                                        criticality = crit,
                                                        location = loc,
                                                        make = make,
                                                        model = model,
                                                        serial = serial,
                                                        capacity = cap,
                                                        installed = inst,
                                                        amcVendor = vendor,
                                                        amcExpiry = exp,
                                                        owner = owner,
                                                        notes = notes,
                                                        onComplete = onDone
                                                    )
                                                },
                                                onAssetAiQueryChange = { sopViewModel.updateAssetAiQuery(it) },
                                                onToggleAssetAiHighThinking = { sopViewModel.toggleAssetAiHighThinking(it) },
                                                onAskAssetAi = { q -> sopViewModel.askAssetAi(activeAsset, activeAssetLogs, q) },
                                                isTwoPane = true
                                            )
                                        } else {
                                            DesktopEmptyDetailPane(
                                                allAssets = allAssets,
                                                onSelectAsset = { id -> sopViewModel.openAssetDetail(id) },
                                                onOpenScanner = { currentTab = MainTab.SCANNER },
                                                onOpenAddAsset = { sopViewModel.setShowAddAssetDialog(true) },
                                                onOpenBulkCsvImport = { sopViewModel.setShowBulkCsvImportDialog(true) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Mobile Single-Pane View
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (activeAsset != null) {
                                        AssetDetailScreen(
                                            asset = activeAsset,
                                            logs = activeAssetLogs,
                                            meName = uiState.meName,
                                            qrPayloadUrl = sopViewModel.buildAssetQrPayload(activeAsset.id),
                                            flashMessage = uiState.flashMessage,
                                            isFlashError = uiState.isFlashError,
                                            isSaving = uiState.isSaving,
                                            assetAiState = uiState.assetAi,
                                            onBack = { sopViewModel.openAssetDetail(null) },
                                            onOpenWhoDialog = { sopViewModel.setShowWhoDialog(true) },
                                            onOpenQuickStatus = { sopViewModel.openQuickStatusDialog(activeAsset.id) },
                                            onOpenDueDateDialog = { sopViewModel.openDueDateDialog(activeAsset.id) },
                                            onOpenAuditLogView = { sopViewModel.openAuditLogView(activeAsset.id) },
                                            onSaveCheck = { freqCode, resultStatus, marks, readings, notes, onDone ->
                                                sopViewModel.recordCheck(
                                                    assetId = activeAsset.id,
                                                    freqCode = freqCode,
                                                    resultStatus = resultStatus,
                                                    marks = marks,
                                                    readings = readings,
                                                    notes = notes,
                                                    onComplete = onDone
                                                )
                                            },
                                            onSaveDetails = { status, crit, loc, make, model, serial, cap, inst, vendor, exp, owner, notes, onDone ->
                                                sopViewModel.saveAssetDetails(
                                                    assetId = activeAsset.id,
                                                    status = status,
                                                    criticality = crit,
                                                    location = loc,
                                                    make = make,
                                                    model = model,
                                                    serial = serial,
                                                    capacity = cap,
                                                    installed = inst,
                                                    amcVendor = vendor,
                                                    amcExpiry = exp,
                                                    owner = owner,
                                                    notes = notes,
                                                    onComplete = onDone
                                                )
                                            },
                                            onAssetAiQueryChange = { sopViewModel.updateAssetAiQuery(it) },
                                            onToggleAssetAiHighThinking = { sopViewModel.toggleAssetAiHighThinking(it) },
                                            onAskAssetAi = { q -> sopViewModel.askAssetAi(activeAsset, activeAssetLogs, q) },
                                            isTwoPane = false
                                        )
                                    } else {
                                        DashboardScreen(
                                            allAssets = allAssets,
                                            filteredAssets = filteredAssets,
                                            pendingOfflineSyncCount = pendingOfflineSyncCount,
                                            uiState = uiState,
                                            selectedAssetId = uiState.selectedAssetId,
                                            onSearchChange = { sopViewModel.setSearchQuery(it) },
                                            onSearchScopeChange = { sopViewModel.setSearchScope(it) },
                                            onSelectLocation = { sopViewModel.setSelectedLocation(it) },
                                            onSelectSystem = { sopViewModel.setSelectedSystem(it) },
                                            onToggleQuickFilter = { sopViewModel.toggleQuickFilter(it) },
                                            onResetFilters = { sopViewModel.resetAllFilters() },
                                            onOpenAsset = { id -> sopViewModel.openAssetDetail(id) },
                                            onOpenQuickStatus = { id -> sopViewModel.openQuickStatusDialog(id) },
                                            onOpenDueDateDialog = { id -> sopViewModel.openDueDateDialog(id) },
                                            onOpenAuditLog = { id -> sopViewModel.openAuditLogView(id) },
                                            onNotifyApproachingDeadlines = {
                                                ensureNotificationPermissionAndRun {
                                                    sopViewModel.notifyApproachingDeadlines(context)
                                                }
                                            },
                                            onOpenAddAssetDialog = { sopViewModel.setShowAddAssetDialog(true) },
                                            onOpenBulkCsvImportDialog = { sopViewModel.setShowBulkCsvImportDialog(true) },
                                            onOpenExportReportDialog = { sopViewModel.setShowExportReportDialog(true) },
                                            onQuickExportCsv = {
                                                sopViewModel.exportAndShareReport(
                                                    context = context,
                                                    format = "CSV",
                                                    useFilteredList = true,
                                                    includeHistory = true
                                                )
                                            },
                                            onQuickExportPdf = {
                                                sopViewModel.exportAndShareReport(
                                                    context = context,
                                                    format = "PDF",
                                                    useFilteredList = true,
                                                    includeHistory = true
                                                )
                                            },
                                            onOpenScanner = { currentTab = MainTab.SCANNER },
                                            onOpenQrLabels = { currentTab = MainTab.LABELS },
                                            onExportAllQrsPdf = {
                                                sopViewModel.exportAndShareQrLabelsPdf(
                                                    context = context,
                                                    assetsToExport = filteredAssets.ifEmpty { allAssets }
                                                )
                                            },
                                            onOpenWhoDialog = { sopViewModel.setShowWhoDialog(true) },
                                            onRegisterAiQueryChange = { sopViewModel.updateRegisterAiQuery(it) },
                                            onToggleRegisterAiHighThinking = { sopViewModel.toggleRegisterAiHighThinking(it) },
                                            onAskRegisterAi = { q -> sopViewModel.askRegisterAi(q) }
                                        )
                                    }
                                }
                            }
                        }

                        MainTab.SCANNER -> {
                            QrScannerScreen(
                                allAssets = allAssets,
                                recorderName = uiState.meName,
                                isDesktopMode = isEffectiveDesktop,
                                onBack = { currentTab = MainTab.REGISTER },
                                onOpenAssetSheet = { id ->
                                    sopViewModel.openAssetDetail(id)
                                    currentTab = MainTab.REGISTER
                                },
                                onQuickUpdateStatus = { assetId, newStatus, note ->
                                    sopViewModel.quickUpdateAssetStatus(
                                        assetId = assetId,
                                        newStatus = newStatus,
                                        note = note
                                    )
                                }
                            )
                        }

                        MainTab.LABELS -> {
                            QrLabelsScreen(
                                allAssets = allAssets,
                                qrBaseAddress = uiState.qrBaseAddress,
                                lanServerUrl = uiState.lanServerUrl,
                                isSyncingLan = uiState.isSyncingLan,
                                lanSyncMessage = uiState.lanSyncMessage,
                                isExportingPdf = uiState.isExportingReport,
                                lastExportMessage = uiState.lastExportReportMessage,
                                suggestAssetId = { sys -> sopViewModel.suggestNextAssetId(sys, allAssets) },
                                buildAssetQrUrl = { id -> sopViewModel.buildAssetQrPayload(id) },
                                onSaveQrAddress = { sopViewModel.saveQrBaseAddress(it) },
                                onSyncLanServer = { sopViewModel.syncWithLanServer(it) },
                                onCreateNewAssetWithQr = { id, name, system, location, criticality, status, owner, notes, make, model, capacity, exportPdfImmediately, onError, onCreated ->
                                    sopViewModel.addNewAssetOffline(
                                        id = id,
                                        name = name,
                                        system = system,
                                        location = location,
                                        criticality = criticality,
                                        status = status,
                                        owner = owner,
                                        notes = notes,
                                        make = make,
                                        model = model,
                                        capacity = capacity,
                                        openAfterSave = false,
                                        onError = onError,
                                        onCreated = { createdAsset ->
                                            onCreated(createdAsset)
                                            if (exportPdfImmediately) {
                                                sopViewModel.exportAndShareQrLabelsPdf(
                                                    context = context,
                                                    assetsToExport = listOf(createdAsset),
                                                    customFileName = "AKAH_QR_${createdAsset.id}.pdf"
                                                )
                                            }
                                        }
                                    )
                                },
                                onExportQrLabelsPdf = { assetsToExport, fileName ->
                                    sopViewModel.exportAndShareQrLabelsPdf(
                                        context = context,
                                        assetsToExport = assetsToExport,
                                        customFileName = fileName
                                    )
                                },
                                onOpenAsset = { id ->
                                    sopViewModel.openAssetDetail(id)
                                    currentTab = MainTab.REGISTER
                                },
                                onBack = { currentTab = MainTab.REGISTER }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showWhoDialog) {
        WhoIsRecordingDialog(
            currentName = uiState.meName,
            onDismiss = { sopViewModel.setShowWhoDialog(false) },
            onSave = { sopViewModel.saveMeName(it) }
        )
    }

    if (uiState.showAddAssetDialog) {
        AddAssetOfflineDialog(
            suggestAssetId = { sys -> sopViewModel.suggestNextAssetId(sys, allAssets) },
            onDismiss = { sopViewModel.setShowAddAssetDialog(false) },
            onAddAsset = { id, name, system, location, criticality, status, owner, notes, make, model, capacity, routine, ppm, openAfterSave, onError ->
                sopViewModel.addNewAssetOffline(
                    id = id,
                    name = name,
                    system = system,
                    location = location,
                    criticality = criticality,
                    status = status,
                    owner = owner,
                    notes = notes,
                    make = make,
                    model = model,
                    capacity = capacity,
                    routine = routine,
                    ppm = ppm,
                    openAfterSave = openAfterSave,
                    onError = onError
                )
            }
        )
    }

    if (quickStatusDialogAsset != null) {
        QuickStatusUpdateDialog(
            asset = quickStatusDialogAsset,
            recorderName = uiState.meName,
            onDismiss = { sopViewModel.openQuickStatusDialog(null) },
            onConfirmUpdate = { newStatus, newLocation, note ->
                sopViewModel.quickUpdateAssetStatus(
                    assetId = quickStatusDialogAsset.id,
                    newStatus = newStatus,
                    newLocation = newLocation,
                    note = note
                )
            },
            onOpenFullSheet = {
                val id = quickStatusDialogAsset.id
                sopViewModel.openQuickStatusDialog(null)
                sopViewModel.openAssetDetail(id)
                currentTab = MainTab.REGISTER
            }
        )
    }

    if (uiState.showBulkCsvImportDialog) {
        BulkCsvImportDialog(
            existingAssets = allAssets,
            isImporting = uiState.isImportingCsv,
            lastImportMessage = uiState.lastCsvImportMessage,
            lastImportIsError = uiState.lastCsvImportIsError,
            onDismiss = { sopViewModel.setShowBulkCsvImportDialog(false) },
            onDownloadSampleTemplate = { sopViewModel.shareSampleCsvTemplate(context) },
            onImportCsv = { rawCsvText, sourceName, conflictMode, onSuccessSummary ->
                sopViewModel.importAssetsFromCsvText(
                    rawCsvText = rawCsvText,
                    sourceName = sourceName,
                    conflictMode = conflictMode,
                    closeDialogOnSuccess = false,
                    onComplete = { summary ->
                        if (summary != null) {
                            onSuccessSummary(summary)
                        }
                    }
                )
            },
            onExportImportedQrPdf = { importedAssets ->
                sopViewModel.exportAndShareQrLabelsPdf(
                    context = context,
                    assetsToExport = importedAssets,
                    customFileName = "AKAH_Imported_Assets_QR_Labels.pdf"
                )
            }
        )
    }

    if (uiState.showExportReportDialog) {
        val isFilterActive = uiState.searchQuery.isNotBlank() ||
            uiState.selectedLocation != "All Locations" ||
            uiState.selectedSystem != "All" ||
            uiState.quickFilter != com.example.viewmodel.QuickStatFilter.ALL

        ExportReportDialog(
            filteredAssetsCount = filteredAssets.size,
            allAssetsCount = allAssets.size,
            isFilterActive = isFilterActive,
            isExporting = uiState.isExportingReport,
            lastExportMessage = uiState.lastExportReportMessage,
            onDismiss = { sopViewModel.setShowExportReportDialog(false) },
            onExport = { format, useFilteredList, includeHistory ->
                sopViewModel.exportAndShareReport(
                    context = context,
                    format = format,
                    useFilteredList = useFilteredList,
                    includeHistory = includeHistory
                )
            }
        )
    }

    if (dueDateDialogAsset != null) {
        SetMaintenanceDueDateDialog(
            asset = dueDateDialogAsset,
            onDismiss = { sopViewModel.openDueDateDialog(null) },
            onSaveDueDate = { freqCode, dueDateIso, reminderLeadHours, notificationsEnabled, taskNote, sendTestNotificationNow ->
                ensureNotificationPermissionAndRun {
                    sopViewModel.saveAssetMaintenanceDueDate(
                        context = context,
                        assetId = dueDateDialogAsset.id,
                        freqCode = freqCode,
                        dueDateIso = dueDateIso,
                        reminderLeadHours = reminderLeadHours,
                        notificationsEnabled = notificationsEnabled,
                        taskNote = taskNote,
                        sendTestNotificationNow = sendTestNotificationNow
                    )
                }
            },
            onClearDueDate = {
                sopViewModel.clearAssetMaintenanceDueDate(context, dueDateDialogAsset.id)
            }
        )
    }

    if (auditLogDialogAsset != null) {
        AssetAuditLogDialog(
            asset = auditLogDialogAsset,
            logs = auditLogDialogLogs,
            defaultOperatorName = uiState.meName,
            isSaving = uiState.isSaving,
            onDismiss = { sopViewModel.openAuditLogView(null) },
            onRecordManualAuditEntry = { activityDateIso, by, activityKind, freqCode, previousStatus, newStatus, summaryNotes, updateCurrentAssetStatus ->
                sopViewModel.recordManualAuditActivity(
                    assetId = auditLogDialogAsset.id,
                    activityDateIso = activityDateIso,
                    by = by,
                    activityKind = activityKind,
                    freqCode = freqCode,
                    previousStatus = previousStatus,
                    newStatus = newStatus,
                    summaryNotes = summaryNotes,
                    updateCurrentAssetStatus = updateCurrentAssetStatus
                )
            },
            onExportAssetAudit = { format ->
                sopViewModel.exportSingleAssetAuditReport(
                    context = context,
                    asset = auditLogDialogAsset,
                    logs = auditLogDialogLogs,
                    format = format
                )
            },
            onOpenAssetSheet = if (uiState.selectedAssetId != auditLogDialogAsset.id) {
                {
                    val id = auditLogDialogAsset.id
                    sopViewModel.openAuditLogView(null)
                    sopViewModel.openAssetDetail(id)
                    currentTab = MainTab.REGISTER
                }
            } else {
                null
            }
        )
    }
}
