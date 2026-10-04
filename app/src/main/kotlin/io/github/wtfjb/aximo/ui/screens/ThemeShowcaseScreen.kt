package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.ui.components.HintChip
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.AppTextStyles
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors

/**
 * Debug screen to check the theme on the phone: colors, text styles and the
 * basic components, in light and dark.
 *
 * Token names (e.g. "accent", "display-l") are shown as-is on purpose: they are
 * identifiers from docs/design, not UI text.
 */
@Composable
fun ThemeShowcaseScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s18),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(Sizes.touch)) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(Sizes.icon),
                )
            }
            Text(text = stringResource(R.string.showcase_title), style = MaterialTheme.typography.headlineSmall)
        }

        Section(stringResource(R.string.showcase_section_theme)) {
            val modes = ThemeMode.entries
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.theme_system),
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark),
                ),
                selectedIndex = modes.indexOf(themeMode),
                onSelect = { onThemeModeChange(modes[it]) },
            )
        }

        Section(stringResource(R.string.showcase_section_components)) {
            PrimaryButton(
                text = stringResource(R.string.showcase_button_primary),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                InverseButton(text = stringResource(R.string.showcase_button_inverse), onClick = {})
                SecondaryButton(text = stringResource(R.string.showcase_button_secondary), onClick = {})
            }
            HintChip(text = stringResource(R.string.showcase_chip_hint))
            InverseCard()
        }

        Section(stringResource(R.string.showcase_section_colors)) {
            colorTokens().forEach { (name, color) -> ColorRow(name, color) }
        }

        Section(stringResource(R.string.showcase_section_type)) {
            val sample = stringResource(R.string.showcase_sample_text)
            textStyleTokens().forEach { (name, style) ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = sample, style = style)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

/** Sample of the inverse surface with its own text and chip colors. */
@Composable
private fun InverseCard() {
    val extended = MaterialTheme.extendedColors
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.s18),
            verticalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            Text(
                text = "inverse-surface · on-inverse-muted",
                style = MaterialTheme.typography.labelMedium,
                color = extended.onInverseMuted,
            )
            Text(text = stringResource(R.string.showcase_button_primary), style = MaterialTheme.typography.displaySmall)
            Surface(shape = MaterialTheme.shapes.small, color = extended.inverseAccentContainer) {
                Text(
                    text = stringResource(R.string.showcase_chip_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.onInverseAccentContainer,
                    modifier = Modifier.padding(horizontal = Spacing.s12, vertical = Spacing.s10),
                )
            }
        }
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.swatch)
                .background(color, MaterialTheme.shapes.small)
                .border(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        )
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun colorTokens(): List<Pair<String, Color>> {
    val c = MaterialTheme.colorScheme
    val e = MaterialTheme.extendedColors
    return listOf(
        "ground" to c.background,
        "surface" to c.surface,
        "surface-sunken" to c.surfaceContainerLow,
        "surface-muted" to c.surfaceVariant,
        "segment-active" to e.segmentActive,
        "track" to e.track,
        "line" to c.outlineVariant,
        "line-control" to c.outline,
        "ink" to c.onSurface,
        "ink-muted" to c.onSurfaceVariant,
        "ink-placeholder" to e.inkPlaceholder,
        "accent" to c.primary,
        "on-accent" to c.onPrimary,
        "accent-chart" to e.accentChart,
        "accent-container" to c.primaryContainer,
        "on-accent-container" to c.onPrimaryContainer,
        "inverse-surface" to c.inverseSurface,
        "on-inverse" to c.inverseOnSurface,
        "on-inverse-muted" to e.onInverseMuted,
        "inverse-raised" to e.inverseRaised,
        "inverse-accent-container" to e.inverseAccentContainer,
        "on-inverse-accent-container" to e.onInverseAccentContainer,
        "chart-grid" to e.chartGrid,
        "chart-band" to e.chartBand,
        "chart-area" to e.chartArea,
    )
}

@Composable
private fun textStyleTokens(): List<Pair<String, TextStyle>> {
    val t = MaterialTheme.typography
    return listOf(
        "display-xl" to t.displayLarge,
        "display-l" to t.displayMedium,
        "display-m" to t.displaySmall,
        "title-l" to t.headlineSmall,
        "title-m" to t.titleLarge,
        "title-s" to t.titleMedium,
        "label-l" to t.titleSmall,
        "body-strong" to t.bodyMedium,
        "body" to t.bodyLarge,
        "label-m" to t.labelLarge,
        "caption" to t.bodySmall,
        "nav-label" to AppTextStyles.navLabel,
        "overline" to t.labelMedium,
        "micro" to t.labelSmall,
    )
}
