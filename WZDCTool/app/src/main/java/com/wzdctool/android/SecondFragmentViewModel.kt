package com.wzdctool.android

import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.wzdctool.android.dataclasses.*
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingSubject
import com.wzdctool.android.repos.DataClassesRepository.gotRPSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.DataFileRepository.markerSubject
import com.wzdctool.android.services.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

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
//    var dataLog = MutableLiveData<Boolean>(false)
//    var gotRP = MutableLiveData<Boolean>(false)
    var notificationText = MutableLiveData<String>()
    // var

//    val currentUIObj: MutableLiveData<SecondFragmentUIObj> by lazy {
//        MutableLiveData<SecondFragmentUIObj>()
//    }


    fun firstTimeSetup() {
        // laneStat.setValue(currLaneStat)
    }

    fun initializeUI(data_obj: DataCollectionObj) {
        localUIObj = mapDataToUIObj(data_obj)
    }

    private fun mapDataToUIObj(data_obj: DataCollectionObj): SecondFragmentUIObj {
        val ui_obj = SecondFragmentUIObj(
            num_lanes = data_obj.num_lanes,
            laneStat = laneStat,
            wpStat = wpStat,
            dataLog = dataLoggingSubject.value!!,
            gotRP = gotRPSubject.value!!,
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
        if (!laneStat[lane]) {
            println("Lane $lane Opened")
            laneStat[lane] = true
            val marker = MarkerObj("LO", lane.toString())
            markerSubject.onNext(marker)
        }
        else {
            println("Lane $lane Closed")
            laneStat[1] = false
            val marker = MarkerObj("LC", lane.toString())
            markerSubject.onNext(marker)
        }
    }

    fun uploadDataFile(fileName: String) {

        viewModelScope.launch(Dispatchers.IO) {
            DataFileRepository.dataFileName =
                "path-data--${ConfigurationRepository.activeWZIDSubject.value}.csv"
            val output = DataFileRepository.uploadPathDataFile(
                fileName,
                DataFileRepository.dataFileName
            )
            println(output)
            notificationSubject.onNext("Path data file uploaded")
        }
    }
}