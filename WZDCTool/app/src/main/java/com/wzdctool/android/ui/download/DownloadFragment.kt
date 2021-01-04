package com.wzdctool.android.ui.download

import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.R
import com.wzdctool.android.repos.ConfigurationRepository
import rx.Subscription
import kotlin.math.roundToInt


/**
 *  Download configuration files
 *
 *
 */


class DownloadFragment : Fragment() {

    private lateinit var viewModel: DownloadFragmentViewModel

    private var adapter: ArrayAdapter<String?>? = null

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.download_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = requireView().findViewById<ListView>(R.id.list)

        view.findViewById<Button>(R.id.button).setOnClickListener {
            val selectedItems = mutableListOf<String>()
            val len: Int = list.count
            val checked: SparseBooleanArray = list.checkedItemPositions
            for (i in 0 until len) if (checked[i]) {
                println(list.getItemAtPosition(i).toString())
                selectedItems.add(list.getItemAtPosition(i).toString())
            }
            disableUI()
            viewModel.downloadConfigFiles(selectedItems)
//            findNavController().navigate(R.id.action_downloadFragment_to_MainFragment)
        }

        view.findViewById<Button>(R.id.clearButton).setOnClickListener {
            val list = view.findViewById<ListView>(R.id.list)
//            for (i in 0 until list.count) {
//                list.setItemChecked(i, false)
//            }
            list.adapter = adapter
            updateSize()
        }

        view.findViewById<Button>(R.id.fillButton).setOnClickListener {
            val list = view.findViewById<ListView>(R.id.list)
            list.adapter = adapter
            for (i in 0 until list.count) {
                list.setItemChecked(i, true)
            }
            updateSize()
        }

        list.setOnItemClickListener { parent, view, position, id ->
            updateSize()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DownloadFragmentViewModel::class.java)
        // TODO: Use the ViewModel

        viewModel.navigationLiveData.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })

        val spinnerObserver = Observer<List<String>> {
            if (it.isNotEmpty())
                requireView().findViewById<Button>(R.id.button).isEnabled = true

            println("Printing config file names")
            for (name in it) {
                println(name)
            }

            val list = requireView().findViewById<ListView>(R.id.list)
            val config_files = resources.getStringArray(R.array.config_files_download)
            adapter = ArrayAdapter(
                this.requireContext(),
                android.R.layout.simple_list_item_multiple_choice, it
            )
            list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            list.adapter = adapter
            adapter!!.notifyDataSetChanged()

            for (configName in ConfigurationRepository.getLocalConfigList()) {
                println(configName)
                println(it.indexOf(configName))
                try {
                    list.setItemChecked(it.indexOf(configName), true)
                }
                catch (e: Exception) {
                    println(e)
                }
            }
            updateSize()
        }
        ConfigurationRepository.cloudConfigListSubject.observe(viewLifecycleOwner, spinnerObserver)

        viewModel.updateCloudConfigFiles()
    }

    private fun disableUI() {
        requireView().findViewById<Button>(R.id.clearButton).isEnabled = false
        requireView().findViewById<Button>(R.id.fillButton).isEnabled = false
        requireView().findViewById<ListView>(R.id.list).isEnabled = false
        requireView().findViewById<Button>(R.id.button).isEnabled = false
    }

    private fun updateSize() {
        val list = requireView().findViewById<ListView>(R.id.list)
        var numFiles = 0
        val checked: SparseBooleanArray = list.checkedItemPositions
        for (i in 0 until list.count) if (checked[i]) {
            numFiles++
        }
        val sizeString = getSizeString(numFiles * 80.0)
        requireView().findViewById<TextView>(R.id.sizeText).text = "Estimated Storage Size: $sizeString"
    }

    private fun getSizeString(sizeKB: Double): String {
        return if (sizeKB > 1024) {
            "${(sizeKB/1024).roundToInt()} MB"
        } else {
            "${sizeKB.roundToInt()} KB"
        }
    }
}