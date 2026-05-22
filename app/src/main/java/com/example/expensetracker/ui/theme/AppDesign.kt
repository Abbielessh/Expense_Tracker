package com.example.expensetracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Color scheme data class — all UI color tokens in one place
// ─────────────────────────────────────────────────────────────────────────────
data class AppColorScheme(
    // Backgrounds
    val Background: Color,
    val CardBackground: Color,
    val SurfaceSoft: Color,

    // Text
    val PrimaryText: Color,
    val SecondaryText: Color,
    val TextMuted: Color,

    // Borders
    val Border: Color,
    val Divider: Color,

    // Brand primary
    val Primary: Color,
    val PrimaryDark: Color,
    val PrimarySoft: Color,

    // Accent
    val Accent: Color,
    val AccentSoft: Color,

    // Semantic — income
    val SuccessIncome: Color,
    val IncomeSoft: Color,

    // Semantic — expense / error
    val ExpenseError: Color,
    val ExpenseSoft: Color,

    // Semantic — warning
    val Warning: Color,
    val WarningSoft: Color,

    // Bottom navigation
    val SelectedNavBackground: Color,
    val SelectedNavIconText: Color,
    val UnselectedNavIconText: Color,

    // Chart palette (dark-mode-safe muted colours)
    val ChartColors: List<Color>
) {
    // Alias kept so legacy call-sites that reference PrimaryAccent still compile
    val PrimaryAccent: Color get() = Primary
}

// ─────────────────────────────────────────────────────────────────────────────
// Light palette
// ─────────────────────────────────────────────────────────────────────────────
internal val LightColors = AppColorScheme(
    Background              = Color(0xFFF7F8FA),
    CardBackground          = Color(0xFFFFFFFF),
    SurfaceSoft             = Color(0xFFF3F5F7),
    PrimaryText             = Color(0xFF111827),
    SecondaryText           = Color(0xFF6B7280),
    TextMuted               = Color(0xFF9CA3AF),
    Border                  = Color(0xFFE5E7EB),
    Divider                 = Color(0xFFECEFF3),
    Primary                 = Color(0xFF1F4E5F),
    PrimaryDark             = Color(0xFF163B48),
    PrimarySoft             = Color(0xFFE8F1F3),
    Accent                  = Color(0xFF2F6F73),
    AccentSoft              = Color(0xFFE9F5F4),
    SuccessIncome           = Color(0xFF047857),
    IncomeSoft              = Color(0xFFECFDF5),
    ExpenseError            = Color(0xFFB91C1C),
    ExpenseSoft             = Color(0xFFFEF2F2),
    Warning                 = Color(0xFFB45309),
    WarningSoft             = Color(0xFFFFF7ED),
    SelectedNavBackground   = Color(0xFFE8F1F3),
    SelectedNavIconText     = Color(0xFF1F4E5F),
    UnselectedNavIconText   = Color(0xFF6B7280),
    ChartColors = listOf(
        Color(0xFF1F4E5F),   // primary teal
        Color(0xFF047857),   // income green
        Color(0xFFB91C1C),   // expense red
        Color(0xFFB45309),   // amber/warning
        Color(0xFF6B7280),   // neutral gray
        Color(0xFF2F6F73),   // accent teal
        Color(0xFF92400E),   // warm brown
        Color(0xFF4B5563)    // dark gray
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Dark palette
// ─────────────────────────────────────────────────────────────────────────────
internal val DarkColors = AppColorScheme(
    Background              = Color(0xFF0F172A),
    CardBackground          = Color(0xFF111827),
    SurfaceSoft             = Color(0xFF1F2937),
    PrimaryText             = Color(0xFFF9FAFB),
    SecondaryText           = Color(0xFFCBD5E1),
    TextMuted               = Color(0xFF94A3B8),
    Border                  = Color(0xFF334155),
    Divider                 = Color(0xFF1E293B),
    Primary                 = Color(0xFF5FA8B2),
    PrimaryDark             = Color(0xFF3D7A82),
    PrimarySoft             = Color(0xFF12343B),
    Accent                  = Color(0xFF7DD3FC),
    AccentSoft              = Color(0xFF0C2A3A),
    SuccessIncome           = Color(0xFF34D399),
    IncomeSoft              = Color(0xFF052E2B),
    ExpenseError            = Color(0xFFF87171),
    ExpenseSoft             = Color(0xFF3B1111),
    Warning                 = Color(0xFFFBBF24),
    WarningSoft             = Color(0xFF3A2605),
    SelectedNavBackground   = Color(0xFF134E4A),  // teal pill
    SelectedNavIconText     = Color(0xFF67E8F9),  // bright cyan
    UnselectedNavIconText   = Color(0xFFCBD5E1),  // clearly visible slate
    ChartColors = listOf(
        Color(0xFF5FA8B2),   // primary teal (light on dark bg)
        Color(0xFF34D399),   // income green
        Color(0xFFF87171),   // expense red
        Color(0xFFFBBF24),   // amber/warning
        Color(0xFF94A3B8),   // neutral blue-gray
        Color(0xFF7DD3FC),   // sky accent
        Color(0xFFD4A574),   // warm tan
        Color(0xFF6B7280)    // gray
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal — provides the active scheme to the entire composition tree
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppColors = staticCompositionLocalOf<AppColorScheme> { LightColors }

/**
 * Convenience accessor — reads the currently-active color scheme.
 * Must be called from inside a composable function.
 */
val AppColors: AppColorScheme
    @Composable get() = LocalAppColors.current

// ─────────────────────────────────────────────────────────────────────────────
// Dimension / spacing constants
// ─────────────────────────────────────────────────────────────────────────────
object AppStyles {
    val CardCornerRadius    = 16.dp
    val CardElevation       = 1.dp
    val PaddingStandard     = 16.dp
    val ButtonCornerRadius  = 14.dp
}
