package com.fahad1005.sellerchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.data.Invoice
import com.fahad1005.sellerchat.data.InvoiceLine
import com.fahad1005.sellerchat.data.InvoiceStatus
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages
import java.text.DateFormat
import java.util.Date

/** Invoices (v2.5): the seller's bill book. List filters Unpaid /
 *  Paid; the detail dialog shows lines + total and offers "Send via
 *  WhatsApp" (pre-filled invoice text, never auto-sent) and "Mark
 *  Paid" (which also marks the linked Recent PAID so Pulse picks the
 *  payment up). Numbers are INV-001, INV-002, … and never reused. */
@Composable
fun InvoicesScreen(vm: MainViewModel, onBack: () -> Unit = {}) {
    val invoices by vm.invoices.collectAsState()
    val recents by vm.recentsAll.collectAsState()
    val country by vm.selectedCountry.collectAsState()
    val selectedApp by vm.selectedAppPackage.collectAsState()

    var filter by remember { mutableStateOf("Unpaid") }
    var showCreate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Invoice?>(null) }

    val shown = when (filter) {
        "Unpaid" -> invoices.filter { it.status == InvoiceStatus.UNPAID }
        "Paid" -> invoices.filter { it.status == InvoiceStatus.PAID }
        else -> invoices
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Unpaid", "Paid", "All").forEach { f ->
                OutlinedButton(onClick = { filter = f }) {
                    Text(if (f == filter) "● $f" else f)
                }
            }
        }
        if (shown.isEmpty()) {
            Text(
                text = if (filter == "Unpaid") {
                    "No unpaid invoices. Create one and it appears here until it is paid."
                } else {
                    "No invoices here yet."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { invoice ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${invoice.number} — ${invoice.customerName}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "+${invoice.customerNumber} · " +
                                    DateFormat.getDateInstance(DateFormat.MEDIUM)
                                        .format(Date(invoice.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                TextButton(onClick = { detail = invoice }) { Text("View") }
                                TextButton(onClick = { vm.deleteInvoice(invoice) }) { Text("Delete") }
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
            Text("New invoice")
        }
    }

    if (showCreate) {
        InvoiceCreateDialog(
            vm = vm,
            recents = recents,
            country = country,
            onDismiss = { showCreate = false },
            onSave = { name, e164, recentNumber, notes, lines ->
                vm.createInvoice(name, e164, recentNumber, notes, lines)
                showCreate = false
            }
        )
    }

    val target = detail
    if (target != null) {
        InvoiceDetailDialog(
            vm = vm,
            invoice = target,
            pkg = selectedApp ?: WhatsAppPackages.WHATSAPP,
            onDismiss = { detail = null }
        )
    }
}

@Composable
private fun InvoiceDetailDialog(vm: MainViewModel, invoice: Invoice, pkg: String, onDismiss: () -> Unit) {
    val lines by vm.invoiceLines(invoice.id).collectAsState()
    val total = lines.sumOf { it.amount }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invoice ${invoice.number}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Customer: ${invoice.customerName} (+${invoice.customerNumber})")
                lines.forEach { line ->
                    Text(
                        "• ${line.description} x${line.qty} — " +
                            CurrencyUtils.formatWithSymbol(line.amount, vm.settings.currencyCode)
                    )
                }
                Text(
                    "Total: " +
                        CurrencyUtils.formatWithSymbol(total, vm.settings.currencyCode),
                    style = MaterialTheme.typography.titleMedium
                )
                invoice.notes?.takeIf { it.isNotBlank() }?.let { Text("Note: $it") }
                Text(
                    "Status: ${invoice.status.name}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (vm.sendInvoice(pkg, invoice, lines)) {
                    vm.recordRecent(
                        invoice.customerNumber, invoice.customerName, pkg,
                        "Invoice ${invoice.number}"
                    )
                }
                onDismiss()
            }) { Text("Send via WhatsApp") }
        },
        dismissButton = {
            Row {
                if (invoice.status == InvoiceStatus.UNPAID) {
                    TextButton(onClick = {
                        vm.markInvoicePaid(invoice)
                        onDismiss()
                    }) { Text("Mark Paid") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

private data class LineDraft(var desc: String, var qty: String, var amount: String)

@Composable
private fun InvoiceCreateDialog(
    vm: MainViewModel,
    recents: List<com.fahad1005.sellerchat.data.Recent>,
    country: com.fahad1005.sellerchat.data.Country,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?, List<Triple<String, Int, Long>>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var recentNumber by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf(LineDraft("", "1", ""))) }
    var pickOpen by remember { mutableStateOf(false) }
    var numberError by remember { mutableStateOf<String?>(null) }

    val parsedLines = lines.mapNotNull { draft ->
        val amount = draft.amount.filter { it.isDigit() }.toLongOrNull()
        if (draft.desc.isNotBlank() && amount != null) {
            Triple(draft.desc, draft.qty.filter { it.isDigit() }.toIntOrNull() ?: 1, amount)
        } else {
            null
        }
    }
    val total = parsedLines.sumOf { it.third }
    val canSave = name.isNotBlank() && number.isNotBlank() && parsedLines.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New invoice") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { pickOpen = true }) {
                    Text(if (recentNumber == null) "Pick an existing customer…" else "Customer picked ✓")
                }
                if (pickOpen) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { pickOpen = false }
                    ) {
                        recents.forEach { recent ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        recent.label.ifBlank { "+" + recent.numberE164 }
                                    )
                                },
                                onClick = {
                                    name = recent.label.ifBlank { recent.numberE164 }
                                    number = recent.numberE164
                                    recentNumber = recent.numberE164
                                    pickOpen = false
                                }
                            )
                        }
                    }
                }
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
                    placeholder = { Text("+${country.dial} …") },
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
                Text("Lines", style = MaterialTheme.typography.titleSmall)
                lines.forEachIndexed { index, draft ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.desc,
                            onValueChange = { value ->
                                lines = lines.toMutableList()
                                    .also { list -> list[index] = draft.copy(desc = value) }
                            },
                            label = { Text("Item") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.qty,
                            onValueChange = { value ->
                                lines = lines.toMutableList()
                                    .also { list ->
                                        list[index] = draft.copy(
                                            qty = value.filter { c -> c.isDigit() }
                                        )
                                    }
                            },
                            label = { Text("Qty") },
                            singleLine = true,
                            modifier = Modifier.weight(0.5f)
                        )
                        OutlinedTextField(
                            value = draft.amount,
                            onValueChange = { value ->
                                lines = lines.toMutableList()
                                    .also { list ->
                                        list[index] = draft.copy(
                                            amount = value.filter { c -> c.isDigit() }
                                        )
                                    }
                            },
                            label = { Text("Amount") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                        IconButton(onClick = {
                            lines = lines.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove line")
                        }
                    }
                }
                TextButton(onClick = {
                    lines = lines + LineDraft("", "1", "")
                }) { Text("+ Add line") }
                Text(
                    "Total: " +
                        CurrencyUtils.formatWithSymbol(total, vm.settings.currencyCode),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
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
                        onSave(name, e164, recentNumber, notes, parsedLines)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
