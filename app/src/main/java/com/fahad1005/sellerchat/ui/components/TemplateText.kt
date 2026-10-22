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
 *  Nastaliq with a strict RTL direction. Nastaliq is for Urdu ONLY —
 *  no other language ever touches the bundled font. */
fun usesNastaliq(language: TemplateLanguage): Boolean = language == TemplateLanguage.UR

/** True for the RTL-script template languages. Urdu renders in
 *  Nastaliq; Arabic (v2.3) renders RTL in the system font, which
 *  carries Arabic natively — no Arabic font file is bundled. */
fun usesRtlLayout(language: TemplateLanguage): Boolean =
    language == TemplateLanguage.UR || language == TemplateLanguage.AR

/** Renders a template body. Urdu-script templates get the Nastaliq
 *  font and a real RTL layout direction so alignment and punctuation
 *  land correctly (digits inside stay LTR — Unicode bidi handles
 *  them; nothing is force-mirrored). Nastaliq needs tall line height
 *  or the script's stacks clip, hence the 30sp override. Arabic gets
 *  the same RTL direction with the plain system-font style. */
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
    } else if (usesRtlLayout(language)) {
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
