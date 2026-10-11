package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AssetEntity
import com.example.model.DeadlineStatus
import com.example.model.SopScheduleParser
import kotlin.math.max

class SopDeadlineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val assetId = intent.getStringExtra(SopNotificationHelper.EXTRA_ASSET_ID) ?: return
        val assetName = intent.getStringExtra(SopNotificationHelper.EXTRA_ASSET_NAME) ?: assetId
        val location = intent.getStringExtra(SopNotificationHelper.EXTRA_LOCATION) ?: ""
        val freqCode = intent.getStringExtra(SopNotificationHelper.EXTRA_FREQ_CODE) ?: "D"
        val dueIso = intent.getStringExtra(SopNotificationHelper.EXTRA_DUE_ISO) ?: ""
        val taskNote = intent.getStringExtra(SopNotificationHelper.EXTRA_TASK_NOTE) ?: ""
        val dueMs = SopScheduleParser.parseIsoMillis(dueIso)
        val isOverdue = dueMs != null && System.currentTimeMillis() > dueMs
        val duePhrase = if (dueMs != null) {
            SopScheduleParser.formatTimeRemaining(dueMs)
        } else {
            "soon"
        }

        SopNotificationHelper.sendDeadlineNotificationRaw(
            context = context,
            assetId = assetId,
            assetName = assetName,
            location = location,
            freqCode = freqCode,
            dueIso = dueIso,
            duePhrase = duePhrase,
            isOverdue = isOverdue,
            taskNote = taskNote
        )
    }
}

object SopNotificationHelper {
    const val CHANNEL_ID = "akah_sop_deadlines"
    const val CHANNEL_NAME = "SOP Maintenance Deadlines"
    const val EXTRA_OPEN_ASSET_ID = "extra_open_asset_id"
    const val EXTRA_ASSET_ID = "extra_asset_id"
    const val EXTRA_ASSET_NAME = "extra_asset_name"
    const val EXTRA_LOCATION = "extra_location"
    const val EXTRA_FREQ_CODE = "extra_freq_code"
    const val EXTRA_DUE_ISO = "extra_due_iso"
    const val EXTRA_TASK_NOTE = "extra_task_note"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when an asset's SOP maintenance check is approaching its due date or overdue."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notificationIdForAsset(assetId: String): Int {
        return 40_000 + (assetId.hashCode() and 0x0FFFFFFF) % 50_000
    }

    fun sendDeadlineNotification(
        context: Context,
        asset: AssetEntity,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!asset.notificationsEnabled) return false
        val eval = SopScheduleParser.evaluateAssetDeadline(asset, nowMs)
        val dueMs = eval.dueAtMs ?: return false
        val duePhrase = SopScheduleParser.formatTimeRemaining(dueMs, nowMs)
        val isOverdue = eval.status == DeadlineStatus.OVERDUE

        return sendDeadlineNotificationRaw(
            context = context,
            assetId = asset.id,
            assetName = asset.name,
            location = asset.location,
            freqCode = eval.freqCode,
            dueIso = eval.dueIso.orEmpty(),
            duePhrase = duePhrase,
            isOverdue = isOverdue,
            taskNote = eval.taskNote
        )
    }

    fun sendDeadlineNotificationRaw(
        context: Context,
        assetId: String,
        assetName: String,
        location: String,
        freqCode: String,
        dueIso: String,
        duePhrase: String,
        isOverdue: Boolean,
        taskNote: String
    ): Boolean {
        return try {
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

            val freqLabel = SopScheduleParser.freqLabel(freqCode)
            val title = if (isOverdue) {
                "SOP Check Overdue: $assetId ($freqLabel)"
            } else {
                "SOP Check Approaching: $assetId ($freqLabel)"
            }

            val formattedDue = SopScheduleParser.fmtDT(dueIso)
            val locSuffix = if (location.isNotBlank()) " · $location" else ""
            val contentText = buildString {
                append("$assetName$locSuffix — Due $duePhrase ($formattedDue)")
                if (taskNote.isNotBlank()) {
                    append(" · Task: $taskNote")
                }
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_ASSET_ID, assetId)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationIdForAsset(assetId),
                openIntent,
                pendingFlags
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_akah_crest)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .build()

            manager.notify(notificationIdForAsset(assetId), notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun scheduleOrTriggerDeadlineNotification(
        context: Context,
        asset: AssetEntity,
        forceImmediateNotification: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val eval = SopScheduleParser.evaluateAssetDeadline(asset, nowMs)
        val dueMs = eval.dueAtMs

        if (!asset.notificationsEnabled || dueMs == null) {
            val cancelIntent = Intent(context, SopDeadlineReminderReceiver::class.java)
            val cancelPi = PendingIntent.getBroadcast(
                context,
                notificationIdForAsset(asset.id),
                cancelIntent,
                pendingFlags
            )
            alarmManager?.cancel(cancelPi)
            return false
        }

        val leadMs = max(1, asset.reminderLeadHours) * 3_600_000L
        val triggerAtMs = max(nowMs + 5_000L, dueMs - leadMs)

        val broadcastIntent = Intent(context, SopDeadlineReminderReceiver::class.java).apply {
            putExtra(EXTRA_ASSET_ID, asset.id)
            putExtra(EXTRA_ASSET_NAME, asset.name)
            putExtra(EXTRA_LOCATION, asset.location)
            putExtra(EXTRA_FREQ_CODE, eval.freqCode)
            putExtra(EXTRA_DUE_ISO, eval.dueIso.orEmpty())
            putExtra(EXTRA_TASK_NOTE, eval.taskNote)
        }
        val broadcastPi = PendingIntent.getBroadcast(
            context,
            notificationIdForAsset(asset.id),
            broadcastIntent,
            pendingFlags
        )

        try {
            alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                broadcastPi
            )
        } catch (_: Exception) {
        }

        // If the SOP check is already within the approaching-deadline window, overdue, or forced, send notification immediately
        val shouldNotifyNow = forceImmediateNotification ||
            eval.status == DeadlineStatus.APPROACHING ||
            eval.status == DeadlineStatus.OVERDUE

        return if (shouldNotifyNow) {
            sendDeadlineNotification(context, asset, nowMs)
        } else {
            false
        }
    }

    fun checkAndNotifyApproachingDeadlines(
        context: Context,
        assets: List<AssetEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): List<AssetEntity> {
        ensureNotificationChannel(context)
        val notified = mutableListOf<AssetEntity>()
        for (asset in assets) {
            if (!asset.notificationsEnabled) continue
            val eval = SopScheduleParser.evaluateAssetDeadline(asset, nowMs)
            val hasExplicitDue = asset.nextDueDateIso.isNotBlank() || asset.dueByFreq.isNotEmpty()
            if (eval.status == DeadlineStatus.APPROACHING ||
                (hasExplicitDue && eval.status == DeadlineStatus.OVERDUE)
            ) {
                if (sendDeadlineNotification(context, asset, nowMs)) {
                    notified.add(asset)
                }
            }
        }
        return notified
    }
}
