package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.GeoSnapViewModel
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.GpsMapScreen
import com.example.ui.screens.OfflineVaultScreen
import com.example.ui.theme.MyApplicationTheme

private data class NavTabItem(
    val destination: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GeoSnapViewModel = viewModel()
            val darkTheme by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = darkTheme) {
                GeoSnapMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GeoSnapMainApp(viewModel: GeoSnapViewModel) {
    val context = LocalContext.current
    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val cameraConfig by viewModel.cameraConfig.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allPhotos.collectAsStateWithLifecycle()
    val filteredPhotos by viewModel.filteredPhotos.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val inspectedPhoto by viewModel.inspectedPhoto.collectAsStateWithLifecycle()
    val selectedMapPhoto by viewModel.selectedMapPhoto.collectAsStateWithLifecycle()
    val mapCenterTarget by viewModel.mapCenterTarget.collectAsStateWithLifecycle()
    val mapStyleMode by viewModel.mapStyleMode.collectAsStateWithLifecycle()
    val forceOfflineMapMode by viewModel.forceOfflineMapMode.collectAsStateWithLifecycle()
    val offlineRegions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val isDownloadingPack by viewModel.isDownloadingMapPack.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()

    if (currentDest != AppDestination.CAMERA) {
        BackHandler {
            viewModel.navigateTo(AppDestination.CAMERA)
        }
    }

    val navItems = listOf(
        NavTabItem(
            destination = AppDestination.CAMERA,
            labelRes = R.string.nav_camera,
            selectedIcon = Icons.Filled.CameraAlt,
            unselectedIcon = Icons.Outlined.CameraAlt,
            testTag = "nav_tab_camera"
        ),
        NavTabItem(
            destination = AppDestination.MAP,
            labelRes = R.string.nav_map,
            selectedIcon = Icons.Filled.Map,
            unselectedIcon = Icons.Outlined.Map,
            testTag = "nav_tab_map"
        ),
        NavTabItem(
            destination = AppDestination.GALLERY,
            labelRes = R.string.nav_gallery,
            selectedIcon = Icons.Filled.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
            testTag = "nav_tab_gallery"
        ),
        NavTabItem(
            destination = AppDestination.OFFLINE_VAULT,
            labelRes = R.string.nav_offline,
            selectedIcon = Icons.Filled.OfflinePin,
            unselectedIcon = Icons.Outlined.OfflinePin,
            testTag = "nav_tab_offline"
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDest == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    if (item.destination == AppDestination.GALLERY && allPhotos.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(allPhotos.size.toString()) }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDest == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentDest) {
                        AppDestination.CAMERA -> {
                            CameraScreen(
                                telemetry = telemetry,
                                cameraConfig = cameraConfig,
                                latestPhoto = allPhotos.firstOrNull(),
                                categories = viewModel.categories,
                                calibrationPresets = viewModel.locationManager.calibrationPresets,
                                onPermissionResult = viewModel::updateLocationPermission,
                                onToggleLens = viewModel::toggleCameraLens,
                                onToggleFlash = viewModel::toggleFlash,
                                onToggleGrid = viewModel::toggleGridOverlay,
                                onToggleLeveler = viewModel::toggleHorizonLeveler,
                                onCycleTemplate = viewModel::cycleStampTemplate,
                                onUpdateStampMeta = viewModel::updateStampConfig,
                                onApplyPreset = viewModel::applyCalibrationPreset,
                                onApplyCustomGps = viewModel::applyCustomCoordinates,
                                onResumeLiveGps = viewModel::resumeLiveHardwareGps,
                                onCapturePhoto = viewModel::captureAndSaveGeotaggedPhoto,
                                onImportPhotoUri = viewModel::importAndGeotagPhotoUri,
                                onOpenLatestPhoto = { photo ->
                                    viewModel.inspectPhoto(photo)
                                    viewModel.navigateTo(AppDestination.GALLERY)
                                },
                                onJumpToMap = { viewModel.navigateTo(AppDestination.MAP) },
                                onClearBanner = viewModel::clearBannerMessage
                            )
                        }

                        AppDestination.MAP -> {
                            GpsMapScreen(
                                telemetry = telemetry,
                                photos = allPhotos,
                                selectedPhoto = selectedMapPhoto ?: allPhotos.firstOrNull(),
                                mapCenterTarget = mapCenterTarget,
                                mapStyleMode = mapStyleMode,
                                forceOfflineMode = forceOfflineMapMode,
                                tileManager = viewModel.tileManager,
                                onSelectPhoto = viewModel::selectPhotoOnMap,
                                onInspectPhoto = { photo ->
                                    viewModel.inspectPhoto(photo)
                                    viewModel.navigateTo(AppDestination.GALLERY)
                                },
                                onSharePhoto = { photo ->
                                    viewModel.shareGeotaggedPhoto(context, photo)
                                },
                                onClearMapTarget = viewModel::clearMapCenterTarget,
                                onSetMapStyle = viewModel::setMapStyleMode,
                                onToggleOfflineLock = viewModel::toggleForceOfflineMapMode,
                                onJumpToCameraWithCoords = { lat, lng ->
                                    viewModel.applyCustomCoordinates(
                                        lat = lat,
                                        lng = lng,
                                        alt = telemetry.altitudeMeters,
                                        label = ""
                                    )
                                    viewModel.navigateTo(AppDestination.CAMERA)
                                }
                            )
                        }

                        AppDestination.GALLERY -> {
                            GalleryScreen(
                                allPhotos = allPhotos,
                                filteredPhotos = filteredPhotos,
                                categories = viewModel.categories,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                favoritesOnly = favoritesOnly,
                                inspectedPhoto = inspectedPhoto,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategoryChange = viewModel::setCategoryFilter,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                onInspectPhoto = viewModel::inspectPhoto,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onUpdatePhotoMeta = viewModel::updatePhotoDetails,
                                onDeletePhoto = viewModel::deletePhoto,
                                onSharePhoto = { photo ->
                                    viewModel.shareGeotaggedPhoto(context, photo)
                                },
                                onFocusOnMap = viewModel::focusPhotoOnMapAndNavigate,
                                onExportGpx = { viewModel.exportAndShareGpxTrack(context) },
                                onSwitchToCamera = { viewModel.navigateTo(AppDestination.CAMERA) }
                            )
                        }

                        AppDestination.OFFLINE_VAULT -> {
                            OfflineVaultScreen(
                                telemetry = telemetry,
                                photos = allPhotos,
                                offlineRegions = offlineRegions,
                                presets = viewModel.locationManager.calibrationPresets,
                                isDownloading = isDownloadingPack,
                                downloadProgress = downloadProgress,
                                darkThemeEnabled = darkThemeEnabled,
                                onToggleTheme = viewModel::toggleDarkTheme,
                                onDownloadRegion = viewModel::downloadOfflineRegionForCurrentLocation,
                                onDeleteRegion = viewModel::deleteOfflineRegion,
                                onJumpToRegionOnMap = { lat, lng ->
                                    viewModel.selectPhotoOnMap(null)
                                    viewModel.applyCustomCoordinates(lat, lng, telemetry.altitudeMeters, "")
                                    viewModel.navigateTo(AppDestination.MAP)
                                },
                                onExportGpxTrack = { viewModel.exportAndShareGpxTrack(context) }
                            )
                        }
                    }
                }
            }
        }
    }
}
