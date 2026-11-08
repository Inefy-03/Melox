package com.melox.player.ui.component.playback

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import com.melox.player.ui.component.os4.IconButton

@Composable
internal fun PlayerControlIconButton(
    onClick: () -> Unit,
    minWidth: Dp,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIndication provides PlayerControlNoIndication) {
        val longClickModifier = if (onLongClick != null) {
            Modifier.combinedClickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = null,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
        } else {
            Modifier
        }
        IconButton(
            onClick = onClick,
            modifier = modifier.then(longClickModifier),
            enabled = enabled && onLongClick == null,
            minWidth = minWidth,
            minHeight = minHeight,
            content = content,
        )
    }
}

private object PlayerControlNoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PlayerControlNoIndicationNode()

    override fun equals(other: Any?) = other === this
    override fun hashCode() = javaClass.hashCode()
}

private class PlayerControlNoIndicationNode : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() = drawContent()
}
