package com.wzdctool.android.ui.visualization

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.wzdctool.android.R
import com.wzdctool.android.dataclasses.CSVMarkerObj
import com.wzdctool.android.dataclasses.VisualizationObj
import com.wzdctool.android.dataclasses.getTitleString
import com.wzdctool.android.dataclasses.parseTitleString
import com.wzdctool.android.repos.DataFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class EditingFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    lateinit var localVisualizationObj: VisualizationObj
    val laneClosedIcons = listOf<Int>(
        R.drawable.lane_1_closed_marker,
        R.drawable.lane_2_closed_marker,
        R.drawable.lane_3_closed_marker,
        R.drawable.lane_4_closed_marker,
        R.drawable.lane_5_closed_marker,
        R.drawable.lane_6_closed_marker,
        R.drawable.lane_7_closed_marker,
        R.drawable.lane_8_closed_marker
    )
    val laneOpenIcons = listOf<Int>(
        R.drawable.lane_1_open_marker,
        R.drawable.lane_2_open_marker,
        R.drawable.lane_3_open_marker,
        R.drawable.lane_4_open_marker,
        R.drawable.lane_5_open_marker,
        R.drawable.lane_6_open_marker,
        R.drawable.lane_7_open_marker,
        R.drawable.lane_8_open_marker
    )

    val markersList = mutableListOf<Marker>()
    var navigationLiveData = MutableLiveData<Int>()

    fun initializeUI(visualizationObj: VisualizationObj) {
        localVisualizationObj = visualizationObj
    }

    fun initMap(mMap: GoogleMap, mMapView: MapView) {
        for ((i, pathPoint) in localVisualizationObj.dataPoints.withIndex()) {
            val dataPointMarker = MarkerOptions()
                .position(pathPoint)
                .title("Data Point $i")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.veh_path_point))
            mMap.addMarker(dataPointMarker)
        }

        for (marker in localVisualizationObj.markers) {
            val title = marker.title
            val icon: BitmapDescriptorFactory
            val iconID: Int
//            val draggable: Boolean
            if (title.type == "LC") {
                iconID = laneClosedIcons[(title.value.toInt() - 1).coerceAtLeast(0).coerceAtMost(8)]
//                draggable = true
            }
            else if (title.type == "LO") {
                iconID = laneOpenIcons[(title.value.toInt() - 1).coerceAtLeast(0).coerceAtMost(8)]
//                draggable = true
            }
            else if (title.type == "WP") {
//                draggable = true
                iconID = if (title.value == "True") {
                    R.drawable.workers_present_marker
                } else {
                    R.drawable.workers_not_present_marker
                }
            }
            else if (title.type == "RP") {
//                draggable = true
                iconID = R.drawable.ref_point_marker
            }
            else if (title.type == "Data Log") {
//                draggable = false
                iconID = if (title.value == "True") {
                    R.drawable.path_start_point
                } else {
                    R.drawable.path_end_point
                }
            }
            else {
//                draggable = false
                iconID = R.drawable.veh_path_point
            }
            val googleMarker = MarkerOptions()
                .position(marker.position)
                .title(getTitleString(title))
                .icon(BitmapDescriptorFactory.fromResource(iconID))
//                .draggable(draggable)

            markersList.add(mMap.addMarker(googleMarker))
            mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(p0: Marker?) {
//                    TODO("Not yet implemented")
                }

                override fun onMarkerDrag(p0: Marker?) {
//                    TODO("Not yet implemented")
                }

                override fun onMarkerDragEnd(p0: Marker?) {
                    val index = getClosestDataPointIndex(p0!!.position)
                    p0!!.position = localVisualizationObj.dataPoints[index]
                    var p0Title = parseTitleString(p0.title)
//                    for (marker in markersList) {
//                        val title = parseTitleString(marker.title)
//                        if (p0Title.IDs.primary == title.IDs.primary) {
//                            marker.position = localVisualizationObj.dataPoints[index]
//                        }
//                    }
//                    TODO("Not yet implemented")
                }

            })
        }
//        mMap.setInfoWindowAdapter(CustomInfoWindowHandler(this))

        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                localVisualizationObj.dataPoints[0],
                15f
            )
        )
    }

    fun saveEdits() {
        val currMarkerList = getCurrentMarkersList()
        viewModelScope.launch(Dispatchers.IO) {
            DataFileRepository.updateDataFileMarkers(localVisualizationObj.dataFile, currMarkerList)
        }
        navigationLiveData.value = R.id.action_editingFragment_to_MainFragment
    }

    private fun getCurrentMarkersList(): List<CSVMarkerObj> {
        val markerObjList = mutableListOf<CSVMarkerObj>()
        for (marker in markersList) {
            val title = parseTitleString(marker.title) ?: continue
            val index = getClosestDataPointIndex(marker.position)
            println("${title.type}: ${title.value}, $index")
            markerObjList.add(CSVMarkerObj(index, title.type, title.value))
        }
        return markerObjList
    }

    private fun getClosestDataPoint(location: LatLng): LatLng {
        var closestPosition: LatLng = LatLng(0.0, 0.0)
        var closestDistance: Float? = null
        for (pathPoint in localVisualizationObj.dataPoints) {
            val results: FloatArray = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                pathPoint.latitude, pathPoint.longitude, results
            )
            if (closestDistance != null) {
                if (results[0] < closestDistance) {
                    closestDistance = results[0]
                    closestPosition = pathPoint
                }
            }
            else {
                closestDistance = results[0]
                closestPosition = pathPoint
            }
        }
        return closestPosition
    }

    private fun getClosestDataPointIndex(location: LatLng): Int {
        var closestIndex = 0
        var closestDistance: Float? = null
        for ((i, pathPoint) in localVisualizationObj.dataPoints.withIndex()) {
            val results: FloatArray = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                pathPoint.latitude, pathPoint.longitude, results
            )
            if (closestDistance != null) {
                if (results[0] < closestDistance) {
                    closestDistance = results[0]
                    closestIndex = i
                }
            }
            else {
                closestDistance = results[0]
                closestIndex = i
            }
        }
        return closestIndex
    }
}
