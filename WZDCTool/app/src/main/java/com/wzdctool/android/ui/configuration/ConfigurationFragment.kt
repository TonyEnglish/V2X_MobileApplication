package com.wzdctool.android.ui.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.R
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.dataclasses.gps_status
import com.wzdctool.android.dataclasses.gps_type
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.ConfigurationRepository.activeConfigSubject
import com.wzdctool.android.repos.ConfigurationRepository.activeWZIDSubject
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.rsmStatus
import rx.Subscription


/**
 *  Configure settings before data collection.
 *
 *  Establish GPS device connection, verify messages to be generated,
 *      change the start and end data collection mode, select local configuration files to import,
 *      start data collection
 *
 *
 */
class ConfigurationFragment : Fragment() {

    private lateinit var viewModel: ConfigurationFragmentViewModel
    private var isGpsValid = false
    private var isConfigValid = false

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.configuration_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.startDataCollectionButton).setOnClickListener {
            viewModel.updateDataCollectionObj()
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        // Help button (small question mark in top right)
        view.findViewById<ImageButton>(R.id.help_button).setOnClickListener() {
            findNavController().navigate(R.id.helpFragment)
        }

        view.findViewById<Spinner>(R.id.configSpinner).onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                isConfigValid = false
                requireView().findViewById<Button>(R.id.startDataCollectionButton).isEnabled = isGpsValid && isConfigValid

                val configName: String = parent?.getItemAtPosition(position).toString()
                val success: Boolean = ConfigurationRepository.activateConfig(configName)
//                viewModel.activateConfig(configName)
            }
        }


        view.findViewById<SwitchCompat>(R.id.collectionModeSwitchGuideline).setOnCheckedChangeListener { _, isChecked ->
            viewModel.automaticDetection = isChecked
            // do something, the isChecked will be
            // true if the switch is in the On position
        }

        view.findViewById<SwitchCompat>(R.id.gpsSwitch).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                activeLocationSourceSubject.onNext(gps_type.usb)
            } else {
                activeLocationSourceSubject.onNext(gps_type.internal)
            }
        }

//        view.findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
//            refreshSpinner()
//        }

        addSubscriptions()
    }

    private fun removeSubscriptions() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
    }

    private fun addSubscriptions() {
        // triggered by gps location sources on status change
        subscriptions.add(locationSourcesSubject.subscribe {
            // Internal GPS
            if (it.internal == gps_status.valid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOff)
                textView.setTextColor(resources.getColor(R.color.usb_status_valid))
            } else if (it.internal == gps_status.invalid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOff)
                textView.setTextColor(resources.getColor(R.color.usb_status_invalid))
            } else if (it.internal == gps_status.disconnected) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOff)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_disconnected))
            }

            // USB GPS
            if (it.usb == gps_status.valid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_valid))
            } else if (it.usb == gps_status.invalid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.setTextColor(resources.getColor(R.color.usb_status_invalid))

                val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 900 //You can manage the blinking time with this parameter
                anim.startOffset = 20
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                textView.startAnimation(anim)
            } else if (it.usb == gps_status.disconnected) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_disconnected))
            }

            val gps_switch = requireView().findViewById<SwitchCompat>(R.id.gpsSwitch)
            gps_switch.isEnabled = it.internal == gps_status.valid && it.usb == gps_status.valid
        })

        // triggered by change in active location source
        subscriptions.add(activeLocationSourceSubject.subscribe {
            val gps_switch = requireView().findViewById<SwitchCompat>(R.id.gpsSwitch)
            if (it == gps_type.internal) {
                gps_switch.isChecked = false
                isGpsValid = true
            } else if (it == gps_type.usb) { // if (usbLocationValid.value)
                gps_switch.isChecked = true
                isGpsValid = true
            } else {
                isGpsValid = false
            }
            requireView().findViewById<Button>(R.id.startDataCollectionButton).isEnabled =
                isGpsValid && isConfigValid
        })

        // triggered by main activity RSM status change
        subscriptions.add(rsmStatus.subscribe {
            requireView().findViewById<CheckBox>(R.id.checkBox3).isChecked = it
        })

        // triggered by configuration file import
        val configObserver = Observer<String> {
            requireView().findViewById<TextView>(R.id.activeConfigTextView).text = "Active Config: $it"
        }
        activeWZIDSubject.observe(viewLifecycleOwner, configObserver)
    }

    override fun onResume() {
        super.onResume()
        addSubscriptions()
    }

    override fun onPause() {
        super.onPause()
        removeSubscriptions()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeSubscriptions()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ConfigurationFragmentViewModel::class.java)

        refreshSpinner()

        // Observe active configuration object
        val configObserver = Observer<ConfigurationObj> {
            if (view != null) {
                isConfigValid = true
                requireView().findViewById<Button>(R.id.startDataCollectionButton).isEnabled = isGpsValid && isConfigValid
            }
            viewModel.updateDataCollectionObj()
        }
        activeConfigSubject.observe(viewLifecycleOwner, configObserver)

        viewModel.updateConfigList()


        // TODO: Use the ViewModel
    }

    private fun refreshSpinner() {
        val spinner = requireView().findViewById(R.id.configSpinner) as Spinner
        val list: Array<String> = resources.getStringArray(R.array.config_files)

        val configList = ConfigurationRepository.getLocalConfigList()
        if (configList.isNotEmpty()) {
            spinner.isEnabled = true
            val spinnerAdapter: ArrayAdapter<String> =
                ArrayAdapter<String>(
                    this.requireContext(), android.R.layout.simple_spinner_item, configList
                )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.adapter = spinnerAdapter
            spinnerAdapter.notifyDataSetChanged()
        }
        else {
            spinner.isEnabled = false
        }
    }
}