package com.wzdctool.android

import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.wzdctool.android.dataclasses.MarkerObj
import com.wzdctool.android.repos.DataClassesRepository
import com.wzdctool.android.repos.DataFileRepository

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

        view.findViewById<Button>(R.id.savebutton).setOnClickListener{
            viewModel.saveEdits()
        }

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
        mMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
        mMap!!.isMyLocationEnabled = true
        viewModel.initMap(mMap!!, mMapView)

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