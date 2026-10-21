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
import com.fahad1005.sellerchat.ui.SellerChatRoot
import com.fahad1005.sellerchat.ui.theme.SellerChatTheme
import com.fahad1005.sellerchat.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSendIntent(intent)
        handleFollowUpIntent(intent)
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
