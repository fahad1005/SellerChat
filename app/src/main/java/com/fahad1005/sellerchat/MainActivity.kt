package com.fahad1005.sellerchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.fahad1005.sellerchat.ui.SellerChatRoot
import com.fahad1005.sellerchat.ui.theme.SellerChatTheme
import com.fahad1005.sellerchat.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSendIntent(intent)
        handleFollowUpIntent(intent)
        handleScheduledIntent(intent)
        handleKhataIntent(intent)
        setContent {
            SellerChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SellerChatRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSendIntent(intent)
        handleFollowUpIntent(intent)
        handleScheduledIntent(intent)
        handleKhataIntent(intent)
    }

    /** Khata due-date notification deep link (v2.6): the
     *  notification carries the entry id + customer. The reminder
     *  text is built from the LIVE ledger at tap time (so a
     *  payment made after scheduling is reflected, and a settled
     *  debt opens nothing). Extras are consumed so a later resume
     *  doesn't re-fire. */
    private fun handleKhataIntent(intent: Intent?) {
        val number = intent?.getStringExtra(
            com.fahad1005.sellerchat.notify.KhataReminders.EXTRA_KHATA_NUMBER
        ) ?: return
        val name = intent.getStringExtra(
            com.fahad1005.sellerchat.notify.KhataReminders.EXTRA_KHATA_NAME
        ) ?: ""
        val entryId = intent.getLongExtra(
            com.fahad1005.sellerchat.notify.KhataReminders.EXTRA_KHATA_ENTRY_ID, -1L
        )
        intent.removeExtra(com.fahad1005.sellerchat.notify.KhataReminders.EXTRA_KHATA_NUMBER)
        intent.removeExtra(com.fahad1005.sellerchat.notify.KhataReminders.EXTRA_KHATA_NAME)
        intent.removeExtra(com.fahad1005.sellerchat.notify.KhataReminders.EXTRA_KHATA_ENTRY_ID)
        if (entryId < 0L) return
        lifecycleScope.launch {
            val text = vm.khataReminderTextForEntry(entryId, number, name) ?: return@launch
            val pkg = vm.selectedAppPackage.value
                ?: vm.settings.defaultAppPackage
                ?: com.fahad1005.sellerchat.viewmodel.WhatsAppPackages.WHATSAPP
            vm.openChat(pkg, number, text)
        }
    }

    /** Scheduled-message notification deep link (v2.5): the
     *  notification carries the item id + customer + approved text.
     *  Tapping it marks the item SENT and opens WhatsApp with the
     *  text pre-filled — the seller still taps send in WhatsApp.
     *  The extras are consumed so a later resume doesn't re-fire. */
    private fun handleScheduledIntent(intent: Intent?) {
        // v2.6 fix: the old `?: -1L` did NOT prove intent non-null to the
        // compiler, so every bare intent.* call below failed to compile.
        // `?: return` does prove it (null intent and missing id both
        // return, exactly as before).
        val id = intent?.getLongExtra(
            com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_ID, -1L
        ) ?: return
        if (id < 0L) return
        val number = intent.getStringExtra(
            com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_NUMBER
        ) ?: return
        val pkg = intent.getStringExtra(
            com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_PACKAGE
        ) ?: com.fahad1005.sellerchat.viewmodel.WhatsAppPackages.WHATSAPP
        val text = intent.getStringExtra(
            com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_TEXT
        ) ?: ""
        intent.removeExtra(com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_ID)
        intent.removeExtra(com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_NUMBER)
        intent.removeExtra(com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_PACKAGE)
        intent.removeExtra(com.fahad1005.sellerchat.notify.ScheduledMessages.EXTRA_SCHEDULED_TEXT)
        vm.markScheduledSentById(id)
        vm.openChat(pkg, number, text)
    }

    /** Follow-up notification deep link (v2): the notification carries
     *  the customer's number + app; tapping it opens that chat
     *  directly. The extras are consumed so a later resume doesn't
     *  re-fire the chat. */
    private fun handleFollowUpIntent(intent: Intent?) {
        val number = intent?.getStringExtra(
            com.fahad1005.sellerchat.notify.FollowUps.EXTRA_OPEN_CHAT_NUMBER
        ) ?: return
        val pkg = intent.getStringExtra(
            com.fahad1005.sellerchat.notify.FollowUps.EXTRA_OPEN_CHAT_PACKAGE
        ) ?: com.fahad1005.sellerchat.viewmodel.WhatsAppPackages.WHATSAPP
        intent.removeExtra(com.fahad1005.sellerchat.notify.FollowUps.EXTRA_OPEN_CHAT_NUMBER)
        intent.removeExtra(com.fahad1005.sellerchat.notify.FollowUps.EXTRA_OPEN_CHAT_PACKAGE)
        vm.openChat(pkg, number, "")
    }

    /** ACTION_SEND growth path: shared text containing a phone number
     *  pre-fills the Home number field. */
    private fun handleSendIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                vm.onSharedText(text)
            }
        }
    }
}
