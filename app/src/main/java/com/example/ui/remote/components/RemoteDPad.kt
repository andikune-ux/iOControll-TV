package dev.andikuneiocontroll.ui.remote.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime

/**
 * RemoteDPad — D-Pad bulat seperti remote fisik.
 *
 * Layout:
 *        [▲]
 *    [◄] [OK] [►]
 *        [▼]
 */
@Composable
fun RemoteDPad(
    size: Dp = 200.dp,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DarkBgCard)
            .border(2.dp, StabiloCyan.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Tombol UP
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.light(context)
                    onUp()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Atas",
                tint = StabiloLime,
                modifier = Modifier.size(size * 0.16f)
            )
        }

        // Tombol DOWN
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.BottomCenter)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.light(context)
                    onDown()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Bawah",
                tint = StabiloLime,
                modifier = Modifier.size(size * 0.16f)
            )
        }

        // Tombol LEFT
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.light(context)
                    onLeft()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Kiri",
                tint = StabiloLime,
                modifier = Modifier.size(size * 0.16f)
            )
        }

        // Tombol RIGHT
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.light(context)
                    onRight()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Kanan",
                tint = StabiloLime,
                modifier = Modifier.size(size * 0.16f)
            )
        }

        // Tombol OK (tengah)
        Box(
            modifier = Modifier
                .size(size * 0.42f)
                .clip(CircleShape)
                .background(StabiloLime)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.medium(context)
                    onOk()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.1f).sp
            )
        }

        // Ring dekorasi
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .clip(CircleShape)
                .border(1.dp, StabiloLime.copy(alpha = 0.15f), CircleShape)
        )
    }
}
