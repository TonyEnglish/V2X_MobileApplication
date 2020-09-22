package com.wzdctool.android.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.wzdctool.android.dataclasses.CSVObj
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.dataclasses.SecondFragmentUIObj

object DataClassesRepository {
    val csvDataSubject = MutableLiveData<CSVObj>()
    val dataCollectionSubject = MutableLiveData<DataCollectionObj>()
    val secondFragmentUISubject = MutableLiveData<SecondFragmentUIObj>()
    val markerSubject = MutableLiveData<MarkerObj>()
    val locationSubject = MutableLiveData<Location>()
    val dataLoggingSubject = MutableLiveData<Boolean>(false)
}