package io.github.wattramp.engine

import kotlin.math.pow

/**
 * Calculator for extended cycling analytics.
 *
 * Metrics:
 * - Normalized Power (NP): 30-sec rolling average raised to 4th power
 * - Variability Index (VI): NP / Average Power (>1.05 indicates variable effort)
 * - Efficiency Factor (EF): NP / Average HR (aerobic efficiency metric)
 */
object AnalyticsCalculator {

    private const val NP_WINDOW_SIZE = 30 // 30-second rolling average

    /**
     * Calculate Normalized Power from power samples.
     *
     * Algorithm:
     * 1. Calculate 30-second rolling average of power
     * 2. Raise each rolling average to the 4th power
     * 3. Take the average of all 4th power values
     * 4. Take the 4th root of that average
     *
     * @param samples Power samples at 1Hz (one per second)
     * @return Normalized Power in watts, or null if insufficient data
     */
    fun calculateNormalizedPower(samples: List<Int>): Int? {
        if (samples.size < NP_WINDOW_SIZE) {
            return null
        }

        // Calculate 30-second rolling averages
        val rollingAverages = mutableListOf<Double>()
        for (i in (NP_WINDOW_SIZE - 1) until samples.size) {
            val windowStart = i - NP_WINDOW_SIZE + 1
            val windowSum = samples.subList(windowStart, i + 1).sum()
            val windowAvg = windowSum.toDouble() / NP_WINDOW_SIZE
            rollingAverages.add(windowAvg)
        }

        if (rollingAverages.isEmpty()) {
            return null
        }

        // Raise to 4th power, average, then take 4th root
        val fourthPowerSum = rollingAverages.sumOf { it.pow(4) }
        val fourthPowerAvg = fourthPowerSum / rollingAverages.size
        val normalizedPower = fourthPowerAvg.pow(0.25)

        return normalizedPower.toInt()
    }

    /**
     * Calculate Variability Index.
     *
     * VI = NP / Average Power
     *
     * Interpretation:
     * - 1.00-1.02: Very steady effort (time trial)
     * - 1.02-1.05: Steady effort
     * - 1.05-1.10: Variable effort
     * - >1.10: Highly variable (criterium, hilly ride)
     *
     * @param normalizedPower NP in watts
     * @param averagePower Average power in watts
     * @return Variability Index (typically 1.0-1.2), or null if invalid
     */
    fun calculateVariabilityIndex(normalizedPower: Int, averagePower: Int): Double? {
        if (averagePower <= 0) {
            return null
        }
        return normalizedPower.toDouble() / averagePower
    }

    /**
     * Calculate Efficiency Factor.
     *
     * EF = NP / Average HR
     *
     * Higher values indicate better aerobic efficiency.
     * Tracking EF over time shows fitness improvements.
     *
     * @param normalizedPower NP in watts
     * @param averageHeartRate Average HR in bpm
     * @return Efficiency Factor, or null if invalid
     */
    fun calculateEfficiencyFactor(normalizedPower: Int, averageHeartRate: Int): Double? {
        if (averageHeartRate <= 0) {
            return null
        }
        return normalizedPower.toDouble() / averageHeartRate
    }

    /**
     * Calculate average from samples, excluding zeros.
     *
     * @param samples List of values
     * @return Average of non-zero values, or null if all zeros
     */
    fun calculateAverage(samples: List<Int>): Int? {
        val nonZero = samples.filter { it > 0 }
        if (nonZero.isEmpty()) {
            return null
        }
        return (nonZero.sum().toDouble() / nonZero.size).toInt()
    }

    /**
     * Container for all analytics results.
     */
    data class AnalyticsResult(
        val normalizedPower: Int?,
        val variabilityIndex: Double?,
        val averageHeartRate: Int?,
        val efficiencyFactor: Double?
    )

    /**
     * Calculate all analytics from power and HR samples.
     *
     * @param powerSamples Power samples at 1Hz
     * @param hrSamples Heart rate samples at 1Hz
     * @return Complete analytics result
     */
    fun calculateAll(powerSamples: List<Int>, hrSamples: List<Int>): AnalyticsResult {
        val np = calculateNormalizedPower(powerSamples)
        val avgPower = calculateAverage(powerSamples)
        val avgHr = calculateAverage(hrSamples)

        val vi = if (np != null && avgPower != null) {
            calculateVariabilityIndex(np, avgPower)
        } else null

        val ef = if (np != null && avgHr != null) {
            calculateEfficiencyFactor(np, avgHr)
        } else null

        return AnalyticsResult(
            normalizedPower = np,
            variabilityIndex = vi,
            averageHeartRate = avgHr,
            efficiencyFactor = ef
        )
    }
}
