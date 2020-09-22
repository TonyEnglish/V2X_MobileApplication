package com.wzdctool.android.dataclasses

data class SecondFragmentUIObj (val num_lanes: Int, val laneStat: List<Boolean>,
                                val wpStat: Boolean, val dataLog: Boolean, val gotRP: Boolean,
                                val currLocation: Coordinate, val start_coord: Coordinate,
                                val end_coord: Coordinate, val speed_limits: SPEEDLIMITS,
                                val automatic_detection: Boolean)