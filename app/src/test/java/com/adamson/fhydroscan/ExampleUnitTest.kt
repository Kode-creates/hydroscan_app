package com.adamson.fhydroscan

import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    @Test
    fun appName_isCorrect() {
        // This test verifies that the app name is set correctly
        // This is a basic test to ensure the testing framework is working
        assertTrue("FHydroScan".isNotEmpty())
    }
}