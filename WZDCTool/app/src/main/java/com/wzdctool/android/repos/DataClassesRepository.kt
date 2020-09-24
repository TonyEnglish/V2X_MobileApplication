package com.wzdctool.android.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.wzdctool.android.dataclasses.CSVObj
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.dataclasses.SecondFragmentUIObj
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subjects.Subject


object DataClassesRepository {
    val dataCollectionSubject = MutableLiveData<DataCollectionObj>()
    val secondFragmentUISubject = MutableLiveData<SecondFragmentUIObj>()
    // val markerSubject = ObservableField<MarkerObj>()
    val locationSubject = BehaviorSubject.create<Location>()
    val dataLoggingSubject = BehaviorSubject.create<Boolean>(false)

    val notificationSubject = BehaviorSubject.create<String>()
    // val dataLoggingSubject = PublishSubject.createDefault<Boolean>() // MutableLiveData<Boolean>(false)

    val gotRPSubject = MutableLiveData<Boolean> (false)
}