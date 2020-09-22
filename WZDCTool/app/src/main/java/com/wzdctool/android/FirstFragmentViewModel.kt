package com.wzdctool.android

import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzdctool.android.repos.ConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirstFragmentViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun initStuffs() {

        viewModelScope.launch(Dispatchers.IO) {
            ConfigurationRepository.updateConfigList()
            // ConfigurationRepository.activateConfig("config--road-name--description.json")
        }
    }
}