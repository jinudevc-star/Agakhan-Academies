package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToLong

@Serializable
data class LastCheckInfo(
    val at: String = "",
    val freq: String = "",
    val result: String = "",
    val by: String = ""
)

@Serializable
data class ChecklistItemMark(
    val t: String,
    val s: String // "ok", "nok", "na"
)

@Serializable
data class ReadingValue(
    val t: String,
    val v: String
)

/**
 * Represents an individual physical equipment or furniture unit from the
 * AKAH Master Asset Register - FM (e.g., individual breaker, pump, RO unit,
 * AC unit, lift, residential appliance, or furniture item with its own Serial/Tag).
 */
@Serializable
data class SubUnitAssetItem(
    val unitId: String,
    val serNo: Int = 0,
    val building: String = "",
    val location: String = "",
    val room: String = "",
    val description: String,
    val model: String = "",
    val serialNo: String = "",
    val assetTag: String = "",
    val occupant: String = "",
    val quantity: Int = 1,
    val purchaseYear: Int = 2012,
    val lifeCycleYears: Int = 15,
    val status: String = "In service" // "In service", "Needs attention", "Out of service", "Damaged", "Shifted"
) {
    val currentAgeYears: Int
        get() = if (purchaseYear in 1990..2026) max(0, 2026 - purchaseYear) else 0

    val operationalEndYear: Int
        get() = if (purchaseYear in 1990..2026) purchaseYear + lifeCycleYears else 2026

    val lifeUsedPercent: Int
        get() = if (lifeCycleYears > 0 && purchaseYear in 1990..2026) {
            ((currentAgeYears * 100.0) / lifeCycleYears).roundToLong().toInt()
        } else 0
}

/**
 * Room Entity storing complete asset details for offline tracking,
 * including asset ID, name, building, location, room, asset tag, lifecycle,
 * individual sub-units, and current maintenance status.
 */
@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["name"]),
        Index(value = ["location"]),
        Index(value = ["building_name"]),
        Index(value = ["asset_tag"]),
        Index(value = ["maintenance_status"]),
        Index(value = ["system"])
    ]
)
@Serializable
data class AssetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    val sr: Int = 0,
    val cat: String = "GEN",
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "system")
    val system: String,
    val category: String = "",
    val criticality: String = "Medium",
    @ColumnInfo(name = "maintenance_status")
    val status: String = "In service",
    @ColumnInfo(name = "building_name")
    val buildingName: String = "",
    @ColumnInfo(name = "location")
    val location: String = "",
    @ColumnInfo(name = "room_no")
    val roomNo: String = "",
    val make: String = "",
    val model: String = "",
    val serial: String = "",
    @ColumnInfo(name = "asset_tag")
    val assetTag: String = "",
    val capacity: String = "",
    val quantity: Int = 1,
    val purchaseYear: Int = 2012,
    val lifeCycleYears: Int = 15,
    val installed: String = "",
    val amcVendor: String = "",
    val amcExpiry: String = "",
    val owner: String = "",
    val notes: String = "",
    val strategy: String = "PPM + operator routine inspection",
    val routine: String = "D: visual inspection, operational status check; W: clean and verify signage",
    val ppm: String = "M: functional check; Q: preventive service; A: full calibration & safety audit",
    val statutory: String = "SOP AKAH-EH-AM-A1-2026; OEM manual",
    val records: String = "Operating status; temperature °C; load/pressure reading",
    val limits: String = "Within OEM rated operating parameters",
    val lastCheckJson: String = "",
    val lastByFreqJson: String = "{}",
    val dueByFreqJson: String = "{}",
    @ColumnInfo(name = "next_due_date_iso")
    val nextDueDateIso: String = "",
    @ColumnInfo(name = "next_due_freq_code")
    val nextDueFreqCode: String = "D",
    @ColumnInfo(name = "next_due_task_note")
    val nextDueTaskNote: String = "",
    @ColumnInfo(name = "reminder_lead_hours")
    val reminderLeadHours: Int = 24,
    @ColumnInfo(name = "notifications_enabled")
    val notificationsEnabled: Boolean = true,
    val subUnitsJson: String = "[]",
    val rev: Int = 0,
    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "pending_offline_sync")
    val pendingOfflineSync: Boolean = false
) {
    val lastCheck: LastCheckInfo?
        get() = if (lastCheckJson.isBlank()) null else try {
            SopJson.json.decodeFromString<LastCheckInfo>(lastCheckJson)
        } catch (_: Exception) {
            null
        }

    val lastByFreq: Map<String, String>
        get() = if (lastByFreqJson.isBlank()) emptyMap() else try {
            SopJson.json.decodeFromString<Map<String, String>>(lastByFreqJson)
        } catch (_: Exception) {
            emptyMap()
        }

    val dueByFreq: Map<String, String>
        get() = if (dueByFreqJson.isBlank() || dueByFreqJson == "{}") emptyMap() else try {
            SopJson.json.decodeFromString<Map<String, String>>(dueByFreqJson)
        } catch (_: Exception) {
            emptyMap()
        }

    val subUnits: List<SubUnitAssetItem>
        get() = if (subUnitsJson.isBlank() || subUnitsJson == "[]") emptyList() else try {
            SopJson.json.decodeFromString<List<SubUnitAssetItem>>(subUnitsJson)
        } catch (_: Exception) {
            emptyList()
        }

    val currentOperationalLifeYears: Int
        get() = if (purchaseYear in 1990..2026) max(0, 2026 - purchaseYear) else 0

    val operationalLifeEndYear: Int
        get() = if (purchaseYear in 1990..2026) purchaseYear + lifeCycleYears else 2026

    val operationalLifePercent: Int
        get() = if (lifeCycleYears > 0 && purchaseYear in 1990..2026) {
            ((currentOperationalLifeYears * 100.0) / lifeCycleYears).roundToLong().toInt()
        } else 0
}

