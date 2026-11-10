package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GeoPhotoEntity
import com.example.location.LiveTelemetryState
import com.example.map.OfflineTileManager
import com.example.ui.MapStyleMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SatelliteEmerald
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SlateNavyCard
import com.example.ui.theme.SlateNavyDeep
import com.example.ui.theme.TelemetryCyan
import com.example.util.GeoFormatUtils
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ScreenPin(
    val photo: GeoPhotoEntity,
    val indexNumber: Int,
    val screenX: Float,
    val screenY: Float
)

@Composable
fun GpsMapScreen(
    telemetry: LiveTelemetryState,
    photos: List<GeoPhotoEntity>,
    selectedPhoto: GeoPhotoEntity?,
    mapCenterTarget: Pair<Double, Double>?,
    mapStyleMode: MapStyleMode,
    forceOfflineMode: Boolean,
    tileManager: OfflineTileManager,
    onSelectPhoto: (GeoPhotoEntity?) -> Unit,
    onInspectPhoto: (GeoPhotoEntity) -> Unit,
    onSharePhoto: (GeoPhotoEntity) -> Unit,
    onClearMapTarget: () -> Unit,
    onSetMapStyle: (MapStyleMode) -> Unit,
    onToggleOfflineLock: () -> Unit,
    onJumpToCameraWithCoords: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    var centerLat by remember { mutableDoubleStateOf(telemetry.latitude) }
    var centerLng by remember { mutableDoubleStateOf(telemetry.longitude) }
    var zoomLevel by remember { mutableDoubleStateOf(14.0) }
    var tileFetchTrigger by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(mapCenterTarget) {
        if (mapCenterTarget != null) {
            centerLat = mapCenterTarget.first
            centerLng = mapCenterTarget.second
            onClearMapTarget()
        }
    }

    // Asynchronously load visible OSM tiles into memory cache when in HYBRID_OSM mode
    val intZoom = zoomLevel.roundToInt().coerceIn(3, 17)
    LaunchedEffect(centerLat, centerLng, intZoom, mapStyleMode, forceOfflineMode) {
        if (mapStyleMode == MapStyleMode.HYBRID_OSM) {
            val cx = GeoFormatUtils.lonToTileX(centerLng, intZoom)
            val cy = GeoFormatUtils.latToTileY(centerLat, intZoom)
            val maxTile = (1 shl intZoom) - 1
            for (dx in -2..2) {
                for (dy in -2..2) {
                    val tx = (cx + dx).coerceIn(0, maxTile)
                    val ty = (cy + dy).coerceIn(0, maxTile)
                    val bmp = tileManager.loadOrFetchTile(
                        z = intZoom,
                        x = tx,
                        y = ty,
                        allowNetwork = !forceOfflineMode && telemetry.isDeviceOnline
                    )
                    if (bmp != null) {
                        tileFetchTrigger++
                    }
                }
            }
        }
    }

    // Precompute screen coordinates of photo pins for tap detection
    val chronologicalPhotos = remember(photos) { photos.sortedBy { it.timestamp } }

    Box(modifier = Modifier.fillMaxSize().background(SlateNavyDeep)) {
        // 1. Interactive Multi-Touch GPS Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomLevel, centerLat, centerLng) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val newZoom = (zoomLevel + kotlin.math.ln(zoomChange.toDouble()) / kotlin.math.ln(2.0))
                            .coerceIn(3.5, 17.5)
                        zoomLevel = newZoom

                        val centerWorldX = GeoFormatUtils.lonToWorldX(centerLng, zoomLevel) - pan.x
                        val centerWorldY = GeoFormatUtils.latToWorldY(centerLat, zoomLevel) - pan.y
                        centerLng = GeoFormatUtils.worldXToLon(centerWorldX, zoomLevel).coerceIn(-179.9, 179.9)
                        centerLat = GeoFormatUtils.worldYToLat(centerWorldY, zoomLevel).coerceIn(-82.0, 82.0)
                    }
                }
                .pointerInput(chronologicalPhotos, centerLat, centerLng, zoomLevel, canvasSize) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (canvasSize.width == 0) return@detectTapGestures
                            val cWorldX = GeoFormatUtils.lonToWorldX(centerLng, zoomLevel)
                            val cWorldY = GeoFormatUtils.latToWorldY(centerLat, zoomLevel)
                            val cx = canvasSize.width / 2f
                            val cy = canvasSize.height / 2f

                            val tappedPhoto = chronologicalPhotos.firstOrNull { p ->
                                val px = (GeoFormatUtils.lonToWorldX(p.longitude, zoomLevel) - cWorldX + cx).toFloat()
                                val py = (GeoFormatUtils.latToWorldY(p.latitude, zoomLevel) - cWorldY + cy).toFloat()
                                hypot(tapOffset.x - px, tapOffset.y - py) <= 44.dp.toPx()
                            }
                            if (tappedPhoto != null) {
                                onSelectPhoto(tappedPhoto)
                            }
                        }
                    )
                }
                .testTag("interactive_gps_map_canvas")
        ) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            // Reference tileFetchTrigger to redraw when cached tiles finish loading
            val _trigger = tileFetchTrigger

            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val zInt = zoomLevel.roundToInt().coerceIn(3, 17)
            val centerWorldX = GeoFormatUtils.lonToWorldX(centerLng, zInt.toDouble())
            val centerWorldY = GeoFormatUtils.latToWorldY(centerLat, zInt.toDouble())

            // Background Layer
            val bgColor = when (mapStyleMode) {
                MapStyleMode.HYBRID_OSM -> Color(0xFF0C1829)
                MapStyleMode.TACTICAL_RADAR -> Color(0xFF07111E)
                MapStyleMode.TOPO_CONTOUR -> Color(0xFF0E1D1A)
            }
            drawRect(color = bgColor)

            // Layer A: OpenStreetMap Raster Tiles (Cached Offline or Online)
            if (mapStyleMode == MapStyleMode.HYBRID_OSM) {
                val centerTileX = floor(centerWorldX / 256.0).toInt()
                val centerTileY = floor(centerWorldY / 256.0).toInt()
                val maxTile = (1 shl zInt) - 1

                for (dx in -2..2) {
                    for (dy in -2..2) {
                        val tx = centerTileX + dx
                        val ty = centerTileY + dy
                        if (tx in 0..maxTile && ty in 0..maxTile) {
                            val tileLeft = (tx * 256.0 - centerWorldX + cx).roundToInt()
                            val tileTop = (ty * 256.0 - centerWorldY + cy).roundToInt()
                            val bmp = tileManager.getMemoryTile(zInt, tx, ty)
                            if (bmp != null) {
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = IntOffset(tileLeft, tileTop),
                                    dstSize = IntSize(256, 256),
                                    alpha = 0.80f
                                )
                            }
                        }
                    }
                }
            }

            // Layer B: Tactical Coordinate Grid & Topographic Contours (Always available 100% Offline)
            val gridStepPx = 140f
            val offsetX = ((-centerWorldX.toFloat()) % gridStepPx + gridStepPx) % gridStepPx
            val offsetY = ((-centerWorldY.toFloat()) % gridStepPx + gridStepPx) % gridStepPx
            val gridColor = when (mapStyleMode) {
                MapStyleMode.HYBRID_OSM -> TelemetryCyan.copy(alpha = 0.16f)
                MapStyleMode.TACTICAL_RADAR -> TelemetryCyan.copy(alpha = 0.25f)
                MapStyleMode.TOPO_CONTOUR -> SatelliteEmerald.copy(alpha = 0.22f)
            }

            var gx = offsetX
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1.dp.toPx())
                gx += gridStepPx
            }
            var gy = offsetY
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx())
                gy += gridStepPx
            }

            // If Topographic or Tactical mode, draw contour rings around waypoints & current fix
            if (mapStyleMode != MapStyleMode.HYBRID_OSM) {
                chronologicalPhotos.forEach { photo ->
                    val px = (GeoFormatUtils.lonToWorldX(photo.longitude, zoomLevel) - GeoFormatUtils.lonToWorldX(centerLng, zoomLevel) + cx).toFloat()
                    val py = (GeoFormatUtils.latToWorldY(photo.latitude, zoomLevel) - GeoFormatUtils.latToWorldY(centerLat, zoomLevel) + cy).toFloat()
                    for (ring in 1..3) {
                        drawCircle(
                            color = if (mapStyleMode == MapStyleMode.TOPO_CONTOUR) {
                                SatelliteEmerald.copy(alpha = 0.16f)
                            } else {
                                TelemetryCyan.copy(alpha = 0.14f)
                            },
                            radius = (ring * 42).dp.toPx(),
                            center = Offset(px, py),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
            }

            // Layer C: Expedition Track Polyline connecting Geotagged Photos in chronological order
            val exactCenterWorldX = GeoFormatUtils.lonToWorldX(centerLng, zoomLevel)
            val exactCenterWorldY = GeoFormatUtils.latToWorldY(centerLat, zoomLevel)

            val pinLocations = chronologicalPhotos.mapIndexed { idx, p ->
                val sx = (GeoFormatUtils.lonToWorldX(p.longitude, zoomLevel) - exactCenterWorldX + cx).toFloat()
                val sy = (GeoFormatUtils.latToWorldY(p.latitude, zoomLevel) - exactCenterWorldY + cy).toFloat()
                ScreenPin(p, idx + 1, sx, sy)
            }

            if (pinLocations.size >= 2) {
                for (i in 0 until pinLocations.size - 1) {
                    val startPin = pinLocations[i]
                    val endPin = pinLocations[i + 1]
                    drawLine(
                        color = SignalAmber.copy(alpha = 0.85f),
                        start = Offset(startPin.screenX, startPin.screenY),
                        end = Offset(endPin.screenX, endPin.screenY),
                        strokeWidth = 3.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                    )
                }
            }

            // Layer D: Live User GPS Location & Directional Compass Heading Cone
            val userScreenX = (GeoFormatUtils.lonToWorldX(telemetry.longitude, zoomLevel) - exactCenterWorldX + cx).toFloat()
            val userScreenY = (GeoFormatUtils.latToWorldY(telemetry.latitude, zoomLevel) - exactCenterWorldY + cy).toFloat()

            // Accuracy Halo
            drawCircle(
                color = TelemetryCyan.copy(alpha = 0.18f),
                radius = 38.dp.toPx(),
                center = Offset(userScreenX, userScreenY)
            )
            drawCircle(
                color = TelemetryCyan.copy(alpha = 0.55f),
                radius = 38.dp.toPx(),
                center = Offset(userScreenX, userScreenY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Directional Cone
            val rad = Math.toRadians((telemetry.bearingDegrees - 90f).toDouble())
            val spread = Math.toRadians(25.0)
            val coneRadius = 56.dp.toPx()
            val conePath = Path().apply {
                moveTo(userScreenX, userScreenY)
                lineTo(
                    (userScreenX + coneRadius * cos(rad - spread)).toFloat(),
                    (userScreenY + coneRadius * sin(rad - spread)).toFloat()
                )
                lineTo(
                    (userScreenX + coneRadius * cos(rad + spread)).toFloat(),
                    (userScreenY + coneRadius * sin(rad + spread)).toFloat()
                )
                close()
            }
            drawPath(conePath, color = TelemetryCyan.copy(alpha = 0.32f))

            // User GPS Dot
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = Offset(userScreenX, userScreenY)
            )
            drawCircle(
                color = TelemetryCyan,
                radius = 6.dp.toPx(),
                center = Offset(userScreenX, userScreenY)
            )

            // Layer E: Geotagged Photo Markers
            pinLocations.forEach { pin ->
                val isSelected = selectedPhoto?.id == pin.photo.id
                val pinRadius = if (isSelected) 18.dp.toPx() else 14.dp.toPx()
                val pinColor = if (isSelected) SignalAmber else SatelliteEmerald

                if (isSelected) {
                    drawCircle(
                        color = SignalAmber.copy(alpha = 0.30f),
                        radius = 28.dp.toPx(),
                        center = Offset(pin.screenX, pin.screenY)
                    )
                }

                drawCircle(
                    color = SlateNavyDeep,
                    radius = pinRadius + 3.dp.toPx(),
                    center = Offset(pin.screenX, pin.screenY)
                )
                drawCircle(
                    color = pinColor,
                    radius = pinRadius,
                    center = Offset(pin.screenX, pin.screenY)
                )

                val badgeLayout = textMeasurer.measure(
                    text = "#${pin.indexNumber}",
                    style = TextStyle(
                        color = Color(0xFF06101E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                )
                drawText(
                    textLayoutResult = badgeLayout,
                    topLeft = Offset(
                        pin.screenX - badgeLayout.size.width / 2f,
                        pin.screenY - badgeLayout.size.height / 2f
                    )
                )
            }

            // Center Target Crosshair Reticle
            val reticleR = 10.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = reticleR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(cx - 16.dp.toPx(), cy),
                end = Offset(cx + 16.dp.toPx(), cy),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(cx, cy - 16.dp.toPx()),
                end = Offset(cx, cy + 16.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Top Map Telemetry & Style Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateNavyDeep.copy(alpha = 0.92f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TelemetryCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MAP RETICLE // ZOOM ${String.format(Locale.US, "%.1fx", zoomLevel)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SignalAmber
                        )
                        Text(
                            text = GeoFormatUtils.formatDecimalDegrees(centerLat, centerLng),
                            style = MaterialTheme.typography.titleSmall,
                            color = TelemetryCyan
                        )
                        Text(
                            text = "PLUS: ${GeoFormatUtils.toPlusCode(centerLat, centerLng)} • ${photos.size} Geotags",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Snap Photo at Map Crosshair Button
                    Button(
                        onClick = { onJumpToCameraWithCoords(centerLat, centerLng) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_snap_at_reticle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Geotag Here",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Map Style & Offline Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MapStyleMode.entries.forEach { mode ->
                        val active = mapStyleMode == mode
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) SignalAmber else SlateNavyDeep.copy(alpha = 0.88f),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (active) SignalAmber else Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(50)
                                )
                                .clickable { onSetMapStyle(mode) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("map_style_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Offline Lock Toggle Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (forceOfflineMode || !telemetry.isDeviceOnline) {
                        SatelliteEmerald.copy(alpha = 0.25f)
                    } else {
                        SlateNavyDeep.copy(alpha = 0.88f)
                    },
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (forceOfflineMode || !telemetry.isDeviceOnline) SatelliteEmerald else Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(50)
                        )
                        .clickable { onToggleOfflineLock() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_toggle_offline_map")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (forceOfflineMode || !telemetry.isDeviceOnline) {
                                Icons.Default.CloudOff
                            } else {
                                Icons.Default.CloudQueue
                            },
                            contentDescription = "Toggle offline tile lock",
                            tint = if (forceOfflineMode || !telemetry.isDeviceOnline) SatelliteEmerald else TelemetryCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (forceOfflineMode || !telemetry.isDeviceOnline) "OFFLINE" else "CACHED+NET",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. Right Floating Map Controls (Zoom In, Zoom Out, Cycle Waypoints, Center on GPS)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapControlFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_zoom_in),
                testTag = "btn_map_zoom_in",
                onClick = { zoomLevel = (zoomLevel + 1.0).coerceAtMost(17.5) }
            )
            MapControlFab(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_zoom_out),
                testTag = "btn_map_zoom_out",
                onClick = { zoomLevel = (zoomLevel - 1.0).coerceAtLeast(3.5) }
            )
            MapControlFab(
                icon = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.cd_center_map),
                testTag = "btn_map_center_gps",
                tint = TelemetryCyan,
                onClick = {
                    centerLat = telemetry.latitude
                    centerLng = telemetry.longitude
                    zoomLevel = 14.5
                }
            )
            if (chronologicalPhotos.isNotEmpty()) {
                MapControlFab(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Cycle through geotagged photo pins",
                    testTag = "btn_map_next_pin",
                    tint = SignalAmber,
                    onClick = {
                        val currentIndex = chronologicalPhotos.indexOfFirst { it.id == selectedPhoto?.id }
                        val nextPhoto = chronologicalPhotos[(currentIndex + 1) % chronologicalPhotos.size]
                        onSelectPhoto(nextPhoto)
                        centerLat = nextPhoto.latitude
                        centerLng = nextPhoto.longitude
                    }
                )
            }
        }

        // 4. Bottom Selected Waypoint Photo Card (or Expedition Trail Summary Bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            if (selectedPhoto != null) {
                val distMeters = GeoFormatUtils.haversineDistanceMeters(
                    telemetry.latitude,
                    telemetry.longitude,
                    selectedPhoto.latitude,
                    selectedPhoto.longitude
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SlateNavyCard.copy(alpha = 0.96f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SignalAmber, RoundedCornerShape(18.dp))
                        .testTag("map_selected_photo_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = File(selectedPhoto.filePath),
                            contentDescription = selectedPhoto.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onInspectPhoto(selectedPhoto) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedPhoto.category.uppercase(Locale.US)} • ${GeoFormatUtils.formatDistance(distMeters)} away",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SignalAmber
                                )
                                IconButton(
                                    onClick = { onSelectPhoto(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close selected waypoint card",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = selectedPhoto.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = GeoFormatUtils.formatDecimalDegrees(selectedPhoto.latitude, selectedPhoto.longitude),
                                style = MaterialTheme.typography.labelMedium,
                                color = TelemetryCyan
                            )
                            Text(
                                text = "ALT ${String.format(Locale.US, "%.1fm", selectedPhoto.altitudeMeters)} • PLUS ${selectedPhoto.plusCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SignalAmber,
                                    modifier = Modifier
                                        .clickable { onInspectPhoto(selectedPhoto) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("btn_inspect_from_map")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInFull,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Inspect",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SlateNavyDeep,
                                    modifier = Modifier
                                        .border(1.dp, TelemetryCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { onSharePhoto(selectedPhoto) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = TelemetryCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Share Stamp",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TelemetryCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Summary Bar when no specific pin is tapped
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SlateNavyDeep.copy(alpha = 0.90f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = SignalAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tap any #Pin on the GPS Map to inspect stamped photo & telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = SlateNavyDeep.copy(alpha = 0.92f),
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, tint.copy(alpha = 0.45f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
