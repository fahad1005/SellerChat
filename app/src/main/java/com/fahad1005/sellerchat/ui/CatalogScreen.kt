package com.fahad1005.sellerchat.ui

import android.net.Uri
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.data.Product
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.util.PhoneUtils
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import com.fahad1005.sellerchat.viewmodel.WhatsAppPackages

/** Product catalog (v2.5): the seller's own price list. Full CRUD;
 *  inactive products stay stored but leave the Home picker. "Send"
 *  opens WhatsApp with the product message for a typed customer
 *  number; "Send photo" picks a file first and opens WhatsApp with
 *  it attached — honest copy throughout: WhatsApp shows the file
 *  attached and the seller taps send there. */
@Composable
fun CatalogScreen(vm: MainViewModel, onBack: () -> Unit = {}) {
    val products by vm.products.collectAsState()
    val country by vm.selectedCountry.collectAsState()
    val selectedApp by vm.selectedAppPackage.collectAsState()
    val homeNumber by vm.homeNumber.collectAsState()

    var editing by remember { mutableStateOf<Product?>(null) }
    var adding by remember { mutableStateOf(false) }
    var sendTarget by remember { mutableStateOf<Product?>(null) }
    // "text" = plain product message, "photo" = picked file + caption.
    // Tracked separately so the text dialog never flashes during the
    // photo flow while the system picker is on screen.
    var sendMode by remember { mutableStateOf("text") }
    var sendNumber by remember { mutableStateOf("") }
    var sendNumberError by remember { mutableStateOf<String?>(null) }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaNote by remember { mutableStateOf<String?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingMediaUri = uri
            sendNumber = homeNumber
            // sendTarget is already the row that asked for a photo.
        } else {
            // Picker cancelled: drop the pending send entirely.
            sendTarget = null
        }
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
            text = "Pick a product on Home and it fills {item} and {price} in your template — preview and send always match.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (products.isEmpty()) {
            Text(
                text = "No products yet. Add your first item below.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products, key = { it.id }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = CurrencyUtils.formatWithSymbol(
                                            product.price, vm.settings.currencyCode
                                        ),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (product.description.isNotBlank()) {
                                        Text(
                                            text = product.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = product.active,
                                    onCheckedChange = {
                                        vm.updateProduct(product.copy(active = it))
                                    }
                                )
                            }
                            Row {
                                TextButton(onClick = {
                                    sendTarget = product
                                    sendMode = "text"
                                    pendingMediaUri = null
                                    sendNumber = homeNumber
                                    sendNumberError = null
                                }) { Text("Send") }
                                TextButton(onClick = {
                                    sendTarget = product
                                    sendMode = "photo"
                                    sendNumber = homeNumber
                                    sendNumberError = null
                                    mediaPicker.launch("*/*")
                                }) { Text("Send photo") }
                                TextButton(onClick = { editing = product }) { Text("Edit") }
                                TextButton(onClick = { vm.deleteProduct(product.id) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { adding = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Add product")
        }
    }

    if (adding || editing != null) {
        ProductEditDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { name, price, description ->
                val target = editing
                if (target == null) {
                    vm.addProduct(name, price, description)
                } else {
                    vm.updateProduct(
                        target.copy(name = name, price = price, description = description)
                    )
                }
                adding = false
                editing = null
            }
        )
    }

    // Number dialog shared by "Send" (text) and "Send photo" (media).
    val target = sendTarget
    if (target != null && sendMode == "text") {
        // Text send only: the photo flow never lands here (see below).
        SendNumberDialog(
            title = "Send ${target.name} to",
            initialNumber = sendNumber,
            error = sendNumberError,
            onDismiss = { sendTarget = null },
            onConfirm = { raw ->
                val e164 = PhoneUtils.normalize(raw, country)
                if (e164 == null) {
                    sendNumberError = "Invalid number for selected country"
                } else {
                    val pkg = selectedApp ?: WhatsAppPackages.WHATSAPP
                    vm.openChat(pkg, e164, vm.productMessage(target))
                    vm.recordRecent(e164, "", pkg, "Product: ${target.name}")
                    sendTarget = null
                }
            }
        )
    }
    if (target != null && sendMode == "photo" && pendingMediaUri != null) {
        val uri = pendingMediaUri
        SendNumberDialog(
            title = "Send photo for ${target.name} to",
            initialNumber = sendNumber,
            error = sendNumberError,
            onDismiss = { sendTarget = null; pendingMediaUri = null },
            onConfirm = { raw ->
                val e164 = PhoneUtils.normalize(raw, country)
                if (e164 == null) {
                    sendNumberError = "Invalid number for selected country"
                } else {
                    val pkg = selectedApp ?: WhatsAppPackages.WHATSAPP
                    val caption = vm.productMessage(target)
                    val ok = if (uri != null) {
                        vm.sendMedia(pkg, e164, uri, caption)
                    } else {
                        false
                    }
                    if (!ok) {
                        vm.openChat(pkg, e164, caption)
                        mediaNote = "WhatsApp opened without the file — " +
                            "attach it in WhatsApp before sending."
                    } else {
                        vm.recordRecent(e164, "", pkg, "Product: ${target.name}")
                    }
                    sendTarget = null
                    pendingMediaUri = null
                }
            }
        )
    }

    val note = mediaNote
    if (note != null) {
        AlertDialog(
            onDismissRequest = { mediaNote = null },
            title = { Text("File not attached") },
            text = { Text(note) },
            confirmButton = {
                TextButton(onClick = { mediaNote = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ProductEditDialog(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    val priceValue = price.filter { it.isDigit() }.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Price (whole units)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && priceValue != null,
                onClick = { onSave(name, priceValue ?: 0L, description) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SendNumberDialog(
    title: String,
    initialNumber: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Customer number") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = number.isNotBlank(),
                onClick = { onConfirm(number) }
            ) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
