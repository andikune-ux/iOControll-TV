package dev.andikuneiocontroll.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TooltipSide { LEFT, RIGHT }

/**
 * Icon Remote TV dengan warna berputar smooth (palet Stabilo).
 * Glow = border tipis 1.5dp, tidak melebar.
 */
@Composable
fun RainbowRemoteIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 24.dp
) {
    val infinite = rememberInfiniteTransition(label = "rainbow")
    val color by infinite.animateColor(
        initialValue = StabiloCyan,
        targetValue = StabiloCyan,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                StabiloCyan at 0 with LinearEasing
                StabiloLime at 1500 with LinearEasing
                StabiloYellow at 3000 with LinearEasing
                StabiloPink at 4500 with LinearEasing
                StabiloCyan at 6000 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowColor"
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .border(1.5.dp, color.copy(alpha = 0.75f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SettingsRemote,
            contentDescription = "Remote TV",
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Icon button dengan tooltip.
 * Long press 1 detik -> popup kecil muncul di samping.
 */
@Composable
fun StabiloTooltipButton(
    icon: ImageVector,
    tooltip: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 26.dp,
    side: TooltipSide = TooltipSide.RIGHT
) {
    var showTooltip by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.wrapContentSize()) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            val down = awaitPointerEvent()

                            var longPressed = false
                            val longJob = scope.launch {
                                delay(1000)
                                longPressed = true
                                showTooltip = true
                            }

                            val up = awaitPointerEvent()
                            longJob.cancel()

                            if (!longPressed && down.changes.isNotEmpty() && up.changes.isNotEmpty()) {
                                onClick()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }

        if (showTooltip) {
            LaunchedEffect(showTooltip) {
                delay(2200)
                showTooltip = false
            }
            val offsetX = if (side == TooltipSide.RIGHT) 60 else -60
            Popup(
                alignment = Alignment.Center,
                offset = IntOffset(offsetX.dp.roundToPx(), 0),
                properties = PopupProperties(focusable = false),
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBgCardElevated,
                    border = BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = tooltip,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
