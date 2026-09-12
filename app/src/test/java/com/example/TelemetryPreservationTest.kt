package com.example

import com.example.data.model.MetricValue
import org.junit.Assert.*
import org.junit.Test

class TelemetryPreservationTest {

    @Test
    fun `zero is preserved as a real zero metric`() {
        val zeroCpu = MetricValue(value = 0.0, unit = "%")
        assertTrue(zeroCpu.isAvailable)
        assertEquals(0.0, zeroCpu.value!!, 0.001)
        assertEquals("0.0%", zeroCpu.displayString)
    }

    @Test
    fun `null is preserved as unavailable and not converted to zero`() {
        val unavailableMemory = MetricValue(value = null, unit = "%")
        assertFalse(unavailableMemory.isAvailable)
        assertNull(unavailableMemory.value)
        assertEquals("—", unavailableMemory.displayString)
    }

    @Test
    fun `positive metric values format accurately`() {
        val activeCpu = MetricValue(value = 42.7, unit = "%")
        assertTrue(activeCpu.isAvailable)
        assertEquals("42.7%", activeCpu.displayString)
    }
}
