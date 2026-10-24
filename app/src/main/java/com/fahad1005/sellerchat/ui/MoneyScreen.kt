package com.fahad1005.sellerchat.ui

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fahad1005.sellerchat.data.Expense
import com.fahad1005.sellerchat.data.KhataEntry
import com.fahad1005.sellerchat.data.KhataEntryType
import com.fahad1005.sellerchat.data.PaymentMethod
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Start of the current day (device time) — the Today card's window. */
private fun dayStartMs(now: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun fmtDate(ms: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))

/**
 * Money tab (v2.6) — the business brain, and the ONLY place with a
 * business switcher (the 3-tab law: no 4th tab, no drawer).
 *
 * Top to bottom, one screen, nothing deeper than 2 taps:
 *   [shop switcher] → Today card → Khata → Expenses →
 *   Invoices / Catalog / Scheduled links → Share my shop.
 *
 * Money figures (Today sales, owed, expenses, profit) are for the
 * SELECTED shop in that shop's currency. The waiting / follow-up
 * line is explicitly global ("All shops") because customers,
 * unlike money, are not scoped to a shop.
 *
 * "Today sales" definition (documented, not hidden): khata
 * payments received today for this shop. Invoice Mark Paid still
 * updates the customer's global status (v2.5 behavior) — the
 * receipt itself belongs in the khata for the shop's books.
 */
