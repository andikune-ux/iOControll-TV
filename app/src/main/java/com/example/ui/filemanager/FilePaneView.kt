package dev.andikuneiocontroll.ui.filemanager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.model.FileCategory
import dev.andikuneiocontroll.model.FileItem
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloPurple
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextMuted
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilePaneView(
    paneTitle: String,
    currentPath: String,
    items: List<FileItem>,
    selectedCount: Int,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onToggleSelect: (FileItem) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSwitchStorage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        // Storage Shortcut Quick Tabs (Internal, APK Apps, Vault)
        val storages = listOf("Penyimpanan Internal", "Aplikasi (APK)", "Vault Terenkripsi")
        ScrollableTabRow(
            selectedTabIndex = if (currentPath.contains("APPLICATIONS")) 1 else if (currentPath.endsWith(".vault")) 2 else 0,
            containerColor = DarkBgCard,
            contentColor = StabiloLime,
            edgePadding = 8.dp
        ) {
            storages.forEachIndexed { idx, label ->
                val isSelected = (idx == 1 && currentPath.contains("APPLICATIONS")) ||
                        (idx == 2 && currentPath.endsWith(".vault")) ||
                        (idx == 0 && !currentPath.contains("APPLICATIONS") && !currentPath.endsWith(".vault"))

                Tab(
                    selected = isSelected,
                    onClick = { onSwitchStorage(label) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (idx) {
                                0 -> Icons.Default.Storage
                                1 -> Icons.Default.Android
                                else -> Icons.Default.Lock
                            }
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) StabiloLime else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) StabiloLime else TextSecondary
                            )
                        }
                    }
                )
            }
        }

        // Current Path Bar & Parent Directory Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBgCardElevated)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateUp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Naik Folder",
                    tint = StabiloCyan
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = paneTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = StabiloLime,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = currentPath,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectedCount > 0) {
                Box(
                    modifier = Modifier
                        .background(StabiloYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, StabiloYellow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$selectedCount dipilih",
                        color = StabiloYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // File and Folder List
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Folder ini kosong atau sedang dimuat...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                items(items, key = { it.path }) { item ->
                    FileItemRow(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onToggleSelect = { onToggleSelect(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) StabiloLime.copy(alpha = 0.12f) else DarkBgCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isSelected) StabiloLime.copy(alpha = 0.6f) else Color(0xFF1E2838)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for Multi-Selection
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = StabiloLime,
                    checkmarkColor = Color.Black,
                    uncheckedColor = Color(0xFF475569)
                ),
                modifier = Modifier.size(36.dp)
            )

            // Category Icon
            val (icon, tint) = getCategoryIconAndColor(item)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File Name and Size Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.formattedSize,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun getCategoryIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) return Icons.Default.Folder to StabiloYellow
    return when (item.category) {
        FileCategory.VIDEO -> Icons.Default.Movie to StabiloPink
        FileCategory.AUDIO -> Icons.Default.AudioFile to StabiloCyan
        FileCategory.IMAGE -> Icons.Default.Image to StabiloLime
        FileCategory.ARCHIVE -> Icons.Default.Archive to StabiloPurple
        FileCategory.APK -> Icons.Default.Android to StabiloLime
        FileCategory.DOCUMENT -> Icons.Default.PictureAsPdf to StabiloCyan
        FileCategory.CODE -> Icons.Default.Code to StabiloPurple
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to TextSecondary
    }
}
