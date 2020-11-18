package com.wzdctool.android.dataclasses

import java.util.*

data class IDSet (val primary: UUID, val secondary: UUID)

fun reverseIDs(ids: IDSet): IDSet {
    return IDSet(ids.secondary, ids.primary)
}