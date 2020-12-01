package com.wzdctool.android.repos

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlob
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.repos.DataClassesRepository.isInternetAvailable
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.AzureInfoRepository.currentConnectionStringSubject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


object ConfigurationRepository {
    val configListSubject = MutableLiveData<List<String>>()
    val cloudConfigListSubject = MutableLiveData<List<String>>()
    val localConfigListSubject = MutableLiveData<List<String>>()
    val activeConfigSubject = MutableLiveData<ConfigurationObj>()
    val activeWZIDSubject = MutableLiveData<String>()


//    val configList: Observable<List<String>>
//        get() = configListSubject
//
//    val localConfigList: Observable<List<String>>
//        get() = localConfigListSubject
//
//    val activeConfig: Observable<ConfigurationObj>
//        get() = activeConfigSubject

//    fun getCurrentConfigList() = configListSubject.value
//
//    fun getCurrentLocalConfigList() = localConfigListSubject.value
//
//    fun getCurrentActiveConfig() = activeConfigSubject.value

    fun getDataFileName(wzId: String): String {
        return "path-data--$wzId.csv"
    }

    fun getConfigFileName(wzId: String): String {
        return "config--$wzId.json"
    }

    private fun getConfigFileLocation(name: String): String {
        return "${Constants.CONFIG_DIRECTORY}/$name"
    }

    // fun refreshWorkZoneList(): Observable<LiveResource<List<String>>> = Observable.just

    fun activateConfig(configName: String): Boolean {
        // TODO: Catch exception and return false
        // if (configName !in configListSubject.value!!) return false

        Log.v("Path", Constants.CONFIG_DIRECTORY)
        val directory: File = File(Constants.CONFIG_DIRECTORY)
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                Log.v("Files: ", file.path)
            }
        }
        else {
            Log.v("result", "null")
        }

        Log.v("activate config", getConfigFileLocation(configName))
//        try {
//        if (configName == "Loading Configuration File List") {
//            return false
//        }
        try {
            val fileContents: String = File(getConfigFileLocation(configName)).readText(Charsets.UTF_8)
            val config: ConfigurationObj = Gson().fromJson(
                fileContents,
                ConfigurationObj::class.java
            )
            activeConfigSubject.postValue(config)
            activeWZIDSubject.postValue(configName.removePrefix("config--").removeSuffix(".json"))
            return true
        }
        catch (e: FileNotFoundException) {
            return false
        }
//        }
//        catch (e: Exception) {
//            return false
//        }
    }

    fun updateLocalConfigList(): Boolean {
        val updatedConfigList = mutableListOf<String>()
        val directory: File = File(Constants.CONFIG_DIRECTORY)
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                updatedConfigList.add(file.name)
            }
            configListSubject.postValue(updatedConfigList)
            return true
        }
        return false
    }

    fun getLocalConfigList(): List<String> {
        val updatedConfigList = mutableListOf<String>()
        val directory: File = File(Constants.CONFIG_DIRECTORY)
        val files = directory.listFiles()
//        val fileObjList = mutableListOf<File>()
        if (files != null) {
            for (file in files) {
                updatedConfigList.add(file.name)
//                fileObjList.add(file)
            }
        }
//        val tempList = fileObjList.sortedWith(compareBy {
//            it.lastModified()  //.compareTo(Calendar.getInstance().time)
//        }).reversed()
//        val sortedFilesList = mutableListOf<String>()
//        tempList.forEach {
//            sortedFilesList.add(it.name)
//        }
        return updatedConfigList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it }))
    }

    fun getLocalConfigSizeKB(): Double {
        var size: Double = 0.0
        val directory: File = File(Constants.CONFIG_DIRECTORY)
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                size += (file.length()/1024)
            }
        }
        return size
    }

    private fun clearLocalConfigFiles(): Boolean {
        val directory: File = File(Constants.CONFIG_DIRECTORY)
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                file.delete()
            }
            return true
        }
        return false
    }

    //    fun getConfigList(): List<String> {
//        return getConfigFileList()
//    }
    fun downloadNewConfigFiles(configList: List<String>): Boolean {
        if (isInternetAvailable()) {
            clearLocalConfigFiles()
            var i = 0
            val updatedConfigList = mutableListOf<String>()
            for (config in configList) {
                if (downloadConfigFile(config, getConfigFileLocation(config)) != null) {
                    updatedConfigList.add(config)
                    i++
                }
            }
            if (i != 0) {
                notificationSubject.onNext("$i configuration file(s) downloaded")
            }
            configListSubject.postValue(updatedConfigList)
            return true
        }
        else {
            return false
        }
    }

    fun updateCloudConfigList(): Boolean {
        if (isInternetAvailable()) {
            val configListUpdated = getConfigFileList()
            if (configListUpdated != listOf<String>()) {
                cloudConfigListSubject.postValue(configListUpdated)
                return true
            }
        }
        return false
    }

    fun getConfigFileList(): List<String> {

        // Retrieve storage account from connection-string.
        if (currentConnectionStringSubject.value == null) {
            return listOf()
        }
        val storageAccount: CloudStorageAccount =
            CloudStorageAccount.parse(currentConnectionStringSubject.value)

        // Create the blob client.
        val blobClient: CloudBlobClient = storageAccount.createCloudBlobClient()

        // Retrieve reference to a previously created container.
        val container: CloudBlobContainer = blobClient.getContainerReference(Constants.AZURE_PUBLISHED_CONFIG_FILES_CONTAINER)
        val configNameList: MutableList<String> = mutableListOf<String>()

        val configList = container.listBlobs()
        println(configList.count())
        val tempList = configList.sortedWith(compareBy {
            (it as CloudBlob).properties.lastModified  //.compareTo(Calendar.getInstance().time)
        }).reversed()
        tempList.forEach {
            println(it.uri.path.toString())
            configNameList.add(it.uri.path.split("/")[it.uri.path.split("/").size - 1])
        }
        return configNameList
    }

    private fun downloadConfigFile(configName: String, fileDir: String): String? {
        // Retrieve storage account from connection-string.
        if (currentConnectionStringSubject.value == null) {
            return null
        }
        val storageAccount: CloudStorageAccount =
            CloudStorageAccount.parse(currentConnectionStringSubject.value) // SecureKeys.AZURE_CONNECTION_STRING)

        // Create the blob client.
        val blobClient: CloudBlobClient = storageAccount.createCloudBlobClient()

        // Retrieve reference to a previously created container.
        val container: CloudBlobContainer = blobClient.getContainerReference(Constants.AZURE_PUBLISHED_CONFIG_FILES_CONTAINER)
        // Create or overwrite the blob (with the name "example.jpeg") with contents from a local file.
        val blob: CloudBlockBlob = container.getBlockBlobReference(configName)
        var filePath = "${Constants.CONFIG_DIRECTORY}/$configName"
        // Activity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        println("File Path: $filePath")
        val source = File(filePath)

        try {
            blob.download(FileOutputStream(source))
        }
        catch (e: Exception) {
            return null
        }

        return filePath
    }
}