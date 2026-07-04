package com.kalos.app.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Additional user-selectable colour themes (coolors.co palettes). Each is a full Material 3
// scheme so every screen adapts. The 5 source colours are mapped to M3 roles; primary/
// secondary/tertiary are nudged for legible contrast, backgrounds/surfaces derived to keep
// the palette's mood. Macro colours (protein/carbs/fat) stay fixed — they're semantic.

// ── Pastel Dreamland (light) ───────────────────────────────────────────────────
internal val PastelScheme = lightColorScheme(
    primary = Color(0xFFB07FC9),              // Pink Orchid, deepened for contrast on white
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFC8DD),     // Pastel Petal
    onPrimaryContainer = Color(0xFF48213A),
    secondary = Color(0xFF5B9BD8),            // Sky Blue, deepened
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBDE0FE),   // Icy Blue
    onSecondaryContainer = Color(0xFF0F3151),
    tertiary = Color(0xFFE070A0),             // Blush Pop, deepened
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFDF7FB),
    onBackground = Color(0xFF3D2F45),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF3D2F45),
    surfaceVariant = Color(0xFFF1E7F4),
    onSurfaceVariant = Color(0xFF7C6B84),
    outline = Color(0xFFD6C6DD),
    outlineVariant = Color(0xFFE7DBEC),
    error = Color(0xFFC0405B),
    onError = Color(0xFFFFFFFF),
    surfaceTint = Color(0xFFB07FC9),
    surfaceDim = Color(0xFFE9DEEC),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF8F0F9),
    surfaceContainerHigh = Color(0xFFF1E7F4),
    surfaceContainerHighest = Color(0xFFEADEEE),
)

// ── Bold Berry (dark) ───────────────────────────────────────────────────────────
internal val BerryScheme = darkColorScheme(
    primary = Color(0xFFFFA5AB),              // Cotton Candy
    onPrimary = Color(0xFF450920),
    primaryContainer = Color(0xFFA53860),     // Cherry Rose
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFDA627D),            // Blush Rose
    onSecondary = Color(0xFF2A0611),
    secondaryContainer = Color(0xFF6E2540),
    onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFFF9DBBD),             // Soft Apricot
    onTertiary = Color(0xFF442913),
    background = Color(0xFF2A0611),
    onBackground = Color(0xFFFFE8EE),
    surface = Color(0xFF330A17),
    onSurface = Color(0xFFFFE8EE),
    surfaceVariant = Color(0xFF4A1528),
    onSurfaceVariant = Color(0xFFDCA9B8),
    outline = Color(0xFF7A3050),
    outlineVariant = Color(0xFF4A1528),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1418),
    surfaceTint = Color(0xFFFFA5AB),
    surfaceDim = Color(0xFF2A0611),
    surfaceBright = Color(0xFF52182E),
    surfaceContainerLowest = Color(0xFF20040C),
    surfaceContainerLow = Color(0xFF330A17),
    surfaceContainer = Color(0xFF3C0F1E),
    surfaceContainerHigh = Color(0xFF481526),
    surfaceContainerHighest = Color(0xFF551D30),
)

// ── Dark Aurora (dark) ────────────────────────────────────────────────────────
internal val AuroraScheme = darkColorScheme(
    primary = Color(0xFFC8B8DB),              // Thistle
    onPrimary = Color(0xFF2A1F33),
    primaryContainer = Color(0xFF502F4C),     // Blackberry Cream
    onPrimaryContainer = Color(0xFFEDE3F3),
    secondary = Color(0xFF9E86AC),            // Dusty Lavender, lifted for contrast
    onSecondary = Color(0xFF241830),
    secondaryContainer = Color(0xFF3E2C46),
    onSecondaryContainer = Color(0xFFE9DFF2),
    tertiary = Color(0xFFC8B8DB),
    onTertiary = Color(0xFF2A1F33),
    background = Color(0xFF0D0912),
    onBackground = Color(0xFFF9F4F5),         // Snow
    surface = Color(0xFF15101C),
    onSurface = Color(0xFFF9F4F5),
    surfaceVariant = Color(0xFF302640),
    onSurfaceVariant = Color(0xFFAE9EBB),
    outline = Color(0xFF4A3A57),
    outlineVariant = Color(0xFF302640),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1418),
    surfaceTint = Color(0xFFC8B8DB),
    surfaceDim = Color(0xFF0D0912),
    surfaceBright = Color(0xFF352A44),
    surfaceContainerLowest = Color(0xFF08050D),
    surfaceContainerLow = Color(0xFF15101C),
    surfaceContainer = Color(0xFF1C1626),
    surfaceContainerHigh = Color(0xFF261E32),
    surfaceContainerHighest = Color(0xFF31283F),
)

