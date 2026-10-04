package io.github.wtfjb.aximo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import io.github.wtfjb.aximo.ui.theme.extendedColors

/**
 * Switch per design system: on = inverse-surface track with accent thumb,
 * off = placeholder-ink track with surface thumb. [onCheckedChange] null
 * when the whole row handles the tap.
 */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MaterialTheme.colorScheme.inverseSurface,
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.inverseSurface,
            uncheckedTrackColor = MaterialTheme.extendedColors.inkPlaceholder,
            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
            uncheckedBorderColor = MaterialTheme.extendedColors.inkPlaceholder,
        ),
    )
}
