package com.wzdctool.android.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.wzdctool.android.R
import androidx.preference.PreferenceManager

import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.wzdctool.android.Constants
import com.wzdctool.android.MainActivity
import com.wzdctool.android.dataclasses.*
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataFileRepository
import java.lang.Math.*
import java.util.*
import kotlin.math.ln
import kotlin.math.roundToInt

class LocationService : Service() {

    // private lateinit var locationCallback: LocationCallback
    private var CHANNEL_ID: String = "location_notification_channel"
    private var locationSource: String = ""
    private var locationSources: MutableList<String> = mutableListOf()

    // var currentLocation = MutableLiveData<Location>()

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationService()
    }

//    override fun onCreate() {
//        Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show()
//    }

//    init {
//        DataClassesRepository.activeLocationSourceSubject.subscribe {
//            updateLocationSource(it)
//        }
//        DataClassesRepository.locationSourcesSubject.subscribe {
//            updateLocationSources(it)
//        }
//    }
//
//    private fun updateLocationSource(source: String) {
//        locationSource = source
//    }
//
//    private fun updateLocationSources(sources: List<String>) {
//        locationSources = sources as MutableList<String>
//    }

    var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            val localLocationSources = locationSourcesSubject.value
            if (localLocationSources.internal != gps_status.valid) {
                localLocationSources.internal = gps_status.valid
                locationSourcesSubject.onNext(localLocationSources)
                DataClassesRepository.toastNotificationSubject.onNext("Internal GPS Valid")
            }
            if (activeLocationSourceSubject.value == gps_type.internal) {
                for (location in locationResult.locations){
                    locationSubject.onNext(location)
//                    Log.v("LocationService", "Lat: ${location.latitude}, " +
//                            "Lon: ${location.longitude}, " + "elevation: ${location.altitude}, " +
//                            "accuracy: ${location.accuracy}")

                    // println(csvObj.toString())
                    //val time: Date, val num_sats: Int, val hdop: Double, val latitude: Double,
                    // val longitude: Double, val altitude: Double, val speed: Double,
                    // val heading: Double, val marker: String, val marker_value: String
                }
            }
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationService() {
        val notificationManager: NotificationManager =
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notification: Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Processing Location")
                .setSmallIcon(R.drawable.ic_baseline_pin_drop_24)
                .setContentIntent(pendingIntent)
                .setTicker("text")
                .build()

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val notificationChannel: NotificationChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Location Service", NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationChannel.description = "This channel is used by location service"
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        else {
            notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Processing Location")
                .setSmallIcon(R.drawable.ic_baseline_pin_drop_24)
                .setContentIntent(pendingIntent)
                .setTicker("text")
                .build()
        }

        val locationRequest: LocationRequest = LocationRequest()
            .setInterval(1000)
            .setFastestInterval(900)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("Failed Permissions")
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        startForeground(Constants.LOCATION_SERVICE_ID, notification)
    }

    private fun stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
            .removeLocationUpdates((locationCallback))
        stopForeground(true)
        stopSelf()
    }

//    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null) {
                if (action == Constants.ACTION_START_LOCATION_SERVICE) {
                    startLocationService()
                    val localLocationSources = locationSourcesSubject.value
                    if (localLocationSources.internal == gps_status.disconnected) {
                        localLocationSources.internal = gps_status.invalid
                        locationSourcesSubject.onNext(localLocationSources)
                    }
//                    val added: Boolean = locationSources.add(Constants.LOCATION_SOURCE_INTERNAL)
//                    DataClassesRepository.locationSourcesSubject.onNext(locationSources)
//                    DataClassesRepository.locationSourceValidSubject.onNext(true)
                }
                else if (action == Constants.ACTION_STOP_LOCATION_SERVICE) {
                    stopLocationService()
                    val localLocationSources = locationSourcesSubject.value
                    localLocationSources.internal = gps_status.disconnected
                    locationSourcesSubject.onNext(localLocationSources)
//                        DataClassesRepository.locationSourceValidSubject.onNext(false)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
