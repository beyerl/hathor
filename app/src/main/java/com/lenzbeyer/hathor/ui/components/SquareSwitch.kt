package com.lenzbeyer.hathor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Square Switch: M3's pill-shaped Switch reflattened to angular per SPEC §4.2.
 * Track is filled when checked, outlined when not. Thumb is a square inside the track.
 */
@Composable
fun SquareSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .height(28.dp)
            .width(56.dp)
            .then(
                if (checked) Modifier.background(cs.onSurface, RectangleShape)
                else Modifier.border(1.dp, cs.outline, RectangleShape)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(24.dp)
                .background(if (checked) cs.surface else cs.onSurface, RectangleShape)
        )
    }
}
