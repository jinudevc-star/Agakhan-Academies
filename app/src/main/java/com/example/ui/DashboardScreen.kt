package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditAttributes
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiSopService
import com.example.model.AssetEntity
import com.example.model.DeadlineStatus
import com.example.model.SopConstants
import com.example.model.SopScheduleParser
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.viewmodel.QuickStatFilter
import com.example.viewmodel.SearchFieldScope
import com.example.viewmodel.SopUiState

@Composable
fun DashboardScreen(
    allAssets: List<AssetEntity>,
    filteredAssets: List<AssetEntity>,
    pendingOfflineSyncCount: Int,
    uiState: SopUiState,
    selectedAssetId: String? = null,
    onSearchChange: (String) -> Unit,
    onSearchScopeChange: (SearchFieldScope) -> Unit = {},
    onSelectLocation: (String) -> Unit = {},
    onSelectSystem: (String) -> Unit,
    onToggleQuickFilter: (QuickStatFilter) -> Unit,
    onResetFilters: () -> Unit = {},
    onOpenAsset: (String) -> Unit,
    onOpenQuickStatus: (String) -> Unit,
    onOpenDueDateDialog: (String) -> Unit = {},
    onOpenAuditLog: (String) -> Unit = {},
    onNotifyApproachingDeadlines: () -> Unit = {},
    onOpenAddAssetDialog: () -> Unit,
    onOpenBulkCsvImportDialog: () -> Unit = {},
    onOpenExportReportDialog: () -> Unit = {},
    onQuickExportCsv: () -> Unit = {},
    onQuickExportPdf: () -> Unit = {},
    onOpenScanner: () -> Unit,
    onOpenQrLabels: () -> Unit,
    onExportAllQrsPdf: () -> Unit = {},
    onOpenWhoDialog: () -> Unit,
    onRegisterAiQueryChange: (String) -> Unit,
    onToggleRegisterAiHighThinking: (Boolean) -> Unit,
    onAskRegisterAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    val focusManager = LocalFocusManager.current

    val inServiceCount = remember(allAssets) { allAssets.count { it.status == "In service" } }
    val needsAttentionCount = remember(allAssets) { allAssets.count { it.status == "Needs attention" } }
    val outOfServiceCount = remember(allAssets) { allAssets.count { it.status == "Out of service" } }
    val overdueCount = remember(allAssets) { allAssets.count { SopScheduleParser.isLate(it) } }
    val dueSoonCount = remember(allAssets) { allAssets.count { SopScheduleParser.isApproachingDeadline(it) } }

    val availableLocations = remember(allAssets) {
        listOf("All Locations") + allAssets
            .map { it.location.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val isFilteringActive = uiState.searchQuery.isNotBlank() ||
        uiState.selectedLocation != "All Locations" ||
        uiState.selectedSystem != "All" ||
        uiState.quickFilter != QuickStatFilter.ALL

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top-Pinned Search Bar for finding assets by Name or Location
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("top_asset_search_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = when (uiState.searchScope) {
                                SearchFieldScope.ALL -> "Search assets by name or location…"
                                SearchFieldScope.NAME -> "Search assets by name or ID…"
                                SearchFieldScope.LOCATION -> "Search assets by campus location…"
                            },
                            color = akah.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search assets by name or location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedBorderColor = akah.line
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )

                // Search Scope pills (Name & Location / Name Only / Location Only) + Live Match Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        SearchFieldScope.entries.forEach { scope ->
                            val selected = uiState.searchScope == scope
                            Surface(
                                onClick = { onSearchScopeChange(scope) },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else akah.line
                                ),
                                modifier = Modifier.testTag("search_scope_${scope.name.lowercase()}")
                            ) {
                                Text(
                                    text = scope.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else akah.muted,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (isFilteringActive) {
                        Surface(
                            onClick = onResetFilters,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("reset_filters_button")
                        ) {
                            Text(
                                text = "Reset (${filteredAssets.size}/${allAssets.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick Location Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableLocations.forEach { loc ->
                        val isSelected = uiState.selectedLocation == loc
                        Surface(
                            onClick = { onSelectLocation(loc) },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                            ),
                            modifier = Modifier.testTag("location_chip_$loc")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else akah.muted
                                )
                                Text(
                                    text = loc,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = akah.line, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // System Filter Chips Row + Asset List Count Header
            item(key = "system_chips_and_count") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allSystems = remember { listOf("All") + SopConstants.SYSTEMS }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allSystems.forEach { sys ->
                            val isSelected = uiState.selectedSystem == sys
                            Surface(
                                onClick = { onSelectSystem(sys) },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                                ),
                                modifier = Modifier.testTag("system_chip_$sys")
                            ) {
                                Text(
                                    text = sys,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else akah.muted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFilteringActive) {
                                    "MATCHING ASSETS (${filteredAssets.size} OF ${allAssets.size})"
                                } else {
                                    "CAMPUS ASSET LIST (${allAssets.size})"
                                },
                                fontFamily = BarlowDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp,
                                color = akah.muted,
                                modifier = Modifier.testTag("asset_list_count_header")
                            )
                            if (uiState.searchQuery.isNotBlank()) {
                                Text(
                                    text = "Query: \"${uiState.searchQuery}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onOpenBulkCsvImportDialog,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("import_csv_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Bulk import assets from CSV file",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import CSV", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = onQuickExportCsv,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("export_csv_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Export current asset list as CSV",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = onQuickExportPdf,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("export_pdf_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export current asset list as PDF",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = onOpenExportReportDialog,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("export_report_dashboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export CSV or PDF report options",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // When not actively typing a search query, show compact KPI cards, QR tools, and Offline Room status bar
            if (uiState.searchQuery.isBlank()) {
                item(key = "offline_room_bar") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, akah.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_room_status_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Offline Room Database",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Offline Room DB · ${allAssets.size} Assets Tracked",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (pendingOfflineSyncCount > 0) {
                                            "$pendingOfflineSyncCount local status/location updates stored offline"
                                        } else {
                                            "All asset names, locations & maintenance statuses cached locally"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = akah.muted
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onOpenBulkCsvImportDialog,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dashboard_import_csv_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = "Bulk Import CSV",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import CSV", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onOpenAddAssetDialog,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_asset_offline_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Asset Offline",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Asset", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (uiState.meName.isBlank()) {
                    item(key = "notice_unset_name") {
                        Surface(
                            onClick = onOpenWhoDialog,
                            shape = RoundedCornerShape(10.dp),
                            color = akah.warnSoft,
                            border = BorderStroke(1.dp, akah.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "You can view assets and history. To record checks or edit details, tap “Set your name” at the top.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                if (!uiState.flashMessage.isNullOrBlank()) {
                    item(key = "flash_banner") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.isFlashError) akah.critSoft else akah.okSoft,
                            border = BorderStroke(1.dp, if (uiState.isFlashError) akah.crit else akah.ok),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.flashMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.isFlashError) akah.crit else akah.ok,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                // 4 KPI Stat Cards (1x4 horizontal row on Desktop/Wide, 2x2 grid on Mobile)
                item(key = "kpi_stats") {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val useFourColumnRow = maxWidth >= 480.dp
                        if (useFourColumnRow) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KpiStatCard(
                                    count = inServiceCount,
                                    label = "In service",
                                    valueColor = akah.ok,
                                    isSelected = uiState.quickFilter == QuickStatFilter.IN_SERVICE,
                                    onClick = { onToggleQuickFilter(QuickStatFilter.IN_SERVICE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stat_in_service")
                                )
                                KpiStatCard(
                                    count = needsAttentionCount,
                                    label = "Needs attention",
                                    valueColor = akah.warn,
                                    isSelected = uiState.quickFilter == QuickStatFilter.NEEDS_ATTENTION,
                                    onClick = { onToggleQuickFilter(QuickStatFilter.NEEDS_ATTENTION) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stat_needs_attention")
                                )
                                KpiStatCard(
                                    count = outOfServiceCount,
                                    label = "Out of service",
                                    valueColor = akah.crit,
                                    isSelected = uiState.quickFilter == QuickStatFilter.OUT_OF_SERVICE,
                                    onClick = { onToggleQuickFilter(QuickStatFilter.OUT_OF_SERVICE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stat_out_of_service")
                                )
                                KpiStatCard(
                                    count = overdueCount,
                                    label = "Routine checks overdue",
                                    valueColor = if (overdueCount > 0) akah.warn else MaterialTheme.colorScheme.onSurface,
                                    isSelected = uiState.quickFilter == QuickStatFilter.OVERDUE,
                                    onClick = { onToggleQuickFilter(QuickStatFilter.OVERDUE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stat_overdue")
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KpiStatCard(
                                        count = inServiceCount,
                                        label = "In service",
                                        valueColor = akah.ok,
                                        isSelected = uiState.quickFilter == QuickStatFilter.IN_SERVICE,
                                        onClick = { onToggleQuickFilter(QuickStatFilter.IN_SERVICE) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("stat_in_service")
                                    )
                                    KpiStatCard(
                                        count = needsAttentionCount,
                                        label = "Needs attention",
                                        valueColor = akah.warn,
                                        isSelected = uiState.quickFilter == QuickStatFilter.NEEDS_ATTENTION,
                                        onClick = { onToggleQuickFilter(QuickStatFilter.NEEDS_ATTENTION) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("stat_needs_attention")
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KpiStatCard(
                                        count = outOfServiceCount,
                                        label = "Out of service",
                                        valueColor = akah.crit,
                                        isSelected = uiState.quickFilter == QuickStatFilter.OUT_OF_SERVICE,
                                        onClick = { onToggleQuickFilter(QuickStatFilter.OUT_OF_SERVICE) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("stat_out_of_service")
                                    )
                                    KpiStatCard(
                                        count = overdueCount,
                                        label = "Routine checks overdue",
                                        valueColor = if (overdueCount > 0) akah.warn else MaterialTheme.colorScheme.onSurface,
                                        isSelected = uiState.quickFilter == QuickStatFilter.OVERDUE,
                                        onClick = { onToggleQuickFilter(QuickStatFilter.OVERDUE) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("stat_overdue")
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Asset Status Distribution Chart Card (Operational / Maintenance Required / Out of Service)
                item(key = "asset_status_distribution_chart") {
                    AssetStatusDistributionCard(
                        allAssets = allAssets,
                        filteredAssets = filteredAssets,
                        isFilteringActive = isFilteringActive,
                        activeQuickFilter = uiState.quickFilter,
                        onToggleQuickFilter = onToggleQuickFilter
                    )
                }

                // Quick QR Scanner CTA & QR Labels Row
                item(key = "qr_tools_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenScanner,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_qr_cta_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Asset QR Code",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scan Asset QR Code",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenQrLabels,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("qr_labels_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR labels",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "QR Labels",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onExportAllQrsPdf,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("dashboard_export_qrs_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export all available QR codes in PDF",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "QRs PDF",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Approaching SOP Deadlines & Local Notification Bar
                item(key = "approaching_deadlines_bar") {
                    val isDueSoonFiltered = uiState.quickFilter == QuickStatFilter.DUE_SOON
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (dueSoonCount > 0) akah.warnSoft else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isDueSoonFiltered) 2.dp else 1.dp,
                            color = if (dueSoonCount > 0 || isDueSoonFiltered) akah.warn else akah.line
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("approaching_deadlines_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "SOP Deadlines & Local Notifications",
                                    tint = if (dueSoonCount > 0) akah.warn else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Column {
                                    Text(
                                        text = if (dueSoonCount > 0) {
                                            "$dueSoonCount Asset SOP Check${if (dueSoonCount == 1) "" else "s"} Approaching Deadline"
                                        } else {
                                            "SOP Maintenance Due Dates & Local Alerts Active"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap the calendar icon on any asset to set a due date & local notification alert",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = akah.muted
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onToggleQuickFilter(QuickStatFilter.DUE_SOON) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                    modifier = Modifier.testTag("due_soon_filter_button")
                                ) {
                                    Text(
                                        text = if (isDueSoonFiltered) "Show All" else "Due Soon ($dueSoonCount)",
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = onNotifyApproachingDeadlines,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                    modifier = Modifier.testTag("notify_approaching_deadlines_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Send local notifications for approaching SOP checks",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Notify", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Empty state or Asset rows
            if (filteredAssets.isEmpty()) {
                item(key = "empty_list") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, akah.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_search_state")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (allAssets.isEmpty()) {
                                    "Loading assets…"
                                } else {
                                    "No asset matches \"${uiState.searchQuery.ifBlank { uiState.selectedLocation }}\" by name or location."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = akah.muted
                            )
                            if (isFilteringActive) {
                                OutlinedButton(
                                    onClick = onResetFilters,
                                    shape = CircleShape
                                ) {
                                    Text("Clear search & filters")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredAssets,
                    key = { it.id }
                ) { asset ->
                    AssetRegisterRow(
                        asset = asset,
                        isSelected = asset.id == selectedAssetId,
                        highlightQuery = uiState.searchQuery,
                        onClick = { onOpenAsset(asset.id) },
                        onQuickStatusClick = { onOpenQuickStatus(asset.id) },
                        onSetDueDateClick = { onOpenDueDateDialog(asset.id) },
                        onAuditLogClick = { onOpenAuditLog(asset.id) }
                    )
                }
            }

            // Ask AI about the register (High Thinking Mode with gemini-3.1-pro-preview)
            item(key = "ask_ai_register") {
                Spacer(modifier = Modifier.height(6.dp))
                AskAiMaintenanceCard(
                    title = "Ask AI about the register",
                    presetPrompts = GeminiSopService.AI_REGISTER_PROMPTS,
                    aiState = uiState.registerAi,
                    onQueryChange = onRegisterAiQueryChange,
                    onToggleHighThinking = onToggleRegisterAiHighThinking,
                    onAskQuestion = onAskRegisterAi
                )
            }

            // Footer Info
            item(key = "footer_info") {
                Text(
                    text = "SOP ${SopConstants.SOP_CODE} · Aga Khan Academy, Hyderabad · Every saved check and edit is stored in the local Room SQLite database and can be exported as Assets.csv and Maintenance_History.csv for Excel or Power BI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 72.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    count: Int,
    label: String,
    valueColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else akah.line
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = count.toString(),
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = akah.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AssetRegisterRow(
    asset: AssetEntity,
    isSelected: Boolean = false,
    highlightQuery: String = "",
    onClick: () -> Unit,
    onQuickStatusClick: () -> Unit,
    onSetDueDateClick: () -> Unit = {},
    onAuditLogClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    val critColor = CriticalityColor(asset.criticality)
    val lastCheck = asset.lastCheck
    val isLate = remember(asset) { SopScheduleParser.isLate(asset) }
    val deadlineEval = remember(asset) { SopScheduleParser.evaluateAssetDeadline(asset) }
    val primaryHighlightBg = MaterialTheme.colorScheme.primaryContainer
    val primaryHighlightFg = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_row_${asset.id}"),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else akah.line
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left 6.dp criticality stripe
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(critColor)
            )

            // Main Asset Info (ID, Name, Location, System/Criticality)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = highlightTextMatch(
                            text = asset.id,
                            query = highlightQuery,
                            highlightBg = primaryHighlightBg,
                            highlightFg = primaryHighlightFg
                        ),
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 12.sp,
                        color = akah.muted
                    )
                    if (asset.pendingOfflineSync) {
                        Text(
                            text = "· Offline saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = highlightTextMatch(
                        text = asset.name,
                        query = highlightQuery,
                        highlightBg = primaryHighlightBg,
                        highlightFg = primaryHighlightFg
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (asset.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = highlightTextMatch(
                                text = asset.location,
                                query = highlightQuery,
                                highlightBg = primaryHighlightBg,
                                highlightFg = primaryHighlightFg
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = "${asset.criticality.uppercase()} · ${asset.system}",
                    fontFamily = BarlowDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.7.sp,
                    color = critColor
                )
            }

            // Right Status & Quick Update Column
            Column(
                modifier = Modifier.padding(end = 10.dp, top = 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StatusPill(status = asset.status)
                    IconButton(
                        onClick = onAuditLogClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("audit_log_btn_${asset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View maintenance and status audit log for ${asset.id}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onSetDueDateClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("set_due_date_btn_${asset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Set maintenance due date for ${asset.id}",
                            tint = if (deadlineEval.status == DeadlineStatus.APPROACHING) {
                                akah.warn
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onQuickStatusClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("quick_status_btn_${asset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditAttributes,
                            contentDescription = "Quick update status for ${asset.id}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = if (lastCheck != null && lastCheck.at.isNotBlank()) {
                        "Checked ${SopScheduleParser.ago(lastCheck.at)}"
                    } else {
                        "No check recorded"
                    },
                    fontSize = 12.sp,
                    color = akah.muted
                )

                if (deadlineEval.shortBadgeText.isNotBlank() && asset.nextDueDateIso.isNotBlank()) {
                    val badgeBg = when (deadlineEval.status) {
                        DeadlineStatus.OVERDUE -> akah.critSoft
                        DeadlineStatus.APPROACHING -> akah.warnSoft
                        else -> akah.okSoft
                    }
                    val badgeFg = when (deadlineEval.status) {
                        DeadlineStatus.OVERDUE -> akah.crit
                        DeadlineStatus.APPROACHING -> akah.warn
                        else -> akah.ok
                    }
                    Surface(
                        onClick = onSetDueDateClick,
                        color = badgeBg,
                        shape = CircleShape,
                        modifier = Modifier.testTag("asset_due_badge_${asset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = badgeFg,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = deadlineEval.shortBadgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeFg
                            )
                        }
                    }
                } else if (isLate) {
                    Surface(
                        onClick = onSetDueDateClick,
                        color = akah.warnSoft,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(akah.warn, CircleShape)
                            )
                            Text(
                                text = "Check due",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = akah.warn
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightTextMatch(
    text: String,
    query: String,
    highlightBg: Color,
    highlightFg: Color
): AnnotatedString {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty()) return AnnotatedString(text)
    val startIndex = text.indexOf(cleanQuery, ignoreCase = true)
    if (startIndex < 0) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val matchIdx = text.indexOf(cleanQuery, startIndex = cursor, ignoreCase = true)
            if (matchIdx < 0) {
                append(text.substring(cursor))
                break
            }
            if (matchIdx > cursor) {
                append(text.substring(cursor, matchIdx))
            }
            withStyle(
                SpanStyle(
                    background = highlightBg,
                    color = highlightFg,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(matchIdx, matchIdx + cleanQuery.length))
            }
            cursor = matchIdx + cleanQuery.length
        }
    }
}

@Composable
fun DesktopEmptyDetailPane(
    allAssets: List<AssetEntity>,
    onSelectAsset: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAddAsset: () -> Unit,
    onOpenBulkCsvImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    val attentionAssets = remember(allAssets) {
        allAssets.filter {
            it.status != "In service" || SopScheduleParser.isLate(it)
        }.take(6)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("desktop_empty_detail_pane"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, akah.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DESKTOP SPLIT-PANE INSPECTOR",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Select any asset from the register on the left to inspect its SOP checklist, record daily/weekly/monthly checks, update maintenance status, or ask Gemini 3.1 Pro High-Thinking AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onOpenScanner) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Asset QR Code")
                    }
                    OutlinedButton(onClick = onOpenAddAsset) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Asset",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register New Asset")
                    }
                    OutlinedButton(
                        onClick = onOpenBulkCsvImport,
                        modifier = Modifier.testTag("desktop_empty_import_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Bulk Import CSV",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bulk CSV Import")
                    }
                }
            }
        }

        if (attentionAssets.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PRIORITY ASSETS REQUIRING ATTENTION OR ROUTINE CHECK",
                        style = MaterialTheme.typography.titleSmall,
                        color = akah.muted
                    )
                    attentionAssets.forEach { asset ->
                        Surface(
                            onClick = { onSelectAsset(asset.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, akah.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${asset.id} · ${asset.name}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${asset.system} · ${asset.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = akah.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                StatusPill(status = asset.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
