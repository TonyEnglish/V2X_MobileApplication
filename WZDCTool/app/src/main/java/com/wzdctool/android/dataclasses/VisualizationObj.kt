package com.wzdctool.android.dataclasses

import com.google.android.gms.maps.model.LatLng

data class VisualizationObj (val dataPoints: List<LatLng>, val markers: List<CustomMarkerObj>, val dataFile: String)