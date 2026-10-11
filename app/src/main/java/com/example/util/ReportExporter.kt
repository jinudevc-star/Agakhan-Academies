package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.AssetEntity
import com.example.model.MaintenanceLogEntity
import com.example.model.SopConstants
import com.example.model.SopScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportedReportResult(
    val primaryFile: File,
    val secondaryFile: File? = null,
    val mimeType: String,
    val exportedAssetCount: Int,
    val formatLabel: String,
    val summaryMessage: String
)

object ReportExporter {

    private fun csvCell(v: Any?): String {
        val s = (v ?: "").toString()
        return if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }
    }

    fun buildAssetsCsvContent(assets: List<AssetEntity>): String {
        val headers = listOf(
            "Asset ID",
            "Sr",
            "Cat",
            "Name",
            "System",
            "Category",
            "Criticality",
            "Maintenance Status",
            "Location",
            "Make",
            "Model",
            "Serial",
            "Capacity",
            "Installed",
            "AMC Vendor",
            "AMC Expiry",
            "Owner",
            "Strategy",
            "Routine Checks",
            "PPM",
            "Last Check At",
            "Last Check Freq",
            "Last Check Result",
            "Last Check By",
            "Notes",
            "Revision",
            "Offline Pending Sync"
        )
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(headers.joinToString(",") { csvCell(it) }).append("\r\n")
        for (a in assets.sortedBy { it.id }) {
            val lc = a.lastCheck
            val row = listOf(
                a.id,
                a.sr,
                a.cat,
                a.name,
                a.system,
                a.category,
                a.criticality,
                a.status,
                a.location,
                a.make,
                a.model,
                a.serial,
                a.capacity,
                a.installed,
                a.amcVendor,
                a.amcExpiry,
                a.owner,
                a.strategy,
                a.routine,
                a.ppm,
                lc?.at ?: "",
                lc?.freq ?: "",
                lc?.result ?: "",
                lc?.by ?: "",
                a.notes,
                a.rev,
                if (a.pendingOfflineSync) "Yes" else "No"
            )
            sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun buildHistoryCsvContent(
        assets: List<AssetEntity>,
        logs: List<MaintenanceLogEntity>
    ): String {
        val assetMap = assets.associateBy { it.id }
        val headers = listOf(
            "Timestamp (ISO)",
            "Asset ID",
            "Asset Name",
            "System",
            "Location",
            "Kind",
            "Frequency",
            "Result / Status",
            "Recorded By",
            "Checklist Items",
            "Readings",
            "Changes",
            "Notes"
        )
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(headers.joinToString(",") { csvCell(it) }).append("\r\n")
        for (e in logs.sortedByDescending { it.at }) {
            val asset = assetMap[e.assetId]
            val itemsStr = e.items.joinToString(" | ") { "${it.t}=${it.s.uppercase()}" }
            val readingsStr = e.readings.joinToString(" | ") { "${it.t}: ${it.v}" }
            val changesStr = e.changes.entries.joinToString(" | ") { (k, v) ->
                "$k: ${v.getOrNull(0).orEmpty()} -> ${v.getOrNull(1).orEmpty()}"
            }
            val row = listOf(
                e.at,
                e.assetId,
                asset?.name ?: "",
                asset?.system ?: "",
                asset?.location ?: "",
                e.kind,
                e.freq,
                e.result,
                e.by,
                itemsStr,
                readingsStr,
                changesStr,
                e.notes
            )
            sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    suspend fun exportCsvReport(
        context: Context,
        assets: List<AssetEntity>,
        logs: List<MaintenanceLogEntity>,
        includeHistoryCsv: Boolean
    ): ExportedReportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val assetsFile = File(exportDir, "Assets.csv")
        assetsFile.writeText(buildAssetsCsvContent(assets), Charsets.UTF_8)

        val historyFile = if (includeHistoryCsv) {
            val filteredIds = assets.map { it.id }.toSet()
            val relevantLogs = logs.filter { it.assetId in filteredIds }
            File(exportDir, "Maintenance_History.csv").also {
                it.writeText(buildHistoryCsvContent(assets, relevantLogs), Charsets.UTF_8)
            }
        } else {
            null
        }

        ExportedReportResult(
            primaryFile = assetsFile,
            secondaryFile = historyFile,
            mimeType = "text/csv",
            exportedAssetCount = assets.size,
            formatLabel = "CSV",
            summaryMessage = if (historyFile != null) {
                "Exported ${assets.size} assets to Assets.csv + Maintenance_History.csv"
            } else {
                "Exported ${assets.size} assets to Assets.csv"
            }
        )
    }

    suspend fun exportPdfReport(
        context: Context,
        assets: List<AssetEntity>,
        logs: List<MaintenanceLogEntity>,
        filterSummary: String,
        operatorName: String,
        includeRecentLogs: Boolean
    ): ExportedReportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "AKAH_SOP_Asset_Report.pdf")

        // A4 Landscape in points (842 x 595) for clean multi-column asset register tables
        val pageWidth = 842
        val pageHeight = 595
        val margin = 32f

        val brandGreen = Color.rgb(0, 128, 74)
        val brandGreenSoft = Color.rgb(225, 243, 234)
        val inkDark = Color.rgb(20, 38, 30)
        val mutedGray = Color.rgb(92, 112, 102)
        val lineGray = Color.rgb(214, 224, 218)
        val rowAltBg = Color.rgb(245, 248, 246)
        val warnAmber = Color.rgb(180, 95, 6)
        val critRed = Color.rgb(185, 28, 28)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 245, 232)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkDark
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkDark
            textSize = 8.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen
            textSize = 8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGray
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = lineGray
        }

        val sortedAssets = assets.sortedBy { it.id }
        val inServiceCount = sortedAssets.count { it.status == "In service" }
        val needsAttentionCount = sortedAssets.count { it.status == "Needs attention" }
        val outOfServiceCount = sortedAssets.count { it.status == "Out of service" }
        val overdueCount = sortedAssets.count { SopScheduleParser.isLate(it) }
        val generatedTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())

        fun truncate(text: String, maxLen: Int): String {
            val clean = text.replace("\n", " ").trim()
            return if (clean.length <= maxLen) clean else clean.take(maxLen - 1) + "…"
        }

        val pageCount = runCatching {
            val pdfDocument = PdfDocument()
            try {
                var pageNumber = 1
                var currentPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                var canvas = currentPage.canvas
                var y = margin

                fun drawPageFooter(c: Canvas, pageNum: Int) {
                    c.drawLine(margin, pageHeight - 26f, pageWidth - margin, pageHeight - 26f, strokePaint)
                    c.drawText(
                        "SOP ${SopConstants.SOP_CODE} · Aga Khan Academy, Hyderabad · Offline Room Asset Register Report",
                        margin,
                        pageHeight - 13f,
                        mutedPaint
                    )
                    val pageLabel = "Page $pageNum"
                    val labelWidth = mutedPaint.measureText(pageLabel)
                    c.drawText(pageLabel, pageWidth - margin - labelWidth, pageHeight - 13f, mutedPaint)
                }

                fun startNewPage() {
                    drawPageFooter(canvas, pageNumber)
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    currentPage = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    )
                    canvas = currentPage.canvas
                    y = margin
                }

                // Top Banner on Page 1
                fillPaint.color = brandGreen
                canvas.drawRoundRect(
                    RectF(margin, y, pageWidth - margin, y + 48f),
                    8f,
                    8f,
                    fillPaint
                )
                canvas.drawText(
                    "AGA KHAN ACADEMY, HYDERABAD — ASSET REGISTER & SOP STATUS REPORT",
                    margin + 14f,
                    y + 20f,
                    titlePaint
                )
                canvas.drawText(
                    "Standard: ${SopConstants.SOP_CODE}  |  Generated: $generatedTimestamp  |  Prepared by: ${operatorName.ifBlank { "Duty Operator" }}  |  Scope: $filterSummary",
                    margin + 14f,
                    y + 37f,
                    subtitlePaint
                )
                y += 58f

                // 5 Summary KPI Boxes
                val kpis = listOf(
                    Triple("TOTAL EXPORTED", sortedAssets.size.toString(), inkDark),
                    Triple("IN SERVICE", inServiceCount.toString(), brandGreen),
                    Triple("NEEDS ATTENTION", needsAttentionCount.toString(), warnAmber),
                    Triple("OUT OF SERVICE", outOfServiceCount.toString(), critRed),
                    Triple("CHECKS OVERDUE", overdueCount.toString(), if (overdueCount > 0) warnAmber else inkDark)
                )
                val kpiGap = 8f
                val totalKpiWidth = pageWidth - 2 * margin
                val boxWidth = (totalKpiWidth - kpiGap * (kpis.size - 1)) / kpis.size
                kpis.forEachIndexed { idx, (label, value, color) ->
                    val left = margin + idx * (boxWidth + kpiGap)
                    val rect = RectF(left, y, left + boxWidth, y + 40f)
                    fillPaint.color = rowAltBg
                    canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
                    canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = color
                        textSize = 14f
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                    canvas.drawText(value, left + 10f, y + 19f, numPaint)
                    canvas.drawText(label, left + 10f, y + 33f, mutedPaint)
                }
                y += 52f

                val colOffsets = floatArrayOf(
                    margin,
                    margin + 92f,
                    margin + 278f,
                    margin + 383f,
                    margin + 521f,
                    margin + 579f,
                    margin + 665f
                )
                val colTitles = arrayOf(
                    "ASSET ID",
                    "ASSET NAME",
                    "SYSTEM",
                    "LOCATION",
                    "CRIT.",
                    "STATUS",
                    "LAST CHECK / OWNER"
                )
                val colMaxChars = intArrayOf(16, 34, 19, 26, 9, 16, 22)

                fun drawAssetTableHeader() {
                    fillPaint.color = brandGreenSoft
                    val headerRect = RectF(margin, y, pageWidth - margin, y + 20f)
                    canvas.drawRoundRect(headerRect, 4f, 4f, fillPaint)
                    for (i in colTitles.indices) {
                        canvas.drawText(colTitles[i], colOffsets[i] + 5f, y + 13.5f, tableHeaderPaint)
                    }
                    y += 22f
                }

                drawAssetTableHeader()

                sortedAssets.forEachIndexed { index, asset ->
                    if (y + 20f > pageHeight - 36f) {
                        startNewPage()
                        drawAssetTableHeader()
                    }

                    if (index % 2 == 1) {
                        fillPaint.color = rowAltBg
                        canvas.drawRect(margin, y, pageWidth - margin, y + 18f, fillPaint)
                    }

                    val rowTextY = y + 12.5f
                    canvas.drawText(truncate(asset.id, colMaxChars[0]), colOffsets[0] + 5f, rowTextY, monoCellPaint)
                    canvas.drawText(truncate(asset.name, colMaxChars[1]), colOffsets[1] + 5f, rowTextY, cellPaint)
                    canvas.drawText(truncate(asset.system, colMaxChars[2]), colOffsets[2] + 5f, rowTextY, cellPaint)
                    canvas.drawText(
                        truncate(asset.location.ifBlank { "—" }, colMaxChars[3]),
                        colOffsets[3] + 5f,
                        rowTextY,
                        cellPaint
                    )

                    val critPaint = Paint(cellPaint).apply {
                        color = when (asset.criticality) {
                            "Critical" -> critRed
                            "Medium" -> warnAmber
                            else -> brandGreen
                        }
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                    canvas.drawText(truncate(asset.criticality, colMaxChars[4]), colOffsets[4] + 5f, rowTextY, critPaint)

                    val statusPaint = Paint(cellPaint).apply {
                        color = when (asset.status) {
                            "In service" -> brandGreen
                            "Needs attention" -> warnAmber
                            "Out of service" -> critRed
                            else -> mutedGray
                        }
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                    canvas.drawText(truncate(asset.status, colMaxChars[5]), colOffsets[5] + 5f, rowTextY, statusPaint)

                    val lastCheckInfo = asset.lastCheck
                    val lastCheckSummary = if (lastCheckInfo != null && lastCheckInfo.at.isNotBlank()) {
                        "${lastCheckInfo.freq}: ${SopScheduleParser.ago(lastCheckInfo.at)}"
                    } else {
                        asset.owner.ifBlank { "No check yet" }
                    }
                    canvas.drawText(truncate(lastCheckSummary, colMaxChars[6]), colOffsets[6] + 5f, rowTextY, mutedPaint)

                    canvas.drawLine(margin, y + 18f, pageWidth - margin, y + 18f, strokePaint)
                    y += 18f
                }

                if (includeRecentLogs) {
                    val assetIdSet = sortedAssets.map { it.id }.toSet()
                    val relevantLogs = logs.filter { it.assetId in assetIdSet }.take(25)
                    if (relevantLogs.isNotEmpty()) {
                        if (y + 60f > pageHeight - 40f) {
                            startNewPage()
                        } else {
                            y += 16f
                        }
                        canvas.drawText(
                            "RECENT MAINTENANCE & STATUS AUDIT LOGS (${relevantLogs.size})",
                            margin,
                            y + 10f,
                            sectionHeaderPaint
                        )
                        y += 18f

                        relevantLogs.forEach { log ->
                            if (y + 18f > pageHeight - 36f) {
                                startNewPage()
                            }
                            val summaryLine =
                                "${log.at.take(16).replace('T', ' ')}  |  ${log.assetId}  |  ${log.kind.uppercase()} ${log.freq}  |  ${log.result}  |  By: ${log.by}  ${if (log.notes.isNotBlank()) "— ${log.notes}" else ""}"
                            canvas.drawText(truncate(summaryLine, 125), margin + 4f, y + 12f, cellPaint)
                            canvas.drawLine(margin, y + 16f, pageWidth - margin, y + 16f, strokePaint)
                            y += 16f
                        }
                    }
                }

                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)

                FileOutputStream(pdfFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                check(pdfFile.length() > 100L) { "Empty PdfDocument output" }
                pageNumber
            } finally {
                runCatching { pdfDocument.close() }
            }
        }.getOrElse {
            writeStandardPdf14Fallback(
                pdfFile = pdfFile,
                assets = sortedAssets,
                logs = logs,
                filterSummary = filterSummary,
                operatorName = operatorName,
                generatedTimestamp = generatedTimestamp,
                includeRecentLogs = includeRecentLogs
            )
        }

        ExportedReportResult(
            primaryFile = pdfFile,
            secondaryFile = null,
            mimeType = "application/pdf",
            exportedAssetCount = sortedAssets.size,
            formatLabel = "PDF",
            summaryMessage = "Exported ${sortedAssets.size} assets to ${pdfFile.name} ($pageCount page${if (pageCount == 1) "" else "s"})"
        )
    }

    suspend fun exportQrLabelsPdf(
        context: Context,
        assets: List<AssetEntity>,
        buildAssetQrUrl: (String) -> String,
        qrBaseAddress: String,
        fileName: String = "AKAH_Asset_QR_Labels.pdf"
    ): ExportedReportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeFileName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val pdfFile = File(exportDir, safeFileName)

        // A4 Portrait in points (595 x 842) — 12 labels per page (3 columns x 4 rows)
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 24f
        val topHeaderHeight = 46f
        val bottomFooterHeight = 22f
        val cols = 3
        val rows = 4
        val labelsPerPage = cols * rows
        val gapX = 10f
        val gapY = 10f

        val gridWidth = pageWidth - (marginX * 2f)
        val gridHeight = pageHeight - topHeaderHeight - bottomFooterHeight - 16f
        val cellWidth = (gridWidth - (gapX * (cols - 1))) / cols
        val cellHeight = (gridHeight - (gapY * (rows - 1))) / rows

        val brandGreen = Color.rgb(0, 128, 74)
        val inkDark = Color.rgb(21, 32, 26)
        val mutedGray = Color.rgb(86, 100, 92)
        val borderGray = Color.rgb(185, 198, 191)
        val warnAmber = Color.rgb(154, 98, 0)
        val critRed = Color.rgb(179, 38, 30)

        val sortedAssets = assets.sortedBy { it.id }
        val chunks = sortedAssets.chunked(labelsPerPage).ifEmpty { listOf(emptyList()) }
        val generatedTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGray
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen
            style = Paint.Style.FILL
        }
        val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 6.8f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val qrModulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkDark
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkDark
            textSize = 7.8f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val locPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGray
            textSize = 6.8f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandGreen
            textSize = 6.6f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        var pageCount = chunks.size

        try {
            val document = PdfDocument()
            chunks.forEachIndexed { pageIdx, pageAssets ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                // Top Page Header
                canvas.drawText(
                    "AGA KHAN ACADEMY, HYDERABAD · PHYSICAL ASSET QR CODE LABELS (${SopConstants.SOP_CODE})",
                    marginX,
                    22f,
                    headerTitlePaint
                )
                canvas.drawText(
                    "Base QR URL: $qrBaseAddress · ${sortedAssets.size} Asset Label${if (sortedAssets.size == 1) "" else "s"} · Generated: $generatedTimestamp · Page ${pageIdx + 1} of ${chunks.size}",
                    marginX,
                    35f,
                    headerSubPaint
                )

                pageAssets.forEachIndexed { indexOnPage, asset ->
                    val col = indexOnPage % cols
                    val row = indexOnPage / cols
                    val left = marginX + col * (cellWidth + gapX)
                    val top = topHeaderHeight + row * (cellHeight + gapY)
                    val right = left + cellWidth
                    val bottom = top + cellHeight
                    val centerX = (left + right) / 2f

                    val cardRect = RectF(left, top, right, bottom)
                    canvas.drawRoundRect(cardRect, 5f, 5f, cardBgPaint)
                    canvas.drawRoundRect(cardRect, 5f, 5f, cardBorderPaint)

                    // Top green header band inside label card
                    val bandRect = RectF(left + 4f, top + 4f, right - 4f, top + 18f)
                    canvas.drawRoundRect(bandRect, 3f, 3f, bannerBgPaint)
                    canvas.drawText(
                        "AGA KHAN ACADEMY · HYDERABAD",
                        centerX,
                        top + 13.5f,
                        bannerTextPaint
                    )

                    // Render crisp vector QR modules directly onto the PDF Canvas
                    val qrUrl = buildAssetQrUrl(asset.id)
                    val matrix = QrCodeUtils.generateQrBitMatrix(qrUrl, 1)
                    val qrBoxSize = minOf(cellWidth - 36f, cellHeight - 76f).coerceAtLeast(68f)
                    val qrLeft = centerX - (qrBoxSize / 2f)
                    val qrTop = top + 22f
                    val moduleCols = matrix.width.coerceAtLeast(1)
                    val moduleRows = matrix.height.coerceAtLeast(1)
                    val moduleW = qrBoxSize / moduleCols.toFloat()
                    val moduleH = qrBoxSize / moduleRows.toFloat()

                    for (my in 0 until moduleRows) {
                        for (mx in 0 until moduleCols) {
                            if (matrix.get(mx, my)) {
                                val mLeft = qrLeft + mx * moduleW
                                val mTop = qrTop + my * moduleH
                                canvas.drawRect(
                                    mLeft,
                                    mTop,
                                    mLeft + moduleW + 0.15f,
                                    mTop + moduleH + 0.15f,
                                    qrModulePaint
                                )
                            }
                        }
                    }

                    // Asset ID (monospace)
                    var textY = qrTop + qrBoxSize + 11f
                    canvas.drawText(asset.id, centerX, textY, idPaint)

                    // Asset Name (up to 2 lines)
                    textY += 10.5f
                    val cleanName = asset.name.trim()
                    if (cleanName.length <= 24) {
                        canvas.drawText(cleanName, centerX, textY, namePaint)
                        textY += 9.5f
                    } else {
                        val line1 = cleanName.take(24)
                        val line2 = cleanName.drop(24).take(22) + if (cleanName.length > 46) "..." else ""
                        canvas.drawText(line1, centerX, textY, namePaint)
                        textY += 9f
                        canvas.drawText(line2, centerX, textY, namePaint)
                        textY += 9.5f
                    }

                    // Location
                    val locText = asset.location.ifBlank { "Campus" }.take(26)
                    canvas.drawText(locText, centerX, textY, locPaint)

                    // Bottom System + Criticality
                    textY += 9.5f
                    metaPaint.color = when (asset.criticality) {
                        "Critical" -> critRed
                        "Medium" -> warnAmber
                        else -> brandGreen
                    }
                    canvas.drawText(
                        "${asset.system} · ${asset.criticality.uppercase(Locale.US)}",
                        centerX,
                        minOf(textY, bottom - 5f),
                        metaPaint
                    )
                }

                // Footer note
                canvas.drawText(
                    "Print on A4 at 100% scale (12 labels per sheet, 63mm x 68mm) · Scan with AKAH SOP Register app or phone camera.",
                    marginX,
                    pageHeight - 8f,
                    headerSubPaint
                )

                document.finishPage(page)
            }

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()
        } catch (_: Throwable) {
            // Will fallback below if needed
        }

        if (!pdfFile.exists() || pdfFile.length() < 100L) {
            pageCount = writeQrLabelsPdf14Fallback(
                pdfFile = pdfFile,
                assets = sortedAssets,
                buildAssetQrUrl = buildAssetQrUrl,
                qrBaseAddress = qrBaseAddress,
                generatedTimestamp = generatedTimestamp
            )
        }

        ExportedReportResult(
            primaryFile = pdfFile,
            secondaryFile = null,
            mimeType = "application/pdf",
            exportedAssetCount = sortedAssets.size,
            formatLabel = "QR PDF",
            summaryMessage = "Exported ${sortedAssets.size} QR code label${if (sortedAssets.size == 1) "" else "s"} to ${pdfFile.name} ($pageCount A4 page${if (pageCount == 1) "" else "s"})"
        )
    }

    private fun writeQrLabelsPdf14Fallback(
        pdfFile: File,
        assets: List<AssetEntity>,
        buildAssetQrUrl: (String) -> String,
        qrBaseAddress: String,
        generatedTimestamp: String
    ): Int {
        val labelsPerPage = 12
        val cols = 3
        val rows = 4
        val pages = assets.chunked(labelsPerPage).ifEmpty { listOf(emptyList()) }

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        fun addObj(id: Int, body: String) {
            offsets.add(sb.length)
            sb.append("$id 0 obj\n$body\nendobj\n")
        }

        val firstPageObjId = 4
        val pageObjIds = pages.indices.map { firstPageObjId + it * 2 }
        addObj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        addObj(
            2,
            "<< /Type /Pages /Kids [${pageObjIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>"
        )
        addObj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>")

        val marginX = 24f
        val gapX = 10f
        val gapY = 10f
        val cellW = (595f - 48f - 20f) / cols
        val cellH = (842f - 68f - 30f) / rows

        pages.forEachIndexed { pageIdx, pageAssets ->
            val pageObjId = firstPageObjId + pageIdx * 2
            val contentObjId = pageObjId + 1
            val stream = StringBuilder()

            // Page header text
            stream.append("BT /F1 9 Tf 24 818 Td (${pdfEscape("AGA KHAN ACADEMY, HYDERABAD - PHYSICAL ASSET QR CODE LABELS (${SopConstants.SOP_CODE})")}) Tj ET\n")
            stream.append("BT /F1 7.5 Tf 24 806 Td (${pdfEscape("Base QR URL: $qrBaseAddress | Assets: ${assets.size} | Generated: $generatedTimestamp | Page ${pageIdx + 1}/${pages.size}")}) Tj ET\n")

            pageAssets.forEachIndexed { idx, asset ->
                val col = idx % cols
                val row = idx / cols
                val left = marginX + col * (cellW + gapX)
                val topPdfY = 790f - row * (cellH + gapY)
                val bottomPdfY = topPdfY - cellH

                // Label border rectangle
                stream.append(String.format(Locale.US, "0.7 0.75 0.72 RG 0.8 w %.1f %.1f %.1f %.1f re S\n", left, bottomPdfY, cellW, cellH))

                // Green top bar
                stream.append(String.format(Locale.US, "0 0.5 0.29 rg %.1f %.1f %.1f 13 re f 0 0 0 rg\n", left + 3f, topPdfY - 16f, cellW - 6f))
                stream.append(String.format(Locale.US, "BT /F1 6.5 Tf %.1f %.1f Td (AGA KHAN ACADEMY - HYDERABAD) Tj ET\n", left + 14f, topPdfY - 12f))

                // Draw QR matrix modules using PDF rectangles
                val qrUrl = buildAssetQrUrl(asset.id)
                val matrix = QrCodeUtils.generateQrBitMatrix(qrUrl, 1)
                val qrSize = 78f
                val qrLeft = left + (cellW - qrSize) / 2f
                val qrTopY = topPdfY - 22f
                val mCols = matrix.width.coerceAtLeast(1)
                val mRows = matrix.height.coerceAtLeast(1)
                val modW = qrSize / mCols.toFloat()
                val modH = qrSize / mRows.toFloat()

                stream.append("0 0 0 rg\n")
                for (my in 0 until mRows) {
                    for (mx in 0 until mCols) {
                        if (matrix.get(mx, my)) {
                            val rx = qrLeft + mx * modW
                            val ry = qrTopY - (my + 1) * modH
                            stream.append(String.format(Locale.US, "%.2f %.2f %.2f %.2f re f\n", rx, ry, modW, modH))
                        }
                    }
                }

                // Asset ID, Name, Location, System & QR URL metadata
                val textBaseY = qrTopY - qrSize - 11f
                stream.append(String.format(Locale.US, "BT /F1 8 Tf %.1f %.1f Td (${pdfEscape(asset.id)}) Tj ET\n", left + 10f, textBaseY))
                stream.append(String.format(Locale.US, "BT /F1 7 Tf %.1f %.1f Td (${pdfEscape(asset.name.take(24))}) Tj ET\n", left + 10f, textBaseY - 10f))
                stream.append(String.format(Locale.US, "BT /F1 6.5 Tf %.1f %.1f Td (${pdfEscape(asset.location.ifBlank { "Campus" }.take(24))}) Tj ET\n", left + 10f, textBaseY - 19f))
                stream.append(String.format(Locale.US, "BT /F1 6.5 Tf %.1f %.1f Td (${pdfEscape("${asset.system} | ${asset.criticality.uppercase(Locale.US)}")}) Tj ET\n", left + 10f, textBaseY - 28f))
                stream.append(String.format(Locale.US, "BT /F1 5.5 Tf %.1f %.1f Td (${pdfEscape(qrUrl.take(34))}) Tj ET\n", left + 8f, textBaseY - 36f))
            }

            val streamContent = stream.toString()
            addObj(
                pageObjId,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentObjId 0 R >>"
            )
            addObj(
                contentObjId,
                "<< /Length ${streamContent.length} >>\nstream\n$streamContent\nendstream"
            )
        }

        val xrefOffset = sb.length
        sb.append("xref\n0 ${offsets.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        offsets.forEach { off ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", off))
        }
        sb.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        pdfFile.writeText(sb.toString(), Charsets.US_ASCII)
        return pages.size
    }

    private fun pdfEscape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .map { ch -> if (ch.code in 32..126) ch else '-' }
            .joinToString("")
    }

    private fun writeStandardPdf14Fallback(
        pdfFile: File,
        assets: List<AssetEntity>,
        logs: List<MaintenanceLogEntity>,
        filterSummary: String,
        operatorName: String,
        generatedTimestamp: String,
        includeRecentLogs: Boolean
    ): Int {
        val lines = mutableListOf<String>()
        lines.add("AGA KHAN ACADEMY, HYDERABAD - ASSET REGISTER & SOP STATUS REPORT")
        lines.add("Standard: ${SopConstants.SOP_CODE} | Generated: $generatedTimestamp | Prepared by: ${operatorName.ifBlank { "Duty Operator" }}")
        lines.add("Scope: $filterSummary")
        lines.add("-".repeat(110))
        lines.add(
            String.format(
                Locale.US,
                "%-16s %-32s %-18s %-24s %-10s %-15s",
                "ASSET ID",
                "ASSET NAME",
                "SYSTEM",
                "LOCATION",
                "CRIT.",
                "STATUS"
            )
        )
        lines.add("-".repeat(110))
        assets.forEach { a ->
            lines.add(
                String.format(
                    Locale.US,
                    "%-16s %-32s %-18s %-24s %-10s %-15s",
                    a.id.take(15),
                    a.name.take(31),
                    a.system.take(17),
                    a.location.ifBlank { "-" }.take(23),
                    a.criticality.take(9),
                    a.status.take(14)
                )
            )
        }
        if (includeRecentLogs && logs.isNotEmpty()) {
            lines.add("")
            lines.add("RECENT MAINTENANCE & STATUS AUDIT LOGS")
            lines.add("-".repeat(110))
            val assetIds = assets.map { it.id }.toSet()
            logs.filter { it.assetId in assetIds }.take(20).forEach { log ->
                lines.add("${log.at.take(16)} | ${log.assetId} | ${log.kind.uppercase()} ${log.freq} | ${log.result} | By: ${log.by}")
            }
        }

        val linesPerPage = 36
        val pages = lines.chunked(linesPerPage).ifEmpty { listOf(listOf("No assets exported")) }

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        fun addObj(id: Int, body: String) {
            offsets.add(sb.length)
            sb.append("$id 0 obj\n$body\nendobj\n")
        }

        // Obj 1: Catalog, Obj 2: Pages, Obj 3: Font
        val firstPageObjId = 4
        val pageObjIds = pages.indices.map { firstPageObjId + it * 2 }
        addObj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        addObj(
            2,
            "<< /Type /Pages /Kids [${pageObjIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>"
        )
        addObj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>")

        pages.forEachIndexed { idx, pageLines ->
            val pageObjId = firstPageObjId + idx * 2
            val contentObjId = pageObjId + 1
            val streamBuilder = StringBuilder()
            streamBuilder.append("BT\n/F1 8.5 Tf\n32 555 Td\n13 TL\n")
            pageLines.forEach { line ->
                streamBuilder.append("(${pdfEscape(line)}) Tj T*\n")
            }
            streamBuilder.append("ET")
            val streamContent = streamBuilder.toString()

            addObj(
                pageObjId,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentObjId 0 R >>"
            )
            addObj(
                contentObjId,
                "<< /Length ${streamContent.length} >>\nstream\n$streamContent\nendstream"
            )
        }

        val xrefOffset = sb.length
        sb.append("xref\n0 ${offsets.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        offsets.forEach { off ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", off))
        }
        sb.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        pdfFile.writeText(sb.toString(), Charsets.US_ASCII)
        return pages.size
    }
}
