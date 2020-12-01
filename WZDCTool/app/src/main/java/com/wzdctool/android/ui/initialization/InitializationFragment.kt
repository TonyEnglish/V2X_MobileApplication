package com.wzdctool.android.ui.initialization

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wzdctool.android.R

class InitializationFragment : Fragment() {

    companion object {
        fun newInstance() = InitializationFragment()
    }

    private lateinit var viewModel: InitializationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.configuration_fragment, container, false)
        // return inflater.inflate(R.layout.initialization_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("TESTING")
//        view.findViewById<Button>(R.id.button_first).setOnClickListener {
//            // (activity as MainActivity).saveDataFile()
//            findNavController().navigate(R.id.action_initializationFragment_to_dataCollectionFragment)
//        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(InitializationViewModel::class.java)
        // TODO: Use the ViewModel
    }

}