package com.wzdctool.android.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.*
import com.wzdctool.android.repos.ConfigurationRepository.activeWZIDSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import rx.Subscription
import rx.subjects.PublishSubject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object DataFileRepository {
    val csvDataSubject = MutableLiveData<CSVObj>()
    var markerSubject = PublishSubject.create<MarkerObj>()
    var dataFileSubject = PublishSubject.create<String>()

    private var dataPath: String = ""
    private lateinit var osw: OutputStreamWriter
    private lateinit var prevLocation: Location
    lateinit var dataFileName: String
    var loggingData = false
    var isFirstTime = true
    val formatter: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SS")

    private var total_lanes: Int = 8

    private var prevCsvObj: CSVObj? = null

    private var lane_stat = MutableList<Boolean>(8+1) {false}
    private var wp_stat = false
    private var got_rp = false

    private var end_when_ready = false

    private var previousLine = ""

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    // private val messages: MutableList<String> = mutableListOf()

    private val laneList: MutableList<String> = mutableListOf()
    // var laneList = List<Boolean>(total_lanes+1) {false}
    var line_num = 0

    private val markerList: List<String> = listOf("Data Log", "RP", "WP+RP", "LC+RP", "WP", "LC", "LO", "")
    private val markerValueDict: HashMap<String, List<String>> = hashMapOf("Data Log" to listOf("True", "False"), "RP" to listOf(""), "WP+RP" to listOf("True", "False"), "LC+RP" to laneList,
            "WP" to listOf("True", "False"), "LC" to laneList, "LO" to laneList, "" to listOf(""))


    // var marker: String = ""
    var markerQueue: Queue<MarkerObj> = LinkedList<MarkerObj>()

//    fun createDataFile() {
//        dataPath = "${Constants.DATA_FILE_DIRECTORY}/dataFile-${Calendar.getInstance().timeInMillis}.csv"
//        osw = createDataFile()
//    }

    fun getDataFileName(wzId: String): String {
        return "path-data--$wzId.csv"
    }

    fun initializeObservers() {
        subscriptions.add(markerSubject.subscribe {
//                    toastNotificationSubject.onNext("${it.marker}: ${it.value}")
            if (it.marker == "Cancel") {
                loggingData = false
                end_when_ready = false
                if (dataPath != "")
                    File(dataPath).delete()
            }
            else if (loggingData) {
                markerQueue.add(it)
            }
            else if (it.marker == "Data Log" && it.value == "True") {
                markerQueue.clear()
                markerQueue.add(it)
                createDataFile()
                // toastNotificationSubject.onNext("Logging Data")
                loggingData = true
                // val csvObj = createCSVObj(prevLocation)
            }
        })

        subscriptions.add(locationSubject.subscribe {
            if (it != null) {
                handleLocation(it)
            }
        })
    }

    fun removeObservers() {
        println("Removing Observers")
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
    }

    fun getVisualizationObj(fileName: String): VisualizationObj {
        val pathPoints = mutableListOf<LatLng>()
        val markers = mutableListOf<CustomMarkerObj>()
        val file = File(fileName)
        val lines = file.readLines().drop(1) // Ignore first line

        var startEndIDs = IDSet(UUID.randomUUID(), UUID.randomUUID())
        var wpIDs = IDSet(UUID.randomUUID(), UUID.randomUUID())
        val laneIDs = mutableListOf<IDSet>(IDSet(UUID.randomUUID(), UUID.randomUUID()),
            IDSet(UUID.randomUUID(), UUID.randomUUID()), IDSet(UUID.randomUUID(), UUID.randomUUID()),
            IDSet(UUID.randomUUID(), UUID.randomUUID()), IDSet(UUID.randomUUID(), UUID.randomUUID()),
            IDSet(UUID.randomUUID(), UUID.randomUUID()), IDSet(UUID.randomUUID(), UUID.randomUUID()),
            IDSet(UUID.randomUUID(), UUID.randomUUID()), IDSet(UUID.randomUUID(), UUID.randomUUID()))

        for (line in lines) {
            val csvObj = parseCSVLine(line)?: continue
            val position = LatLng(csvObj.latitude, csvObj.longitude)
            pathPoints.add(position)
            if (csvObj.marker != "") {
                var IDs: IDSet = IDSet(UUID.randomUUID(), UUID.randomUUID())
                if (csvObj.marker == "LC" || csvObj.marker == "LO") {
                    if (csvObj.marker == "LC") {
                        laneIDs[csvObj.marker_value.toInt()] = IDSet(UUID.randomUUID(), UUID.randomUUID())
                        IDs = laneIDs[csvObj.marker_value.toInt()]
                    } else {
                        IDs = reverseIDs(laneIDs[csvObj.marker_value.toInt()])
                    }
                }
                else if (csvObj.marker == "WP") {
                    if (csvObj.marker_value == "True") {
                        wpIDs = IDSet(UUID.randomUUID(), UUID.randomUUID())
                        IDs = wpIDs
                    } else {
                        IDs = reverseIDs(wpIDs)
                    }
                }
                else if (csvObj.marker == "RP") {
                    // Title correct
                }
                else if (csvObj.marker == "Data Log") {
                    if (csvObj.marker_value == "True") {
                        startEndIDs = IDSet(UUID.randomUUID(), UUID.randomUUID())
                        IDs = startEndIDs
                    } else {
                        IDs = reverseIDs(startEndIDs)
                    }
                }
                else {

                }
                markers.add(CustomMarkerObj(position, Title(csvObj.marker, csvObj.marker_value, IDs)))
            }
        }
        return VisualizationObj(pathPoints, markers, fileName)
    }

    fun updateDataFileMarkers(fileName: String, markers: List<CSVMarkerObj>): Boolean {
        val file = File(fileName)
        val lines = file.readLines() // Ignore first line
        val osw = OutputStreamWriter(FileOutputStream(fileName))
        osw.write("")
        for ((i, line) in lines.withIndex()) {
            if (i == 0) {
                osw.appendLine(line)
            }
            else {
                val csvObj = parseCSVLine(line) ?: return false
                csvObj.marker = ""
                csvObj.marker_value = ""
                for (marker in markers) {
                    if (marker.row + 1 == i) {
                        csvObj.marker = marker.marker
                        csvObj.marker_value = marker.value
                        println("Writing Marker: ${csvObj.marker}, ${csvObj.marker_value} to Line $i")
                        println(getCSVLine(csvObj))
                        break
                    }
                }
                osw.appendLine(getCSVLine(csvObj))
            }
        }
        osw.flush()
        osw.close()
        println("File Size: ${File(fileName).length()}")
        notificationSubject.onNext("Data File updated")
        return true
    }

    private fun handleLocation(location: Location) {
        println("Data File Repo")
        if (loggingData) {
            val csvObj = createCSVObj(location)
            validateDataLine(csvObj, line_num)
            if (csvObj == prevCsvObj)
                return
            if (csvObj.marker == "Data Log" && csvObj.marker_value == "False") {
                val requiredMarkers = getRequiredMarkers(csvObj)
                if (requiredMarkers.isEmpty()) {
                    writeToDataFile(csvObj)
                    loggingData = false
                    saveDataFile()
                }
                else {
                    markerQueue.clear()
                    for (marker in requiredMarkers) {
                        markerQueue.add(marker)
                    }
                    markerQueue.add(MarkerObj("Data Log", "False"))
                    val updatedCsvObj = createCSVObj(location)
                    validateDataLine(updatedCsvObj, line_num)
                    writeToDataFile(updatedCsvObj)
                }
            }
            else {
                writeToDataFile(csvObj)
            }
            prevCsvObj = csvObj
            line_num++
        }
    }

    private fun createCSVObj(location: Location): CSVObj {
        val marker: MarkerObj = if (markerQueue.size >= 1) markerQueue.remove() else MarkerObj("", "")
        var numSats = 0
        try {
            numSats = location.extras.getInt("satellites")
        }
        catch (e: Exception) {
            numSats = 0
        }
        val csvObj = CSVObj(
            Date(location.time), numSats, location.accuracy,
            location.latitude, location.longitude, location.altitude, location.speed,
            location.bearing, marker.marker, marker.value
        )
        return csvObj
    }

    private fun getCSVLine(csvObj: CSVObj): String {
        return "${formatter.format(csvObj.time)},${csvObj.num_sats},${csvObj.hdop},${csvObj.latitude},${csvObj.longitude},${csvObj.altitude},${csvObj.speed},${csvObj.heading},${csvObj.marker},${csvObj.marker_value}"
    }

    private fun parseCSVLine(line: String): CSVObj? {
        val fields = line.split(',')
        if (fields.size < 9)
            return null

        val time = 0
        val num_sats = 1
        val hdop = 2
        val latitude = 3
        val longitde = 4
        val altitude = 5
        val speed = 6
        val heading = 7
        val marker = 8
        val value = 9

        val csvObj = CSVObj(
            formatter.parse(fields[time])!!, fields[num_sats].toInt(), fields[hdop].toFloat(),
            fields[latitude].toDouble(), fields[longitde].toDouble(), fields[altitude].toDouble(),
            fields[speed].toFloat(), fields[heading].toFloat(), fields[marker], fields[value]
        )
        return csvObj
    }


    private fun createDataFile() {
        dataPath = "${Constants.DATA_FILE_DIRECTORY}/path-data--${activeWZIDSubject.value!!}.csv"
        if (DataClassesRepository.automaticDetectionSubject.value == false) {
            dataPath = "${Constants.DATA_FILE_DIRECTORY}/path-data--${activeWZIDSubject.value!!}--update-image.csv"
        }
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
        osw.write("")
        osw.appendLine(
            csvHeaders.toString().replace("[", "").replace("]", "")
        )

        // Initialize validation parameters
        total_lanes = 8 //activeConfigSubject.value!!.LaneInfo.NumberOfLanes
        for (i in 1..total_lanes) {
            laneList.add(i.toString())
        }
        lane_stat = MutableList<Boolean>(total_lanes+1) {false}
        wp_stat = false
        got_rp = false
        line_num = 0
        // messages.clear()
    }

    private fun writeToDataFile(message: CSVObj) {
        val formattedMessage: String = getCSVLine(message)
        println(formattedMessage)

        osw.appendLine(formattedMessage)
        previousLine = formattedMessage
    }

    private fun saveDataFile() {
        osw.flush()
        osw.close()
        println("File Size: ${File(dataPath).length()}")
        val dataFileDownloadsLocation: String = "${Constants.PENDING_UPLOAD_DIRECTORY}/${dataPath.split(
            "/"
        ).last()}"
        println("Download location: $dataFileDownloadsLocation")
        copy(dataPath, dataFileDownloadsLocation)
        File(dataPath).delete()

        dataFileSubject.onNext(dataFileDownloadsLocation)
    }

    fun uploadAllDataFiles() {
        val directory: File = File(Constants.PENDING_UPLOAD_DIRECTORY)
        val files = directory.listFiles()
        if (files != null) {
            var i = 0
            for (file in files) {
                println("Name: ${file.name}")
                println("Path: ${file.path}")
                i++
                uploadPathDataFile(
                    file.path,
                    file.name
                )
            }
            if (i != 0) {
                notificationSubject.onNext("$i Path data file(s) uploaded")
            }
        }
    }

    fun getDataFilesList(): List<String> {
        val fileList = mutableListOf<String>()
        val directory: File = File(Constants.PENDING_UPLOAD_DIRECTORY)
        val files = directory.listFiles()
        val fileObjList = mutableListOf<File>()
        if (files != null) {
            for (file in files) {
                fileObjList.add(file)
                fileList.add(file.name)
            }
        }
        val tempList = fileObjList.sortedWith(compareBy {
            it.lastModified()  //.compareTo(Calendar.getInstance().time)
        }).reversed()
        val sortedFilesList = mutableListOf<String>()
        tempList.forEach {
            sortedFilesList.add(it.name)
        }
        return sortedFilesList //fileList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it }))
    }

    fun uploadDataFiles(fileList: List<String>) {
        var i = 0
        for (file in fileList) {
            uploadPathDataFile(
                "${Constants.PENDING_UPLOAD_DIRECTORY}/$file",
                file
            )
            i++
        }
        notificationSubject.onNext("$i Path data file(s) uploaded")
    }

    private fun validateDataLine(csvObj: CSVObj, lineNum: Int): List<String> {
        var valid = true
        val messages: MutableList<String> = mutableListOf()

        val time    = csvObj.time
        val sats    = csvObj.num_sats
        val hdop    = csvObj.hdop
        val lat     = csvObj.latitude
        val lon     = csvObj.longitude
        val elev    = csvObj.altitude
        val speed   = csvObj.speed
        val heading = csvObj.heading
        val marker  = csvObj.marker
        val value   = csvObj.marker_value

        // Simple verification
        if (! ("""([0-9]){4}\/(0[1-9]|1[0-2])\/([0-9]){2}-(0[0-9]|1[0-9]|2[0-4]):([0-5][0-9]):([0-5][0-9]):([0-9]){2}""").toRegex().matches(formatter.format(time))) {
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

    private fun getRequiredMarkers(csvObj: CSVObj): List<MarkerObj> {
//        val fields = line.split(",")
        val markers: MutableList<MarkerObj> = mutableListOf()

        if (!got_rp) {
            markers.add(MarkerObj("RP", ""))
        }

        for (i in 1 until lane_stat.size) {
            if (lane_stat[i]) {
                markers.add(MarkerObj("LO", i.toString()))
            }
        }

        if (wp_stat) {
            markers.add(MarkerObj("WP", "False"))
        }

        return markers
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

    fun uploadPathDataFile(filePath: String, fileName: String): Boolean {
        try {
            println("started")
            // Retrieve storage account from connection-string.
            if (AzureInfoRepository.currentConnectionStringSubject.value == null) {
                return false
            }
            val storageAccount: CloudStorageAccount =
                CloudStorageAccount.parse(AzureInfoRepository.currentConnectionStringSubject.value)

            // Create the blob client.
            val blobClient: CloudBlobClient = storageAccount.createCloudBlobClient()

            // Retrieve reference to a previously created container.
            val container: CloudBlobContainer = blobClient.getContainerReference(Constants.AZURE_PATH_DATA_UPLOADS_CONTAINER)

            // Create or overwrite the blob (with the name "example.jpeg") with contents from a local file.
            val blob: CloudBlockBlob = container.getBlockBlobReference(fileName)
            val source = File(filePath)
            println("About to upload $filePath")
            blob.upload(FileInputStream(source), source.length())

            println("Deleted: ${source.delete()}")

            return true

        } catch (e: Exception) {
            // Output the stack trace.
            e.printStackTrace()
            return false
        }
    }

}