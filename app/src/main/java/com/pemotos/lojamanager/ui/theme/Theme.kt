package com.pemotos.lojamanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LojaLightColors = lightColorScheme(
    primary = RosePrimary,
    onPrimary = RoseOnPrimary,
    primaryContainer = RosePrimaryContainer,
    onPrimaryContainer = RoseOnPrimaryContainer,
    secondary = CoralSecondary,
    onSecondary = CoralOnSecondary,
    secondaryContainer = CoralSecondaryContainer,
    onSecondaryContainer = CoralOnSecondaryContainer,
    tertiary = PeachTertiary,
    onTertiary = PeachOnTertiary,
    tertiaryContainer = PeachTertiaryContainer,
    onTertiaryContainer = PeachOnTertiaryContainer,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val LojaDarkColors = darkColorScheme(
    primary = RosePrimaryDark,
    onPrimary = RoseOnPrimaryDark,
    primaryContainer = RosePrimaryContainerDark,
    onPrimaryContainer = RoseOnPrimaryContainerDark,
    secondary = CoralSecondaryDark,
    onSecondary = CoralOnSecondaryDark,
    secondaryContainer = CoralSecondaryContainerDark,
    onSecondaryContainer = CoralOnSecondaryContainerDark,
    tertiary = PeachTertiaryDark,
    onTertiary = PeachOnTertiaryDark,
    tertiaryContainer = PeachTertiaryContainerDark,
    onTertiaryContainer = PeachOnTertiaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

@Composable
fun LojaManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LojaDarkColors
        else -> LojaLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LojaTypography,
        content = content
    )
}
