package com.wzdctool.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UploadFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun uploadDataFiles(fileList: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DataFileRepository.uploadDataFiles(fileList)
        }
    }
}