@Composable
fun MoneyScreen(
    vm: MainViewModel,
    onOpenInvoices: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenKhataDetail: (number: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val businesses by vm.businesses.collectAsState()
    val selectedBusiness by vm.selectedBusiness.collectAsState()
    val khata by vm.khataEntries.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val recentsAll by vm.recentsAll.collectAsState()

    val currency = selectedBusiness?.currency ?: vm.settings.currencyCode
    val now = System.currentTimeMillis()
    val dayStart = dayStartMs(now)

    // ---- Today figures (selected shop, shop currency) ----
    val salesToday = khata
        .filter { it.type == KhataEntryType.PAYMENT && it.createdAt >= dayStart }
        .sumOf { it.amount }
    val expensesToday = expenses
        .filter { it.createdAt >= dayStart }
        .sumOf { it.amount }
    val balances = khata.groupBy { it.customerNumber }
        .mapValues { (_, rows) -> vm.khataBalance(rows) }
    val owedTotal = balances.values.filter { it > 0 }.sum()
    val profitToday = salesToday - expensesToday

    // ---- Global counts (customers are not scoped) ----
    val waitingCount = recentsAll.count { it.status.name == "INQUIRY" }
    val followUpsDue = recentsAll.count {
        it.followUpAt != null && it.followUpAt <= now
    }

    var switcherOpen by remember { mutableStateOf(false) }
    var khataFilter by remember { mutableStateOf("All") }
    var khataQuery by remember { mutableStateOf("") }
    var showCustomerPick by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    var shopNumber by remember { mutableStateOf(vm.settings.myNumberE164 ?: "") }
    var shopSource by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Shop switcher ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Shop:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 14.dp)
            )
            OutlinedButton(onClick = { switcherOpen = true }) {
                Text(selectedBusiness?.name ?: "My Business")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch shop")
            }
            DropdownMenu(
                expanded = switcherOpen,
                onDismissRequest = { switcherOpen = false }
            ) {
                for (b in businesses) {
                    DropdownMenuItem(
                        text = { Text(b.name + " (" + b.currency + ")") },
                        onClick = {
                            vm.selectBusiness(b.id)
                            switcherOpen = false
                        }
                    )
                }
            }
        }

        // ---- Today card: the whole shop in one look ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today · ${selectedBusiness?.name ?: "My Business"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Sales today: " +
                        CurrencyUtils.formatWithSymbol(salesToday, currency),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Owed to you: " +
                        CurrencyUtils.formatWithSymbol(owedTotal, currency),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Expenses today: " +
                        CurrencyUtils.formatWithSymbol(expensesToday, currency),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Profit today: " +
                        CurrencyUtils.formatWithSymbol(profitToday, currency),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Waiting: $waitingCount · Follow-ups due: $followUpsDue (all shops)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Khata ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Khata — who owes you", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (f in listOf("All", "To receive", "Advance", "Settled")) {
                        TextButton(onClick = { khataFilter = f }) {
                            Text(
                                text = f,
                                color = if (khataFilter == f) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = khataQuery,
                    onValueChange = { khataQuery = it },
                    placeholder = { Text("Search name or number") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val q = khataQuery.trim()
                val customers = balances.entries
                    .filter { (_, balance) ->
                        when (khataFilter) {
                            "To receive" -> balance > 0
                            "Advance" -> balance < 0
                            "Settled" -> balance == 0L
                            else -> true
                        }
                    }
                    .filter { (number, _) ->
                        if (q.isBlank()) true else {
                            val name = khata.firstOrNull { it.customerNumber == number }
                                ?.customerName.orEmpty()
                            name.contains(q, ignoreCase = true) ||
                                number.contains(q, ignoreCase = true)
                        }
                    }
                    .sortedByDescending { (_, balance) -> balance }
                if (customers.isEmpty()) {
                    Text(
                        text = "No khata entries yet. Open a customer below to add udhaar or a payment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    for ((number, balance) in customers) {
                        val name = khata.firstOrNull { it.customerNumber == number }
                            ?.customerName.orEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { onOpenKhataDetail(number, name) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = (name.ifBlank { number } + " — " +
                                        vm.khataBalanceText(balance, currency)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Button(onClick = { showCustomerPick = true }) {
                    Text("Add entry for a customer")
                }
                Text(
                    text = "Tip: tap a customer to open their full ledger, add entries and share a statement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Expenses ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Expenses", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showExpenseDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add expense")
                    }
                }
                if (expenses.isEmpty()) {
                    Text(
                        text = "No expenses yet — packaging, courier and stock costs go here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    for (e in expenses.take(20)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = (if (e.note.isBlank()) "Expense" else e.note) +
                                    " · " + fmtDate(e.createdAt),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyUtils.formatWithSymbol(e.amount, currency),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { expenseToDelete = e }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete expense"
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Existing business tools (linked, not duplicated) ----
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
            }
        }

        // ---- Share my shop: catalog card + ad link ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Share my shop", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "One text with your products, prices and a chat link for ads, status or bio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = shopNumber,
                    onValueChange = { shopNumber = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Your WhatsApp number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopSource,
                    onValueChange = { shopSource = it },
                    label = { Text("Ad source (optional, e.g. Instagram)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (shopNumber.isNotBlank()) {
                            vm.settings.myNumberE164 = shopNumber
                        }
                        val text = vm.catalogCardText(shopNumber, shopSource)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Share my shop")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Share shop card")
                }
            }
        }
    }

    if (showCustomerPick) {
        CustomerPickDialog(
            recents = recentsAll,
            onDismiss = { showCustomerPick = false },
            onPick = { number, name ->
                showCustomerPick = false
                onOpenKhataDetail(number, name)
            }
        )
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { amount, note ->
                vm.addExpense(amount, note)
                showExpenseDialog = false
            }
        )
    }

    val doomed = expenseToDelete
    if (doomed != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete this expense?") },
            text = { Text("It will stop counting toward today's profit.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteExpense(doomed)
                    expenseToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/** Pick a customer to open their ledger: recent customers as
 *  one-tap rows, or a typed name + number for a brand-new one.
 *  The ledger itself (entries, statement) lives on the detail
 *  screen — this dialog only chooses who. */
@Composable
private fun CustomerPickDialog(
    recents: List<com.fahad1005.sellerchat.data.Recent>,
    onDismiss: () -> Unit,
    onPick: (number: String, name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Whose ledger?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Customer number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                if (recents.isNotEmpty()) {
                    Text(
                        text = "Or pick a recent customer:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        for (r in recents.take(15)) {
                            TextButton(onClick = {
                                onPick(r.numberE164, r.label)
                            }) {
                                Text(
                                    text = (r.label.ifBlank { r.numberE164 }) +
                                        " · " + r.numberE164,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPick(number, name) },
                enabled = number.isNotBlank()
            ) { Text("Open ledger") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** "Add → amount → save" — the same shape as the invoice and
 *  product forms, so learning one teaches all. */
@Composable
private fun ExpenseDialog(onDismiss: () -> Unit, onSave: (Long, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("What for (packaging, courier…)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) onSave(amount, note)
                },
                enabled = (amountText.toLongOrNull() ?: 0L) > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * One customer's full ledger inside the selected shop (pushed
 * screen, 1 tap from the Money tab). Add → amount → save, same
 * shape as every other form: type chips, method chips for
 * payments, optional note, and for udhaar a due-date chip row
 * (None / Tomorrow / 3 days / 7 days) plus "Remind me".
 */
@Composable
fun KhataDetailScreen(
    vm: MainViewModel,
    number: String,
    name: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val entries by vm.khataForCustomer(number).collectAsState()
    val selectedBusiness by vm.selectedBusiness.collectAsState()
    val currency = selectedBusiness?.currency ?: vm.settings.currencyCode
    val balance = vm.khataBalance(entries)

    var showAdd by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<KhataEntry?>(null) }

    // Due-date reminder permission: asked in-context ONLY when the
    // seller saves an udhaar entry with "Remind me" on — never at
    // launch, never for any other action.
    var pendingReminder by remember { mutableStateOf<KhataEntry?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingReminder?.let { entry ->
            if (granted) vm.scheduleKhataReminder(entry, true)
        }
        pendingReminder = null
    }

    fun requestReminder(entry: KhataEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                vm.scheduleKhataReminder(entry, true)
            } else {
                pendingReminder = entry
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            vm.scheduleKhataReminder(entry, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Back")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = name.ifBlank { number },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Balance: " + vm.khataBalanceText(balance, currency),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = number + " · " + (selectedBusiness?.name ?: "My Business"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showAdd = true }) { Text("Add entry") }
                    OutlinedButton(onClick = {
                        val text = vm.khataStatementText(
                            name.ifBlank { number }, entries, currency
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Share statement")
                        )
                    }) { Text("Share statement") }
                }
            }
        }

        if (entries.isEmpty()) {
            Text(
                text = "No entries yet. Add udhaar when you give credit, a payment when money arrives, or an advance when a customer pays extra.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (e in entries) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val kind = when (e.type) {
                                KhataEntryType.UDHAAR -> "Udhaar"
                                KhataEntryType.PAYMENT -> "Payment" +
                                    (e.method?.let { " · " + it.name.lowercase() } ?: "")
                                KhataEntryType.ADVANCE -> "Advance"
                            }
                            Text(text = kind, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = fmtDate(e.createdAt) +
                                    (e.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (e.type == KhataEntryType.UDHAAR && e.dueAt != null) {
                                val overdue = e.dueAt <= System.currentTimeMillis()
                                Text(
                                    text = if (overdue) {
                                        "Due ${fmtDate(e.dueAt)} — overdue"
                                    } else {
                                        "Due ${fmtDate(e.dueAt)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overdue) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        Text(
                            text = CurrencyUtils.formatWithSymbol(e.amount, currency),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { entryToDelete = e }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        KhataEntryDialog(
            onDismiss = { showAdd = false },
            onSave = { type, method, amount, note, dueAt, remind ->
                vm.addKhataEntry(
                    customerNumber = number,
                    customerName = name,
                    type = type,
                    method = method,
                    amount = amount,
                    note = note,
                    dueAt = dueAt
                ) { saved ->
                    if (remind && saved.dueAt != null) {
                        requestReminder(saved)
                    }
                }
                showAdd = false
            }
        )
    }

    val doomed = entryToDelete
    if (doomed != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("The balance will be recalculated without it.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteKhataEntry(doomed)
                    entryToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/** Khata add form: type → method (payments only) → amount → note →
 *  due date (udhaar only) → remind toggle. Same add → amount →
 *  save shape as every other form in the app. */
@Composable
private fun KhataEntryDialog(
    onDismiss: () -> Unit,
    onSave: (
        KhataEntryType, PaymentMethod?, Long, String?, Long?, Boolean
    ) -> Unit
) {
    var type by remember { mutableStateOf(KhataEntryType.UDHAAR) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDays by remember { mutableStateOf<Int?>(null) }
    var remind by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add khata entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in KhataEntryType.values()) {
                        TextButton(onClick = { type = t }) {
                            Text(
                                text = when (t) {
                                    KhataEntryType.UDHAAR -> "Udhaar"
                                    KhataEntryType.PAYMENT -> "Payment"
                                    KhataEntryType.ADVANCE -> "Advance"
                                },
                                color = if (type == t) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                if (type == KhataEntryType.PAYMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in PaymentMethod.values()) {
                            TextButton(onClick = { method = m }) {
                                Text(
                                    text = when (m) {
                                        PaymentMethod.CASH -> "Cash"
                                        PaymentMethod.JAZZCASH -> "JazzCash"
                                        PaymentMethod.EASYPEPAISA -> "Easypaisa"
                                        PaymentMethod.BANK -> "Bank"
                                    },
                                    color = if (method == m) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
                if (type == KhataEntryType.UDHAAR) {
                    Text(
                        text = "Payment due:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { dueDays = null; remind = false }) {
                            Text(
                                text = "None",
                                color = if (dueDays == null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        for (d in listOf(1, 3, 7)) {
                            TextButton(onClick = { dueDays = d }) {
                                Text(
                                    text = if (d == 1) "Tomorrow" else "$d days",
                                    color = if (dueDays == d) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    if (dueDays != null) {
                        TextButton(onClick = { remind = !remind }) {
                            Text(
                                text = if (remind) {
                                    "✓ Remind me on the due date"
                                } else {
                                    "Remind me on the due date"
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        val dueAt = dueDays?.let {
                            System.currentTimeMillis() + it * 24L * 3_600_000L
                        }
                        onSave(
                            type,
                            if (type == KhataEntryType.PAYMENT) method else null,
                            amount,
                            note.ifBlank { null },
                            dueAt,
                            remind
                        )
                    }
                },
                enabled = (amountText.toLongOrNull() ?: 0L) > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
