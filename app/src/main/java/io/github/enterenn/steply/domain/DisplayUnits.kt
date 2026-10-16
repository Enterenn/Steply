package io.github.enterenn.steply.domain

import kotlin.math.roundToInt

private const val KilometersPerMile = 1.609344
private const val PoundsPerKilogram = 2.2046226218
private const val CentimetersPerInch = 2.54

fun kmToMi(kilometers: Double): Double = kilometers / KilometersPerMile

fun kgToLb(kg: Int): Int = (kg * PoundsPerKilogram).roundToInt()
    .coerceIn(kgToLbRaw(ProfileWeightMinKg), kgToLbRaw(ProfileWeightMaxKg))

fun lbToKg(lb: Int): Int = (lb / PoundsPerKilogram).roundToInt()
    .coerceIn(ProfileWeightMinKg, ProfileWeightMaxKg)

fun cmToIn(cm: Int): Int = (cm / CentimetersPerInch).roundToInt()
    .coerceIn(cmToInRaw(ProfileHeightMinCm), cmToInRaw(ProfileHeightMaxCm))

fun inchesToCm(inches: Int): Int = (inches * CentimetersPerInch).roundToInt()
    .coerceIn(ProfileHeightMinCm, ProfileHeightMaxCm)

private fun kgToLbRaw(kg: Int): Int = (kg * PoundsPerKilogram).roundToInt()

private fun cmToInRaw(cm: Int): Int = (cm / CentimetersPerInch).roundToInt()
