package com.wzdctool.android.ui.settings

import androidx.lifecycle.ViewModel
import com.wzdctool.android.dataclasses.AzureInfoObj
import com.wzdctool.android.repos.AzureInfoRepository.currentAzureInfoSubject
import com.wzdctool.android.repos.AzureInfoRepository.isConnectionStringValid

class SettingsViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    // Save azure settings. Only call if settings are valid
    fun saveSettings(azureInfo: AzureInfoObj) {
        currentAzureInfoSubject.onNext(azureInfo)
        // updateConnectionStringFromObj(azureInfo)
    }

    // Check azure info against saved hash value
    fun verifyAzureInfo(azureInfo: AzureInfoObj): Boolean {
        return isConnectionStringValid(azureInfo, false)
    }

}