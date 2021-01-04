package com.wzdctool.android.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.R
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.internetStatusSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.AzureInfoRepository.currentConnectionStringSubject
import rx.Subscription


/**
 *  Main Page
 *
 *
 */


class MainFragment : Fragment() {

//    companion object {
//        fun newInstance() = MainFragment()
//    }

    private lateinit var viewModel: MainFragmentViewModel

    private val subscriptions: MutableList<Subscription> = mutableListOf()

    private var hasConnectionString = false

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

        view.findViewById<ImageButton>(R.id.help_button).setOnClickListener() {
            findNavController().navigate(R.id.helpFragment)
        }

        view.findViewById<Button>(R.id.settingsButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SettingsFragment)
        }

        view.findViewById<Button>(R.id.createMapButton).setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_FirstFragment)
        }

        view.findViewById<Button>(R.id.viewMapButton).setOnClickListener{
//            val testFileName = "${Constants.PENDING_UPLOAD_DIRECTORY}/path-data--sample-work-zone--white-rock-cir--update-image.csv"
//            val visualizationObj = DataFileRepository.getVisualizationObj(testFileName)
//            DataClassesRepository.visualizationObj = visualizationObj
            findNavController().navigate(R.id.action_MainFragment_to_editingSelectionFragment)
        }

        val uploadButton = view.findViewById<Button>(R.id.uploadbutton)
        uploadButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_uploadFragment)
        }
        uploadButton.visibility = if (DataClassesRepository.automaticUploadSubject.value) View.GONE else View.VISIBLE


        if (currentConnectionStringSubject.value != null) {
            hasConnectionString = true
            view.findViewById<Button>(R.id.createMapButton).isEnabled = true
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainFragmentViewModel::class.java)
        // TODO: Use the ViewModel

        // No longer automatically uploading data files
//        if (DataClassesRepository.automaticUploadSubject.value) {
//            viewModel.uploadDataFiles()
//        }
    }

    private fun addSubscriptions() {
        subscriptions.add(internetStatusSubject.subscribe {
            requireView().findViewById<Button>(R.id.downloadConfigButton).isEnabled =
                (it && hasConnectionString)

            val enabled = (it && DataFileRepository.getDataFilesList().isNotEmpty() && hasConnectionString)
            val uploadButton = requireView().findViewById<Button>(R.id.uploadbutton)
            uploadButton.isEnabled = enabled
            if (enabled) {
                val animation: Animation =
                    AlphaAnimation(1f, .3f) // Change alpha from fully visible to invisible
                animation.duration = 800
                animation.interpolator = LinearInterpolator() // do not alter animation rate
                animation.repeatCount = Animation.INFINITE // Repeat animation infinitely
                animation.repeatMode = Animation.REVERSE // Reverse animation at the end so the button will fade back in
                uploadButton.startAnimation(animation)
            }
            else {
                uploadButton.clearAnimation()
            }

            requireView().findViewById<Button>(R.id.viewMapButton).isEnabled =
                DataFileRepository.getDataFilesList().isNotEmpty()
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