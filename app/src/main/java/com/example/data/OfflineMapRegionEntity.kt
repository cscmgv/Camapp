package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_map_regions")
data class OfflineMapRegionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val regionName: String,
    val centerLat: Double,
    val centerLng: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val cachedTilesCount: Int,
    val storageBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)
