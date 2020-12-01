package com.wzdctool.android

import androidx.lifecycle.ViewModel
import com.wzdctool.android.dataclasses.AzureInfoObj
import com.wzdctool.android.repos.AzureInfoRepository.currentAzureInfoSubject
import com.wzdctool.android.repos.AzureInfoRepository.isConnectionStringValid

class SettingsViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun saveSettings(azureInfo: AzureInfoObj) {
        currentAzureInfoSubject.onNext(azureInfo)
        // updateConnectionStringFromObj(azureInfo)
    }

    fun verifyAzureInfo(azureInfo: AzureInfoObj): Boolean {
        return isConnectionStringValid(azureInfo, false)
    }

}