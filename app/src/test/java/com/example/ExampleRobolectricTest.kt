package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.SopDao
import com.example.data.SopDatabase
import com.example.data.SopRepository
import com.example.model.AssetEntity
import com.example.model.ChecklistItemMark
import com.example.model.ReadingValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: SopDatabase
    private lateinit var dao: SopDao
    private lateinit var repository: SopRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SopDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sopDao()
        repository = SopRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AKAH SOP Register", appName)
    }

    @Test
    fun `room database seeds and stores asset name location and maintenance status offline`() = runBlocking {
        repository.ensureSeeded()
        val assets = repository.allAssets.first()
        assertEquals(48, assets.size)

        val firstAsset = dao.getAssetById("AKA-HYD-ELE-001")
        assertNotNull(firstAsset)
        assertEquals("Electrical Distribution Boards (DBs)", firstAsset?.name)
        assertEquals("Academic & Residential Blocks", firstAsset?.location)
        assertEquals("In service", firstAsset?.status)
    }

    @Test
    fun `offline status and location update persists in Room and creates audit log`() = runBlocking {
        dao.upsertAsset(
            AssetEntity(
                id = "AKA-HYD-HVC-001",
                name = "Split AC – All Capacities",
                system = "HVAC",
                location = "Admin Block Floor 1",
                status = "In service"
            )
        )

        val updated = repository.quickUpdateStatus(
            assetId = "AKA-HYD-HVC-001",
            newStatus = "Needs attention",
            newLocation = "Science Block Room 204",
            by = "A. Khan",
            note = "Filter cleaning and drain tray flush needed"
        )

        assertNotNull(updated)
        assertEquals("Needs attention", updated?.status)
        assertEquals("Science Block Room 204", updated?.location)
        assertTrue(updated?.pendingOfflineSync == true)

        val withLogs = dao.getAssetWithLogsOnce("AKA-HYD-HVC-001")
        assertNotNull(withLogs)
        assertEquals("Needs attention", withLogs?.asset?.status)
        assertEquals("Science Block Room 204", withLogs?.asset?.location)
        assertEquals(1, withLogs?.logs?.size)
        assertEquals("A. Khan", withLogs?.logs?.first()?.by)
    }

    @Test
    fun `offline asset registration and SOP check recording update Room state`() = runBlocking {
        val createdRes = repository.addNewAssetOffline(
            id = "AKA-HYD-ELE-011",
            name = "Backup Distribution Panel B2",
            system = "Electrical",
            location = "Senior School Basement",
            criticality = "Critical",
            status = "In service",
            owner = "Campus Electrical Lead",
            notes = "Newly commissioned panel",
            by = "R. Sharma"
        )
        assertTrue(createdRes.isSuccess)

        val afterCheck = repository.recordCheck(
            assetId = "AKA-HYD-ELE-011",
            by = "R. Sharma",
            freqCode = "D",
            resultStatus = "Out of service",
            marks = listOf(ChecklistItemMark("visual inspection", "nok")),
            readings = listOf(ReadingValue("temperature °C", "72 °C")),
            notes = "Isolated breaker for terminal retightening"
        )
        assertNotNull(afterCheck)
        assertEquals("Out of service", afterCheck?.status)

        val outOfServiceAssets = dao.observeAssetsByStatus("Out of service").first()
        assertEquals(1, outOfServiceAssets.size)
        assertEquals("AKA-HYD-ELE-011", outOfServiceAssets.first().id)
    }

    @Test
    fun `search assets by name or location returns matching offline assets`() = runBlocking {
        repository.ensureSeeded()

        val byName = repository.searchAssetsByNameOrLocation("Transformer").first()
        assertTrue(byName.isNotEmpty())
        assertTrue(byName.any { it.name.contains("Transformer", ignoreCase = true) })

        val byLocation = repository.searchAssetsByNameOrLocation("Main Kitchen").first()
        assertTrue(byLocation.isNotEmpty())
        assertTrue(byLocation.all { it.location.contains("Main Kitchen", ignoreCase = true) })
    }

    @Test
    fun `export current asset list as CSV and PDF report files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository.ensureSeeded()
        val kitchenAssets = repository.searchAssetsByNameOrLocation("Main Kitchen").first()
        val logs = repository.allLogs.first()

        val csvResult = com.example.util.ReportExporter.exportCsvReport(
            context = context,
            assets = kitchenAssets,
            logs = logs,
            includeHistoryCsv = true
        )
        assertTrue(csvResult.primaryFile.exists())
        assertTrue(csvResult.primaryFile.length() > 100L)
        assertEquals(kitchenAssets.size, csvResult.exportedAssetCount)
        assertEquals("CSV", csvResult.formatLabel)
        val csvText = csvResult.primaryFile.readText()
        assertTrue(csvText.contains("Asset ID,Sr,Cat,Name,System"))
        assertTrue(csvText.contains("Main Kitchen"))

        val pdfResult = com.example.util.ReportExporter.exportPdfReport(
            context = context,
            assets = kitchenAssets,
            logs = logs,
            filterSummary = "Location: Main Kitchen (${kitchenAssets.size} assets)",
            operatorName = "R. Sharma",
            includeRecentLogs = true
        )
        assertTrue(pdfResult.primaryFile.exists())
        assertTrue(pdfResult.primaryFile.length() > 500L)
        assertEquals(kitchenAssets.size, pdfResult.exportedAssetCount)
        assertEquals("PDF", pdfResult.formatLabel)
    }

    @Test
    fun `set maintenance due date evaluates approaching deadline and sends local notification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository.ensureSeeded()

        val dueInTwoHoursIso = com.example.model.SopScheduleParser.isoFromMillis(
            System.currentTimeMillis() + 2 * 3_600_000L
        )
        val updated = repository.setMaintenanceDueDate(
            assetId = "AKA-HYD-ELE-003",
            freqCode = "W",
            dueDateIso = dueInTwoHoursIso,
            reminderLeadHours = 24,
            notificationsEnabled = true,
            taskNote = "Weekly ATS auto-changeover & battery terminal inspection",
            by = "R. Sharma"
        )

        assertNotNull(updated)
        assertEquals(dueInTwoHoursIso, updated?.nextDueDateIso)
        assertEquals("W", updated?.nextDueFreqCode)
        assertEquals(24, updated?.reminderLeadHours)
        assertTrue(updated?.notificationsEnabled == true)

        val evaluation = com.example.model.SopScheduleParser.evaluateAssetDeadline(updated!!)
        assertEquals(com.example.model.DeadlineStatus.APPROACHING, evaluation.status)
        assertTrue(com.example.model.SopScheduleParser.isApproachingDeadline(updated))

        val posted = com.example.util.SopNotificationHelper.sendDeadlineNotification(
            context = context,
            asset = updated
        )
        assertTrue(posted)
    }

    @Test
    fun `asset audit log tracks past maintenance activities dates and previous status updates`() = runBlocking {
        repository.ensureSeeded()

        val initialLogs = repository.observeLogsForAsset("AKA-HYD-ELE-001").first()
        assertTrue(initialLogs.size >= 3)
        assertTrue(initialLogs.any { it.hasStatusTransition })
        assertTrue(initialLogs.any { it.previousStatus == "In service" && it.updatedStatus == "Needs attention" })
        assertTrue(initialLogs.any { it.previousStatus == "Needs attention" && it.updatedStatus == "In service" })

        val customDateIso = "2026-10-08T10:30:00.000Z"
        val afterManualAudit = repository.recordManualAuditActivity(
            assetId = "AKA-HYD-ELE-001",
            activityDateIso = customDateIso,
            by = "S. Kulkarni (AMC Auditor)",
            activityKind = "details",
            freqCode = "M",
            previousStatus = "In service",
            newStatus = "Needs attention",
            summaryNotes = "Infrared thermography audit identified loose neutral link in DB-02",
            updateCurrentAssetStatus = true
        )
        assertNotNull(afterManualAudit)
        assertEquals("Needs attention", afterManualAudit?.status)

        val updatedLogs = repository.observeLogsForAsset("AKA-HYD-ELE-001").first()
        assertEquals(initialLogs.size + 1, updatedLogs.size)
        val recordedEntry = updatedLogs.first { it.by == "S. Kulkarni (AMC Auditor)" }
        assertEquals(customDateIso, recordedEntry.at)
        assertEquals("In service", recordedEntry.previousStatus)
        assertEquals("Needs attention", recordedEntry.updatedStatus)
        assertTrue(recordedEntry.hasStatusTransition)
    }

    @Test
    fun `bulk CSV import parses structured CSV rows and seeds asset database`() = runBlocking {
        repository.ensureSeeded()
        val beforeAssets = repository.allAssets.first()

        val sampleCsv = com.example.util.CsvAssetImporter.buildSampleTemplateCsv()
        val preview = com.example.util.CsvAssetImporter.parseCsvText(
            rawCsvText = sampleCsv,
            sourceName = "AKAH_Sample_Seed_Batch.csv",
            existingAssets = beforeAssets
        )
        assertEquals(5, preview.validRows.size)
        assertTrue(preview.rowErrors.isEmpty())

        val importResult = repository.importAssetsFromCsv(
            rawCsvText = sampleCsv,
            sourceName = "AKAH_Sample_Seed_Batch.csv",
            conflictMode = com.example.util.CsvImportConflictMode.UPSERT_MERGE,
            recordedBy = "R. Sharma"
        )
        assertTrue(importResult.isSuccess)
        val summary = importResult.getOrThrow()
        assertEquals(5, summary.insertedCount)

        val afterAssets = repository.allAssets.first()
        assertEquals(beforeAssets.size + 5, afterAssets.size)
        val solarAsset = dao.getAssetById("AKA-HYD-ELE-051")
        assertNotNull(solarAsset)
        assertEquals("Main Solar Inverter Bank 250 kWp", solarAsset?.name)
        assertEquals("Electrical", solarAsset?.system)
        assertEquals("In service", solarAsset?.status)
    }

    @Test
    fun `exportQrLabelsPdf generates printable PDF file for available and new assets`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository.ensureSeeded()
        val assets = repository.allAssets.first().take(14)

        val qrPdfResult = com.example.util.ReportExporter.exportQrLabelsPdf(
            context = context,
            assets = assets,
            buildAssetQrUrl = { id -> "https://script.google.com/macros/s/AKAH_SOP_REGISTER/exec?asset=$id" },
            qrBaseAddress = "https://script.google.com/macros/s/AKAH_SOP_REGISTER/exec",
            fileName = "AKAH_Asset_QR_Labels.pdf"
        )
        assertTrue(qrPdfResult.primaryFile.exists())
        assertTrue(qrPdfResult.primaryFile.length() > 500L)
        assertEquals(assets.size, qrPdfResult.exportedAssetCount)
    }
}
