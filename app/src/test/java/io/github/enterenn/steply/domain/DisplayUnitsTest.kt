package io.github.enterenn.steply.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayUnitsTest {
    @Test
    fun kilometersConvertToMiles() {
        assertEquals(0.0, kmToMi(0.0), 0.0)
        assertEquals(1.0, kmToMi(1.609344), 1e-9)
        assertEquals(3.106855961, kmToMi(5.0), 1e-9)
    }

    @Test
    fun kilogramsConvertToPoundsAndBack() {
        assertEquals(154, kgToLb(70))
        assertEquals(70, lbToKg(154))
        assertEquals(66, kgToLb(ProfileWeightMinKg))
        assertEquals(441, kgToLb(ProfileWeightMaxKg))
        assertEquals(ProfileWeightMinKg, lbToKg(66))
        assertEquals(ProfileWeightMaxKg, lbToKg(441))
    }

    @Test
    fun centimetersConvertToInchesAndBack() {
        assertEquals(67, cmToIn(170))
        assertEquals(170, inchesToCm(67))
        assertEquals(47, cmToIn(ProfileHeightMinCm))
        assertEquals(87, cmToIn(ProfileHeightMaxCm))
        assertEquals(ProfileHeightMinCm, inchesToCm(47))
        assertEquals(ProfileHeightMaxCm, inchesToCm(87))
    }
}
