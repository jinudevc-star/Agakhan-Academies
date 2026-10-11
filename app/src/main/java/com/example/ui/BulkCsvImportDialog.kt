package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AssetEntity
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.util.BulkCsvImportSummary
import com.example.util.CsvAssetImporter
import com.example.util.CsvImportConflictMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkCsvImportDialog(
    existingAssets: List<AssetEntity>,
    isImporting: Boolean,
    lastImportMessage: String?,
    lastImportIsError: Boolean,
    onDismiss: () -> Unit,
    onDownloadSampleTemplate: () -> Unit,
    onImportCsv: (
        rawCsvText: String,
        sourceName: String,
        conflictMode: CsvImportConflictMode,
        onSuccessSummary: (BulkCsvImportSummary) -> Unit
    ) -> Unit,
    onExportImportedQrPdf: (List<AssetEntity>) -> Unit = {}
) {
    val context = LocalContext.current
    val akah = LocalAkahColors.current
    val coroutineScope = rememberCoroutineScope()

    var csvTextInput by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("No file selected") }
    var conflictMode by remember { mutableStateOf(CsvImportConflictMode.UPSERT_MERGE) }
    var showRawEditor by remember { mutableStateOf(false) }
    var fileReadError by remember { mutableStateOf<String?>(null) }
    var completedSummary by remember { mutableStateOf<BulkCsvImportSummary?>(null) }

    val parsePreview = remember(csvTextInput, sourceName, existingAssets, conflictMode) {
        if (csvTextInput.isBlank()) {
            null
        } else {
            val baseAssets = if (conflictMode == CsvImportConflictMode.REPLACE_ALL) {
                emptyList()
            } else {
                existingAssets
            }
            CsvAssetImporter.parseCsvText(
                rawCsvText = csvTextInput,
                sourceName = sourceName,
                existingAssets = baseAssets
            )
        }
    }

    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                fileReadError = null
                completedSummary = null
                val resolvedName = resolveFileName(context, uri) ?: "Uploaded_Assets.csv"
                val result = CsvAssetImporter.readCsvTextFromUri(context, uri)
                result.fold(
                    onSuccess = { text ->
                        sourceName = resolvedName
                        csvTextInput = text
                    },
                    onFailure = { err ->
                        fileReadError = err.message ?: "Could not read selected CSV file."
                    }
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, akah.line),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 780.dp)
                .heightIn(max = 760.dp)
                .testTag("bulk_csv_import_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Bulk CSV Import",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Bulk CSV Asset Import & Database Seeder",
                                fontFamily = BarlowDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Upload a structured .csv file or paste spreadsheet rows to seed the offline Room database",
                                style = MaterialTheme.typography.bodySmall,
                                color = akah.muted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_csv_import_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close CSV Import dialog"
                        )
                    }
                }

                HorizontalDivider(color = akah.line)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Step 1: Upload File / Load Sample / Download Template Actions
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, akah.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "1. Select Structured CSV Source",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Supports Assets.csv exports, Excel/Google Sheets CSVs (comma, semicolon, or tab-delimited). Missing Asset IDs are auto-generated (e.g. AKA-HYD-ELE-051).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = akah.muted
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        csvFileLauncher.launch(
                                            arrayOf(
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "application/csv",
                                                "application/vnd.ms-excel",
                                                "text/plain",
                                                "*/*"
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("select_csv_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = "Upload CSV File",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload CSV File", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        fileReadError = null
                                        completedSummary = null
                                        sourceName = "AKAH_Sample_Seed_Batch.csv"
                                        csvTextInput = CsvAssetImporter.buildSampleTemplateCsv()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("load_sample_csv_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAddCheck,
                                        contentDescription = "Load Sample 5-Asset CSV",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Sample CSV (5 Assets)")
                                }

                                OutlinedButton(
                                    onClick = { showRawEditor = !showRawEditor },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("toggle_csv_editor_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste or edit CSV text",
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (showRawEditor) "Hide CSV Editor" else "Paste / Edit CSV Text")
                                }

                                OutlinedButton(
                                    onClick = onDownloadSampleTemplate,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("download_csv_template_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Download Blank CSV Template",
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Template.csv")
                                }
                            }

                            // Supported Columns Quick Reference Chip Strip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, akah.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Supported columns",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Columns: Asset ID (optional/auto), Name (required), System, Location, Building, Room, Maintenance Status, Criticality, Make, Model, Serial, Asset Tag, Capacity, Owner, AMC Vendor, AMC Expiry, Routine Checks, PPM, Notes",
                                        fontFamily = IbmPlexMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = akah.muted
                                    )
                                }
                            }
                        }
                    }

                    // Optional Direct CSV Text Editor / Paste Box
                    AnimatedVisibility(visible = showRawEditor || csvTextInput.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Source File",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Source: $sourceName",
                                        fontFamily = IbmPlexMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (csvTextInput.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            csvTextInput = ""
                                            sourceName = "No file selected"
                                            completedSummary = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("clear_csv_input_button")
                                    ) {
                                        Text("Clear CSV", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (showRawEditor) {
                                OutlinedTextField(
                                    value = csvTextInput,
                                    onValueChange = {
                                        csvTextInput = it
                                        if (sourceName == "No file selected") {
                                            sourceName = "Pasted_CSV_Data.csv"
                                        }
                                        completedSummary = null
                                    },
                                    label = { Text("Structured CSV Content (Header row + Asset rows)") },
                                    placeholder = {
                                        Text("Asset ID,Name,System,Location,Maintenance Status,Criticality\nAKA-HYD-ELE-055,Backup UPS 40 kVA,Electrical,Server Room,In service,Critical")
                                    },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = IbmPlexMonoFontFamily,
                                        fontSize = 11.sp
                                    ),
                                    minLines = 5,
                                    maxLines = 9,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("csv_raw_text_input")
                                )
                            }
                        }
                    }

                    // Step 2: Import Conflict / Database Seeding Mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, akah.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "2. Database Import Strategy",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            CsvImportConflictMode.entries.forEach { mode ->
                                val selected = conflictMode == mode
                                Surface(
                                    onClick = { conflictMode = mode },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else akah.line
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("csv_mode_${mode.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { conflictMode = mode }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mode.label,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = mode.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = akah.muted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // File Read Error or Last Import Result Banner
                    if (!fileReadError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = akah.critSoft,
                            border = BorderStroke(1.dp, akah.crit),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "File error",
                                    tint = akah.crit
                                )
                                Text(
                                    text = fileReadError!!,
                                    color = akah.crit,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (!lastImportMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (lastImportIsError) akah.critSoft else akah.okSoft,
                            border = BorderStroke(1.dp, if (lastImportIsError) akah.crit else akah.ok),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("csv_import_result_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (lastImportIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                        contentDescription = "Import status",
                                        tint = if (lastImportIsError) akah.crit else akah.ok
                                    )
                                    Text(
                                        text = lastImportMessage,
                                        color = if (lastImportIsError) akah.crit else akah.ok,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val summary = completedSummary
                                if (!lastImportIsError && summary != null && summary.importedAssetIds.isNotEmpty()) {
                                    val importedEntities = remember(summary, existingAssets, parsePreview) {
                                        val byId = existingAssets.associateBy { it.id }
                                        val parsedById = parsePreview?.validRows?.associate { it.asset.id to it.asset }.orEmpty()
                                        summary.importedAssetIds.mapNotNull { id -> byId[id] ?: parsedById[id] }
                                    }
                                    if (importedEntities.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { onExportImportedQrPdf(importedEntities) },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("export_imported_qrs_pdf_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCode2,
                                                    contentDescription = "Export QR Labels PDF for Imported Assets",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Export QR Labels PDF for ${importedEntities.size} Imported Assets",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 3: Live Validation & Parsed Asset Table Preview
                    if (parsePreview != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, akah.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("csv_preview_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
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
                                            imageVector = Icons.Default.TableView,
                                            contentDescription = "Parsed Preview",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "3. Validation & Preview (${parsePreview.validRows.size} Valid Assets)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    Text(
                                        text = "${parsePreview.newAssetsCount} New · ${parsePreview.existingAssetsCount} Existing",
                                        fontFamily = IbmPlexMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Validation errors if any
                                if (parsePreview.rowErrors.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = akah.warnSoft,
                                        border = BorderStroke(1.dp, akah.warn),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${parsePreview.rowErrors.size} Row Warning(s) / Skipped Rows:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = akah.warn
                                            )
                                            parsePreview.rowErrors.take(5).forEach { err ->
                                                Text(
                                                    text = "• Line ${err.rowNumber} (${err.rawIdentifier}): ${err.message}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Preview Rows (up to 15 rows shown inline)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    parsePreview.validRows.take(15).forEach { item ->
                                        val asset = item.asset
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, akah.line),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("csv_preview_row_${asset.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = asset.id,
                                                            fontFamily = IbmPlexMonoFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (item.isExistingInDb) akah.warnSoft else akah.okSoft
                                                        ) {
                                                            Text(
                                                                text = when {
                                                                    item.isExistingInDb && conflictMode == CsvImportConflictMode.INSERT_ONLY_SKIP_EXISTING -> "SKIP (EXISTS)"
                                                                    item.isExistingInDb -> "UPDATE"
                                                                    item.isAutoGeneratedId -> "NEW (AUTO-ID)"
                                                                    else -> "NEW"
                                                                },
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (item.isExistingInDb) akah.warn else akah.ok,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = asset.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${asset.system} · ${asset.location} · ${asset.criticality}",
                                                        fontSize = 11.sp,
                                                        color = akah.muted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                StatusPill(status = asset.status)
                                            }
                                        }
                                    }
                                    if (parsePreview.validRows.size > 15) {
                                        Text(
                                            text = "+ ${parsePreview.validRows.size - 15} additional valid assets ready to import…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = akah.muted,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = akah.line)

                // Footer Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_csv_import_button")
                    ) {
                        Text("Close")
                    }

                    val actionableCount = when (conflictMode) {
                        CsvImportConflictMode.INSERT_ONLY_SKIP_EXISTING -> parsePreview?.newAssetsCount ?: 0
                        else -> parsePreview?.validRows?.size ?: 0
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (csvTextInput.isNotBlank()) {
                                    onImportCsv(csvTextInput, sourceName, conflictMode) { summary ->
                                        completedSummary = summary
                                    }
                                }
                            },
                            enabled = !isImporting && actionableCount > 0,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("confirm_csv_import_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importing Assets…")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Execute Bulk CSV Import",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (actionableCount > 0) {
                                        "Import $actionableCount Asset${if (actionableCount == 1) "" else "s"} to DB"
                                    } else {
                                        "Import CSV to Database"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else null
        } ?: uri.lastPathSegment
    } catch (_: Exception) {
        uri.lastPathSegment
    }
}