/**
 * Room Entity storing individual maintenance checks, readings, and status update history
 * linked to [AssetEntity] via ForeignKey for offline audit trail.
 */
@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["at"])
    ]
)
@Serializable
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val assetId: String,
    val at: String,
    val by: String,
    val kind: String, // "check" or "details"
    val freq: String = "",
    val result: String = "",
    val itemsJson: String = "[]",
    val readingsJson: String = "[]",
    val notes: String = "",
    val changesJson: String = "{}",
    @ColumnInfo(name = "synced_to_server")
    val syncedToServer: Boolean = false
) {
    val items: List<ChecklistItemMark>
        get() = try {
            SopJson.json.decodeFromString(itemsJson)
        } catch (_: Exception) {
            emptyList()
        }

    val readings: List<ReadingValue>
        get() = try {
            SopJson.json.decodeFromString(readingsJson)
        } catch (_: Exception) {
            emptyList()
        }

    val changes: Map<String, List<String>>
        get() = try {
            SopJson.json.decodeFromString(changesJson)
        } catch (_: Exception) {
            emptyMap()
        }

    val previousStatus: String
        get() = changes["status"]?.getOrNull(0)?.trim().orEmpty()

    val updatedStatus: String
        get() = changes["status"]?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: result

    val hasStatusTransition: Boolean
        get() {
            val prev = previousStatus
            val next = updatedStatus
            return prev.isNotBlank() && next.isNotBlank() && !prev.equals(next, ignoreCase = true)
        }

    val isSopCheckEntry: Boolean
        get() = kind == "check"

    val isDueDateEntry: Boolean
        get() = kind == "schedule" || changes.containsKey("nextDueDate") || changes.containsKey("nextDueDateIso") || changes.containsKey("nextDueFreqCode")

    val isStatusUpdateEntry: Boolean
        get() = changes.containsKey("status") || hasStatusTransition

    val activityTitle: String
        get() = when {
            kind == "check" -> "${SopScheduleParser.freqLabel(freq)} (${freq.ifBlank { "D" }}) SOP Check"
            isDueDateEntry -> "Maintenance Due Date Scheduled"
            changes.containsKey("status") && changes.size == 1 -> "Maintenance Status Update"
            changes.containsKey("status") && changes.containsKey("location") && previousStatus.isBlank() -> "Asset Registered Offline"
            changes.containsKey("status") -> "Status & Asset Details Update"
            else -> "Asset Details Update"
        }
}

