package com.wzdctool.android.ui.configuration

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

class ConfigurationFragmentViewModel : ViewModel() {

    var automaticDetection = false

    fun updateConfigList() {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateLocalConfigList()
        }
    }

    fun activateConfig(configName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success: Boolean = ConfigurationRepository.activateConfig(configName)
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