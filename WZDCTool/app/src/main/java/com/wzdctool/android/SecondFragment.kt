package com.wzdctool.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.dataclasses.SecondFragmentUIObj
import com.wzdctool.android.dataclasses.gps_status
import com.wzdctool.android.dataclasses.gps_type
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataClassesRepository.activeLocationSourceSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSourcesSubject
import com.wzdctool.android.repos.DataClassesRepository.locationSubject
import com.wzdctool.android.repos.DataClassesRepository.toastNotificationSubject
import com.wzdctool.android.repos.DataFileRepository.dataFileSubject
import com.wzdctool.android.repos.DataFileRepository.markerSubject
import rx.Subscription
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
    private var mMap: GoogleMap? = null
    private lateinit var mMapView: MapView

    private val subscriptions: MutableList<Subscription> = mutableListOf()

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
    val laneLayouts = listOf(
        0,
        R.id.lane1_ll,
        R.id.lane2_ll,
        R.id.lane3_ll,
        R.id.lane4_ll,
        R.id.lane5_ll,
        R.id.lane6_ll,
        R.id.lane7_ll,
        R.id.lane8_ll
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

        mMapView = view.findViewById(R.id.mapView)
        mMapView.onCreate(savedInstanceState)
        mMapView.getMapAsync(this)

        view.findViewById<ImageButton>(R.id.wp).setOnClickListener {
            if (viewModel.wpStat) {
                println("Workers Not Present")
                viewModel.wpStat = false
                view.findViewById<ImageButton>(R.id.wp).setImageDrawable(resources.getDrawable(R.drawable.ic_construction_worker_bw))
                view.findViewById<ImageButton>(R.id.wp).backgroundTintList = resources.getColorStateList(
                    R.color.colorAccent
                )
                val marker = MarkerObj("WP", "False")
                markerSubject.onNext(marker)
            }
            else {
                println("Workers Present")
                viewModel.wpStat = true
                view.findViewById<ImageButton>(R.id.wp).setImageDrawable(resources.getDrawable(R.drawable.ic_construction_worker_small))
                view.findViewById<ImageButton>(R.id.wp).backgroundTintList = resources.getColorStateList(
                    R.color.primary_active
                )
                val marker = MarkerObj("WP", "True")
                markerSubject.onNext(marker)
            }
        }

        view.findViewById<SwitchCompat>(R.id.gpsSwitch).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                activeLocationSourceSubject.onNext(gps_type.usb)
            }
            else {
                activeLocationSourceSubject.onNext(gps_type.internal)
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
            requireView().findViewById<TextView>(R.id.automaticStatus).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.endBtn).isEnabled = false
            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.startBtn).isEnabled = true
            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.VISIBLE
        }
        for (i in 1..viewModel.localUIObj.num_lanes)
            requireView().findViewById<ImageButton>(buttons[i]).isEnabled = false
        requireView().findViewById<ImageButton>(R.id.wp).isEnabled = false
        requireView().findViewById<ImageButton>(R.id.wp).visibility = View.GONE
        requireView().findViewById<LinearLayout>(R.id.lanes_ll).visibility = View.GONE
    }

    fun markRefPtUI() {
        if (viewModel.automaticDetection.value!!) {
            requireView().findViewById<TextView>(R.id.automaticStatus).visibility = View.GONE
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
                requireView().findViewById<ImageButton>(buttons[i]).isEnabled = true
        requireView().findViewById<ImageButton>(R.id.wp).isEnabled = true
        requireView().findViewById<ImageButton>(R.id.wp).visibility = View.VISIBLE
        requireView().findViewById<LinearLayout>(R.id.lanes_ll).visibility = View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun laneClickedUI(laneStatLocal: List<Boolean>) {
        for (lane in 1..viewModel.localUIObj.num_lanes) {
            val statusMsg = requireView().findViewById<ImageButton>(buttons[lane])
            if (lane != viewModel.localUIObj.data_lane) { //Check if driven lane
                if (!laneStatLocal[lane]) {
                    //change open image
                    val button = requireView().findViewById<ImageButton>(buttons[lane])
                    button.setImageDrawable(resources.getDrawable(R.drawable.ic_road_nolines))
                    button.backgroundTintList = resources.getColorStateList(
                        R.color.colorAccent
                    )
                }
                else {
                    //change close image
                    val button = requireView().findViewById<ImageButton>(buttons[lane])
                    button.setImageDrawable(resources.getDrawable(R.drawable.ic_road_closed))
                    button.backgroundTintList = resources.getColorStateList(
                        R.color.primary_active
                    )
                }
            }
        }
    }

    fun collectionModeUI() {
        if (viewModel.automaticDetection.value!!) {
            requireView().findViewById<TextView>(R.id.automaticStatus).visibility = View.VISIBLE
            requireView().findViewById<TextView>(R.id.automaticStatus).text = "Waiting for Start Point"
            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.GONE
            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.GONE
            requireView().findViewById<ImageButton>(R.id.ref).visibility = View.GONE

//            requireView().findViewById<LinearLayout>(R.id.manual_buttons_ll).visibility = View.GONE
        }
        else {
            requireView().findViewById<TextView>(R.id.automaticStatus).visibility = View.GONE
//            requireView().findViewById<ImageButton>(R.id.startBtn).visibility = View.VISIBLE
////            requireView().findViewById<ImageButton>(R.id.endBtn).visibility = View.GONE
////            requireView().findViewById<ImageButton>(R.id.ref).visibility = View.GONE
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
        mMap!!.isMyLocationEnabled = true
        viewModel.initMap(mMap!!, mMapView)

//        val currLocation = LatLng(locationSubject.value.latitude, locationSubject.value.longitude)
//        val center = CameraUpdateFactory.newLatLngZoom(currLocation, viewModel.zoom.toFloat())
//        mMap.animateCamera(center, 10, null);
    }

    private fun removeSubscriptions() {
        for (subscription in subscriptions) {
            subscription.unsubscribe()
        }
    }

    private fun addSubscriptions() {
        subscriptions.add(dataFileSubject.subscribe {
            viewModel.uploadDataFile(it)
        })

        subscriptions.add(locationSubject.subscribe {
            viewModel.checkLocation(it)
        })

        subscriptions.add(locationSourcesSubject.subscribe {
            // Internal GPS
            if (it.internal == gps_status.valid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOff)
                textView.setTextColor(resources.getColor(R.color.usb_status_valid))
            } else if (it.internal == gps_status.invalid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOff)
                textView.setTextColor(resources.getColor(R.color.usb_status_invalid))
            } else if (it.internal == gps_status.disconnected) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOff)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_disconnected))
            }

            // USB GPS
            if (it.usb == gps_status.valid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_valid))
            } else if (it.usb == gps_status.invalid) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.setTextColor(resources.getColor(R.color.usb_status_invalid))

                val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 900 //You can manage the blinking time with this parameter
                anim.startOffset = 20
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                textView.startAnimation(anim)
            } else if (it.usb == gps_status.disconnected) {
                val textView = requireView().findViewById<TextView>(R.id.locationSourceOn)
                textView.clearAnimation()
                textView.setTextColor(resources.getColor(R.color.usb_status_disconnected))
            }

            val gps_switch = requireView().findViewById<SwitchCompat>(R.id.gpsSwitch)
            gps_switch.isEnabled = it.internal == gps_status.valid && it.usb == gps_status.valid
        })

        subscriptions.add(activeLocationSourceSubject.subscribe {
            val gps_switch = requireView().findViewById<SwitchCompat>(R.id.gpsSwitch)
            if (it == gps_type.internal) {
                gps_switch.isChecked = false
            } else if (it == gps_type.usb) { // if (usbLocationValid.value)
                gps_switch.isChecked = true
            } else {
                requireView().findViewById<Button>(R.id.button_first).isEnabled = false
            }
        })

        subscriptions.add(DataClassesRepository.rsmStatus.subscribe {
            requireView().findViewById<CheckBox>(R.id.checkBox3).isChecked = it
        })

        subscriptions.add(locationSubject.subscribe {
            println("updating map")
            viewModel.updateMapLocation(it, mMap)
        })
    }

    override fun onResume() {
        super.onResume()
        addSubscriptions()
        try {
            mMapView.onResume()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onPause() {
        super.onPause()
        removeSubscriptions()
        try {
            mMapView.onPause()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeSubscriptions()
        val marker = MarkerObj("Cancel", "")
        markerSubject.onNext(marker)
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

        viewModel.navigationLiveData.observe(viewLifecycleOwner, {
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

        // addSubscriptions()
    }

    private fun updateUI() {

    }

    private fun ititializeLaneBtns(numLanes: Int, dataLane: Int) {
        println("Data Lane: $dataLane")

        //lane3_ll
        //laneLine3_4

        requireView().findViewById<ImageButton>(R.id.lane1btn).setOnClickListener {
            viewModel.laneClicked(1)
        }

//        val carImageViewparams = requireView().findViewById<ImageView>(R.id.carImageView).layoutParams as ConstraintLayout.LayoutParams
//        carImageViewparams.endToEnd = buttons[dataLane]
//        carImageViewparams.startToStart = buttons[dataLane]

        //val drivenStatusText = requireView().findViewById<TextView>(statusList[dataLane])
        //drivenStatusText.setText(drivenStatusText.text)
        // println(drivenStatusText.text)

        // val adapter = AdapterView<TextView>(this)

        //drivenStatusText.text = resources.getString(R.string.status_driven)
        //println(drivenStatusText.text)
        //drivenStatusText.setTextColor(resources.getColor(R.color.status_driven))
        //requireView().findViewById<Button>(buttons[dataLane]).isClickable = false

        val layout_params = requireView().findViewById<LinearLayout>(R.id.lanes_ll).layoutParams
        layout_params.width = ((requireView().parent as View).width * 0.9 * numLanes/8).toInt()
        println(layout_params.width)
        requireView().findViewById<LinearLayout>(R.id.lanes_ll).layoutParams = layout_params


        requireView().findViewById<ImageButton>(buttons[dataLane]).setImageDrawable(resources.getDrawable(R.drawable.ic_road_driven_2))




//        val btn1params = requireView().findViewById<ToggleButton>(R.id.lane1btn).layoutParams as ConstraintLayout.LayoutParams
        if (numLanes <= 1 ) {
            // TODO: Throw exception
            return
        }
        else if (numLanes > 1) {
            for (i in (min(numLanes, 8)+1)..8) {
                val laneLayout = requireView().findViewById<LinearLayout>(laneLayouts[i])
                laneLayout.visibility = View.GONE

                //val laneLine = requireView().findViewById<ImageView>(laneLines[i - 1])
                //laneLine.visibility = View.INVISIBLE

//                val button = requireView().findViewById<ToggleButton>(buttons[i])
//                button.visibility = View.INVISIBLE
//                val status = requireView().findViewById<TextView>(statusList[i])
//                status.visibility = View.INVISIBLE
//                val textView = requireView().findViewById<TextView>(textViewList[i])
//                textView.visibility = View.INVISIBLE
            }
            for (i in 2..min(numLanes, 8)) {
                val button = requireView().findViewById<ImageButton>(buttons[i])
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