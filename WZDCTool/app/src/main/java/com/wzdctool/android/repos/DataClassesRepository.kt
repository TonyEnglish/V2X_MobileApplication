package com.wzdctool.android.repos

import android.location.Location
import android.net.ConnectivityManager
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.dataclasses.VisualizationObj
import com.wzdctool.android.dataclasses.gps_type
import com.wzdctool.android.dataclasses.locationSources
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.net.InetAddress


object DataClassesRepository {
    val dataCollectionSubject: PublishSubject<DataCollectionObj> = PublishSubject.create<DataCollectionObj>()
    lateinit var dataCollectionObj: DataCollectionObj
    lateinit var visualizationObj: VisualizationObj
    // val secondFragmentUISubject = MutableLiveData<SecondFragmentUIObj>()
    // val markerSubject = ObservableField<MarkerObj>()

    val automaticDetectionSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>()

    val internetStatusSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(false)

    val automaticUploadSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>()

    ////// LOCATION
    val locationSubject: PublishSubject<Location> = PublishSubject.create<Location>()

    val activeLocationSourceSubject: BehaviorSubject<gps_type> = BehaviorSubject.create<gps_type>()

    val locationSourcesSubject: BehaviorSubject<locationSources> = BehaviorSubject.create<locationSources>(
        locationSources()
    )

    val rsmStatus: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(true)


    var dataLoggingVar = false
    // val dataLoggingSubject = BehaviorSubject.create<Boolean>(false)

    val toolbarActiveSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(false)

    val notificationSubject: PublishSubject<String> = PublishSubject.create<String>()
    val toastNotificationSubject: PublishSubject<String> = PublishSubject.create<String>()
    val longToastNotificationSubject: PublishSubject<String> = PublishSubject.create<String>()
    // val dataLoggingSubject = PublishSubject.createDefault<Boolean>() // MutableLiveData<Boolean>(false)

    // val gotRPSubject = BehaviorSubject.create<Boolean>(false)

    fun isInternetAvailable(): Boolean {
        return try {
            val ipAddr: InetAddress = InetAddress.getByName("google.com")
            //You can replace it with your name
            !ipAddr.equals("")
        } catch (e: java.lang.Exception) {
            println(e.printStackTrace())
            false
        }
    }
}