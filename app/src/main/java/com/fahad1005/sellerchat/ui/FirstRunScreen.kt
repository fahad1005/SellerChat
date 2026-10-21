package com.fahad1005.sellerchat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.ui.components.CountryPickerDialog
import com.fahad1005.sellerchat.viewmodel.MainViewModel

/** Screen 7 — ONE first-run screen, skippable. Three value lines, a
 *  country confirmation, the privacy promise, done. No carousel. */
@Composable
fun FirstRunScreen(vm: MainViewModel, onDone: () -> Unit) {
    val country by vm.selectedCountry.collectAsState()
    var showCountryPicker by remember { mutableStateOf(false) }
    var demoFailed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("Skip") }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.ic_chat_bubble),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Text("SellerChat", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Message any customer on WhatsApp — with your reply already typed.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ValueLine("Message any customer without saving their number first.")
        ValueLine("Your price and order replies, ready in one tap.")
        ValueLine("Works with WhatsApp and WhatsApp Business.")

        Spacer(modifier = Modifier.height(8.dp))

        Text("Your country", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${country.flag}  ${country.name}  (+${country.dial})")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text(
                text = "Your data stays on your phone. No account, no cloud, nothing uploaded.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // "Try Demo" (v2 spec §2A): one tap pre-fills a fake number +
        // the Price Inquiry template and opens WhatsApp, so the user
        // sees the whole trick before setting anything up.
        OutlinedButton(
            onClick = { demoFailed = !vm.tryDemo() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("Try Demo") }
        if (demoFailed) {
            Text(
                text = "WhatsApp is not installed on this phone — install it, then try the demo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("Get started") }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            onPick = {
                vm.setCountry(it)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@Composable
private fun ValueLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
