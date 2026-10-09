package dev.andikuneiocontroll.ui.remote.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary
import kotlin.math.abs

// ==========================================================
// 1. GRID (angka 0-9 + RGBY)
// ==========================================================
@Composable
fun RemoteGrid(
    onNumber: (Int) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onColor: (String) -> Unit,
    onBackToRemote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tombol biru Kembali ke Remote
        Button(
            onClick = onBackToRemote,
            colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembali ke Remote", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("0", "⌫", "⏎")
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { label ->
                    RemoteTextButton(
                        text = label,
                        accentColor = StabiloLime,
                        onClick = {
                            when (label) {
                                "⌫" -> onBackspace()
                                "⏎" -> onEnter()
                                else -> onNumber(label.toIntOrNull() ?: 0)
                            }
                        },
                        width = 72.dp,
                        height = 58.dp,
                        fontSize = 20
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ColorCircle("R", "MERAH", Color(0xFFEF4444), onColor)
            ColorCircle("G", "HIJAU", Color(0xFF22C55E), onColor)
            ColorCircle("Y", "KUNING", Color(0xFFFACC15), onColor)
            ColorCircle("B", "BIRU", Color(0xFF3B82F6), onColor)
        }
    }
}

@Composable
private fun ColorCircle(
    code: String,
    label: String,
    color: Color,
    onColor: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(color)
                .clickable { onColor(code) },
            contentAlignment = Alignment.Center
        ) {
            Text(code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}

// ==========================================================
// 2. MOUSE (touchpad kursor)
// ==========================================================
@Composable
fun RemoteMouse(
    onMove: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBgCardElevated)
            .border(1.5.dp, StabiloCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) onDoubleClick() else onClick()
                        lastTapTime = now
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Mouse, "Mouse", tint = StabiloCyan.copy(alpha = 0.5f), modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Touchpad", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Drag = gerakkan kursor\nTap = klik\nDouble tap = klik 2x", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================================
// 3. GESTURE (swipe = D-Pad virtual)
// ==========================================================
@Composable
fun RemoteGesture(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBgCardElevated)
            .border(1.5.dp, StabiloYellow.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { /* reset */ },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        if (abs(dx) > abs(dy)) {
                            if (dx > 30) onRight() else if (dx < -30) onLeft()
                        } else {
                            if (dy > 30) onDown() else if (dy < -30) onUp()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ScreenRotation, "Gesture", tint = StabiloYellow.copy(alpha = 0.5f), modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Gesture Area", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Swipe 4 arah = D-Pad\nTap = OK", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================================
// 4. AIR MOUSE (gyroscope)
// ==========================================================
@Composable
fun RemoteAirMouse(
    onMove: (Float, Float) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isActive by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (gyro != null) {
            val listener = object : SensorEventListener {
                private var lastTime = 0L
                override fun onSensorChanged(event: SensorEvent?) {
                    if (!isActive || event == null) return
                    val now = System.currentTimeMillis()
                    if (now - lastTime < 35) return
                    lastTime = now

                    val dx: Float
                    val dy: Float
                    if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        dx = -event.values[2] * 25f
                        dy = -event.values[0] * 25f
                    } else {
                        dx = -event.values[0] * 8f
                        dy = event.values[1] * 8f
                    }
                    if (abs(dx) > 0.5f || abs(dy) > 0.5f) onMove(dx, dy)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBgCardElevated)
            .border(1.5.dp, StabiloCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Sensors, "Air Mouse", tint = StabiloCyan.copy(alpha = 0.6f), modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Air Mouse Aktif", color = StabiloCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Gerakkan HP untuk\nmenggerakkan kursor TV\n\nTap = klik", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}
