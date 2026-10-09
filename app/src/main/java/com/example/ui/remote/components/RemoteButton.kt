package dev.andikuneiocontroll.ui.remote.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.TextPrimary

/**
 * Ukuran tombol remote.
 */
enum class RemoteButtonSize(val buttonSize: Dp, val iconSize: Dp, val labelSize: Int) {
    SMALL(40.dp, 18.dp, 10),
    MEDIUM(52.dp, 24.dp, 11),
    LARGE(64.dp, 28.dp, 12)
}

/**
 * Tombol icon bulat untuk remote.
 * Warna bisa diatur, border accent, haptic feedback.
 */
@Composable
fun RemoteButton(
    icon: ImageVector,
    contentDescription: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: RemoteButtonSize = RemoteButtonSize.MEDIUM,
    filled: Boolean = false,
    hapticType: Int = 0 // 0=light, 1=medium, 2=heavy
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size.buttonSize)
            .clip(CircleShape)
            .background(if (filled) accentColor else DarkBgCardElevated)
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = if (filled) 1f else 0.5f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                when (hapticType) {
                    2 -> HapticHelper.heavy(context)
                    1 -> HapticHelper.medium(context)
                    else -> HapticHelper.light(context)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) Color.Black else accentColor,
            modifier = Modifier.size(size.iconSize)
        )
    }
}

/**
 * Tombol text (angka, RGBY, dll).
 */
@Composable
fun RemoteTextButton(
    text: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkBgCardElevated,
    textColor: Color = TextPrimary,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    width: Dp = 56.dp,
    height: Dp = 52.dp,
    fontSize: Int = 16,
    bold: Boolean = true,
    hapticType: Int = 0
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.5f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                when (hapticType) {
                    2 -> HapticHelper.heavy(context)
                    1 -> HapticHelper.medium(context)
                    else -> HapticHelper.light(context)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Tombol icon + label (untuk Home, Back, Recent, dll).
 */
@Composable
fun RemoteIconLabelButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: RemoteButtonSize = RemoteButtonSize.MEDIUM,
    hapticType: Int = 0
) {
    val context = LocalContext.current

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size.buttonSize)
                .clip(CircleShape)
                .background(DarkBgCardElevated)
                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    when (hapticType) {
                        2 -> HapticHelper.heavy(context)
                        1 -> HapticHelper.medium(context)
                        else -> HapticHelper.light(context)
                    }
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(size.iconSize)
            )
        }

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.size(4.dp)
        )

        Text(
            text = label,
            color = TextPrimary.copy(alpha = 0.85f),
            fontSize = size.labelSize.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Tombol persegi untuk toolbar (Voice, Input, Cast, dll).
 */
@Composable
fun RemoteSquareButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticType: Int = 0
) {
    val context = LocalContext.current

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkBgCardElevated)
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    when (hapticType) {
                        2 -> HapticHelper.heavy(context)
                        1 -> HapticHelper.medium(context)
                        else -> HapticHelper.light(context)
                    }
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(3.dp))
        Text(
            text = label,
            color = TextPrimary.copy(alpha = 0.8f),
            fontSize = 9.sp
        )
    }
}
