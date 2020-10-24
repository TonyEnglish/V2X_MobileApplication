package com.wzdctool.android

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.wzdctool.android.repos.ConfigurationRepository.activeWZIDSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.services.LocationService
import com.wzdctool.android.services.UsbService
import com.wzdctool.android.services.UsbService.UsbBinder
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val premissionRequestCode = 100
    private var usbService: UsbService? = null
    private var display: TextView? = null
    private var editText: EditText? = null
    private var mHandler: MyHandler? = null
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
                UsbService.ACTION_USB_PERMISSION_GRANTED -> Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED -> Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_NO_USB -> Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_DISCONNECTED -> Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_NOT_SUPPORTED -> Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        // startLocationService()

        mHandler = MyHandler(this)
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

private class MyHandler(activity: MainActivity?) : Handler() {
    private val mActivity: WeakReference<MainActivity> = WeakReference(activity!!)
    private var prevLocation: Location = Location("")
    val formatter: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SS")
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                val data = msg.obj as String
                println(data)
                try {
                    if (data.length >= 7) {
                        val key = data.substring(3, 6)

                        val newLocation: Location?
                        var update = false

                        if (key == "RMC") {
                            newLocation = parseRMC(data, prevLocation)
                            update = true
                        }
                        else if (key == "GSA") {
                            newLocation = parseGSA(data, prevLocation)
                        }
                        else if (key == "GGA") {
                            newLocation = parseGGA(data, prevLocation)
                        }
                        else {
                            newLocation = null
                        }

                        if (update && newLocation != null) {
                            locationSubject.onNext(newLocation)
                            prevLocation = newLocation
                        }
                    }
                }
                catch (e:Exception) {
                    Toast.makeText(mActivity.get(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            UsbService.CTS_CHANGE -> Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show()
            UsbService.DSR_CHANGE -> Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show()
        }
    }

    private fun parseRMC(NMEAData: String, prevLocation: Location?): Location? {
        //#### ----------
        //       GNRMC,222218.000,A,3805.047687,N,12212.496518,W,0.02,224.41,201116,,,D
        //         0     1        2     3       4      5       6   7    8       9
        //       GPRMC,010046.000,A,3805.052482,N,12212.496245,W,0.00,354.54,061116,,
        //       Where:
        //       0     RMC           Recommended Minimum sentence C
        //       1     123519        Fix taken at 12:35:19 UTC
        //       2     A             Status A=active or V=Void. (Valid or Invalid)
        //       3,4   4807.038,N    Latitude 48 deg 07.038' N
        //       5,6   01131.000,E   Longitude 11 deg 31.000' E
        //       7     022.4         Speed over the ground in knots
        //       8     084.4         Track angle in degrees True
        //       9     230394        Date - 23rd of March 1994
        //       10,11 003.1,W       Magnetic Variation, Direction
        //       12    *6A           The checksum data, always begins with *
        //       Unhandled
        //##### ----------

        //##
        //       Constants...$GxRMC
        //##


        val RMCSTAT: Int = 2         // Status A=Valid, V=Invalid
        val RMCLAT: Int = 3         // Latitude in GGA
        val RMCLATNS: Int = 4         // Either N(+) or S(-)
        val RMCLON: Int = 5         // Longitude in GGA
        val RMCLONEW: Int = 6         // Either E(+) or W(-)
        //
        val RMCKNOTS: Int = 7         //Speed in Knots
        val RMCANGLE: Int = 8         //Direction angle
        val RMCDATE: Int = 9         //Date from GPRMC

        //////
        //       Break up the input sentence
        //       Check for RMCDATE
        //       Check for RMCSTAT, process the following only if Valid...
        //////

        val s = NMEAData.split(',')

        if (s[0] != "\$GPRMC" && s[0] != "\$GNRMC") {
            return null
        }

        if (s[RMCSTAT] != "A") {
            // Toast.makeText(mActivity.get(), "Invalid Fix", Toast.LENGTH_LONG).show()
            return null
        }
        // Toast.makeText(mActivity.get(), NMEAData, Toast.LENGTH_SHORT).show()

        // val GPSDate = "20${s[RMCDATE].substring(4, 6)}/${s[RMCDATE].substring(2, 4)}/${s[RMCDATE].substring(0, 2)}"
        val GPSDate: Long = Calendar.getInstance().getTime().time

        //////
        //       Get Latitude and convert to decimal degrees
        //////

        // var lats = 100.0
        val lats = s[RMCLAT].toDouble()
        var p1  = (lats / 100.0).toInt()
        var lat = (p1 + (lats - p1 * 100) / 60.0)
        if (s[RMCLATNS] == "S")
        {
            lat = -lat
        }
        val GPSLat = lat
//
//        //////
//        //       Get longitude and convert to decimal degrees
//        //////
//
        val lng = s[RMCLON].toDouble()
        //var lng = 100.0
//        if (s[RMCLON] != "") {
//            lng = s[RMCLON].toDouble()
//        }
//        val lon  = lats / 100.0;
        p1  = (lng / 100.0).toInt()
        var lon = (p1+(lng-p1*100)/60.0)
        if (s[RMCLONEW] == "W") {
            lon = -lon
        }
        val GPSLon = lon
//
//        //////
//        //       Get speed and heading...
//        //////
//
        val GPSSpeed = s[RMCKNOTS].toFloat()       // Speed in Knots
        var GPSHeading: Float = 0.0f
        if (s[RMCANGLE] != "") {
            GPSHeading = s[RMCANGLE].toFloat()
        }
//        val angleString = s[RMCANGLE]
//        val GPSHeading = s[RMCANGLE].toFloat() // Direction angle
//
        var newLocation = prevLocation
        if (newLocation == null) {
            newLocation = Location("")
        }


        newLocation!!.latitude = GPSLat
        newLocation.longitude = GPSLon
        newLocation.speed = GPSSpeed
        newLocation.bearing = GPSHeading
        newLocation.latitude = GPSLat
        newLocation.time = GPSDate
        // Toast.makeText(mActivity.get(), "Valid Fix", Toast.LENGTH_LONG).show()
        // Toast.makeText(mActivity.get(), "$GPSLat, $GPSLon; $GPSSpeed; $GPSHeading", Toast.LENGTH_SHORT).show() //$NMEAData;${s[RMCLAT]},${s[RMCLON]} $GPSSpeed;  "$GPSLat, $GPSLon; $GPSSpeed"

        //////
        //   Return to caller with value...
        // Toast.makeText(mActivity.get(), "${newLocation.latitude}, ${newLocation.longitude}, ${newLocation.accuracy}", Toast.LENGTH_SHORT).show()

        return newLocation
    }

    private fun parseGSA(NMEAData: String, prevLocation: Location?): Location? {
        //#### ----------
        //       GPGSA,A,3,17,28,19,06,01,03,22,24,51,30,11,,1.79,0.98,1.50*09
        //          0  1 2  3  4  5  6  7  8  9 10 11 12 13   15   16   17
        //       GNGSA,A,3,67,66,76,82,77,83,68,,,,,,1.2,0.7,1.0
        //       GSA     Satellite status
        //       1 -     A       Auto selection of 2D or 3D fix (M = manual)
        //       2 -     3       3D fix - values include: 1 = no fix
        //                       2 = 2D fix
        //                       3 = 3D fix
        //       4-5...  PRNs of satellites used for fix (space for 12)
        //       15 -    1.79    PDOP (dilution of precision)
        //       16 -    0.98    Horizontal dilution of precision (HDOP)
        //       17 -    1.50    Vertical dilution of precision (VDOP)
        //       *39     the checksum data, always begins with *
        //##### ----------

        val GSAStat: Int = 2         // Status A=Valid, V=Invalid
        val GSAHDOP: Int = 16         // Latitude in GGA

        val s = NMEAData.split(',')

        if (s[GSAStat].toInt() <= 1) {
            // Toast.makeText(mActivity.get(), "Invalid Fix", Toast.LENGTH_LONG).show()
            return null
        }

        val GPSHdop = s[GSAHDOP].toFloat()       // Accuracy in meters

        var newLocation = prevLocation
        if (newLocation == null) {
            newLocation = Location("")
        }


        newLocation!!.accuracy = GPSHdop
        // Toast.makeText(mActivity.get(), "$GPSHdop", Toast.LENGTH_SHORT).show() //$NMEAData;${s[RMCLAT]},${s[RMCLON]} $GPSSpeed;  "$GPSLat, $GPSLon; $GPSSpeed"
        // Toast.makeText(mActivity.get(), "${newLocation.latitude}, ${newLocation.longitude}, ${newLocation.accuracy}", Toast.LENGTH_SHORT).show()

        return newLocation
    }

    private fun parseGGA(NMEAData: String, prevLocation: Location?): Location? {
        //#### ----------
        //
        //       Here's the $GxGGA sentence decoding logic.
        //
        //       If there was a checksum problem (missing or mismatch), NMEAData is cleared.
        //       That will cause all processing to be skipped because there will be no match
        //       in the first 5 columns.
        //
        //       GPGGA,010049.000,3805.0524,N,12212.4962,W,2,18,0.7,75.3,M,-24.5,M,0000,0000*47
        //         0         1        2     3      4     5 6  7  8    9  10  11  12 13   14
        //       GxGGA        Global Positioning System Fix Data
        //       1   - 123519.000    Fix taken at 12:35:19 UTC
        //       2,3 - 4807.038,N    Latitude 48 deg 07.038' N
        //       4,5 - 01131.000,E   Longitude 11 deg 31.000' E
        //       6   - Fix quality:  0 = invalid
        //                           1 = GPS fix (SPS)
        //                           2 = DGPS fix
        //                           3 = PPS fix
        //                           4 = Real Time Kinematic
        //			    5 = Float RTK
        //                           6 = estimated (dead reckoning) (2.3 feature)
        //	        	    7 = Manual input mode
        //			    8 = Simulation mode
        //       7     - 18          Number of satellites being tracked
        //       8     - 0.7         Horizontal dilution of position
        //       9,10  - 75.3,M      Altitude, Meters, above mean sea level
        //       11,12 - -24.5,M     Height of geoid (mean sea level) above WGS84 ellipsoid
        //       13    -             (empty field)   time in seconds since last DGPS update
        //       14    -             (empty field) DGPS station ID number
        //       *47   -             the checksum data, always begins with *
        //
        //##### ----------

        val GGAGMT: Int         = 1         // Time in GMT in GGA
        val GGAFIXQUAL: Int     = 6         // Fix Quality
        val GGASATS: Int        = 7         // # of satellites
        val GGAALT: Int         = 9         // Altitude
        val GGAALTUN: Int       = 10         // Altitude Units

        val s = NMEAData.split(',')

        if (s[0] != "\$GPRMC" && s[0] != "\$GNRMC") {
            return null
        }

        if (s[GGAFIXQUAL].toDouble() < 0.0) {
            // Toast.makeText(mActivity.get(), "Invalid Fix", Toast.LENGTH_LONG).show()
            return null
        }


        // val GPSTime = s[GGAGMT][0:2]+":"+s[GGAGMT][2:4]+":"+s[GGAGMT][4:6]+":"+s[GGAGMT][7:9]

        //##
        //       Get # of satellites
        //##
        val GPSSats = s[GGASATS].toInt()

        //##
        //       Get altitude in meters
        //##
        val GPSAlt      = s[GGAALT].toDouble()
//
        var newLocation = prevLocation
        if (newLocation == null) {
            newLocation = Location("")
        }


        newLocation!!.altitude = GPSAlt

//        var extras = newLocation.extras
//        extras["sats"] =
        // GPSLocation!!.time =

        // Toast.makeText(mActivity.get(), "$GPSAlt", Toast.LENGTH_SHORT).show() //$NMEAData;${s[RMCLAT]},${s[RMCLON]} $GPSSpeed;  "$GPSLat, $GPSLon; $GPSSpeed"
        // Toast.makeText(mActivity.get(), "${newLocation.latitude}, ${newLocation.longitude}, ${newLocation.accuracy}", Toast.LENGTH_SHORT).show()

        return newLocation
    }
}