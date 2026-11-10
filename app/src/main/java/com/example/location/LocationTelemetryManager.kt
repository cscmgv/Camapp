package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class WaypointPreset(
    val name: String,
    val regionLabel: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double
)

data class LiveTelemetryState(
    val latitude: Double = 13.0500,
    val longitude: Double = 80.2824,
    val altitudeMeters: Double = 14.2,
    val accuracyMeters: Float = 4.2f,
    val bearingDegrees: Float = 42f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val speedMps: Float = 0.0f,
    val addressLine: String = "Marina Coastal Sector, Chennai, IN",
    val hasLocationPermission: Boolean = false,
    val hasLiveGpsFix: Boolean = false,
    val isManualCalibrationOverride: Boolean = false,
    val isDeviceOnline: Boolean = true,
    val satelliteCountEstimate: Int = 9,
    val lastFixTimestamp: Long = System.currentTimeMillis()
)

class LocationTelemetryManager(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _telemetry = MutableStateFlow(LiveTelemetryState())
    val telemetry: StateFlow<LiveTelemetryState> = _telemetry.asStateFlow()

    private var rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravityReading = FloatArray(3)
    private val magneticReading = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false

    private var isTrackingLocation = false

    val calibrationPresets = listOf(
        WaypointPreset("Marina Lighthouse, Chennai", "Marina Coastal Sector, Chennai, IN", 13.0397, 80.2794, 16.5),
        WaypointPreset("Nilgiri Highland Ridge, Ooty", "Doddabetta Survey Crest, Tamil Nadu, IN", 11.4008, 76.7358, 2637.0),
        WaypointPreset("Kodaikanal Pillar Rocks", "Palani Hills Field Station, TN, IN", 10.2102, 77.4652, 2195.0),
        WaypointPreset("Western Ghats Misty Valley", "Anamalai Eco Trail, Pollachi, IN", 10.4704, 77.0588, 1140.0),
        WaypointPreset("Golden Gate Vista Point", "Marin Headlands, San Francisco, US", 37.8270, -122.4994, 98.4)
    )

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            onNewHardwareLocation(loc)
        }
    }

    private val frameworkListener = LocationListener { loc ->
        onNewHardwareLocation(loc)
    }

    fun updatePermissionState(granted: Boolean) {
        _telemetry.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            startLocationUpdates()
        }
    }

    fun refreshConnectivityStatus() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _telemetry.update { it.copy(isDeviceOnline = online) }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        refreshConnectivityStatus()
        startSensors()
        if (!_telemetry.value.hasLocationPermission || isTrackingLocation) return

        try {
            isTrackingLocation = true
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
                .setMinUpdateIntervalMillis(1000L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                fusedCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) onNewHardwareLocation(loc)
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2500L,
                    1f,
                    frameworkListener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: Exception) {
            isTrackingLocation = false
        }
    }

    fun stopUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(fusedCallback)
            locationManager.removeUpdates(frameworkListener)
            sensorManager.unregisterListener(this)
            isTrackingLocation = false
        } catch (_: Exception) {
        }
    }

    private fun startSensors() {
        try {
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
                magnetometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun onNewHardwareLocation(loc: Location) {
        if (_telemetry.value.isManualCalibrationOverride) return
        val lat = loc.latitude
        val lng = loc.longitude
        val alt = if (loc.hasAltitude()) loc.altitude else _telemetry.value.altitudeMeters
        val acc = if (loc.hasAccuracy()) loc.accuracy else 4.8f
        val spd = if (loc.hasSpeed()) loc.speed else 0f
        val brg = if (loc.hasBearing() && loc.bearing != 0f) loc.bearing else _telemetry.value.bearingDegrees

        _telemetry.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                altitudeMeters = alt,
                accuracyMeters = acc,
                speedMps = spd,
                bearingDegrees = brg,
                hasLiveGpsFix = true,
                satelliteCountEstimate = if (acc < 6f) 11 else if (acc < 15f) 8 else 5,
                lastFixTimestamp = System.currentTimeMillis()
            )
        }
        resolveAddressAsync(lat, lng)
    }

    fun applyCalibrationWaypoint(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        customLabel: String? = null
    ) {
        val fallbackLabel = customLabel ?: offlineRegionFallback(latitude, longitude)
        _telemetry.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = altitudeMeters,
                accuracyMeters = 3.2f,
                isManualCalibrationOverride = true,
                addressLine = fallbackLabel,
                lastFixTimestamp = System.currentTimeMillis()
            )
        }
        if (customLabel == null) {
            resolveAddressAsync(latitude, longitude)
        }
    }

    fun resumeLiveGpsSensor() {
        _telemetry.update {
            it.copy(isManualCalibrationOverride = false)
        }
        if (_telemetry.value.hasLocationPermission) {
            isTrackingLocation = false
            startLocationUpdates()
        }
    }

    private fun resolveAddressAsync(lat: Double, lng: Double) {
        scope.launch(Dispatchers.IO) {
            val fallback = offlineRegionFallback(lat, lng)
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            val first = addresses.firstOrNull()
                            val line = formatAddress(first) ?: fallback
                            _telemetry.update { it.copy(addressLine = line) }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        val line = formatAddress(addresses?.firstOrNull()) ?: fallback
                        _telemetry.update { it.copy(addressLine = line) }
                    }
                } else {
                    _telemetry.update { it.copy(addressLine = fallback) }
                }
            } catch (_: Exception) {
                _telemetry.update { it.copy(addressLine = fallback) }
            }
        }
    }

    private fun formatAddress(addr: android.location.Address?): String? {
        if (addr == null) return null
        val parts = listOfNotNull(
            addr.featureName?.takeIf { it.length > 2 },
            addr.subLocality ?: addr.locality,
            addr.adminArea,
            addr.countryCode
        ).distinct()
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }

    private fun offlineRegionFallback(lat: Double, lng: Double): String {
        val match = calibrationPresets.minByOrNull {
            abs(it.latitude - lat) + abs(it.longitude - lng)
        }
        if (match != null && abs(match.latitude - lat) < 0.15 && abs(match.longitude - lng) < 0.15) {
            return match.regionLabel
        }
        val latHem = if (lat >= 0) "North" else "South"
        val lngHem = if (lng >= 0) "East" else "West"
        return String.format(Locale.US, "Offline Sector %.3f° %s, %.3f° %s", abs(lat), latHem, abs(lng), lngHem)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                updateOrientation(orientationAngles)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.size)
                hasGravity = true
                if (hasMagnetic) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magneticReading)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        updateOrientation(orientationAngles)
                    }
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magneticReading, 0, magneticReading.size)
                hasMagnetic = true
                if (hasGravity) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magneticReading)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        updateOrientation(orientationAngles)
                    }
                }
            }
        }
    }

    private fun updateOrientation(orientationAngles: FloatArray) {
        val azimuthDeg = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f)
        val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        val current = _telemetry.value
        if (abs(current.bearingDegrees - azimuthDeg) > 1.5f || abs(current.rollDegrees - rollDeg) > 1.0f) {
            _telemetry.update {
                it.copy(
                    bearingDegrees = azimuthDeg,
                    pitchDegrees = pitchDeg,
                    rollDegrees = rollDeg
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
