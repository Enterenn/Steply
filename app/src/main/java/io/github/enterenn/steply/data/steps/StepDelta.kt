package io.github.enterenn.steply.data.steps

data class StepDelta(val steps: Long, val counterReset: Boolean)

/**
 * Écart entre deux relevés du compteur cumulatif.
 * Une baisse signifie que le capteur est reparti de zéro (redémarrage du téléphone) :
 * les pas manquants entre les deux relevés ne sont pas récupérables.
 */
fun stepDelta(previousCumulative: Long?, currentCumulative: Long): StepDelta? {
    previousCumulative ?: return null
    return if (currentCumulative >= previousCumulative) {
        StepDelta(steps = currentCumulative - previousCumulative, counterReset = false)
    } else {
        StepDelta(steps = currentCumulative, counterReset = true)
    }
}