/**
 * Room 1-to-Many relation joining an [AssetEntity] with its offline [MaintenanceLogEntity] history.
 */
data class AssetWithLogs(
    @Embedded val asset: AssetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "assetId"
    )
    val logs: List<MaintenanceLogEntity>
)

object SopJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeLastCheck(info: LastCheckInfo?): String =
        if (info == null) "" else json.encodeToString(info)

    fun encodeMap(map: Map<String, String>): String =
        json.encodeToString(map)

    fun encodeItems(items: List<ChecklistItemMark>): String =
        json.encodeToString(items)

    fun encodeReadings(readings: List<ReadingValue>): String =
        json.encodeToString(readings)

    fun encodeChanges(changes: Map<String, List<String>>): String =
        json.encodeToString(changes)

    fun encodeSubUnits(units: List<SubUnitAssetItem>): String =
        json.encodeToString(units)
}

data class ChecklistTaskItem(
    val text: String,
    val who: String // "Operator" or "Technician / AMC"
)

data class ChecklistFrequencyGroup(
    val code: String,
    val items: List<ChecklistTaskItem>
)

data class DueScheduleItem(
    val code: String,
    val lastIso: String?,
    val isLate: Boolean,
    val dueIso: String? = null,
    val isApproaching: Boolean = false
)

enum class DeadlineStatus {
    NONE,
    ON_TRACK,
    APPROACHING,
    OVERDUE
}

data class AssetDeadlineEvaluation(
    val status: DeadlineStatus,
    val dueAtMs: Long? = null,
    val dueIso: String? = null,
    val freqCode: String = "D",
    val taskNote: String = "",
    val remainingHours: Double? = null,
    val shortBadgeText: String = "",
    val detailedText: String = ""
)

object SopConstants {
    const val APP_NAME = "AKAH_SOP_Server"
    const val SOP_CODE = "AKAH-EH-AM-A1-2026"
    val ASSET_ID_REGEX = Regex("""AKA-HYD-[A-Z]+-\d{3}""")
    val EXACT_ASSET_ID_REGEX = Regex("""^AKA-HYD-[A-Z]+-\d{3}$""")
    val AKA_TAG_REGEX = Regex("""AKA/\d{5,7}|AKA-\d{2}[A-Z]?""")

    val FREQ_MAP: Map<String, Pair<String, Int>> = mapOf(
        "D" to ("Daily" to 1),
        "W" to ("Weekly" to 7),
        "F" to ("Fortnightly" to 14),
        "W/F" to ("Weekly / fortnightly" to 14),
        "M" to ("Monthly" to 31),
        "Q" to ("Quarterly" to 92),
        "H" to ("Half-yearly" to 183),
        "A" to ("Annual" to 366)
    )

    val FREQ_ORDER = listOf("D", "W", "W/F", "F", "M", "Q", "H", "A")
    val ROUTINE_OVERDUE_CODES = setOf("D", "W", "W/F", "F", "M")

    val STATUS_OPTIONS = listOf(
        "In service",
        "Needs attention",
        "Out of service",
        "Decommissioned"
    )
    val STATUSES = STATUS_OPTIONS

    val SUB_UNIT_STATUS_OPTIONS = listOf(
        "In service",
        "Needs attention",
        "Out of service",
        "Damaged",
        "Shifted"
    )

    val CRITICALITY_OPTIONS = listOf(
        "Critical",
        "Medium",
        "Non-Critical"
    )
    val CRITICALITIES = CRITICALITY_OPTIONS

    val SYSTEMS = listOf(
        "Electrical",
        "Power backup",
        "Renewable energy",
        "Water & wastewater",
        "Fire & life safety",
        "Lifts",
        "HVAC",
        "Refrigeration",
        "Kitchen",
        "Laundry & appliances",
        "Residential FM",
        "Furniture & fixtures"
    )

