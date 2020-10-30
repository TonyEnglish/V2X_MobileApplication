package com.wzdctool.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.repos.ConfigurationRepository.activeConfigSubject
import com.wzdctool.android.repos.ConfigurationRepository.configListSubject
import com.wzdctool.android.repos.DataClassesRepository.toolbarActiveSubject


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

//    companion object {
//        fun newInstance() = test_fragment()
//    }

    private lateinit var viewModel: FirstFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarActiveSubject.onNext(true)

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
        // .layoutParams.height = 90

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