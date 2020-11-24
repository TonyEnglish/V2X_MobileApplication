package com.wzdctool.android.dataclasses

import java.util.*

data class CSVObj (val time: Date, val num_sats: Int, val hdop: Float, val latitude: Double,
                   val longitude: Double, val altitude: Double, val speed: Float,
                   val heading: Float, var marker: String, var marker_value: String)