package com.wzdctool.android.ui.visualization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Marker
import com.wzdctool.android.R
import com.wzdctool.android.dataclasses.Title
import com.wzdctool.android.dataclasses.parseTitleString
import com.wzdctool.android.repos.DataClassesRepository


/**
 *  Visualize and edit mapped work zone
 *
 *
 */


class EditingFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var mMapView: MapView

    private lateinit var viewModel: EditingFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.editing_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMapView = view.findViewById(R.id.mapView)
        mMapView.onCreate(savedInstanceState)
        mMapView.getMapAsync(this)

//        view.findViewById<Button>(R.id.savebutton).setOnClickListener{
//            viewModel.saveEdits()
//        }

        view.findViewById<Button>(R.id.continueButton).setOnClickListener{
            findNavController().navigate(R.id.action_editingFragment_to_MainFragment)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(EditingFragmentViewModel::class.java)

        viewModel.initializeUI(DataClassesRepository.visualizationObj)

        viewModel.navigationLiveData.observe(viewLifecycleOwner, {
            findNavController().navigate(it)
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

//        if (ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
        mMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
//        mMap!!.isMyLocationEnabled = true
        viewModel.initMap(mMap!!, mMapView)

//        val supportMapFragment = SupportMapFragment.newInstance()
//        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
//        fragmentTransaction.add(R.id.map, supportMapFragment)
//        fragmentTransaction.commit()

//        mMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
//            override fun getInfoWindow(marker: Marker): View? {
//                return null
//            }
//
//            override fun getInfoContents(marker: Marker): View {
//                val itemOmanPostAddressMapInfoWindowBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.marker_info_window, null, true);
//                itemOmanPostAddressMapInfoWindowBinding.setData(getRelevantData(marker.getPosition()));
//                itemOmanPostAddressMapInfoWindowBinding.executePendingBindings();
//                return itemOmanPostAddressMapInfoWindowBinding.getRoot();
//            }
//        })

        mMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val infoWindowLayout =
                    activity!!.layoutInflater.inflate(R.layout.marker_info_window, null)
                val linLayout = infoWindowLayout.findViewById<LinearLayout>(R.id.content)
                infoWindowLayout.findViewById<TextView>(R.id.description).text = processTitle(parseTitleString(marker.title))
//                val fragmentContainerLayout =
//                    infoWindowLayout!!.findViewById<LinearLayout>(R.id.content)
//                val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
//                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                transaction.add(fragmentContainerLayout.id, EditingFragment(), "fragment_id")
//                transaction.commit()
                return linLayout
            }

            fun processTitle(title: Title?): String {
                if (title == null) {
                    return "Path Data Point"
                }
                if (title.type == "LC") {
                    return "Lane ${title.value} Closed"
                }
                else if (title.type == "LO") {
                    return "Lane ${title.value} Open"
                }
                else if (title.type == "WP") {
                    if (title.value == "True") {
                        return "Workers Present"
                    }
                    else {
                        return "Workers Not Present"
                    }
                }
                else if (title.type == "RP") {
                    return "Start of Work Zone"
                }
                return ""
            }
        })

//        val currLocation = LatLng(locationSubject.value.latitude, locationSubject.value.longitude)
//        val center = CameraUpdateFactory.newLatLngZoom(currLocation, viewModel.zoom.toFloat())
//        mMap.animateCamera(center, 10, null);
    }

    override fun onResume() {
        super.onResume()
//        addSubscriptions()
        try {
            mMapView.onResume()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onPause() {
        super.onPause()
//        removeSubscriptions()
        try {
            mMapView.onPause()
        }
        catch (e: UninitializedPropertyAccessException) {
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        removeSubscriptions()
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
}