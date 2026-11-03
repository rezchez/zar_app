package com.zarfolio.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.zarfolio.core.designsystem.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val Doran = FontFamily(Font(R.font.doran_regular, FontWeight.Normal), Font(R.font.doran_bold, FontWeight.Bold))
private val BodySansSerif = FontFamily.SansSerif
private val ZarTypography = Typography(
    displayLarge = TextStyle(fontFamily = Doran, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Doran, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = Doran, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Doran, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = BodySansSerif, fontSize = 15.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = BodySansSerif, fontSize = 13.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = BodySansSerif, fontSize = 12.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = BodySansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 22.sp),
)
private val LightColors = lightColorScheme(primary = ZarOrange, onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = ZarOrange.copy(alpha = .12f), onPrimaryContainer = ZarOrange, secondary = ZarNavy,
    background = androidx.compose.ui.graphics.Color(0xFFF8F9FC), onBackground = ZarInk,
    surface = androidx.compose.ui.graphics.Color.White, onSurface = ZarInk, surfaceVariant = ZarPanelLight,
    onSurfaceVariant = ZarMuted, outline = ZarBorder, error = ZarDanger)
private val ColorPlaceholder = androidx.compose.ui.graphics.Color(0xFF1B2442)
private val DarkColors = darkColorScheme(primary = ZarOrangeDark, onPrimary = ZarNavy,
    primaryContainer = ZarOrange.copy(alpha = .2f), onPrimaryContainer = ZarOrangeDark, secondary = androidx.compose.ui.graphics.Color(0xFFBEC9E6),
    background = ZarNavy, onBackground = androidx.compose.ui.graphics.Color(0xFFF5F6FB),
    surface = ZarPanel, onSurface = androidx.compose.ui.graphics.Color(0xFFF5F6FB), surfaceVariant = ColorPlaceholder,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB5BED2), outline = androidx.compose.ui.graphics.Color(0xFF39415C), error = ZarDanger)

@Composable
fun ZarfolioTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
        else androidx.compose.material3.dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
    } else if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = ZarTypography, content = content)
    }
}
