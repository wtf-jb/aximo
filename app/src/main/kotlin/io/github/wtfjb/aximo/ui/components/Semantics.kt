package io.github.wtfjb.aximo.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Gives a visual label a longer description for screen readers. */
fun Modifier.semanticsDescription(description: String): Modifier =
    semantics { contentDescription = description }
