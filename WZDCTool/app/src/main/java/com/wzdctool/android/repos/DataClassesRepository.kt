package com.wzdctool.android.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.*
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subjects.Subject


object DataClassesRepository {
    val dataCollectionSubject: PublishSubject<DataCollectionObj> = PublishSubject.create<DataCollectionObj>()
    lateinit var dataCollectionObj: DataCollectionObj
    // val secondFragmentUISubject = MutableLiveData<SecondFragmentUIObj>()
    // val markerSubject = ObservableField<MarkerObj>()


    ////// LOCATION
    val locationSubject: PublishSubject<Location> = PublishSubject.create<Location>()

    val activeLocationSourceSubject: BehaviorSubject<gps_type> = BehaviorSubject.create<gps_type>()

    val locationSourcesSubject: BehaviorSubject<locationSources> = BehaviorSubject.create<locationSources>(locationSources())

    val rsmStatus: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(true)


    var dataLoggingVar = false
    // val dataLoggingSubject = BehaviorSubject.create<Boolean>(false)

    val toolbarActiveSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(false)

    val notificationSubject: PublishSubject<String> = PublishSubject.create<String>()
    val toastNotificationSubject: PublishSubject<String> = PublishSubject.create<String>()
    // val dataLoggingSubject = PublishSubject.createDefault<Boolean>() // MutableLiveData<Boolean>(false)

    // val gotRPSubject = BehaviorSubject.create<Boolean>(false)
}