package com.wzdctool.android

import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.ConfigurationRepository.activeConfigSubject
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.dataCollectionSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirstFragmentViewModel : ViewModel() {

    var automaticDetection = false

    fun updateConfigList() {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateConfigList()
            // ConfigurationRepository.activateConfig("config--road-name--description.json")
        }
    }

    fun activateConfig(configName: String, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.activateConfig(configName, filePath)
            // ConfigurationRepository.activateConfig("config--road-name--description.json")
        }
    }

    fun updateDataCollectionObj() {
        val data_obj = DataCollectionObj(
            num_lanes = activeConfigSubject.value!!.LaneInfo.NumberOfLanes,
            start_coord = activeConfigSubject.value!!.Location.BeginningLocation,
            end_coord = activeConfigSubject.value!!.Location.EndingLocation,
            speed_limits = activeConfigSubject.value!!.SpeedLimits,
            automatic_detection = automaticDetection)
        dataCollectionSubject.value = data_obj
    }
}