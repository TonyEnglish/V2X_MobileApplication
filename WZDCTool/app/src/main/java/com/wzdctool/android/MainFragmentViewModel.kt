package com.wzdctool.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun uploadDataFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            if (DataClassesRepository.isInternetAvailable()) {
                println("Attempting to upload")
                DataFileRepository.uploadAllDataFiles()
            }
            else {
                println("No Internet")
            }
        }
    }
}