    val SYSTEM_CODES: Map<String, String> = mapOf(
        "Electrical" to "ELE",
        "Power backup" to "PWR",
        "Renewable energy" to "SOL",
        "Water & wastewater" to "WAT",
        "Plumbing & Water" to "WAT",
        "Fire & life safety" to "FIR",
        "Fire & Life Safety" to "FIR",
        "Lifts" to "LFT",
        "Vertical Transport & Utility" to "UTL",
        "HVAC" to "HVC",
        "Refrigeration" to "REF",
        "Kitchen" to "KIT",
        "Kitchen & Culinary" to "KIT",
        "Laundry & appliances" to "APL",
        "Residential FM" to "RES",
        "Furniture & fixtures" to "FRN",
        "Civil & Building Fabric" to "CIV",
        "Athletic & Pool" to "WAT"
    )

    val DETAIL_FIELD_LABELS = mapOf(
        "status" to "Status",
        "criticality" to "Criticality",
        "buildingName" to "Building Name",
        "location" to "Location (building / floor / room)",
        "roomNo" to "Room No.",
        "make" to "Make",
        "model" to "Model",
        "serial" to "Identification # / Serial no.",
        "assetTag" to "Asset Tag #",
        "capacity" to "Capacity / rating",
        "quantity" to "Quantity",
        "purchaseYear" to "Year of Purchase",
        "lifeCycleYears" to "Life cycle (in years)",
        "installed" to "Commissioned on",
        "amcVendor" to "AMC vendor & contract no.",
        "amcExpiry" to "AMC expiry",
        "owner" to "Responsible person / Occupant",
        "notes" to "Notes",
        "nextDueDate" to "Scheduled Due Date",
        "nextDueDateIso" to "Next SOP Due Date",
        "nextDueFreqCode" to "Scheduled SOP Frequency",
        "reminderLeadHours" to "Notification Lead Time (hours)"
    )
}

object SopScheduleParser {

    fun extractAssetIdFromQr(rawValue: String, allAssets: List<AssetEntity> = emptyList()): String? {
        val trimmed = rawValue.trim()
        val upper = trimmed.uppercase(Locale.US)
        val match = SopConstants.ASSET_ID_REGEX.find(upper)
        if (match != null) return match.value

        // Check if scanned QR / barcode matches an AKA/00... tag, serial number, or sub-unit tag
        if (allAssets.isNotEmpty()) {
            allAssets.find { a ->
                a.id.equals(trimmed, ignoreCase = true) ||
                    (a.assetTag.isNotBlank() && upper.contains(a.assetTag.uppercase(Locale.US))) ||
                    (a.serial.isNotBlank() && upper.contains(a.serial.uppercase(Locale.US))) ||
                    a.subUnits.any { u ->
                        (u.assetTag.isNotBlank() && upper.contains(u.assetTag.uppercase(Locale.US))) ||
                            (u.serialNo.isNotBlank() && u.serialNo.length >= 5 && upper.contains(u.serialNo.uppercase(Locale.US)))
                    }
            }?.let { return it.id }
        }
        return null
    }

