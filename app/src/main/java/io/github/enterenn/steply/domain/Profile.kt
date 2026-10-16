package io.github.enterenn.steply.domain

const val ProfileNameMaxLength = 40
const val ProfileWeightMinKg = 30
const val ProfileWeightMaxKg = 200
const val ProfileHeightMinCm = 120
const val ProfileHeightMaxCm = 220
const val ProfileMeasureStep = 1
const val ProfileMeasureMajorEvery = 10

data class Profile(
    val displayName: String? = null,
    val weightKg: Int? = null,
    val heightCm: Int? = null,
)

/**
 * Nom trimé, 40 caractères maximum. Vide après trim : effacé.
 * Poids 30..200 kg, taille 120..220 cm. Hors bornes : la valeur précédente reste.
 * Un argument omis ne change pas ce champ.
 */
fun resolveProfile(
    current: Profile = Profile(),
    proposedName: String? = current.displayName,
    proposedWeightKg: Int? = current.weightKg,
    proposedHeightCm: Int? = current.heightCm,
): Profile {
    fun name(previous: String?, proposed: String?): String? {
        if (proposed == null) return previous
        val trimmed = proposed.trim()
        if (trimmed.length > ProfileNameMaxLength) return previous
        if (trimmed.isEmpty()) return null
        return trimmed
    }

    fun measure(previous: Int?, proposed: Int?, min: Int, max: Int): Int? {
        if (proposed == null) return previous
        if (proposed !in min..max) return previous
        return proposed
    }

    return Profile(
        displayName = name(current.displayName, proposedName),
        weightKg = measure(current.weightKg, proposedWeightKg, ProfileWeightMinKg, ProfileWeightMaxKg),
        heightCm = measure(current.heightCm, proposedHeightCm, ProfileHeightMinCm, ProfileHeightMaxCm),
    )
}
