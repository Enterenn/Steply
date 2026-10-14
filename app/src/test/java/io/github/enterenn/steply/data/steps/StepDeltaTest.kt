package io.github.enterenn.steply.data.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepDeltaTest {
    @Test
    fun noPreviousReading() {
        assertNull(stepDelta(previousCumulative = null, currentCumulative = 10))
    }

    @Test
    fun subtractsWhenCounterMovesForward() {
        val delta = stepDelta(previousCumulative = 100, currentCumulative = 140)
        assertEquals(40L, delta!!.steps)
        assertFalse(delta.counterReset)
    }

    @Test
    fun zeroWhenUnchanged() {
        val delta = stepDelta(previousCumulative = 100, currentCumulative = 100)
        assertEquals(0L, delta!!.steps)
        assertFalse(delta.counterReset)
    }

    @Test
    fun treatsDropAsReset() {
        val delta = stepDelta(previousCumulative = 5_000, currentCumulative = 12)
        assertEquals(12L, delta!!.steps)
        assertTrue(delta.counterReset)
    }
}
