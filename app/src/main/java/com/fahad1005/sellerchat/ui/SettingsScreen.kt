package com.fahad1005.sellerchat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.BuildConfig
import com.fahad1005.sellerchat.data.RecentStatus
import com.fahad1005.sellerchat.data.SettingsStore
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.ui.components.CountryPickerDialog
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages

/** Display names for the template languages (UI stays English, §3). */
fun templateLanguageLabel(language: TemplateLanguage): String = when (language) {
    TemplateLanguage.EN -> "English"
    TemplateLanguage.UR -> "Urdu"
    TemplateLanguage.ROMAN_UR -> "Roman Urdu"
    TemplateLanguage.HI -> "Hindi"
    TemplateLanguage.TA -> "Tamil"
    TemplateLanguage.TE -> "Telugu"
    TemplateLanguage.MR -> "Marathi"
    TemplateLanguage.FR -> "French"
    TemplateLanguage.ES -> "Spanish"
    TemplateLanguage.AR -> "Arabic"
    TemplateLanguage.ZH -> "Chinese"
}

/** TODO(FAHAD): replace with the real hosted privacy policy URL. */
private const val PRIVACY_POLICY_URL = "https://example.com/sellerchat/privacy-policy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val country by vm.selectedCountry.collectAsState()
    val templates by vm.templates.collectAsState()
    val installedWa by vm.installedWhatsApp.collectAsState()
    val installedWab by vm.installedWhatsAppBusiness.collectAsState()

    var showCountryPicker by remember { mutableStateOf(false) }
    var defaultApp by remember { mutableStateOf(vm.settings.defaultAppPackage) }
    var defaultTemplateId by remember { mutableStateOf(vm.settings.defaultTemplateId) }
    var templateMenuOpen by remember { mutableStateOf(false) }
    var preferredLanguage by remember { mutableStateOf(vm.settings.preferredTemplateLanguage) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    var currencyCode by remember { mutableStateOf(vm.settings.currencyCode) }
    var currencyMenuOpen by remember { mutableStateOf(false) }
    var defaultStatus by remember { mutableStateOf(vm.settings.defaultStatus) }
    var statusMenuOpen by remember { mutableStateOf(false) }
    var teammates by remember { mutableStateOf(vm.settings.getTeammates()) }
    var teamName by remember { mutableStateOf("") }
    var teamNumber by remember { mutableStateOf("") }
    var teamError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Region & Currency (v2.3): country, currency and the
            // preferred template language grouped in ONE section, each
            // one a single source of truth backed by the same storage
            // key it always had (plus the new currency code). The
            // one-time v2.3 detection initialized all three; whatever
            // the seller picks here overrides it and persists.
            Text("Region & Currency", style = MaterialTheme.typography.titleMedium)

            Text("Country", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { showCountryPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${country.flag}  ${country.name}  (+${country.dial})")
            }

            Text("Currency", style = MaterialTheme.typography.titleSmall)
            Column {
                OutlinedButton(
                    onClick = { currencyMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "$currencyCode (${CurrencyUtils.symbolForCode(currencyCode)})")
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = currencyMenuOpen,
                    onDismissRequest = { currencyMenuOpen = false }
                ) {
                    CurrencyUtils.supportedCurrencies.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.code} (${option.symbol})") },
                            onClick = {
                                currencyCode = option.code
                                vm.settings.currencyCode = option.code
                                currencyMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Preferred template language (v2 spec §3A): Home shows
            // these template chips first. Moved into this section in
            // v2.3 — same setting, same key, one UI instance only.
            Text("Preferred template language", style = MaterialTheme.typography.titleSmall)
            Column {
                OutlinedButton(
                    onClick = { languageMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = preferredLanguage?.let { templateLanguageLabel(it) } ?: "Any"
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = languageMenuOpen,
                    onDismissRequest = { languageMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Any") },
                        onClick = {
                            preferredLanguage = null
                            vm.settings.preferredTemplateLanguage = null
                            languageMenuOpen = false
                        }
                    )
                    TemplateLanguage.values().forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(templateLanguageLabel(lang)) },
                            onClick = {
                                preferredLanguage = lang
                                vm.settings.preferredTemplateLanguage = lang
                                languageMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Default app
            Text("Default app", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (installedWa) {
                    SettingsChoiceChip(
                        text = "WhatsApp",
                        selected = defaultApp == WhatsAppPackages.WHATSAPP,
                        onClick = {
                            defaultApp = WhatsAppPackages.WHATSAPP
                            vm.settings.defaultAppPackage = WhatsAppPackages.WHATSAPP
                        }
                    )
                }
                if (installedWab) {
                    SettingsChoiceChip(
                        text = "WhatsApp Business",
                        selected = defaultApp == WhatsAppPackages.WHATSAPP_BUSINESS,
                        onClick = {
                            defaultApp = WhatsAppPackages.WHATSAPP_BUSINESS
                            vm.settings.defaultAppPackage = WhatsAppPackages.WHATSAPP_BUSINESS
                        }
                    )
                }
                if (!installedWa && !installedWab) {
                    Text(
                        text = "No WhatsApp app installed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Default template (v2 spec §2B: the default may NOT be
            // "No template" — the picker no longer offers it; per-send
            // "No template" on Home is unaffected.)
            Text("Default template", style = MaterialTheme.typography.titleSmall)
            Column {
                OutlinedButton(
                    onClick = { templateMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = templates.firstOrNull { it.id == defaultTemplateId }?.title
                            ?: "Choose a template"
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = templateMenuOpen,
                    onDismissRequest = { templateMenuOpen = false }
                ) {
                    templates.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.title) },
                            onClick = {
                                defaultTemplateId = t.id
                                vm.settings.defaultTemplateId = t.id
                                vm.selectedTemplateId.value = t.id
                                templateMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Default status for new recents (v2 spec §5).
            Text("Default status for new customers", style = MaterialTheme.typography.titleSmall)
            Column {
                OutlinedButton(
                    onClick = { statusMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = statusLabel(defaultStatus))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = statusMenuOpen,
                    onDismissRequest = { statusMenuOpen = false }
                ) {
                    RecentStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = { Text(statusLabel(status)) },
                            onClick = {
                                defaultStatus = status
                                vm.settings.defaultStatus = status
                                statusMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Team Members (v2.2): up to 3 teammates for "Forward to
            // Teammate" on the Recents sheet. Name + number, stored
            // only on this phone (SettingsStore slots) — no account,
            // no sync, nothing uploaded.
            Text("Team Members", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                teammates.forEach { mate ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${mate.name} — ${mate.number}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            vm.settings.removeTeammate(mate)
                            teammates = vm.settings.getTeammates()
                        }) { Text("Delete") }
                    }
                }
                if (teammates.size < SettingsStore.MAX_TEAMMATES) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = {
                            teamName = it
                            teamError = null
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teamNumber,
                        onValueChange = {
                            teamNumber = it
                            teamError = null
                        },
                        label = { Text("Phone number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    teamError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val digits = teamNumber.count { c -> c.isDigit() }
                            if (teamName.isBlank() || digits < 7) {
                                teamError =
                                    "Enter a name and a valid number (at least 7 digits)."
                            } else if (vm.settings.addTeammate(teamName, teamNumber)) {
                                teammates = vm.settings.getTeammates()
                                teamName = ""
                                teamNumber = ""
                                teamError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add teammate") }
                }
            }

            // Privacy block
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                        Text(
                            "Your data stays on your phone",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "Your numbers, labels and templates are stored only on this phone. " +
                            "SellerChat has no account, no cloud and no tracking. Nothing is uploaded or shared.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
            }) { Text("Privacy policy") }

            TextButton(onClick = {
                val market = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${context.packageName}")
                )
                try {
                    context.startActivity(market)
                } catch (e: Exception) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                        )
                    )
                }
            }) { Text("Rate this app") }

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@Composable
private fun SettingsChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
