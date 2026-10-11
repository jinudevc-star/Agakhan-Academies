package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiSopService
import com.example.ai.ThinkingLevel
import com.example.data.SopDatabase
import com.example.data.SopRepository
import com.example.model.AssetEntity
import com.example.model.ChecklistItemMark
import com.example.model.MaintenanceLogEntity
import com.example.model.ReadingValue
import com.example.model.SopScheduleParser
import com.example.util.ReportExporter
import com.example.util.SopNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuickStatFilter {
    ALL,
    IN_SERVICE,
    NEEDS_ATTENTION,
    OUT_OF_SERVICE,
    OVERDUE,
    DUE_SOON
}

enum class SearchFieldScope(val label: String) {
    ALL("Name & Location"),
    NAME("Name Only"),
    LOCATION("Location Only")
}

data class AiCardState(
    val queryInput: String = "",
    val askedQuestion: String = "",
    val isBusy: Boolean = false,
    val answerText: String = "",
    val thinkingSummary: String = "",
    val errorText: String = "",
    val highThinkingEnabled: Boolean = true
)

data class SopUiState(
    val meName: String = "",
    val qrBaseAddress: String = "https://script.google.com/macros/s/AKAH_SOP_REGISTER/exec",
    val lanServerUrl: String = "",
    val searchQuery: String = "",
    val searchScope: SearchFieldScope = SearchFieldScope.ALL,
    val selectedLocation: String = "All Locations",
    val selectedSystem: String = "All",
    val quickFilter: QuickStatFilter = QuickStatFilter.ALL,
    val selectedAssetId: String? = null,
    val showWhoDialog: Boolean = false,
    val showAddAssetDialog: Boolean = false,
    val showBulkCsvImportDialog: Boolean = false,
    val isImportingCsv: Boolean = false,
    val lastCsvImportMessage: String? = null,
    val lastCsvImportIsError: Boolean = false,
    val showExportReportDialog: Boolean = false,
    val isExportingReport: Boolean = false,
    val lastExportReportMessage: String? = null,
    val showQuickStatusDialogForAssetId: String? = null,
    val showDueDateDialogForAssetId: String? = null,
    val showAuditLogForAssetId: String? = null,
    val flashMessage: String? = null,
    val isFlashError: Boolean = false,
    val isSaving: Boolean = false,
    val isSyncingLan: Boolean = false,
    val lanSyncMessage: String? = null,
    val registerAi: AiCardState = AiCardState(),
    val assetAi: AiCardState = AiCardState()
)

class SopViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("akah_sop_prefs", Context.MODE_PRIVATE)
    private val repository = SopRepository(SopDatabase.getInstance(application).sopDao())

    private val _uiState = MutableStateFlow(
        SopUiState(
            meName = prefs.getString("me_name", "R. Sharma") ?: "R. Sharma",
            qrBaseAddress = prefs.getString(
                "qr_base_address",
                "https://script.google.com/macros/s/AKAH_SOP_REGISTER/exec"
            ) ?: "https://script.google.com/macros/s/AKAH_SOP_REGISTER/exec",
            lanServerUrl = prefs.getString("lan_server_url", "") ?: ""
        )
    )
    val uiState: StateFlow<SopUiState> = _uiState.asStateFlow()

    val allAssets: StateFlow<List<AssetEntity>> = repository.allAssets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<MaintenanceLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingOfflineSyncCount: StateFlow<Int> = repository.pendingOfflineSyncCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val filteredAssets: StateFlow<List<AssetEntity>> = combine(
        allAssets,
        _uiState
    ) { assets, state ->
        val q = state.searchQuery.trim().lowercase()
        assets.filter { a ->
            val sysMatch = state.selectedSystem == "All" || a.system.equals(state.selectedSystem, ignoreCase = true)
            val locChipMatch = state.selectedLocation == "All Locations" ||
                a.location.equals(state.selectedLocation, ignoreCase = true)
            val qMatch = if (q.isEmpty()) {
                true
            } else {
                when (state.searchScope) {
                    SearchFieldScope.ALL ->
                        a.name.lowercase().contains(q) ||
                            a.location.lowercase().contains(q) ||
                            a.id.lowercase().contains(q) ||
                            a.system.lowercase().contains(q) ||
                            a.status.lowercase().contains(q)
                    SearchFieldScope.NAME ->
                        a.name.lowercase().contains(q) || a.id.lowercase().contains(q)
                    SearchFieldScope.LOCATION ->
                        a.location.lowercase().contains(q)
                }
            }
            val quickMatch = when (state.quickFilter) {
                QuickStatFilter.ALL -> true
                QuickStatFilter.IN_SERVICE -> a.status == "In service"
                QuickStatFilter.NEEDS_ATTENTION -> a.status == "Needs attention"
                QuickStatFilter.OUT_OF_SERVICE -> a.status == "Out of service"
                QuickStatFilter.OVERDUE -> SopScheduleParser.isLate(a)
                QuickStatFilter.DUE_SOON -> SopScheduleParser.isApproachingDeadline(a)
            }
            sysMatch && locChipMatch && qMatch && quickMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        SopNotificationHelper.ensureNotificationChannel(application)
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun setSearchScope(scope: SearchFieldScope) {
        _uiState.update { it.copy(searchScope = scope) }
    }

    fun setSelectedLocation(location: String) {
        _uiState.update { it.copy(selectedLocation = location) }
    }

    fun setSelectedSystem(system: String) {
        _uiState.update { it.copy(selectedSystem = system) }
    }

    fun resetAllFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchScope = SearchFieldScope.ALL,
                selectedLocation = "All Locations",
                selectedSystem = "All",
                quickFilter = QuickStatFilter.ALL
            )
        }
    }

    fun toggleQuickFilter(filter: QuickStatFilter) {
        _uiState.update {
            val next = if (it.quickFilter == filter) QuickStatFilter.ALL else filter
            it.copy(quickFilter = next)
        }
    }

    fun openAssetDetail(assetId: String?) {
        _uiState.update {
            it.copy(
                selectedAssetId = assetId,
                flashMessage = null,
                isFlashError = false,
                assetAi = AiCardState()
            )
        }
    }

    fun setShowWhoDialog(show: Boolean) {
        _uiState.update { it.copy(showWhoDialog = show) }
    }

    fun setShowAddAssetDialog(show: Boolean) {
        _uiState.update { it.copy(showAddAssetDialog = show) }
    }

    fun setShowBulkCsvImportDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showBulkCsvImportDialog = show,
                lastCsvImportMessage = if (show) it.lastCsvImportMessage else null,
                lastCsvImportIsError = if (show) it.lastCsvImportIsError else false
            )
        }
    }

    fun setShowExportReportDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showExportReportDialog = show,
                lastExportReportMessage = if (show) it.lastExportReportMessage else null
            )
        }
    }

    fun saveMeName(name: String) {
        val trimmed = name.trim().take(60)
        if (trimmed.isEmpty()) return
        prefs.edit().putString("me_name", trimmed).apply()
        _uiState.update { it.copy(meName = trimmed, showWhoDialog = false) }
    }

    fun openQuickStatusDialog(assetId: String?) {
        _uiState.update { it.copy(showQuickStatusDialogForAssetId = assetId) }
    }

    fun openDueDateDialog(assetId: String?) {
        _uiState.update { it.copy(showDueDateDialogForAssetId = assetId) }
    }

    fun openAuditLogView(assetId: String?) {
        _uiState.update { it.copy(showAuditLogForAssetId = assetId) }
    }

    fun recordManualAuditActivity(
        assetId: String,
        activityDateIso: String,
        by: String,
        activityKind: String,
        freqCode: String,
        previousStatus: String,
        newStatus: String,
        summaryNotes: String,
        updateCurrentAssetStatus: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = repository.recordManualAuditActivity(
                assetId = assetId,
                activityDateIso = activityDateIso,
                by = by.ifBlank { _uiState.value.meName.ifBlank { "Duty Operator" } },
                activityKind = activityKind,
                freqCode = freqCode,
                previousStatus = previousStatus,
                newStatus = newStatus,
                summaryNotes = summaryNotes,
                updateCurrentAssetStatus = updateCurrentAssetStatus
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    flashMessage = if (updated != null) {
                        "Audit log entry recorded for $assetId ($previousStatus → $newStatus) · ${SopScheduleParser.fmtDT(activityDateIso)}"
                    } else {
                        "Failed to record audit log entry"
                    },
                    isFlashError = updated == null
                )
            }
            if (updated != null) onComplete()
        }
    }

    fun saveAssetMaintenanceDueDate(
        context: Context,
        assetId: String,
        freqCode: String,
        dueDateIso: String,
        reminderLeadHours: Int,
        notificationsEnabled: Boolean,
        taskNote: String,
        sendTestNotificationNow: Boolean = false
    ) {
        val by = _uiState.value.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = repository.setMaintenanceDueDate(
                assetId = assetId,
                freqCode = freqCode,
                dueDateIso = dueDateIso,
                reminderLeadHours = reminderLeadHours,
                notificationsEnabled = notificationsEnabled,
                taskNote = taskNote,
                by = by
            )
            if (updated != null) {
                val notifiedImmediately = SopNotificationHelper.scheduleOrTriggerDeadlineNotification(
                    context = context,
                    asset = updated,
                    forceImmediateNotification = sendTestNotificationNow
                )
                val formattedDue = SopScheduleParser.fmtDT(dueDateIso)
                val notifSuffix = if (notifiedImmediately) {
                    " · Local notification sent"
                } else if (notificationsEnabled) {
                    " · Reminder scheduled (${reminderLeadHours}h before)"
                } else {
                    ""
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showDueDateDialogForAssetId = null,
                        flashMessage = "Due date set for ${updated.id} (${SopScheduleParser.freqLabel(freqCode)}): $formattedDue$notifSuffix",
                        isFlashError = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        flashMessage = "Failed to save maintenance due date",
                        isFlashError = true
                    )
                }
            }
        }
    }

    fun clearAssetMaintenanceDueDate(context: Context, assetId: String) {
        val by = _uiState.value.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            val updated = repository.clearMaintenanceDueDate(assetId, by)
            if (updated != null) {
                SopNotificationHelper.scheduleOrTriggerDeadlineNotification(
                    context = context,
                    asset = updated,
                    forceImmediateNotification = false
                )
            }
            _uiState.update {
                it.copy(
                    showDueDateDialogForAssetId = null,
                    flashMessage = "Cleared scheduled maintenance due date for $assetId",
                    isFlashError = false
                )
            }
        }
    }

    fun notifyApproachingDeadlines(context: Context) {
        val assets = allAssets.value
        val notified = SopNotificationHelper.checkAndNotifyApproachingDeadlines(context, assets)
        val msg = if (notified.isNotEmpty()) {
            "Sent ${notified.size} local notification${if (notified.size == 1) "" else "s"} for approaching SOP deadlines (${notified.joinToString(", ") { it.id }})"
        } else {
            "All scheduled SOP tasks are outside their notification alert window."
        }
        _uiState.update {
            it.copy(
                flashMessage = msg,
                isFlashError = false
            )
        }
    }

    fun suggestNextAssetId(system: String, assets: List<AssetEntity> = allAssets.value): String {
        val prefixCode = when (system) {
            "Electrical" -> "ELE"
            "HVAC" -> "HVC"
            "Plumbing & Water" -> "WAT"
            "Fire & Life Safety" -> "FIR"
            "Kitchen & Culinary" -> "KIT"
            "Vertical Transport & Utility" -> "UTL"
            "Civil & Building Fabric" -> "CIV"
            "Athletic & Pool" -> "pol".uppercase()
            else -> "GEN"
        }
        val sysCode = if (prefixCode == "POL") "WAT" else prefixCode
        val fullPrefix = "AKA-HYD-$sysCode-"
        val maxExisting = assets
            .mapNotNull { asset ->
                if (asset.id.startsWith(fullPrefix)) {
                    asset.id.removePrefix(fullPrefix).toIntOrNull()
                } else null
            }
            .maxOrNull() ?: 0
        return String.format("AKA-HYD-%s-%03d", sysCode, maxExisting + 1)
    }

    fun addNewAssetOffline(
        id: String,
        name: String,
        system: String,
        location: String,
        criticality: String,
        status: String,
        owner: String,
        notes: String,
        make: String = "",
        model: String = "",
        capacity: String = "",
        routine: String = "",
        ppm: String = "",
        openAfterSave: Boolean = false,
        onError: (String) -> Unit,
        onCreated: (AssetEntity) -> Unit = {}
    ) {
        val by = _uiState.value.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            val res = repository.addNewAssetOffline(
                id = id,
                name = name,
                system = system,
                location = location,
                criticality = criticality,
                status = status,
                owner = owner,
                notes = notes,
                by = by,
                make = make,
                model = model,
                capacity = capacity,
                routine = routine,
                ppm = ppm
            )
            res.fold(
                onSuccess = { created ->
                    _uiState.update {
                        it.copy(
                            showAddAssetDialog = false,
                            selectedAssetId = if (openAfterSave) created.id else it.selectedAssetId,
                            flashMessage = "Asset ${created.id} (${created.name}) added with QR label ready",
                            isFlashError = false
                        )
                    }
                    onCreated(created)
                },
                onFailure = { err ->
                    onError(err.message ?: "Failed to add asset")
                }
            )
        }
    }

    fun saveQrBaseAddress(address: String) {
        val trimmed = address.trim().trimEnd('/')
        if (trimmed.isEmpty()) return
        prefs.edit().putString("qr_base_address", trimmed).apply()
        _uiState.update {
            it.copy(
                qrBaseAddress = trimmed,
                flashMessage = "Saved. All QR codes now point to $trimmed",
                isFlashError = false
            )
        }
    }

    fun saveLanServerUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        prefs.edit().putString("lan_server_url", trimmed).apply()
        _uiState.update { it.copy(lanServerUrl = trimmed, lanSyncMessage = null) }
    }

    fun syncWithLanServer(url: String) {
        val trimmed = url.trim().trimEnd('/')
        saveLanServerUrl(trimmed)
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(lanSyncMessage = "Cleared LAN server address (running in local offline mode).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingLan = true, lanSyncMessage = "Connecting to $trimmed…") }
            val result = repository.syncWithLanServer(trimmed)
            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isSyncingLan = false,
                            lanSyncMessage = "Synced $count assets from AKAH_SOP_Server at $trimmed"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSyncingLan = false,
                            lanSyncMessage = "Could not reach $trimmed (${err.message ?: "offline"}). Local register remains active."
                        )
                    }
                }
            )
        }
    }

    fun buildAssetQrPayload(assetId: String): String {
        val base = _uiState.value.qrBaseAddress.trim().trimEnd('/')
        return if (base.contains("script.google.com")) {
            "$base?asset=$assetId"
        } else {
            "$base/#$assetId"
        }
    }

    fun recordCheck(
        assetId: String,
        freqCode: String,
        resultStatus: String,
        marks: List<ChecklistItemMark>,
        readings: List<ReadingValue>,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        val state = _uiState.value
        val by = state.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, flashMessage = null) }
            val updated = repository.recordCheck(
                assetId = assetId,
                by = by,
                freqCode = freqCode,
                resultStatus = resultStatus,
                marks = marks,
                readings = readings,
                notes = notes,
                lanServerUrl = state.lanServerUrl
            )
            val msg = if (updated != null) {
                "${SopScheduleParser.freqLabel(freqCode)} check saved offline · ${SopScheduleParser.fmtDT(updated.lastCheck?.at)}"
            } else {
                "Could not save check."
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    flashMessage = msg,
                    isFlashError = updated == null
                )
            }
            if (updated != null) onComplete()
        }
    }

    fun quickUpdateAssetStatus(
        assetId: String,
        newStatus: String,
        newLocation: String? = null,
        note: String,
        onComplete: () -> Unit = {}
    ) {
        val state = _uiState.value
        val by = state.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = repository.quickUpdateStatus(
                assetId = assetId,
                newStatus = newStatus,
                newLocation = newLocation,
                by = by,
                note = note,
                lanServerUrl = state.lanServerUrl
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    showQuickStatusDialogForAssetId = null,
                    flashMessage = if (updated != null) "Offline status updated to $newStatus · ${SopScheduleParser.fmtDT(SopScheduleParser.nowIso())}" else "Failed to update status",
                    isFlashError = updated == null
                )
            }
            if (updated != null) onComplete()
        }
    }

    fun saveAssetDetails(
        assetId: String,
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
        onComplete: () -> Unit = {}
    ) {
        val state = _uiState.value
        val by = state.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = repository.updateAssetDetails(
                assetId = assetId,
                by = by,
                status = status,
                criticality = criticality,
                location = location,
                make = make,
                model = model,
                serial = serial,
                capacity = capacity,
                installed = installed,
                amcVendor = amcVendor,
                amcExpiry = amcExpiry,
                owner = owner,
                notes = notes,
                lanServerUrl = state.lanServerUrl
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    flashMessage = if (updated != null) "Asset details saved offline · ${SopScheduleParser.fmtDT(SopScheduleParser.nowIso())}" else "Failed to save asset details",
                    isFlashError = updated == null
                )
            }
            if (updated != null) onComplete()
        }
    }

    fun updateRegisterAiQuery(q: String) {
        _uiState.update { it.copy(registerAi = it.registerAi.copy(queryInput = q)) }
    }

    fun toggleRegisterAiHighThinking(enabled: Boolean) {
        _uiState.update { it.copy(registerAi = it.registerAi.copy(highThinkingEnabled = enabled)) }
    }

    fun askRegisterAi(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _uiState.value.registerAi.isBusy) return
        val useHigh = _uiState.value.registerAi.highThinkingEnabled
        val level = if (useHigh) ThinkingLevel.HIGH else ThinkingLevel.LOW
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    registerAi = it.registerAi.copy(
                        queryInput = q,
                        askedQuestion = q,
                        isBusy = true,
                        answerText = "",
                        thinkingSummary = "",
                        errorText = ""
                    )
                )
            }
            val contextStr = GeminiSopService.buildRegisterContext(allAssets.value)
            val result = GeminiSopService.askMaintenanceAi(
                question = q,
                recordContext = contextStr,
                thinkingLevel = level
            )
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            registerAi = it.registerAi.copy(
                                isBusy = false,
                                answerText = res.answerText,
                                thinkingSummary = res.thinkingSummary,
                                errorText = ""
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            registerAi = it.registerAi.copy(
                                isBusy = false,
                                errorText = err.message ?: "Could not reach Gemini AI service."
                            )
                        )
                    }
                }
            )
        }
    }

    fun updateAssetAiQuery(q: String) {
        _uiState.update { it.copy(assetAi = it.assetAi.copy(queryInput = q)) }
    }

    fun toggleAssetAiHighThinking(enabled: Boolean) {
        _uiState.update { it.copy(assetAi = it.assetAi.copy(highThinkingEnabled = enabled)) }
    }

    fun askAssetAi(asset: AssetEntity, logs: List<MaintenanceLogEntity>, question: String) {
        val q = question.trim()
        if (q.isEmpty() || _uiState.value.assetAi.isBusy) return
        val useHigh = _uiState.value.assetAi.highThinkingEnabled
        val level = if (useHigh) ThinkingLevel.HIGH else ThinkingLevel.LOW
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    assetAi = it.assetAi.copy(
                        queryInput = q,
                        askedQuestion = q,
                        isBusy = true,
                        answerText = "",
                        thinkingSummary = "",
                        errorText = ""
                    )
                )
            }
            val contextStr = GeminiSopService.buildAssetContext(asset, logs)
            val result = GeminiSopService.askMaintenanceAi(
                question = q,
                recordContext = contextStr,
                thinkingLevel = level
            )
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            assetAi = it.assetAi.copy(
                                isBusy = false,
                                answerText = res.answerText,
                                thinkingSummary = res.thinkingSummary,
                                errorText = ""
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            assetAi = it.assetAi.copy(
                                isBusy = false,
                                errorText = err.message ?: "Could not reach Gemini AI service."
                            )
                        )
                    }
                }
            )
        }
    }

    fun exportAndShareReport(
        context: Context,
        format: String,
        useFilteredList: Boolean,
        includeHistory: Boolean
    ) {
        val state = _uiState.value
        val assetsToExport = if (useFilteredList) filteredAssets.value else allAssets.value
        val logsToExport = allLogs.value
        val filterSummary = if (!useFilteredList) {
            "Full Campus Register (${assetsToExport.size} assets)"
        } else {
            buildList {
                if (state.selectedSystem != "All") add("System: ${state.selectedSystem}")
                if (state.selectedLocation != "All Locations") add("Location: ${state.selectedLocation}")
                if (state.quickFilter != QuickStatFilter.ALL) add("Filter: ${state.quickFilter.name}")
                if (state.searchQuery.isNotBlank()) add("Search: \"${state.searchQuery}\"")
            }.joinToString(" · ").ifBlank { "Current Asset List (${assetsToExport.size} assets)" }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExportingReport = true, lastExportReportMessage = null) }
            try {
                val result = if (format.equals("PDF", ignoreCase = true)) {
                    ReportExporter.exportPdfReport(
                        context = context,
                        assets = assetsToExport,
                        logs = logsToExport,
                        filterSummary = filterSummary,
                        operatorName = state.meName.ifBlank { "Duty Operator" },
                        includeRecentLogs = includeHistory
                    )
                } else {
                    ReportExporter.exportCsvReport(
                        context = context,
                        assets = assetsToExport,
                        logs = logsToExport,
                        includeHistoryCsv = includeHistory
                    )
                }

                val authority = "${context.packageName}.fileprovider"
                val primaryUri = FileProvider.getUriForFile(context, authority, result.primaryFile)

                val shareIntent = if (result.secondaryFile != null) {
                    val secondaryUri = FileProvider.getUriForFile(context, authority, result.secondaryFile)
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = result.mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(listOf(primaryUri, secondaryUri)))
                        putExtra(Intent.EXTRA_SUBJECT, "AKAH SOP Register — ${result.formatLabel} Report")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND).apply {
                        type = result.mimeType
                        putExtra(Intent.EXTRA_STREAM, primaryUri)
                        putExtra(Intent.EXTRA_SUBJECT, "AKAH SOP Register — ${result.formatLabel} Report")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }

                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        lastExportReportMessage = result.summaryMessage,
                        flashMessage = result.summaryMessage,
                        isFlashError = false
                    )
                }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Export ${result.formatLabel} Report")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        lastExportReportMessage = "Export error: ${e.message}",
                        flashMessage = "Export error: ${e.message}",
                        isFlashError = true
                    )
                }
            }
        }
    }

    fun shareExportedCsvs(context: Context) {
        exportAndShareReport(
            context = context,
            format = "CSV",
            useFilteredList = false,
            includeHistory = true
        )
    }

    fun exportSingleAssetAuditReport(
        context: Context,
        asset: AssetEntity,
        logs: List<MaintenanceLogEntity>,
        format: String
    ) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingReport = true) }
            try {
                val result = if (format.equals("PDF", ignoreCase = true)) {
                    ReportExporter.exportPdfReport(
                        context = context,
                        assets = listOf(asset),
                        logs = logs,
                        filterSummary = "Single Asset Audit Trail: ${asset.id} (${asset.name})",
                        operatorName = state.meName.ifBlank { "Duty Operator" },
                        includeRecentLogs = true
                    )
                } else {
                    ReportExporter.exportCsvReport(
                        context = context,
                        assets = listOf(asset),
                        logs = logs,
                        includeHistoryCsv = true
                    )
                }
                val authority = "${context.packageName}.fileprovider"
                val primaryUri = FileProvider.getUriForFile(context, authority, result.primaryFile)
                val shareIntent = if (result.secondaryFile != null) {
                    val secondaryUri = FileProvider.getUriForFile(context, authority, result.secondaryFile)
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = result.mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(listOf(primaryUri, secondaryUri)))
                        putExtra(Intent.EXTRA_SUBJECT, "AKAH Audit Log — ${asset.id} (${result.formatLabel})")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND).apply {
                        type = result.mimeType
                        putExtra(Intent.EXTRA_STREAM, primaryUri)
                        putExtra(Intent.EXTRA_SUBJECT, "AKAH Audit Log — ${asset.id} (${result.formatLabel})")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        flashMessage = "Exported ${asset.id} audit trail (${logs.size} log entries) as ${result.formatLabel}",
                        isFlashError = false
                    )
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Export ${asset.id} Audit Report")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        flashMessage = "Audit export error: ${e.message}",
                        isFlashError = true
                    )
                }
            }
        }
    }

    fun exportAndShareQrLabelsPdf(
        context: Context,
        assetsToExport: List<AssetEntity>,
        customFileName: String = "AKAH_Asset_QR_Labels.pdf"
    ) {
        val targetAssets = if (assetsToExport.isEmpty()) allAssets.value else assetsToExport
        if (targetAssets.isEmpty()) {
            _uiState.update {
                it.copy(
                    flashMessage = "No assets available to export QR code PDF.",
                    isFlashError = true
                )
            }
            return
        }
        val baseAddr = _uiState.value.qrBaseAddress
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingReport = true, lastExportReportMessage = null) }
            try {
                val result = ReportExporter.exportQrLabelsPdf(
                    context = context,
                    assets = targetAssets,
                    buildAssetQrUrl = { id -> buildAssetQrPayload(id) },
                    qrBaseAddress = baseAddr,
                    fileName = customFileName
                )
                val authority = "${context.packageName}.fileprovider"
                val pdfUri = FileProvider.getUriForFile(context, authority, result.primaryFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "AKAH Physical Asset QR Labels (${result.exportedAssetCount} Assets)"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        lastExportReportMessage = result.summaryMessage,
                        flashMessage = result.summaryMessage,
                        isFlashError = false
                    )
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Download / Share Printable QR Labels PDF")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        lastExportReportMessage = "QR PDF export error: ${e.message}",
                        flashMessage = "QR PDF export error: ${e.message}",
                        isFlashError = true
                    )
                }
            }
        }
    }

    fun importAssetsFromCsvText(
        rawCsvText: String,
        sourceName: String = "Uploaded CSV",
        conflictMode: com.example.util.CsvImportConflictMode = com.example.util.CsvImportConflictMode.UPSERT_MERGE,
        closeDialogOnSuccess: Boolean = false,
        onComplete: (com.example.util.BulkCsvImportSummary?) -> Unit = {}
    ) {
        val recorder = _uiState.value.meName.ifBlank { "Duty Operator" }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImportingCsv = true,
                    lastCsvImportMessage = null,
                    lastCsvImportIsError = false
                )
            }
            val result = repository.importAssetsFromCsv(
                rawCsvText = rawCsvText,
                sourceName = sourceName,
                conflictMode = conflictMode,
                recordedBy = recorder
            )
            result.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            isImportingCsv = false,
                            showBulkCsvImportDialog = if (closeDialogOnSuccess) false else it.showBulkCsvImportDialog,
                            lastCsvImportMessage = summary.summaryMessage,
                            lastCsvImportIsError = false,
                            flashMessage = summary.summaryMessage,
                            isFlashError = false
                        )
                    }
                    onComplete(summary)
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Failed to import CSV file."
                    _uiState.update {
                        it.copy(
                            isImportingCsv = false,
                            lastCsvImportMessage = errMsg,
                            lastCsvImportIsError = true,
                            flashMessage = "CSV Import failed: $errMsg",
                            isFlashError = true
                        )
                    }
                    onComplete(null)
                }
            )
        }
    }

    fun shareSampleCsvTemplate(context: Context) {
        viewModelScope.launch {
            try {
                val file = com.example.util.CsvAssetImporter.writeSampleTemplateFile(context)
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "AKAH Asset Import Template (CSV)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val msg = "Generated AKAH_Asset_Import_Template.csv with 5 sample campus assets."
                _uiState.update {
                    it.copy(
                        lastCsvImportMessage = msg,
                        lastCsvImportIsError = false,
                        flashMessage = msg,
                        isFlashError = false
                    )
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Download / Share Blank CSV Template")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        lastCsvImportMessage = "Template error: ${e.message}",
                        lastCsvImportIsError = true
                    )
                }
            }
        }
    }
}
