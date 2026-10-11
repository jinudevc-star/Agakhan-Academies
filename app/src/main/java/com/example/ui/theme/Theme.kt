package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AkahExtendedColors(
    val sunken: Color,
    val line: Color,
    val muted: Color,
    val ok: Color,
    val okSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val crit: Color,
    val critSoft: Color,
    val off: Color,
    val offSoft: Color,
    val maroon: Color
)

val LocalAkahColors = staticCompositionLocalOf {
    AkahExtendedColors(
        sunken = AkahSunkenLight,
        line = AkahLineLight,
        muted = AkahMutedLight,
        ok = AkahOkLight,
        okSoft = AkahOkSoftLight,
        warn = AkahWarnLight,
        warnSoft = AkahWarnSoftLight,
        crit = AkahCritLight,
        critSoft = AkahCritSoftLight,
        off = AkahOffLight,
        offSoft = AkahOffSoftLight,
        maroon = AkahMaroonLight
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = AkahAccentDark,
    onPrimary = Color(0xFF062014),
    primaryContainer = AkahAccentSoftDark,
    onPrimaryContainer = AkahInkDark,
    secondary = AkahMaroonDark,
    onSecondary = Color(0xFF2B0714),
    secondaryContainer = AkahSunkenDark,
    onSecondaryContainer = AkahInkDark,
    tertiary = AkahWarnDark,
    background = AkahBgDark,
    onBackground = AkahInkDark,
    surface = AkahSurfaceDark,
    onSurface = AkahInkDark,
    surfaceVariant = AkahSunkenDark,
    onSurfaceVariant = AkahMutedDark,
    outline = AkahLineDark,
    error = AkahCritDark,
    errorContainer = AkahCritSoftDark,
    onErrorContainer = AkahInkDark
)

private val LightColorScheme = lightColorScheme(
    primary = AkahAccentLight,
    onPrimary = Color.White,
    primaryContainer = AkahAccentSoftLight,
    onPrimaryContainer = AkahInkLight,
    secondary = AkahMaroonLight,
    onSecondary = Color.White,
    secondaryContainer = AkahSunkenLight,
    onSecondaryContainer = AkahInkLight,
    tertiary = AkahWarnLight,
    background = AkahBgLight,
    onBackground = AkahInkLight,
    surface = AkahSurfaceLight,
    onSurface = AkahInkLight,
    surfaceVariant = AkahSunkenLight,
    onSurfaceVariant = AkahMutedLight,
    outline = AkahLineLight,
    error = AkahCritLight,
    errorContainer = AkahCritSoftLight,
    onErrorContainer = AkahInkLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        AkahExtendedColors(
            sunken = AkahSunkenDark,
            line = AkahLineDark,
            muted = AkahMutedDark,
            ok = AkahOkDark,
            okSoft = AkahOkSoftDark,
            warn = AkahWarnDark,
            warnSoft = AkahWarnSoftDark,
            crit = AkahCritDark,
            critSoft = AkahCritSoftDark,
            off = AkahOffDark,
            offSoft = AkahOffSoftDark,
            maroon = AkahMaroonDark
        )
    } else {
        AkahExtendedColors(
            sunken = AkahSunkenLight,
            line = AkahLineLight,
            muted = AkahMutedLight,
            ok = AkahOkLight,
            okSoft = AkahOkSoftLight,
            warn = AkahWarnLight,
            warnSoft = AkahWarnSoftLight,
            crit = AkahCritLight,
            critSoft = AkahCritSoftLight,
            off = AkahOffLight,
            offSoft = AkahOffSoftLight,
            maroon = AkahMaroonLight
        )
    }

    CompositionLocalProvider(LocalAkahColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
