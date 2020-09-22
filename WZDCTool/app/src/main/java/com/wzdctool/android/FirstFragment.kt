package com.wzdctool.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.dataclasses.ConfigurationObj
import com.wzdctool.android.repos.ConfigurationRepository
import kotlinx.coroutines.Dispatchers

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

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }


        val spinnerObserver = Observer<List<String>> {
            println("Printing config file names")
            for (name in it) {
                println(name)
            }
            val spinner = view.findViewById(R.id.spinner2) as Spinner
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
        ConfigurationRepository.configListSubject.observe(viewLifecycleOwner, spinnerObserver)

        // AzureDownloadConfigFile().execute()
        val configObserver = Observer<ConfigurationObj> {
            println("Configuration object Updated")
            view.findViewById<Button>(R.id.button_first).isEnabled = true
        }
        ConfigurationRepository.activeConfigSubject.observe(viewLifecycleOwner, configObserver)

        view.findViewById<Button>(R.id.import_config).setOnClickListener {
            view.findViewById<Button>(R.id.button_first).isEnabled = false
            val spinner = view.findViewById(R.id.spinner2) as Spinner
            if (spinner.selectedItem == null) {
                return@setOnClickListener
            }
            val configName: String = spinner.selectedItem.toString()
            ConfigurationRepository.activateConfig(configName)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FirstFragmentViewModel::class.java)
        viewModel.initStuffs()
        // TODO: Use the ViewModel
    }
}