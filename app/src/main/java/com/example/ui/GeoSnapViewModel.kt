package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.GpsStampData
import com.example.data.AppDatabase
import com.example.data.GeoPhotoEntity
import com.example.data.GeoSnapRepository
import com.example.data.OfflineMapRegionEntity
import com.example.data.StampTemplate
import com.example.location.LiveTelemetryState
import com.example.location.LocationTelemetryManager
import com.example.location.WaypointPreset
import com.example.map.OfflineTileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppDestination {
    CAMERA,
    MAP,
    GALLERY,
    OFFLINE_VAULT
}

enum class MapStyleMode(val label: String) {
    HYBRID_OSM("OSM + Grid"),
    TACTICAL_RADAR("Tactical Radar"),
    TOPO_CONTOUR("Topo Contour")
}

data class CameraHudConfig(
    val selectedTemplate: StampTemplate = StampTemplate.TACTICAL_HUD,
    val waypointTitle: String = "Coastal Field Survey",
    val waypointNotes: String = "Verified GPS benchmark & visual inspection",
    val selectedCategory: String = "Field Survey",
    val showGridOverlay: Boolean = true,
    val showHorizonLeveler: Boolean = true,
    val useBackCamera: Boolean = true,
    val flashEnabled: Boolean = false,
    val isCapturing: Boolean = false,
    val statusBannerMessage: String? = null
)

class GeoSnapViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val tileManager = OfflineTileManager(application)
    private val repository = GeoSnapRepository(application, database.geoPhotoDao(), tileManager)
    val locationManager = LocationTelemetryManager(application, viewModelScope)

    val categories = listOf(
        "Field Survey",
        "Trail Waypoint",
        "Site Inspection",
        "Landmark",
        "Hazard"
    )

    private val _currentDestination = MutableStateFlow(AppDestination.CAMERA)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _cameraConfig = MutableStateFlow(CameraHudConfig())
    val cameraConfig: StateFlow<CameraHudConfig> = _cameraConfig.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _inspectedPhoto = MutableStateFlow<GeoPhotoEntity?>(null)
    val inspectedPhoto: StateFlow<GeoPhotoEntity?> = _inspectedPhoto.asStateFlow()

    private val _selectedMapPhoto = MutableStateFlow<GeoPhotoEntity?>(null)
    val selectedMapPhoto: StateFlow<GeoPhotoEntity?> = _selectedMapPhoto.asStateFlow()

    private val _mapCenterTarget = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapCenterTarget: StateFlow<Pair<Double, Double>?> = _mapCenterTarget.asStateFlow()

    private val _mapStyleMode = MutableStateFlow(MapStyleMode.HYBRID_OSM)
    val mapStyleMode: StateFlow<MapStyleMode> = _mapStyleMode.asStateFlow()

    private val _forceOfflineMapMode = MutableStateFlow(false)
    val forceOfflineMapMode: StateFlow<Boolean> = _forceOfflineMapMode.asStateFlow()

    private val _isDownloadingMapPack = MutableStateFlow(false)
    val isDownloadingMapPack: StateFlow<Boolean> = _isDownloadingMapPack.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _darkThemeEnabled = MutableStateFlow(true)
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    val telemetry: StateFlow<LiveTelemetryState> = locationManager.telemetry

    val allPhotos: StateFlow<List<GeoPhotoEntity>> = repository.allPhotos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredPhotos: StateFlow<List<GeoPhotoEntity>> = combine(
        allPhotos,
        _selectedCategoryFilter,
        _searchQuery,
        _favoritesOnly
    ) { photos, category, query, favOnly ->
        photos.filter { photo ->
            val matchesCat = category == "All" || photo.category.equals(category, ignoreCase = true)
            val matchesFav = !favOnly || photo.isFavorite
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                photo.title.lowercase().contains(q) ||
                photo.notes.lowercase().contains(q) ||
                photo.addressLine.lowercase().contains(q) ||
                photo.plusCode.lowercase().contains(q) ||
                photo.category.lowercase().contains(q)
            matchesCat && matchesFav && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val offlineRegions: StateFlow<List<OfflineMapRegionEntity>> = repository.allOfflineRegions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun navigateTo(destination: AppDestination) {
        locationManager.refreshConnectivityStatus()
        _currentDestination.value = destination
    }

    fun toggleDarkTheme() {
        _darkThemeEnabled.update { !it }
    }

    fun updateLocationPermission(granted: Boolean) {
        locationManager.updatePermissionState(granted)
    }

    fun cycleStampTemplate() {
        val values = StampTemplate.entries
        val nextIdx = (_cameraConfig.value.selectedTemplate.ordinal + 1) % values.size
        _cameraConfig.update { it.copy(selectedTemplate = values[nextIdx]) }
    }

    fun updateStampConfig(title: String, notes: String, category: String) {
        _cameraConfig.update {
            it.copy(
                waypointTitle = title,
                waypointNotes = notes,
                selectedCategory = category
            )
        }
    }

    fun toggleGridOverlay() {
        _cameraConfig.update { it.copy(showGridOverlay = !it.showGridOverlay) }
    }

    fun toggleHorizonLeveler() {
        _cameraConfig.update { it.copy(showHorizonLeveler = !it.showHorizonLeveler) }
    }

    fun toggleCameraLens() {
        _cameraConfig.update { it.copy(useBackCamera = !it.useBackCamera) }
    }

    fun toggleFlash() {
        _cameraConfig.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun clearBannerMessage() {
        _cameraConfig.update { it.copy(statusBannerMessage = null) }
    }

    fun applyCalibrationPreset(preset: WaypointPreset) {
        locationManager.applyCalibrationWaypoint(
            latitude = preset.latitude,
            longitude = preset.longitude,
            altitudeMeters = preset.altitudeMeters,
            customLabel = preset.regionLabel
        )
        _cameraConfig.update {
            it.copy(
                waypointTitle = preset.name,
                statusBannerMessage = "Calibrated GPS to ${preset.name}"
            )
        }
    }

    fun applyCustomCoordinates(lat: Double, lng: Double, alt: Double, label: String) {
        locationManager.applyCalibrationWaypoint(
            latitude = lat.coerceIn(-85.0, 85.0),
            longitude = lng.coerceIn(-180.0, 180.0),
            altitudeMeters = alt,
            customLabel = label.ifBlank { null }
        )
        _cameraConfig.update {
            it.copy(statusBannerMessage = "Custom GPS coordinates locked")
        }
    }

    fun resumeLiveHardwareGps() {
        locationManager.resumeLiveGpsSensor()
        _cameraConfig.update {
            it.copy(statusBannerMessage = "Switched to live hardware GPS sensor")
        }
    }

    fun captureAndSaveGeotaggedPhoto(capturedBitmap: Bitmap?) {
        if (_cameraConfig.value.isCapturing) return
        _cameraConfig.update { it.copy(isCapturing = true) }

        viewModelScope.launch {
            try {
                val bmp = capturedBitmap ?: repository.createFallbackScenicFrame()
                val stampData = buildCurrentStampData()
                val savedEntity = repository.saveCapturedGeotagPhoto(bmp, stampData)
                _selectedMapPhoto.value = savedEntity
                _cameraConfig.update {
                    it.copy(
                        isCapturing = false,
                        statusBannerMessage = "Saved Geotag Photo • ${savedEntity.plusCode} (Offline Vault)"
                    )
                }
            } catch (e: Exception) {
                _cameraConfig.update {
                    it.copy(
                        isCapturing = false,
                        statusBannerMessage = "Capture failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun importAndGeotagPhotoUri(uri: Uri) {
        _cameraConfig.update { it.copy(isCapturing = true) }
        viewModelScope.launch {
            val stampData = buildCurrentStampData()
            val saved = repository.importAndGeotagFromUri(uri, stampData)
            _cameraConfig.update {
                it.copy(
                    isCapturing = false,
                    statusBannerMessage = if (saved != null) {
                        "Geotagged imported photo @ ${saved.plusCode}"
                    } else {
                        "Could not read selected image"
                    }
                )
            }
            if (saved != null) {
                _selectedMapPhoto.value = saved
            }
        }
    }

    private fun buildCurrentStampData(): GpsStampData {
        val loc = telemetry.value
        val cfg = _cameraConfig.value
        return GpsStampData(
            title = cfg.waypointTitle.ifBlank { "Field Waypoint" },
            notes = cfg.waypointNotes,
            category = cfg.selectedCategory,
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitudeMeters = loc.altitudeMeters,
            accuracyMeters = loc.accuracyMeters,
            bearingDegrees = loc.bearingDegrees,
            speedMps = loc.speedMps,
            addressLine = loc.addressLine,
            timestamp = System.currentTimeMillis(),
            template = cfg.selectedTemplate
        )
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesOnly() {
        _favoritesOnly.update { !it }
    }

    fun inspectPhoto(photo: GeoPhotoEntity?) {
        _inspectedPhoto.value = photo
    }

    fun selectPhotoOnMap(photo: GeoPhotoEntity?) {
        _selectedMapPhoto.value = photo
        if (photo != null) {
            _mapCenterTarget.value = Pair(photo.latitude, photo.longitude)
        }
    }

    fun focusPhotoOnMapAndNavigate(photo: GeoPhotoEntity) {
        _inspectedPhoto.value = null
        _selectedMapPhoto.value = photo
        _mapCenterTarget.value = Pair(photo.latitude, photo.longitude)
        _currentDestination.value = AppDestination.MAP
    }

    fun clearMapCenterTarget() {
        _mapCenterTarget.value = null
    }

    fun setMapStyleMode(mode: MapStyleMode) {
        _mapStyleMode.value = mode
    }

    fun toggleForceOfflineMapMode() {
        _forceOfflineMapMode.update { !it }
    }

    fun toggleFavorite(photo: GeoPhotoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(photo)
            if (_inspectedPhoto.value?.id == photo.id) {
                _inspectedPhoto.value = photo.copy(isFavorite = !photo.isFavorite)
            }
        }
    }

    fun updatePhotoDetails(photo: GeoPhotoEntity, title: String, notes: String, category: String) {
        viewModelScope.launch {
            repository.updatePhotoMetadata(photo, title, notes, category)
            if (_inspectedPhoto.value?.id == photo.id) {
                _inspectedPhoto.value = photo.copy(
                    title = title.ifBlank { photo.title },
                    notes = notes,
                    category = category
                )
            }
        }
    }

    fun deletePhoto(photo: GeoPhotoEntity) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            if (_inspectedPhoto.value?.id == photo.id) {
                _inspectedPhoto.value = null
            }
            if (_selectedMapPhoto.value?.id == photo.id) {
                _selectedMapPhoto.value = null
            }
        }
    }

    fun downloadOfflineRegionForCurrentLocation(regionName: String, lat: Double, lng: Double) {
        if (_isDownloadingMapPack.value) return
        _isDownloadingMapPack.value = true
        _downloadProgress.value = 0.05f
        viewModelScope.launch {
            repository.downloadOfflineMapPack(
                regionName = regionName.ifBlank { "Field Sector (${String.format("%.2f, %.2f", lat, lng)})" },
                centerLat = lat,
                centerLng = lng,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )
            _isDownloadingMapPack.value = false
            _downloadProgress.value = 0f
        }
    }

    fun deleteOfflineRegion(regionId: Long) {
        viewModelScope.launch {
            repository.deleteOfflineRegion(regionId)
        }
    }

    fun shareGeotaggedPhoto(context: Context, photo: GeoPhotoEntity) {
        try {
            val file = File(photo.filePath)
            if (!file.exists()) return
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareText = buildString {
                appendLine("📍 ${photo.title} [${photo.category}]")
                appendLine("🌐 GPS: ${photo.latitude}, ${photo.longitude} (±${photo.accuracyMeters}m)")
                appendLine("⛰️ Altitude: ${photo.altitudeMeters}m | Heading: ${photo.bearingDegrees.toInt()}°")
                appendLine("🗺️ Plus Code: ${photo.plusCode} | UTM: ${photo.utmCoord}")
                appendLine("📌 Location: ${photo.addressLine}")
                if (photo.notes.isNotBlank()) appendLine("📝 Note: ${photo.notes}")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Geotagged Photo"))
        } catch (_: Exception) {
        }
    }

    fun exportAndShareGpxTrack(context: Context) {
        viewModelScope.launch {
            try {
                val gpxFile = repository.exportGpxFile()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    gpxFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/gpx+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "GeoSnap Expedition GPX Waypoints")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export Offline GPX Track"))
            } catch (_: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.stopUpdates()
    }
}
