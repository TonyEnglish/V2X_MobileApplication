package com.wzdctool.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.dataclasses.SecondFragmentUIObj
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataFileRepository.dataFileSubject
import com.wzdctool.android.repos.DataFileRepository.markerSubject
import com.wzdctool.android.services.LocationService
import kotlin.math.*


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
    private lateinit var mMapView: MapView

    val buttons = listOf(
        0,
        R.id.lane1btn,
        R.id.lane2btn,
        R.id.lane3btn,
        R.id.lane4btn,
        R.id.lane5btn,
        R.id.lane6btn,
        R.id.lane7btn,
        R.id.lane8btn
    )
    val statusList = listOf(
        0,
        R.id.lane1status,
        R.id.lane2status,
        R.id.lane3status,
        R.id.lane4status
    )
    val textViewList = listOf(
        0,
        R.id.lane1textView,
        R.id.lane2textView,
        R.id.lane3textView,
        R.id.lane4textView
    )
    val laneLines = listOf(
        0,
        R.id.laneLine1_2,
        R.id.laneLine2_3,
        R.id.laneLine3_4,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMapView = view.findViewById(R.id.mapView)
        mMapView.onCreate(savedInstanceState)
        mMapView.getMapAsync(this)

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
    }

    fun startDataCollectionUI() {
        if (viewModel.automaticDetection.value!!) {
            // TODO: stuffs
        }
        else {
            requireView().findViewById<Button>(R.id.startBtn).isEnabled = false
            requireView().findViewById<Button>(R.id.ref).isEnabled = true
        }
    }

    fun stopDataCollectionUI() {
        if (viewModel.automaticDetection.value!!) {
            // TODO: stuffs
        }
        else {
            requireView().findViewById<Button>(R.id.endBtn).isEnabled = false
            requireView().findViewById<Button>(R.id.wp).isEnabled = false
            requireView().findViewById<Button>(R.id.startBtn).isEnabled = true
        }
        for (i in 1..viewModel.localUIObj.num_lanes)
            requireView().findViewById<ToggleButton>(buttons[i]).isEnabled = false
    }

    fun markRefPtUI() {
        if (viewModel.automaticDetection.value!!) {
            // TODO: stuffs
        }
        else {
            requireView().findViewById<Button>(R.id.ref).isEnabled = false
            requireView().findViewById<Button>(R.id.endBtn).isEnabled = true
            requireView().findViewById<Button>(R.id.wp).isEnabled = true
        }
        for (i in 1..viewModel.localUIObj.num_lanes)
            if (i != viewModel.localUIObj.data_lane)
                requireView().findViewById<ToggleButton>(buttons[i]).isEnabled = true
    }

    fun laneClickedUI(laneStatLocal: List<Boolean>) {
        for (lane in 1..viewModel.localUIObj.num_lanes) {
            val statusMsg = requireView().findViewById<TextView>(statusList[lane])
            if (!laneStatLocal[lane]) {
                statusMsg.text = resources.getString(R.string.status_open)
                statusMsg.setTextColor(resources.getColor(R.color.status_open))
            }
            else {
                statusMsg.text = resources.getString(R.string.status_closed)
                statusMsg.setTextColor(resources.getColor(R.color.status_closed))
            }
        }
    }

    fun collectionModeUI() {
        if (viewModel.automaticDetection.value!!) {
            requireView().findViewById<Button>(R.id.startBtn).visibility = View.INVISIBLE
            requireView().findViewById<Button>(R.id.endBtn).visibility = View.INVISIBLE
            requireView().findViewById<Button>(R.id.ref).visibility = View.INVISIBLE

            requireView().findViewById<Guideline>(R.id.wp_guideline).setGuidelineEnd(resources.getDimension(R.dimen.wp_guideline_height_auto).toInt())
            requireView().findViewById<Guideline>(R.id.lane_guideline).setGuidelineEnd(resources.getDimension(R.dimen.lane_guideline_height_auto).toInt())
            requireView().findViewById<FrameLayout>(R.id.button_background).layoutParams.height = resources.getDimension(R.dimen.lane_guideline_height_auto).toInt() + 10
        }
        else {
            requireView().findViewById<Button>(R.id.startBtn).visibility = View.VISIBLE
            requireView().findViewById<Button>(R.id.endBtn).visibility = View.VISIBLE
            requireView().findViewById<Button>(R.id.ref).visibility = View.VISIBLE

            requireView().findViewById<Guideline>(R.id.wp_guideline).setGuidelineEnd(resources.getDimension(R.dimen.wp_guideline_height_manual).toInt())
            requireView().findViewById<Guideline>(R.id.lane_guideline).setGuidelineEnd(resources.getDimension(R.dimen.lane_guideline_height_manual).toInt())
            requireView().findViewById<FrameLayout>(R.id.button_background).layoutParams.height = resources.getDimension(R.dimen.lane_guideline_height_manual).toInt() + 10
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.isMyLocationEnabled = true
        viewModel.initMap(mMap, mMapView)

        if (viewModel.automaticDetection.value!!) {
            locationSubject.subscribe{
                viewModel.updateMapLocation(it, mMap)
                viewModel.checkLocation(it)
            }
        }
        else {
            locationSubject.subscribe{
                viewModel.updateMapLocation(it, mMap)
            }
        }

//        val currLocation = LatLng(locationSubject.value.latitude, locationSubject.value.longitude)
//        val center = CameraUpdateFactory.newLatLngZoom(currLocation, viewModel.zoom.toFloat())
//        mMap.animateCamera(center, 10, null);
    }

    override fun onResume() {
        super.onResume()
        try {
            mMapView.onResume()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            mMapView.onPause()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mMapView.onDestroy()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            mMapView.onLowMemory()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SecondFragmentViewModel::class.java)

        viewModel.initializeUI(DataClassesRepository.dataCollectionObj)
        ititializeLaneBtns(viewModel.localUIObj.num_lanes, viewModel.localUIObj.data_lane)

        dataFileSubject.subscribe {
            viewModel.uploadDataFile(it)
        }

        viewModel.navigationLiveData.observe(viewLifecycleOwner, {   findNavController().navigate(it)    })

        viewModel.dataLog.observe(viewLifecycleOwner, {
            if (it)
                startDataCollectionUI()
            else
                stopDataCollectionUI()
        })

        viewModel.gotRP.observe(viewLifecycleOwner, {
            if (it)
                markRefPtUI()
            // else
            // TODO: Something??
        })

        viewModel.laneStat.observe(viewLifecycleOwner, {
            laneClickedUI(it)
        })

        viewModel.automaticDetection.observe(viewLifecycleOwner, {
            collectionModeUI()
        })

        // if (!viewModel.automaticDetection) {
        requireView().findViewById<Button>(R.id.startBtn).setOnClickListener {
            viewModel.startDataCollection()
        }

        requireView().findViewById<Button>(R.id.endBtn).setOnClickListener {
            viewModel.stopDataCollection()
        }

        requireView().findViewById<Button>(R.id.ref).setOnClickListener {
            viewModel.markRefPt()
        }
        // }
    }

    private fun updateUI() {

    }

    private fun ititializeLaneBtns(numLanes: Int, dataLane: Int) {
        requireView().findViewById<Button>(R.id.lane1btn).setOnClickListener {
            viewModel.laneClicked(1)
        }

        val carImageViewparams = requireView().findViewById<ImageView>(R.id.carImageView).layoutParams as ConstraintLayout.LayoutParams
        carImageViewparams.endToEnd = buttons[dataLane]
        carImageViewparams.startToStart = buttons[dataLane]

        val textViewParams = requireView().findViewById<TextView>(statusList[dataLane])
        textViewParams.visibility = View.GONE

        requireView().findViewById<Button>(buttons[dataLane]).isClickable = false

        val btn1params = requireView().findViewById<ToggleButton>(R.id.lane1btn).layoutParams as ConstraintLayout.LayoutParams
        if (numLanes <= 1 ) {
            // TODO: Throw exception
            return
        }
        else if (numLanes > 1) {
            for (i in (min(numLanes, 4)+1)..4) {
                val button = requireView().findViewById<ToggleButton>(buttons[i])
                button.visibility = View.INVISIBLE
                val status = requireView().findViewById<TextView>(statusList[i])
                status.visibility = View.INVISIBLE
                val textView = requireView().findViewById<TextView>(textViewList[i])
                textView.visibility = View.INVISIBLE
                val laneLine = requireView().findViewById<ImageView>(laneLines[i-1])
                laneLine.visibility = View.INVISIBLE
            }
            for (i in 2..min(numLanes, 4)) {
                val button = requireView().findViewById<ToggleButton>(buttons[i])
                button.setOnClickListener {
                    viewModel.laneClicked(i)
                }
            }
        }
//            btn1params.endToStart = R.id.lane2btn
//            btn1params.startToStart = 0
//
//            for (i in 2..min(numLanes, 4)) {
//                val button = requireView().findViewById<ToggleButton>(buttons[i])
//                val params = button.layoutParams as ConstraintLayout.LayoutParams
//                params.startToEnd = buttons[i - 1]
//                if (i == numLanes || i == 4) params.endToEnd = 0
//                else params.endToEnd = buttons[i + 1]
//                button.visibility = View.VISIBLE
//                button.setOnClickListener {
//                    viewModel.laneClicked(i)
//                }
//            }
//
//            // Second row of lane buttons
//            if (numLanes == 5) {
//                val btn5params = requireView().findViewById<ToggleButton>(R.id.lane5btn).layoutParams as ConstraintLayout.LayoutParams
//                btn1params.endToEnd = 0
//                btn1params.startToStart = 0
//                requireView().findViewById<ToggleButton>(R.id.lane5btn).visibility = View.VISIBLE
//            }
//            else if (numLanes > 5) {
//                val btn5params = requireView().findViewById<ToggleButton>(R.id.lane5btn).layoutParams as ConstraintLayout.LayoutParams
//                btn5params.endToStart = R.id.lane6btn
//                btn5params.startToStart = 0
//                requireView().findViewById<ToggleButton>(R.id.lane5btn).visibility = View.VISIBLE
//                for (i in 6..numLanes) {
//                    val button = requireView().findViewById<ToggleButton>(buttons[i])
//                    val params = button.layoutParams as ConstraintLayout.LayoutParams
//                    params.startToEnd = buttons[i - 1]
//                    if (i == numLanes) params.endToEnd = 0
//                    else params.endToEnd = buttons[i + 1]
//                    button.visibility = View.VISIBLE
//                    button.setOnClickListener {
//                        viewModel.laneClicked(i)
//                    }
//                }
//            }

//            app:layout_constraintEnd_toEndOf="@+id/lane1btn"
//            app:layout_constraintStart_toStartOf="@+id/lane1btn"
//        }
    }
}