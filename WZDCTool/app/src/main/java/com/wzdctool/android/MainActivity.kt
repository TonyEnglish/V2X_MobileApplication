package com.wzdctool.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.wzdctool.android.dataclasses.azureInfoObj
import com.wzdctool.android.handlers.UsbHandler
import com.wzdctool.android.repos.ConfigurationRepository.activeWZIDSubject
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingVar
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataClassesRepository.rsmStatus
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.DataClassesRepository.toolbarActiveSubject
import com.wzdctool.android.repos.DataClassesRepository.usbGpsStatus
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.azureInfoRepository.currentAzureInfoSubject
import com.wzdctool.android.repos.azureInfoRepository.currentConnectionStringSubject
import com.wzdctool.android.repos.azureInfoRepository.updateConnectionStringFromObj
import com.wzdctool.android.services.LocationService
import com.wzdctool.android.services.UsbService
import com.wzdctool.android.services.UsbService.UsbBinder
import rx.Subscription


class MainActivity : AppCompatActivity() {
    private val premissionRequestCode = 100
    private var usbService: UsbService? = null
    private var display: TextView? = null
    private var editText: EditText? = null
    private var mHandler: UsbHandler? = null
    private var isGPSConnected: Boolean = false
    private val subscriptions: MutableList<Subscription> = mutableListOf()
    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbBinder).service
            usbService!!.setHandler(mHandler)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED -> {
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show()
                    val locationSources = (locationSourcesSubject.value!! as MutableList<String>)
                    val added: Boolean = locationSources.add(Constants.LOCATION_SOURCE_USB)
                    if (added)
                        locationSourcesSubject.onNext(locationSources)
                    usbGpsStatus.onNext("invalid")
                    isGPSConnected = true
                }
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED -> Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_NO_USB -> Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_DISCONNECTED -> {
                    // if (activeLocationSourceSubject.value == Constants.LOCATION_SOURCE_USB)
                    val locationSources = (locationSourcesSubject.value!! as MutableList<String>)
                    val removed: Boolean = locationSources.remove(Constants.LOCATION_SOURCE_USB)
                    if (removed)
                        locationSourcesSubject.onNext(locationSources)
                    usbGpsStatus.onNext("disconnected")
                    activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
                    mHandler?.usbDisconnected()
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                }
                UsbService.ACTION_USB_NOT_SUPPORTED -> Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

//        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
////            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                    .setAction("Action", null).show()
////        }

        notificationSubject.subscribe {
            val mySnackbar = Snackbar.make(
                findViewById(R.id.toolbar),
                it,
                5000
            )
            mySnackbar.show()
        }

        toastNotificationSubject.subscribe {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        Constants.CONFIG_DIRECTORY = filesDir.toString()
        Constants.DATA_FILE_DIRECTORY = filesDir.toString()
        Constants.DOWNLOAD_LOCTION = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()

        startLocationService()
        DataFileRepository.initializeObservers()

        mHandler = UsbHandler()

        subscriptions.add(locationSubject.subscribe {
            if (rsmStatus.value && it.accuracy > 2) {
                rsmStatus.onNext(false)
            } else if (!dataLoggingVar && it.accuracy <= 2) {
                rsmStatus.onNext(true)
            }
        })

        subscriptions.add(locationSourcesSubject.subscribe {
            if (!it.contains(Constants.LOCATION_SOURCE_USB) && activeLocationSourceSubject.value!! == Constants.LOCATION_SOURCE_USB) {
                if (it.contains(Constants.LOCATION_SOURCE_INTERNAL)) {
                    activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
                }
            }
        })

        subscriptions.add(usbGpsStatus.subscribe {
            if (it == "valid") {
                activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_USB)
            } else if (it == "invalid") {
                activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
            } else if (it == "disconnected") {
                activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
            }
        })

        activeLocationSourceSubject.subscribe {
//            toastNotificationSubject.onNext(it)
        }

        // TODO: Do not re-save values to saved preferences on initial load
        currentAzureInfoSubject.subscribe {
            updateConnectionStringFromObj(it)
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString(getString(R.string.preference_account_name), it.account_name)
                putString(getString(R.string.preference_account_key), it.account_key)
                apply()
            }
            println(sharedPref.getString(resources.getString(R.string.preference_account_name), null))
            println(sharedPref.getString(resources.getString(R.string.preference_account_key), null))
        }

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        if (sharedPref != null) {
            println(sharedPref.getString(resources.getString(R.string.preference_account_name), null))
            println(sharedPref.getString(resources.getString(R.string.preference_account_key), null))
            val accountName = sharedPref.getString(resources.getString(R.string.preference_account_name), null)
            val accountKey = sharedPref.getString(resources.getString(R.string.preference_account_key), null)

            if (accountName != null && accountKey != null) {
                currentAzureInfoSubject.onNext(azureInfoObj(accountName, accountKey))
            }
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
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        premissionRequestCode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setFilters() // Start listening notifications from UsbService
        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
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
            R.id.action_settings ->  {
                println("SETTINGS")
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun locationSourceSwitchClicked() {
        if (findViewById<Switch>(R.id.switch1).isChecked) {
            // if (locationSource == Constants.LOCATION_SOURCE_USB)
            activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_USB)
        }
        else {
            activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
        }
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
        println("starting location service")
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

    private fun startService(service: Class<*>, serviceConnection: ServiceConnection, extras: Bundle?) {
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

