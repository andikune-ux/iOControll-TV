package dev.andikuneiocontroll.viewers

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dev.andikuneiocontroll.model.StorageCategoryInfo
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
    filePath: String,
    fileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true // Auto-play immediately as requested!
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = fileName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ImageViewerDialog(
    filePath: String,
    fileName: String,
    allImages: List<String>,
    onDismiss: () -> Unit
) {
    var currentIndex by remember {
        mutableStateOf(allImages.indexOf(filePath).coerceAtLeast(0))
    }
    val currentPath = if (allImages.isNotEmpty()) allImages[currentIndex] else filePath
    val currentFile = remember(currentPath) { File(currentPath) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${currentFile.name} (${currentIndex + 1}/${allImages.size.coerceAtLeast(1)})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }

            // Image Container with Zoom & Pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.8f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = currentFile,
                    contentDescription = currentFile.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            // Navigation Controls (Previous & Next)
            if (allImages.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            }
                        },
                        enabled = currentIndex > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = null)
                        Text("Sebelumnya")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < allImages.size - 1) {
                                currentIndex++
                                scale = 1f; offsetX = 0f; offsetY = 0f
                            }
                        },
                        enabled = currentIndex < allImages.size - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text("Berikutnya")
                        Icon(Icons.Default.NavigateNext, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerDialog(
    filePath: String,
    fileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = fileName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Text("Tutup Pemutar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TextViewerDialog(
    fileName: String,
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Teks: $fileName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HexViewerDialog(
    fileName: String,
    hexContent: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0F1D)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hex Viewer: $fileName",
                        color = Color(0xFF06B6D4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = hexContent,
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiskMapDialog(
    categoryStats: List<StorageCategoryInfo>,
    totalSizeBytes: Long,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Peta Disk (Disk Map)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Visualisasi penggunaan ruang memori berdasarkan kategori berkas:",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                categoryStats.forEach { cat ->
                    val fraction = if (totalSizeBytes > 0) (cat.sizeBytes.toFloat() / totalSizeBytes).coerceIn(0f, 1f) else 0f
                    val mb = cat.sizeBytes / (1024.0 * 1024.0)
                    val gb = mb / 1024.0
                    val sizeFormatted = if (gb >= 1.0) String.format("%.2f GB", gb) else String.format("%.1f MB", mb)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = cat.title, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(
                                    text = "$sizeFormatted (${cat.fileCount} berkas)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(cat.colorHex)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(cat.colorHex),
                                trackColor = Color(0xFF334155),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultDialog(
    onEncrypt: (String) -> Unit,
    onDecrypt: (String) -> Unit,
    isDecryptMode: Boolean,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDecryptMode) "Buka Kunci Vault (AES-256)" else "Kunci ke Vault (AES-256)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isDecryptMode)
                        "Masukkan kata sandi untuk mendekripsi arsip .vault ini."
                    else
                        "Berkas yang dipilih akan dienkripsi dengan standar militer AES-256 GCM.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi Vault") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Batal", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (password.isNotEmpty()) {
                                if (isDecryptMode) onDecrypt(password) else onEncrypt(password)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text(if (isDecryptMode) "Dekripsi" else "Enkripsi", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
