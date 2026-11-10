package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GeoPhotoEntity
import com.example.data.StampTemplate
import com.example.location.LiveTelemetryState
import com.example.location.WaypointPreset
import com.example.ui.CameraHudConfig
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SatelliteEmerald
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SlateNavyDeep
import com.example.ui.theme.TelemetryCyan
import com.example.util.GeoFormatUtils
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CameraScreen(
    telemetry: LiveTelemetryState,
    cameraConfig: CameraHudConfig,
    latestPhoto: GeoPhotoEntity?,
    categories: List<String>,
    calibrationPresets: List<WaypointPreset>,
    onPermissionResult: (Boolean) -> Unit,
    onToggleLens: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLeveler: () -> Unit,
    onCycleTemplate: () -> Unit,
    onUpdateStampMeta: (String, String, String) -> Unit,
    onApplyPreset: (WaypointPreset) -> Unit,
    onApplyCustomGps: (Double, Double, Double, String) -> Unit,
    onResumeLiveGps: () -> Unit,
    onCapturePhoto: (Bitmap?) -> Unit,
    onImportPhotoUri: (android.net.Uri) -> Unit,
    onOpenLatestPhoto: (GeoPhotoEntity) -> Unit,
    onJumpToMap: () -> Unit,
    onClearBanner: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val camGranted = result[Manifest.permission.CAMERA] ?: hasCameraPerm
        hasCameraPerm = camGranted
        val locGranted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        onPermissionResult(locGranted)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportPhotoUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        val locGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (locGranted) {
            onPermissionResult(true)
        }

        if (!hasCameraPerm || !locGranted) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var cameraBoundSuccessfully by remember { mutableStateOf(false) }

    DisposableEffect(hasCameraPerm, cameraConfig.useBackCamera, cameraConfig.flashEnabled) {
        if (hasCameraPerm) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    imageCapture.flashMode = if (cameraConfig.flashEnabled) {
                        ImageCapture.FLASH_MODE_ON
                    } else {
                        ImageCapture.FLASH_MODE_OFF
                    }
                    val selector = if (cameraConfig.useBackCamera) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture
                    )
                    cameraBoundSuccessfully = true
                } catch (_: Exception) {
                    cameraBoundSuccessfully = false
                }
            }, executor)
        }
        onDispose { }
    }

    var showEditStampSheet by remember { mutableStateOf(false) }
    var showGpsCalibrateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SlateNavyDeep)) {
        // 1. Scenic Expedition Backdrop + Live CameraX Viewfinder
        Image(
            painter = painterResource(id = R.drawable.img_hero_expedition_1794244848753),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.72f
        )

        if (hasCameraPerm) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize().testTag("camera_preview_view")
            )
        }

        // 2. Tactical Rule-of-Thirds Grid & Horizon Leveler Canvas
        if (cameraConfig.showGridOverlay || cameraConfig.showHorizonLeveler) {
            ViewfinderHudCanvas(
                showGrid = cameraConfig.showGridOverlay,
                showLeveler = cameraConfig.showHorizonLeveler,
                rollDegrees = telemetry.rollDegrees
            )
        }

        // 3. Top Tactical Telemetry & Control Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SlateNavyDeep.copy(alpha = 0.88f),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (telemetry.isManualCalibrationOverride) SignalAmber else SatelliteEmerald,
                            RoundedCornerShape(50)
                        )
                        .clickable { showGpsCalibrateDialog = true }
                        .testTag("gps_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (telemetry.isManualCalibrationOverride) {
                                Icons.Default.EditLocationAlt
                            } else {
                                Icons.Default.SatelliteAlt
                            },
                            contentDescription = null,
                            tint = if (telemetry.isManualCalibrationOverride) SignalAmber else SatelliteEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (telemetry.isManualCalibrationOverride) {
                                "WAYPOINT LOCK • ±${String.format(Locale.US, "%.1fm", telemetry.accuracyMeters)}"
                            } else if (telemetry.hasLiveGpsFix) {
                                "GPS LIVE (${telemetry.satelliteCountEstimate} SAT) • ±${String.format(Locale.US, "%.1fm", telemetry.accuracyMeters)}"
                            } else {
                                "GPS STANDBY • OFFLINE READY"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                // Compass Bearing & Elevation Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SlateNavyDeep.copy(alpha = 0.88f),
                    modifier = Modifier.border(1.dp, TelemetryCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${GeoFormatUtils.cardinalDirection(telemetry.bearingDegrees)} • ${String.format(Locale.US, "%.0fm", telemetry.altitudeMeters)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelemetryCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Camera Utility Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HudIconButton(
                        icon = if (cameraConfig.flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        active = cameraConfig.flashEnabled,
                        contentDescription = stringResource(R.string.cd_flash_toggle),
                        testTag = "btn_flash_toggle",
                        onClick = onToggleFlash
                    )
                    HudIconButton(
                        icon = Icons.Default.GridOn,
                        active = cameraConfig.showGridOverlay,
                        contentDescription = "Toggle viewfinder grid",
                        testTag = "btn_grid_toggle",
                        onClick = onToggleGrid
                    )
                    HudIconButton(
                        icon = Icons.Default.Straighten,
                        active = cameraConfig.showHorizonLeveler,
                        contentDescription = "Toggle horizon leveler",
                        testTag = "btn_leveler_toggle",
                        onClick = onToggleLeveler
                    )
                    HudIconButton(
                        icon = Icons.Default.Cameraswitch,
                        active = !cameraConfig.useBackCamera,
                        contentDescription = stringResource(R.string.cd_switch_camera),
                        testTag = "btn_switch_camera",
                        onClick = onToggleLens
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = { showGpsCalibrateDialog = true },
                        label = {
                            Text(
                                "Calibrate GPS",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.cd_calibrate_gps),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SlateNavyDeep.copy(alpha = 0.85f),
                            labelColor = SignalAmber,
                            leadingIconContentColor = SignalAmber
                        ),
                        modifier = Modifier.testTag("btn_calibrate_gps")
                    )
                }
            }

            // Status notification toast inside HUD
            AnimatedVisibility(
                visible = cameraConfig.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                cameraConfig.statusBannerMessage?.let { msg ->
                    Surface(
                        color = SignalAmber,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clickable { onClearBanner() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "DISMISS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Live GPS Stamp Preview Card + Shutter Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Live Burn-In Stamp Preview Card (tappable to edit note/title or jump to map)
            LiveGpsStampHudPreview(
                telemetry = telemetry,
                config = cameraConfig,
                onEditNoteClick = { showEditStampSheet = true },
                onMiniRadarClick = onJumpToMap
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Camera Action Deck
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Latest Geotagged Thumbnail OR Import Photo Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (latestPhoto != null) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, SignalAmber, RoundedCornerShape(12.dp))
                                .clickable { onOpenLatestPhoto(latestPhoto) }
                                .testTag("latest_photo_thumbnail")
                        ) {
                            AsyncImage(
                                model = File(latestPhoto.filePath),
                                contentDescription = "Open latest geotagged photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = SlateNavyDeep.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("btn_import_geotag_photo")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = stringResource(R.string.cd_import_photo),
                                tint = TelemetryCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Center: Tactical Shutter Button (captures real CameraX frame + burns GPS Stamp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .border(4.dp, SignalAmber, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (cameraConfig.isCapturing) TelemetryCyan else SignalAmber)
                        .clickable(enabled = !cameraConfig.isCapturing) {
                            captureCameraFrameOrFallback(
                                context = context,
                                hasCameraPerm = hasCameraPerm,
                                cameraBound = cameraBoundSuccessfully,
                                imageCapture = imageCapture,
                                previewView = previewView,
                                onBitmapReady = onCapturePhoto
                            )
                        }
                        .testTag("shutter_capture_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = stringResource(R.string.cd_shutter_button),
                        tint = Color(0xFF09111E),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Right: Stamp Style Switcher + Note Editor Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SlateNavyDeep.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { showEditStampSheet = true }
                            .testTag("btn_edit_stamp_note")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Edit waypoint title and notes",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SlateNavyDeep.copy(alpha = 0.9f),
                        modifier = Modifier
                            .height(48.dp)
                            .border(1.dp, SignalAmber.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            .clickable { onCycleTemplate() }
                            .padding(horizontal = 12.dp)
                            .testTag("btn_cycle_stamp_template")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = stringResource(R.string.cd_stamp_style),
                                tint = SignalAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = cameraConfig.selectedTemplate.badge,
                                style = MaterialTheme.typography.labelMedium,
                                color = SignalAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Edit Stamp Metadata (Title, Category, Field Notes)
    if (showEditStampSheet) {
        EditStampMetadataDialog(
            initialTitle = cameraConfig.waypointTitle,
            initialNotes = cameraConfig.waypointNotes,
            initialCategory = cameraConfig.selectedCategory,
            categories = categories,
            onDismiss = { showEditStampSheet = false },
            onSave = { title, notes, category ->
                onUpdateStampMeta(title, notes, category)
                showEditStampSheet = false
            }
        )
    }

    // Dialog 2: Field GPS Waypoint Calibrator / Preset Selector
    if (showGpsCalibrateDialog) {
        GpsCalibrationDialog(
            telemetry = telemetry,
            presets = calibrationPresets,
            onDismiss = { showGpsCalibrateDialog = false },
            onSelectPreset = { preset ->
                onApplyPreset(preset)
                showGpsCalibrateDialog = false
            },
            onSaveCustom = { lat, lng, alt, label ->
                onApplyCustomGps(lat, lng, alt, label)
                showGpsCalibrateDialog = false
            },
            onUseLiveSensor = {
                onResumeLiveGps()
                showGpsCalibrateDialog = false
            }
        )
    }
}

@Composable
private fun LiveGpsStampHudPreview(
    telemetry: LiveTelemetryState,
    config: CameraHudConfig,
    onEditNoteClick: () -> Unit,
    onMiniRadarClick: () -> Unit
) {
    val plusCode = remember(telemetry.latitude, telemetry.longitude) {
        GeoFormatUtils.toPlusCode(telemetry.latitude, telemetry.longitude)
    }
    val utmCoord = remember(telemetry.latitude, telemetry.longitude) {
        GeoFormatUtils.toUtmString(telemetry.latitude, telemetry.longitude)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SlateNavyDeep.copy(alpha = 0.88f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SignalAmber.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .testTag("live_gps_stamp_preview")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive Mini GPS Map Radar Box (Tapping jumps to full GPS Map)
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101E30))
                    .border(1.dp, TelemetryCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onMiniRadarClick() }
                    .testTag("mini_radar_inset"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.40f
                    drawCircle(
                        color = TelemetryCyan.copy(alpha = 0.25f),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = TelemetryCyan.copy(alpha = 0.20f),
                        radius = r * 0.6f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawLine(
                        color = TelemetryCyan.copy(alpha = 0.25f),
                        start = Offset(8f, cy),
                        end = Offset(size.width - 8f, cy),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = TelemetryCyan.copy(alpha = 0.25f),
                        start = Offset(cx, 8f),
                        end = Offset(cx, size.height - 8f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Directional heading cone
                    val rad = Math.toRadians((telemetry.bearingDegrees - 90f).toDouble())
                    val spread = Math.toRadians(26.0)
                    val conePath = Path().apply {
                        moveTo(cx, cy)
                        lineTo(
                            (cx + r * cos(rad - spread)).toFloat(),
                            (cy + r * sin(rad - spread)).toFloat()
                        )
                        lineTo(
                            (cx + r * cos(rad + spread)).toFloat(),
                            (cy + r * sin(rad + spread)).toFloat()
                        )
                        close()
                    }
                    drawPath(conePath, color = SignalAmber.copy(alpha = 0.40f))
                    drawCircle(color = SignalAmber, radius = 5.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))
                }
                Text(
                    text = "MAP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetryCyan,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Live Telemetry Readout
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditNoteClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${config.selectedCategory.uppercase(Locale.US)}] • ${config.selectedTemplate.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalAmber
                    )
                    Text(
                        text = "PLUS: $plusCode",
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetryCyan
                    )
                }

                Text(
                    text = config.waypointTitle.ifBlank { "Tap to set waypoint title" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = GeoFormatUtils.formatDecimalDegrees(telemetry.latitude, telemetry.longitude),
                    style = MaterialTheme.typography.titleSmall,
                    color = TelemetryCyan,
                    fontFamily = JetBrainsMonoFontFamily
                )

                Text(
                    text = if (config.selectedTemplate == StampTemplate.SURVEYOR_PRO) {
                        "UTM: $utmCoord"
                    } else {
                        "DMS: ${GeoFormatUtils.formatDMS(telemetry.latitude, telemetry.longitude)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Text(
                    text = "${telemetry.addressLine} • ALT ${String.format(Locale.US, "%.1fm", telemetry.altitudeMeters)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ViewfinderHudCanvas(
    showGrid: Boolean,
    showLeveler: Boolean,
    rollDegrees: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (showGrid) {
            val gridColor = Color.White.copy(alpha = 0.22f)
            val stroke = 1.dp.toPx()
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), stroke)
            drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), stroke)
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), stroke)
            drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), stroke)
        }

        if (showLeveler) {
            val cx = w / 2f
            val cy = h * 0.44f
            val barHalf = w * 0.16f
            val isLevel = kotlin.math.abs(rollDegrees) < 2.0f
            val levelColor = if (isLevel) SatelliteEmerald else SignalAmber

            rotate(degrees = -rollDegrees.coerceIn(-45f, 45f), pivot = Offset(cx, cy)) {
                drawLine(
                    color = levelColor.copy(alpha = 0.85f),
                    start = Offset(cx - barHalf, cy),
                    end = Offset(cx - 18.dp.toPx(), cy),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawLine(
                    color = levelColor.copy(alpha = 0.85f),
                    start = Offset(cx + 18.dp.toPx(), cy),
                    end = Offset(cx + barHalf, cy),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
            drawCircle(
                color = levelColor,
                radius = 5.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun HudIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (active) SignalAmber.copy(alpha = 0.25f) else SlateNavyDeep.copy(alpha = 0.78f)
            )
            .border(
                1.dp,
                if (active) SignalAmber else Color.White.copy(alpha = 0.2f),
                CircleShape
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) SignalAmber else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EditStampMetadataDialog(
    initialTitle: String,
    initialNotes: String,
    initialCategory: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf(initialNotes) }
    var category by remember { mutableStateOf(initialCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Geotag Stamp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Site / Waypoint Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_stamp_title")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Field Survey Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("input_stamp_notes")
                )
                Text(
                    text = "Category Tag",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, notes, category) },
                modifier = Modifier.testTag("btn_save_stamp_meta")
            ) {
                Text("Apply to Stamp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GpsCalibrationDialog(
    telemetry: LiveTelemetryState,
    presets: List<WaypointPreset>,
    onDismiss: () -> Unit,
    onSelectPreset: (WaypointPreset) -> Unit,
    onSaveCustom: (Double, Double, Double, String) -> Unit,
    onUseLiveSensor: () -> Unit
) {
    var latInput by remember { mutableStateOf(String.format(Locale.US, "%.6f", telemetry.latitude)) }
    var lngInput by remember { mutableStateOf(String.format(Locale.US, "%.6f", telemetry.longitude)) }
    var altInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", telemetry.altitudeMeters)) }
    var labelInput by remember { mutableStateOf(telemetry.addressLine) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPS Telemetry & Waypoint Calibrator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select a real-world benchmark preset or enter exact survey coordinates:",
                    style = MaterialTheme.typography.bodySmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.take(4).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPreset(preset) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.4f, %.4f", preset.latitude, preset.longitude)} • ${preset.altitudeMeters}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = "LOCK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = altInput,
                        onValueChange = { altInput = it },
                        label = { Text("Alt (m)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Sector / Address") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }

                if (telemetry.isManualCalibrationOverride) {
                    OutlinedButton(
                        onClick = onUseLiveSensor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Resume Hardware GPS Sensor")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latInput.toDoubleOrNull() ?: telemetry.latitude
                    val lng = lngInput.toDoubleOrNull() ?: telemetry.longitude
                    val alt = altInput.toDoubleOrNull() ?: telemetry.altitudeMeters
                    onSaveCustom(lat, lng, alt, labelInput)
                }
            ) {
                Text("Lock Coordinates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun captureCameraFrameOrFallback(
    context: Context,
    hasCameraPerm: Boolean,
    cameraBound: Boolean,
    imageCapture: ImageCapture,
    previewView: PreviewView,
    onBitmapReady: (Bitmap?) -> Unit
) {
    // Fast path: if PreviewView already has a rendered bitmap frame, grab it or use ImageCapture
    val previewBmp = try {
        previewView.bitmap
    } catch (_: Exception) {
        null
    }

    if (!hasCameraPerm || !cameraBound) {
        onBitmapReady(previewBmp)
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bmp = try {
                    val rawBmp = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
                    } else {
                        rawBmp
                    }
                } catch (_: Exception) {
                    previewBmp
                } finally {
                    image.close()
                }
                onBitmapReady(bmp ?: previewBmp)
            }

            override fun onError(exception: ImageCaptureException) {
                onBitmapReady(previewBmp)
            }
        }
    )
}
