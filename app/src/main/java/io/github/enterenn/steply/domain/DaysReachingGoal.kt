package io.github.enterenn.steply.domain

fun daysReachingGoal(dailySteps: List<Long>, goal: Int): Int =
    dailySteps.count { it >= goal.toLong() }
