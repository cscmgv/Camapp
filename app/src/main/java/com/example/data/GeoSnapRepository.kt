package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import com.example.R
import com.example.camera.GpsStampData
import com.example.camera.GpsStampRenderer
import com.example.map.OfflineTileManager
import com.example.util.GeoFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class GeoSnapRepository(
    private val context: Context,
    private val dao: GeoPhotoDao,
    val tileManager: OfflineTileManager
) {
    val allPhotos: Flow<List<GeoPhotoEntity>> = dao.getAllPhotos()
    val allOfflineRegions: Flow<List<OfflineMapRegionEntity>> = dao.getAllOfflineRegions()

    private val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
    private val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }

    /**
     * Seeds initial field expedition geotagged photos with burned-in GPS stamps and EXIF
     * on first app launch so the offline map, trail, and gallery are immediately rich.
     */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.getPhotoCount() == 0) {
            val now = System.currentTimeMillis()
            val sample1Bmp = decodeDrawableOrFallback(R.drawable.img_sample_geotag_1_1794244850846)
            val stamp1 = GpsStampData(
                title = "Coastal Headland Lighthouse",
                notes = "Baseline coastal erosion & tide benchmark survey at golden hour.",
                category = "Field Survey",
                latitude = 13.0397,
                longitude = 80.2794,
                altitudeMeters = 16.5,
                accuracyMeters = 3.4f,
                bearingDegrees = 78f,
                speedMps = 0.0f,
                addressLine = "Marina Coastal Sector, Chennai, IN",
                timestamp = now - 7_200_000L,
                template = StampTemplate.TACTICAL_HUD
            )
            val file1 = File(photosDir, "geosnap_seed_1.jpg")
            val size1 = GpsStampRenderer.saveStampedPhotoToFile(sample1Bmp, stamp1, file1)
            dao.insertPhoto(
                GeoPhotoEntity(
                    title = stamp1.title,
                    notes = stamp1.notes,
                    category = stamp1.category,
                    filePath = file1.absolutePath,
                    latitude = stamp1.latitude,
                    longitude = stamp1.longitude,
                    altitudeMeters = stamp1.altitudeMeters,
                    accuracyMeters = stamp1.accuracyMeters,
                    bearingDegrees = stamp1.bearingDegrees,
                    speedMps = stamp1.speedMps,
                    addressLine = stamp1.addressLine,
                    plusCode = GeoFormatUtils.toPlusCode(stamp1.latitude, stamp1.longitude),
                    utmCoord = GeoFormatUtils.toUtmString(stamp1.latitude, stamp1.longitude),
                    stampStyle = stamp1.template.name,
                    timestamp = stamp1.timestamp,
                    isFavorite = true,
                    fileSizeBytes = size1
                )
            )

            val sample2Bmp = decodeDrawableOrFallback(R.drawable.img_sample_geotag_2_1794244850514)
            val stamp2 = GpsStampData(
                title = "Highland Heritage Sanctuary",
                notes = "Waypoint #02 along eastern ridge trail. Clear GPS satellite lock.",
                category = "Trail Waypoint",
                latitude = 13.0584,
                longitude = 80.2682,
                altitudeMeters = 42.8,
                accuracyMeters = 2.9f,
                bearingDegrees = 315f,
                speedMps = 1.2f,
                addressLine = "Heritage Ridge Reserve, Chennai, IN",
                timestamp = now - 3_600_000L,
                template = StampTemplate.SURVEYOR_PRO
            )
            val file2 = File(photosDir, "geosnap_seed_2.jpg")
            val size2 = GpsStampRenderer.saveStampedPhotoToFile(sample2Bmp, stamp2, file2)
            dao.insertPhoto(
                GeoPhotoEntity(
                    title = stamp2.title,
                    notes = stamp2.notes,
                    category = stamp2.category,
                    filePath = file2.absolutePath,
                    latitude = stamp2.latitude,
                    longitude = stamp2.longitude,
                    altitudeMeters = stamp2.altitudeMeters,
                    accuracyMeters = stamp2.accuracyMeters,
                    bearingDegrees = stamp2.bearingDegrees,
                    speedMps = stamp2.speedMps,
                    addressLine = stamp2.addressLine,
                    plusCode = GeoFormatUtils.toPlusCode(stamp2.latitude, stamp2.longitude),
                    utmCoord = GeoFormatUtils.toUtmString(stamp2.latitude, stamp2.longitude),
                    stampStyle = stamp2.template.name,
                    timestamp = stamp2.timestamp,
                    isFavorite = false,
                    fileSizeBytes = size2
                )
            )
        }

        if (dao.getOfflineRegionCount() == 0) {
            dao.insertOfflineRegion(
                OfflineMapRegionEntity(
                    regionName = "Chennai Marina & Coastal Corridor",
                    centerLat = 13.0500,
                    centerLng = 80.2824,
                    minZoom = 12,
                    maxZoom = 15,
                    cachedTilesCount = 27,
                    storageBytes = 412_800L
                )
            )
        }
    }

    /**
     * Stamps a captured or imported Bitmap with real-time GPS metadata, writes EXIF,
     * saves to internal offline storage, and persists in Room.
     */
    suspend fun saveCapturedGeotagPhoto(
        sourceBitmap: Bitmap,
        stampData: GpsStampData
    ): GeoPhotoEntity = withContext(Dispatchers.IO) {
        val fileName = "GEOSNAP_${System.currentTimeMillis()}.jpg"
        val outFile = File(photosDir, fileName)
        val fileSize = GpsStampRenderer.saveStampedPhotoToFile(sourceBitmap, stampData, outFile)

        val entity = GeoPhotoEntity(
            title = stampData.title.ifBlank { "Waypoint @ ${GeoFormatUtils.toPlusCode(stampData.latitude, stampData.longitude)}" },
            notes = stampData.notes,
            category = stampData.category,
            filePath = outFile.absolutePath,
            latitude = stampData.latitude,
            longitude = stampData.longitude,
            altitudeMeters = stampData.altitudeMeters,
            accuracyMeters = stampData.accuracyMeters,
            bearingDegrees = stampData.bearingDegrees,
            speedMps = stampData.speedMps,
            addressLine = stampData.addressLine,
            plusCode = GeoFormatUtils.toPlusCode(stampData.latitude, stampData.longitude),
            utmCoord = GeoFormatUtils.toUtmString(stampData.latitude, stampData.longitude),
            stampStyle = stampData.template.name,
            timestamp = stampData.timestamp,
            isFavorite = false,
            fileSizeBytes = fileSize
        )
        val newId = dao.insertPhoto(entity)
        entity.copy(id = newId)
    }

    /**
     * Decodes a photo selected from Android Photo Picker and stamps it with GPS data.
     */
    suspend fun importAndGeotagFromUri(
        uri: Uri,
        stampData: GpsStampData
    ): GeoPhotoEntity? = withContext(Dispatchers.IO) {
        val bmp = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext null
        saveCapturedGeotagPhoto(bmp, stampData)
    }

    /**
     * Generates a high-resolution scenic field frame when capturing on headless/cloud emulators
     * where physical CameraX hardware might not return a sensor frame.
     */
    fun createFallbackScenicFrame(): Bitmap {
        return decodeDrawableOrFallback(R.drawable.img_hero_expedition_1794244848753)
    }

    private fun decodeDrawableOrFallback(resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId) ?: run {
            val bmp = Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 1280f, 960f,
                    Color.rgb(13, 27, 42),
                    Color.rgb(27, 58, 75),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, 1280f, 960f, paint)
            bmp
        }
    }

    suspend fun toggleFavorite(photo: GeoPhotoEntity) = withContext(Dispatchers.IO) {
        dao.updatePhoto(photo.copy(isFavorite = !photo.isFavorite))
    }

    suspend fun updatePhotoMetadata(
        photo: GeoPhotoEntity,
        newTitle: String,
        newNotes: String,
        newCategory: String
    ) = withContext(Dispatchers.IO) {
        dao.updatePhoto(
            photo.copy(
                title = newTitle.ifBlank { photo.title },
                notes = newNotes,
                category = newCategory
            )
        )
    }

    suspend fun deletePhoto(photo: GeoPhotoEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(photo.filePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
        dao.deletePhotoById(photo.id)
    }

    suspend fun downloadOfflineMapPack(
        regionName: String,
        centerLat: Double,
        centerLng: Double,
        onProgress: (Float) -> Unit
    ): OfflineMapRegionEntity = withContext(Dispatchers.IO) {
        val (tileCount, bytes) = tileManager.downloadRegionTiles(
            centerLat = centerLat,
            centerLng = centerLng,
            zoomLevels = 12..14,
            onProgress = onProgress
        )
        val entity = OfflineMapRegionEntity(
            regionName = regionName,
            centerLat = centerLat,
            centerLng = centerLng,
            minZoom = 12,
            maxZoom = 14,
            cachedTilesCount = tileCount,
            storageBytes = bytes
        )
        val id = dao.insertOfflineRegion(entity)
        entity.copy(id = id)
    }

    suspend fun deleteOfflineRegion(regionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteOfflineRegionById(regionId)
    }

    /**
     * Exports all offline geotagged waypoints into a standard GPX 1.1 XML file for sharing/GIS import.
     */
    suspend fun exportGpxFile(): File = withContext(Dispatchers.IO) {
        val photos = dao.getAllPhotosSync()
        val gpxFile = File(exportsDir, "geosnap_field_waypoints_${System.currentTimeMillis()}.gpx")
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="GeoSnap Cam Offline Survey" xmlns="http://www.topografix.com/GPX/1/1">""")
        photos.forEach { p ->
            sb.appendLine(
                String.format(
                    Locale.US,
                    """  <wpt lat="%.6f" lon="%.6f"><ele>%.1f</ele><name>%s</name><desc>%s (%s)</desc></wpt>""",
                    p.latitude,
                    p.longitude,
                    p.altitudeMeters,
                    escapeXml(p.title),
                    escapeXml(p.notes.ifBlank { p.category }),
                    escapeXml(p.plusCode)
                )
            )
        }
        sb.appendLine("""  <trk><name>GeoSnap Expedition Track</name><trkseg>""")
        photos.forEach { p ->
            sb.appendLine(
                String.format(
                    Locale.US,
                    """    <trkpt lat="%.6f" lon="%.6f"><ele>%.1f</ele></trkpt>""",
                    p.latitude,
                    p.longitude,
                    p.altitudeMeters
                )
            )
        }
        sb.appendLine("""  </trkseg></trk>""")
        sb.appendLine("""</gpx>""")
        gpxFile.writeText(sb.toString())
        gpxFile
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
