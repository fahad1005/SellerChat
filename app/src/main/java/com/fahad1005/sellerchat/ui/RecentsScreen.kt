package com.fahad1005.sellerchat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.data.Recent
import com.fahad1005.sellerchat.data.RecentStatus
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.util.TimeUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages
import kotlinx.coroutines.delay

private val WaitingOrange = Color(0xFFEF6C00)

fun statusLabel(status: RecentStatus): String = when (status) {
    RecentStatus.INQUIRY -> "Inquiry"
    RecentStatus.ORDERED -> "Ordered"
    RecentStatus.PAID -> "Paid"
    RecentStatus.SHIPPED -> "Shipped"
}

private fun nextStatus(status: RecentStatus): RecentStatus = when (status) {
    RecentStatus.INQUIRY -> RecentStatus.ORDERED
    RecentStatus.ORDERED -> RecentStatus.PAID
    RecentStatus.PAID -> RecentStatus.SHIPPED
    RecentStatus.SHIPPED -> RecentStatus.INQUIRY
}

/** "Waiting 25m" / "Waiting 3h 20m" / "Waiting 2d 4h" (spec §2C). */
private fun waitingText(statusChangedAt: Long, now: Long): String {
    val totalMinutes = ((now - statusChangedAt).coerceAtLeast(0L)) / 60_000L
    val hours = totalMinutes / 60
    val days = hours / 24
    return when {
        totalMinutes < 60 -> "Waiting ${totalMinutes}m"
        hours < 48 -> "Waiting ${hours}h ${totalMinutes % 60}m"
        else -> "Waiting ${days}d ${hours % 24}h"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(vm: MainViewModel) {
    val recents by vm.recents.collectAsState()
    val allRecents by vm.recentsAll.collectAsState()
    val query by vm.recentSearchQuery.collectAsState()
    val country by vm.selectedCountry.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // One clock for the whole screen: waiting timers + the Pulse week
    // window re-derive every 60 s while Recents is visible (spec §2C).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }

    var menuFor by remember { mutableStateOf<Recent?>(null) }
    var sheetFor by remember { mutableStateOf<Recent?>(null) }
    var renaming by remember { mutableStateOf<Recent?>(null) }
    var renameText by remember { mutableStateOf("") }
    var memoFor by remember { mutableStateOf<Recent?>(null) }
    var memoText by remember { mutableStateOf("") }
    var amountFor by remember { mutableStateOf<Recent?>(null) }
    var amountText by remember { mutableStateOf("") }

    // POST_NOTIFICATIONS is asked ONLY here, in-context, when the user
    // taps "Follow-up in 2 days" (spec §0) — never at launch. Denied
    // still saves the reminder, in-app only.
    var pendingFollowUp by remember { mutableStateOf<Recent?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingFollowUp?.let { vm.setFollowUp(it, granted) }
        pendingFollowUp = null
    }

    fun requestFollowUp(recent: Recent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                vm.setFollowUp(recent, withNotification = true)
            } else {
                pendingFollowUp = recent
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            vm.setFollowUp(recent, withNotification = true)
        }
    }

    fun askAmount(recent: Recent) {
        amountText = recent.paidAmountMinor?.toString() ?: ""
        amountFor = recent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Pulse dashboard (spec §4): one quiet text row, real
        // numbers from the unfiltered list — zero, never a placeholder.
        val waitingCount = allRecents.count { it.status == RecentStatus.INQUIRY }
        val paidCount = allRecents.count { it.status == RecentStatus.PAID }
        val weekStart = now - 7L * 24 * 3_600_000L
        val weekSum = allRecents
            .filter { (it.paidAt ?: 0L) >= weekStart && it.paidAt != null }
            .sumOf { it.paidAmountMinor ?: 0L }
        Text(
            text = "Leads: ${allRecents.size} | Waiting: $waitingCount | Paid: $paidCount | " +
                "This Week: ${CurrencyUtils.formatWithSymbol(weekSum, country)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = query,
            onValueChange = { vm.recentSearchQuery.value = it },
            placeholder = { Text("Search label, number or template") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (recents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (query.isBlank()) "Numbers you message will appear here."
                    else "No matches.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recents, key = { it.numberE164 }) { recent ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                vm.deleteRecent(recent.numberE164)
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                        RecentRow(
                            recent = recent,
                            now = now,
                            onTap = { sheetFor = recent },
                            onLongPress = { menuFor = recent },
                            onStatusChipTap = {
                                val next = nextStatus(recent.status)
                                if (next == RecentStatus.PAID) {
                                    // Cycling INTO Paid asks for the
                                    // amount first (Skip allowed).
                                    askAmount(recent)
                                } else {
                                    vm.setRecentStatus(recent, next)
                                }
                            }
                        )
                    }

                    if (menuFor?.numberE164 == recent.numberE164) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { menuFor = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename label") },
                                onClick = {
                                    renameText = recent.label
                                    renaming = recent
                                    menuFor = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy number") },
                                onClick = {
                                    clipboard.setText(AnnotatedString("+" + recent.numberE164))
                                    menuFor = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy wa.me link") },
                                onClick = {
                                    clipboard.setText(
                                        AnnotatedString(PhoneUtils.waMeLink(recent.numberE164))
                                    )
                                    menuFor = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    vm.deleteRecent(recent.numberE164)
                                    menuFor = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Row action sheet (spec §2C) ----
    val sheet = sheetFor
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetFor = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sheet.label.ifBlank { "+" + sheet.numberE164 },
                    style = MaterialTheme.typography.titleMedium
                )
                SheetAction("Open Chat") {
                    sheetFor = null
                    // Re-open the chat with NO template pre-filled.
                    vm.openChat(sheet.appPackage, sheet.numberE164, "")
                }
                SheetAction("Follow-up in 2 days") {
                    sheetFor = null
                    requestFollowUp(sheet)
                }
                SheetAction(if (sheet.memo.isNullOrBlank()) "Add Memo" else "Edit Memo") {
                    memoText = sheet.memo ?: ""
                    memoFor = sheet
                    sheetFor = null
                }
                SheetAction("Mark Paid") {
                    sheetFor = null
                    askAmount(sheet)
                }
            }
        }
    }

    // ---- Memo editor ----
    val memoTarget = memoFor
    if (memoTarget != null) {
        AlertDialog(
            onDismissRequest = { memoFor = null },
            title = { Text("Memo") },
            text = {
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("Note (e.g. Size 14, blue, deliver Friday)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setRecentMemo(memoTarget, memoText)
                    memoFor = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { memoFor = null }) { Text("Cancel") }
            }
        )
    }

    // ---- Paid amount prompt (optional; Skip keeps the amount empty) ----
    val amountTarget = amountFor
    if (amountTarget != null) {
        AlertDialog(
            onDismissRequest = { amountFor = null },
            title = { Text("Mark as Paid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Amount received (optional) — used by the This Week total.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount") },
                        prefix = { Text(CurrencyUtils.symbolFor(country) + " ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setRecentStatus(
                        amountTarget,
                        RecentStatus.PAID,
                        amountText.toLongOrNull()
                    )
                    amountFor = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.setRecentStatus(amountTarget, RecentStatus.PAID, null)
                    amountFor = null
                }) { Text("Skip") }
            }
        )
    }

    val toRename = renaming
    if (toRename != null) {
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename label") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameRecent(toRename, renameText.trim())
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SheetAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    recent: Recent,
    now: Long,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onStatusChipTap: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_chat_bubble),
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recent.label.ifBlank { "+" + recent.numberE164 },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "+${recent.numberE164} · ${WhatsAppPackages.displayName(recent.appPackage)} · ${TimeUtils.relative(recent.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Pinned memo, under the name (spec §2C).
                val memo = recent.memo
                if (!memo.isNullOrBlank()) {
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Waiting clock: only while the lead is still an inquiry.
                if (recent.status == RecentStatus.INQUIRY) {
                    val waitingHours = (now - recent.statusChangedAt).coerceAtLeast(0L) / 3_600_000L
                    Text(
                        text = waitingText(recent.statusChangedAt, now),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (waitingHours >= 24) MaterialTheme.colorScheme.error
                        else WaitingOrange
                    )
                }
                // Follow-up state, while it is still ahead of us.
                val followUpAt = recent.followUpAt
                if (followUpAt != null && followUpAt > now) {
                    Text(
                        text = if (recent.followUpNotified) "Reminder set" else "Reminder set (in-app)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status chip: tap cycles Inquiry → Ordered → Paid → Shipped.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (recent.status) {
                        RecentStatus.INQUIRY -> MaterialTheme.colorScheme.tertiaryContainer
                        RecentStatus.ORDERED -> MaterialTheme.colorScheme.secondaryContainer
                        RecentStatus.PAID -> MaterialTheme.colorScheme.primaryContainer
                        RecentStatus.SHIPPED -> MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.clickable(onClick = onStatusChipTap)
                ) {
                    Text(
                        text = statusLabel(recent.status),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                val tag = recent.templateTitle
                if (!tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
