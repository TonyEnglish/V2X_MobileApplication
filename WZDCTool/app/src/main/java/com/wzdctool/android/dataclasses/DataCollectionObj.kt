package com.wzdctool.android.dataclasses

data class DataCollectionObj (val num_lanes: Int, val start_coord: Coordinate,
                         val end_coord: Coordinate, val speed_limits: SPEEDLIMITS,
                         val automatic_detection: Boolean, val data_lane: Int)