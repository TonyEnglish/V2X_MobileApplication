package com.wzdctool.android

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.azureInfoRepository.currentConnectionStringSubject

class MainFragment : Fragment() {

//    companion object {
//        fun newInstance() = MainFragment()
//    }

    private lateinit var viewModel: MainFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("onViewCreated")

        view.findViewById<Button>(R.id.settingsButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SettingsFragment)
        }

        view.findViewById<Button>(R.id.createMapButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_FirstFragment)
        }

        if (currentConnectionStringSubject.value != null) {
            view.findViewById<Button>(R.id.createMapButton).isEnabled = true
        }

        DataClassesRepository.toolbarActiveSubject.onNext(false)
        // requireActivity().findViewById<ConstraintLayout>(R.id.toolbar_stuffs).layoutParams.height = 0

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainFragmentViewModel::class.java)
        // TODO: Use the ViewModel
    }

}