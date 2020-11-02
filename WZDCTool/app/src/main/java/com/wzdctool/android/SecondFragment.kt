package com.wzdctool.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
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
import rx.Subscription
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
    private lateinit var locationSubscription: Subscription

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
    val laneLayouts = listOf(
        0,
        R.id.lane1_ll,
        R.id.lane2_ll,
        R.id.lane3_ll,
        R.id.lane4_ll
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DataClassesRepository.toolbarActiveSubject.onNext(true)

        mMapView = view.findViewById(R.id.mapView)
        mMapView.onCreate(savedInstanceState)
        mMapView.getMapAsync(this)

        view.findViewById<ImageButton>(R.id.wp).setOnClickListener {
            if (viewModel.wpStat) {
                println("Workers Not Present")
                viewModel.wpStat = false
                view.findViewById<ImageButton>(R.id.wp).setImageDrawable(resources.getDrawable(R.drawable.ic_construction_worker_small))
                view.findViewById<ImageButton>(R.id.wp).backgroundTintList = resources.getColorStateList(R.color.colorAccent)
                val marker = MarkerObj("WP", "False")
                markerSubject.onNext(marker)
            }
            else {
                println("Workers Present")
                viewModel.wpStat = true
                view.findViewById<ImageButton>(R.id.wp).setImageDrawable(resources.getDrawable(R.drawable.ic_construction_noworker))
                view.findViewById<ImageButton>(R.id.wp).backgroundTintList = resources.getColorStateList(R.color.primary_active)
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
//            requireView().findViewById<ImageButton>(R.id.startBtn).isEnabled = false
            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.endBtn).isEnabled = false
            requireView().findViewById<ImageButton>(R.id.ref).visibility = View.VISIBLE
//            requireView().findViewById<ImageButton>(R.id.ref).isEnabled = true
        }
    }

    fun stopDataCollectionUI() {
        if (viewModel.automaticDetection.value!!) {
            // TODO: stuffs
        }
        else {
//            requireView().findViewById<ImageButton>(R.id.endBtn).isEnabled = false
            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.startBtn).isEnabled = true
            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.VISIBLE
        }
        for (i in 1..viewModel.localUIObj.num_lanes)
            requireView().findViewById<ToggleButton>(buttons[i]).isEnabled = false
        requireView().findViewById<ImageButton>(R.id.wp).isEnabled = false
    }

    fun markRefPtUI() {
        if (viewModel.automaticDetection.value!!) {
            // TODO: stuffs
        }
        else {
//            requireView().findViewById<ImageButton>(R.id.ref).isEnabled = false
            requireView().findViewById<ImageButton>(R.id.ref).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.endBtn).isEnabled = true
            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.VISIBLE
        }
        for (i in 1..viewModel.localUIObj.num_lanes)
            if (i != viewModel.localUIObj.data_lane)
                requireView().findViewById<ToggleButton>(buttons[i]).isEnabled = true
        requireView().findViewById<ImageButton>(R.id.wp).isEnabled = true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun laneClickedUI(laneStatLocal: List<Boolean>) {
        for (lane in 1..viewModel.localUIObj.num_lanes) {
            val statusMsg = requireView().findViewById<TextView>(statusList[lane])
            if (statusMsg.text != resources.getString(R.string.status_driven)) {
                if (!laneStatLocal[lane]) {
                    statusMsg.text = resources.getString(R.string.status_open)
                    statusMsg.setTextColor(resources.getColor(R.color.status_open))
                    requireView().findViewById<Button>(buttons[lane]).backgroundTintList = resources.getColorStateList(R.color.colorAccent)
                }
                else {
                    statusMsg.text = resources.getString(R.string.status_closed)
                    statusMsg.setTextColor(resources.getColor(R.color.status_closed))
                    requireView().findViewById<Button>(buttons[lane]).backgroundTintList = resources.getColorStateList(R.color.primary_active)
                }
            }
        }
    }

    fun collectionModeUI() {
        if (viewModel.automaticDetection.value!!) {
            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.GONE
            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.GONE
            requireView().findViewById<ImageButton>(R.id.ref).visibility = View.GONE

//            requireView().findViewById<LinearLayout>(R.id.manual_buttons_ll).visibility = View.GONE
        }
        else {
            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.VISIBLE
            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.GONE
            requireView().findViewById<ImageButton>(R.id.ref).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.ref).isEnabled = false

            requireView().findViewById<LinearLayout>(R.id.manual_buttons_ll).visibility = View.VISIBLE
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mMap.isMyLocationEnabled = true
        viewModel.initMap(mMap, mMapView)

        if (viewModel.automaticDetection.value!!) {
            locationSubscription = locationSubject.subscribe{
                println("called")
                viewModel.updateMapLocation(it, mMap)
                viewModel.checkLocation(it)
            }
        }
        else {
            locationSubscription = locationSubject.subscribe{
                println("called")
                viewModel.updateMapLocation(it, mMap)
            }
        }


//        val currLocation = LatLng(locationSubject.value.latitude, locationSubject.value.longitude)
//        val center = CameraUpdateFactory.newLatLngZoom(currLocation, viewModel.zoom.toFloat())
//        mMap.animateCamera(center, 10, null);
    }

    private fun onEnd() {
        locationSubscription.unsubscribe()
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SecondFragmentViewModel::class.java)

        viewModel.initializeUI(DataClassesRepository.dataCollectionObj)
        ititializeLaneBtns(viewModel.localUIObj.num_lanes, viewModel.localUIObj.data_lane)

        dataFileSubject.subscribe {
            viewModel.uploadDataFile(it)
        }

        viewModel.navigationLiveData.observe(viewLifecycleOwner, {
            onEnd()
            findNavController().navigate(it)
        })

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
        requireView().findViewById<ImageButton>(R.id.startBtn).setOnClickListener {
            viewModel.startDataCollection()
        }

        requireView().findViewById<ImageButton>(R.id.endBtn).setOnClickListener {
            viewModel.stopDataCollection()
        }

        requireView().findViewById<ImageButton>(R.id.ref).setOnClickListener {
            viewModel.markRefPt()
        }
        // }
    }

    private fun updateUI() {

    }

    private fun ititializeLaneBtns(numLanes: Int, dataLane: Int) {
        println("Data Lane: $dataLane")

        //lane3_ll
        //laneLine3_4

        requireView().findViewById<Button>(R.id.lane1btn).setOnClickListener {
            viewModel.laneClicked(1)
        }

//        val carImageViewparams = requireView().findViewById<ImageView>(R.id.carImageView).layoutParams as ConstraintLayout.LayoutParams
//        carImageViewparams.endToEnd = buttons[dataLane]
//        carImageViewparams.startToStart = buttons[dataLane]

        val drivenStatusText = requireView().findViewById<TextView>(statusList[dataLane])
        drivenStatusText.setText(drivenStatusText.text)
        // println(drivenStatusText.text)

        // val adapter = AdapterView<TextView>(this)

        drivenStatusText.text = resources.getString(R.string.status_driven)
        println(drivenStatusText.text)
        drivenStatusText.setTextColor(resources.getColor(R.color.status_driven))
        requireView().findViewById<Button>(buttons[dataLane]).isClickable = false

//        val btn1params = requireView().findViewById<ToggleButton>(R.id.lane1btn).layoutParams as ConstraintLayout.LayoutParams
        if (numLanes <= 1 ) {
            // TODO: Throw exception
            return
        }
        else if (numLanes > 1) {
            for (i in (min(numLanes, 4)+1)..4) {
                val laneLayout = requireView().findViewById<LinearLayout>(laneLayouts[i])
                laneLayout.visibility = View.INVISIBLE

                val laneLine = requireView().findViewById<ImageView>(laneLines[i-1])
                laneLine.visibility = View.INVISIBLE

//                val button = requireView().findViewById<ToggleButton>(buttons[i])
//                button.visibility = View.INVISIBLE
//                val status = requireView().findViewById<TextView>(statusList[i])
//                status.visibility = View.INVISIBLE
//                val textView = requireView().findViewById<TextView>(textViewList[i])
//                textView.visibility = View.INVISIBLE
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