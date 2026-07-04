package com.novafinance.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

/**
 * Base surface for every card in the app — elevated-tone fill, large
 * radius, standard internal padding. Screens should reach for this (or
 * [NovaOutlinedCard]) instead of a raw `Surface`/`Card` so a future change
 * to "what a card looks like" happens in exactly one place.
 */
@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    shape: Shape = Nova.shapes.large,
    contentPadding: PaddingValues = PaddingValues(Nova.spacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(Nova.colors.elevatedSurface)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.sm),
        content = content
    )
}

/** Same role as [NovaCard] but bordered on the base surface instead of filled — for lower-emphasis grouped content. */
@Composable
fun NovaOutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = Nova.shapes.large,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(Nova.colors.surface)
            .border(BorderStroke(1.dp, Nova.colors.border), shape)
            .padding(Nova.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.sm),
        content = content
    )
}
