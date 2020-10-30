package com.wzdctool.android.repos

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.azureInfoObj
import com.wzdctool.android.repos.ConfigurationRepository.getConfigFileList
import rx.subjects.BehaviorSubject
import java.security.InvalidKeyException

object azureInfoRepository {

    val validLoginInfoSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(true)

    val currentAzureInfoSubject: BehaviorSubject<azureInfoObj> = BehaviorSubject.create<azureInfoObj>()

    val currentConnectionStringSubject: BehaviorSubject<String> = BehaviorSubject.create<String>()

    // var savedAzure

    private fun createConnectionString(azureInfo: azureInfoObj): String {
        return "DefaultEndpointsProtocol=https;" +
                "AccountName=${azureInfo.account_name};" +
                "AccountKey=${azureInfo.account_key};" +
                "EndpointSuffix=core.windows.net"
    }

    fun updateConnectionString(connectionString: String) {
        currentConnectionStringSubject.onNext(
            connectionString
        )
    }

    fun updateConnectionStringFromObj(azureInfo: azureInfoObj) {
        currentConnectionStringSubject.onNext(
            createConnectionString(azureInfo)
        )
    }

    fun isConnectionStringValid(azureInfo: azureInfoObj, update: Boolean): Boolean {
        if (azureInfo.account_name.isEmpty() || azureInfo.account_key.isEmpty())
            return false

        val connectionString = createConnectionString(azureInfo)

        return connectionString.hashCode() == Constants.AZURE_CONNECTION_STRING_HASH_CODE
    }

    fun parseConnectionString(connectionString: String) {

    }

}