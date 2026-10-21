package com.fahad1005.sellerchat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fahad1005.sellerchat.data.Countries
import com.fahad1005.sellerchat.data.Country

/** Searchable dial-code picker: flag + name + code. Pure UI, no data
 *  leaves the device. */
@Composable
fun CountryPickerDialog(
    onPick: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) Countries.all
        else Countries.all.filter {
            it.name.lowercase().contains(q) || it.dial.contains(q) || it.iso.lowercase() == q
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose country") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search country or code") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(filtered, key = { it.iso }) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(country) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(text = country.flag, modifier = Modifier.padding(end = 12.dp))
                            Text(text = country.name, modifier = Modifier.weight(1f))
                            Text(text = "+${country.dial}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
