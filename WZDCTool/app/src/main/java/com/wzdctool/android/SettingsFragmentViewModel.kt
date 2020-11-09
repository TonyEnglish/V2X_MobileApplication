package com.wzdctool.android

import androidx.lifecycle.ViewModel
import com.wzdctool.android.dataclasses.azureInfoObj
import com.wzdctool.android.repos.azureInfoRepository.currentAzureInfoSubject
import com.wzdctool.android.repos.azureInfoRepository.isConnectionStringValid
import com.wzdctool.android.repos.azureInfoRepository.updateConnectionStringFromObj

class SettingsViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun saveSettings(azureInfo: azureInfoObj) {
        currentAzureInfoSubject.onNext(azureInfo)
        // updateConnectionStringFromObj(azureInfo)
    }

    fun verifyAzureInfo(azureInfo: azureInfoObj): Boolean {
        return isConnectionStringValid(azureInfo, false)
    }

}