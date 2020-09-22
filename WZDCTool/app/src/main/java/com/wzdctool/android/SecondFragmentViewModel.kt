package com.wzdctool.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wzdctool.android.dataclasses.*
import com.wzdctool.android.repos.DataClassesRepository.markerSubject
import com.wzdctool.android.services.LocationService

class SecondFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel\

    // Required parameters
    lateinit var localDataObj: DataCollectionObj
    lateinit var localUIObj: SecondFragmentUIObj
    //DataCollectionObj (val num_lanes: Int, val start_coord: Coordinate,
    //                         val end_coord: Coordinate, val speed_limits: SPEEDLIMITS,
    //                         val automatic_detection: Boolean)

    //
    var laneStat = MutableList<Boolean>(8+1) {false}
    var wpStat = false
    var currWpStat = false
    var dataLog = MutableLiveData<Boolean>(false)
    var gotRP = MutableLiveData<Boolean>(false)
    // var

//    val currentUIObj: MutableLiveData<SecondFragmentUIObj> by lazy {
//        MutableLiveData<SecondFragmentUIObj>()
//    }


    fun firstTimeSetup() {
        // laneStat.setValue(currLaneStat)
    }

    fun initializeUI(data_obj: DataCollectionObj) {
        localUIObj = mapDataToUIObj(data_obj)
        // LocationService
        // SecondFragment().setUI(ui_obj)
    }

    fun mapDataToUIObj(data_obj: DataCollectionObj): SecondFragmentUIObj {
        val ui_obj = SecondFragmentUIObj(
            num_lanes = data_obj.num_lanes,
            laneStat = laneStat,
            wpStat = wpStat,
            dataLog = dataLog.value!!,
            gotRP = gotRP.value!!,
            currLocation = Coordinate(0.0, 0.0, 0.0), // currentLocation,
            start_coord = data_obj.start_coord,
            end_coord = data_obj.end_coord,
            speed_limits = data_obj.speed_limits,
            automatic_detection = data_obj.automatic_detection
        )
        return ui_obj
        //(val num_lanes: Int, val laneStat: List<Boolean>,
        //                                val wpStat: Boolean, val dataLog: Boolean, val gotRP: Boolean,
        //                                val currLocation: Coordinate, val start_coord: Coordinate,
        //                                val end_coord: Coordinate, val speed_limits: SPEEDLIMITS,
        //                                val automatic_detection: Boolean)
    }

    fun laneClicked(lane: Int) {
        if (laneStat[lane]) {
            println("Lane $lane Opened")
            laneStat[lane] = false
            val marker = MarkerObj("LO", lane.toString())
            markerSubject.value = marker
        }
        else {
            println("Lane $lane Closed")
            laneStat[1] = true
            val marker = MarkerObj("LC", lane.toString())
            markerSubject.value = marker
        }
    }
}