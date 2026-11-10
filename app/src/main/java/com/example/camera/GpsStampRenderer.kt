package com.example.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import com.example.data.StampTemplate
import com.example.util.GeoFormatUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class GpsStampData(
    val title: String,
    val notes: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val accuracyMeters: Float,
    val bearingDegrees: Float,
    val speedMps: Float,
    val addressLine: String,
    val timestamp: Long,
    val template: StampTemplate
)

object GpsStampRenderer {

    /**
     * Renders a permanent GPS Map & Telemetry Stamp watermark onto the source Bitmap
     * and writes standard EXIF GPS tags to the saved JPEG file.
     */
    fun saveStampedPhotoToFile(
        sourceBitmap: Bitmap,
        stampData: GpsStampData,
        outputFile: File
    ): Long {
        val targetWidth = 1440
        val scale = targetWidth.toFloat() / sourceBitmap.width.coerceAtLeast(1)
        val targetHeight = (sourceBitmap.height * scale).toInt().coerceAtLeast(960)

        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw base photo scaled cleanly
        val matrix = Matrix().apply { postScale(scale, scale) }
        val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(sourceBitmap, matrix, photoPaint)

        // Draw subtle bottom gradient vignette for legibility
        val vignetteHeight = targetHeight * 0.36f
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, targetHeight - vignetteHeight,
                0f, targetHeight.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.argb(190, 5, 11, 20)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(
            0f,
            targetHeight - vignetteHeight,
            targetWidth.toFloat(),
            targetHeight.toFloat(),
            vignettePaint
        )

        when (stampData.template) {
            StampTemplate.TACTICAL_HUD -> drawTacticalHudStamp(canvas, targetWidth, targetHeight, stampData)
            StampTemplate.SURVEYOR_PRO -> drawSurveyorStamp(canvas, targetWidth, targetHeight, stampData)
            StampTemplate.MINIMAL_GEO -> drawMinimalStamp(canvas, targetWidth, targetHeight, stampData)
        }

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.flush()
        }

