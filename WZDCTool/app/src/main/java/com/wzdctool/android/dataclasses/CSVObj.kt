package com.wzdctool.android.dataclasses

import java.util.*

data class CSVObj (val time: Date, val num_sats: Int, val hdop: Float, val latitude: Double,
                   val longitude: Double, val altitude: Double, val speed: Float,
                   val heading: Float, val marker: String, val marker_value: String,
                   val is_last: Boolean)