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
import androidx.lifecycle.MutableLiveData
import com.wzdctool.android.Constants
import com.wzdctool.android.MainActivity
import com.wzdctool.android.dataclasses.CSVObj
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.csvDataSubject
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import java.util.*


class LocationService : Service() {

    // private lateinit var locationCallback: LocationCallback
    private var CHANNEL_ID: String = "location_notification_channel"
    // var marker: String = ""
    var markerQueue: Queue<String> = LinkedList<String>()
    // var markerValue: String = ""
    var markerValueQueue: Queue<String> = LinkedList<String>()

    // var currentLocation = MutableLiveData<Location>()

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service
        return null
    }

    var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations){

                locationSubject.value = location
                Log.v("LocationService", "Lat: ${location.latitude}, " +
                        "Lon: ${location.longitude}, " + "elevation: ${location.altitude}, " +
                        "accuracy: ${location.accuracy}")

                if (dataLoggingSubject.value != true) {
                    val csvObj = CSVObj(Date(location.time), 0, location.accuracy,
                        location.latitude, location.longitude, location.altitude, location.speed,
                        location.bearing, "", "", false)
                    csvDataSubject.value = csvObj
                }
                //val time: Date, val num_sats: Int, val hdop: Double, val latitude: Double,
                // val longitude: Double, val altitude: Double, val speed: Double,
                // val heading: Double, val marker: String, val marker_value: String
            }
        }
    }

    //    var builder = NotificationCompat.Builder(this, CHANNEL_ID)
//        .setSmallIcon(R.drawable.notification_icon)
//        .setContentTitle(textTitle)
//        .setContentText(textContent)
//        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//    private fun createNotificationChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string.channel_name)
//            val descriptionText = getString(R.string.channel_description)
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationService() {
        val notificationManager: NotificationManager =
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        //Notification.
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Using Location")
            .setContentText("logging location")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setTicker("text")
            .build()

        if (notificationManager != null &&
            notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel: NotificationChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "This channel is used by location service"
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val locationRequest: LocationRequest = LocationRequest()
            .setInterval(4000)
            .setFastestInterval(1000)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null) {
                if (action == Constants.ACTION_START_LOCATION_SERVICE) {
                    startLocationService()
                }
                else if (action == Constants.ACTION_STOP_LOCATION_SERVICE) {
                    stopLocationService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