        writeExifGpsAttributes(outputFile, stampData)
        return outputFile.length()
    }

    private fun drawTacticalHudStamp(
        canvas: Canvas,
        width: Int,
        height: Int,
        data: GpsStampData
    ) {
        val pad = 36f
        val cardHeight = 290f
        val cardTop = height - cardHeight - pad
        val cardRect = RectF(pad, cardTop, width - pad, height - pad)

        // Card background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(225, 11, 20, 34)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, bgPaint)

        // Card border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 122, 0)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        // Left Mini-Map Radar Box
        val mapSize = cardHeight - 44f
        val mapRect = RectF(cardRect.left + 22f, cardRect.top + 22f, cardRect.left + 22f + mapSize, cardRect.bottom - 22f)
        drawMiniRadarInset(canvas, mapRect, data.latitude, data.longitude, data.bearingDegrees)

        // Right Telemetry Block
        val textLeft = mapRect.right + 28f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 122, 0)
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 229, 255)
            textSize = 27f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 23f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        // Category + Plus Code header
        val plusCode = GeoFormatUtils.toPlusCode(data.latitude, data.longitude)
        canvas.drawText(
            "[${data.category.uppercase(Locale.US)}] • PLUS: $plusCode",
            textLeft,
            cardRect.top + 48f,
            badgePaint
        )

        // Waypoint Title + Address
        val displayTitle = if (data.title.isNotBlank()) data.title else "Field Geotag Capture"
        canvas.drawText(
            ellipsize(displayTitle, 34),
            textLeft,
            cardRect.top + 92f,
            titlePaint
        )

        // Decimal & DMS Coordinates
        canvas.drawText(
            GeoFormatUtils.formatDecimalDegrees(data.latitude, data.longitude),
            textLeft,
            cardRect.top + 136f,
            monoPaint
        )
        canvas.drawText(
            "DMS: ${GeoFormatUtils.formatDMS(data.latitude, data.longitude)}",
            textLeft,
            cardRect.top + 174f,
            subPaint
        )

        // Altitude, Bearing, Accuracy
        val telemetryLine = String.format(
            Locale.US,
            "ALT: %.1fm   HDG: %s   ACC: ±%.1fm",
            data.altitudeMeters,
            GeoFormatUtils.cardinalDirection(data.bearingDegrees),
            data.accuracyMeters
        )
        canvas.drawText(telemetryLine, textLeft, cardRect.top + 214f, subPaint)

        // Address & Timestamp footer
        val footerText = "${ellipsize(data.addressLine, 30)} • ${GeoFormatUtils.formatTimestamp(data.timestamp)}"
        val footerPaint = Paint(subPaint).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 21f
        }
        canvas.drawText(footerText, textLeft, cardRect.top + 254f, footerPaint)
    }

    private fun drawSurveyorStamp(
        canvas: Canvas,
        width: Int,
        height: Int,
        data: GpsStampData
    ) {
        val pad = 36f
        val cardHeight = 310f
        val cardRect = RectF(pad, height - cardHeight - pad, width - pad, height - pad)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 15, 23, 42)
        }
        canvas.drawRoundRect(cardRect, 18f, 18f, bgPaint)

        // Top Amber Header Bar
        val headerRect = RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + 56f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 122, 0)
        }
        canvas.drawRoundRect(headerRect, 18f, 18f, headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 15, 25)
            textSize = 25f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(
            "GEOSNAP SURVEYOR RECORD // ${data.category.uppercase(Locale.US)}",
            cardRect.left + 24f,
            cardRect.top + 37f,
            headerTextPaint
        )

        val mapRect = RectF(cardRect.left + 22f, cardRect.top + 74f, cardRect.left + 236f, cardRect.bottom - 22f)
        drawMiniRadarInset(canvas, mapRect, data.latitude, data.longitude, data.bearingDegrees)

        val textLeft = mapRect.right + 26f
        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 25f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(186, 230, 253)
            textSize = 23f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val utm = GeoFormatUtils.toUtmString(data.latitude, data.longitude)
        canvas.drawText("SITE: ${ellipsize(data.title.ifBlank { "Waypoint" }, 32)}", textLeft, cardRect.top + 104f, rowPaint)
        canvas.drawText("LAT/LON: ${GeoFormatUtils.formatDecimalDegrees(data.latitude, data.longitude)}", textLeft, cardRect.top + 144f, detailPaint)
        canvas.drawText("UTM GRID: $utm", textLeft, cardRect.top + 182f, detailPaint)
        canvas.drawText(
            String.format(Locale.US, "ELEV: %.1f m MSL | BEARING: %s | ±%.1f m", data.altitudeMeters, GeoFormatUtils.cardinalDirection(data.bearingDegrees), data.accuracyMeters),
            textLeft,
            cardRect.top + 220f,
            detailPaint
        )
        val noteOrAddr = if (data.notes.isNotBlank()) "NOTE: ${ellipsize(data.notes, 28)} | " else ""
        canvas.drawText(
            "$noteOrAddr${GeoFormatUtils.formatTimestamp(data.timestamp)}",
            textLeft,
            cardRect.top + 258f,
            detailPaint
        )
    }

    private fun drawMinimalStamp(
        canvas: Canvas,
        width: Int,
        height: Int,
        data: GpsStampData
    ) {
        val pad = 40f
        val pillHeight = 168f
        val pillRect = RectF(pad, height - pillHeight - pad, width - pad, height - pad)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 12, 19, 31)
        }
        canvas.drawRoundRect(pillRect, 36f, 36f, bgPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 229, 255)
        }
        canvas.drawRoundRect(
            RectF(pillRect.left + 24f, pillRect.top + 26f, pillRect.left + 34f, pillRect.bottom - 26f),
            6f,
            6f,
            accentPaint
        )

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 158, 64)
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val leftX = pillRect.left + 56f
        canvas.drawText(
            "${ellipsize(data.title.ifBlank { data.addressLine }, 36)} (${data.addressLine})",
            leftX,
            pillRect.top + 56f,
            titlePaint
        )
        canvas.drawText(
            "${GeoFormatUtils.formatDecimalDegrees(data.latitude, data.longitude)}  •  ALT ${String.format(Locale.US, "%.1fm", data.altitudeMeters)}",
            leftX,
            pillRect.top + 100f,
            coordPaint
        )
        canvas.drawText(
            "${GeoFormatUtils.formatTimestamp(data.timestamp)}  •  PLUS ${GeoFormatUtils.toPlusCode(data.latitude, data.longitude)}",
            leftX,
            pillRect.top + 140f,
            metaPaint
        )
    }

    private fun drawMiniRadarInset(
        canvas: Canvas,
        rect: RectF,
        lat: Double,
        lng: Double,
        bearing: Float
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 30, 48)
        }
        canvas.drawRoundRect(rect, 18f, 18f, bgPaint)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 229, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Draw topographic rings & lat/lng grid lines inside radar box
        val cx = rect.centerX()
        val cy = rect.centerY()
        val maxR = rect.width() * 0.42f
        canvas.drawCircle(cx, cy, maxR, gridPaint)
        canvas.drawCircle(cx, cy, maxR * 0.62f, gridPaint)
        canvas.drawLine(rect.left + 12f, cy, rect.right - 12f, cy, gridPaint)
        canvas.drawLine(cx, rect.top + 12f, cx, rect.bottom - 12f, gridPaint)

        // Draw heading cone
        val rad = Math.toRadians((bearing - 90f).toDouble())
        val conePath = Path().apply {
            moveTo(cx, cy)
            val spread = Math.toRadians(28.0)
            lineTo(
                (cx + maxR * cos(rad - spread)).toFloat(),
                (cy + maxR * sin(rad - spread)).toFloat()
            )
            lineTo(
                (cx + maxR * cos(rad + spread)).toFloat(),
                (cy + maxR * sin(rad + spread)).toFloat()
            )
            close()
        }
        val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 255, 122, 0)
            style = Paint.Style.FILL
        }
        canvas.drawPath(conePath, conePaint)

        // Draw center GPS Pin
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 122, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 12f, pinPaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 5f, dotPaint)

        // Draw North 'N' label
        val nPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 229, 255)
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("N", cx, rect.top + 24f, nPaint)

        // Mini coord label at bottom of radar
        val miniCoord = String.format(Locale.US, "%.3f,%.3f", lat, lng)
        canvas.drawText(miniCoord, cx, rect.bottom - 10f, nPaint)
    }

    private fun writeExifGpsAttributes(file: File, data: GpsStampData) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToExifDMS(data.latitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (data.latitude >= 0) "N" else "S")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToExifDMS(data.longitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (data.longitude >= 0) "E" else "W")
            exif.setAttribute(
                ExifInterface.TAG_GPS_ALTITUDE,
                "${(abs(data.altitudeMeters) * 100).toInt()}/100"
            )
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (data.altitudeMeters >= 0) "0" else "1")
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "${data.title} | ${data.notes}")
            exif.saveAttributes()
        } catch (_: Exception) {
            // Ignore EXIF errors on unsupported filesystems
        }
    }

    private fun decimalToExifDMS(coord: Double): String {
        val absCoord = abs(coord)
        val degrees = absCoord.toInt()
        val minutesFull = (absCoord - degrees) * 60.0
        val minutes = minutesFull.toInt()
        val secondsMilli = ((minutesFull - minutes) * 60.0 * 1000.0).toInt()
        return "$degrees/1,$minutes/1,$secondsMilli/1000"
    }

    private fun ellipsize(input: String, maxChars: Int): String {
        return if (input.length <= maxChars) input else input.take(maxChars - 1) + "…"
    }
}
