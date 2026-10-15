package io.github.enterenn.steply.data.steps

fun daysReachingGoal(dailySteps: List<Long>, goal: Int): Int =
    dailySteps.count { it >= goal.toLong() }
