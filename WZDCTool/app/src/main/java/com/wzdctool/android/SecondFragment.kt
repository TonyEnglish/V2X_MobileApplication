package com.wzdctool.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.wzdctool.android.dataclasses.CSVObj
import com.wzdctool.android.dataclasses.DataCollectionObj
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.dataclasses.SecondFragmentUIObj
import com.wzdctool.android.repos.ConfigurationRepository
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.dataLoggingSubject
import com.wzdctool.android.repos.DataFileRepository
import com.wzdctool.android.repos.DataFileRepository.dataFileSubject
import com.wzdctool.android.repos.DataFileRepository.markerSubject
import kotlin.math.min

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(), OnMapReadyCallback {

//    companion object {
//        fun newInstance() = test_fragment()
//    }

    private lateinit var viewModel: SecondFragmentViewModel
    private lateinit var uiObjObserver: Observer<SecondFragmentUIObj>
    private lateinit var localUIObj: SecondFragmentUIObj
    private lateinit var mMap: GoogleMap

    val buttons = listOf(0, R.id.lane1btn, R.id.lane2btn, R.id.lane3btn, R.id.lane4btn, R.id.lane5btn, R.id.lane6btn, R.id.lane7btn, R.id.lane8btn)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ititializeLaneBtns()

//        val configObserver = Observer<DataCollectionObj> {
//            println("Configuration object Updated")
//            // view.findViewById<Button>(R.id.button_first).isEnabled = true
//        }
//        configObserver
//        DataClassesRepository.dataCollectionSubject.observe(viewLifecycleOwner, configObserver)


        view.findViewById<Button>(R.id.startBtn).setOnClickListener {
            println("Data Logging Started")
            val marker = MarkerObj("Data Log", "True")
            markerSubject.onNext(marker)
            // dataLoggingSubject.value = true

            view.findViewById<Button>(R.id.startBtn).isEnabled = false
            view.findViewById<Button>(R.id.ref).isEnabled = true
            // (activity as MainActivity).osw.appendln("stuff")
        }

        view.findViewById<Button>(R.id.endBtn).setOnClickListener {
            println("Data Logging Ended")
            val marker = MarkerObj("Data Log", "False")
            markerSubject.onNext(marker)
            // dataLoggingSubject.value = false

            view.findViewById<Button>(R.id.endBtn).isEnabled = false
            view.findViewById<Button>(R.id.wp).isEnabled = false
            for (i in 1..viewModel.localUIObj.num_lanes)
                view.findViewById<ToggleButton>(buttons[i]).isEnabled = false
            view.findViewById<Button>(R.id.startBtn).isEnabled = true
            // mySnackbar.show()
            // (activity as MainActivity).stopLocationService()
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        view.findViewById<Button>(R.id.ref).setOnClickListener {
            println("Reference Point Marked")
            val marker = MarkerObj("RP", "")
            markerSubject.onNext(marker)
            view.findViewById<Button>(R.id.ref).isEnabled = false
            view.findViewById<Button>(R.id.endBtn).isEnabled = true
            view.findViewById<Button>(R.id.wp).isEnabled = true
            for (i in 1..viewModel.localUIObj.num_lanes)
                view.findViewById<ToggleButton>(buttons[i]).isEnabled = true
        }

        view.findViewById<Button>(R.id.lane1btn).setOnClickListener {
            viewModel.laneClicked(1)
        }

        view.findViewById<Button>(R.id.wp).setOnClickListener {
            if (viewModel.wpStat) {
                println("Workers Not Present")
                viewModel.wpStat = false
                val marker = MarkerObj("WP", "False")
                markerSubject.onNext(marker)
            }
            else {
                println("Workers Present")
                viewModel.wpStat = true
                val marker = MarkerObj("WP", "True")
                markerSubject.onNext(marker)
            }
        }

        // SecondFragment.getMapAsync(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }



//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
////        view.findViewById<Button>(R.id.button_second).setOnClickListener {
////            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
////        }
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SecondFragmentViewModel::class.java)

        viewModel.initializeUI(DataClassesRepository.dataCollectionSubject.value!!)
        ititializeLaneBtns(viewModel.localUIObj.num_lanes)




        // viewModelScope.launch(Dispatchers.IO) {

        dataFileSubject.subscribe {
            viewModel.uploadDataFile(it)
        }
        // notificationText.postValue("Uploaded Path Data")
        // ConfigurationRepository.activateConfig("config--road-name--description.json")
        // }

//        var csvObserver = Observer<CSVObj> {
//            viewModel.writeToDataFile(it)
//        }
//        csvDataSubject.observe(viewLifecycleOwner, csvObserver)

        // TODO: Use the ViewModel
        // viewModel.numLanes
//        uiObjObserver = Observer<SecondFragmentUIObj> {
//            localUIObj = it
//            updateUI()
//        }
//        viewModel.currentUIObj.observe(viewLifecycleOwner, uiObjObserver)
    }

    private fun updateUI() {

    }

    private fun ititializeLaneBtns(numLanes: Int) {
        val btn1params = requireView().findViewById<ToggleButton>(R.id.lane1btn).layoutParams as ConstraintLayout.LayoutParams
        if (numLanes <= 1 ) {
            // TODO: Throw exception
            return
        }
        else if (numLanes > 1) {
            btn1params.endToStart = R.id.lane2btn
            btn1params.startToStart = 0

            for (i in 2..min(numLanes, 4)) {
                val button = requireView().findViewById<ToggleButton>(buttons[i])
                val params = button.layoutParams as ConstraintLayout.LayoutParams
                params.startToEnd = buttons[i-1]
                if (i == numLanes || i == 4) params.endToEnd = 0
                else params.endToEnd = buttons[i+1]
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    viewModel.laneClicked(i)
                }
            }

            // Second row of lane buttons
            if (numLanes == 5) {
                val btn5params = requireView().findViewById<ToggleButton>(R.id.lane5btn).layoutParams as ConstraintLayout.LayoutParams
                btn1params.endToEnd = 0
                btn1params.startToStart = 0
                requireView().findViewById<ToggleButton>(R.id.lane5btn).visibility = View.VISIBLE
            }
            else if (numLanes > 5) {
                val btn5params = requireView().findViewById<ToggleButton>(R.id.lane5btn).layoutParams as ConstraintLayout.LayoutParams
                btn5params.endToStart = R.id.lane6btn
                btn5params.startToStart = 0
                requireView().findViewById<ToggleButton>(R.id.lane5btn).visibility = View.VISIBLE
                for (i in 6..numLanes) {
                    val button = requireView().findViewById<ToggleButton>(buttons[i])
                    val params = button.layoutParams as ConstraintLayout.LayoutParams
                    params.startToEnd = buttons[i-1]
                    if (i == numLanes) params.endToEnd = 0
                    else params.endToEnd = buttons[i+1]
                    button.visibility = View.VISIBLE
                    button.setOnClickListener {
                        viewModel.laneClicked(i)
                    }
                }
            }
        }
    }
}