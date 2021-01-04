package com.wzdctool.android.dataclasses

class locationSources() {
    var internal = gps_status.disconnected
    var usb = gps_status.disconnected
}

data class gpsDevice(val type: gps_type, var status: gps_status) {
}

enum class gps_type {
    internal,
    usb,
    none
}

enum class gps_status {
    valid,
    invalid,
    disconnected,
}