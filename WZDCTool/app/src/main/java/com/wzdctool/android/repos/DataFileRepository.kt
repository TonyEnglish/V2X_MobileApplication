package com.wzdctool.android.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.CSVObj
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.repos.ConfigurationRepository.activeConfigSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import rx.subjects.PublishSubject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object DataFileRepository {
    val csvDataSubject = MutableLiveData<CSVObj>()
    var markerSubject = PublishSubject.create<MarkerObj>()
    var dataFileSubject = PublishSubject.create<String>()

    private lateinit var dataPath: String
    private lateinit var osw: OutputStreamWriter
    private lateinit var prevLocation: Location
    lateinit var dataFileName: String
    var loggingData = false
    var isFirstTime = true
    val formatter: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SS")

    private var total_lanes: Int = 8

    private var lane_stat = MutableList<Boolean>(8+1) {false}
    private var wp_stat = false
    private var got_rp = false

    private var end_when_ready = false

    private var previousLine = ""

    // private val messages: MutableList<String> = mutableListOf()

    private val laneList: MutableList<String> = mutableListOf()
    // var laneList = List<Boolean>(total_lanes+1) {false}
    var line_num = 0

    private val markerList: List<String> = listOf("Data Log", "RP", "WP+RP", "LC+RP", "WP", "LC", "LO", "")
    private val markerValueDict: HashMap<String, List<String>> = hashMapOf("Data Log" to listOf("True", "False"), "RP" to listOf(""), "WP+RP" to listOf("True", "False"), "LC+RP" to laneList,
            "WP" to listOf("True", "False"), "LC" to laneList, "LO" to laneList, "" to listOf(""))


    // var marker: String = ""
    var markerQueue: Queue<String> = LinkedList<String>()
    // var markerValue: String = ""
    var markerValueQueue: Queue<String> = LinkedList<String>()

//    fun createDataFile() {
//        dataPath = "${Constants.DATA_FILE_DIRECTORY}/dataFile-${Calendar.getInstance().timeInMillis}.csv"
//        osw = createDataFile()
//    }

    fun getConfigName(wzId: String): String {
        return "config--$wzId.json"
    }

    fun initializeObservers() {
        synchronized(this) {
            if (isFirstTime) {
                isFirstTime = false
                markerSubject.subscribe {
                    if (loggingData) {
                        markerQueue.add(it.marker)
                        markerValueQueue.add(it.value)
                    }
                    else if (it.marker == "Data Log" && it.value == "True") {
                        markerQueue.clear()
                        markerQueue.add(it.marker)
                        markerValueQueue.clear()
                        markerValueQueue.add(it.value)
                        createDataFile()
                        toastNotificationSubject.onNext("Logging Data")
                        loggingData = true
                        // val csvObj = createCSVObj(prevLocation)
                    }
                }

                locationSubject.subscribe {
                    val csvObj = createCSVObj(it)
                    if (loggingData) {
                        writeToDataFile(csvObj)
                    }
                    prevLocation = it
                }
            }
        }
    }

    private fun createCSVObj(location: Location): CSVObj {
        val marker: String = if (markerQueue.size >= 1) markerQueue.remove() else ""
        val markerValue: String = if (markerValueQueue.size >= 1) markerValueQueue.remove() else ""
        val csvObj = CSVObj(
            Date(location.time), location.extras.getInt("satellites"), location.accuracy,
            location.latitude, location.longitude, location.altitude, location.speed,
            location.bearing, marker, markerValue, false
        )
        return csvObj
    }


    private fun createDataFile() {
        dataPath = "${Constants.DATA_FILE_DIRECTORY}/dataFile-${Calendar.getInstance().timeInMillis}.csv"
        val fOut: FileOutputStream = FileOutputStream(dataPath)

        val csvHeaders = listOf<String>(
            "GPS Date & Time",
            "# of Sats",
            "HDOP",
            "Latitude",
            "Longitude",
            "Altitude(m)",
            "Speed(m/s)",
            "Heading(Deg)",
            "Marker",
            "Value"
        )
        osw = OutputStreamWriter(fOut)
        osw.appendLine(
            csvHeaders.toString().replace("[", "").replace("]", "")
        )

        // Initialize validation parameters
        total_lanes = activeConfigSubject.value!!.LaneInfo.NumberOfLanes
        for (i in 1..total_lanes) {
            laneList.add(i.toString())
        }
        lane_stat = MutableList<Boolean>(total_lanes+1) {false}
        var wp_stat = false
        got_rp = false
        line_num = 0
        // messages.clear()
    }

    private fun writeToDataFile(message: CSVObj) {
        val formattedMessage: String = "${formatter.format(message.time)},${message.num_sats},${message.hdop},${message.latitude},${message.longitude},${message.altitude},${message.speed},${message.heading},${message.marker},${message.marker_value}"
        println(formattedMessage)

        validateDataLine(formattedMessage, line_num)
//        val messages =
//        if (messages.isNotEmpty()) {
//            // Line invalid
//            for (msg in messages) {
//                toastNotificationSubject.onNext("Invalid data line: $msg")
//            }
//        }

        println(loggingData)
        // Remove duplicates
        if (formattedMessage != previousLine && loggingData) {
            // Ignores duplicate lines
            if (message.marker == "Data Log" && message.marker_value == "False") {
                // Verify that app state is valid (lane closures and worker presence
                val lastMessages = validateLastDataLine(formattedMessage)
                if (lastMessages.isNotEmpty()) {
                    end_when_ready = true
                    // App state invalid, do not end data collection
                    // Print messages and remove 'Data Log False' from message
                    for (msg in lastMessages) {
                        toastNotificationSubject.onNext("Cannot end data collection because: $msg")
                    }

                    // Recreate message without marker and marker_value
                    val updatedMessage = "${formatter.format(message.time)},${message.num_sats},${message.hdop},${message.latitude},${message.longitude},${message.altitude},${message.speed},${message.heading},,"
                    if (updatedMessage != previousLine) {
                        osw.appendLine(updatedMessage)
                        previousLine = updatedMessage
                    }
                }
                else {
                    // App state valid, write line to file and end data collection/upload data file
                    osw.appendLine(formattedMessage)
                    previousLine = formattedMessage
                    loggingData = false
                    end_when_ready = false
                    saveDataFile()
                }
            }
            else if (end_when_ready) {
                println("Attmepting to end")
                val lastMessages = validateLastDataLine(formattedMessage)
                if (lastMessages.isEmpty()) {
                    // App state valid, ensure last message written to file has Data Log False
                    if (message.marker != "") {
                        println(message)
                        // Add Data Log False, write line to file and end data collection/upload data file
                        val updatedMessage = "${formatter.format(message.time)},${message.num_sats},${message.hdop},${message.latitude},${message.longitude},${message.altitude},${message.speed},${message.heading},Data Log,False"
                        osw.appendLine(updatedMessage)
                        loggingData = false
                        end_when_ready = false
                        saveDataFile()
                    }
                    else {
                        osw.appendLine(formattedMessage)
                        previousLine = formattedMessage
                    }
                }
            }
            else {
                // Nothing special, just write line to data file
                osw.appendLine(formattedMessage)
                previousLine = formattedMessage
            }
        }
    }

    private fun saveDataFile() {
        osw.flush()
        osw.close()
        println("File Size: ${File(dataPath).length()}")
        val dataFileDownloadsLocation: String = "${Constants.DOWNLOAD_LOCTION}/${dataPath.split(
            "/"
        ).last()}"
        println("Download location: $dataFileDownloadsLocation")
        copy(dataPath, dataFileDownloadsLocation)
        File(dataPath).delete()

        dataFileSubject.onNext(dataFileDownloadsLocation)
    }

    private fun validateDataLine(line: String, lineNum: Int): List<String> {
        val fields = line.split(",")
        var valid = true
        val messages: MutableList<String> = mutableListOf()

        val time    = fields[0]
        val sats    = fields[1].toInt()
        val hdop    = fields[2].toDouble()
        val lat     = fields[3].toDouble()
        val lon     = fields[4].toDouble()
        val elev    = fields[5].toDouble()
        val speed   = fields[6].toDouble()
        val heading = fields[7].toDouble()
        val marker  = fields[8]
        val value   = fields[9]

        // Simple verification
        if (! ("""([0-9]){4}\/(0[1-9]|1[0-2])\/([0-9]){2}-(0[0-9]|1[0-9]|2[0-4]):([0-5][0-9]):([0-5][0-9]):([0-9]){2}""").toRegex().matches(time)) {
            messages.add("Line $lineNum: GPS date time gormat invalid: $time")
        }
        if (sats !in 0..12) {
            messages.add("Line $lineNum: Number of sattelites invalid: $sats")
        }
        if (hdop <= 0) {
            messages.add("Line $lineNum: HDOP format invalid: $hdop")
        }
        if (! (lat >= -90 && lat <= 90)) {
            messages.add("Line $lineNum: Latitude invalid: $lat")
        }
        if (! (lon >= -180.0 && lon <= 180.0)) {
            messages.add("Line $lineNum: Longitude invalid: $lon")
        }
        if (! (elev >= -4096.0 && elev <= 61439.0)) {
            messages.add("Line $lineNum: Altitude invalid: $elev")
        }
        if (speed !in 0.0..8191.0) {
            messages.add("Line $lineNum: Speed invalid: $speed")
        }
        if (heading !in 0.0..360.0) {
            messages.add("Line $lineNum: Heading invalid: $heading")
        }
        // Verify marker + value combination is valid
        if (markerValueDict[marker] != null) {
            if (!markerValueDict[marker]!!.contains(value)) {
                messages.add("Line $lineNum: Marker and value combination invalid. Marker: $marker, Value: $value")
            }
        }
        else {
            messages.add("Line $lineNum: Marker invalid: $marker")
        }

        ////// Advanced verification

        // Verify Reference Point
        if (marker == "RP" ||  marker == "LC+RP" ||  marker == "LC+WP") {
            got_rp = true
        }

        if (value in markerValueDict["LC"]!!) { // Equivalent to checking markerValueDict["LO"]
            // This means that value is an integer
            // Verify lane closure continuity
            if (marker == "LC" ||  marker == "LC+RP") {
                if (lane_stat[value.toInt()]) {
                    messages.add("Line $lineNum: Lane closure invalid, closed lane being closed: $marker: $value")
                }
                else {
                    lane_stat[value.toInt()] = true
                }
            }
            if (marker == "LO") {
                if (!lane_stat[value.toInt()])
                    messages.add("Line $lineNum: Lane opening invalid, open lane being opened: $marker: $value")
                else {
                    lane_stat[value.toInt()] = false
                }
            }
            else {
                // This error is caught by the basic validator
            }
        }

        // Verify worker presence continuity
        if (marker == "WP" ||  marker == "WP+RP") {
            if (value == "True" || value == "False") {
                if (wp_stat == value.toLowerCase(Locale.ROOT).toBoolean()) {
                    messages.add("Line $lineNum: Worker Presence change invalid, wp: $wp_stat, value: $value")
                }
                else {
                    wp_stat = !wp_stat
                }
            }
        }
        return messages
    }

    private fun validateLastDataLine(line: String): List<String> {
//        val fields = line.split(",")
        val messages: MutableList<String> = mutableListOf()

        if (!got_rp) {
            messages.add("Reference point must be marked")
        }

        for (i in 1 until lane_stat.size) {
            if (lane_stat[i]) {
                messages.add("Lane $i still closed")
            }
        }

        if (wp_stat) {
            messages.add("Workers still present")
        }

        return messages
    }

