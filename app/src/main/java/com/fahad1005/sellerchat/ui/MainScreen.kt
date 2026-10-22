package com.fahad1005.sellerchat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.viewmodel.MainViewModel

private sealed interface Route {
    data object Main : Route
    data class Qr(val number: String, val message: String) : Route
    data object ShareStatus : Route
    data object Settings : Route
}

/** App root: first-run gate, then the three-tab shell (Home / Recents /
 *  Templates) with QR, Share-to-Status and Settings as pushed screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerChatRoot(vm: MainViewModel) {
    // Keep the installed-apps state fresh every time we return to the app
    // (e.g. right after the user installs WhatsApp from the Play Store).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshInstalledApps()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var firstRunDone by remember { mutableStateOf(vm.settings.firstRunDone) }
    if (!firstRunDone) {
        FirstRunScreen(vm = vm, onDone = {
            vm.settings.firstRunDone = true
            firstRunDone = true
        })
        return
    }

    var tab by rememberSaveable { mutableStateOf(0) }
    var route by remember { mutableStateOf<Route>(Route.Main) }
    var pendingEdit by remember { mutableStateOf<Template?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    when (val r = route) {
        is Route.Qr -> QrScreen(
            initialNumberE164 = r.number,
            initialMessage = r.message,
            onBack = { route = Route.Main }
        )
        is Route.ShareStatus -> ShareStatusScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Settings -> SettingsScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Main -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("SellerChat") },
                        actions = {
                            if (tab == 1) {
                                Box {
                                    IconButton(onClick = { overflowOpen = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                                    }
                                    DropdownMenu(
                                        expanded = overflowOpen,
                                        onDismissRequest = { overflowOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Clear all") },
                                            onClick = {
                                                overflowOpen = false
                                                showClearConfirm = true
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { route = Route.Settings }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                },
                bottomBar = {
                    // Keyboard rule (v2.1 task 1): while the soft keyboard
                    // is visible the bottom navigation HIDES — with
                    // adjustResize the window shrinks, and a nav bar left
                    // in place would sit on top of the keyboard looking
                    // like it floats. It returns when the keyboard closes.
                    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                    if (!imeVisible) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(painter = painterResource(R.drawable.ic_history), contentDescription = null) },
                            label = { Text("Recents") }
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(painter = painterResource(R.drawable.ic_description), contentDescription = null) },
                            label = { Text("Templates") }
                        )
                    }
                    } // end if (!imeVisible)
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (tab) {
                        0 -> HomeScreen(
                            vm = vm,
                            onShowQr = { number, message ->
                                route = Route.Qr(number, message)
                            },
                            onShareStatus = { route = Route.ShareStatus },
                            onEditTemplate = { template ->
                                pendingEdit = template
                                tab = 2
                            }
                        )
                        1 -> RecentsScreen(
                            vm = vm,
                            onOpenSettings = { route = Route.Settings }
                        )
                        else -> TemplatesScreen(
                            vm = vm,
                            pendingEdit = pendingEdit,
                            onPendingEditConsumed = { pendingEdit = null }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all recents?") },
            text = { Text("Every saved number will be removed from Recents. Templates are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearRecents()
                    showClearConfirm = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
