package com.wzdctool.android.repos

import android.app.Activity
import android.location.Location
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.wzdctool.android.Constants
import com.wzdctool.android.SecureKeys
import com.wzdctool.android.dataclasses.CSVObj
import com.wzdctool.android.dataclasses.LOCATION
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object DataFileRepository {
    val csvDataSubject = MutableLiveData<CSVObj>()
    var markerSubject = BehaviorSubject.create<MarkerObj>()
    var dataFileSubject = BehaviorSubject.create<String>()

    private lateinit var dataPath: String
    private lateinit var osw: OutputStreamWriter
    private lateinit var prevLocation: Location
    lateinit var dataFileName: String
    var loggingData = false
    var isFirstTime = true
    val formatter: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SS")


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
            Date(location.time), 0, location.accuracy,
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
    }

    private fun writeToDataFile(message: CSVObj) {
        val formattedMessage: String = "${formatter.format(message.time)},${message.num_sats},${message.hdop},${message.latitude},${message.longitude},${message.altitude},${message.speed},${message.heading},${message.marker},${message.marker_value}"
        println(formattedMessage)
        if (loggingData) osw.appendLine(formattedMessage)
        if (message.marker == "Data Log" && message.marker_value == "False") {
            loggingData = false
            saveDataFile()
        }
    }

    private fun saveDataFile() {
        osw.flush()
        osw.close()
        println("File Size: ${File(dataPath).length()}")
        val dataFileDownloadsLocation: String = "${Constants.DOWNLOAD_LOCTION}/${dataPath.split(
            '/'
        ).last()}"
        println("Download location: $dataFileDownloadsLocation")
        copy(dataPath, dataFileDownloadsLocation)
        File(dataPath).delete()

//        Thread {
//            println("Downloaded Configuration File")
//            val output = uploadPathDataFile(dataFileDownloadsLocation, dataFileName)
//            runOnUiThread {
//                println("Running on Main thread")
//                println(output)
//                val mySnackbar = Snackbar.make(
//                    findViewById<TextView>(R.id.textView),
//                    "Uploaded Path Data",
//                    5000
//                )
//                mySnackbar.show()
//            }
//        }.start()

        dataFileSubject.onNext(dataFileDownloadsLocation)

        // viewModelScope.launch(Dispatchers.IO) {
//        dataFileName = "path-data--${activeWZIDSubject.value}.csv"
//        val output = uploadPathDataFile(dataFileDownloadsLocation, dataFileName)
//        println(output)
            // notificationText.postValue("Uploaded Path Data")
            // ConfigurationRepository.activateConfig("config--road-name--description.json")
        // }

        // findNavController(SecondFragment()).navigate(R.id.action_SecondFragment_to_FirstFragment)
    }

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
            val storageAccount: CloudStorageAccount =
                CloudStorageAccount.parse(SecureKeys.AZURE_CONNECTION_STRING)

            // Create the blob client.
            val blobClient: CloudBlobClient = storageAccount.createCloudBlobClient()

            // Retrieve reference to a previously created container.
            val container: CloudBlobContainer = blobClient.getContainerReference("workzoneuploads")

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