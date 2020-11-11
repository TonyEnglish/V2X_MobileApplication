package com.wzdctool.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun updateCloudConfigFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateCloudConfigList()
        }
    }

    fun downloadConfigFiles(configList: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.downloadNewConfigFiles(configList)
            notificationSubject.onNext("Configuration Files Downloaded")
        }
    }
}