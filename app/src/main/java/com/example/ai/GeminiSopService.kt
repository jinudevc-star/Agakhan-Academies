package com.example.ai

import com.example.BuildConfig
import com.example.model.AssetEntity
import com.example.model.MaintenanceLogEntity
import com.example.model.SopConstants
import com.example.model.SopScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Date
import java.util.concurrent.TimeUnit

@Serializable
enum class ThinkingLevel {
    @SerialName("LOW")
    LOW,
    @SerialName("MEDIUM")
    MEDIUM,
    @SerialName("HIGH")
    HIGH
}

@Serializable
data class ThinkingConfig(
    val thinkingLevel: ThinkingLevel = ThinkingLevel.HIGH
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = 0.2f,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class Part(
    val text: String? = null,
    val thought: Boolean? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    @SerialName("system_instruction")
    val systemInstruction: Content? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

data class AiAskResult(
    val answerText: String,
    val thinkingSummary: String = "",
    val modelUsed: String = GeminiSopService.MODEL_ID
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiSopService {
    const val MODEL_ID = "gemini-3.1-pro-preview"

    val AI_ASSET_PROMPTS = listOf(
        "Summarise this asset's history and current condition",
        "What is overdue and what should be checked next?",
        "Explain the limits and what to do if one is exceeded"
    )

    val AI_REGISTER_PROMPTS = listOf(
        "Which assets need attention first, and why?",
        "Summarise the overall maintenance position",
        "Plan a high-priority inspection route for critical overdue systems"
    )

    private const val AI_SYSTEM_INSTRUCTION =
        "You are a maintenance assistant for the facilities team of a school campus. " +
            "Answer only from the RECORD you are given: the asset register entry, its maintenance standard and its recorded history. " +
            "If the RECORD does not contain what is asked, say that it is not recorded. Never invent readings, dates, names, part numbers, costs or standards. " +
            "Where no check has been recorded, say so plainly instead of assuming the asset is healthy or faulty. " +
            "For electrical, high-voltage, gas, fire-system or confined-space work, remind the reader to follow the permit-to-work and use the licensed people named in the RECORD. " +
            "Be brief and practical. Write plain text with short lines and simple dashes for lists. Do not use Markdown symbols such as asterisks or hashes."

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun buildAssetContext(asset: AssetEntity, logs: List<MaintenanceLogEntity>): String {
        val dues = SopScheduleParser.dueState(asset).joinToString("; ") { d ->
            val lastStr = if (d.lastIso != null) SopScheduleParser.stamp(d.lastIso) else "never recorded"
            val lateStr = if (d.isLate) " (OVERDUE)" else ""
            "${SopScheduleParser.freqLabel(d.code)}: last $lastStr$lateStr"
        }
        val histLines = logs.take(25).map { e ->
            if (e.kind == "details") {
                val chg = e.changes.entries.joinToString("; ") { (k, v) ->
                    "${SopConstants.DETAIL_FIELD_LABELS[k] ?: k}: ${v.getOrNull(0)?.ifBlank { "-" } ?: "-"} -> ${v.getOrNull(1)?.ifBlank { "-" } ?: "-"}"
                }
                "${SopScheduleParser.stamp(e.at)} details edited by ${e.by.ifBlank { "someone" }}: $chg"
            } else {
                val faults = e.items.filter { it.s == "nok" }.joinToString(", ") { it.t }.ifBlank { "none" }
                val readings = e.readings.joinToString(", ") { "${it.t}=${it.v}" }.ifBlank { "none" }
                val notes = e.notes.ifBlank { "none" }
                "${SopScheduleParser.stamp(e.at)} ${SopScheduleParser.freqLabel(e.freq)} check by ${e.by.ifBlank { "someone" }}, result ${e.result}; faults: $faults; readings: $readings; notes: $notes"
            }
        }
        fun nr(v: String) = v.ifBlank { "not recorded" }
        val lines = mutableListOf(
            "Today: ${SopScheduleParser.stamp(SopScheduleParser.nowIso())}",
            "Asset ${asset.id}: ${asset.name}",
            "System: ${asset.system}; category: ${asset.category}; criticality: ${asset.criticality}; status: ${asset.status}",
            "Location: ${nr(asset.location)}; responsible person: ${nr(asset.owner)}; make: ${nr(asset.make)}; model: ${nr(asset.model)}; serial: ${nr(asset.serial)}; capacity: ${nr(asset.capacity)}",
            "Commissioned: ${nr(asset.installed)}; AMC vendor: ${nr(asset.amcVendor)}; AMC expiry: ${nr(asset.amcExpiry)}; notes: ${nr(asset.notes)}",
            "Schedule status: ${dues.ifBlank { "no schedule" }}",
            "Maintenance strategy: ${asset.strategy}",
            "Operator routine: ${asset.routine}",
            "Preventive maintenance: ${asset.ppm}",
            "Data to record: ${asset.records}",
            "Limits: ${nr(asset.limits)}",
            "Standards and statutory: ${asset.statutory}",
            "History, newest first (${logs.size} entries in total):"
        )
        if (histLines.isEmpty()) {
            lines.add("No checks or edits have been recorded yet.")
        } else {
            lines.addAll(histLines)
        }
        return lines.joinToString("\n")
    }

    fun buildRegisterContext(assets: List<AssetEntity>): String {
        val sorted = assets.sortedBy { it.id }
        val rows = sorted.map { a ->
            val late = SopScheduleParser.dueState(a)
                .filter { it.isLate }
                .joinToString(", ") { SopScheduleParser.freqLabel(it.code) }
                .ifBlank { "none" }
            val lc = a.lastCheck
            val lcStr = if (lc != null && lc.at.isNotBlank()) {
                "${SopScheduleParser.stamp(lc.at)} ${lc.result}"
            } else {
                "never recorded"
            }
            "${a.id} | ${a.name} | ${a.system} | ${a.criticality} | ${a.status} | last check: $lcStr | overdue: $late"
        }
        return (listOf(
            "Today: ${SopScheduleParser.stamp(SopScheduleParser.nowIso())}",
            "Asset register of ${rows.size} assets. Columns: ID | name | system | criticality | status | last check | overdue check frequencies"
        ) + rows).joinToString("\n")
    }

    suspend fun askMaintenanceAi(
        question: String,
        recordContext: String,
        thinkingLevel: ThinkingLevel = ThinkingLevel.HIGH
    ): Result<AiAskResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured yet. Please set GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Enter a question first."))
        }

        // Uses gemini-3.1-pro-preview with thinkingLevel = ThinkingLevel.HIGH and NO maxOutputTokens
        val request = GenerateContentRequest(
            systemInstruction = Content(
                parts = listOf(Part(text = AI_SYSTEM_INSTRUCTION))
            ),
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = "RECORD\n$recordContext\n\nQUESTION\n$trimmedQuestion"))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                thinkingConfig = ThinkingConfig(thinkingLevel = thinkingLevel)
            )
        )

        try {
            val response = service.generateContent(apiKey = apiKey, request = request)
            val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
            val thoughtParts = parts.filter { it.thought == true }.mapNotNull { it.text?.trim() }.filter { it.isNotEmpty() }
            val answerParts = parts.filter { it.thought != true }.mapNotNull { it.text?.trim() }.filter { it.isNotEmpty() }

            val finalAnswer = if (answerParts.isNotEmpty()) {
                answerParts.joinToString("\n\n")
            } else {
                parts.mapNotNull { it.text?.trim() }.joinToString("\n\n")
            }

            if (finalAnswer.isBlank()) {
                Result.failure(IllegalStateException("Gemini returned an empty response. Please try asking again."))
            } else {
                Result.success(
                    AiAskResult(
                        answerText = finalAnswer,
                        thinkingSummary = thoughtParts.joinToString("\n\n"),
                        modelUsed = "$MODEL_ID (Thinking: ${thinkingLevel.name})"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
