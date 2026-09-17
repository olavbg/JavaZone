package com.olavbg.javazone.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OemDetectionTest {

    @Test
    fun detectsKnownAggressiveOems() {
        assertTrue(isAggressiveOem("Xiaomi", "xiaomi"))
        assertTrue(isAggressiveOem("samsung", "samsung"))
        assertTrue(isAggressiveOem("HUAWEI", "HONOR"))
        assertTrue(isAggressiveOem("OPPO", "realme"))
        assertTrue(isAggressiveOem("OnePlus", "OnePlus"))
        assertTrue(isAggressiveOem("vivo", "vivo"))
        assertTrue(isAggressiveOem("HMD Global", "Nokia"))
    }

    @Test
    fun ignoresManufacturersWithoutAggressiveRestrictions() {
        assertFalse(isAggressiveOem("Google", "google"))
        assertFalse(isAggressiveOem("Sony", "Sony"))
        assertFalse(isAggressiveOem("motorola", "motorola"))
        assertFalse(isAggressiveOem("", ""))
    }
}
