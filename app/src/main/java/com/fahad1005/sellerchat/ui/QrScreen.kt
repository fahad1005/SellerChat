package com.fahad1005.sellerchat.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.core.content.FileProvider
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.util.QrUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Screen 4 — wa.me link + QR. The number arrives pre-filled from Home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(initialNumberE164: String, initialMessage: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var numberText by remember { mutableStateOf(initialNumberE164) }
    var message by remember { mutableStateOf(initialMessage) }

    val digits = numberText.filter { it.isDigit() }
    val valid = digits.isNotEmpty() && PhoneUtils.isValidE164(digits)
    val link = if (valid) PhoneUtils.waMeLink(digits, message) else ""

    val qrBitmap by produceState<Bitmap?>(null, link) {
        value = if (link.isBlank()) null
        else withContext(Dispatchers.Default) { QrUtils.generate(link) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My wa.me link & QR") },
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
            OutlinedTextField(
                value = numberText,
                onValueChange = { numberText = it.filter { c -> c.isDigit() || c == '+' } },
                label = { Text("WhatsApp number (with country code)") },
                placeholder = { Text("923001234567") },
                singleLine = true,
                isError = numberText.isNotBlank() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Pre-filled message (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (valid) {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(link)) }) {
                        Text("Copy")
                    }
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share link"))
                    }) { Text("Share") }
                }

                val bmp = qrBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "QR code for your wa.me link",
                        modifier = Modifier.size(260.dp)
                    )
                    Button(onClick = {
                        // Save QR: PNG into the app cache, then the system
                        // share sheet via FileProvider (spec §4).
                        val dir = File(context.cacheDir, "qr").apply { mkdirs() }
                        val file = File(dir, "sellerchat-qr.png")
                        file.outputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val uri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, link)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share QR code"))
                    }) { Text("Save QR") }
                }
            } else {
                Text(
                    text = "Enter a valid number to see your link and QR code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
