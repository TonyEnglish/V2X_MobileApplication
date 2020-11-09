package com.wzdctool.android

import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.ConfigurationRepository.activeConfigSubject
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.dataCollectionObj
import com.wzdctool.android.repos.DataClassesRepository.dataCollectionSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirstFragmentViewModel : ViewModel() {

    var automaticDetection = true

    fun updateConfigList() {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateConfigList()
        }
    }

    fun activateConfig(configName: String, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success: Boolean = ConfigurationRepository.activateConfig(configName, filePath)
//            if (!success) {
//                DataClassesRepository.notificationSubject.onNext("Config Download Failed")
//            }
        }
    }

    fun updateDataCollectionObj() {
        println("Automatic Detection: $automaticDetection")
        val data_obj = DataCollectionObj(
            num_lanes = activeConfigSubject.value!!.LaneInfo.NumberOfLanes,
            start_coord = activeConfigSubject.value!!.Location.BeginningLocation,
            end_coord = activeConfigSubject.value!!.Location.EndingLocation,
            speed_limits = activeConfigSubject.value!!.SpeedLimits,
            automatic_detection = automaticDetection,
            data_lane = activeConfigSubject.value!!.LaneInfo.VehiclePathDataLane)
        dataCollectionObj = data_obj
    }
}