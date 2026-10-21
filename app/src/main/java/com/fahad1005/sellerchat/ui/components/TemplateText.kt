package com.fahad1005.sellerchat.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.fahad1005.sellerchat.R
import com.fahad1005.sellerchat.data.TemplateLanguage

/** The bundled subsetted Noto Nastaliq Urdu (res/font, 899,288 bytes
 *  raw after subsetting — see BUILD_NOTES). Used ONLY for Urdu-script template
 *  text: bodies, previews, chips and the editor. UI chrome stays in
 *  the system font, and Hindi/Marathi/Tamil/Telugu templates use the
 *  system fonts, which carry those scripts natively. */
val NastaliqUrduFontFamily = FontFamily(
    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal)
)

/** True when [language] is Urdu script and its text should render in
 *  Nastaliq with a strict RTL direction. */
fun usesNastaliq(language: TemplateLanguage): Boolean = language == TemplateLanguage.UR

/** Renders a template body. Urdu-script templates get the Nastaliq
 *  font and a real RTL layout direction so alignment and punctuation
 *  land correctly (digits inside stay LTR — Unicode bidi handles
 *  them; nothing is force-mirrored). Nastaliq needs tall line height
 *  or the script's stacks clip, hence the 30sp override. */
@Composable
fun TemplateBodyText(body: String, language: TemplateLanguage, maxLines: Int = Int.MAX_VALUE) {
    if (usesNastaliq(language)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = NastaliqUrduFontFamily,
                    lineHeight = 30.sp
                ),
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
