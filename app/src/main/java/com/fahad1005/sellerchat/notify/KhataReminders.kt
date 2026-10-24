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
 * Khata due-date reminders (v2.6). Same pattern as the scheduled
 * messages, reusing the existing follow-ups notification channel —
 * no new channel, no new permission: the in-context
 * POST_NOTIFICATIONS ask already covers this channel's doorbell.
 *
 * Flow: the seller saves an udhaar entry with a due date and the
 * "Remind me" toggle on (permission asked in-context at that
 * moment, exactly like "Follow-up in 2 days") → a WorkManager
 * one-time job is armed for the due date. At fire time the
 * notification shows the live balance line and tapping it opens
 * MainActivity, which opens WhatsApp with the polite reminder
 * pre-filled — the seller still taps send in WhatsApp. Nothing
 * auto-sends, ever. Denied permission → the due date still shows
 * in the khata (in-app only); deleting the entry cancels the job,
 * so a settled debt never rings.
 */
object KhataReminders {
    const val EXTRA_KHATA_NUMBER = "khata_number"
    const val EXTRA_KHATA_NAME = "khata_name"
    const val EXTRA_KHATA_ENTRY_ID = "khata_entry_id"

    private fun workName(entryId: Long) = "khata_due_$entryId"

    fun schedule(
        context: Context,
        entryId: Long,
        customerNumber: String,
        customerName: String,
        dueAt: Long
    ) {
        val delayMs = (dueAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putLong(KEY_ENTRY_ID, entryId)
            .putString(KEY_NUMBER, customerNumber)
            .putString(KEY_NAME, customerName)
            .build()
        val request = OneTimeWorkRequestBuilder<KhataDueWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(entryId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, entryId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(entryId))
    }

    internal const val KEY_ENTRY_ID = "khata_entry_id"
    internal const val KEY_NUMBER = "number"
    internal const val KEY_NAME = "name"
}

/** Posts the due-date notification when its time comes. The
 *  balance is NOT computed here (workers have no UI state): the
 *  notification carries the customer, and MainActivity builds the
 *  reminder text from the live ledger at tap time — so a payment
 *  made after scheduling is reflected in what the seller sends. */
class KhataDueWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val entryId = inputData.getLong(KhataReminders.KEY_ENTRY_ID, -1L)
        val number = inputData.getString(KhataReminders.KEY_NUMBER) ?: return Result.failure()
        val name = inputData.getString(KhataReminders.KEY_NAME) ?: ""
        val who = name.ifBlank { "+$number" }

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(KhataReminders.EXTRA_KHATA_NUMBER, number)
            putExtra(KhataReminders.EXTRA_KHATA_NAME, name)
            putExtra(KhataReminders.EXTRA_KHATA_ENTRY_ID, entryId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            ("khata$entryId").hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, FollowUps.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat_bubble)
            .setContentTitle("Payment due from $who")
            .setContentText("A khata payment is due today. Tap to open WhatsApp with a polite reminder.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(applicationContext)
                .notify(("khata$entryId").hashCode(), notification)
            Result.success()
        } catch (e: SecurityException) {
            // Permission revoked between scheduling and firing: the
            // due date still exists in the khata; fail quietly.
            Result.failure()
        }
    }
}
