package com.fahad1005.sellerchat.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.ui.components.CountryPickerDialog
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.util.QrUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Screen 5 — the seller's own link, pushed to their WhatsApp status so
 *  customers land straight in a chat with them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStatusScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val country by vm.selectedCountry.collectAsState()

    var myNumber by remember { mutableStateOf(vm.settings.myNumberE164) }
    var numberInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }

    val link = myNumber?.let { PhoneUtils.waMeLink(it) }
    val qrBitmap by produceState<Bitmap?>(null, link) {
        value = if (link == null) null
        else withContext(Dispatchers.Default) { QrUtils.generate(link) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share to my status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Customers tap your link and land directly in your chat.",
                style = MaterialTheme.typography.bodyLarge
            )

            if (link == null) {
                Text(
                    text = "First, set your own WhatsApp number:",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCountryPicker = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("${country.flag} +${country.dial}")
                    }
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = {
                            numberInput = it.filter { c -> c.isDigit() || c == '+' }
                            inputError = null
                        },
                        label = { Text("My WhatsApp number") },
                        singleLine = true,
                        isError = inputError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (inputError != null) {
                    Text(
                        text = inputError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        val e164 = PhoneUtils.normalize(numberInput, country)
                        if (e164 == null) {
                            inputError = "Enter a valid number"
                        } else {
                            vm.settings.myNumberE164 = e164
                            myNumber = e164
                        }
                    },
                    enabled = numberInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) { Text("Save my number") }
            } else {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(link)) }) {
                    Text("Copy my link")
                }
                val bmp = qrBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "QR code for your wa.me link",
                        modifier = Modifier.size(240.dp)
                    )
                }
                Button(
                    onClick = {
                        val text = "Order with me on WhatsApp: $link"
                        val waIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            setPackage(WhatsAppPackages.WHATSAPP)
                        }
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            val generic = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(generic, "Share my link"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) { Text("Share my link to WhatsApp status") }
                TextButton(onClick = {
                    vm.settings.myNumberE164 = null
                    myNumber = null
                    numberInput = ""
                }) { Text("Change my number") }
            }
        }
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
