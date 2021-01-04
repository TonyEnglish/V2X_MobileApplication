package com.wzdctool.android.ui.settings

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.R
import com.wzdctool.android.dataclasses.AzureInfoObj
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.notificationSubject
import com.wzdctool.android.repos.AzureInfoRepository.currentAzureInfoSubject
import rx.Subscription


/**
 *  Settings page
 *
 *
 */

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            processAzureSettings()
        }

        view.findViewById<ImageButton>(R.id.help_button).setOnClickListener() {
            findNavController().navigate(R.id.helpFragment)
        }

        if (currentAzureInfoSubject.value != null) {
            requireView().findViewById<EditText>(R.id.editTextAccountName).setText(currentAzureInfoSubject.value.account_name)
            requireView().findViewById<EditText>(R.id.editTextAccountKey).setText(currentAzureInfoSubject.value.account_key)
        }

        val autoUploadSwitch = requireView().findViewById<SwitchCompat>(R.id.autoUploadSwitch)
        autoUploadSwitch.isChecked = DataClassesRepository.automaticUploadSubject.value
        autoUploadSwitch.setOnCheckedChangeListener { _, isChecked ->
            DataClassesRepository.automaticUploadSubject.onNext(isChecked)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun processAzureSettings(): Boolean {
        val account_name: String = requireView().findViewById<EditText>(R.id.editTextAccountName).text.toString()
        val account_key: String = requireView().findViewById<EditText>(R.id.editTextAccountKey).text.toString()
        val azureInfo = AzureInfoObj(account_name, account_key)

        val valid = viewModel.verifyAzureInfo(azureInfo)
        return if (valid) {
            viewModel.saveSettings(azureInfo)
            notificationSubject.onNext("Azure Information Saved")
            findNavController().navigate(R.id.action_SettingsFragment_to_MainFragment)
            true
        } else {
            notificationSubject.onNext("Invalid Azure Information")
            false
        }
    }

}