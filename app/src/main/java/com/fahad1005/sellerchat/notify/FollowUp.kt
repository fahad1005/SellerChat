package com.fahad1005.sellerchat.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
 * Follow-up reminders (v2 spec §4). The ONLY scheduled work in the app.
 *
 * Flow: the user taps "Follow-up in 2 days" on a Recent → the Recents
 * screen asks for POST_NOTIFICATIONS in-context (Android 13+) →
 * granted: a WorkManager one-time job is scheduled and the row reads
 * "Reminder set"; denied/unavailable: the reminder is saved in Room
 * only and the row reads "Reminder set (in-app)". Either way the
 * follow-up exists; only the doorbell differs.
 */
object FollowUps {
    const val CHANNEL_ID = "follow_ups"
    const val EXTRA_OPEN_CHAT_NUMBER = "open_chat_number"
    const val EXTRA_OPEN_CHAT_PACKAGE = "open_chat_package"

    private fun workName(numberE164: String) = "followup_$numberE164"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow-up reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to follow up with a customer"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    /** Schedule the real notification for [followUpAt] (epoch ms). */
    fun schedule(context: Context, numberE164: String, appPackage: String, label: String, followUpAt: Long) {
        val delayMs = (followUpAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(KEY_NUMBER, numberE164)
            .putString(KEY_PACKAGE, appPackage)
            .putString(KEY_LABEL, label)
            .build()
        val request = OneTimeWorkRequestBuilder<FollowUpWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(numberE164),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, numberE164: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(numberE164))
    }

    internal const val KEY_NUMBER = "number"
    internal const val KEY_PACKAGE = "pkg"
    internal const val KEY_LABEL = "label"
}

/** Posts the follow-up notification when its time comes. Tapping it
 *  opens MainActivity with the customer's number, which deep-links
 *  straight into that customer's WhatsApp chat. */
class FollowUpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val number = inputData.getString(FollowUps.KEY_NUMBER) ?: return Result.failure()
        val pkg = inputData.getString(FollowUps.KEY_PACKAGE) ?: WhatsAppPackages.WHATSAPP
        val label = inputData.getString(FollowUps.KEY_LABEL) ?: ""
        val who = label.ifBlank { "+$number" }

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(FollowUps.EXTRA_OPEN_CHAT_NUMBER, number)
            putExtra(FollowUps.EXTRA_OPEN_CHAT_PACKAGE, pkg)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            number.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, FollowUps.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat_bubble)
            .setContentTitle("Follow up with $who")
            .setContentText("You asked to be reminded about this customer. Tap to open the chat.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(applicationContext)
                .notify(number.hashCode(), notification)
            Result.success()
        } catch (e: SecurityException) {
            // Permission revoked between scheduling and firing: the
            // reminder still exists in Recents; fail quietly.
            Result.failure()
        }
    }
}
