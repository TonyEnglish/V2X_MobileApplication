package com.wzdctool.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.repos.ConfigurationRepository.activeConfigSubject
import com.wzdctool.android.repos.ConfigurationRepository.activeWZIDSubject
import com.wzdctool.android.repos.ConfigurationRepository.configListSubject
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingVar
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.rsmStatus
import com.wzdctool.android.repos.DataClassesRepository.usbGpsStatus
import rx.Subscription


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

//    companion object {
//        fun newInstance() = test_fragment()
//    }

    private lateinit var viewModel: FirstFragmentViewModel

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            viewModel.updateDataCollectionObj()
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        val configSpinner = view.findViewById(R.id.spinner2) as Spinner

        view.findViewById<Button>(R.id.import_config).setOnClickListener {
            view.findViewById<Button>(R.id.button_first).isEnabled = false
            if (configSpinner.selectedItem == null) {
                return@setOnClickListener
            }
            val configName: String = configSpinner.selectedItem.toString()
            viewModel.activateConfig(configName, activity?.filesDir.toString())
        }
        view.findViewById<Switch>(R.id.switch1).setOnClickListener {
            viewModel.automaticDetection = view.findViewById<Switch>(R.id.switch1).isChecked
//            if (activeConfigSubject.value != null)
//                viewModel.updateDataCollectionObj()
        }


        view.findViewById<Switch>(R.id.gpsSwitch).setOnClickListener {
            locationSourceSwitchClicked()
        }
        locationSourceSwitchClicked()

        addSubscriptions()
    }

    private fun removeSubscriptions() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
    }

    private fun addSubscriptions() {
        subscriptions.add(locationSourcesSubject.subscribe {
//            updateLocationSources(it)
//            if (!it.contains(Constants.LOCATION_SOURCE_INTERNAL) && locationSource == Constants.LOCATION_SOURCE_INTERNAL) {
//                findViewById<Switch>(R.id.switch1).isChecked = true
//                findViewById<Switch>(R.id.switch1).isEnabled = false
//                if (it.contains(Constants.LOCATION_SOURCE_USB)) {
//                    activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_USB)
//                }
//
//            }
            if (!it.contains(Constants.LOCATION_SOURCE_USB) && activeLocationSourceSubject.value!! == Constants.LOCATION_SOURCE_USB) {
                requireView().findViewById<Switch>(R.id.gpsSwitch).isChecked = false
                requireView().findViewById<Switch>(R.id.gpsSwitch).isEnabled = false
                if (it.contains(Constants.LOCATION_SOURCE_INTERNAL)) {
                    activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
                }
            }
        })

        subscriptions.add(activeLocationSourceSubject.subscribe {
//            updateLocationSource(it)
            if (it == Constants.LOCATION_SOURCE_INTERNAL) {
                requireView().findViewById<Switch>(R.id.gpsSwitch).isChecked = false
                if (locationSourcesSubject.value!!.contains(Constants.LOCATION_SOURCE_USB)) {
                    requireView().findViewById<Switch>(R.id.gpsSwitch).isEnabled = false
                }
            }
            else { // if (usbLocationValid.value)
                requireView().findViewById<Switch>(R.id.gpsSwitch).isEnabled = true
                requireView().findViewById<Switch>(R.id.gpsSwitch).isChecked = true
            }
        })

        subscriptions.add(usbGpsStatus.subscribe {
            if (it == "valid") {
                activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_USB)
                requireView().findViewById<Switch>(R.id.gpsSwitch).isEnabled = true
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_valid))
//                findViewById<Switch>(R.id.switch1).isChecked = true
//                locationSourceSwitchClicked()
//                locationSourceSwitchClicked()
            }
            else if (it == "invalid") {
                activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
                requireView().findViewById<Switch>(R.id.gpsSwitch).isEnabled = false
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.setTextColor(resources.getColor(R.color.usb_status_invalid))

                val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 900 //You can manage the blinking time with this parameter
                anim.startOffset = 20
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                textView.startAnimation(anim)
//                findViewById<Switch>(R.id.switch1).isChecked = false
//                locationSourceSwitchClicked()
            }
            else if (it == "disconnected") {
                activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
                requireView().findViewById<Switch>(R.id.gpsSwitch).isEnabled = false
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_disconnected))
//                findViewById<Switch>(R.id.switch1).isChecked = false
//                locationSourceSwitchClicked()
            }
        })

        subscriptions.add(locationSubject.subscribe {
            if (rsmStatus.value && it.accuracy > 2) {
                rsmStatus.onNext(false)
            }
            else if (!dataLoggingVar && it.accuracy <= 2) {
                rsmStatus.onNext(true)
            }
        })

        subscriptions.add(rsmStatus.subscribe {
            requireView().findViewById<CheckBox>(R.id.checkBox3).isChecked = it
        })

        val configObserver = Observer<String> {
            requireView().findViewById<TextView>(R.id.activeConfigTextView).text = "Active Config: $it"
        }
        activeWZIDSubject.observe(viewLifecycleOwner, configObserver)
    }

    private fun locationSourceSwitchClicked() {
        if (requireView().findViewById<Switch>(R.id.gpsSwitch).isChecked) {
            // if (locationSource == Constants.LOCATION_SOURCE_USB)
            activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_USB)
        }
        else {
            activeLocationSourceSubject.onNext(Constants.LOCATION_SOURCE_INTERNAL)
        }
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
        viewModel = ViewModelProvider(this).get(FirstFragmentViewModel::class.java)

        val spinnerObserver = Observer<List<String>> {
            println("Printing config file names")
            for (name in it) {
                println(name)
            }
            val spinner = requireView().findViewById(R.id.spinner2) as Spinner
            val list: Array<String> = resources.getStringArray(R.array.config_files)
            //
            val spinnerAdapter: ArrayAdapter<String> =
                ArrayAdapter<String>(
                    this.requireContext(), android.R.layout.simple_spinner_item, it
                )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.adapter = spinnerAdapter
            spinnerAdapter.notifyDataSetChanged()

            // println("Setting to index of: config--${activeWZIDSubject.value}.json")

            try {
                spinner.setSelection(configListSubject.value!!.indexOf("config--${activeWZIDSubject.value}.json"))
            }
            catch (e: Exception) {
                println(e)
            }
        }
        configListSubject.observe(viewLifecycleOwner, spinnerObserver)

        // AzureDownloadConfigFile().execute()
        val configObserver = Observer<ConfigurationObj> {
            println("Configuration object Updated")
            if (view != null) {
                requireView().findViewById<Button>(R.id.button_first).isEnabled = true
            }
            viewModel.updateDataCollectionObj()
        }
        activeConfigSubject.observe(viewLifecycleOwner, configObserver)

        viewModel.updateConfigList()

        // TODO: Use the ViewModel
    }
}