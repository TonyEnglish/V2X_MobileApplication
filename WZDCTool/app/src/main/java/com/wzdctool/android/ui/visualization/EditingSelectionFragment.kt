package com.wzdctool.android.ui.visualization

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.SparseBooleanArray
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.Constants
import com.wzdctool.android.R
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataFileRepository
import rx.Subscription


/**
 *  Select data file to visualize/edit
 *
 *
 */

class EditingSelectionFragment : Fragment() {

    private lateinit var viewModel: EditingSelectionFragmentViewModel

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.editing_selection_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = requireView().findViewById<ListView>(R.id.list)
        val configList = ConfigurationRepository.getLocalConfigList()

        view.findViewById<Button>(R.id.button).setOnClickListener {
            val selectedItems = mutableListOf<String>()
            val len: Int = list.count
            val checked: SparseBooleanArray = list.checkedItemPositions
            for (i in 0 until len) if (checked[i]) {
                println(list.getItemAtPosition(i).toString())
                selectedItems.add(list.getItemAtPosition(i).toString())
            }
            if (selectedItems.isNotEmpty()) {
                println(selectedItems[0])
//                if (hasConfig(selectedItems[0], configList)) {
                    val fileName = "${Constants.PENDING_UPLOAD_DIRECTORY}/${selectedItems[0]}"
                    val visualizationObj = DataFileRepository.getVisualizationObj(fileName)
                    DataClassesRepository.visualizationObj = visualizationObj

                    findNavController().navigate(R.id.action_editingSelectionFragment_to_editingFragment)
//                }
//                else {
//                    toastNotificationSubject.onNext("No config found for selected file")
//                }
            }
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(EditingSelectionFragmentViewModel::class.java)
        // TODO: Use the ViewModel

        val fileList = DataFileRepository.getDataFilesList()

        val list = requireView().findViewById<ListView>(R.id.list)
        val adapter = ArrayAdapter(
            this.requireContext(),
            android.R.layout.simple_list_item_single_choice, fileList
        )

        list.choiceMode = ListView.CHOICE_MODE_SINGLE
        list.adapter = adapter
        adapter!!.notifyDataSetChanged()
    }

    fun hasConfig(dataFile: String, configFiles: List<String>): Boolean {
        val wzID = dataFile.removePrefix("path-data--").removeSuffix(".csv").removeSuffix("--update-image")
        for (configName in configFiles) {
            if (configName.contains(wzID)) {
                return true
            }
        }
        return false
    }

}