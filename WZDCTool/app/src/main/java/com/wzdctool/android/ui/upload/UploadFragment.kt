package com.wzdctool.android.ui.upload

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
import com.wzdctool.android.R
import com.wzdctool.android.repos.DataFileRepository
import rx.Subscription


/**
 *  Upload data files
 *
 *
 */

class UploadFragment : Fragment() {

    private lateinit var viewModel: UploadFragmentViewModel

    private var adapter: ArrayAdapter<String?>? = null

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_fragment, container, false)
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
            viewModel.uploadDataFiles(selectedItems)
//            findNavController().navigate(R.id.action_uploadFragment_to_MainFragment)
        }

        view.findViewById<Button>(R.id.clearButton).setOnClickListener {
            val list = view.findViewById<ListView>(R.id.list)
            list.adapter = adapter
        }

        view.findViewById<Button>(R.id.fillButton).setOnClickListener {
            val list = view.findViewById<ListView>(R.id.list)
            list.adapter = adapter
            for (i in 0 until list.count) {
                list.setItemChecked(i, true)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(UploadFragmentViewModel::class.java)
        // TODO: Use the ViewModel

        val fileList = DataFileRepository.getDataFilesList()

        val list = requireView().findViewById<ListView>(R.id.list)
        val config_files = resources.getStringArray(R.array.config_files_download)
        adapter = ArrayAdapter(
            this.requireContext(),
            android.R.layout.simple_list_item_multiple_choice, fileList
        )
        list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        list.adapter = adapter
        adapter!!.notifyDataSetChanged()

        for (i in 0 until list.count) {
            list.setItemChecked(i, true)
        }

        viewModel.navigationLiveData.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })
    }

    private fun disableUI() {
        requireView().findViewById<Button>(R.id.clearButton).isEnabled = false
        requireView().findViewById<Button>(R.id.fillButton).isEnabled = false
        requireView().findViewById<ListView>(R.id.list).isEnabled = false
        requireView().findViewById<Button>(R.id.button).isEnabled = false
    }
}