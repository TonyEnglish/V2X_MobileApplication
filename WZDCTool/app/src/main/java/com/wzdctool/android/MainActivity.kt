package com.wzdctool.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.wzdctool.android.dataclasses.AzureInfoObj
import com.wzdctool.android.dataclasses.gps_status
import com.wzdctool.android.dataclasses.gps_type
import com.wzdctool.android.handlers.UsbHandler
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.automaticUploadSubject
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingVar
import com.wzdctool.android.repos.DataClassesRepository.internetStatusSubject
import com.wzdctool.android.repos.DataClassesRepository.isInternetAvailable
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.longToastNotificationSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataClassesRepository.rsmStatus
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.AzureInfoRepository.currentAzureInfoSubject
import com.wzdctool.android.repos.AzureInfoRepository.updateConnectionStringFromObj
import com.wzdctool.android.services.LocationService
import com.wzdctool.android.services.UsbService
import com.wzdctool.android.services.UsbService.UsbBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rx.Subscription


class MainActivity : AppCompatActivity() {
    private val premissionRequestCode = 100
    private var usbService: UsbService? = null
    private var display: TextView? = null
    private var editText: EditText? = null
    private var mHandler: UsbHandler? = null
    private val locationCheckHandler: Handler = Handler(Looper.getMainLooper())
    private var isGPSConnected: Boolean = false
    private var prevTime: Long? = null
    private var currTime: Long? = null
    private val subscriptions: MutableList<Subscription> = mutableListOf()
    private lateinit var lm: LocationManager
    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbBinder).service
            usbService!!.setHandler(mHandler)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private val locationCheckRunnable: Runnable = object : Runnable {
        override fun run() {
            var updated = false
            val localLocationSources = locationSourcesSubject.value

            // Check if location updated since last check
            if (prevTime != null && currTime != null) {
                if (prevTime == currTime) {
                    // Invalidate current location source
                    val activeSource = activeLocationSourceSubject.value
                    if (activeSource != gps_type.none) {
                        if (activeSource == gps_type.internal) { // && localLocationSources.internal == gps_status.valid
                            updated = true
                            localLocationSources.internal = gps_status.invalid
                        }
                        else if (activeSource == gps_type.usb) { // && localLocationSources.usb == gps_status.valid
                            updated = true
                            localLocationSources.usb = gps_status.invalid
                        }
                        else {
                            TODO("Unknown Location Source")
                        }
                    }
                }
            }

            if (updated) {
                locationSourcesSubject.onNext(localLocationSources)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val internetStatus = isInternetAvailable()
                runOnUiThread {
                    if (internetStatusSubject.value != internetStatus) {
                        internetStatusSubject.onNext(internetStatus)
                    }
                }
            }

            prevTime = currTime

            locationCheckHandler.postDelayed(this, 4000)
        }
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED -> {
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show()
                    val localLocationSources = locationSourcesSubject.value
                    localLocationSources.usb = gps_status.invalid
                    locationSourcesSubject.onNext(localLocationSources)
                }
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED -> Toast.makeText(
                    context,
                    "USB Permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                UsbService.ACTION_NO_USB -> Toast.makeText(
                    context,
                    "No USB connected",
                    Toast.LENGTH_SHORT
                ).show()
                UsbService.ACTION_USB_DISCONNECTED -> {
                    // if (activeLocationSourceSubject.value == Constants.LOCATION_SOURCE_USB)
                    val localLocationSources = locationSourcesSubject.value
                    localLocationSources.usb = gps_status.disconnected
                    locationSourcesSubject.onNext(localLocationSources)
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                }
                UsbService.ACTION_USB_NOT_SUPPORTED -> Toast.makeText(
                    context,
                    "USB device not supported",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        lm = (this.getSystemService(Context.LOCATION_SERVICE) as LocationManager);

//        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
////            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                    .setAction("Action", null).show()
////        }


//        var cm = (this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
//        var netInfo = cm.activeNetworkInfo
//
//        //should check null because in airplane mode it will be null
//        var nc = cm.getNetworkCapabilities(cm.activeNetwork)
//        var downSpeed = nc.linkDownstreamBandwidthKbps
//        var upSpeed = nc.linkUpstreamBandwidthKbps

        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var isWifiConn: Boolean = false
        var isMobileConn: Boolean = false
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network).apply {
                if (type == ConnectivityManager.TYPE_WIFI) {
                    isWifiConn = isWifiConn or isConnected
                }
                if (type == ConnectivityManager.TYPE_MOBILE) {
                    isMobileConn = isMobileConn or isConnected
                }
            }
        }
        Log.d("NetworkStatusExample", "Wifi connected: $isWifiConn")
        Log.d("NetworkStatusExample", "Mobile connected: $isMobileConn")


        subscriptions.add(notificationSubject.subscribe {
            val mySnackbar = Snackbar.make(
                findViewById(R.id.toolbar),
                it,
                5000
            )
            mySnackbar.show()
        })

        subscriptions.add(toastNotificationSubject.subscribe {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

        subscriptions.add(longToastNotificationSubject.subscribe {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        })

        Constants.CONFIG_DIRECTORY = getExternalFilesDir("Configuration_Files").toString() //"${filesDir}/config"
        Constants.DATA_FILE_DIRECTORY = filesDir.toString()
        Constants.PENDING_UPLOAD_DIRECTORY = getExternalFilesDir("Pending_Uploads").toString()
        Constants.RECENT_WZ_MAPS = getExternalFilesDir("WZ_MAPS").toString()

        startLocationService()
        DataFileRepository.initializeObservers()

        mHandler = UsbHandler()

        subscriptions.add(locationSubject.subscribe {
            currTime = it?.time
            if (rsmStatus.value && it.accuracy > 2) {
                rsmStatus.onNext(false)
            } else if (!dataLoggingVar && it.accuracy <= 2) {
                rsmStatus.onNext(true)
            }
        })

        subscriptions.add(locationSourcesSubject.subscribe { locationSources ->
            if (locationSources.internal == gps_status.valid && locationSources.usb != gps_status.valid) {
                activeLocationSourceSubject.onNext(gps_type.internal)
            } else if (locationSources.usb == gps_status.valid) {
                activeLocationSourceSubject.onNext(gps_type.usb)
            } else if (locationSources.internal != gps_status.valid && locationSources.usb != gps_status.valid) {
                activeLocationSourceSubject.onNext(gps_type.none)
            }
        })


//        activeLocationSourceSubject.subscribe {
//            toastNotificationSubject.onNext(it.toString())
////            toastNotificationSubject.onNext(it)
//        }

        subscriptions.add(locationSubject.subscribe {
            Log.v(
                "LocationService", "Lat: ${it.latitude}, " +
                        "Lon: ${it.longitude}, " + "elevation: ${it.altitude}, " +
                        "accuracy: ${it.accuracy}"
            )
        })

        // TODO: Do not re-save values to saved preferences on initial load
        subscriptions.add(currentAzureInfoSubject.subscribe {
            updateConnectionStringFromObj(it)
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(getString(R.string.preference_account_name), it.account_name)
                putString(getString(R.string.preference_account_key), it.account_key)
                apply()
            }
        })

        subscriptions.add(automaticUploadSubject.subscribe {
            println("Saving Upload Status: $it")
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.preference_upload), it)
                apply()
            }
        })

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        if (sharedPref != null) {
            val accountName = sharedPref.getString(
                resources.getString(R.string.preference_account_name),
                null
            )
            val accountKey = sharedPref.getString(
                resources.getString(R.string.preference_account_key),
                null
            )

            if (accountName != null && accountKey != null) {
                currentAzureInfoSubject.onNext(AzureInfoObj(accountName, accountKey))
            }

            val automaticUpload = sharedPref.getBoolean(
                resources.getString(R.string.preference_upload),
                false
            )
            println("Upload Status: $automaticUpload")
            automaticUploadSubject.onNext(false) //automaticUpload
        }

