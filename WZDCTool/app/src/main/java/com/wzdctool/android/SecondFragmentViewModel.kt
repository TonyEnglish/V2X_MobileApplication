package com.wzdctool.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.dataclasses.Coordinate
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.dataclasses.SecondFragmentUIObj
import com.wzdctool.android.services.LocationService

class SecondFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel\

    // Required parameters
    lateinit var localDataObj: DataCollectionObj
    //DataCollectionObj (val num_lanes: Int, val start_coord: Coordinate,
    //                         val end_coord: Coordinate, val speed_limits: SPEEDLIMITS,
    //                         val automatic_detection: Boolean)

    //
    var laneStat = MutableLiveData<List<Boolean>>(List<Boolean>(8+1) {false})
    var wpStat = MutableLiveData<Boolean>(false)
    var currWpStat = false
    var dataLog = MutableLiveData<Boolean>(false)
    var gotRP = MutableLiveData<Boolean>(false)
    // var

    val currentUIObj: MutableLiveData<SecondFragmentUIObj> by lazy {
        MutableLiveData<SecondFragmentUIObj>()
    }

    fun firstTimeSetup() {
        // laneStat.setValue(currLaneStat)
    }

    fun initializeUI(data_obj: DataCollectionObj) {
        var ui_obj = mapDataToUIObj(data_obj)
        // LocationService
        // SecondFragment().setUI(ui_obj)
    }

    fun mapDataToUIObj(data_obj: DataCollectionObj): SecondFragmentUIObj {
        val ui_obj = SecondFragmentUIObj(
            num_lanes = data_obj.num_lanes,
            laneStat = laneStat.value!!,
            wpStat = wpStat.value!!,
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
}