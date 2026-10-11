package com.example.data

import android.content.Context
import com.example.model.AssetEntity
import com.example.model.AssetWithLogs
import com.example.model.ChecklistItemMark
import com.example.model.LastCheckInfo
import com.example.model.MaintenanceLogEntity
import com.example.model.ReadingValue
import com.example.model.SopConstants
import com.example.model.SopJson
import com.example.model.SopScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class SopRepository(private val dao: SopDao) {

    val allAssets: Flow<List<AssetEntity>> = dao.observeAllAssets()
    val allLogs: Flow<List<MaintenanceLogEntity>> = dao.observeAllLogs()
    val pendingOfflineSyncCount: Flow<Int> = dao.observePendingOfflineSyncCount()

    fun observeAsset(id: String): Flow<AssetEntity?> = dao.observeAssetById(id)
    fun observeAssetWithLogs(id: String): Flow<AssetWithLogs?> = dao.observeAssetWithLogs(id)
    fun observeLogsForAsset(id: String): Flow<List<MaintenanceLogEntity>> = dao.observeLogsForAsset(id)
    fun searchAssetsByNameOrLocation(query: String): Flow<List<AssetEntity>> =
        dao.searchAssetsByNameOrLocation(query.trim())
    fun searchAssetsOffline(query: String, system: String = "All"): Flow<List<AssetEntity>> =
        dao.searchAssetsOffline(query.trim(), system)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.getAssetCount() == 0) {
            val seeds = SeedAssetsData.getAllSeedAssets()
            dao.insertAssets(seeds)
            val nowMs = System.currentTimeMillis()
            val twoHoursAgo = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(nowMs - 2 * 3600_000L))
            val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(nowMs - 18 * 3600_000L))
            val threeDaysAgo = SopScheduleParser.isoFromMillis(nowMs - 3 * 86_400_000L)
            val sevenDaysAgo = SopScheduleParser.isoFromMillis(nowMs - 7 * 86_400_000L)
            val fourteenDaysAgo = SopScheduleParser.isoFromMillis(nowMs - 14 * 86_400_000L)
            val in4Hours = SopScheduleParser.isoFromMillis(nowMs + 4 * 3600_000L)
            val in8Hours = SopScheduleParser.isoFromMillis(nowMs + 8 * 3600_000L)
            val in18Hours = SopScheduleParser.isoFromMillis(nowMs + 18 * 3600_000L)

            // 1. Record multi-step maintenance history & status transitions on AKA-HYD-ELE-001
            dao.getAssetById("AKA-HYD-ELE-001")?.let { a ->
                val checkInfo = LastCheckInfo(at = twoHoursAgo, freq = "D", result = "In service", by = "R. Sharma (Duty Electrician)")
                val freqMap = mapOf("D" to twoHoursAgo, "W" to sevenDaysAgo, "M" to fourteenDaysAgo)
                val dueMap = mapOf("D" to in18Hours)
                dao.upsertAsset(
                    a.copy(
                        status = "In service",
                        lastCheckJson = SopJson.encodeLastCheck(checkInfo),
                        lastByFreqJson = SopJson.encodeMap(freqMap),
                        dueByFreqJson = SopJson.encodeMap(dueMap),
                        nextDueDateIso = in18Hours,
                        nextDueFreqCode = "D",
                        nextDueTaskNote = "Daily DB visual & hotspot temperature inspection",
                        reminderLeadHours = 24,
                        notificationsEnabled = true,
                        rev = 3,
                        lastUpdatedAt = nowMs - 2 * 3600_000L,
                        pendingOfflineSync = false
                    )
                )
                // Historical entry 1 (14 days ago): Monthly SOP Check -> Needs attention
                dao.insertLog(
                    MaintenanceLogEntity(
                        assetId = a.id,
                        at = fourteenDaysAgo,
                        by = "M. Baig (Senior Technician)",
                        kind = "check",
                        freq = "M",
                        result = "Needs attention",
                        itemsJson = SopJson.encodeItems(
                            listOf(
                                ChecklistItemMark("ELCB/RCCB test button", "ok"),
                                ChecklistItemMark("tightness of outgoing terminals", "nok"),
                                ChecklistItemMark("cleaning inside", "ok"),
                                ChecklistItemMark("phase balance", "ok")
                            )
                        ),
                        readingsJson = SopJson.encodeReadings(
                            listOf(
                                ReadingValue("load A per phase", "48 A / 39 A / 44 A"),
                                ReadingValue("hotspot temp °C", "58 °C")
                            )
                        ),
                        changesJson = SopJson.encodeChanges(
                            mapOf("status" to listOf("In service", "Needs attention"))
                        ),
                        notes = "Minor thermal hotspot detected on B-phase outgoing terminal in Academic Block DB-04; scheduled torque re-tightening.",
                        syncedToServer = true
                    )
                )
                // Historical entry 2 (7 days ago): Status Update after corrective torque tightening -> In service
                dao.insertLog(
                    MaintenanceLogEntity(
                        assetId = a.id,
                        at = sevenDaysAgo,
                        by = "R. Sharma (Duty Electrician)",
                        kind = "details",
                        result = "In service",
                        changesJson = SopJson.encodeChanges(
                            mapOf(
                                "status" to listOf("Needs attention", "In service"),
                                "notes" to listOf(
                                    "B-phase terminal torque check required",
                                    "Re-torqued B-phase terminal lugs and verified 34 °C with IR thermometer"
                                )
                            )
                        ),
                        notes = "Completed terminal retightening and thermal re-verification; returned DB-04 to normal service.",
                        syncedToServer = true
                    )
                )
                // Historical entry 3 (2 hours ago): Daily SOP check -> In service
                dao.insertLog(
                    MaintenanceLogEntity(
                        assetId = a.id,
                        at = twoHoursAgo,
                        by = "R. Sharma (Duty Electrician)",
                        kind = "check",
                        freq = "D",
                        result = "In service",
                        itemsJson = SopJson.encodeItems(
                            listOf(
                                ChecklistItemMark("visual", "ok"),
                                ChecklistItemMark("door closed/locked", "ok"),
                                ChecklistItemMark("no burning smell or heat", "ok"),
                                ChecklistItemMark("indicators ON", "ok")
                            )
                        ),
                        readingsJson = SopJson.encodeReadings(
                            listOf(
                                ReadingValue("load A per phase", "42 A / 40 A / 41 A"),
                                ReadingValue("hotspot temp °C", "34 °C")
                            )
                        ),
                        changesJson = SopJson.encodeChanges(
                            mapOf("status" to listOf("In service", "In service"))
                        ),
                        notes = "All academic block DB indicators normal.",
                        syncedToServer = true
                    )
                )
            }

            // 2. Record multi-step history & Needs attention check on AKA-HYD-ELE-005 (Transformer)
            dao.getAssetById("AKA-HYD-ELE-005")?.let { a ->
                val checkInfo = LastCheckInfo(at = yesterday, freq = "D", result = "Needs attention", by = "K. Reddy (HT Operator)")
                val freqMap = mapOf("D" to yesterday, "W" to threeDaysAgo)
                val dueMap = mapOf("D" to in4Hours)
                dao.upsertAsset(
                    a.copy(
                        status = "Needs attention",
                        lastCheckJson = SopJson.encodeLastCheck(checkInfo),
                        lastByFreqJson = SopJson.encodeMap(freqMap),
                        dueByFreqJson = SopJson.encodeMap(dueMap),
                        nextDueDateIso = in4Hours,
                        nextDueFreqCode = "D",
                        nextDueTaskNote = "Replace silica gel breather & verify oil/winding temperature",
                        reminderLeadHours = 24,
                        notificationsEnabled = true,
                        rev = 2,
                        lastUpdatedAt = nowMs - 18 * 3600_000L,
                        pendingOfflineSync = false
                    )
                )
                dao.insertLog(
                    MaintenanceLogEntity(
                        assetId = a.id,
                        at = threeDaysAgo,
                        by = "K. Reddy (HT Operator)",
                        kind = "check",
                        freq = "W",
                        result = "In service",
                        itemsJson = SopJson.encodeItems(
                            listOf(
                                ChecklistItemMark("bushing cleanliness", "ok"),
                                ChecklistItemMark("conservator level", "ok"),
                                ChecklistItemMark("earthing strip continuity", "ok")
                            )
                        ),
                        readingsJson = SopJson.encodeReadings(
                            listOf(
                                ReadingValue("Oil temp °C", "51 °C"),
                                ReadingValue("winding temp °C", "58 °C"),
                                ReadingValue("load kVA", "395 kVA")
                            )
                        ),
                        changesJson = SopJson.encodeChanges(
                            mapOf("status" to listOf("In service", "In service"))
                        ),
                        notes = "Weekly HT yard inspection passed.",
                        syncedToServer = true
                    )
                )
                dao.insertLog(
                    MaintenanceLogEntity(
                        assetId = a.id,
                        at = yesterday,
                        by = "K. Reddy (HT Operator)",
                        kind = "check",
                        freq = "D",
                        result = "Needs attention",
                        itemsJson = SopJson.encodeItems(
                            listOf(
                                ChecklistItemMark("oil temp", "ok"),
                                ChecklistItemMark("winding temp", "ok"),
                                ChecklistItemMark("oil level", "ok"),
                                ChecklistItemMark("load A", "ok"),
                                ChecklistItemMark("silica gel colour", "nok"),
                                ChecklistItemMark("leaks", "ok"),
                                ChecklistItemMark("noise", "ok")
                            )
                        ),
                        readingsJson = SopJson.encodeReadings(
                            listOf(
                                ReadingValue("Oil temp °C", "54 °C"),
                                ReadingValue("winding temp °C", "61 °C"),
                                ReadingValue("load kVA", "410 kVA")
                            )
                        ),
                        changesJson = SopJson.encodeChanges(
                            mapOf("status" to listOf("In service", "Needs attention"))
                        ),
                        notes = "Breather silica gel >2/3 turned pink; scheduled replacement tomorrow morning.",
                        syncedToServer = true
                    )
                )
            }

            // 3. Seed an approaching Weekly SOP check deadline + past check log on AKA-HYD-HVC-001 (due in 8h)
            dao.getAssetById("AKA-HYD-HVC-001")?.let { a ->
                val dueMap = mapOf("W" to in8Hours)
                val freqMap = mapOf("W" to sevenDaysAgo)
                val checkInfo = LastCheckInfo(at = sevenDaysAgo, freq = "W", result = "In service", by = "S. Pasha (HVAC Tech)")
                dao.upsertAsset(
                    a.copy(
                        lastCheckJson = SopJson.encodeLastCheck(checkInfo),
                        lastByFreqJson = SopJson.encodeMap(freqMap),
                        dueByFreqJson = SopJson.encodeMap(dueMap),
                        nextDueDateIso = in8Hours,
                        nextDueFreqCode = "W",
                        nextDueTaskNote = "Weekly filter wash & condensate drain check across Admin Block",
                        reminderLeadHours = 24,
                        notificationsEnabled = true
                    )
                )
                dao.insertLog(
                    MaintenanceLogEntity(
                        assetId = a.id,
                        at = sevenDaysAgo,
                        by = "S. Pasha (HVAC Tech)",
                        kind = "check",
                        freq = "W",
                        result = "In service",
                        itemsJson = SopJson.encodeItems(
                            listOf(
                                ChecklistItemMark("filter wash", "ok"),
                                ChecklistItemMark("condensate drain clear", "ok"),
                                ChecklistItemMark("grille/louvre condition", "ok")
                            )
                        ),
                        readingsJson = SopJson.encodeReadings(
                            listOf(
                                ReadingValue("supply air temp °C", "14.2 °C"),
                                ReadingValue("running current A", "6.8 A")
                            )
                        ),
                        changesJson = SopJson.encodeChanges(
                            mapOf("status" to listOf("In service", "In service"))
                        ),
                        notes = "Washed return air filters in Admin Block Floor 1.",
                        syncedToServer = true
                    )
                )
            }
        }
    }

    suspend fun addNewAssetOffline(
        id: String,
        name: String,
        system: String,
        location: String,
        criticality: String,
        status: String,
        owner: String,
        notes: String,
        by: String,
        make: String = "",
        model: String = "",
        capacity: String = "",
        routine: String = "",
        ppm: String = ""
    ): Result<AssetEntity> = withContext(Dispatchers.IO) {
        val cleanId = id.trim().uppercase()
        if (!SopConstants.EXACT_ASSET_ID_REGEX.matches(cleanId)) {
            return@withContext Result.failure(
                IllegalArgumentException("Asset ID must follow format AKA-HYD-XXX-000 (e.g., AKA-HYD-ELE-011)")
            )
        }
        if (name.trim().isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Asset name is required."))
        }
        if (dao.getAssetById(cleanId) != null) {
            return@withContext Result.failure(IllegalArgumentException("Asset ID $cleanId already exists in the offline register."))
        }
        val catCode = cleanId.split("-").getOrNull(2) ?: "GEN"
        val nowMs = System.currentTimeMillis()
        val at = SopScheduleParser.nowIso()
        val defaultRoutine = routine.trim().ifBlank {
            "D: Visual inspection, abnormal noise/vibration check, indicator status | W: Cleaning, terminal/connection check, functional test | M: Full operational parameter log and safety interlock verification"
        }
        val defaultPpm = ppm.trim().ifBlank {
            "Q: Comprehensive preventive maintenance, calibration & wear inspection | A: Annual overhaul, statutory testing & certification"
        }
        val newAsset = AssetEntity(
            id = cleanId,
            sr = dao.getAssetCount() + 1,
            cat = catCode,
            name = name.trim(),
            system = system.trim().ifBlank { "Electrical" },
            category = system.trim().ifBlank { "Electrical" },
            criticality = criticality.trim().ifBlank { "Medium" },
            status = status.trim().ifBlank { "In service" },
            location = location.trim(),
            make = make.trim(),
            model = model.trim(),
            capacity = capacity.trim(),
            routine = defaultRoutine,
            ppm = defaultPpm,
            strategy = "Preventive + Condition-Based (SOP-AKA-HYD-001)",
            owner = owner.trim(),
            notes = notes.trim(),
            rev = 1,
            lastUpdatedAt = nowMs,
            pendingOfflineSync = true
        )
        dao.upsertAsset(newAsset)
        dao.insertLog(
            MaintenanceLogEntity(
                assetId = cleanId,
                at = at,
                by = by.ifBlank { "Duty Operator" },
                kind = "details",
                result = newAsset.status,
                notes = "Registered asset offline in Room database",
                changesJson = SopJson.encodeChanges(
                    mapOf(
                        "status" to listOf("", newAsset.status),
                        "location" to listOf("", newAsset.location)
                    )
                ),
                syncedToServer = false
            )
        )
        Result.success(newAsset)
    }

    suspend fun recordCheck(
        assetId: String,
        by: String,
        freqCode: String,
        resultStatus: String,
        marks: List<ChecklistItemMark>,
        readings: List<ReadingValue>,
        notes: String,
        lanServerUrl: String = ""
    ): AssetEntity? = withContext(Dispatchers.IO) {
        val current = dao.getAssetById(assetId) ?: return@withContext null
        val at = SopScheduleParser.nowIso()
        val nowMs = System.currentTimeMillis()
        val recorder = by.ifBlank { "Duty Operator" }
        val updatedFreqMap = current.lastByFreq.toMutableMap().apply {
            put(freqCode, at)
        }
        val lastCheck = LastCheckInfo(
            at = at,
            freq = freqCode,
            result = resultStatus,
            by = recorder
        )
        val intervalDays = SopScheduleParser.intervalDays(freqCode) ?: 1.0
        val nextScheduledForFreqIso = SopScheduleParser.isoFromMillis(
            nowMs + (intervalDays * 86_400_000.0).toLong()
        )
        val updatedDueMap = current.dueByFreq.toMutableMap().apply {
            put(freqCode, nextScheduledForFreqIso)
        }
        val shouldAdvancePrimaryDue = current.nextDueDateIso.isBlank() || current.nextDueFreqCode == freqCode
        val tempAsset = current.copy(
            status = resultStatus,
            lastCheckJson = SopJson.encodeLastCheck(lastCheck),
            lastByFreqJson = SopJson.encodeMap(updatedFreqMap),
            dueByFreqJson = SopJson.encodeMap(updatedDueMap),
            nextDueDateIso = if (shouldAdvancePrimaryDue) nextScheduledForFreqIso else current.nextDueDateIso,
            nextDueFreqCode = if (shouldAdvancePrimaryDue) freqCode else current.nextDueFreqCode,
            rev = current.rev + 1,
            lastUpdatedAt = nowMs,
            pendingOfflineSync = true
        )
        val logEntry = MaintenanceLogEntity(
            assetId = assetId,
            at = at,
            by = recorder,
            kind = "check",
            freq = freqCode,
            result = resultStatus,
            itemsJson = SopJson.encodeItems(marks),
            readingsJson = SopJson.encodeReadings(readings),
            notes = notes.trim(),
            changesJson = SopJson.encodeChanges(
                mapOf("status" to listOf(current.status, resultStatus))
            ),
            syncedToServer = false
        )
        persistLocallyThenSync(tempAsset, logEntry, lanServerUrl)
    }

    suspend fun quickUpdateStatus(
        assetId: String,
        newStatus: String,
        newLocation: String? = null,
        by: String,
        note: String,
        lanServerUrl: String = ""
    ): AssetEntity? = withContext(Dispatchers.IO) {
        val current = dao.getAssetById(assetId) ?: return@withContext null
        val at = SopScheduleParser.nowIso()
        val nowMs = System.currentTimeMillis()
        val recorder = by.ifBlank { "Duty Operator" }
        val resolvedLocation = newLocation?.trim() ?: current.location
        val changes = mutableMapOf<String, List<String>>()
        if (current.status != newStatus) {
            changes["status"] = listOf(current.status, newStatus)
        }
        if (resolvedLocation != current.location) {
            changes["location"] = listOf(current.location, resolvedLocation)
        }
        if (note.isNotBlank()) {
            changes["notes"] = listOf(current.notes, note.trim())
        }
        if (changes.isEmpty()) {
            changes["status"] = listOf(current.status, newStatus)
        }
        val tempAsset = current.copy(
            status = newStatus,
            location = resolvedLocation,
            notes = if (note.isNotBlank()) note.trim() else current.notes,
            rev = current.rev + 1,
            lastUpdatedAt = nowMs,
            pendingOfflineSync = true
        )
        val logEntry = MaintenanceLogEntity(
            assetId = assetId,
            at = at,
            by = recorder,
            kind = "details",
            result = newStatus,
            notes = note.trim(),
            changesJson = SopJson.encodeChanges(changes),
            syncedToServer = false
        )
        persistLocallyThenSync(tempAsset, logEntry, lanServerUrl)
    }

    suspend fun updateAssetDetails(
        assetId: String,
        by: String,
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
        lanServerUrl: String = ""
    ): AssetEntity? = withContext(Dispatchers.IO) {
        val current = dao.getAssetById(assetId) ?: return@withContext null
        val changes = linkedMapOf<String, List<String>>()
        fun track(key: String, oldVal: String, newVal: String) {
            val o = oldVal.trim()
            val n = newVal.trim()
            if (o != n) {
                changes[key] = listOf(o, n)
            }
        }
        track("status", current.status, status)
        track("criticality", current.criticality, criticality)
        track("location", current.location, location)
        track("make", current.make, make)
        track("model", current.model, model)
        track("serial", current.serial, serial)
        track("capacity", current.capacity, capacity)
        track("installed", current.installed, installed)
        track("amcVendor", current.amcVendor, amcVendor)
        track("amcExpiry", current.amcExpiry, amcExpiry)
        track("owner", current.owner, owner)
        track("notes", current.notes, notes)

        if (changes.isEmpty()) return@withContext current

        val at = SopScheduleParser.nowIso()
        val nowMs = System.currentTimeMillis()
        val recorder = by.ifBlank { "Duty Operator" }
        val tempAsset = current.copy(
            status = status.trim(),
            criticality = criticality.trim(),
            location = location.trim(),
            make = make.trim(),
            model = model.trim(),
            serial = serial.trim(),
            capacity = capacity.trim(),
            installed = installed.trim(),
            amcVendor = amcVendor.trim(),
            amcExpiry = amcExpiry.trim(),
            owner = owner.trim(),
            notes = notes.trim(),
            rev = current.rev + 1,
            lastUpdatedAt = nowMs,
            pendingOfflineSync = true
        )
        val logEntry = MaintenanceLogEntity(
            assetId = assetId,
            at = at,
            by = recorder,
            kind = "details",
            changesJson = SopJson.encodeChanges(changes),
            syncedToServer = false
        )
        persistLocallyThenSync(tempAsset, logEntry, lanServerUrl)
    }

    suspend fun setMaintenanceDueDate(
        assetId: String,
        freqCode: String,
        dueDateIso: String,
        reminderLeadHours: Int,
        notificationsEnabled: Boolean,
        taskNote: String,
        by: String
    ): AssetEntity? = withContext(Dispatchers.IO) {
        val current = dao.getAssetById(assetId) ?: return@withContext null
        val cleanFreq = freqCode.trim().ifBlank { "D" }
        val cleanIso = dueDateIso.trim()
        val nowMs = System.currentTimeMillis()
        val at = SopScheduleParser.nowIso()
        val recorder = by.ifBlank { "Duty Operator" }

        val updatedDueMap = current.dueByFreq.toMutableMap().apply {
            if (cleanIso.isNotBlank()) {
                put(cleanFreq, cleanIso)
            } else {
                remove(cleanFreq)
            }
        }
        val dueByFreqJson = SopJson.encodeMap(updatedDueMap)
        val cleanLeadHours = reminderLeadHours.coerceIn(1, 336)

        dao.updateAssetDueDateOffline(
            assetId = assetId,
            dueDateIso = cleanIso,
            freqCode = cleanFreq,
            taskNote = taskNote.trim(),
            reminderLeadHours = cleanLeadHours,
            notificationsEnabled = notificationsEnabled,
            dueByFreqJson = dueByFreqJson,
            updatedAt = nowMs
        )

        val formattedDue = SopScheduleParser.fmtDT(cleanIso)
        val changes = mapOf(
            "nextDueDate" to listOf(
                current.nextDueDateIso.ifBlank { "None" },
                "$cleanFreq · $formattedDue (Notify ${cleanLeadHours}h before)"
            )
        )
        dao.insertLog(
            MaintenanceLogEntity(
                assetId = assetId,
                at = at,
                by = recorder,
                kind = "schedule",
                freq = cleanFreq,
                result = current.status,
                notes = taskNote.trim().ifBlank {
                    "Scheduled ${SopScheduleParser.freqLabel(cleanFreq)} ($cleanFreq) due date for $formattedDue"
                },
                changesJson = SopJson.encodeChanges(changes),
                syncedToServer = false
            )
        )
        dao.getAssetById(assetId)
    }

    suspend fun clearMaintenanceDueDate(
        assetId: String,
        by: String
    ): AssetEntity? = withContext(Dispatchers.IO) {
        val current = dao.getAssetById(assetId) ?: return@withContext null
        val nowMs = System.currentTimeMillis()
        dao.updateAssetDueDateOffline(
            assetId = assetId,
            dueDateIso = "",
            freqCode = current.nextDueFreqCode,
            taskNote = "",
            reminderLeadHours = current.reminderLeadHours,
            notificationsEnabled = false,
            dueByFreqJson = "{}",
            updatedAt = nowMs
        )
        dao.getAssetById(assetId)
    }

    suspend fun recordManualAuditActivity(
        assetId: String,
        activityDateIso: String,
        by: String,
        activityKind: String,
        freqCode: String,
        previousStatus: String,
        newStatus: String,
        summaryNotes: String,
        updateCurrentAssetStatus: Boolean = true
    ): AssetEntity? = withContext(Dispatchers.IO) {
        val current = dao.getAssetById(assetId) ?: return@withContext null
        val cleanAt = activityDateIso.trim().ifBlank { SopScheduleParser.nowIso() }
        val recorder = by.trim().ifBlank { "Duty Operator" }
        val resolvedPrevStatus = previousStatus.trim().ifBlank { current.status }
        val resolvedNewStatus = newStatus.trim().ifBlank { current.status }

        val changes = linkedMapOf<String, List<String>>()
        changes["status"] = listOf(resolvedPrevStatus, resolvedNewStatus)
        if (summaryNotes.isNotBlank()) {
            changes["notes"] = listOf(current.notes, summaryNotes.trim())
        }

        val logEntry = MaintenanceLogEntity(
            assetId = assetId,
            at = cleanAt,
            by = recorder,
            kind = activityKind.ifBlank { "check" },
            freq = if (activityKind == "check") freqCode.ifBlank { "D" } else "",
            result = resolvedNewStatus,
            itemsJson = if (activityKind == "check") {
                SopJson.encodeItems(
                    listOf(
                        ChecklistItemMark(
                            t = summaryNotes.trim().ifBlank { "Routine maintenance inspection" }.take(60),
                            s = if (resolvedNewStatus == "In service") "ok" else "nok"
                        )
                    )
                )
            } else {
                "[]"
            },
            readingsJson = "[]",
            notes = summaryNotes.trim(),
            changesJson = SopJson.encodeChanges(changes),
            syncedToServer = false
        )
        dao.insertLog(logEntry)

        if (updateCurrentAssetStatus) {
            val nowMs = System.currentTimeMillis()
            val updatedAsset = current.copy(
                status = resolvedNewStatus,
                lastCheckJson = if (activityKind == "check") {
                    SopJson.encodeLastCheck(
                        LastCheckInfo(
                            at = cleanAt,
                            freq = freqCode.ifBlank { "D" },
                            result = resolvedNewStatus,
                            by = recorder
                        )
                    )
                } else {
                    current.lastCheckJson
                },
                rev = current.rev + 1,
                lastUpdatedAt = nowMs,
                pendingOfflineSync = true
            )
            dao.upsertAsset(updatedAsset)
            updatedAsset
        } else {
            current
        }
    }

    suspend fun syncWithLanServer(baseUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().trimEnd('/')
        if (!cleanBase.startsWith("http://") && !cleanBase.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("Enter an address starting with http:// or https://"))
        }
        try {
            val req = Request.Builder()
                .url("$cleanBase/api/assets")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code} from $cleanBase"))
                }
                val bodyStr = resp.body?.string() ?: "[]"
                val jsonArr = SopJson.json.parseToJsonElement(bodyStr).jsonArray
                var count = 0
                for (elem in jsonArr) {
                    val obj = elem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: continue
                    val existing = dao.getAssetById(id)
                    val remoteRev = obj["rev"]?.jsonPrimitive?.intOrNull ?: 0
                    if (existing != null && remoteRev >= existing.rev) {
                        val updated = existing.copy(
                            status = obj["status"]?.jsonPrimitive?.content ?: existing.status,
                            criticality = obj["criticality"]?.jsonPrimitive?.content ?: existing.criticality,
                            location = obj["location"]?.jsonPrimitive?.content ?: existing.location,
                            make = obj["make"]?.jsonPrimitive?.content ?: existing.make,
                            model = obj["model"]?.jsonPrimitive?.content ?: existing.model,
                            serial = obj["serial"]?.jsonPrimitive?.content ?: existing.serial,
                            capacity = obj["capacity"]?.jsonPrimitive?.content ?: existing.capacity,
                            installed = obj["installed"]?.jsonPrimitive?.content ?: existing.installed,
                            amcVendor = obj["amcVendor"]?.jsonPrimitive?.content ?: existing.amcVendor,
                            amcExpiry = obj["amcExpiry"]?.jsonPrimitive?.content ?: existing.amcExpiry,
                            owner = obj["owner"]?.jsonPrimitive?.content ?: existing.owner,
                            notes = obj["notes"]?.jsonPrimitive?.content ?: existing.notes,
                            rev = remoteRev,
                            pendingOfflineSync = false
                        )
                        dao.upsertAsset(updated)
                        count++
                    }
                }
                dao.clearAllPendingOfflineSyncFlags()
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryPushToLanServer(baseUrl: String, asset: AssetEntity, log: MaintenanceLogEntity): Boolean {
        return try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            if (!cleanBase.startsWith("http://") && !cleanBase.startsWith("https://")) return false
            val assetJson = buildServerAssetJson(asset)
            val entryJson = buildServerLogJson(log)
            val csvRow = buildHistoryCsvRow(asset, log)
            val payload = "$assetJson\n$entryJson\n$csvRow"
            val req = Request.Builder()
                .url("$cleanBase/api/save/${asset.id}")
                .header("X-Requested-By", SopConstants.APP_NAME)
                .post(payload.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun persistLocallyThenSync(
        asset: AssetEntity,
        log: MaintenanceLogEntity,
        lanServerUrl: String
    ): AssetEntity {
        dao.upsertAsset(asset)
        val logId = dao.insertLog(log)
        if (lanServerUrl.isBlank() || !tryPushToLanServer(lanServerUrl, asset, log)) {
            return asset
        }

        dao.markAssetSyncedIfRevisionMatches(asset.id, asset.rev)
        dao.markLogSynced(logId)
        return dao.getAssetById(asset.id) ?: asset
    }

    private fun csvCell(v: String?): String {
        var s = (v ?: "").replace(Regex("""\s*\r?\n\s*"""), " / ")
        if (Regex("""^[=+\-@\t]""").containsMatchIn(s)) {
            s = "'$s"
        }
        return if (s.contains('"') || s.contains(',')) {
            "\"${s.replace("\"", "\"\"")}\""
        } else {
            s
        }
    }

    fun buildAssetsCsvString(assets: List<AssetEntity>): String {
        val freqHeaders = SopConstants.FREQ_ORDER.map { "Last ${SopConstants.FREQ_MAP[it]?.first ?: it}" }
        val head = listOf(
            "Asset ID", "Asset Name", "System", "Category", "Criticality", "Status",
            "Location", "Responsible Person", "Make", "Model", "Serial No", "Capacity",
            "Commissioned On", "AMC Vendor", "AMC Expiry", "Notes",
            "Last Check At", "Last Check Frequency", "Last Check Result", "Last Check By"
        ) + freqHeaders

        val rows = assets.sortedBy { it.id }.map { a ->
            val lc = a.lastCheck
            val lf = a.lastByFreq
            val baseCols = listOf(
                a.id, a.name, a.system, a.category, a.criticality, a.status,
                a.location, a.owner, a.make, a.model, a.serial, a.capacity,
                a.installed, a.amcVendor, a.amcExpiry, a.notes,
                SopScheduleParser.stamp(lc?.at),
                if (!lc?.freq.isNullOrBlank()) SopScheduleParser.freqLabel(lc!!.freq) else "",
                lc?.result ?: "",
                lc?.by ?: ""
            )
            val freqCols = SopConstants.FREQ_ORDER.map { code -> SopScheduleParser.stamp(lf[code]) }
            (baseCols + freqCols).joinToString(",") { csvCell(it) }
        }
        return (listOf(head.joinToString(",")) + rows).joinToString("\r\n") + "\r\n"
    }

    fun buildHistoryCsvRow(asset: AssetEntity, log: MaintenanceLogEntity): String {
        val items = log.items
        fun countMark(mark: String) = items.count { it.s == mark }.toString()
        val row = if (log.kind == "details") {
            val changeSummary = log.changes.entries.joinToString("; ") { (k, pair) ->
                val label = SopConstants.DETAIL_FIELD_LABELS[k] ?: k
                val oldV = pair.getOrNull(0)?.ifBlank { "-" } ?: "-"
                val newV = pair.getOrNull(1)?.ifBlank { "-" } ?: "-"
                "$label: $oldV -> $newV"
            }
            listOf(
                SopScheduleParser.stamp(log.at),
                asset.id,
                asset.name,
                asset.system,
                "Details updated",
                "",
                log.result,
                "",
                "",
                "",
                "",
                "",
                log.notes,
                changeSummary,
                log.by
            )
        } else {
            val faultItems = items.filter { it.s == "nok" }.joinToString("; ") { it.t }
            val readingItems = log.readings.joinToString("; ") { "${it.t}: ${it.v}" }
            listOf(
                SopScheduleParser.stamp(log.at),
                asset.id,
                asset.name,
                asset.system,
                "Check",
                SopScheduleParser.freqLabel(log.freq),
                log.result,
                countMark("ok"),
                countMark("nok"),
                countMark("na"),
                faultItems,
                readingItems,
                log.notes,
                "",
                log.by
            )
        }
        return row.joinToString(",") { csvCell(it) }
    }

    fun buildHistoryCsvString(assetsMap: Map<String, AssetEntity>, logs: List<MaintenanceLogEntity>): String {
        val head = "Recorded At,Asset ID,Asset Name,System,Entry Type,Frequency,Result,OK,Faults,NA,Fault Items,Readings,Notes,Changes,Recorded By"
        val rows = logs.mapNotNull { log ->
            val asset = assetsMap[log.assetId] ?: return@mapNotNull null
            buildHistoryCsvRow(asset, log)
        }
        return (listOf(head) + rows).joinToString("\r\n") + "\r\n"
    }

    suspend fun exportCsvFiles(context: Context): Pair<File, File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val assets = dao.getAllAssetsOnce()
        val logs = dao.getAllLogsOnce()
        val assetMap = assets.associateBy { it.id }

        val assetsFile = File(exportDir, "Assets.csv")
        assetsFile.writeText(buildAssetsCsvString(assets), Charsets.UTF_8)

        val historyFile = File(exportDir, "Maintenance_History.csv")
        historyFile.writeText(buildHistoryCsvString(assetMap, logs), Charsets.UTF_8)

        assetsFile to historyFile
    }

    suspend fun importAssetsFromCsv(
        rawCsvText: String,
        sourceName: String,
        conflictMode: com.example.util.CsvImportConflictMode,
        recordedBy: String
    ): Result<com.example.util.BulkCsvImportSummary> = withContext(Dispatchers.IO) {
        val currentAssets = dao.getAllAssetsOnce()
        val existingForParse = if (conflictMode == com.example.util.CsvImportConflictMode.REPLACE_ALL) {
            emptyList()
        } else {
            currentAssets
        }
        val preview = com.example.util.CsvAssetImporter.parseCsvText(
            rawCsvText = rawCsvText,
            sourceName = sourceName,
            existingAssets = existingForParse
        )
        if (preview.validRows.isEmpty()) {
            val firstErr = preview.rowErrors.firstOrNull()?.message
                ?: "No valid asset rows found in uploaded CSV."
            return@withContext Result.failure(IllegalArgumentException(firstErr))
        }

        val existingById = currentAssets.associateBy { it.id.uppercase() }
        val at = SopScheduleParser.nowIso()
        val recorder = recordedBy.ifBlank { "Duty Operator" }

        if (conflictMode == com.example.util.CsvImportConflictMode.REPLACE_ALL) {
            dao.deleteAllAssets()
        }

        var insertedCount = 0
        var updatedCount = 0
        var skippedCount = 0
        val importedIds = mutableListOf<String>()
        val assetsToUpsert = mutableListOf<AssetEntity>()
        val logsToInsert = mutableListOf<MaintenanceLogEntity>()

        for (row in preview.validRows) {
            val existing = existingById[row.asset.id.uppercase()]
            when (conflictMode) {
                com.example.util.CsvImportConflictMode.INSERT_ONLY_SKIP_EXISTING -> {
                    if (existing != null) {
                        skippedCount++
                        continue
                    } else {
                        insertedCount++
                    }
                }
                com.example.util.CsvImportConflictMode.UPSERT_MERGE -> {
                    if (existing != null) {
                        updatedCount++
                    } else {
                        insertedCount++
                    }
                }
                com.example.util.CsvImportConflictMode.REPLACE_ALL -> {
                    insertedCount++
                }
            }

            assetsToUpsert.add(row.asset)
            importedIds.add(row.asset.id)

            val prevStatus = if (conflictMode == com.example.util.CsvImportConflictMode.REPLACE_ALL) {
                ""
            } else {
                existing?.status ?: ""
            }
            val prevLoc = if (conflictMode == com.example.util.CsvImportConflictMode.REPLACE_ALL) {
                ""
            } else {
                existing?.location ?: ""
            }

            logsToInsert.add(
                MaintenanceLogEntity(
                    assetId = row.asset.id,
                    at = at,
                    by = recorder,
                    kind = "details",
                    result = row.asset.status,
                    notes = "Seeded / imported via Bulk CSV Import ($sourceName)",
                    changesJson = SopJson.encodeChanges(
                        mapOf(
                            "status" to listOf(prevStatus, row.asset.status),
                            "location" to listOf(prevLoc, row.asset.location)
                        )
                    ),
                    syncedToServer = false
                )
            )
        }

        if (assetsToUpsert.isNotEmpty()) {
            dao.insertAssets(assetsToUpsert)
            for (log in logsToInsert) {
                dao.insertLog(log)
            }
        }

        val summaryMsg = buildString {
            append("CSV Import Complete ($sourceName): ")
            val parts = mutableListOf<String>()
            if (insertedCount > 0) parts.add("$insertedCount new asset${if (insertedCount == 1) "" else "s"} seeded")
            if (updatedCount > 0) parts.add("$updatedCount updated")
            if (skippedCount > 0) parts.add("$skippedCount duplicate${if (skippedCount == 1) "" else "s"} skipped")
            if (preview.rowErrors.isNotEmpty()) parts.add("${preview.rowErrors.size} row error${if (preview.rowErrors.size == 1) "" else "s"}")
            append(parts.joinToString(", ").ifBlank { "No changes applied" })
        }

        Result.success(
            com.example.util.BulkCsvImportSummary(
                sourceName = sourceName,
                totalParsed = preview.validRows.size,
                insertedCount = insertedCount,
                updatedCount = updatedCount,
                skippedCount = skippedCount,
                errorCount = preview.rowErrors.size,
                importedAssetIds = importedIds,
                summaryMessage = summaryMsg
            )
        )
    }

    private fun buildServerAssetJson(a: AssetEntity): String {
        return SopJson.json.encodeToString(AssetEntity.serializer(), a)
    }

    private fun buildServerLogJson(e: MaintenanceLogEntity): String {
        return SopJson.json.encodeToString(MaintenanceLogEntity.serializer(), e)
    }
}
