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
