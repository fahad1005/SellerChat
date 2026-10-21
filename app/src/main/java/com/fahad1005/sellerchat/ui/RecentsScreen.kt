package com.fahad1005.sellerchat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.data.Recent
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.util.TimeUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(vm: MainViewModel) {
    val recents by vm.recents.collectAsState()
    val query by vm.recentSearchQuery.collectAsState()
    val clipboard = LocalClipboardManager.current

    var menuFor by remember { mutableStateOf<Recent?>(null) }
    var renaming by remember { mutableStateOf<Recent?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.recentSearchQuery.value = it },
            placeholder = { Text("Search label or number") },
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
                            onTap = {
                                // Re-open the chat with NO template pre-filled.
                                vm.openChat(recent.appPackage, recent.numberE164, "")
                            },
                            onLongPress = { menuFor = recent }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(recent: Recent, onTap: () -> Unit, onLongPress: () -> Unit) {
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
