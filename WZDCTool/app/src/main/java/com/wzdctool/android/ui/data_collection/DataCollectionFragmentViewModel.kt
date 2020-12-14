package com.wzdctool.android.ui.data_collection

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.wzdctool.android.dataclasses.*
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.automaticDetectionSubject
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingVar
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.DataFileRepository.markerSubject
import kotlin.math.*


class DataCollectionFragmentViewModel : ViewModel() {
    // Required parameters
    lateinit var localDataObj: DataCollectionObj
    lateinit var localUIObj: DataCollectionUIObj

    private var prevDistance = 0.0
    var automaticDetection = MutableLiveData<Boolean>()
    var updatingMap = MutableLiveData<Boolean>()

    var hasSetDataLogFalseMarker = false
    var isViewDisabled = false

    // var laneStat = MutableList<Boolean>(8+1) {false}
    var wpStat = false
    var currWpStat = false
    var zoom = -1
    var dataLog = MutableLiveData<Boolean>(false)
    var gotRP = MutableLiveData<Boolean>(false)
    var notificationText = MutableLiveData<String>()
    var laneStat = MutableLiveData<MutableList<Boolean>>(MutableList<Boolean>(8 + 1) { false })

    var prevLocation: Location? = null


    fun initializeUI(data_obj: DataCollectionObj) {
        localUIObj = mapDataToUIObj(data_obj)
        automaticDetection.value = localUIObj.automatic_detection
        automaticDetectionSubject.onNext(localUIObj.automatic_detection)
    }

    private fun mapDataToUIObj(data_obj: DataCollectionObj): DataCollectionUIObj {
        val ui_obj = DataCollectionUIObj(
            num_lanes = data_obj.num_lanes,
            laneStat = laneStat.value!!,
            wpStat = wpStat,
            dataLog = dataLog.value!!,
            gotRP = gotRP.value!!,
            currLocation = Coordinate(0.0, 0.0, 0.0), // currentLocation,
            start_coord = data_obj.start_coord,
            end_coord = data_obj.end_coord,
            speed_limits = data_obj.speed_limits,
            automatic_detection = data_obj.automatic_detection,
            data_lane = data_obj.data_lane
        )
        return ui_obj
    }

    fun startDataCollection() {
        println("Data Logging Started")
        val marker = MarkerObj("Data Log", "True")
        markerSubject.onNext(marker)
        dataLog.value = true
        dataLoggingVar = true
    }

    fun stopDataCollection() {
        // toastNotificationSubject.onNext("Stopping data collection")
        println("Data Logging Ended")
        isViewDisabled = true
        val marker = MarkerObj("Data Log", "False")
        markerSubject.onNext(marker)
        dataLoggingVar = false
        dataLog.value = false
        gotRP.value = false
    }

    fun markRefPt() {
        println("Reference Point Marked")
        val marker = MarkerObj("RP", "")
        markerSubject.onNext(marker)
        gotRP.value = true
    }

    private fun mapLocationToCoord(location: Location): Coordinate {
        return Coordinate(location.latitude, location.longitude, location.altitude)
    }

    // Automatic start/end mode: Automatically start and end data collection based on markers set
    //     in config file
    fun checkLocation(location: Location) {
        println("Checking Location")
        if (!automaticDetection.value!!)
            return
        val currCoord = mapLocationToCoord(location)
        if (dataLog.value!!) {
            if (!gotRP.value!!) {
                val distance = distDeg(localUIObj.start_coord, currCoord)
                if (prevDistance != 0.0 && prevDistance < distance) {
                    // If just moved past start point:
                    toastNotificationSubject.onNext("Starting Data Collection")
                    markRefPt()
                }
                else {
                    prevDistance = distance
                }
            }
            else {
                val distance = distDeg(localUIObj.end_coord, currCoord)
                if (distance < 50) { //prevDistance != 0.0 && prevDistance < distance &&
                    // If close enough to end marker:
                    stopDataCollection()
                }
                // prevDistance = distance
            }
        }
        else if (!dataLog.value!!) {
            val distance = distDeg(localUIObj.start_coord, currCoord)
            if (distance <= 50) {
                // If close enough to start point:
                prevDistance = distance
                startDataCollection()
            }
        }
    }

    // Initialize google map
    // Add start + end markers
    fun initMap(mMap: GoogleMap, mMapView: MapView) {

        if (localUIObj.automatic_detection) {
            // Automatic start + end point detection
            val startMarkerPosition = LatLng(
                localUIObj.start_coord.Lat,
                localUIObj.start_coord.Lon
            )
            val endMarkerPosition = LatLng(
                localUIObj.end_coord.Lat,
                localUIObj.end_coord.Lon
            )
            val startMarker = MarkerOptions()
                .position(startMarkerPosition)
                .title("Start of Work Zone")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            val endMarker = MarkerOptions()
                .position(endMarkerPosition)
                .title("End of Work Zone")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            mMap.addMarker(startMarker)
            mMap.addMarker(endMarker)

            val centerLat = (startMarkerPosition.latitude + endMarkerPosition.latitude)/2
            val centerLon = (startMarkerPosition.longitude + endMarkerPosition.longitude)/2
            // val center = str(centerLat) + ',' + str(centerLon)

            val north = max(startMarkerPosition.latitude, endMarkerPosition.latitude)
            val south = min(startMarkerPosition.latitude, endMarkerPosition.latitude)
            val east = max(startMarkerPosition.longitude, endMarkerPosition.longitude)
            val west = min(startMarkerPosition.longitude, endMarkerPosition.longitude)

            val pixelWidth = mMapView.width
            val pixelHeight = mMapView.height

            zoom = calcZoomLevel(north, south, east, west, pixelWidth, pixelHeight)
        }
        else {
            zoom = 10
        }

        mMap.setOnCameraMoveStartedListener {
            if (it == 1) {
                // User moved map
                updatingMap.value = false
            }
            // else if (it == 3) //Code moved map
        }
    }

