package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StampTemplate(val label: String, val badge: String) {
    TACTICAL_HUD("Tactical HUD", "HUD"),
    SURVEYOR_PRO("Surveyor Pro", "SURVEY"),
    MINIMAL_GEO("Minimal Geo", "MINIMAL")
}

@Entity(tableName = "geo_photos")
data class GeoPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String,
    val category: String,
    val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val accuracyMeters: Float,
    val bearingDegrees: Float,
    val speedMps: Float,
    val addressLine: String,
    val plusCode: String,
    val utmCoord: String,
    val stampStyle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 0L
)
