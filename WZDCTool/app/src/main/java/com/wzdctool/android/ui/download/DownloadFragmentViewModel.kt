package com.wzdctool.android.ui.download

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.R
import com.wzdctool.android.repos.ConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    var navigationLiveData = MutableLiveData<Int>()

    // Async. get sorted list of config files
    fun updateCloudConfigFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateCloudConfigList()
        }
    }

    // Async. download specific config files
    fun downloadConfigFiles(configList: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.downloadNewConfigFiles(configList)
            navigationLiveData.postValue(R.id.action_downloadFragment_to_MainFragment)
        }
    }
}