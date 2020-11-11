package com.wzdctool.android

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    var navigationLiveData = MutableLiveData<Int>()

    fun updateCloudConfigFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateCloudConfigList()
        }
    }

    fun downloadConfigFiles(configList: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.downloadNewConfigFiles(configList)
            navigationLiveData.postValue(R.id.action_downloadFragment_to_MainFragment)
        }
    }
}