    fun splitTop(s: String?, sep: Char): List<String> {
        if (s.isNullOrBlank()) return emptyList()
        val out = mutableListOf<String>()
        var depth = 0
        val cur = StringBuilder()
        for (ch in s) {
            if (ch == '(') depth++
            if (ch == ')') depth = max(0, depth - 1)
            if (ch == sep && depth == 0) {
                out.add(cur.toString())
                cur.clear()
            } else {
                cur.append(ch)
            }
        }
        out.add(cur.toString())
        return out.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun normCode(raw: String): String {
        val c = raw.replace(Regex("""\s+"""), " ").trim()
        val u = c.uppercase(Locale.US)
        if (SopConstants.FREQ_MAP.containsKey(u)) return u
        return c.replace(Regex("""\s*(yr|y|years?)$""", RegexOption.IGNORE_CASE), " yr")
            .replace(Regex("""\s*h$""", RegexOption.IGNORE_CASE), " h")
    }

    private data class RawScheduleGroup(
        val code: String,
        val items: MutableList<String> = mutableListOf()
    )

    private fun parseSched(text: String): List<RawScheduleGroup> {
        val out = mutableListOf<RawScheduleGroup>()
        var cur: RawScheduleGroup? = null
        val hqRegex = Regex("""^(\d+)\s*h\s+or\s+([DWFMQHA])\s*:\s*(.*)$""", RegexOption.IGNORE_CASE)
        val stdRegex = Regex("""^((?:\d+\s*(?:h|yr|y|years?))|[DWFMQHA](?:/[DWFMQHA])?)\s*:\s*(.*)$""", RegexOption.IGNORE_CASE)

        for (rawSeg in splitTop(text, ';')) {
            var seg = rawSeg
            val hqMatch = hqRegex.find(seg)
            if (hqMatch != null) {
                val hours = hqMatch.groupValues[1]
                val freqCode = hqMatch.groupValues[2].uppercase(Locale.US)
                val rest = hqMatch.groupValues[3]
                cur = RawScheduleGroup(code = freqCode)
                out.add(cur)
                for (it in splitTop(rest, ',')) {
                    cur.items.add("$it (or every $hours running hours, whichever is first)")
                }
                continue
            }
            val stdMatch = stdRegex.find(seg)
            if (stdMatch != null) {
                cur = RawScheduleGroup(code = normCode(stdMatch.groupValues[1]))
                out.add(cur)
                seg = stdMatch.groupValues[2]
            }
            if (cur == null) {
                cur = RawScheduleGroup(code = "Other")
                out.add(cur)
            }
            for (it in splitTop(seg, ',')) {
                cur.items.add(it)
            }
        }
        return out
    }

    private fun rank(c: String): Double {
        val idx = SopConstants.FREQ_ORDER.indexOf(c)
        if (idx >= 0) return idx.toDouble()
        val num = Regex("""\d+(\.\d+)?""").find(c)?.value?.toDoubleOrNull() ?: 0.0
        return when {
            c.contains("yr") -> 200.0 + num
            c.endsWith("h") -> 100.0 + num / 1000.0
            else -> 300.0
        }
    }

    fun checklist(asset: AssetEntity): List<ChecklistFrequencyGroup> {
        val map = linkedMapOf<String, MutableList<ChecklistTaskItem>>()
        val sources = listOf("Operator" to asset.routine, "Technician / AMC" to asset.ppm)
        for ((who, txt) in sources) {
            for (g in parseSched(txt)) {
                val list = map.getOrPut(g.code) { mutableListOf() }
                for (itemText in g.items) {
                    list.add(ChecklistTaskItem(text = itemText, who = who))
                }
            }
        }
        val sortedCodes = map.keys.sortedBy { rank(it) }
        return sortedCodes.map { code ->
            ChecklistFrequencyGroup(code = code, items = map[code] ?: emptyList())
        }
    }

    fun freqLabel(code: String): String {
        SopConstants.FREQ_MAP[code]?.let { return it.first }
        val num = Regex("""\d+""").find(code)?.value
        return when {
            code.contains("yr") && num != null -> "Every $num years"
            code.endsWith("h") && num != null -> "Every $num running hours"
            else -> code
        }
    }

    fun intervalDays(code: String): Double? {
        SopConstants.FREQ_MAP[code]?.let { return it.second.toDouble() }
        if (code.contains("yr")) {
            val num = Regex("""\d+(\.\d+)?""").find(code)?.value?.toDoubleOrNull()
            if (num != null) return num * 366.0
        }
        return null
    }

    fun readingsOf(asset: AssetEntity): List<String> {
        return splitTop(asset.records, ';')
            .map { it.removeSuffix(".").trim() }
            .filter { it.isNotEmpty() && it.length < 90 }
    }

    fun dueState(asset: AssetEntity, nowMs: Long = System.currentTimeMillis()): List<DueScheduleItem> {
        val out = mutableListOf<DueScheduleItem>()
        val lastMap = asset.lastByFreq
        val dueMap = asset.dueByFreq
        val leadMs = max(1, asset.reminderLeadHours) * 3_600_000L

        for (g in checklist(asset)) {
            val days = intervalDays(g.code) ?: continue
            val lastIso = lastMap[g.code]
            val lastMs = parseIsoMillis(lastIso)
            val explicitDueIso = dueMap[g.code]
                ?: if (asset.nextDueDateIso.isNotBlank() && asset.nextDueFreqCode == g.code) asset.nextDueDateIso else null
            val explicitDueMs = parseIsoMillis(explicitDueIso)
            val computedDueMs = explicitDueMs ?: lastMs?.let { it + (days * 86_400_000.0).roundToLong() }
            val computedDueIso = explicitDueIso ?: computedDueMs?.let { isoFromMillis(it) }

            val graceDays = if (g.code == "D") 0.0 else 1.0
            val late = when {
                explicitDueMs != null -> nowMs > explicitDueMs
                lastMs == null -> true
                else -> {
                    val elapsedDays = (nowMs - lastMs).toDouble() / 86_400_000.0
                    elapsedDays > (days + graceDays)
                }
            }
            val approaching = !late && computedDueMs != null && (computedDueMs - nowMs) in 0..leadMs
            out.add(
                DueScheduleItem(
                    code = g.code,
                    lastIso = lastIso,
                    isLate = late,
                    dueIso = computedDueIso,
                    isApproaching = approaching
                )
            )
        }
        return out
    }

    fun evaluateAssetDeadline(
        asset: AssetEntity,
        nowMs: Long = System.currentTimeMillis()
    ): AssetDeadlineEvaluation {
        val leadHours = max(1, asset.reminderLeadHours)
        val leadMs = leadHours * 3_600_000L

        // Check explicit nextDueDateIso first, or earliest dueByFreq entry
        val targetIso = asset.nextDueDateIso.ifBlank {
            asset.dueByFreq.entries
                .mapNotNull { (code, iso) -> parseIsoMillis(iso)?.let { Triple(code, iso, it) } }
                .minByOrNull { it.third }
                ?.second
                .orEmpty()
        }
        val targetFreq = if (asset.nextDueDateIso.isNotBlank()) {
            asset.nextDueFreqCode.ifBlank { "D" }
        } else {
            asset.dueByFreq.entries
                .mapNotNull { (code, iso) -> parseIsoMillis(iso)?.let { code to it } }
                .minByOrNull { it.second }
                ?.first
                ?: "D"
        }

        val dueMs = parseIsoMillis(targetIso)
        if (dueMs != null) {
            val diffMs = dueMs - nowMs
            val remHours = diffMs.toDouble() / 3_600_000.0
            val status = when {
                diffMs < 0L -> DeadlineStatus.OVERDUE
                diffMs <= leadMs -> DeadlineStatus.APPROACHING
                else -> DeadlineStatus.ON_TRACK
            }
            val timePhrase = formatTimeRemaining(dueMs, nowMs)
            val freqName = freqLabel(targetFreq)
            val shortBadge = when (status) {
                DeadlineStatus.OVERDUE -> "$targetFreq overdue ($timePhrase)"
                DeadlineStatus.APPROACHING -> "$targetFreq due $timePhrase"
                DeadlineStatus.ON_TRACK -> "$targetFreq due ${fmtD(targetIso)}"
                DeadlineStatus.NONE -> ""
            }
            val detailed = buildString {
                append("$freqName ($targetFreq) SOP check ")
                when (status) {
                    DeadlineStatus.OVERDUE -> append("overdue ($timePhrase) · Deadline was ${fmtDT(targetIso)}")
                    DeadlineStatus.APPROACHING -> append("approaching deadline ($timePhrase) · Due ${fmtDT(targetIso)}")
                    DeadlineStatus.ON_TRACK -> append("scheduled for ${fmtDT(targetIso)} ($timePhrase)")
                    DeadlineStatus.NONE -> {}
                }
            }
            return AssetDeadlineEvaluation(
                status = status,
                dueAtMs = dueMs,
                dueIso = targetIso,
                freqCode = targetFreq,
                taskNote = asset.nextDueTaskNote,
                remainingHours = remHours,
                shortBadgeText = shortBadge,
                detailedText = detailed
            )
        }

        // If no explicit due date is set, check if a lastCheck-derived schedule is approaching
        val derived = dueState(asset, nowMs).firstOrNull { it.dueIso != null }
        val derivedMs = parseIsoMillis(derived?.dueIso)
        if (derived != null && derivedMs != null) {
            val diffMs = derivedMs - nowMs
            val remHours = diffMs.toDouble() / 3_600_000.0
            val status = when {
                diffMs < 0L -> DeadlineStatus.OVERDUE
                diffMs <= leadMs -> DeadlineStatus.APPROACHING
                else -> DeadlineStatus.ON_TRACK
            }
            val timePhrase = formatTimeRemaining(derivedMs, nowMs)
            return AssetDeadlineEvaluation(
                status = status,
                dueAtMs = derivedMs,
                dueIso = derived.dueIso,
                freqCode = derived.code,
                taskNote = asset.nextDueTaskNote,
                remainingHours = remHours,
                shortBadgeText = when (status) {
                    DeadlineStatus.OVERDUE -> "${derived.code} overdue"
                    DeadlineStatus.APPROACHING -> "${derived.code} due $timePhrase"
                    else -> "${derived.code} due ${fmtD(derived.dueIso)}"
                },
                detailedText = "${freqLabel(derived.code)} (${derived.code}) due ${fmtDT(derived.dueIso)}"
            )
        }

        return AssetDeadlineEvaluation(status = DeadlineStatus.NONE)
    }

    fun isApproachingDeadline(asset: AssetEntity, nowMs: Long = System.currentTimeMillis()): Boolean {
        val eval = evaluateAssetDeadline(asset, nowMs)
        return eval.status == DeadlineStatus.APPROACHING
    }

    fun isLate(asset: AssetEntity, nowMs: Long = System.currentTimeMillis()): Boolean {
        val eval = evaluateAssetDeadline(asset, nowMs)
        if (eval.status == DeadlineStatus.OVERDUE) return true
        return dueState(asset, nowMs).any { it.isLate && it.code in SopConstants.ROUTINE_OVERDUE_CODES }
    }

    fun formatTimeRemaining(dueMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffMs = dueMs - nowMs
        val absMinutes = max(1L, (kotlin.math.abs(diffMs) / 60_000.0).roundToLong())
        val absHours = (absMinutes / 60.0).roundToLong()
        val absDays = (absHours / 24.0).roundToLong()
        return if (diffMs >= 0L) {
            when {
                absMinutes < 60L -> "in $absMinutes min"
                absHours < 48L -> "in $absHours h"
                else -> "in $absDays d"
            }
        } else {
            when {
                absMinutes < 60L -> "$absMinutes min ago"
                absHours < 48L -> "$absHours h overdue"
                else -> "$absDays d overdue"
            }
        }
    }

    fun isoFromMillis(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(ms))
    }

    fun parseIsoMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val d = sdf.parse(iso)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun fmtDT(iso: String?): String {
        val ms = parseIsoMillis(iso) ?: return if (iso.isNullOrBlank()) "—" else iso
        val sdf = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date(ms))
    }

    fun fmtD(iso: String?): String {
        val ms = parseIsoMillis(iso) ?: return if (iso.isNullOrBlank()) "—" else iso
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.UK)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date(ms))
    }

    fun stamp(iso: String?): String {
        val ms = parseIsoMillis(iso) ?: return if (iso.isNullOrBlank()) "" else iso
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date(ms))
    }

    fun ago(iso: String?, nowMs: Long = System.currentTimeMillis()): String {
        val ms = parseIsoMillis(iso) ?: return "never"
        val sec = max(0L, (nowMs - ms) / 1000L)
        return when {
            sec < 3600L -> "${max(1L, (sec / 60.0).roundToLong())} min ago"
            sec < 86400L -> "${(sec / 3600.0).roundToLong()} h ago"
            else -> "${(sec / 86400.0).roundToLong()} d ago"
        }
    }
}
