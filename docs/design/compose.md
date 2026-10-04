# Compose-Umsetzung

So werden die Tokens in Jetpack Compose (Material 3) abgebildet. Alles liegt im Modul `:app` unter `ui/theme/`.

## Grundsätze

- **Material 3 als Gerüst, nicht als Look.** Die Tokens füllen das `ColorScheme`; was M3 nicht kennt (Chart-, Inverse-Raised-, Segment-Farben, Abstände, Größen), liegt in eigenen `CompositionLocal`s.
- **Dynamic Color aus.** `dynamicLightColorScheme()` wird nicht verwendet.
- **Schriften gebündelt** als TTF in `res/font/` (OFL). Keine Downloadable Fonts: die brauchen Google Play Services und brechen die F-Droid-Tauglichkeit.
- **Tabellarische Ziffern** global über `fontFeatureSettings = "tnum"` in jedem `TextStyle`.

## Farb-Mapping

| M3-Rolle | Token |
| --- | --- |
| `background` | `ground` |
| `surface`, `surfaceContainer` | `surface` |
| `surfaceContainerLow` | `surface-sunken` |
| `surfaceVariant`, `surfaceContainerHigh` | `surface-muted` |
| `onBackground`, `onSurface` | `ink` |
| `onSurfaceVariant` | `ink-muted` |
| `primary` / `onPrimary` | `accent` / `on-accent` |
| `primaryContainer` / `onPrimaryContainer` | `accent-container` / `on-accent-container` |
| `inverseSurface` / `inverseOnSurface` | `inverse-surface` / `on-inverse` |
| `outline` / `outlineVariant` | `line-control` / `line` |

Eigene Rollen (`ExtendedColors`): `inkPlaceholder`, `onInverseMuted`, `inverseRaised`, `inverseAccentContainer`, `onInverseAccentContainer`, `accentChart`, `chartGrid`, `chartBand`, `chartArea`, `segmentActive`, `track`.

```kotlin
val LightColors = lightColorScheme(
    background = Color(0xFFF3F3F1),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F1),
    surfaceContainerHigh = Color(0xFFEDEDE9),
    surfaceVariant = Color(0xFFEDEDE9),
    onBackground = Color(0xFF17181C),
    onSurface = Color(0xFF17181C),
    onSurfaceVariant = Color(0xFF5D626C),
    primary = Color(0xFFF0643F),
    onPrimary = Color(0xFF17181C),
    primaryContainer = Color(0xFFFBE3DB),
    onPrimaryContainer = Color(0xFF9E2F18),
    inverseSurface = Color(0xFF17181C),
    inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFD6D6D1),
    outlineVariant = Color(0xFFE4E4E0),
)

val DarkColors = darkColorScheme(
    background = Color(0xFF0F1013),
    surface = Color(0xFF1B1D22),
    surfaceContainer = Color(0xFF1B1D22),
    surfaceContainerLow = Color(0xFF24262C),
    surfaceContainerHigh = Color(0xFF24262C),
    surfaceVariant = Color(0xFF24262C),
    onBackground = Color(0xFFF2F2EF),
    onSurface = Color(0xFFF2F2EF),
    onSurfaceVariant = Color(0xFFA3A8B2),
    primary = Color(0xFFF0643F),
    onPrimary = Color(0xFF17181C),
    primaryContainer = Color(0xFF3A2620),
    onPrimaryContainer = Color(0xFFFFB39F),
    inverseSurface = Color(0xFFF2F2EF),
    inverseOnSurface = Color(0xFF17181C),
    outline = Color(0xFF3A3D45),
    outlineVariant = Color(0xFF2C2F36),
)
```

## Typografie

```kotlin
val Display = FontFamily(
    Font(R.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
)
val Body = FontFamily(
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private fun style(family: FontFamily, size: Int, line: Float, weight: FontWeight, tracking: Float = 0f) =
    TextStyle(
        fontFamily = family, fontSize = size.sp, lineHeight = (size * line).sp,
        fontWeight = weight, letterSpacing = tracking.em, fontFeatureSettings = "tnum",
    )

val AppTypography = Typography(
    displayLarge = style(Display, 44, 1.0f, FontWeight.ExtraBold, -0.02f),   // display-xl
    displayMedium = style(Display, 36, 1.05f, FontWeight.ExtraBold, -0.02f), // display-l
    displaySmall = style(Display, 32, 1.05f, FontWeight.ExtraBold, -0.02f),  // display-m
    headlineSmall = style(Display, 24, 1.15f, FontWeight.ExtraBold, -0.01f), // title-l
    titleLarge = style(Display, 20, 1.2f, FontWeight.ExtraBold, -0.01f),     // title-m
    titleMedium = style(Display, 18, 1.25f, FontWeight.ExtraBold),           // title-s
    titleSmall = style(Body, 16, 1.375f, FontWeight.ExtraBold),              // label-l
    bodyLarge = style(Body, 15, 1.53f, FontWeight.Medium),                   // body
    bodyMedium = style(Body, 15, 1.4f, FontWeight.Bold),                     // body-strong
    bodySmall = style(Body, 13, 1.38f, FontWeight.Medium),                   // caption
    labelLarge = style(Body, 14, 1.43f, FontWeight.ExtraBold),               // label-m
    labelMedium = style(Body, 12, 1.33f, FontWeight.ExtraBold, 0.08f),       // overline (uppercase im Text)
    labelSmall = style(Body, 11, 1.27f, FontWeight.ExtraBold, 0.06f),        // micro
)
```

`nav-label` (12/16, SemiBold, aktiv ExtraBold) setzt die Navigationsleiste selbst.

## Form, Abstände, Größen

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // radius-xs
    small = RoundedCornerShape(14.dp),       // radius-md
    medium = RoundedCornerShape(18.dp),      // radius-lg
    large = RoundedCornerShape(22.dp),       // radius-xl
    extraLarge = RoundedCornerShape(28.dp),  // radius-3xl
)

object Spacing { val s4 = 4.dp; val s6 = 6.dp; val s8 = 8.dp; val s10 = 10.dp; val s12 = 12.dp
    val s14 = 14.dp; val s16 = 16.dp; val s18 = 18.dp; val s20 = 20.dp; val s24 = 24.dp; val s28 = 28.dp }

object Sizes { val touch = 44.dp; val input = 46.dp; val cta = 54.dp; val nav = 80.dp; val icon = 22.dp }
```

`radius-sm` (10) und `radius-2xl` (26) sind keine M3-Stufen und werden direkt als `RoundedCornerShape` genutzt.

## Theme-Wahl

Einstellung „Design“: System (Standard), Hell, Dunkel. Light ist Prio A, Dark Prio B; beide Farbschemata existieren ab dem ersten Build, damit keine Komponente Farben hart codiert.
