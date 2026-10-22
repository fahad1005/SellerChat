package com.fahad1005.sellerchat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.fahad1005.sellerchat.ui.components.NastaliqUrduFontFamily
import com.fahad1005.sellerchat.ui.components.TemplateBodyText
import com.fahad1005.sellerchat.ui.components.usesNastaliq
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
    // Strict per-country validation (v2.4 task 1), derived live by
    // the VM from the number field + the selected country.
    val validation by vm.homeNumberValidation.collectAsState()

    var numberError by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var previewTemplate by remember { mutableStateOf<Template?>(null) }
    var failedOtherApp by remember { mutableStateOf<String?>(null) }
    var showFailedDialog by remember { mutableStateOf(false) }
    // One-time tooltip (v2 spec §2A): persisted flag, shown exactly once.
    var showTooltip by remember { mutableStateOf(!vm.settings.homeTooltipShown) }

    // Chip order (v2.1 task 4): the Preferred Template Language leads.
    // When it is "Any"/unset, the priority is derived from the user's
    // default country instead of showing a random language mix:
    //   PK → EN, UR, ROMAN_UR · IN → HI, EN · BD → EN · AE/NG → EN ·
    //   anywhere else → EN. (Table also documented in BUILD_NOTES.)
    // The sort is stable: templates outside the priority list keep
    // their library order after the prioritised ones.
    val preferredLanguage = vm.settings.preferredTemplateLanguage
    val priorityLanguages = remember(preferredLanguage, country) {
        when {
            preferredLanguage != null -> listOf(preferredLanguage)
            country.iso == "PK" -> listOf(
                TemplateLanguage.EN, TemplateLanguage.UR, TemplateLanguage.ROMAN_UR
            )
            country.iso == "IN" -> listOf(TemplateLanguage.HI, TemplateLanguage.EN)
            else -> listOf(TemplateLanguage.EN)
        }
    }
    val sortedTemplates = remember(templates, priorityLanguages) {
        templates.sortedBy { t ->
            val idx = priorityLanguages.indexOf(t.language)
            if (idx >= 0) idx else priorityLanguages.size
        }
    }

    // Send paths take the validation state's normalized number —
    // what was validated is what gets sent (v2.4 task 1). Tapping an
    // action with an EMPTY field keeps the old "Enter a valid
    // number" hint; an Invalid pair never reaches here — the
    // actions are disabled and the inline country error below is
    // showing instead.
    fun validatedNumber(): String? = when (val v = validation) {
        is PhoneUtils.NumberValidation.Valid -> v.waNumber
        is PhoneUtils.NumberValidation.Empty -> {
            numberError = "Enter a valid number"
            null
        }
        is PhoneUtils.NumberValidation.Invalid -> null
    }

    val invalidForCountry =
        validation is PhoneUtils.NumberValidation.Invalid && number.isNotBlank()
    val shownError = if (invalidForCountry) {
        "Invalid number for selected country (+${country.dial}). " +
            "Please check the country code."
    } else {
        numberError
    }

    // The send text: the VM's one shared fill (v2.1 task 3) — the exact
    // same function that renders the preview under the chips.
    fun builtText(): String = vm.filledTemplateText(vm.templateById(selectedTemplateId))

    fun openChat(pkg: String) {
        val e164 = validatedNumber() ?: return
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

    // Layout math (v2.1 task 1 — keyboard overlap): the manifest sets
    // windowSoftInputMode="adjustResize" on MainActivity, so when the
    // keyboard opens the window itself shrinks; the Scaffold in
    // MainScreen additionally hides the bottom navigation while the IME
    // is visible, so the nav can never float above the keyboard. What
    // remains for Home is exactly (viewport − top bar) tall:
    //   [ scrollable content column, weight 1 ] + [ banner strip ]
    // The "Open chat" CTA lives INSIDE the scroll region, so it is
    // always reachable by scrolling, and the banner is laid out AFTER
    // the scroll region — a sibling, never a scroll child — so it sits
    // fixed at the bottom of Home (like v1.0) and can neither be pushed
    // off-screen by the content nor overlap the CTA. (In v2 the banner
    // was the last child of a NON-scrolling Column behind a weight-1
    // Spacer: any viewport too short for the full content — small
    // phone, or adjustResize shrinking the window for the keyboard —
    // pushed the banner past the bottom edge. That is why it looked
    // "missing". It was never deleted from the code.)
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // ---- One-time tooltip: the whole app in one line, once ----
        if (showTooltip) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                ) {
                    Text(
                        text = "Paste a number → Pick a template → Open Chat.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp)
                    )
                    IconButton(onClick = {
                        showTooltip = false
                        vm.settings.homeTooltipShown = true
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss tip")
                    }
                }
            }
        }

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
                isError = shownError != null,
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
        if (shownError != null) {
            Text(
                text = shownError ?: "",
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

        // ---- Template chips (moved UP in v2, spec §2B: sellers see
        // their replies before anything optional). v2.1 task 4: the
        // "No template" chip sits at the FAR END of the row, after every
        // template, so it is never tapped by accident. ----
        Text(text = "Reply template", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sortedTemplates, key = { it.id }) { template ->
                TemplateChip(
                    text = template.title,
                    selected = selectedTemplateId == template.id,
                    onClick = { vm.selectedTemplateId.value = template.id },
                    onLongClick = { previewTemplate = template },
                    language = template.language
                )
            }
            item {
                TemplateChip(
                    text = "No template",
                    selected = selectedTemplateId == -1L,
                    onClick = { vm.selectedTemplateId.value = -1L },
                    onLongClick = null
                )
            }
        }

        // ---- Filled preview (v2.1 task 3): the selected template with
        // the label's {name} / {item} already filled in, 2 lines max —
        // rendered by the SAME VM fill function the send path uses, so
        // a seller always knows exactly what they are sending. ----
        val selectedTemplate = vm.templateById(selectedTemplateId)
        if (selectedTemplate != null) {
            TemplateBodyText(
                body = vm.filledTemplateText(selectedTemplate),
                language = selectedTemplate.language,
                maxLines = 2
            )
        }

        // ---- Optional label (moved DOWN in v2, spec §2B) ----
        OutlinedTextField(
            value = label,
            onValueChange = { vm.homeLabel.value = it },
            label = { Text("Customer label (optional)") },
            placeholder = { Text("Ayesha — kurta order") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ---- Primary CTA (v2.4 task 1: enabled only for a number
        // that strictly validates against the selected country) ----
        Button(
            onClick = { selectedApp?.let { openChat(it) } },
            enabled = selectedApp != null &&
                validation is PhoneUtils.NumberValidation.Valid,
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
            TextButton(
                onClick = {
                    val e164 = validatedNumber() ?: return@TextButton
                    clipboard.setText(AnnotatedString(PhoneUtils.waMeLink(e164, builtText())))
                },
                enabled = validation !is PhoneUtils.NumberValidation.Invalid
            ) { Text("Copy wa.me link") }
            TextButton(
                onClick = {
                    val e164 = validatedNumber() ?: return@TextButton
                    onShowQr(e164, builtText())
                },
                enabled = validation !is PhoneUtils.NumberValidation.Invalid
            ) { Text("Show QR") }
            TextButton(onClick = onShareStatus) { Text("Share to my status") }
        }

        } // end of the scrollable content column

        // The ONE ad in the app: a fixed strip at the bottom of Home,
        // OUTSIDE the scroll area, directly above the app's bottom
        // navigation — exactly where v1.0 had it (v2.1 task 2). The
        // 12.dp top padding (v2.4 task 6) is fixed layout space on
        // the banner container itself, so the scroll content's last
        // row — the "Open chat" CTA zone — can never rest flush
        // against the ad in any scroll or keyboard state (AdMob
        // accidental-click policy).
        AdBanner(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            onPick = {
                vm.setCountry(it)
                numberError = null
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
    onLongClick: (() -> Unit)?,
    language: TemplateLanguage? = null
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
            style = if (language != null && usesNastaliq(language)) {
                // Urdu template titles chip in Nastaliq too (spec §3B).
                MaterialTheme.typography.labelLarge.copy(fontFamily = NastaliqUrduFontFamily)
            } else {
                MaterialTheme.typography.labelLarge
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
