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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditAttributes
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiSopService
import com.example.model.AssetEntity
import com.example.model.ChecklistItemMark
import com.example.model.DeadlineStatus
import com.example.model.MaintenanceLogEntity
import com.example.model.ReadingValue
import com.example.model.SopConstants
import com.example.model.SopScheduleParser
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.util.QrCodeUtils
import com.example.viewmodel.AiCardState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssetDetailScreen(
    asset: AssetEntity,
    logs: List<MaintenanceLogEntity>,
    meName: String,
    qrPayloadUrl: String,
    flashMessage: String?,
    isFlashError: Boolean,
    isSaving: Boolean,
    assetAiState: AiCardState,
    onBack: () -> Unit,
    onOpenWhoDialog: () -> Unit,
    onOpenQuickStatus: () -> Unit,
    onOpenDueDateDialog: () -> Unit = {},
    onOpenAuditLogView: () -> Unit = {},
    onSaveCheck: (
        freqCode: String,
        resultStatus: String,
        marks: List<ChecklistItemMark>,
        readings: List<ReadingValue>,
        notes: String,
        onComplete: () -> Unit
    ) -> Unit,
    onSaveDetails: (
        status: String,
        criticality: String,
        location: String,
        make: String,
        model: String,
        serial: String,
        capacity: String,
        installed: String,
        amcVendor: String,
        amcExpiry: String,
        owner: String,
        notes: String,
        onComplete: () -> Unit
    ) -> Unit,
    onAssetAiQueryChange: (String) -> Unit,
    onToggleAssetAiHighThinking: (Boolean) -> Unit,
    onAskAssetAi: (String) -> Unit,
    isTwoPane: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isTwoPane, onBack = onBack)

    val akah = LocalAkahColors.current
    val critColor = CriticalityColor(asset.criticality)
    val dues = remember(asset) { SopScheduleParser.dueState(asset) }
    val deadlineEval = remember(asset) { SopScheduleParser.evaluateAssetDeadline(asset) }
    val checklistGroups = remember(asset) { SopScheduleParser.checklist(asset) }
    val readingLabels = remember(asset) { SopScheduleParser.readingsOf(asset) }

    // Record a check draft state
    var selectedFreqCode by remember(asset.id) {
        mutableStateOf(checklistGroups.firstOrNull()?.code ?: "D")
    }
    val itemMarks = remember(asset.id, selectedFreqCode) { mutableStateMapOf<Int, String>() }
    val readingValues = remember(asset.id, selectedFreqCode) { mutableStateMapOf<Int, String>() }
    var checkNotes by remember(asset.id, selectedFreqCode) { mutableStateOf("") }
    var outOfServiceChecked by remember(asset.id, selectedFreqCode) { mutableStateOf(false) }

    // Details edit mode state
    var isEditingDetails by remember(asset.id) { mutableStateOf(false) }
    var standardExpanded by remember(asset.id) { mutableStateOf(false) }

    // QR Code Bitmap
    var qrBitmap by remember(qrPayloadUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(qrPayloadUrl) {
        qrBitmap = QrCodeUtils.generateQrImageBitmap(qrPayloadUrl, 420)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("asset_detail_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Back Button Row
        item(key = "back_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("back_to_register_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isTwoPane) "Close inspector" else "All assets",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTwoPane) "Close inspector" else "All assets",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    OutlinedButton(
                        onClick = onOpenAuditLogView,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("detail_top_audit_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Open Detailed Audit Log",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Audit Log (${logs.size})", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenDueDateDialog,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("detail_set_due_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Set Maintenance Due Date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Set Due Date", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenQuickStatus,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("detail_quick_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditAttributes,
                            contentDescription = "Update Status",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Status Update", fontSize = 12.sp)
                    }
                }
            }
        }

        // Flash Banner
        if (!flashMessage.isNullOrBlank()) {
            item(key = "detail_flash") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFlashError) akah.critSoft else akah.okSoft,
                    border = BorderStroke(1.dp, if (isFlashError) akah.crit else akah.ok),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = flashMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFlashError) akah.crit else akah.ok,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // 1. Asset Nameplate + Due Schedule Grid
        item(key = "asset_plate") {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .background(critColor)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = asset.id,
                                    fontFamily = IbmPlexMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = akah.muted
                                )
                                Text(
                                    text = asset.name,
                                    fontFamily = BarlowDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = buildString {
                                        append(asset.criticality.uppercase())
                                        append(" · ")
                                        append(asset.system)
                                        if (asset.location.isNotBlank()) {
                                            append(" · ")
                                            append(asset.location)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = akah.muted,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(
                                status = asset.status,
                                modifier = Modifier.clickable { onOpenQuickStatus() }
                            )
                        }
                    }

                    if (asset.nextDueDateIso.isNotBlank() && deadlineEval.detailedText.isNotBlank()) {
                        HorizontalDivider(color = akah.line)
                        val bannerBg = when (deadlineEval.status) {
                            DeadlineStatus.OVERDUE -> akah.critSoft
                            DeadlineStatus.APPROACHING -> akah.warnSoft
                            else -> akah.okSoft
                        }
                        val bannerFg = when (deadlineEval.status) {
                            DeadlineStatus.OVERDUE -> akah.crit
                            DeadlineStatus.APPROACHING -> akah.warn
                            else -> akah.ok
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bannerBg.copy(alpha = 0.6f))
                                .clickable { onOpenDueDateDialog() }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .testTag("detail_scheduled_due_banner"),
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
                                    contentDescription = null,
                                    tint = bannerFg,
                                    modifier = Modifier.size(17.dp)
                                )
                                Column {
                                    Text(
                                        text = deadlineEval.detailedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = bannerFg
                                    )
                                    if (asset.nextDueTaskNote.isNotBlank()) {
                                        Text(
                                            text = "Task: ${asset.nextDueTaskNote}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Edit Due Date",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (dues.isNotEmpty()) {
                        HorizontalDivider(color = akah.line)
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dues.forEach { d ->
                                val toneColor = when {
                                    d.isLate -> akah.crit
                                    d.isApproaching -> akah.warn
                                    else -> akah.ok
                                }
                                val bg = when {
                                    d.isLate -> akah.critSoft.copy(alpha = 0.45f)
                                    d.isApproaching -> akah.warnSoft.copy(alpha = 0.55f)
                                    else -> akah.okSoft.copy(alpha = 0.45f)
                                }
                                Surface(
                                    onClick = onOpenDueDateDialog,
                                    shape = RoundedCornerShape(8.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, akah.line)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = SopScheduleParser.freqLabel(d.code).uppercase(),
                                            fontFamily = BarlowDisplayFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp,
                                            color = toneColor
                                        )
                                        Text(
                                            text = buildString {
                                                if (d.lastIso != null) {
                                                    append("Last ${SopScheduleParser.fmtD(d.lastIso)}")
                                                } else {
                                                    append("Not yet recorded")
                                                }
                                                if (!d.dueIso.isNullOrBlank()) {
                                                    append(" · Due ${SopScheduleParser.fmtD(d.dueIso)}")
                                                } else if (d.isLate) {
                                                    append(" · due")
                                                }
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Record a Check Card
        item(key = "record_check_card") {
            val activeGroup = checklistGroups.find { it.code == selectedFreqCode } ?: checklistGroups.firstOrNull()
            val tasks = activeGroup?.items.orEmpty()
            val faultCount = itemMarks.values.count { it == "nok" }
            val markedCount = itemMarks.values.count { it.isNotEmpty() }
            val computedResult = when {
                outOfServiceChecked -> "Out of service"
                faultCount > 0 -> "Needs attention"
                else -> "In service"
            }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECORD A CHECK",
                            style = MaterialTheme.typography.titleSmall,
                            color = akah.muted
                        )

                        if (tasks.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    tasks.indices.forEach { idx ->
                                        itemMarks[idx] = "ok"
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("mark_all_ok_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Mark all OK",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark all OK", fontSize = 12.sp)
                            }
                        }
                    }

                    // Frequency selector chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        checklistGroups.forEach { grp ->
                            val isSelected = grp.code == selectedFreqCode
                            Surface(
                                onClick = { selectedFreqCode = grp.code },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                                ),
                                modifier = Modifier.testTag("freq_chip_${grp.code}")
                            ) {
                                Text(
                                    text = SopScheduleParser.freqLabel(grp.code),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else akah.muted,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Checklist items grouped by Operator / Technician
                    var lastWho: String? = null
                    tasks.forEachIndexed { idx, task ->
                        if (task.who != lastWho) {
                            Text(
                                text = if (task.who == "Operator") "OPERATOR CHECKS" else "TECHNICIAN / AMC TASKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = akah.muted,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            lastWho = task.who
                        } else {
                            HorizontalDivider(color = akah.line.copy(alpha = 0.6f))
                        }

                        val currentMark = itemMarks[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            SegmentedCheckButtons(
                                selected = currentMark,
                                onSelect = { value ->
                                    if (currentMark == value) {
                                        itemMarks.remove(idx)
                                    } else {
                                        itemMarks[idx] = value
                                    }
                                },
                                itemIndex = idx
                            )
                        }
                    }

                    // Readings Section
                    if (readingLabels.isNotEmpty()) {
                        Text(
                            text = "READINGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = akah.muted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            readingLabels.forEachIndexed { rIdx, label ->
                                OutlinedTextField(
                                    value = readingValues[rIdx] ?: "",
                                    onValueChange = { readingValues[rIdx] = it },
                                    label = { Text(label, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reading_input_$rIdx")
                                )
                            }
                        }
                    }

                    // Limits Alert Box
                    if (asset.limits.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = akah.critSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Limits: ${asset.limits}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Notes input
                    OutlinedTextField(
                        value = checkNotes,
                        onValueChange = { checkNotes = it },
                        label = { Text("Notes, defects found, parts used") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_notes_input")
                    )

                    // Out of service checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { outOfServiceChecked = !outOfServiceChecked },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = outOfServiceChecked,
                            onCheckedChange = { outOfServiceChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = akah.crit),
                            modifier = Modifier.testTag("out_of_service_checkbox")
                        )
                        Text(
                            text = "Asset is out of service",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Save Check button & live status preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (meName.isBlank()) {
                                    onOpenWhoDialog()
                                    return@Button
                                }
                                val marksList = tasks.mapIndexedNotNull { i, t ->
                                    val m = itemMarks[i]
                                    if (m.isNullOrBlank()) null else ChecklistItemMark(t = t.text, s = m)
                                }
                                val readingsList = readingLabels.mapIndexedNotNull { i, lbl ->
                                    val v = readingValues[i]?.trim()
                                    if (v.isNullOrBlank()) null else ReadingValue(t = lbl, v = v)
                                }
                                onSaveCheck(
                                    selectedFreqCode,
                                    computedResult,
                                    marksList,
                                    readingsList,
                                    checkNotes
                                ) {
                                    itemMarks.clear()
                                    readingValues.clear()
                                    checkNotes = ""
                                    outOfServiceChecked = false
                                }
                            },
                            enabled = !isSaving && markedCount > 0,
                            modifier = Modifier.testTag("save_check_button")
                        ) {
                            Text(if (isSaving) "Saving…" else "Save check")
                        }

                        Text(
                            text = "$markedCount of ${tasks.size} items marked · result: $computedResult",
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Asset Details Card (View & Edit)
        item(key = "asset_details_card") {
            AssetDetailsCard(
                asset = asset,
                isEditing = isEditingDetails,
                isSaving = isSaving,
                onStartEdit = {
                    if (meName.isBlank()) onOpenWhoDialog() else isEditingDetails = true
                },
                onCancelEdit = { isEditingDetails = false },
                onSaveEdit = { status, crit, loc, make, model, serial, cap, inst, vendor, exp, owner, notes ->
                    onSaveDetails(status, crit, loc, make, model, serial, cap, inst, vendor, exp, owner, notes) {
                        isEditingDetails = false
                    }
                }
            )
        }

        // 4. Ask AI about this asset (gemini-3.1-pro-preview + ThinkingLevel.HIGH)
        item(key = "ask_ai_asset_card") {
            AskAiMaintenanceCard(
                title = "Ask AI about this asset",
                presetPrompts = GeminiSopService.AI_ASSET_PROMPTS,
                aiState = assetAiState,
                onQueryChange = onAssetAiQueryChange,
                onToggleHighThinking = onToggleAssetAiHighThinking,
                onAskQuestion = onAskAssetAi
            )
        }

        // 5. Maintenance Standard Accordion (SOP AKAH-EH-AM-A1-2026, Appendix A)
        item(key = "maintenance_standard_card") {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { standardExpanded = !standardExpanded }
                            .testTag("maintenance_standard_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Maintenance standard (SOP ${SopConstants.SOP_CODE}, Appendix A)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (standardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Maintenance Standard"
                        )
                    }

                    AnimatedVisibility(visible = standardExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StandardSection("STRATEGY", asset.strategy)
                            StandardSection("OPERATOR ROUTINE", asset.routine)
                            StandardSection("PREVENTIVE MAINTENANCE", asset.ppm)
                            StandardSection("STANDARDS & STATUTORY", asset.statutory)
                            StandardSection("DATA TO RECORD", asset.records)
                            if (asset.limits.isNotBlank()) {
                                StandardSection("LIMITS", asset.limits)
                            }
                        }
                    }
                }
            }
        }

        // 6. Detailed Maintenance & Status Audit Log Card
        item(key = "history_card") {
            var inlineAuditFilter by remember(asset.id) { mutableStateOf("ALL") } // "ALL", "CHECKS", "STATUS"
            val inlineFilteredLogs = remember(logs, inlineAuditFilter) {
                when (inlineAuditFilter) {
                    "CHECKS" -> logs.filter { it.isSopCheckEntry }
                    "STATUS" -> logs.filter { it.hasStatusTransition || it.changes.containsKey("status") }
                    else -> logs
                }
            }
            val statusTransitions = remember(logs) {
                logs.filter { it.hasStatusTransition || it.changes.containsKey("status") }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("asset_detail_audit_log_section")
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
                                text = "DETAILED MAINTENANCE & STATUS AUDIT LOG",
                                style = MaterialTheme.typography.titleSmall,
                                color = akah.muted
                            )
                            Text(
                                text = "${logs.size} past activities · ${logs.count { it.isSopCheckEntry }} SOP checks · ${statusTransitions.size} status updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = onOpenAuditLogView,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("detail_open_audit_log_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Open Full Audit Log View",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Full Log View", fontSize = 12.sp)
                        }
                    }

                    // Inline Filter Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val filterOptions = listOf(
                            "ALL" to "All Activities (${logs.size})",
                            "CHECKS" to "Past SOP Checks (${logs.count { it.isSopCheckEntry }})",
                            "STATUS" to "Status Updates (${statusTransitions.size})"
                        )
                        filterOptions.forEach { (code, label) ->
                            val selected = inlineAuditFilter == code
                            Surface(
                                onClick = { inlineAuditFilter = code },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else akah.line),
                                modifier = Modifier.testTag("inline_audit_filter_${code.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Previous Status Updates Strip
                    if (statusTransitions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, akah.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PREVIOUS STATUS UPDATES TIMELINE",
                                    fontFamily = BarlowDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.6.sp,
                                    color = akah.muted
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    statusTransitions.take(6).forEach { stLog ->
                                        val prev = stLog.previousStatus
                                        val next = stLog.updatedStatus.ifBlank { asset.status }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, akah.line)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = SopScheduleParser.fmtDT(stLog.at),
                                                    fontFamily = IbmPlexMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    color = akah.muted
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (prev.isNotBlank()) {
                                                        StatusPill(status = prev)
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                            contentDescription = "to",
                                                            tint = akah.muted,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    StatusPill(status = next)
                                                }
                                                Text(
                                                    text = stLog.by.ifBlank { "Duty Operator" },
                                                    fontSize = 10.sp,
                                                    color = akah.muted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (inlineFilteredLogs.isEmpty()) {
                        Text(
                            text = "No checks or status updates recorded in this view yet. Saved checks and status changes appear here, newest first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted
                        )
                    } else {
                        inlineFilteredLogs.forEachIndexed { idx, entry ->
                            DetailedAuditLogEntryCard(
                                entry = entry,
                                entryNumber = inlineFilteredLogs.size - idx
                            )
                        }
                    }
                }
            }
        }

        // 7. QR Label Card
        item(key = "qr_label_card") {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, akah.line),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "QR LABEL",
                        style = MaterialTheme.typography.titleSmall,
                        color = akah.muted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, akah.line, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            qrBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "QR Code for ${asset.id}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = asset.id,
                                fontFamily = IbmPlexMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = qrPayloadUrl,
                                fontFamily = IbmPlexMonoFontFamily,
                                fontSize = 11.sp,
                                color = akah.muted
                            )
                            Text(
                                text = "Scanning this code opens this asset sheet immediately. View all 48 printable labels from “QR labels” on the register.",
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedCheckButtons(
    selected: String?,
    onSelect: (String) -> Unit,
    itemIndex: Int
) {
    val akah = LocalAkahColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, akah.line, RoundedCornerShape(8.dp))
    ) {
        val options = listOf(
            Triple("ok", "OK", akah.ok),
            Triple("nok", "Fault", akah.crit),
            Triple("na", "N/A", akah.off)
        )
        options.forEachIndexed { idx, (code, label, activeBg) ->
            val isPressed = selected == code
            Box(
                modifier = Modifier
                    .background(if (isPressed) activeBg else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(code) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("check_seg_${itemIndex}_$code"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPressed) Color.White else akah.muted
                )
            }
            if (idx < options.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(akah.line)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetDetailsCard(
    asset: AssetEntity,
    isEditing: Boolean,
    isSaving: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (
        status: String,
        criticality: String,
        location: String,
        make: String,
        model: String,
        serial: String,
        capacity: String,
        installed: String,
        amcVendor: String,
        amcExpiry: String,
        owner: String,
        notes: String
    ) -> Unit
) {
    val akah = LocalAkahColors.current
    var status by remember(asset, isEditing) { mutableStateOf(asset.status) }
    var criticality by remember(asset, isEditing) { mutableStateOf(asset.criticality) }
    var location by remember(asset, isEditing) { mutableStateOf(asset.location) }
    var make by remember(asset, isEditing) { mutableStateOf(asset.make) }
    var model by remember(asset, isEditing) { mutableStateOf(asset.model) }
    var serial by remember(asset, isEditing) { mutableStateOf(asset.serial) }
    var capacity by remember(asset, isEditing) { mutableStateOf(asset.capacity) }
    var installed by remember(asset, isEditing) { mutableStateOf(asset.installed) }
    var amcVendor by remember(asset, isEditing) { mutableStateOf(asset.amcVendor) }
    var amcExpiry by remember(asset, isEditing) { mutableStateOf(asset.amcExpiry) }
    var owner by remember(asset, isEditing) { mutableStateOf(asset.owner) }
    var notes by remember(asset, isEditing) { mutableStateOf(asset.notes) }

    var statusExpanded by remember { mutableStateOf(false) }
    var critExpanded by remember { mutableStateOf(false) }

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
                text = "ASSET DETAILS",
                style = MaterialTheme.typography.titleSmall,
                color = akah.muted
            )

            if (!isEditing) {
                val rows = listOf(
                    "Asset ID" to asset.id,
                    "Category" to asset.category,
                    "Criticality" to asset.criticality,
                    "Status" to asset.status,
                    "Location (building / floor / room)" to asset.location,
                    "Make" to asset.make,
                    "Model" to asset.model,
                    "Serial no." to asset.serial,
                    "Capacity / rating" to asset.capacity,
                    "Commissioned on" to asset.installed,
                    "AMC vendor & contract no." to asset.amcVendor,
                    "AMC expiry" to asset.amcExpiry,
                    "Responsible person" to asset.owner,
                    "Notes" to asset.notes
                )
                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted,
                            modifier = Modifier.weight(0.42f)
                        )
                        Text(
                            text = value.ifBlank { "Not recorded" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (value.isBlank()) akah.muted else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.58f)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onStartEdit,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("edit_details_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit details",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit details")
                }
            } else {
                // Status dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        SopConstants.STATUS_OPTIONS.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    status = opt
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Criticality dropdown
                ExposedDropdownMenuBox(
                    expanded = critExpanded,
                    onExpandedChange = { critExpanded = !critExpanded }
                ) {
                    OutlinedTextField(
                        value = criticality,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Criticality") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = critExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = critExpanded,
                        onDismissRequest = { critExpanded = false }
                    ) {
                        SopConstants.CRITICALITY_OPTIONS.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    criticality = opt
                                    critExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (building / floor / room)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = make,
                        onValueChange = { make = it },
                        label = { Text("Make") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serial,
                        onValueChange = { serial = it },
                        label = { Text("Serial no.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacity / rating") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = installed,
                        onValueChange = { installed = it },
                        label = { Text("Commissioned on (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amcExpiry,
                        onValueChange = { amcExpiry = it },
                        label = { Text("AMC expiry (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = amcVendor,
                    onValueChange = { amcVendor = it },
                    label = { Text("AMC vendor & contract no.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Responsible person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveEdit(
                                status, criticality, location, make, model, serial,
                                capacity, installed, amcVendor, amcExpiry, owner, notes
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("save_details_button")
                    ) {
                        Text(if (isSaving) "Saving…" else "Save details")
                    }
                    OutlinedButton(onClick = onCancelEdit) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardSection(title: String, body: String) {
    val akah = LocalAkahColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            fontFamily = BarlowDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.6.sp,
            color = akah.muted
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HistoryTimelineItem(entry: MaintenanceLogEntity) {
    val akah = LocalAkahColors.current
    val dotColor = when {
        entry.kind == "details" -> MaterialTheme.colorScheme.primary
        entry.result == "Out of service" -> akah.crit
        entry.result == "Needs attention" -> akah.warn
        else -> akah.ok
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(12.dp)
                .clip(CircleShape)
                .border(2.5.dp, dotColor, CircleShape)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (entry.kind == "details") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Details updated",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (entry.result.isNotBlank()) {
                        StatusPill(status = entry.result)
                    }
                }
                Text(
                    text = "${SopScheduleParser.fmtDT(entry.at)} · ${entry.by.ifBlank { "Someone" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted
                )
                entry.changes.forEach { (key, pair) ->
                    val label = SopConstants.DETAIL_FIELD_LABELS[key] ?: key
                    val oldVal = pair.getOrNull(0)?.ifBlank { "—" } ?: "—"
                    val newVal = pair.getOrNull(1)?.ifBlank { "—" } ?: "—"
                    Text(
                        text = "$label: $oldVal → $newVal",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (entry.notes.isNotBlank()) {
                    Text(
                        text = "Note: ${entry.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = akah.muted
                    )
                }
            } else {
                val items = entry.items
                val okCnt = items.count { it.s == "ok" }
                val faultItems = items.filter { it.s == "nok" }
                val naCnt = items.count { it.s == "na" }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${SopScheduleParser.freqLabel(entry.freq)} check",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    StatusPill(status = entry.result)
                }
                Text(
                    text = "${SopScheduleParser.fmtDT(entry.at)} · ${entry.by.ifBlank { "Someone" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted
                )
                Text(
                    text = "$okCnt OK · ${faultItems.size} fault · $naCnt N/A",
                    style = MaterialTheme.typography.bodySmall
                )
                if (faultItems.isNotEmpty()) {
                    Text(
                        text = "Faults: ${faultItems.joinToString("; ") { it.t }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = akah.crit,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (entry.readings.isNotEmpty()) {
                    Text(
                        text = entry.readings.joinToString(" · ") { "${it.t}: ${it.v}" },
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (entry.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${entry.notes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
