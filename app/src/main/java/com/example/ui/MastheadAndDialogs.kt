package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AssetEntity
import com.example.model.SopConstants
import com.example.model.SopScheduleParser
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.viewmodel.AiCardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class LayoutViewportMode(val label: String) {
    AUTO("Auto"),
    DESKTOP("Desktop"),
    MOBILE("Mobile");

    fun next(): LayoutViewportMode = when (this) {
        AUTO -> DESKTOP
        DESKTOP -> MOBILE
        MOBILE -> AUTO
    }
}

@Composable
fun AkahMasthead(
    meName: String,
    layoutMode: LayoutViewportMode = LayoutViewportMode.AUTO,
    isEffectiveDesktop: Boolean = false,
    onToggleLayoutMode: () -> Unit = {},
    onOpenWhoDialog: () -> Unit,
    onImportCsv: () -> Unit = {},
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, akah.line, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_akah_crest),
                        contentDescription = "The Aga Khan Academies logo",
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AGA KHAN ACADEMY",
                        fontFamily = BarlowDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEffectiveDesktop) {
                            "AKAH_SOP_SERVER · DESKTOP SPLIT-PANE"
                        } else {
                            "AKAH_SOP_SERVER · MOBILE REGISTER"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = akah.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Desktop / Mobile / Auto Viewport Switcher
                Surface(
                    onClick = onToggleLayoutMode,
                    shape = CircleShape,
                    color = if (layoutMode != LayoutViewportMode.AUTO) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        if (layoutMode != LayoutViewportMode.AUTO) MaterialTheme.colorScheme.primary else akah.line
                    ),
                    modifier = Modifier.testTag("layout_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (layoutMode) {
                                LayoutViewportMode.AUTO -> Icons.Default.Devices
                                LayoutViewportMode.DESKTOP -> Icons.Default.DesktopWindows
                                LayoutViewportMode.MOBILE -> Icons.Default.Smartphone
                            },
                            contentDescription = "Switch Desktop/Mobile layout mode",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = layoutMode.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onImportCsv,
                    modifier = Modifier.testTag("import_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Bulk Import Assets from CSV",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onExportCsv,
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Assets.csv and Maintenance_History.csv",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                val isUnset = meName.isBlank()
                Surface(
                    onClick = onOpenWhoDialog,
                    shape = CircleShape,
                    color = if (isUnset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isUnset) MaterialTheme.colorScheme.primary else akah.line
                    ),
                    modifier = Modifier.testTag("who_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Recorder Name",
                            modifier = Modifier.size(15.dp),
                            tint = if (isUnset) MaterialTheme.colorScheme.primary else akah.muted
                        )
                        Text(
                            text = if (isUnset) "Set name" else meName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isUnset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = akah.line, thickness = 1.dp)
        }
    }
}

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    val (bg, fg) = when (status) {
        "In service" -> akah.okSoft to akah.ok
        "Needs attention" -> akah.warnSoft to akah.warn
        "Out of service" -> akah.critSoft to akah.crit
        else -> akah.offSoft to akah.off
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun CriticalityColor(criticality: String): Color {
    val akah = LocalAkahColors.current
    return when (criticality) {
        "Critical" -> akah.crit
        "Medium" -> akah.warn
        else -> akah.ok
    }
}

@Composable
fun WhoIsRecordingDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Who is recording?",
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your name is saved with every check and edit you record from this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAkahColors.current.muted
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it.take(60)
                        errorMsg = ""
                    },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("who_name_input")
                )
                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.trim().isEmpty()) {
                        errorMsg = "Enter your name."
                    } else {
                        onSave(nameInput.trim())
                    }
                },
                modifier = Modifier.testTag("who_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickStatusUpdateDialog(
    asset: AssetEntity,
    recorderName: String,
    onDismiss: () -> Unit,
    onConfirmUpdate: (newStatus: String, newLocation: String, note: String) -> Unit,
    onOpenFullSheet: () -> Unit
) {
    val akah = LocalAkahColors.current
    var selectedStatus by remember(asset.status) { mutableStateOf(asset.status) }
    var locationInput by remember(asset.location) { mutableStateOf(asset.location) }
    var noteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = asset.id,
                    fontFamily = IbmPlexMonoFontFamily,
                    fontSize = 12.sp,
                    color = akah.muted
                )
                Text(
                    text = asset.name,
                    fontFamily = BarlowDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Update offline maintenance status & location in Room DB (recorded by ${recorderName.ifBlank { "Duty Operator" }}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted
                )
                SopConstants.STATUS_OPTIONS.forEach { st ->
                    val isSelected = selectedStatus == st
                    val (bg, fg) = when (st) {
                        "In service" -> akah.okSoft to akah.ok
                        "Needs attention" -> akah.warnSoft to akah.warn
                        "Out of service" -> akah.critSoft to akah.crit
                        else -> akah.offSoft to akah.off
                    }
                    val icon = when (st) {
                        "In service" -> Icons.Default.CheckCircle
                        "Needs attention" -> Icons.Default.Warning
                        "Out of service" -> Icons.Default.Error
                        else -> Icons.Default.RemoveCircleOutline
                    }
                    Surface(
                        onClick = { selectedStatus = st },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) bg else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) fg else akah.line
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_status_option_$st")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = st,
                                tint = fg,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = st,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) fg else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location (building / floor / room)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_status_location_input")
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Status note / observation (optional)") },
                    placeholder = { Text("e.g., Replaced blown fuse, silica gel regenerated...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_status_note_input")
                )

                OutlinedButton(
                    onClick = onOpenFullSheet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Full SOP Checklist & Sheet")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmUpdate(selectedStatus, locationInput, noteInput) },
                modifier = Modifier.testTag("quick_status_save_button")
            ) {
                Text("Save Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetOfflineDialog(
    suggestAssetId: (String) -> String,
    onDismiss: () -> Unit,
    onAddAsset: (
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
        routine: String,
        ppm: String,
        openAfterSave: Boolean,
        onError: (String) -> Unit
    ) -> Unit
) {
    var systemInput by remember { mutableStateOf(SopConstants.SYSTEMS.first()) }
    var idInput by remember { mutableStateOf(suggestAssetId(SopConstants.SYSTEMS.first())) }
    var nameInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var criticalityInput by remember { mutableStateOf("Medium") }
    var statusInput by remember { mutableStateOf("In service") }
    var makeInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    var capacityInput by remember { mutableStateOf("") }
    var ownerInput by remember { mutableStateOf("") }
    var routineInput by remember {
        mutableStateOf("D: Visual inspection, indicator status, abnormal noise/heat check | W: Cleaning, terminal/connection check, functional test | M: Full operational parameter log and safety interlock verification")
    }
    var ppmInput by remember {
        mutableStateOf("Q: Comprehensive preventive maintenance & calibration | A: Annual overhaul, statutory testing & certification")
    }
    var notesInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    var sysExpanded by remember { mutableStateOf(false) }
    var critExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Asset to Offline Room DB",
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // System dropdown (auto-updates suggested Asset ID)
                ExposedDropdownMenuBox(
                    expanded = sysExpanded,
                    onExpandedChange = { sysExpanded = !sysExpanded }
                ) {
                    OutlinedTextField(
                        value = systemInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("System Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sysExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sysExpanded,
                        onDismissRequest = { sysExpanded = false }
                    ) {
                        SopConstants.SYSTEMS.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    systemInput = opt
                                    idInput = suggestAssetId(opt)
                                    sysExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = idInput,
                    onValueChange = {
                        idInput = it.uppercase()
                        errorMsg = ""
                    },
                    label = { Text("Asset ID (AKA-HYD-XXX-000)") },
                    trailingIcon = {
                        TextButton(
                            onClick = { idInput = suggestAssetId(systemInput) }
                        ) {
                            Text("Auto ID", fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_asset_id_input")
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        errorMsg = ""
                    },
                    label = { Text("Asset Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_asset_name_input")
                )
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location (building / floor / room)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_asset_location_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = statusInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Maintenance Status") },
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
                                        statusInput = opt
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = critExpanded,
                        onExpandedChange = { critExpanded = !critExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = criticalityInput,
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
                                        criticalityInput = opt
                                        critExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = makeInput,
                        onValueChange = { makeInput = it },
                        label = { Text("Make") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Model") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = capacityInput,
                        onValueChange = { capacityInput = it },
                        label = { Text("Capacity / Rating") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ownerInput,
                        onValueChange = { ownerInput = it },
                        label = { Text("Responsible Owner") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = routineInput,
                    onValueChange = { routineInput = it },
                    label = { Text("Routine Checks (D: / W: / M:)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ppmInput,
                    onValueChange = { ppmInput = it },
                    label = { Text("PPM Schedule (Q: / SA: / A:)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        onAddAsset(
                            idInput,
                            nameInput,
                            systemInput,
                            locationInput,
                            criticalityInput,
                            statusInput,
                            ownerInput,
                            notesInput,
                            makeInput,
                            modelInput,
                            capacityInput,
                            routineInput,
                            ppmInput,
                            true
                        ) { err ->
                            errorMsg = err
                        }
                    },
                    modifier = Modifier.testTag("add_asset_open_sheet_button")
                ) {
                    Text("Save & Open")
                }
                Button(
                    onClick = {
                        onAddAsset(
                            idInput,
                            nameInput,
                            systemInput,
                            locationInput,
                            criticalityInput,
                            statusInput,
                            ownerInput,
                            notesInput,
                            makeInput,
                            modelInput,
                            capacityInput,
                            routineInput,
                            ppmInput,
                            false
                        ) { err ->
                            errorMsg = err
                        }
                    },
                    modifier = Modifier.testTag("add_asset_confirm_button")
                ) {
                    Text("Save Offline")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskAiMaintenanceCard(
    title: String,
    presetPrompts: List<String>,
    aiState: AiCardState,
    onQueryChange: (String) -> Unit,
    onToggleHighThinking: (Boolean) -> Unit,
    onAskQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    var showThinkingTrace by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, akah.line)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = akah.muted
                    )
                }

                // High Thinking Mode Toggle Badge (gemini-3.1-pro-preview + ThinkingLevel.HIGH)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "High Thinking Mode",
                        tint = if (aiState.highThinkingEnabled) MaterialTheme.colorScheme.primary else akah.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "High Thinking",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (aiState.highThinkingEnabled) MaterialTheme.colorScheme.primary else akah.muted
                    )
                    Switch(
                        checked = aiState.highThinkingEnabled,
                        onCheckedChange = onToggleHighThinking,
                        modifier = Modifier.testTag("high_thinking_switch")
                    )
                }
            }

            Text(
                text = "Gemini AI requires internet (${if (aiState.highThinkingEnabled) "ThinkingLevel.HIGH" else "ThinkingLevel.LOW"}) · Grounded strictly on the SOP register & history. Register features work offline.",
                style = MaterialTheme.typography.bodySmall,
                color = akah.muted
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetPrompts.forEachIndexed { idx, prompt ->
                    Surface(
                        onClick = {
                            if (!aiState.isBusy) onAskQuestion(prompt)
                        },
                        enabled = !aiState.isBusy,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, akah.line),
                        modifier = Modifier.testTag("ai_preset_chip_$idx")
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = aiState.queryInput,
                onValueChange = onQueryChange,
                label = { Text("Or type your own complex maintenance question") },
                minLines = 2,
                maxLines = 4,
                enabled = !aiState.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_question_input")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAskQuestion(aiState.queryInput) },
                    enabled = !aiState.isBusy && aiState.queryInput.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                    modifier = Modifier.testTag("ask_ai_button")
                ) {
                    if (aiState.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking deeply…")
                    } else {
                        Text("Ask AI")
                    }
                }

                Text(
                    text = when {
                        aiState.errorText.isNotEmpty() -> aiState.errorText
                        aiState.isBusy -> "Analyzing SOP record with gemini-3.1-pro-preview (ThinkingLevel.HIGH)…"
                        else -> "Check the answer against SOP ${SopConstants.SOP_CODE} before acting on it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (aiState.errorText.isNotEmpty()) akah.crit else akah.muted,
                    modifier = Modifier.weight(1f)
                )
            }

            if (aiState.answerText.isNotEmpty()) {
                Text(
                    text = "Asked: ${aiState.askedQuestion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted
                )

                if (aiState.thinkingSummary.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, akah.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThinkingTrace = !showThinkingTrace }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Reasoning Trace",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "High Thinking Reasoning Trace",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (showThinkingTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Reasoning Trace",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = showThinkingTrace) {
                                Text(
                                    text = aiState.thinkingSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = akah.muted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = akah.sunken,
                    border = BorderStroke(1.dp, akah.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiState.answerText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExportReportDialog(
    filteredAssetsCount: Int,
    allAssetsCount: Int,
    isFilterActive: Boolean,
    isExporting: Boolean,
    lastExportMessage: String?,
    onDismiss: () -> Unit,
    onExport: (format: String, useFilteredList: Boolean, includeHistory: Boolean) -> Unit
) {
    val akah = LocalAkahColors.current
    var useFiltered by remember(isFilterActive) { mutableStateOf(isFilterActive) }
    var includeHistory by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Asset Register Report",
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Generate and share an official AKAH SOP-AKA-HYD-001 Asset Register & Maintenance Status report in CSV (Excel/Power BI) or multi-page A4 PDF format.",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted
                )

                Text(
                    text = "REPORT SCOPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = akah.muted
                )

                // Option 1: Current Filtered Asset List
                Surface(
                    onClick = { useFiltered = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (useFiltered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (useFiltered) 2.dp else 1.dp,
                        color = if (useFiltered) MaterialTheme.colorScheme.primary else akah.line
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_scope_filtered")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(
                            text = "Current Asset List ($filteredAssetsCount assets)",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFilterActive) {
                                "Exports only the assets matching your current search/location/status filter."
                            } else {
                                "No filter active — exports all $filteredAssetsCount displayed assets."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted
                        )
                    }
                }

                // Option 2: Full Campus Register
                Surface(
                    onClick = { useFiltered = false },
                    shape = RoundedCornerShape(10.dp),
                    color = if (!useFiltered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (!useFiltered) 2.dp else 1.dp,
                        color = if (!useFiltered) MaterialTheme.colorScheme.primary else akah.line
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_scope_all")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(
                            text = "Full Campus Register ($allAssetsCount assets)",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Exports every registered asset in the offline Room database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted
                        )
                    }
                }

                // Include Maintenance History Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeHistory = !includeHistory }
                ) {
                    Checkbox(
                        checked = includeHistory,
                        onCheckedChange = { includeHistory = it },
                        modifier = Modifier.testTag("export_include_history_checkbox")
                    )
                    Text(
                        text = "Include Maintenance & Status Audit History",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = akah.line)

                Text(
                    text = "CHOOSE REPORT FORMAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = akah.muted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onExport("CSV", useFiltered, includeHistory) },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_format_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV")
                    }

                    Button(
                        onClick = { onExport("PDF", useFiltered, includeHistory) },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_format_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF")
                    }
                }

                if (isExporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Generating report file…",
                            style = MaterialTheme.typography.bodySmall,
                            color = akah.muted
                        )
                    }
                }

                if (!lastExportMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = akah.okSoft,
                        border = BorderStroke(1.dp, akah.ok),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_report_status_banner")
                    ) {
                        Text(
                            text = lastExportMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = akah.ok,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetMaintenanceDueDateDialog(
    asset: AssetEntity,
    onDismiss: () -> Unit,
    onSaveDueDate: (
        freqCode: String,
        dueDateIso: String,
        reminderLeadHours: Int,
        notificationsEnabled: Boolean,
        taskNote: String,
        sendTestNotificationNow: Boolean
    ) -> Unit,
    onClearDueDate: () -> Unit
) {
    val akah = LocalAkahColors.current
    val checklistGroups = remember(asset) { SopScheduleParser.checklist(asset) }
    val availableFreqs = remember(checklistGroups) {
        val codes = checklistGroups.map { it.code }.distinct()
        if (codes.isNotEmpty()) codes else listOf("D", "W", "M", "Q", "H", "A")
    }

    var selectedFreq by remember(asset) {
        mutableStateOf(
            asset.nextDueFreqCode.takeIf { it in availableFreqs }
                ?: availableFreqs.firstOrNull()
                ?: "D"
        )
    }

    val defaultDueMs = remember(asset) {
        SopScheduleParser.parseIsoMillis(asset.nextDueDateIso)
            ?: (System.currentTimeMillis() + 6 * 3_600_000L)
    }

    fun formatLocalInput(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(ms))
    }

    fun parseInputToIso(input: String): String {
        val trimmed = input.trim()
        val parsedMs = SopScheduleParser.parseIsoMillis(trimmed)
            ?: (System.currentTimeMillis() + 6 * 3_600_000L)
        return SopScheduleParser.isoFromMillis(parsedMs)
    }

    var dueDateInput by remember(asset) { mutableStateOf(formatLocalInput(defaultDueMs)) }
    var reminderLeadHours by remember(asset) {
        mutableIntStateOf(asset.reminderLeadHours.takeIf { it > 0 } ?: 24)
    }
    var notificationsEnabled by remember(asset) { mutableStateOf(asset.notificationsEnabled) }

    val defaultTaskForFreq = remember(selectedFreq, checklistGroups) {
        checklistGroups.find { it.code == selectedFreq }
            ?.items
            ?.joinToString(", ") { it.text }
            ?.take(120)
            .orEmpty()
    }
    var taskNote by remember(asset) {
        mutableStateOf(asset.nextDueTaskNote.ifBlank { defaultTaskForFreq })
    }
    var showCalendarPicker by remember { mutableStateOf(false) }

    if (showCalendarPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = SopScheduleParser.parseIsoMillis(dueDateInput)
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showCalendarPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMs = datePickerState.selectedDateMillis
                        if (pickedMs != null) {
                            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val datePart = sdfDate.format(Date(pickedMs))
                            val timePart = dueDateInput.substringAfter(' ', "09:00").take(5)
                            dueDateInput = "$datePart $timePart"
                        }
                        showCalendarPicker = false
                    }
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Set Maintenance Due Date & Alert",
                    fontFamily = BarlowDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                )
                Text(
                    text = "${asset.id} · ${asset.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = akah.muted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SOP TASK FREQUENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = akah.muted
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableFreqs.forEach { code ->
                        val isSelected = selectedFreq == code
                        Surface(
                            onClick = {
                                selectedFreq = code
                                val existingIso = asset.dueByFreq[code]
                                if (!existingIso.isNullOrBlank()) {
                                    SopScheduleParser.parseIsoMillis(existingIso)?.let {
                                        dueDateInput = formatLocalInput(it)
                                    }
                                }
                                val freqTasks = checklistGroups.find { it.code == code }
                                    ?.items
                                    ?.joinToString(", ") { it.text }
                                    ?.take(120)
                                    .orEmpty()
                                if (freqTasks.isNotBlank()) {
                                    taskNote = freqTasks
                                }
                            },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                            ),
                            modifier = Modifier.testTag("due_freq_chip_$code")
                        ) {
                            Text(
                                text = "$code · ${SopScheduleParser.freqLabel(code)}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "QUICK DEADLINE PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = akah.muted
                )

                val presets = listOf(
                    Triple("In 2 Hours", 2L * 3_600_000L, "due_preset_2h"),
                    Triple("In 6 Hours", 6L * 3_600_000L, "due_preset_6h"),
                    Triple("Tomorrow (+24h)", 24L * 3_600_000L, "due_preset_24h"),
                    Triple("In 3 Days", 72L * 3_600_000L, "due_preset_3d"),
                    Triple("In 7 Days", 168L * 3_600_000L, "due_preset_7d")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, offsetMs, tag) ->
                        Surface(
                            onClick = {
                                dueDateInput = formatLocalInput(System.currentTimeMillis() + offsetMs)
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag(tag)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dueDateInput,
                        onValueChange = { dueDateInput = it },
                        label = { Text("Due Date & Time (YYYY-MM-DD HH:mm)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("due_date_input")
                    )

                    OutlinedButton(
                        onClick = { showCalendarPicker = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("open_calendar_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Calendar Date",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = taskNote,
                    onValueChange = { taskNote = it },
                    label = { Text("SOP Maintenance Task / Instructions") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("due_task_note_input")
                )

                HorizontalDivider(color = akah.line)

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            contentDescription = "Local Notification Alert",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Local Deadline Notification",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Alert when SOP check approaches deadline",
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.muted
                            )
                        }
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        modifier = Modifier.testTag("due_notifications_switch")
                    )
                }

                if (notificationsEnabled) {
                    Text(
                        text = "NOTIFY BEFORE DEADLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = akah.muted
                    )
                    val leadOptions = listOf(2, 6, 12, 24, 48)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        leadOptions.forEach { hours ->
                            val isSelected = reminderLeadHours == hours
                            Surface(
                                onClick = { reminderLeadHours = hours },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                                ),
                                modifier = Modifier.testTag("lead_hours_chip_$hours")
                            ) {
                                Text(
                                    text = "${hours}h before",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveDueDate(
                                selectedFreq,
                                parseInputToIso(dueDateInput),
                                reminderLeadHours,
                                true,
                                taskNote,
                                true
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Save and send notification now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Save & Notify Now", fontSize = 12.sp)
                    }

                    if (asset.nextDueDateIso.isNotBlank()) {
                        OutlinedButton(
                            onClick = onClearDueDate,
                            modifier = Modifier.testTag("clear_due_date_button")
                        ) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveDueDate(
                        selectedFreq,
                        parseInputToIso(dueDateInput),
                        reminderLeadHours,
                        notificationsEnabled,
                        taskNote,
                        false
                    )
                },
                modifier = Modifier.testTag("save_due_date_button")
            ) {
                Text("Save Due Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
