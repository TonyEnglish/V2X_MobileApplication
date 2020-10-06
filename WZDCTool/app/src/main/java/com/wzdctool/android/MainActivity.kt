package com.wzdctool.android

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.maps.SupportMapFragment
import com.wzdctool.android.repos.ConfigurationRepository.activeWZIDSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.services.LocationService


class MainActivity : AppCompatActivity() {
    private val premissionRequestCode = 100

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

//        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }

        notificationSubject.subscribe {
            val mySnackbar = Snackbar.make(
                findViewById(R.id.toolbar),
                it,
                5000
            )
            mySnackbar.show()
        }

        val configObserver = Observer<String> {
            findViewById<TextView>(R.id.activeConfigTextView).text = "Active Config: $it"
        }
        activeWZIDSubject.observe(this, configObserver)

        Constants.CONFIG_DIRECTORY = filesDir.toString()
        Constants.DATA_FILE_DIRECTORY = filesDir.toString()
        Constants.DOWNLOAD_LOCTION = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()

        startLocationService()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
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
}