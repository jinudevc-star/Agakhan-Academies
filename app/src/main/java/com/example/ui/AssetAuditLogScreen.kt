package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AssetEntity
import com.example.model.MaintenanceLogEntity
import com.example.model.SopConstants
import com.example.model.SopScheduleParser
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class AuditLogFilterTab(val label: String) {
    ALL("All Activity"),
    SOP_CHECKS("SOP Checks"),
    STATUS_UPDATES("Status Updates"),
    FAULTS("Faults / Attention"),
    SCHEDULE_AND_EDITS("Schedule & Edits")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssetAuditLogDialog(
    asset: AssetEntity,
    logs: List<MaintenanceLogEntity>,
    defaultOperatorName: String,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onRecordManualAuditEntry: (
        activityDateIso: String,
        by: String,
        activityKind: String,
        freqCode: String,
        previousStatus: String,
        newStatus: String,
        summaryNotes: String,
        updateCurrentAssetStatus: Boolean
    ) -> Unit,
    onExportAssetAudit: (format: String) -> Unit = {},
    onOpenAssetSheet: (() -> Unit)? = null
) {
    val akah = LocalAkahColors.current

    var selectedTab by remember { mutableStateOf(AuditLogFilterTab.ALL) }
    var selectedFreqFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddActivityPanel by remember { mutableStateOf(false) }

    // Manual Audit Entry form state
    var manualKind by remember { mutableStateOf("check") } // "check" or "details"
    var manualFreq by remember { mutableStateOf("D") }
    var manualPrevStatus by remember(asset.status) { mutableStateOf(asset.status) }
    var manualNewStatus by remember(asset.status) { mutableStateOf(asset.status) }
    var manualBy by remember(defaultOperatorName) {
        mutableStateOf(defaultOperatorName.ifBlank { "R. Sharma" })
    }
    var manualNotes by remember { mutableStateOf("") }
    var manualUpdateCurrent by remember { mutableStateOf(true) }

    fun formatLocalDateTime(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(ms))
    }

    var manualDateInput by remember { mutableStateOf(formatLocalDateTime(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = SopScheduleParser.parseIsoMillis(manualDateInput)
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { picked ->
                            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val datePart = sdfDate.format(Date(picked))
                            val timePart = manualDateInput.substringAfter(' ', "10:00").take(5)
                            manualDateInput = "$datePart $timePart"
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    val totalLogsCount = logs.size
    val sopChecksCount = remember(logs) { logs.count { it.isSopCheckEntry } }
    val statusUpdatesCount = remember(logs) {
        logs.count { it.hasStatusTransition || it.changes.containsKey("status") }
    }
    val faultsCount = remember(logs) {
        logs.sumOf { log -> log.items.count { it.s == "nok" } }
    }

    val statusTransitionHistory = remember(logs) {
        logs.filter { it.hasStatusTransition || it.changes.containsKey("status") || it.result.isNotBlank() }
    }

    val filteredLogs = remember(logs, selectedTab, selectedFreqFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        logs.filter { entry ->
            val tabMatch = when (selectedTab) {
                AuditLogFilterTab.ALL -> true
                AuditLogFilterTab.SOP_CHECKS -> entry.isSopCheckEntry
                AuditLogFilterTab.STATUS_UPDATES -> entry.hasStatusTransition || entry.changes.containsKey("status")
                AuditLogFilterTab.FAULTS ->
                    entry.result == "Needs attention" ||
                        entry.result == "Out of service" ||
                        entry.items.any { it.s == "nok" }
                AuditLogFilterTab.SCHEDULE_AND_EDITS -> !entry.isSopCheckEntry
            }
            val freqMatch = if (selectedFreqFilter == "ALL") {
                true
            } else {
                entry.freq.equals(selectedFreqFilter, ignoreCase = true)
            }
            val searchMatch = if (q.isEmpty()) {
                true
            } else {
                entry.by.lowercase().contains(q) ||
                    entry.notes.lowercase().contains(q) ||
                    entry.result.lowercase().contains(q) ||
                    entry.previousStatus.lowercase().contains(q) ||
                    entry.activityTitle.lowercase().contains(q) ||
                    SopScheduleParser.fmtDT(entry.at).lowercase().contains(q) ||
                    entry.items.any { it.t.lowercase().contains(q) } ||
                    entry.readings.any { it.t.lowercase().contains(q) || it.v.lowercase().contains(q) }
            }
            tabMatch && freqMatch && searchMatch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, akah.line),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 920.dp)
                .fillMaxHeight(0.92f)
                .testTag("asset_audit_log_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Audit Log",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "ASSET MAINTENANCE & STATUS AUDIT LOG",
                                        fontFamily = BarlowDisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.8.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "${asset.id} · ${asset.name}",
                                    fontFamily = BarlowDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = buildString {
                                        append(asset.system)
                                        if (asset.location.isNotBlank()) {
                                            append(" · ")
                                            append(asset.location)
                                        }
                                        append(" · Current Status: ")
                                        append(asset.status)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = akah.muted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusPill(status = asset.status)
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("close_audit_log_dialog_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Audit Log"
                                    )
                                }
                            }
                        }

                        // Action Bar: Log Past Activity, Export CSV/PDF, Open Full Sheet
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showAddActivityPanel = !showAddActivityPanel },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = CircleShape,
                                modifier = Modifier.testTag("toggle_log_manual_activity_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Log Maintenance or Status Activity",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (showAddActivityPanel) "Hide Activity Form" else "Log Past Activity / Status",
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { onExportAssetAudit("CSV") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = CircleShape,
                                modifier = Modifier.testTag("audit_export_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Export Asset Audit CSV",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Export CSV", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onExportAssetAudit("PDF") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = CircleShape,
                                modifier = Modifier.testTag("audit_export_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export Asset Audit PDF",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Export PDF", fontSize = 12.sp)
                            }

                            if (onOpenAssetSheet != null) {
                                TextButton(
                                    onClick = onOpenAssetSheet,
                                    modifier = Modifier.testTag("audit_open_asset_sheet_button")
                                ) {
                                    Text("Open SOP Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = akah.line)

                // Main Scrollable Audit Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("audit_log_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Collapsible Manual Past Activity / Status Update Form
                    if (showAddActivityPanel) {
                        item(key = "manual_audit_form") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_audit_activity_form")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "LOG PAST MAINTENANCE ACTIVITY OR STATUS CHANGE",
                                        fontFamily = BarlowDisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Activity Kind Selector
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val kinds = listOf(
                                            "check" to "SOP Maintenance Check",
                                            "details" to "Status / Corrective Update"
                                        )
                                        kinds.forEach { (code, label) ->
                                            val sel = manualKind == code
                                            Surface(
                                                onClick = { manualKind = code },
                                                shape = CircleShape,
                                                color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                                border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else akah.line),
                                                modifier = Modifier.testTag("manual_kind_$code")
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Frequency selector when SOP Check is chosen
                                    if (manualKind == "check") {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = "Frequency:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = akah.muted
                                            )
                                            listOf("D", "W", "M", "Q", "H", "A").forEach { f ->
                                                val sel = manualFreq == f
                                                Surface(
                                                    onClick = { manualFreq = f },
                                                    shape = CircleShape,
                                                    color = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                    border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else akah.line)
                                                ) {
                                                    Text(
                                                        text = "${SopScheduleParser.freqLabel(f)} ($f)",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Activity Date & Time + Quick Presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = manualDateInput,
                                            onValueChange = { manualDateInput = it },
                                            label = { Text("Activity Date & Time (YYYY-MM-DD HH:mm)") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("manual_audit_date_input")
                                        )
                                        OutlinedButton(
                                            onClick = { showDatePicker = true },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Pick Activity Date",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        val now = System.currentTimeMillis()
                                        val presets = listOf(
                                            "Now" to now,
                                            "Yesterday" to (now - 86_400_000L),
                                            "3 Days Ago" to (now - 3 * 86_400_000L),
                                            "1 Week Ago" to (now - 7 * 86_400_000L),
                                            "1 Month Ago" to (now - 30 * 86_400_000L)
                                        )
                                        presets.forEach { (label, ms) ->
                                            Surface(
                                                onClick = { manualDateInput = formatLocalDateTime(ms) },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.background,
                                                border = BorderStroke(1.dp, akah.line)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Previous Status -> New Status Transition Selectors
                                    Text(
                                        text = "STATUS TRANSITION (PREVIOUS STATUS → UPDATED STATUS)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = akah.muted
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Previous Status", fontSize = 11.sp, color = akah.muted)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                SopConstants.STATUS_OPTIONS.forEach { st ->
                                                    val sel = manualPrevStatus == st
                                                    Surface(
                                                        onClick = { manualPrevStatus = st },
                                                        shape = CircleShape,
                                                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else akah.line),
                                                        modifier = Modifier.testTag("manual_prev_status_${st.lowercase().replace(' ', '_')}")
                                                    ) {
                                                        Text(
                                                            text = st,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Updated Status", fontSize = 11.sp, color = akah.muted)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                SopConstants.STATUS_OPTIONS.forEach { st ->
                                                    val sel = manualNewStatus == st
                                                    Surface(
                                                        onClick = { manualNewStatus = st },
                                                        shape = CircleShape,
                                                        color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else akah.line),
                                                        modifier = Modifier.testTag("manual_new_status_${st.lowercase().replace(' ', '_')}")
                                                    ) {
                                                        Text(
                                                            text = st,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = manualBy,
                                            onValueChange = { manualBy = it },
                                            label = { Text("Technician / Operator / AMC Vendor") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("manual_audit_by_input")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = manualNotes,
                                        onValueChange = { manualNotes = it },
                                        label = { Text("Maintenance Activity Summary, Parts Replaced & Audit Notes") },
                                        minLines = 2,
                                        maxLines = 4,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("manual_audit_notes_input")
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { manualUpdateCurrent = !manualUpdateCurrent }
                                    ) {
                                        Checkbox(
                                            checked = manualUpdateCurrent,
                                            onCheckedChange = { manualUpdateCurrent = it }
                                        )
                                        Text(
                                            text = "Also update asset's current status to \"$manualNewStatus\"",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val parsedMs = SopScheduleParser.parseIsoMillis(manualDateInput)
                                                    ?: System.currentTimeMillis()
                                                val iso = SopScheduleParser.isoFromMillis(parsedMs)
                                                onRecordManualAuditEntry(
                                                    iso,
                                                    manualBy,
                                                    manualKind,
                                                    manualFreq,
                                                    manualPrevStatus,
                                                    manualNewStatus,
                                                    manualNotes.ifBlank {
                                                        if (manualKind == "check") {
                                                            "${SopScheduleParser.freqLabel(manualFreq)} maintenance inspection completed"
                                                        } else {
                                                            "Status updated from $manualPrevStatus to $manualNewStatus"
                                                        }
                                                    },
                                                    manualUpdateCurrent
                                                )
                                                manualNotes = ""
                                                showAddActivityPanel = false
                                            },
                                            enabled = !isSaving,
                                            modifier = Modifier.testTag("save_manual_audit_entry_button")
                                        ) {
                                            Text(if (isSaving) "Saving…" else "Save Audit Log Entry")
                                        }
                                        OutlinedButton(onClick = { showAddActivityPanel = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Audit KPI Summary Strip
                    item(key = "audit_kpi_strip") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuditKpiBox(
                                label = "Total Audit Logs",
                                value = totalLogsCount.toString(),
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            AuditKpiBox(
                                label = "SOP Checks",
                                value = sopChecksCount.toString(),
                                accentColor = akah.ok
                            )
                            AuditKpiBox(
                                label = "Status Updates",
                                value = statusUpdatesCount.toString(),
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                            AuditKpiBox(
                                label = "Faults Recorded",
                                value = faultsCount.toString(),
                                accentColor = if (faultsCount > 0) akah.crit else akah.muted
                            )
                        }
                    }

                    // 3. Previous Status Updates Summary Card
                    if (statusTransitionHistory.isNotEmpty()) {
                        item(key = "status_transition_chain") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, akah.line),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("previous_status_updates_card")
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
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Previous Status Updates",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "PREVIOUS STATUS UPDATES & OPERATIONAL STATE TRAIL",
                                            fontFamily = BarlowDisplayFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.6.sp,
                                            color = akah.muted
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        statusTransitionHistory.take(8).forEach { entry ->
                                            val prev = entry.previousStatus
                                            val next = entry.updatedStatus.ifBlank { asset.status }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.background,
                                                border = BorderStroke(1.dp, akah.line)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = SopScheduleParser.fmtDT(entry.at),
                                                        fontFamily = IbmPlexMonoFontFamily,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (prev.isNotBlank()) {
                                                            StatusPill(status = prev)
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                                contentDescription = "changed to",
                                                                tint = akah.muted,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                        StatusPill(status = next)
                                                    }
                                                    Text(
                                                        text = "${entry.activityTitle} · ${entry.by.ifBlank { "Operator" }}",
                                                        fontSize = 11.sp,
                                                        color = akah.muted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Search & Filter Bar for Audit Logs
                    item(key = "audit_filter_controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search past maintenance activities, dates, statuses, or operators…",
                                        fontSize = 13.sp,
                                        color = akah.muted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search audit logs",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear audit search"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("audit_log_search_input")
                            )

                            // Filter Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AuditLogFilterTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    Surface(
                                        onClick = { selectedTab = tab },
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else akah.line
                                        ),
                                        modifier = Modifier.testTag("audit_filter_tab_${tab.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = tab.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Frequency Sub-Filter Pills
                            if (selectedTab == AuditLogFilterTab.ALL || selectedTab == AuditLogFilterTab.SOP_CHECKS) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SOP Freq:",
                                        fontSize = 11.sp,
                                        color = akah.muted
                                    )
                                    val freqs = listOf("ALL", "D", "W", "M", "Q", "H", "A")
                                    freqs.forEach { code ->
                                        val selected = selectedFreqFilter == code
                                        Surface(
                                            onClick = { selectedFreqFilter = code },
                                            shape = CircleShape,
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary else akah.line
                                            ),
                                            modifier = Modifier.testTag("audit_freq_filter_${code.lowercase()}")
                                        ) {
                                            Text(
                                                text = if (code == "ALL") "All Frequencies" else "${SopScheduleParser.freqLabel(code)} ($code)",
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) MaterialTheme.colorScheme.primary else akah.muted,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Filtered Audit Entries List
                    if (filteredLogs.isEmpty()) {
                        item(key = "empty_audit_state") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, akah.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = akah.muted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = if (logs.isEmpty()) {
                                            "No maintenance activities or status updates recorded yet for ${asset.id}."
                                        } else {
                                            "No log entries match your current filter or search."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = akah.muted
                                    )
                                    if (logs.isEmpty()) {
                                        OutlinedButton(onClick = { showAddActivityPanel = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Log First Maintenance Activity")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredLogs,
                            key = { idx, item -> "${item.logId}_${item.at}_$idx" }
                        ) { index, entry ->
                            DetailedAuditLogEntryCard(
                                entry = entry,
                                entryNumber = filteredLogs.size - index
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditKpiBox(
    label: String,
    value: String,
    accentColor: Color
) {
    val akah = LocalAkahColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, akah.line),
        modifier = Modifier.widthIn(min = 135.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = akah.muted
            )
            Text(
                text = value,
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = accentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedAuditLogEntryCard(
    entry: MaintenanceLogEntity,
    entryNumber: Int? = null,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    val items = entry.items
    val readings = entry.readings
    val okCount = items.count { it.s == "ok" }
    val faultItems = items.filter { it.s == "nok" }
    val naCount = items.count { it.s == "na" }

    val accentColor = when {
        entry.isDueDateEntry -> MaterialTheme.colorScheme.primary
        entry.updatedStatus == "Out of service" || faultItems.isNotEmpty() -> akah.crit
        entry.updatedStatus == "Needs attention" -> akah.warn
        entry.kind == "details" -> MaterialTheme.colorScheme.primary
        else -> akah.ok
    }

    var checklistExpanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, akah.line),
        modifier = modifier
            .fillMaxWidth()
            .testTag("audit_log_entry_card_${entry.logId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Activity Title Badge + Sync Badge + Entry Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = entry.activityTitle,
                        fontFamily = BarlowDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (entryNumber != null) {
                        Text(
                            text = "#$entryNumber",
                            fontFamily = IbmPlexMonoFontFamily,
                            fontSize = 11.sp,
                            color = akah.muted
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (entry.syncedToServer) akah.okSoft else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (entry.syncedToServer) "Synced" else "Offline Room DB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (entry.syncedToServer) akah.ok else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Date/Time & Operator Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Activity Date",
                        tint = akah.muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${SopScheduleParser.fmtDT(entry.at)} (${SopScheduleParser.ago(entry.at)})",
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Operator",
                        tint = akah.muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = entry.by.ifBlank { "Duty Operator" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Status Update / Verification Banner (Previous Status -> New Status)
            val prevStatus = entry.previousStatus
            val nextStatus = entry.updatedStatus
            if (prevStatus.isNotBlank() || nextStatus.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, akah.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (entry.hasStatusTransition) {
                                "STATUS TRANSITION"
                            } else {
                                "MAINTENANCE STATUS"
                            },
                            fontFamily = BarlowDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            color = if (entry.hasStatusTransition) MaterialTheme.colorScheme.primary else akah.muted
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (prevStatus.isNotBlank()) {
                                Text(
                                    text = "Prev:",
                                    fontSize = 11.sp,
                                    color = akah.muted
                                )
                                StatusPill(status = prevStatus)
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "updated to",
                                    tint = akah.muted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (nextStatus.isNotBlank()) {
                                StatusPill(status = nextStatus)
                            }
                        }
                    }
                }
            }

            // SOP Checklist Items Breakdown
            if (items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checklistExpanded = !checklistExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SOP Tasks Inspected: $okCount OK · ${faultItems.size} Fault · $naCount N/A",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (checklistExpanded) "Hide items" else "Show items",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = checklistExpanded) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items.forEach { item ->
                                val isFault = item.s == "nok"
                                val isOk = item.s == "ok"
                                val chipBg = when {
                                    isFault -> akah.critSoft
                                    isOk -> akah.okSoft
                                    else -> MaterialTheme.colorScheme.background
                                }
                                val chipFg = when {
                                    isFault -> akah.crit
                                    isOk -> akah.ok
                                    else -> akah.muted
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = chipBg,
                                    border = BorderStroke(1.dp, chipFg.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFault) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = chipFg,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${item.t}: ${item.s.uppercase()}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isFault) FontWeight.Bold else FontWeight.Medium,
                                            color = chipFg
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recorded Telemetry / Readings
            if (readings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RECORDED READINGS & TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = akah.muted
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        readings.forEach { r ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, akah.line)
                            ) {
                                Text(
                                    text = "${r.t}: ${r.v}",
                                    fontFamily = IbmPlexMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Non-status Field Changes Diff Table
            val otherChanges = entry.changes.filterKeys { it != "status" }
            if (otherChanges.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, akah.line, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "FIELD CHANGES RECORDED",
                        fontFamily = BarlowDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = akah.muted
                    )
                    otherChanges.forEach { (key, pair) ->
                        val label = SopConstants.DETAIL_FIELD_LABELS[key] ?: key
                        val oldVal = pair.getOrNull(0)?.ifBlank { "—" } ?: "—"
                        val newVal = pair.getOrNull(1)?.ifBlank { "—" } ?: "—"
                        Text(
                            text = "$label: $oldVal → $newVal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Notes / Audit Remarks
            if (entry.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${entry.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
