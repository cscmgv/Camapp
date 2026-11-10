package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoPhotoDao {
    @Query("SELECT * FROM geo_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<GeoPhotoEntity>>

    @Query("SELECT * FROM geo_photos ORDER BY timestamp ASC")
    suspend fun getAllPhotosSync(): List<GeoPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: GeoPhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: GeoPhotoEntity)

    @Query("DELETE FROM geo_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)

    @Query("SELECT COUNT(*) FROM geo_photos")
    suspend fun getPhotoCount(): Int

    @Query("SELECT * FROM offline_map_regions ORDER BY downloadedAt DESC")
    fun getAllOfflineRegions(): Flow<List<OfflineMapRegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineRegion(region: OfflineMapRegionEntity): Long

    @Query("DELETE FROM offline_map_regions WHERE id = :id")
    suspend fun deleteOfflineRegionById(id: Long)

    @Query("SELECT COUNT(*) FROM offline_map_regions")
    suspend fun getOfflineRegionCount(): Int
}
