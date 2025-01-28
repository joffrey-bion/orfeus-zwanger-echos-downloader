package org.hildan.orfeus.zwanger

import java.nio.file.*
import java.util.*
import kotlin.io.path.*

fun Path.readProperties() = bufferedReader().use {
    Properties().apply { load(it) }
}
