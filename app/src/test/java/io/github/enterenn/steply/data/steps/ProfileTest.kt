package io.github.enterenn.steply.data.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {
    @Test
    fun unsetProfileStaysUnset() {
        assertEquals(Profile(), resolveProfile())
    }

    @Test
    fun blankNameClearsPreviousAndKeepsMeasures() {
        val current = Profile(displayName = "Ada", weightKg = 80, heightCm = 170)
        assertEquals(
            Profile(weightKg = 80, heightCm = 170),
            resolveProfile(current, proposedName = " \n "),
        )
    }

    @Test
    fun nameIsTrimmedUpToFortyCharacters() {
        val name = "a".repeat(ProfileNameMaxLength)
        assertEquals(name, resolveProfile(proposedName = "  $name  ").displayName)
    }

    @Test
    fun nameOverFortyKeepsPrevious() {
        val current = Profile(displayName = "Ada", weightKg = 80)
        val next = resolveProfile(current, proposedName = " ${"a".repeat(ProfileNameMaxLength + 1)} ")
        assertEquals(current, next)
        assertEquals(null, resolveProfile(proposedName = "b".repeat(ProfileNameMaxLength + 1)).displayName)
    }

    @Test
    fun weightBoundsKeepPrevious() {
        assertEquals(ProfileWeightMinKg, resolveProfile(proposedWeightKg = ProfileWeightMinKg).weightKg)
        assertEquals(ProfileWeightMaxKg, resolveProfile(proposedWeightKg = ProfileWeightMaxKg).weightKg)
        val current = Profile(displayName = "Ada", weightKg = 80, heightCm = 170)
        assertEquals(current, resolveProfile(current, proposedWeightKg = ProfileWeightMinKg - 1))
        assertEquals(current, resolveProfile(current, proposedWeightKg = ProfileWeightMaxKg + 1))
        assertEquals(null, resolveProfile(proposedWeightKg = ProfileWeightMinKg - 1).weightKg)
        assertEquals(81, resolveProfile(current, proposedWeightKg = 81).weightKg)
    }

    @Test
    fun heightBoundsKeepPrevious() {
        assertEquals(ProfileHeightMinCm, resolveProfile(proposedHeightCm = ProfileHeightMinCm).heightCm)
        assertEquals(ProfileHeightMaxCm, resolveProfile(proposedHeightCm = ProfileHeightMaxCm).heightCm)
        val current = Profile(displayName = "Ada", weightKg = 80, heightCm = 170)
        assertEquals(current, resolveProfile(current, proposedHeightCm = ProfileHeightMinCm - 1))
        assertEquals(current, resolveProfile(current, proposedHeightCm = ProfileHeightMaxCm + 1))
        assertEquals(null, resolveProfile(proposedHeightCm = ProfileHeightMinCm - 1).heightCm)
        assertEquals(171, resolveProfile(current, proposedHeightCm = 171).heightCm)
    }
}
