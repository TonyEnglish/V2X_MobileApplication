package com.wzdctool.android.handlers

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.gps_status
import com.wzdctool.android.dataclasses.gps_type
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.services.UsbService
import java.text.SimpleDateFormat
import java.util.*

class UsbHandler : Handler() {
    // private val mActivity: WeakReference<MainActivity> = WeakReference(activity!!)
    private var prevLocation: Location = Location("")
    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SS")
    private val formatParser: SimpleDateFormat = SimpleDateFormat("HHmmss.SS-ddMMyy")
    private var isFirstTime = true
    private var locationSource: String = ""
    private var locationSources: MutableList<String> = mutableListOf()
    private var usbGpsStatus_local: String = ""
    private var isUsbLocationValid: Boolean = false

    fun usbDisconnected() {
        isFirstTime = true
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                parseGPSMessage(msg)
            }
//            UsbService.CTS_CHANGE -> Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show()
//            UsbService.DSR_CHANGE -> Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show()
        }
    }

    private fun parseGPSMessage(msg: Message) {
        val data = msg.obj as String
        try {
            if (data.length >= 7) {
                val key = data.substring(3, 6)

                val newLocation: Location?
                var update = false
                var isValid = true

                if (key == "RMC") {
                    newLocation = parseRMC(data, prevLocation)
                    if (newLocation != null) {
                        if (newLocation.bearing == 0f) {
                            isValid = false
//                            DataClassesRepository.notificationSubject.onNext("RMC Invalid")
                        }
                        update = true   // Only update when RMC line is parsed
                    }
                }
                else if (key == "GSA") {
                    newLocation = parseGSA(data, prevLocation)
                }
                else if (key == "GGA") {
                    newLocation = parseGGA(data, prevLocation)
                    if (newLocation != null) {
                        if (newLocation.altitude == 0.0) {
                            isValid = false
//                            DataClassesRepository.notificationSubject.onNext("GGA Invalid")
                        }
                    }
                }
                else {
                    newLocation = null
                }

                if (update && newLocation != null && isValid) {
                    val localLocationSources = locationSourcesSubject.value
                    if (localLocationSources.usb != gps_status.valid) {
                        localLocationSources.usb = gps_status.valid
                        locationSourcesSubject.onNext(localLocationSources)
                        toastNotificationSubject.onNext("USB GPS Valid")
                    }
                    if (activeLocationSourceSubject.value == gps_type.usb) {
                        DataClassesRepository.locationSubject.onNext(newLocation)
                    }
                    prevLocation = newLocation
                }
            }
        }
        catch (e:Exception) {
            // DataClassesRepository.notificationSubject.onNext("Error: ${e.message}")
            // Toast.makeText(mActivity.get(), "Error: ${e.message}. ${e.stackTrace}", Toast.LENGTH_LONG).show()
        }
    }

    private fun parseRMC(NMEAData: String, prevLocation: Location?): Location? {
        //#### ----------
        //       GNRMC,222218.000,A,3805.047687,N,12212.496518,W,0.02,224.41,201116,,,D
        //         0     1        2     3       4      5       6   7    8       9
        //       GPRMC,010046.000,A,3805.052482,N,12212.496245,W,0.00,354.54,061116,,
        //       Where:
        //       0     RMC           Recommended Minimum sentence C
        //       1     123519.000        Fix taken at 12:35:19.000 UTC
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

        val RMCTIME: Int = 1        // Time from GPRMC
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

        if (s.size < 9) {
            return null
        }
        else if (s[RMCSTAT] != "A") {
            return null
        }

        val timeString = s[RMCTIME] + '-' + s[RMCDATE]
        val date: Date? = formatParser.parse(timeString)
        val GPSDate = date?.time ?: Calendar.getInstance().time.time

        //////
        //       Get Latitude and convert to decimal degrees
        //////

        val lats = s[RMCLAT].toDoubleOrNull() ?: return null
        var p1  = (lats / 100.0).toInt()
        var lat = (p1 + (lats - p1 * 100) / 60.0)
        if (s[RMCLATNS] == "S")
        {
            lat = -lat
        }
        val GPSLat = lat

//        //////
//        //       Get longitude and convert to decimal degrees
//        //////

        val lng = s[RMCLON].toDoubleOrNull() ?: return null
        p1  = (lng / 100.0).toInt()
        var lon = (p1+(lng-p1*100)/60.0)
        if (s[RMCLONEW] == "W") {
            lon = -lon
        }
        val GPSLon = lon

//        //////
//        //       Get speed and heading...
//        //////

        val GPSSpeed = s[RMCKNOTS].toFloatOrNull() ?: return null      // Speed in Knots

        val GPSHeading = s[RMCANGLE].toFloatOrNull()


        val newLocation = prevLocation ?: Location("")

        newLocation.latitude = GPSLat
        newLocation.longitude = GPSLon
        newLocation.speed = GPSSpeed
        if (GPSHeading != null) {
            newLocation.bearing = GPSHeading
        }
        newLocation.time = GPSDate

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

        if (s.size < 16) {
            return null
        }
        else if (s[GSAStat].toIntOrNull() ?: 0 <= 1) {
            return null
        }

        val GPSHdop = s[GSAHDOP].toFloatOrNull() ?: return null      // Accuracy in meters

        val newLocation = prevLocation ?: Location("")

        newLocation.accuracy = GPSHdop

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

        if (s.size < 10) {
            return null
        }
        else if (s[GGAFIXQUAL].toDoubleOrNull() ?: -1.0 <= 0.0) {
            return null
        }

        //##
        //       Get # of satellites
        //##
        val GPSSats = s[GGASATS].toIntOrNull() ?: return null

        //##
        //       Get altitude in meters
        //##
        val GPSAlt = s[GGAALT].toDoubleOrNull()

        val newLocation = prevLocation ?: Location("")

        if (GPSAlt != null) {
            newLocation.altitude = GPSAlt
        }
        newLocation.extras = Bundle()
        newLocation.extras.putInt("satellites", GPSSats)

        return newLocation
    }
}