package com.example.ui.filemanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FileItem

@Composable
fun FileContextMenuDialog(
    item: FileItem,
    onView: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onHexView: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${item.formattedSize} • ${item.path}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(14.dp))

                ContextMenuItem(Icons.Default.Visibility, "Lihat / Buka (View)", onDismiss, onView)
                ContextMenuItem(Icons.Default.ContentCopy, "Salin ke Pane Sebelah (Copy)", onDismiss, onCopy)
                ContextMenuItem(Icons.Default.DriveFileMove, "Pindah ke Pane Sebelah (Move)", onDismiss, onMove)
                if (item.isArchive) {
                    ContextMenuItem(Icons.Default.Unarchive, "Ekstrak Arsip (Extract)", onDismiss, onExtract)
                } else {
                    ContextMenuItem(Icons.Default.Archive, "Kompres ke ZIP (Compress)", onDismiss, onCompress)
                }
                ContextMenuItem(Icons.Default.Edit, "Ubah Nama (Rename)", onDismiss, onRename)
                ContextMenuItem(Icons.Default.OpenInNew, "Hex Viewer", onDismiss, onHexView)
                ContextMenuItem(Icons.Default.Share, "Bagikan Berkas (Share)", onDismiss, onShare)
                ContextMenuItem(Icons.Default.Delete, "Hapus (Delete)", onDismiss, onDelete, Color(0xFFF43F5E))
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    title: String,
    onDismiss: () -> Unit,
    action: () -> Unit,
    tint: Color = Color(0xFF06B6D4)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onDismiss()
                action()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
fun CreateFolderDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Buat Folder Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nama Folder") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("Batal", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (folderName.isNotEmpty()) {
                                onCreate(folderName.trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Text("Buat", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    initialName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Ubah Nama Berkas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("Batal", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newName.isNotEmpty()) {
                                onRename(newName.trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BatchRenameDialog(
    fileCount: Int,
    onBatchRename: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Ubah Nama Massal ($fileCount berkas)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Awalan (Prefix)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = suffix,
                    onValueChange = { suffix = it },
                    label = { Text("Akhiran (Suffix)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("Batal", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onBatchRename(prefix, suffix)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Text("Terapkan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