//    private fun validateDataFile() {
//
//        var gotRP = false
//        var i = 0
//
//        lane_stat = [0]*9
//        wp_stat = False
//        messages = []
//        file_valid = True
//        got_rp = False
//        i = 0
//        with open(veh_path_data_file, "r") as f:
//        headers = f.readline()
//        data = f.readline().rstrip("\n")
//        while data:
//        i += 1
//        valid, msg, lane_stat, wp_stat, got_rp = validate_data_line(data, markerList, markerValueDict, lane_stat, wp_stat, got_rp)
//        if (! valid):
//        vileValid = False
//        messages.append("Line " + str(i) + " " + msg)
//
//        data = f.readline().rstrip("\n")
//
//        if (! got_rp == True)
//        file_valid = False
//        messages.append(" No reference point found by end")
//        if (! wp_stat == False)
//        file_valid = False
//        messages.append("Workers present not false at end")
//        if (! lane_stat == [0]*9)
//        file_valid = False
//        messages.append("All lanes not open at end")
//
//        return messages
//    }

    fun copy(src: String, dst: String) {
        val `in`: InputStream = FileInputStream(src)
        `in`.use { `in` ->
            val out: OutputStream = FileOutputStream(dst)
            out.use { out ->
                // Transfer bytes from in to out
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            }
        }
    }

    fun uploadPathDataFile(filePath: String, fileName: String): String {
        try {
            // Retrieve storage account from connection-string.
            if (azureInfoRepository.currentConnectionStringSubject.value == null) {
                return ""
            }
            val storageAccount: CloudStorageAccount =
                CloudStorageAccount.parse(azureInfoRepository.currentConnectionStringSubject.value)

            // Create the blob client.
            val blobClient: CloudBlobClient = storageAccount.createCloudBlobClient()

            // Retrieve reference to a previously created container.
            val container: CloudBlobContainer = blobClient.getContainerReference(Constants.AZURE_PATH_DATA_UPLOADS_CONTAINER)

            // Create or overwrite the blob (with the name "example.jpeg") with contents from a local file.
            val blob: CloudBlockBlob = container.getBlockBlobReference(fileName)
            val source = File(filePath)
            blob.upload(FileInputStream(source), source.length())
            return "Success!"

        } catch (e: Exception) {
            // Output the stack trace.
            e.printStackTrace()
            return "FAILED${e.stackTrace}"
        }
    }

}