// ── Monochrome Harmony (dark, gold accent) ──────────────────────────────────────
internal val MonochromeScheme = darkColorScheme(
    primary = Color(0xFFF5CB5C),              // Tuscan Sun
    onPrimary = Color(0xFF2A2600),
    primaryContainer = Color(0xFF4A4420),
    onPrimaryContainer = Color(0xFFFBE7A8),
    secondary = Color(0xFFCFDBD5),            // Dust Grey
    onSecondary = Color(0xFF242423),
    secondaryContainer = Color(0xFF3A3D3A),
    onSecondaryContainer = Color(0xFFE8EDDF),
    tertiary = Color(0xFFF5CB5C),
    onTertiary = Color(0xFF2A2600),
    background = Color(0xFF1B1C1B),
    onBackground = Color(0xFFE8EDDF),         // Ivory
    surface = Color(0xFF242423),              // Shadow Grey
    onSurface = Color(0xFFE8EDDF),
    surfaceVariant = Color(0xFF333533),       // Graphite
    onSurfaceVariant = Color(0xFFB0B5AE),
    outline = Color(0xFF4C4E4B),
    outlineVariant = Color(0xFF333533),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1418),
    surfaceTint = Color(0xFFF5CB5C),
    surfaceDim = Color(0xFF1B1C1B),
    surfaceBright = Color(0xFF3A3C39),
    surfaceContainerLowest = Color(0xFF141514),
    surfaceContainerLow = Color(0xFF242423),
    surfaceContainer = Color(0xFF2A2C2A),
    surfaceContainerHigh = Color(0xFF333533),
    surfaceContainerHighest = Color(0xFF3D3F3C),
)

// ── Aqua Whimsy (dark, cyan / teal) ─────────────────────────────────────────────
internal val AquaScheme = darkColorScheme(
    primary = Color(0xFF14B3BA),              // Dark Cyan, brightened
    onPrimary = Color(0xFF00272A),
    primaryContainer = Color(0xFF0C7489),     // Cerulean
    onPrimaryContainer = Color(0xFFADE8EC),
    secondary = Color(0xFF5FC2C9),
    onSecondary = Color(0xFF00272A),
    secondaryContainer = Color(0xFF13505B),   // Dark Teal
    onSecondaryContainer = Color(0xFFCDEEF1),
    tertiary = Color(0xFFD7D9CE),             // Dust Grey (neutral warm accent)
    onTertiary = Color(0xFF2B2D26),
    background = Color(0xFF071417),
    onBackground = Color(0xFFE4EEED),
    surface = Color(0xFF0E2529),
    onSurface = Color(0xFFE4EEED),
    surfaceVariant = Color(0xFF163C43),
    onSurfaceVariant = Color(0xFFA6C2C5),
    outline = Color(0xFF2F6068),
    outlineVariant = Color(0xFF163C43),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1418),
    surfaceTint = Color(0xFF14B3BA),
    surfaceDim = Color(0xFF071417),
    surfaceBright = Color(0xFF244449),
    surfaceContainerLowest = Color(0xFF040F11),
    surfaceContainerLow = Color(0xFF0E2529),
    surfaceContainer = Color(0xFF122C31),
    surfaceContainerHigh = Color(0xFF17363C),
    surfaceContainerHighest = Color(0xFF1D4147),
)

// ── Ocean Breeze (dark, navy + warm sand) ───────────────────────────────────────
internal val OceanScheme = darkColorScheme(
    primary = Color(0xFF88BCA2),              // Muted Teal, lifted
    onPrimary = Color(0xFF06251A),
    primaryContainer = Color(0xFF2F5D47),
    onPrimaryContainer = Color(0xFFCDEEDA),
    secondary = Color(0xFFDAB785),            // Tan
    onSecondary = Color(0xFF3A2A12),
    secondaryContainer = Color(0xFF5A4527),
    onSecondaryContainer = Color(0xFFF5E3C8),
    tertiary = Color(0xFFD5896F),             // Burnt Peach
    onTertiary = Color(0xFF3A1710),
    background = Color(0xFF04162E),
    onBackground = Color(0xFFE6EDF5),
    surface = Color(0xFF072240),
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = Color(0xFF0E3557),
    onSurfaceVariant = Color(0xFFA9C0D6),
    outline = Color(0xFF2F5378),
    outlineVariant = Color(0xFF0E3557),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1418),
    surfaceTint = Color(0xFF88BCA2),
    surfaceDim = Color(0xFF04162E),
    surfaceBright = Color(0xFF1E3E5E),
    surfaceContainerLowest = Color(0xFF020F22),
    surfaceContainerLow = Color(0xFF072240),
    surfaceContainer = Color(0xFF0A2A4A),
    surfaceContainerHigh = Color(0xFF0F3558),
    surfaceContainerHighest = Color(0xFF154066),
)

// ── Ocean Sunset (dark, dusk blue + coral) ──────────────────────────────────────
internal val SunsetScheme = darkColorScheme(
    primary = Color(0xFFE56B6F),              // Light Coral
    onPrimary = Color(0xFF3A0E12),
    primaryContainer = Color(0xFFB56576),     // Rosewood
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFEAAC8B),            // Light Bronze
    onSecondary = Color(0xFF3A2113),
    secondaryContainer = Color(0xFF5A4433),
    onSecondaryContainer = Color(0xFFF7DDC8),
    tertiary = Color(0xFFB39CC0),             // Dusty Lavender, lifted
    onTertiary = Color(0xFF2A1F33),
    background = Color(0xFF18243B),
    onBackground = Color(0xFFEDEFF5),
    surface = Color(0xFF20304C),
    onSurface = Color(0xFFEDEFF5),
    surfaceVariant = Color(0xFF33455F),
    onSurfaceVariant = Color(0xFFB2BCCF),
    outline = Color(0xFF4A5F80),
    outlineVariant = Color(0xFF33455F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1418),
    surfaceTint = Color(0xFFE56B6F),
    surfaceDim = Color(0xFF18243B),
    surfaceBright = Color(0xFF35496A),
    surfaceContainerLowest = Color(0xFF121B2C),
    surfaceContainerLow = Color(0xFF20304C),
    surfaceContainer = Color(0xFF263856),
    surfaceContainerHigh = Color(0xFF2E4262),
    surfaceContainerHighest = Color(0xFF374D6F),
)
