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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.BuildConfig
import com.fahad1005.sellerchat.ui.components.CountryPickerDialog
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages

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
            // Default country
            Text("Default country", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { showCountryPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${country.flag}  ${country.name}  (+${country.dial})")
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

            // Default template
            Text("Default template", style = MaterialTheme.typography.titleSmall)
            Column {
                OutlinedButton(
                    onClick = { templateMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = templates.firstOrNull { it.id == defaultTemplateId }?.title
                            ?: "No template"
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = templateMenuOpen,
                    onDismissRequest = { templateMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No template") },
                        onClick = {
                            defaultTemplateId = -1L
                            vm.settings.defaultTemplateId = -1L
                            vm.selectedTemplateId.value = -1L
                            templateMenuOpen = false
                        }
                    )
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
