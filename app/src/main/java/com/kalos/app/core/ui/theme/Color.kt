package com.kalos.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ────────────────────────────────────────────────────────────────────
val PrimaryGreen              = Color(0xFF34D399)   // Emerald 400 — vivid on dark
val PrimaryGreenDark          = Color(0xFF065F46)   // Emerald 900 — container
val AccentOrange              = Color(0xFFFB923C)   // Orange 400 — refined accent
val AccentOrangeDark          = Color(0xFF2A1200)   // Dark orange — container

// ── Macro colors ──────────────────────────────────────────────────────────────
val ColorProtein              = Color(0xFF818CF8)   // Indigo 400
val ColorCarbs                = Color(0xFFFBBF24)   // Amber 400
val ColorFat                  = Color(0xFFF87171)   // Rose 400
val ColorCalories             = PrimaryGreen

// Over-target signal (macros / calories beyond goal): amber warning, never red —
// a cue for steering, not a punishment.
val ColorOverTarget           = AccentOrange

// ── Dark theme (app always forces dark) ───────────────────────────────────────
val md_theme_dark_primary              = PrimaryGreen
val md_theme_dark_onPrimary            = Color(0xFF022C22)
val md_theme_dark_primaryContainer     = PrimaryGreenDark
val md_theme_dark_onPrimaryContainer   = Color(0xFFA7F3D0)
val md_theme_dark_secondary            = PrimaryGreen
val md_theme_dark_onSecondary          = Color(0xFF022C22)
val md_theme_dark_secondaryContainer   = PrimaryGreenDark
val md_theme_dark_onSecondaryContainer = Color(0xFFA7F3D0)
val md_theme_dark_tertiary             = Color(0xFF60A5FA)
val md_theme_dark_onTertiary           = Color(0xFF001736)
val md_theme_dark_background           = Color(0xFF09090F)
val md_theme_dark_onBackground         = Color(0xFFEAEAF5)
val md_theme_dark_surface              = Color(0xFF111120)
val md_theme_dark_onSurface            = Color(0xFFEAEAF5)
val md_theme_dark_surfaceVariant       = Color(0xFF1A1A30)
val md_theme_dark_onSurfaceVariant     = Color(0xFF8A8AAC)
val md_theme_dark_outline              = Color(0xFF2E2E50)
val md_theme_dark_error                = Color(0xFFFF6B6B)
val md_theme_dark_onError              = Color(0xFF2D0000)

// ── Surface container family (M3 elevation tokens — must stay in the same blue-dark family)
// ElevatedCard uses surfaceContainerLow; Card uses surfaceContainerHighest by default.
// Defining these explicitly prevents Compose from falling back to its neutral-gray baseline.
val md_theme_dark_surfaceDim              = Color(0xFF0E0E1C)
val md_theme_dark_surfaceBright           = Color(0xFF1E1E38)
val md_theme_dark_surfaceTint             = PrimaryGreen
val md_theme_dark_surfaceContainerLowest  = Color(0xFF0C0C18)
val md_theme_dark_surfaceContainerLow     = Color(0xFF111120)  // = surface → ElevatedCard matches Card
val md_theme_dark_surfaceContainer        = Color(0xFF151530)
val md_theme_dark_surfaceContainerHigh    = Color(0xFF1A1A38)
val md_theme_dark_surfaceContainerHighest = Color(0xFF1F1F40)