    // update local variable with zoom level of map
    fun setCurrentZoom(mMap: GoogleMap?) {
        zoom = mMap!!.cameraPosition.zoom.toInt()
    }

    fun zoomIn() {
        zoom++
    }

    fun zoomOut() {
        zoom--
    }

    // Update map camera location and zoom
    fun updateMapLocation(location: Location?, mMap: GoogleMap?) {
        if (mMap == null || location == null)
            return
        val currLocation = LatLng(location.latitude, location.longitude)
        val center = CameraUpdateFactory.newLatLngZoom(currLocation, zoom.toFloat())
        if (updatingMap.value == true) {
            mMap.animateCamera(center, 10, null)

        }
        prevLocation = location
    }

    // Dynamically calculate zoom level to fit bounds
    fun calcZoomLevel(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        pixelWidth: Int,
        pixelHeight: Int
    ): Int {
        val GLOBE_WIDTH = 256
        val ZOOM_MAX = 21 - 7
        var angle = east - west
        if (angle < 0) {
            angle += 360
        }
        val zoomHoriz = (ln(
            pixelWidth * 360
                    / angle
                    / GLOBE_WIDTH
        )
                / ln(2.0)
                ).roundToInt() - 3

        angle = north - south
        val centerLat = (north + south) / 2
        if (angle < 0)
            angle += 360
        val zoomVert = (
                ln(
                    pixelHeight * 360
                            / angle
                            / GLOBE_WIDTH
                            * cos(centerLat * Math.PI / 180)
                )
                        / ln(2.0)
                ).roundToInt() - 3

        return zoomHoriz.coerceAtMost(zoomVert).coerceAtMost(ZOOM_MAX).coerceAtLeast(0)
    }

    // Get distance between points (Locations in radians)
    private fun dist(p1: Coordinate, p2: Coordinate): Double {
        val R = 6371000
        val avgLat = (p1.Lat + p2.Lat) / 2
        return R * sqrt((p1.Lat - p2.Lat).pow(2) + cos(avgLat).pow(2) * (p1.Lon - p2.Lon).pow(2))
    }

    // Get distance between points (Locations in degrees)
    private fun distDeg(p1: Coordinate, p2: Coordinate): Double {
        val R = 6371000
        val p = PI/180
        val avgLat = (p1.Lat*p + p2.Lat*p) / 2
        return R * sqrt(
            (p1.Lat * p - p2.Lat * p).pow(2) + cos(avgLat).pow(2) * (p1.Lon * p - p2.Lon * p).pow(
                2
            )
        )
    }

    // Find closest point from point on line
    // TODO: Determine if need lon=lon*cos(lat)
    private fun dist_to_line(v: Coordinate, w: Coordinate, p: Coordinate): Double {
        val l = dist(v, w).pow(2.0)
        val t = max(0.0, min(1.0, dot(dif(p, v), dif(w, v))))
        val pp = Coordinate(v.Lat + t * (w.Lat - v.Lat), v.Lon + t * (w.Lon - v.Lon), null)
        return dist(p, pp)
    }

    // Dot product
    private fun dot(v: Coordinate, w: Coordinate): Double {
        return v.Lat * w.Lat + v.Lon * w.Lon
    }

    // Get difference between two coordinates
    private fun dif(v: Coordinate, w: Coordinate): Coordinate {
        return if (v.Elev != null && w.Elev != null )
            Coordinate(v.Lon - w.Lon, v.Lon - w.Lon, v.Elev - w.Elev)
        else
            Coordinate(v.Lon - w.Lon, v.Lon - w.Lon, null)
    }

    // Lane closure toggled
    fun laneClicked(lane: Int) {
        val currLaneStat: MutableList<Boolean> = laneStat.value!!
        if (currLaneStat[lane]) {
            println("Lane $lane Opened")
            currLaneStat[lane] = false
            laneStat.value = currLaneStat
            val marker = MarkerObj("LO", lane.toString())
            markerSubject.onNext(marker)
        }
        else {
            println("Lane $lane Closed")
            currLaneStat[lane] = true
            laneStat.value = currLaneStat
            val marker = MarkerObj("LC", lane.toString())
            markerSubject.onNext(marker)
        }
    }

    fun initVisualizer(fileName: String) {
        val visualizationObj = DataFileRepository.getVisualizationObj(fileName)
        DataClassesRepository.visualizationObj = visualizationObj
    }
}