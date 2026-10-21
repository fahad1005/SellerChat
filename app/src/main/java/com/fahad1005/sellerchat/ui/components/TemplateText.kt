package com.fahad1005.sellerchat.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.fahad1005.sellerchat.data.TemplateLanguage

/** Renders a template body. Urdu-script templates get a real RTL layout
 *  direction so alignment and punctuation land correctly; everything
 *  else renders LTR. */
@Composable
fun TemplateBodyText(body: String, language: TemplateLanguage, maxLines: Int = Int.MAX_VALUE) {
    if (language == TemplateLanguage.UR) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                maxLines = maxLines
            )
        }
    } else {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines
        )
    }
}
