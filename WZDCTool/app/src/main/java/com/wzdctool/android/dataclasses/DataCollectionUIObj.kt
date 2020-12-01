package com.wzdctool.android.dataclasses

data class DataCollectionUIObj (val num_lanes: Int, val laneStat: List<Boolean>,
                                val wpStat: Boolean, val dataLog: Boolean, val gotRP: Boolean,
                                val currLocation: Coordinate, val start_coord: Coordinate,
                                val end_coord: Coordinate, val speed_limits: SPEEDLIMITS,
                                val automatic_detection: Boolean, val data_lane: Int)