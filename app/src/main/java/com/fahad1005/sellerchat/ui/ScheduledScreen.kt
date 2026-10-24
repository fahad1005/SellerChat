package com.fahad1005.sellerchat.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fahad1005.sellerchat.data.ScheduledStatus
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Scheduled messages (v2.5) — reminder-to-send, NEVER auto-send.
 *  At the chosen time a notification fires; tapping it (or "Send
 *  now" here) opens WhatsApp with the approved text pre-filled and
 *  marks the item SENT. Dismissing the notification leaves the item
 *  PENDING and it shows as Due here. The notification permission is
 *  asked in-context at the schedule tap, exactly like the Recents
 *  follow-up pattern; denied still saves the item in-app. */
@Composable
fun ScheduledScreen(vm: MainViewModel, onBack: () -> Unit = {}) {
    val items by vm.scheduledMessages.collectAsState()
    val templates by vm.templates.collectAsState()
    val country by vm.selectedCountry.collectAsState()
    val selectedApp by vm.selectedAppPackage.collectAsState()
    val installedWa by vm.installedWhatsApp.collectAsState()
    val installedWab by vm.installedWhatsAppBusiness.collectAsState()
    val context = LocalContext.current
    val now = System.currentTimeMillis()

    var filter by remember { mutableStateOf("Pending") }
    var showCreate by remember { mutableStateOf(false) }

    // Permission state for the in-context ask (mirrors Recents).
    var pendingDraft by remember {
        mutableStateOf<Triple<String, String, Triple<String, String, Long>>?>(null)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingDraft?.let { (name, number, rest) ->
            val (pkg, text, at) = rest
            vm.scheduleMessage(name, number, pkg, text, at, granted)
        }
        pendingDraft = null
    }

    fun requestSchedule(name: String, number: String, pkg: String, text: String, at: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                vm.scheduleMessage(name, number, pkg, text, at, true)
            } else {
                pendingDraft = Triple(name, number, Triple(pkg, text, at))
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            vm.scheduleMessage(name, number, pkg, text, at, true)
        }
    }

    val shown = when (filter) {
        "Pending" -> items.filter { it.status == ScheduledStatus.PENDING }
        "Sent" -> items.filter { it.status == ScheduledStatus.SENT }
        "Cancelled" -> items.filter { it.status == ScheduledStatus.CANCELLED }
        else -> items
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Back")
        }
        Text(
            text = "Nothing sends itself: at the time you pick, you get a reminder and tap to send in WhatsApp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Pending", "Sent", "Cancelled", "All").forEach { f ->
                OutlinedButton(onClick = { filter = f }) {
                    Text(if (f == filter) "● $f" else f)
                }
            }
        }
        if (shown.isEmpty()) {
            Text(
                text = "Nothing here yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { item ->
                    val due = item.status == ScheduledStatus.PENDING && item.scheduledAt <= now
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.customerName.ifBlank { "+" + item.customerNumber },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = DateFormat.getDateTimeInstance(
                                    DateFormat.MEDIUM, DateFormat.SHORT
                                ).format(Date(item.scheduledAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (due) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (due) {
                                Text(
                                    text = "Due now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                            Row {
                                if (item.status == ScheduledStatus.PENDING) {
                                    TextButton(onClick = { vm.sendScheduledNow(item) }) {
                                        Text("Send now")
                                    }
                                    TextButton(onClick = { vm.cancelScheduled(item) }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { showCreate = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Schedule a message")
        }
    }

    if (showCreate) {
        ScheduleCreateDialog(
            vm = vm,
            templates = templates,
            country = country,
            installedWa = installedWa,
            installedWab = installedWab,
            defaultPkg = selectedApp,
            onDismiss = { showCreate = false },
            onSave = { name, number, pkg, text, at ->
                requestSchedule(name, number, pkg, text, at)
                showCreate = false
            }
        )
    }
}

@Composable
private fun ScheduleCreateDialog(
    vm: MainViewModel,
    templates: List<Template>,
    country: com.fahad1005.sellerchat.data.Country,
    installedWa: Boolean,
    installedWab: Boolean,
    defaultPkg: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var numberError by remember { mutableStateOf<String?>(null) }
    var useCustom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var pickedTemplate by remember { mutableStateOf<Template?>(templates.firstOrNull()) }
    var templateMenuOpen by remember { mutableStateOf(false) }
    var pkg by remember {
        mutableStateOf(
            defaultPkg ?: when {
                installedWa -> WhatsAppPackages.WHATSAPP
                installedWab -> WhatsAppPackages.WHATSAPP_BUSINESS
                else -> WhatsAppPackages.WHATSAPP
            }
        )
    }
    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var atText by remember {
        mutableStateOf(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(calendar.timeInMillis))
        )
    }

    fun refreshAtText() {
        atText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(calendar.timeInMillis))
    }

    // Templates arrive from Room after first composition, so fall back
    // to the first loaded template until the seller picks one — otherwise
    // the preview (and Save) would stay empty on a fresh open.
    val effectiveTemplate = pickedTemplate ?: templates.firstOrNull()
    val previewText = if (useCustom) {
        customText
    } else {
        vm.filledTemplateTextFor(effectiveTemplate, name)
    }
    val canSave = name.isNotBlank() && number.isNotBlank() &&
        previewText.isNotBlank() && calendar.timeInMillis > System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule a message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = {
                        number = it.filter { c -> c.isDigit() || c == '+' }
                        numberError = null
                    },
                    label = { Text("Customer number") },
                    singleLine = true,
                    isError = numberError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (numberError != null) {
                    Text(
                        text = numberError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (installedWa) {
                        OutlinedButton(onClick = { pkg = WhatsAppPackages.WHATSAPP }) {
                            Text(
                                if (pkg == WhatsAppPackages.WHATSAPP) "● WhatsApp" else "WhatsApp"
                            )
                        }
                    }
                    if (installedWab) {
                        OutlinedButton(onClick = { pkg = WhatsAppPackages.WHATSAPP_BUSINESS }) {
                            Text(
                                if (pkg == WhatsAppPackages.WHATSAPP_BUSINESS) {
                                    "● WhatsApp Business"
                                } else {
                                    "WhatsApp Business"
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { templateMenuOpen = true }) {
                    Text(
                        if (useCustom) "Custom text…" else (effectiveTemplate?.title ?: "Pick a template…")
                    )
                }
                if (templateMenuOpen) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { templateMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Custom text…") },
                            onClick = {
                                useCustom = true
                                templateMenuOpen = false
                            }
                        )
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.title) },
                                onClick = {
                                    useCustom = false
                                    pickedTemplate = template
                                    templateMenuOpen = false
                                }
                            )
                        }
                    }
                }
                if (useCustom) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (previewText.isNotBlank()) {
                    Text(
                        text = "Preview: $previewText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, day)
                                refreshAtText()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) { Text("Pick date") }
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                refreshAtText()
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }) { Text("Pick time") }
                }
                Text(
                    text = "Scheduled for: $atText",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val e164 = PhoneUtils.normalize(number, country)
                    if (e164 == null) {
                        numberError = "Invalid number for selected country"
                    } else {
                        onSave(name, e164, pkg, previewText, calendar.timeInMillis)
                    }
                }
            ) { Text("Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