//        toolbarActiveSubject.subscribe {
//            if (it) {
//                findViewById<LinearLayout>(R.id.toolbar_stuffs).visibility = View.VISIBLE
//                findViewById<LinearLayout>(R.id.gps_ll).visibility = View.VISIBLE
//                findViewById<LinearLayout>(R.id.checkbox_ll).visibility = View.VISIBLE
//                // findViewById<ConstraintLayout>(R.id.toolbar_stuffs).layoutParams.height = 90
//            }
//            else {
//                findViewById<LinearLayout>(R.id.toolbar_stuffs).visibility = View.GONE
//                findViewById<LinearLayout>(R.id.gps_ll).visibility = View.GONE
//                findViewById<LinearLayout>(R.id.checkbox_ll).visibility = View.GONE
//                // findViewById<ConstraintLayout>(R.id.toolbar_stuffs).layoutParams.height = 0
//            }
//        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        setFilters() // Start listening notifications from UsbService
        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // You can use the API that requires the permission.
                // performAction(...)
            }
            else -> {
                // You can directly ask for the permission.
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    premissionRequestCode
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        locationCheckHandler.post(locationCheckRunnable)
        checkLocationEnabled()
//        setFilters() // Start listening notifications from UsbService
//        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
    }

    override fun onPause() {
        super.onPause()
        locationCheckHandler.removeCallbacks(locationCheckRunnable)
//        unregisterReceiver(mUsbReceiver)
//        unbindService(usbConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
        DataFileRepository.removeObservers()
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            premissionRequestCode -> {
                // If request is cancelled, the result arrays are empty.
//                if ((grantResults.isNotEmpty() &&
//                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                    // Permission is granted. Continue the action or workflow
//                    // in your app.
//                } else {
//                    // TODO: Notify user that application will not function
//                    // Explain to the user that the feature is unavailable because
//                    // the features requires a permission that the user has denied.
//                    // At the same time, respect the user's decision. Don't link to
//                    // system settings in an effort to convince the user to change
//                    // their decision.
//                }
                return
            }
        }
    }

    //override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.menu_main, menu)
        //return true
    //}

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                println("SETTINGS")
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun checkLocationEnabled() {
        var gps_enabled = false;
        var network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (ex: Exception) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (ex: Exception) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder(this)
                .setMessage("Location Disabled")
                .setPositiveButton("Open Location Settings") { _: DialogInterface, _: Int ->
                    this.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
                .setNegativeButton("Cancel") { _: DialogInterface, _: Int ->
                    val localLocationSources = locationSourcesSubject.value
                    localLocationSources.internal = gps_status.invalid
                    locationSourcesSubject.onNext(localLocationSources)
                }
                .show();
        }
//        else {
//            val localLocationSources = locationSourcesSubject.value
//            localLocationSources.internal = gps_status.valid
//            locationSourcesSubject.onNext(localLocationSources)
//        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        // TODO: Update from deprecated function
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (LocationService::class.java.name == service.service.className) {
                println("Location service running")
                return true
            }
        }
        println("Location service not running")
        return false
    }

    private fun startLocationService() {
        println("Starting location service Activity")
        if (!isLocationServiceRunning()) {
            val intent: Intent = Intent(applicationContext, LocationService::class.java)
            intent.action = Constants.ACTION_START_LOCATION_SERVICE
            startService(intent)
        }
    }

    private fun stopLocationService() {
        println("Stopping location service")
        if (isLocationServiceRunning()) {
            val intent: Intent = Intent(applicationContext, LocationService::class.java)
            intent.action = Constants.ACTION_STOP_LOCATION_SERVICE
            startService(intent)
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startService(
        service: Class<*>,
        serviceConnection: ServiceConnection,
        extras: Bundle?
    ) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(this, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            startService(startService)
        }
        val bindingIntent = Intent(this, service)
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)
    }
}

