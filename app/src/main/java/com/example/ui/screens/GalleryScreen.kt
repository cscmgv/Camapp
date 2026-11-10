package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GeoPhotoEntity
import com.example.ui.theme.SatelliteEmerald
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SlateNavyDeep
import com.example.ui.theme.TelemetryCyan
import com.example.util.GeoFormatUtils
import java.io.File
import java.util.Locale

@Composable
fun GalleryScreen(
    allPhotos: List<GeoPhotoEntity>,
    filteredPhotos: List<GeoPhotoEntity>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    favoritesOnly: Boolean,
    inspectedPhoto: GeoPhotoEntity?,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onInspectPhoto: (GeoPhotoEntity?) -> Unit,
    onToggleFavorite: (GeoPhotoEntity) -> Unit,
    onUpdatePhotoMeta: (GeoPhotoEntity, String, String, String) -> Unit,
    onDeletePhoto: (GeoPhotoEntity) -> Unit,
    onSharePhoto: (GeoPhotoEntity) -> Unit,
    onFocusOnMap: (GeoPhotoEntity) -> Unit,
    onExportGpx: () -> Unit,
    onSwitchToCamera: () -> Unit
) {
    // Compute total expedition trail length across chronological photos
    val totalTrailMeters = remember(allPhotos) {
        val sorted = allPhotos.sortedBy { it.timestamp }
        var dist = 0.0
        for (i in 0 until sorted.size - 1) {
            dist += GeoFormatUtils.haversineDistanceMeters(
                sorted[i].latitude,
                sorted[i].longitude,
                sorted[i + 1].latitude,
                sorted[i + 1].longitude
            )
        }
        dist
    }

    val maxElevation = remember(allPhotos) {
        allPhotos.maxOfOrNull { it.altitudeMeters } ?: 0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("gallery_field_log_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Expedition Hero Summary Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(175.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_expedition_1794244848753),
                        contentDescription = "Field Survey Summary Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        SlateNavyDeep.copy(alpha = 0.35f),
                                        SlateNavyDeep.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SignalAmber
                            ) {
                                Text(
                                    text = "OFFLINE GEOTAG VAULT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SlateNavyDeep.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .border(1.dp, TelemetryCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
                                    .clickable { onExportGpx() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("btn_export_gpx_header")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export GPX Track",
                                        tint = TelemetryCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Export GPX",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TelemetryCyan
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Field Geotag Logbook",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                MetricPill("GEOTAGS", "${allPhotos.size} Photos", SignalAmber)
                                MetricPill("TRAIL SPAN", GeoFormatUtils.formatDistance(totalTrailMeters), TelemetryCyan)
                                MetricPill("MAX ELEV", String.format(Locale.US, "%.1fm", maxElevation), SatelliteEmerald)
                            }
                        }
                    }
                }
            }
        }

        // 2. Search & Category Filter Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search title, notes, Plus Code, or sector...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = onToggleFavoritesOnly,
                        label = { Text("★ Favorites") },
                        modifier = Modifier.testTag("chip_filter_favorites")
                    )

                    val allTabs = listOf("All") + categories
                    allTabs.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        // 3. Empty State or Geotagged Photo Cards
        if (filteredPhotos.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = SignalAmber,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching geotagged photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Capture a new field photo with burned-in GPS coordinates or clear your search filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )
                        Button(onClick = onSwitchToCamera) {
                            Text("Open GeoCam Viewfinder")
                        }
                    }
                }
            }
        } else {
            items(filteredPhotos, key = { it.id }) { photo ->
                GeotagPhotoLogCard(
                    photo = photo,
                    onClick = { onInspectPhoto(photo) },
                    onToggleFavorite = { onToggleFavorite(photo) },
                    onViewOnMap = { onFocusOnMap(photo) },
                    onShare = { onSharePhoto(photo) }
                )
            }
        }
    }

    // Full Geotag Inspector & Editor Modal
    if (inspectedPhoto != null) {
        GeotagInspectorDialog(
            photo = inspectedPhoto,
            categories = categories,
            onDismiss = { onInspectPhoto(null) },
            onSaveEdits = { title, notes, cat ->
                onUpdatePhotoMeta(inspectedPhoto, title, notes, cat)
            },
            onFocusMap = { onFocusOnMap(inspectedPhoto) },
            onShare = { onSharePhoto(inspectedPhoto) },
            onDelete = { onDeletePhoto(inspectedPhoto) }
        )
    }
}

@Composable
private fun MetricPill(label: String, value: String, accent: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GeotagPhotoLogCard(
    photo: GeoPhotoEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onViewOnMap: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("photo_log_card_${photo.id}")
    ) {
        Column {
            // Top Stamped Photo Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Category + Plus Code Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SlateNavyDeep.copy(alpha = 0.86f),
                        modifier = Modifier.border(1.dp, SignalAmber.copy(alpha = 0.7f), RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "${photo.category} • ${photo.plusCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SignalAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SlateNavyDeep.copy(alpha = 0.80f))
                    ) {
                        Icon(
                            imageVector = if (photo.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle favorite waypoint",
                            tint = if (photo.isFavorite) SignalAmber else Color.White
                        )
                    }
                }
            }

            // Bottom Metadata & Quick Actions
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = photo.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = GeoFormatUtils.formatShortDate(photo.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (photo.notes.isNotBlank()) {
                    Text(
                        text = photo.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Coordinate & Telemetry Strip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "GPS: ${GeoFormatUtils.formatDecimalDegrees(photo.latitude, photo.longitude)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "UTM: ${photo.utmCoord} • ALT: ${String.format(Locale.US, "%.1fm", photo.altitudeMeters)} • HDG: ${GeoFormatUtils.cardinalDirection(photo.bearingDegrees)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "📍 ${photo.addressLine}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onViewOnMap,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_card_locate_map_${photo.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Locate on Map")
                    }

                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share_photo),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Stamped JPG")
                    }
                }
            }
        }
    }
}

@Composable
private fun GeotagInspectorDialog(
    photo: GeoPhotoEntity,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSaveEdits: (String, String, String) -> Unit,
    onFocusMap: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember(photo) { mutableStateOf(photo.title) }
    var editNotes by remember(photo) { mutableStateOf(photo.notes) }
    var editCategory by remember(photo) { mutableStateOf(photo.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Geotag Record" else "Geotag Telemetry Inspector",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit waypoint metadata"
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                if (isEditing) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Waypoint Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Field Survey Notes") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = editCategory == cat,
                                onClick = { editCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = photo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (photo.notes.isNotBlank()) {
                        Text(
                            text = photo.notes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "DEC: ${GeoFormatUtils.formatDecimalDegrees(photo.latitude, photo.longitude)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "DMS: ${GeoFormatUtils.formatDMS(photo.latitude, photo.longitude)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "UTM: ${photo.utmCoord}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "PLUS CODE: ${photo.plusCode}  |  ACC: ±${photo.accuracyMeters}m",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "ELEVATION: ${String.format(Locale.US, "%.1f m MSL", photo.altitudeMeters)}  |  BEARING: ${GeoFormatUtils.cardinalDirection(photo.bearingDegrees)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "SECTOR: ${photo.addressLine}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "OFFLINE FILE: ${photo.fileSizeBytes / 1024} KB (EXIF + Stamp Burned)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onFocusMap,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Map Pin")
                        }
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        onSaveEdits(editTitle, editNotes, editCategory)
                        isEditing = false
                    }
                ) {
                    Text("Save Changes")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (!isEditing) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_photo),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    )
}
