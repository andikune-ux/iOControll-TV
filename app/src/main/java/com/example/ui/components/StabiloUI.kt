package dev.andikuneiocontroll.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * Icon Remote TV dengan warna berputar smooth (palet Stabilo).
 * Glow = border tipis 1.5dp, tidak melebar.
 * Support tooltip saat long-press 1 detik.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RainbowRemoteIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 42.dp,
    iconSize: Dp = 24.dp,
    tooltip: String = "Remote TV"
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

    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(2000)
            showTooltip = false
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .border(1.5.dp, color.copy(alpha = 0.75f), CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showTooltip = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SettingsRemote,
                contentDescription = tooltip,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }

        if (showTooltip) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-88).dp)
            ) {
                TooltipBubble(text = tooltip)
            }
        }
    }
}

/**
 * Icon button biasa dengan tooltip long-press 1 detik.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StabiloTooltipButton(
    icon: ImageVector,
    tooltip: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 26.dp
) {
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(2000)
            showTooltip = false
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showTooltip = true }
                ),
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
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-88).dp)
            ) {
                TooltipBubble(text = tooltip)
            }
        }
    }
}

@Composable
private fun TooltipBubble(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkBgCardElevated,
        border = BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.6f)),
        shadowElevation = 8.dp
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
