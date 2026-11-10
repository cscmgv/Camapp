package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GeoPhotoEntity
import com.example.data.OfflineMapRegionEntity
import com.example.location.LiveTelemetryState
import com.example.location.WaypointPreset
import com.example.ui.theme.SatelliteEmerald
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TelemetryCyan
import com.example.util.GeoFormatUtils
import java.util.Locale

@Composable
fun OfflineVaultScreen(
    telemetry: LiveTelemetryState,
    photos: List<GeoPhotoEntity>,
    offlineRegions: List<OfflineMapRegionEntity>,
    presets: List<WaypointPreset>,
    isDownloading: Boolean,
    downloadProgress: Float,
    darkThemeEnabled: Boolean,
    onToggleTheme: () -> Unit,
    onDownloadRegion: (String, Double, Double) -> Unit,
    onDeleteRegion: (Long) -> Unit,
    onJumpToRegionOnMap: (Double, Double) -> Unit,
    onExportGpxTrack: () -> Unit
) {
    var customPackName by remember(telemetry.addressLine) {
        mutableStateOf(telemetry.addressLine.take(28))
    }

    val totalPhotoBytes = remember(photos) {
        photos.sumOf { it.fileSizeBytes }
    }
    val totalTileBytes = remember(offlineRegions) {
        offlineRegions.sumOf { it.storageBytes }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("offline_vault_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Offline Storage Vault Status Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = SatelliteEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Storage & Map Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Daylight / Tactical Dark Theme Switcher for Field Visibility
                        OutlinedButton(
                            onClick = onToggleTheme,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("btn_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (darkThemeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Switch between tactical dark and daylight survey theme",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (darkThemeEnabled) "Daylight Mode" else "Tactical Dark",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All geotagged photos, EXIF coordinates, Plus Codes, and downloaded map tile packs are stored locally in encrypted-ready internal app storage for 100% offline field reliability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StorageStatBox(
                            title = "STAMPED PHOTOS",
                            value = "${photos.size} files (${formatBytes(totalPhotoBytes)})",
                            color = SignalAmber,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StorageStatBox(
                            title = "OFFLINE MAP PACKS",
                            value = "${offlineRegions.size} packs (${formatBytes(totalTileBytes)})",
                            color = TelemetryCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onExportGpxTrack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_vault_export_gpx")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All Waypoints & Track (.GPX)")
                    }
                }
            }
        }

        // 2. Download New Offline Map Tile Pack for Current GPS Sector
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = TelemetryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cache Current GPS Sector for Offline Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Target Coordinates: ${GeoFormatUtils.formatDecimalDegrees(telemetry.latitude, telemetry.longitude)} (Zoom 12–14 tiles + Topo Grid)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = customPackName,
                        onValueChange = { customPackName = it },
                        label = { Text("Offline Sector Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_offline_pack_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDownloading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Caching map tiles to internal storage... ${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TelemetryCyan
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onDownloadRegion(
                                    customPackName.ifBlank { "Sector ${GeoFormatUtils.toPlusCode(telemetry.latitude, telemetry.longitude)}" },
                                    telemetry.latitude,
                                    telemetry.longitude
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_download_offline_pack")
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Offline Map Pack")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Or one-tap cache a field expedition benchmark:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.take(3).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isDownloading) {
                                        onDownloadRegion(preset.name, preset.latitude, preset.longitude)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(preset.name, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "${String.format(Locale.US, "%.4f, %.4f", preset.latitude, preset.longitude)} • Elev ${preset.altitudeMeters}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "CACHE PACK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Saved Offline Map Regions List
        item {
            Text(
                text = "Downloaded Offline Map Regions (${offlineRegions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(offlineRegions, key = { it.id }) { region ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = region.regionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = GeoFormatUtils.formatDecimalDegrees(region.centerLat, region.centerLng),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Zoom ${region.minZoom}–${region.maxZoom} • ${region.cachedTilesCount} Tiles • ${formatBytes(region.storageBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onJumpToRegionOnMap(region.centerLat, region.centerLng) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open offline region on map",
                                tint = SignalAmber
                            )
                        }
                        IconButton(
                            onClick = { onDeleteRegion(region.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove offline region",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageStatBox(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
