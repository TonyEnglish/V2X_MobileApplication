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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.internetStatusSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.azureInfoRepository.currentConnectionStringSubject
import kotlinx.coroutines.launch
import rx.Subscription

class MainFragment : Fragment() {

//    companion object {
//        fun newInstance() = MainFragment()
//    }

    private lateinit var viewModel: MainFragmentViewModel

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?


    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("onViewCreated")

        view.findViewById<Button>(R.id.downloadConfigButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_downloadFragment)
        }

        view.findViewById<Button>(R.id.settingsButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SettingsFragment)
        }

        view.findViewById<Button>(R.id.createMapButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_FirstFragment)
        }

        val uploadButton = view.findViewById<Button>(R.id.uploadbutton)
        uploadButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_uploadFragment)
        }
        uploadButton.visibility = if (DataClassesRepository.automaticUploadSubject.value) View.GONE else View.VISIBLE


        if (currentConnectionStringSubject.value != null) {
            view.findViewById<Button>(R.id.createMapButton).isEnabled = true
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainFragmentViewModel::class.java)
        // TODO: Use the ViewModel

        if (DataClassesRepository.automaticUploadSubject.value) {
            viewModel.uploadDataFiles()
        }
    }

    private fun addSubscriptions() {
        subscriptions.add(internetStatusSubject.subscribe {
            requireView().findViewById<Button>(R.id.downloadConfigButton).isEnabled = it

            requireView().findViewById<Button>(R.id.uploadbutton).isEnabled = (it && DataFileRepository.getDataFilesList().isNotEmpty())
        })
    }

    private fun removeSubscriptions() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
    }

    override fun onPause() {
        super.onPause()
        removeSubscriptions()
    }

    override fun onResume() {
        super.onResume()
        addSubscriptions()
    }

}