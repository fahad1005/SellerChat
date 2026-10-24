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
    // v2.5 business tools: pushed screens, each one tap from the
    // "Business tools" row on Home or Money (two taps from any tab).
    data object Invoices : Route
    data object Catalog : Route
    data object Scheduled : Route
    // v2.6: Templates is no longer a tab (the 3-tab law: Sell /
    // Customers / Money only) — it is a pushed screen from Home,
    // and the template-preview "Edit" flow lands here too.
    data object Templates : Route
    // v2.6: one customer's khata ledger, pushed from the Money tab.
    data class KhataDetail(val number: String, val name: String) : Route
}

/** App root: first-run gate, then the three-tab shell (Sell /
 *  Customers / Money) with QR, Share-to-Status, Settings, the
 *  business tools, Templates and Khata detail as pushed screens. */
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

    // Tab jumps from the Home Today card and the Money links:
    // back to the shell, on the requested tab.
    fun goToTab(index: Int) {
        route = Route.Main
        tab = index
    }

    when (val r = route) {
        is Route.Qr -> QrScreen(
            initialNumberE164 = r.number,
            initialMessage = r.message,
            onBack = { route = Route.Main }
        )
        is Route.ShareStatus -> ShareStatusScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Settings -> SettingsScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Invoices -> InvoicesScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Catalog -> CatalogScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Scheduled -> ScheduledScreen(vm = vm, onBack = { route = Route.Main })
        is Route.Templates -> TemplatesScreen(
            vm = vm,
            pendingEdit = pendingEdit,
            onPendingEditConsumed = { pendingEdit = null },
            onBack = { route = Route.Main }
        )
        is Route.KhataDetail -> KhataDetailScreen(
            vm = vm,
            number = r.number,
            name = r.name,
            onBack = { route = Route.Main }
        )
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
                            label = { Text("Sell") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(painter = painterResource(R.drawable.ic_history), contentDescription = null) },
                            label = { Text("Customers") }
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(painter = painterResource(R.drawable.ic_description), contentDescription = null) },
                            label = { Text("Money") }
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
                                route = Route.Templates
                            },
                            onOpenInvoices = { route = Route.Invoices },
                            onOpenCatalog = { route = Route.Catalog },
                            onOpenScheduled = { route = Route.Scheduled },
                            onManageTemplates = { route = Route.Templates },
                            onGoToCustomers = { goToTab(1) },
                            onGoToMoney = { goToTab(2) }
                        )
                        1 -> RecentsScreen(
                            vm = vm,
                            onOpenSettings = { route = Route.Settings }
                        )
                        else -> MoneyScreen(
                            vm = vm,
                            onOpenInvoices = { route = Route.Invoices },
                            onOpenCatalog = { route = Route.Catalog },
                            onOpenScheduled = { route = Route.Scheduled },
                            onOpenKhataDetail = { number, name ->
                                route = Route.KhataDetail(number, name)
                            }
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
