package com.fahad1005.sellerchat.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.ui.components.TemplateBodyText
import com.fahad1005.sellerchat.viewmodel.MainViewModel
import java.util.Collections

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Existing(val template: Template) : EditorTarget
}

@Composable
fun TemplatesScreen(
    vm: MainViewModel,
    pendingEdit: Template?,
    onPendingEditConsumed: () -> Unit
) {
    val templates by vm.templates.collectAsState()
    var languageFilter by remember { mutableStateOf<TemplateLanguage?>(null) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    LaunchedEffect(pendingEdit) {
        if (pendingEdit != null) {
            editorTarget = EditorTarget.Existing(pendingEdit)
            onPendingEditConsumed()
        }
    }

    val target = editorTarget
    if (target != null) {
        TemplateEditor(
            existing = (target as? EditorTarget.Existing)?.template,
            onSave = { title, body, language ->
                when (target) {
                    is EditorTarget.New -> vm.addTemplate(title, body, language)
                    is EditorTarget.Existing ->
                        vm.updateTemplate(target.template.copy(title = title, body = body, language = language))
                }
                editorTarget = null
            },
            onDelete = (target as? EditorTarget.Existing)?.let { existing ->
                {
                    vm.deleteTemplate(existing.template.id)
                    editorTarget = null
                }
            },
            onBack = { editorTarget = null }
        )
        return
    }

    // Local mirror of the Room order, used only while drag-reordering.
    val localList = remember { mutableStateListOf<Template>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(templates, dragging) {
        if (!dragging) {
            localList.clear()
            localList.addAll(templates)
        }
    }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val spacingPx = with(LocalDensity.current) { 8.dp.toPx() }
    var menuForId by remember { mutableStateOf<Long?>(null) }

    val reorderEnabled = languageFilter == null
    val displayed = if (reorderEnabled) localList.toList()
    else templates.filter { it.language == languageFilter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Language filter tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterTab("All", languageFilter == null) { languageFilter = null }
            FilterTab("English", languageFilter == TemplateLanguage.EN) {
                languageFilter = TemplateLanguage.EN
            }
            FilterTab("اردو", languageFilter == TemplateLanguage.UR) {
                languageFilter = TemplateLanguage.UR
            }
            FilterTab("Roman Urdu", languageFilter == TemplateLanguage.ROMAN_UR) {
                languageFilter = TemplateLanguage.ROMAN_UR
            }
        }
        if (reorderEnabled) {
            Text(
                text = "Long-press a template and drag to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayed, key = { it.id }) { template ->
                val isDragging = draggingId == template.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .onSizeChanged { size ->
                            if (itemHeightPx == 0) itemHeightPx = size.height
                        }
                        .pointerInput(template.id, reorderEnabled) {
                            if (!reorderEnabled) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = template.id
                                    dragging = true
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    vm.reorderTemplates(localList.map { it.id })
                                    draggingId = null
                                    dragOffset = 0f
                                    dragging = false
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = 0f
                                    dragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    val stride = itemHeightPx + spacingPx
                                    if (stride > 0f) {
                                        var idx = localList.indexOfFirst { it.id == template.id }
                                        while (dragOffset > stride / 2 && idx in 0 until localList.lastIndex) {
                                            Collections.swap(localList, idx, idx + 1)
                                            idx++
                                            dragOffset -= stride
                                        }
                                        while (dragOffset < -stride / 2 && idx > 0) {
                                            Collections.swap(localList, idx, idx - 1)
                                            idx--
                                            dragOffset += stride
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.title, style = MaterialTheme.typography.titleMedium)
                            TemplateBodyText(
                                body = template.body,
                                language = template.language,
                                maxLines = 2
                            )
                        }
                        IconButton(onClick = { menuForId = template.id }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Template actions")
                        }
                    }
                    if (menuForId == template.id) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { menuForId = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuForId = null
                                    editorTarget = EditorTarget.Existing(template)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = {
                                    menuForId = null
                                    vm.duplicateTemplate(template)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuForId = null
                                    vm.deleteTemplate(template.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { editorTarget = EditorTarget.New },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  New template")
        }
    }
}

@Composable
private fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TemplateEditor(
    existing: Template?,
    onSave: (title: String, body: String, language: TemplateLanguage) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var bodyValue by remember(existing?.id) {
        mutableStateOf(TextFieldValue(existing?.body ?: ""))
    }
    var language by remember(existing?.id) {
        mutableStateOf(existing?.language ?: TemplateLanguage.EN)
    }

    fun insertPlaceholder(ph: String) {
        val sel = bodyValue.selection
        val newText = bodyValue.text.replaceRange(sel.start, sel.end, ph)
        bodyValue = TextFieldValue(newText, TextRange(sel.start + ph.length))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (existing == null) "New template" else "Edit template",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterTab("English", language == TemplateLanguage.EN) { language = TemplateLanguage.EN }
            FilterTab("اردو", language == TemplateLanguage.UR) { language = TemplateLanguage.UR }
            FilterTab("Roman Urdu", language == TemplateLanguage.ROMAN_UR) {
                language = TemplateLanguage.ROMAN_UR
            }
        }

        OutlinedTextField(
            value = bodyValue,
            onValueChange = { bodyValue = it },
            label = { Text("Message") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        // Placeholder helpers: tap to insert at the cursor.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (ph in listOf("{name}", "{price}", "{item}", "{qty}")) {
                OutlinedButton(onClick = { insertPlaceholder(ph) }) { Text(ph) }
            }
        }

        val len = bodyValue.text.length
        Text(
            text = if (len > 900) "Long template: $len characters (over 900 — WhatsApp may trim it)"
            else "$len characters",
            style = MaterialTheme.typography.bodySmall,
            color = if (len > 900) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = { onSave(title.trim(), bodyValue.text, language) },
            enabled = title.isNotBlank() && bodyValue.text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("Save template") }

        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete template", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
