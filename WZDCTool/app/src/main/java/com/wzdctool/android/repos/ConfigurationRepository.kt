package com.wzdctool.android.repos

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlob
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.azureInfoRepository.currentConnectionStringSubject
import java.io.File
import java.io.FileOutputStream

object ConfigurationRepository {
    val configListSubject = MutableLiveData<List<String>>()
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

    // fun refreshWorkZoneList(): Observable<LiveResource<List<String>>> = Observable.just

    fun activateConfig(configName: String, filePath: String): Boolean {
        // TODO: Catch exception and return false
        // if (configName !in configListSubject.value!!) return false

        val filePath = downloadConfigFile(configName, filePath) ?: return false
        val fileContents: String = File(filePath).readText(Charsets.UTF_8)
        val config: ConfigurationObj = Gson().fromJson(
            fileContents,
            ConfigurationObj::class.java
        )
        activeConfigSubject.postValue(config)
        activeWZIDSubject.postValue(configName.removePrefix("config--").removeSuffix(".json"))
        return true
    }

    fun updateLocalConfigList(): Boolean {
        val localConfigList = getLocalConfigFileList()
        if (localConfigList != listOf<String>()) {
            localConfigListSubject.postValue(localConfigList)
            return true
        }
        return true
    }

    //    fun getConfigList(): List<String> {
//        return getConfigFileList()
//    }
    fun updateConfigList(): Boolean {
        val configListUpdated = getConfigFileList()
        if (configListUpdated != listOf<String>()) {
            configListSubject.postValue(configListUpdated)
            return true
        }
        return false
    }

    private fun getLocalConfigFileList(): List<String> {
        // TODO: Check permissions
        val configList = mutableListOf<String>()
        val fileList: Array<String>? = File(Constants.CONFIG_DIRECTORY).list()
        if (fileList != null) {
            for (file in fileList) {
                val end = file.split('/')[file.split('/').size - 1].removePrefix(Constants.CONFIG_DIRECTORY + "/")
                if (!end.contains("/") && end.contains(".json")) {
                    configList.add(end.removePrefix("config-").removeSuffix(".json"))
                }
            }
            return configList
        }
        return listOf<String>()

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
            CloudStorageAccount.parse( currentConnectionStringSubject.value ) // SecureKeys.AZURE_CONNECTION_STRING)

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