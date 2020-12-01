package com.wzdctool.android.repos

import com.wzdctool.android.Constants
import com.wzdctool.android.dataclasses.AzureInfoObj
import rx.subjects.BehaviorSubject

object AzureInfoRepository {

    val validLoginInfoSubject: BehaviorSubject<Boolean> = BehaviorSubject.create<Boolean>(true)

    val currentAzureInfoSubject: BehaviorSubject<AzureInfoObj> = BehaviorSubject.create<AzureInfoObj>()

    val currentConnectionStringSubject: BehaviorSubject<String> = BehaviorSubject.create<String>()

    // var savedAzure

    private fun createConnectionString(azureInfo: AzureInfoObj): String {
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

    fun updateConnectionStringFromObj(azureInfo: AzureInfoObj) {
        currentConnectionStringSubject.onNext(
            createConnectionString(azureInfo)
        )
    }

    fun isConnectionStringValid(azureInfo: AzureInfoObj, update: Boolean): Boolean {
        if (azureInfo.account_name.isEmpty() || azureInfo.account_key.isEmpty())
            return false

        val connectionString = createConnectionString(azureInfo)

        return connectionString.hashCode() == Constants.AZURE_CONNECTION_STRING_HASH_CODE
    }

    fun parseConnectionString(connectionString: String) {

    }

}