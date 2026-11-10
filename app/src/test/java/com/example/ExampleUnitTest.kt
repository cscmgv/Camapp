package com.example

import com.example.util.GeoFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun coordinateFormattingAndPlusCode_isAccurate() {
        val lat = 13.0397
        val lng = 80.2794
        val dms = GeoFormatUtils.formatDMS(lat, lng)
        assertTrue(dms.contains("13°02'"))
        assertTrue(dms.contains("80°16'"))

        val plusCode = GeoFormatUtils.toPlusCode(lat, lng)
        assertEquals(11, plusCode.length)
        assertTrue(plusCode.contains("+"))

        val utm = GeoFormatUtils.toUtmString(lat, lng)
        assertTrue(utm.startsWith("44P"))
    }
}
