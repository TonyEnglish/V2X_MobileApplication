package com.wzdctool.android.handlers

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.wzdctool.android.EditingFragmentViewModel
import com.wzdctool.android.R

class CustomInfoWindowHandler(context: Context) : GoogleMap.InfoWindowAdapter {
        var mContext = context
        var mWindow = (context as Activity).layoutInflater.inflate(R.layout.marker_info_window, null)

        private fun rendorWindowText(marker: Marker, view: View){

            val tvTitle = view.findViewById<TextView>(R.id.title)
//            val tvSnippet = view.findViewById<TextView>(R.id.snippet)

            tvTitle.text = marker.title
//            tvSnippet.text = marker.snippet

        }

        override fun getInfoContents(marker: Marker): View {
            rendorWindowText(marker, mWindow)
            return mWindow
        }

        override fun getInfoWindow(marker: Marker): View? {
            rendorWindowText(marker, mWindow)
            return mWindow
        }
}