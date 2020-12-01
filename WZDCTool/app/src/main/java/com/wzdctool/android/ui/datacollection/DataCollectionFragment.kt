package com.wzdctool.android.ui.datacollection

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wzdctool.android.R

class DataCollectionFragment : Fragment() {

    companion object {
        fun newInstance() = DataCollectionFragment()
    }

    private lateinit var viewModel: DataCollectionViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.data_collection_fragment, container, false)
        // return inflater.inflate(R.layout.data_collection_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DataCollectionViewModel::class.java)
        // TODO: Use the ViewModel
    }

}