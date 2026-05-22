package com.example.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Material3 color schemes — wired to our custom palettes
// ─────────────────────────────────────────────────────────────────────────────
private val LightMaterialColors = lightColorScheme(
    primary             = LightColors.Primary,
    onPrimary           = Color.White,
    primaryContainer    = LightColors.PrimarySoft,
    onPrimaryContainer  = LightColors.PrimaryDark,
    secondary           = LightColors.Accent,
    onSecondary         = Color.White,
    secondaryContainer  = LightColors.AccentSoft,
    background          = LightColors.Background,
    surface             = LightColors.CardBackground,
    surfaceVariant      = LightColors.SurfaceSoft,
    onBackground        = LightColors.PrimaryText,
    onSurface           = LightColors.PrimaryText,
    onSurfaceVariant    = LightColors.SecondaryText,
    error               = LightColors.ExpenseError,
    onError             = Color.White,
    outline             = LightColors.Border,
    outlineVariant      = LightColors.Divider
)

private val DarkMaterialColors = darkColorScheme(
    primary             = DarkColors.Primary,
    onPrimary           = Color.White,
    primaryContainer    = DarkColors.PrimarySoft,
    onPrimaryContainer  = DarkColors.Primary,
    secondary           = DarkColors.Accent,
    onSecondary         = DarkColors.Background,
    secondaryContainer  = DarkColors.AccentSoft,
    background          = DarkColors.Background,
    surface             = DarkColors.CardBackground,
    surfaceVariant      = DarkColors.SurfaceSoft,
    onBackground        = DarkColors.PrimaryText,
    onSurface           = DarkColors.PrimaryText,
    onSurfaceVariant    = DarkColors.SecondaryText,
    error               = DarkColors.ExpenseError,
    onError             = DarkColors.Background,
    outline             = DarkColors.Border,
    outlineVariant      = DarkColors.Divider
)

// ─────────────────────────────────────────────────────────────────────────────
// Root theme composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors      = if (darkTheme) DarkColors      else LightColors
    val materialColors = if (darkTheme) DarkMaterialColors else LightMaterialColors

    // Provide our custom color scheme so all AppColors.X calls in composables get
    // the correct light or dark values without any per-screen code changes.
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography  = Typography,
            content     = content
        )
    }
}