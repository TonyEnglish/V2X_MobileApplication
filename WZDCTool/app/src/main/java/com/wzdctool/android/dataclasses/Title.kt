package com.wzdctool.android.dataclasses

import java.util.*

data class Title (val type: String, val value: String, val IDs: IDSet)

fun getTitleString(title: Title): String {
    return "${title.type},${title.value},${title.IDs.primary},${title.IDs.secondary}"
}

fun parseTitleString(string: String): Title {
    val fields = string.split(',')
    return Title(fields[0], fields[1], IDSet(UUID.fromString(fields[2]), UUID.fromString(fields[3])))
}