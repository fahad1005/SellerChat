package com.fahad1005.sellerchat.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.data.RecentStatus
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.ui.components.AdBanner
import com.fahad1005.sellerchat.ui.components.CountryPickerDialog
import com.fahad1005.sellerchat.ui.components.NastaliqUrduFontFamily
import com.fahad1005.sellerchat.ui.components.TemplateBodyText
import com.fahad1005.sellerchat.ui.components.usesNastaliq
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onShowQr: (numberE164: String, message: String) -> Unit,
    onShareStatus: () -> Unit,
    onEditTemplate: (Template) -> Unit,
    onOpenInvoices: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onOpenScheduled: () -> Unit = {},
    // v2.6: Templates left the tab bar (3-tab law) — reached from here.
    onManageTemplates: () -> Unit = {},
    // v2.6: Today-card taps jump to the right tab.
    onGoToCustomers: () -> Unit = {},
    onGoToMoney: () -> Unit = {}
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
    val products by vm.products.collectAsState()
    val selectedProduct by vm.selectedProduct.collectAsState()
    val recentsAll by vm.recentsAll.collectAsState()
    // v2.6: Today card + discount + channel picker state.
    val khataToday by vm.khataEntries.collectAsState()
    val businessToday by vm.selectedBusiness.collectAsState()
    val discount by vm.discountPercent.collectAsState()
    var showChannelPicker by remember { mutableStateOf(false) }
    var channelNote by remember { mutableStateOf<String?>(null) }

    var numberError by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var previewTemplate by remember { mutableStateOf<Template?>(null) }
    var failedOtherApp by remember { mutableStateOf<String?>(null) }
    var showFailedDialog by remember { mutableStateOf(false) }
    // One-time tooltip (v2 spec §2A): persisted flag, shown exactly once.
    var showTooltip by remember { mutableStateOf(!vm.settings.homeTooltipShown) }
    // v2.5 state: greeting override (one-shot suggested text), clipboard
    // suggestion (foreground-only, never stored), media/contact notes.
    var greetingOverride by remember { mutableStateOf<String?>(null) }
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }
    var dismissedClipboard by remember { mutableStateOf<String?>(null) }
    var mediaNote by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

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

    // Clipboard auto-detect (v2.5): on every foreground resume, read
    // the clipboard ONCE; if it holds a phone-like digit run different
    // from the field, offer it as a dismissible chip. The clipboard
    // text is never stored in the VM, prefs, or Room — only this
    // composition-local suggestion, cleared on dismiss or use.
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val clip = clipboard.getText()?.text
                val found = clip?.let { PhoneUtils.extractNumber(it) }
                val digits = found?.filter { it.isDigit() } ?: ""
                val currentDigits = vm.homeNumber.value.filter { it.isDigit() }
                if (digits.length in 7..15 && digits != currentDigits &&
                    digits != dismissedClipboard
                ) {
                    clipboardSuggestion = digits
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Contact import (v2.5): the system picker returns one phone row;
    // reading THAT row needs no permission — the picker grants access
    // to the picked item only. Empty/unreadable results show a note,
    // never a crash.
    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri == null) {
            importError = "No contact picked"
        } else {
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.Contacts.DISPLAY_NAME
                    ),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val pickedNumber = it.getString(0)
                        val pickedName = it.getString(1)
                        if (!pickedNumber.isNullOrBlank()) {
                            vm.homeNumber.value =
                                pickedNumber.filter { c -> c.isDigit() || c == '+' }
                            if (!pickedName.isNullOrBlank()) {
                                vm.homeLabel.value = pickedName
                            }
                            numberError = null
                            importError = null
                        } else {
                            importError = "That contact has no phone number"
                        }
                    } else {
                        importError = "Could not read that contact"
                    }
                } ?: run { importError = "Could not read that contact" }
            } catch (e: Exception) {
                importError = "Could not read that contact"
            }
        }
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
    // same function that renders the preview under the chips. A
    // greeting override (v2.5 suggestion) replaces it for exactly one
    // send, then clears — the preview below shows the override while
    // it is active, so preview and send still always match.
    fun builtText(): String =
        greetingOverride ?: vm.filledTemplateText(vm.templateById(selectedTemplateId))

    fun openChat(pkg: String) {
        val e164 = validatedNumber() ?: return
        val template = vm.templateById(selectedTemplateId)
        val ok = vm.openChat(pkg, e164, builtText())
        if (ok) {
            greetingOverride = null
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

    // Media picker (v2.5): GetContent needs no permission either.
    // On failure the fallback opens the wa.me chat with the caption
    // and tells the seller to attach the file inside WhatsApp.
    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val e164 = validatedNumber() ?: return@rememberLauncherForActivityResult
        val pkg = selectedApp ?: return@rememberLauncherForActivityResult
        val template = vm.templateById(selectedTemplateId)
        val ok = vm.sendMedia(pkg, e164, uri, builtText())
        if (!ok) {
            vm.openChat(pkg, e164, builtText())
            mediaNote = "WhatsApp opened without the file — " +
                "attach it in WhatsApp before sending."
        } else {
            vm.recordRecent(e164, label.trim(), pkg, template?.title)
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

        // ---- Today card (v2.6): the whole business in one look.
        // Money figures belong to the SELECTED shop (labelled as
        // such); waiting / follow-up counts are global and labelled
        // "All shops" because customers are not scoped to a shop.
        // Each figure taps through to the tab that explains it.
        run {
            val businessCurrency = businessToday?.currency ?: vm.settings.currencyCode
            val owedToday = khataToday.groupBy { it.customerNumber }
                .values.map { vm.khataBalance(it) }.filter { it > 0 }.sum()
            val waitingToday = recentsAll.count { it.status == RecentStatus.INQUIRY }
            val followUpsToday = recentsAll.count {
                it.followUpAt != null && it.followUpAt <= System.currentTimeMillis()
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Today · ${businessToday?.name ?: "My Business"}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onGoToCustomers) {
                            Text("Waiting: $waitingToday")
                        }
                        TextButton(onClick = onGoToMoney) {
                            Text(
                                "Owed: " + CurrencyUtils.formatWithSymbol(
                                    owedToday, businessCurrency
                                )
                            )
                        }
                        TextButton(onClick = onGoToCustomers) {
                            Text("Due: $followUpsToday")
                        }
                    }
                    Text(
                        text = "Waiting and due counts are for all shops.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        // ---- Away mode banner (v2.5, assisted only): when away mode
        // is on and the hour is outside business hours, the banner
        // shows and the away text becomes a one-tap send. Nothing
        // sends itself — the seller taps, WhatsApp opens, they send.
        if (vm.settings.awayEnabled && vm.isAwayNow()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Away mode — outside business hours " +
                            "(${vm.settings.awayOpenHour}:00–${vm.settings.awayCloseHour}:00).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            val e164 = validatedNumber() ?: return@Button
                            val pkg = selectedApp ?: return@Button
                            val template = vm.templateById(selectedTemplateId)
                            if (vm.openChat(pkg, e164, vm.settings.awayMessage)) {
                                vm.recordRecent(e164, label.trim(), pkg, template?.title)
                            }
                        },
                        enabled = selectedApp != null &&
                            validation is PhoneUtils.NumberValidation.Valid
                    ) { Text("Send away message") }
                }
            }
        }

        // ---- Clipboard suggestion chip (v2.5): dismissible, never stored.
        val clipSuggestion = clipboardSuggestion
        if (clipSuggestion != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
                    Text(
                        text = "Number from clipboard: +$clipSuggestion — Use it?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp)
                    )
                    TextButton(onClick = {
                        vm.homeNumber.value = clipSuggestion
                        numberError = null
                        clipboardSuggestion = null
                    }) { Text("Use it") }
                    IconButton(onClick = {
                        dismissedClipboard = clipSuggestion
                        clipboardSuggestion = null
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        // ---- Greeting suggestion (v2.5, assisted only): a validated
        // number with no prior Recent is a first contact — offer the
        // greeting text for one send. Never auto-sent, never forced.
        val validE164 = (validation as? PhoneUtils.NumberValidation.Valid)?.waNumber
        if (vm.settings.greetingEnabled && validE164 != null &&
            recentsAll.none { it.numberE164 == validE164 } &&
            greetingOverride == null
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
                    Text(
                        text = "New customer — send the greeting?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp)
                    )
                    TextButton(onClick = {
                        greetingOverride = vm.settings.greetingMessage
                    }) { Text("Use greeting") }
                }
            }
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
        val overrideText = greetingOverride
        if (overrideText != null) {
            TemplateBodyText(
                body = overrideText,
                language = TemplateLanguage.EN,
                maxLines = 2
            )
        } else if (selectedTemplate != null) {
            TemplateBodyText(
                body = vm.filledTemplateText(selectedTemplate),
                language = selectedTemplate.language,
                maxLines = 2
            )
        }

        // ---- Product picker (v2.5): tapping a product fills {item}
        // and {price} through the VM's shared fill — the preview above
        // and the send below always match. Tap again to clear.
        val activeProducts = products.filter { it.active }
        if (activeProducts.isNotEmpty()) {
            Text(text = "Products", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeProducts, key = { it.id }) { product ->
                    TemplateChip(
                        text = product.name + " — " +
                            CurrencyUtils.formatWithSymbol(
                                product.price, vm.settings.currencyCode
                            ),
                        selected = selectedProduct?.id == product.id,
                        onClick = {
                            vm.selectedProduct.value =
                                if (selectedProduct?.id == product.id) null else product
                        },
                        onLongClick = null
                    )
                }
            }
            if (selectedProduct != null) {
                Text(
                    text = "Picked: ${selectedProduct?.name} — fills {item} and {price}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Discount calculator (v2.6, inline): an optional %
                // computes the final price live, and the VM's shared
                // fill uses that price for {price} — preview and
                // send still always match. No new screen.
                val picked = selectedProduct
                if (picked != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (discount == 0) "" else discount.toString(),
                            onValueChange = {
                                val p = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                vm.discountPercent.value = p.coerceIn(0, 99)
                            },
                            label = { Text("Discount % (optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        if (discount in 1..99) {
                            val finalPrice = picked.price * (100 - discount) / 100
                            Text(
                                text = CurrencyUtils.formatWithSymbol(
                                    picked.price, vm.settings.currencyCode
                                ) + " − $discount% = " + CurrencyUtils.formatWithSymbol(
                                    finalPrice, vm.settings.currencyCode
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
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

        // ---- v2.5: contact import + media (both need no permission:
        // the system picker grants access to the picked item only).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                val pickIntent = Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                )
                contactPicker.launch(pickIntent)
            }) { Text("Import from contacts") }
            TextButton(
                onClick = { mediaPicker.launch("*/*") },
                enabled = validation is PhoneUtils.NumberValidation.Valid &&
                    selectedApp != null
            ) { Text("Send photo/file") }
        }
        // ---- v2.6: multichannel open-chat (LINKS ONLY). Telegram /
        // Messenger / SMS open that app's chat for this number. The
        // picker says plainly that Telegram and Messenger do not
        // pre-fill the message — only SMS does. If the target app
        // cannot open, the wa.me chat opens instead, with a note.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { showChannelPicker = true },
                enabled = validation is PhoneUtils.NumberValidation.Valid
            ) { Text("Send on another app") }
            if (channelNote != null) {
                Text(
                    text = channelNote ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (importError != null) {
            Text(
                text = importError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (mediaNote != null) {
            Text(
                text = mediaNote ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- Business tools (v2.5): Invoices, Catalog and Scheduled
        // are each one tap from Home (two taps to any screen inside).
        // v2.6: Templates joins them here (it left the tab bar).
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Business tools", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onOpenInvoices) { Text("Invoices") }
                    TextButton(onClick = onOpenCatalog) { Text("Catalog") }
                    TextButton(onClick = onOpenScheduled) { Text("Scheduled") }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onManageTemplates) { Text("Templates") }
                }
            }
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

    // ---- Channel picker dialog (v2.6) ----
    if (showChannelPicker) {
        var pickedChannel by remember { mutableStateOf(MainViewModel.Channels.SMS) }
        var usernameText by remember { mutableStateOf("") }
        var usernameError by remember { mutableStateOf<String?>(null) }
        val dialogE164 = (validation as? PhoneUtils.NumberValidation.Valid)?.waNumber
        // Load a previously saved username for Telegram / Messenger
        // so the seller types it once, not every time.
        androidx.compose.runtime.LaunchedEffect(showChannelPicker, pickedChannel, dialogE164) {
            if (dialogE164 != null && pickedChannel != MainViewModel.Channels.SMS) {
                usernameText = vm.getChannelHandle(dialogE164, pickedChannel)?.username ?: ""
            } else {
                usernameText = ""
            }
        }
        fun openPickedChannel() {
            val e164 = dialogE164 ?: return
            val pkg = selectedApp ?: WhatsAppPackages.WHATSAPP
            val ok = when (pickedChannel) {
                MainViewModel.Channels.SMS -> vm.openChannel(
                    MainViewModel.Channels.SMS, e164, null, builtText()
                )
                else -> {
                    val user = usernameText.trim().trimStart('@')
                    if (user.isBlank()) {
                        usernameError = "Enter their $pickedChannel username first"
                        return
                    }
                    vm.saveChannelHandle(e164, pickedChannel, user)
                    vm.openChannel(pickedChannel, e164, user, builtText())
                }
            }
            showChannelPicker = false
            if (ok) {
                vm.recordRecent(e164, label.trim(), pkg, vm.templateById(selectedTemplateId)?.title)
                channelNote = when (pickedChannel) {
                    MainViewModel.Channels.SMS -> null
                    else -> "Opened in $pickedChannel — your message is not " +
                        "pre-filled there. Copy it from the preview if needed."
                }
            } else {
                // Honest fallback: the target app could not open, so
                // the wa.me chat opens instead, and the note says so.
                if (vm.openChat(pkg, e164, builtText())) {
                    channelNote = "Could not open $pickedChannel — " +
                        "opened WhatsApp instead."
                } else {
                    channelNote = "Could not open $pickedChannel or WhatsApp."
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showChannelPicker = false },
            title = { Text("Send on another app") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Opens that app's chat for this customer. " +
                            "Replies stay in that app — SellerChat does not receive messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((key, label) in listOf(
                            MainViewModel.Channels.TELEGRAM to "Telegram",
                            MainViewModel.Channels.MESSENGER to "Messenger",
                            MainViewModel.Channels.SMS to "SMS"
                        )) {
                            TextButton(onClick = { pickedChannel = key }) {
                                Text(
                                    text = label,
                                    color = if (pickedChannel == key) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    if (pickedChannel != MainViewModel.Channels.SMS) {
                        OutlinedTextField(
                            value = usernameText,
                            onValueChange = {
                                usernameText = it
                                usernameError = null
                            },
                            label = { Text("Their $pickedChannel username (without @)") },
                            singleLine = true,
                            isError = usernameError != null
                        )
                        if (usernameError != null) {
                            Text(
                                text = usernameError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Telegram and Messenger open the chat but do " +
                                "not pre-fill your message.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "SMS opens your messaging app with the text ready.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openPickedChannel() }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showChannelPicker = false }) { Text("Cancel") }
            }
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
