package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object GeoFormatUtils {

    private const val PLUS_CODE_ALPHABET = "23456789CFGHJMPQRVWX"

    fun formatDecimalDegrees(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.6f° %s, %.6f° %s", abs(lat), latDir, abs(lng), lngDir)
    }

    fun formatDMS(lat: Double, lng: Double): String {
        return "${toDMS(lat, isLat = true)}  ${toDMS(lng, isLat = false)}"
    }

    private fun toDMS(coord: Double, isLat: Boolean): String {
        val dir = if (isLat) {
            if (coord >= 0) "N" else "S"
        } else {
            if (coord >= 0) "E" else "W"
        }
        val absCoord = abs(coord)
        val degrees = floor(absCoord).toInt()
        val minutesFull = (absCoord - degrees) * 60.0
        val minutes = floor(minutesFull).toInt()
        val seconds = (minutesFull - minutes) * 60.0
        return String.format(Locale.US, "%02d°%02d'%04.1f\"%s", degrees, minutes, seconds, dir)
    }

    /**
     * Offline Open Location Code (Plus Code) 10-character encoder.
     */
    fun toPlusCode(lat: Double, lng: Double): String {
        var adjLat = (lat + 90.0).coerceIn(0.0, 179.999999)
        var adjLng = (lng + 180.0).coerceIn(0.0, 359.999999)

        val sb = StringBuilder()
        var latPlaceVal = 20.0
        var lngPlaceVal = 20.0

        for (i in 0 until 5) {
            val latDigit = floor(adjLat / latPlaceVal).toInt().coerceIn(0, 19)
            val lngDigit = floor(adjLng / lngPlaceVal).toInt().coerceIn(0, 19)
            adjLat -= latDigit * latPlaceVal
            adjLng -= lngDigit * lngPlaceVal
            sb.append(PLUS_CODE_ALPHABET[latDigit])
            sb.append(PLUS_CODE_ALPHABET[lngDigit])
            if (i == 3) sb.append('+')
            latPlaceVal /= 20.0
            lngPlaceVal /= 20.0
        }
        return sb.toString()
    }

    /**
     * Offline simplified UTM Grid Zone + Easting/Northing approximation for field surveys.
     */
    fun toUtmString(lat: Double, lng: Double): String {
        val clampedLat = lat.coerceIn(-80.0, 84.0)
        val zoneNumber = (floor((lng + 180.0) / 6.0).toInt() + 1).coerceIn(1, 60)
        val zoneLetters = "CDEFGHJKLMNPQRSTUVWXX"
        val letterIdx = floor((clampedLat + 80.0) / 8.0).toInt().coerceIn(0, zoneLetters.length - 1)
        val zoneLetter = zoneLetters[letterIdx]

        val latRad = Math.toRadians(clampedLat)
        val lngRad = Math.toRadians(lng)
        val centralMeridianRad = Math.toRadians((zoneNumber - 1) * 6.0 - 180.0 + 3.0)

        val a = 6378137.0
        val k0 = 0.9996
        val eSq = 0.00669438
        val n = a / sqrt(1.0 - eSq * sin(latRad).pow(2))
        val t = tan(latRad).pow(2)
        val c = (eSq / (1.0 - eSq)) * cos(latRad).pow(2)
        val aCoeff = cos(latRad) * (lngRad - centralMeridianRad)

        val m = a * ((1.0 - eSq / 4.0 - 3.0 * eSq.pow(2) / 64.0) * latRad -
            (3.0 * eSq / 8.0 + 3.0 * eSq.pow(2) / 32.0) * sin(2.0 * latRad) +
            (15.0 * eSq.pow(2) / 256.0) * sin(4.0 * latRad))

        val easting = (k0 * n * (aCoeff + (1.0 - t + c) * aCoeff.pow(3) / 6.0) + 500000.0).roundToInt()
        var northing = (k0 * (m + n * tan(latRad) * (aCoeff.pow(2) / 2.0))).roundToInt()
        if (clampedLat < 0) northing += 10000000

        return String.format(Locale.US, "%02d%c %06dE %07dN", zoneNumber, zoneLetter, easting, northing)
    }

    fun cardinalDirection(bearing: Float): String {
        val normalized = ((bearing % 360f) + 360f) % 360f
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = ((normalized + 22.5f) / 45f).toInt() % 8
        return "${normalized.roundToInt()}° ${directions[idx]}"
    }

    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            String.format(Locale.US, "%.0f m", meters)
        } else {
            String.format(Locale.US, "%.2f km", meters / 1000.0)
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Web Mercator tile X/Y conversion for OpenStreetMap & offline tile caching
     */
    fun lonToTileX(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun latToTileY(lat: Double, zoom: Int): Int {
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val latRad = Math.toRadians(clampedLat)
        val n = 1 shl zoom
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n
        return floor(y).toInt().coerceIn(0, n - 1)
    }

    fun lonToWorldX(lon: Double, zoom: Double): Double {
        val scale = 256.0 * 2.0.pow(zoom)
        return (lon + 180.0) / 360.0 * scale
    }

    fun latToWorldY(lat: Double, zoom: Double): Double {
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val latRad = Math.toRadians(clampedLat)
        val scale = 256.0 * 2.0.pow(zoom)
        return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * scale
    }

    fun worldXToLon(worldX: Double, zoom: Double): Double {
        val scale = 256.0 * 2.0.pow(zoom)
        return (worldX / scale) * 360.0 - 180.0
    }

    fun worldYToLat(worldY: Double, zoom: Double): Double {
        val scale = 256.0 * 2.0.pow(zoom)
        val n = Math.PI - 2.0 * Math.PI * worldY / scale
        return Math.toDegrees(atan2(0.5 * ( kotlin.math.exp(n) - kotlin.math.exp(-n) ), 1.0))
    }
}
