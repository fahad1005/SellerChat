package com.fahad1005.sellerchat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Brand accent: WhatsApp-adjacent deep green #128C7E on neutral surfaces.
 *  Material 3 dynamic color where the device offers it (Android 12+),
 *  brand schemes as the fallback. System font only — no font files. */
val BrandGreen = Color(0xFF128C7E)
private val BrandGreenDark = Color(0xFF075E54)
private val BrandGreenLight = Color(0xFF4ADE80)

private val LightScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F2EC),
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGreenDark,
    surface = Color(0xFFFBFBF9),
    onSurface = Color(0xFF1C1B1A)
)

private val DarkScheme = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = Color(0xFF06381F),
    primaryContainer = Color(0xFF0B4F45),
    onPrimaryContainer = Color(0xFFB9F0DD),
    secondary = BrandGreenLight,
    surface = Color(0xFF121413),
    onSurface = Color(0xFFE4E6E3)
)

@Composable
fun SellerChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
