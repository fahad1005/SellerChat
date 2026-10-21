package com.fahad1005.sellerchat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.ui.components.AdBanner
import com.fahad1005.sellerchat.ui.components.CountryPickerDialog
import com.fahad1005.sellerchat.ui.components.TemplateBodyText
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onShowQr: (numberE164: String, message: String) -> Unit,
    onShareStatus: () -> Unit,
    onEditTemplate: (Template) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val templates by vm.templates.collectAsState()
    val number by vm.homeNumber.collectAsState()
    val label by vm.homeLabel.collectAsState()
    val country by vm.selectedCountry.collectAsState()
    val selectedApp by vm.selectedAppPackage.collectAsState()
    val selectedTemplateId by vm.selectedTemplateId.collectAsState()
    val installedWa by vm.installedWhatsApp.collectAsState()
    val installedWab by vm.installedWhatsAppBusiness.collectAsState()

    var numberError by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var previewTemplate by remember { mutableStateOf<Template?>(null) }
    var failedOtherApp by remember { mutableStateOf<String?>(null) }
    var showFailedDialog by remember { mutableStateOf(false) }

    fun normalizedOrError(): String? {
        val e164 = PhoneUtils.normalize(number, country)
        numberError = if (e164 == null) "Enter a valid number" else null
        return e164
    }

    fun builtText(): String {
        val t = vm.templateById(selectedTemplateId)
        return if (t != null) PhoneUtils.applyTemplate(t.body, label.trim()) else ""
    }

    fun openChat(pkg: String) {
        val e164 = normalizedOrError() ?: return
        val template = vm.templateById(selectedTemplateId)
        val ok = vm.openChat(pkg, e164, builtText())
        if (ok) {
            vm.recordRecent(e164, label.trim(), pkg, template?.title)
        } else {
            failedOtherApp = when {
                pkg == WhatsAppPackages.WHATSAPP && installedWab -> WhatsAppPackages.WHATSAPP_BUSINESS
                pkg == WhatsAppPackages.WHATSAPP_BUSINESS && installedWa -> WhatsAppPackages.WHATSAPP
                else -> null
            }
            showFailedDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Number row: country selector + field (paste / clear inside) ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showCountryPicker = true },
                modifier = Modifier.height(56.dp)
            ) {
                Text(text = "${country.flag} +${country.dial}")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose country")
            }
            OutlinedTextField(
                value = number,
                onValueChange = {
                    vm.homeNumber.value = it.filter { c -> c.isDigit() || c == '+' }
                    numberError = null
                },
                label = { Text("Customer number") },
                placeholder = { Text("0300 1234567") },
                singleLine = true,
                isError = numberError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            // Clipboard is read ONLY here: foreground + explicit tap.
                            val pasted = clipboard.getText()?.text
                            if (!pasted.isNullOrBlank()) {
                                vm.homeNumber.value =
                                    pasted.filter { c -> c.isDigit() || c == '+' }
                                numberError = null
                            }
                        }) {
                            Icon(painter = painterResource(R.drawable.ic_content_paste), contentDescription = "Paste number")
                        }
                        if (number.isNotEmpty()) {
                            IconButton(onClick = { vm.homeNumber.value = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear number")
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (numberError != null) {
            Text(
                text = numberError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // ---- App chooser: only installed apps exist here ----
        when {
            !installedWa && !installedWab -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "WhatsApp is not installed on this phone",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")
                            )
                            context.startActivity(intent)
                        }) {
                            Text("Get WhatsApp on Google Play")
                        }
                    }
                }
            }
            installedWa && installedWab -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(
                        text = "WhatsApp",
                        selected = selectedApp == WhatsAppPackages.WHATSAPP,
                        onClick = { vm.selectedAppPackage.value = WhatsAppPackages.WHATSAPP }
                    )
                    SelectChip(
                        text = "WhatsApp Business",
                        selected = selectedApp == WhatsAppPackages.WHATSAPP_BUSINESS,
                        onClick = { vm.selectedAppPackage.value = WhatsAppPackages.WHATSAPP_BUSINESS }
                    )
                }
            }
            else -> {
                Text(
                    text = "Using ${WhatsAppPackages.displayName(selectedApp ?: WhatsAppPackages.WHATSAPP)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Optional label ----
        OutlinedTextField(
            value = label,
            onValueChange = { vm.homeLabel.value = it },
            label = { Text("Customer label (optional)") },
            placeholder = { Text("Ayesha — kurta order") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ---- Template chips ----
        Text(text = "Reply template", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                TemplateChip(
                    text = "No template",
                    selected = selectedTemplateId == -1L,
                    onClick = { vm.selectedTemplateId.value = -1L },
                    onLongClick = null
                )
            }
            items(templates, key = { it.id }) { template ->
                TemplateChip(
                    text = template.title,
                    selected = selectedTemplateId == template.id,
                    onClick = { vm.selectedTemplateId.value = template.id },
                    onLongClick = { previewTemplate = template }
                )
            }
        }

        // ---- Primary CTA ----
        Button(
            onClick = { selectedApp?.let { openChat(it) } },
            enabled = selectedApp != null && number.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Open chat", style = MaterialTheme.typography.titleMedium)
        }

        // ---- Secondary actions ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                val e164 = normalizedOrError() ?: return@TextButton
                clipboard.setText(AnnotatedString(PhoneUtils.waMeLink(e164, builtText())))
            }) { Text("Copy wa.me link") }
            TextButton(onClick = {
                val e164 = normalizedOrError() ?: return@TextButton
                onShowQr(e164, builtText())
            }) { Text("Show QR") }
            TextButton(onClick = onShareStatus) { Text("Share to my status") }
        }

        Spacer(modifier = Modifier.weight(1f))

        // The ONE ad in the app: Home bottom only.
        AdBanner()
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

    val preview = previewTemplate
    if (preview != null) {
        ModalBottomSheet(
            onDismissRequest = { previewTemplate = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(preview.title, style = MaterialTheme.typography.titleMedium)
                TemplateBodyText(body = preview.body, language = preview.language)
                Button(
                    onClick = {
                        previewTemplate = null
                        onEditTemplate(preview)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Edit") }
            }
        }
    }

    if (showFailedDialog) {
        AlertDialog(
            onDismissRequest = { showFailedDialog = false },
            title = { Text("Could not open WhatsApp") },
            text = { Text("Could not open WhatsApp. Try the other app.") },
            confirmButton = {
                val other = failedOtherApp
                if (other != null) {
                    TextButton(onClick = {
                        showFailedDialog = false
                        vm.selectedAppPackage.value = other
                        openChat(other)
                    }) { Text("Open in ${WhatsAppPackages.displayName(other)}") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFailedDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .height(48.dp)
            .combinedClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .height(48.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
