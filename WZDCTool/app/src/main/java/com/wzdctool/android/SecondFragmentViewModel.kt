package com.wzdctool.android

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.wzdctool.android.dataclasses.*
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.automaticDetectionSubject
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingVar
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.DataFileRepository.markerSubject
import kotlin.math.*


class SecondFragmentViewModel : ViewModel() {
    // Required parameters
    lateinit var localDataObj: DataCollectionObj
    lateinit var localUIObj: SecondFragmentUIObj

    private var prevDistance = 0.0
    var automaticDetection = MutableLiveData<Boolean>()
    var updatingMap = MutableLiveData<Boolean>()

    var hasSetDataLogFalseMarker = false

    // var laneStat = MutableList<Boolean>(8+1) {false}
    var wpStat = false
    var currWpStat = false
    var zoom = -1
    var dataLog = MutableLiveData<Boolean>(false)
    var gotRP = MutableLiveData<Boolean>(false)
    var navigationLiveData = MutableLiveData<Int>()
    var notificationText = MutableLiveData<String>()
    var laneStat = MutableLiveData<MutableList<Boolean>>(MutableList<Boolean>(8 + 1) { false })

    var prevLocation: Location? = null


    fun initializeUI(data_obj: DataCollectionObj) {
        localUIObj = mapDataToUIObj(data_obj)
        automaticDetection.value = localUIObj.automatic_detection
        automaticDetectionSubject.onNext(localUIObj.automatic_detection)
    }

    private fun mapDataToUIObj(data_obj: DataCollectionObj): SecondFragmentUIObj {
        val ui_obj = SecondFragmentUIObj(
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

    fun checkLocation(location: Location) {
        println("Checking Location")
        if (!automaticDetection.value!!)
            return
        val currCoord = mapLocationToCoord(location)
        if (dataLog.value!!) {
            if (!gotRP.value!!) {
                val distance = distDeg(localUIObj.start_coord, currCoord)
                if (prevDistance != 0.0 && prevDistance < distance) {
                    toastNotificationSubject.onNext("Starting Data Collection")
                    markRefPt()
//                    prevDistance = 0.0
                }
                else {
                    prevDistance = distance
                }
            }
            else {
                val distance = distDeg(localUIObj.end_coord, currCoord)
                if (distance < 50) { //prevDistance != 0.0 && prevDistance < distance &&
                    stopDataCollection()
                }
                // prevDistance = distance
            }
        }
        else if (!dataLog.value!!) {
            val distance = distDeg(localUIObj.start_coord, currCoord)
            if (distance <= 50) {
                prevDistance = distance
                startDataCollection()
            }
        }
    }

    fun initMap(mMap: GoogleMap, mMapView: MapView) {
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

        mMap.setOnCameraMoveStartedListener {
            if (it == 1) {
                // User moved map
                updatingMap.value = false
            }
            // else if (it == 3) //Code moved map
        }
    }

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

        return Math.max(Math.min(Math.min(zoomHoriz, zoomVert), ZOOM_MAX), 0)
    }

    private fun dist(p1: Coordinate, p2: Coordinate): Double {
        val R = 6371000
        val avgLat = (p1.Lat + p2.Lat) / 2
        return R * sqrt((p1.Lat - p2.Lat).pow(2) + cos(avgLat).pow(2) * (p1.Lon - p2.Lon).pow(2))
    }

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

    // TODO: Determine if need lon=lon*cos(lat)
    private fun dist_to_line(v: Coordinate, w: Coordinate, p: Coordinate): Double {
        val l = dist(v, w).pow(2.0)
        val t = max(0.0, min(1.0, dot(dif(p, v), dif(w, v))))
        val pp = Coordinate(v.Lat + t * (w.Lat - v.Lat), v.Lon + t * (w.Lon - v.Lon), null)
        return dist(p, pp)
    }

    private fun dot(v: Coordinate, w: Coordinate): Double {
        return v.Lat * w.Lat + v.Lon * w.Lon
    }

    private fun dif(v: Coordinate, w: Coordinate): Coordinate {
        return if (v.Elev != null && w.Elev != null )
            Coordinate(v.Lon - w.Lon, v.Lon - w.Lon, v.Elev - w.Elev)
        else
            Coordinate(v.Lon - w.Lon, v.Lon - w.Lon, null)
    }

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

    fun uploadDataFile(fileName: String) {
//        println("UploadDataFile")
//        dataFileName =
//            "path-data--${ConfigurationRepository.activeWZIDSubject.value}.csv"
//        val uploadFileName =
//            if (automaticDetection.value!!) {
//                dataFileName
//            }
//            else {
//                dataFileName.replace(".csv", "--update-image.csv")
//            }
//
//        viewModelScope.launch(Dispatchers.IO) {
//            val output = DataFileRepository.uploadPathDataFile(
//                fileName,
//                uploadFileName
//            )
//            println(output)
//            notificationSubject.onNext("Path data file uploaded")
//        }
//        navigationLiveData.value = R.id.action_SecondFragment_to_MainFragment
//        val testFileName = "${Constants.PENDING_UPLOAD_DIRECTORY}/path-data--sample-work-zone--white-rock-cir--update-image.csv"
        val visualizationObj = DataFileRepository.getVisualizationObj(fileName)
        DataClassesRepository.visualizationObj = visualizationObj
        navigationLiveData.value = R.id.action_SecondFragment_to_editingFragment2
    }
}