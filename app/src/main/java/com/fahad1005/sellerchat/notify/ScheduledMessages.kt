package com.fahad1005.sellerchat.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fahad1005.sellerchat.MainActivity
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages
import java.util.concurrent.TimeUnit

/**
 * Scheduled messages, v2.5 — reminder-to-send, NEVER auto-send.
 *
 * The seller picks a time; at that time a notification fires. Tapping
 * it opens MainActivity, which marks the item SENT and opens WhatsApp
 * with the pre-filled text — the seller still taps send in WhatsApp.
 * Dismissing the notification leaves the item PENDING and overdue in
 * the list, where "Send now" does the same job. No
 * NotificationListenerService, no accessibility service, no
 * notification reading, no background sending: that boundary is
 * documented in BUILD_NOTES (v2.5) and is deliberate.
 *
 * The channel is the existing follow-ups channel (created once in
 * SellerChatApp): one channel for every "tap to act on a customer"
 * notification, so the seller manages a single permission.
 */
object ScheduledMessages {
    const val EXTRA_SCHEDULED_ID = "scheduled_id"
    const val EXTRA_SCHEDULED_NUMBER = "scheduled_number"
    const val EXTRA_SCHEDULED_PACKAGE = "scheduled_package"
    const val EXTRA_SCHEDULED_TEXT = "scheduled_text"

    private fun workName(id: Long) = "scheduled_$id"

    fun schedule(
        context: Context,
        id: Long,
        numberE164: String,
        appPackage: String,
        customerName: String,
        text: String,
        at: Long
    ) {
        val delayMs = (at - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putLong(KEY_ID, id)
            .putString(KEY_NUMBER, numberE164)
            .putString(KEY_PACKAGE, appPackage)
            .putString(KEY_NAME, customerName)
            .putString(KEY_TEXT, text)
            .build()
        val request = OneTimeWorkRequestBuilder<ScheduledWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    internal const val KEY_ID = "scheduled_id"
    internal const val KEY_NUMBER = "number"
    internal const val KEY_PACKAGE = "pkg"
    internal const val KEY_NAME = "name"
    internal const val KEY_TEXT = "text"
}

/** Posts the scheduled-message notification when its time comes. */
class ScheduledWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getLong(ScheduledMessages.KEY_ID, -1L)
        val number = inputData.getString(ScheduledMessages.KEY_NUMBER) ?: return Result.failure()
        val pkg = inputData.getString(ScheduledMessages.KEY_PACKAGE) ?: WhatsAppPackages.WHATSAPP
        val name = inputData.getString(ScheduledMessages.KEY_NAME) ?: ""
        val text = inputData.getString(ScheduledMessages.KEY_TEXT) ?: ""
        val who = name.ifBlank { "+$number" }

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ScheduledMessages.EXTRA_SCHEDULED_ID, id)
            putExtra(ScheduledMessages.EXTRA_SCHEDULED_NUMBER, number)
            putExtra(ScheduledMessages.EXTRA_SCHEDULED_PACKAGE, pkg)
            putExtra(ScheduledMessages.EXTRA_SCHEDULED_TEXT, text)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            ("scheduled_$id").hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, FollowUps.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat_bubble)
            .setContentTitle("Time to message $who")
            .setContentText("Your scheduled message is ready. Tap to open WhatsApp and send it.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(applicationContext)
                .notify(("scheduled_$id").hashCode(), notification)
            Result.success()
        } catch (e: SecurityException) {
            // Permission revoked between scheduling and firing: the
            // item stays PENDING and due in the list; fail quietly.
            Result.failure()
        }
    }
}
