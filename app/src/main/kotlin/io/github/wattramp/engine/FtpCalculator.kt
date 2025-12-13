package io.github.wattramp.engine

import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.PreferencesRepository.FtpCalcMethod

/**
 * Utility class for FTP calculations and power zone determination.
 */
object FtpCalculator {

    /**
     * Power zone definitions (Coggan zones).
     */
    enum class PowerZone(val zoneName: String, val minPercent: Double, val maxPercent: Double) {
        Z1("Active Recovery", 0.0, 0.55),
        Z2("Endurance", 0.56, 0.75),
        Z3("Tempo", 0.76, 0.90),
        Z4("Threshold", 0.91, 1.05),
        Z5("VO2max", 1.06, 1.20),
        Z6("Anaerobic", 1.21, 1.50),
        Z7("Neuromuscular", 1.51, Double.MAX_VALUE);

        override fun toString(): String {
            return name
        }
    }

    /**
     * Calculate FTP from Ramp test max 1-minute power.
     */
    fun calculateFromRamp(maxOneMinutePower: Int, method: FtpCalcMethod): Int {
        val coefficient = when (method) {
            FtpCalcMethod.CONSERVATIVE -> 0.72
            FtpCalcMethod.STANDARD -> 0.75
            FtpCalcMethod.AGGRESSIVE -> 0.77
        }
        return (maxOneMinutePower * coefficient).toInt()
    }

    /**
     * Calculate FTP from 20-minute average power.
     */
    fun calculateFrom20Min(avgPower: Int, method: FtpCalcMethod): Int {
        val coefficient = when (method) {
            FtpCalcMethod.CONSERVATIVE -> 0.93
            FtpCalcMethod.STANDARD -> 0.95
            FtpCalcMethod.AGGRESSIVE -> 0.97
        }
        return (avgPower * coefficient).toInt()
    }

    /**
     * Calculate FTP from 8-minute test averages.
     */
    fun calculateFrom8Min(avg1: Int, avg2: Int, method: FtpCalcMethod): Int {
        val combinedAvg = (avg1 + avg2) / 2.0
        val coefficient = when (method) {
            FtpCalcMethod.CONSERVATIVE -> 0.88
            FtpCalcMethod.STANDARD -> 0.90
            FtpCalcMethod.AGGRESSIVE -> 0.92
        }
        return (combinedAvg * coefficient).toInt()
    }

    /**
     * Get the power zone for a given power output relative to FTP.
     */
    fun getPowerZone(currentPower: Int, ftp: Int): PowerZone {
        if (ftp <= 0) return PowerZone.Z1

        val percent = currentPower.toDouble() / ftp

        return PowerZone.entries.find {
            percent >= it.minPercent && percent < it.maxPercent
        } ?: PowerZone.Z7
    }

    /**
     * Check if current power is within tolerance of target.
     */
    fun isInTargetZone(currentPower: Int, targetPower: Int, tolerancePercent: Int): Boolean {
        if (targetPower <= 0) return true // No target = always in zone

        val tolerance = targetPower * tolerancePercent / 100.0
        return currentPower >= (targetPower - tolerance) &&
                currentPower <= (targetPower + tolerance)
    }

    /**
     * Get deviation from target in watts.
     */
    fun getDeviation(currentPower: Int, targetPower: Int): Int {
        return currentPower - targetPower
    }

    /**
     * Get deviation from target as percentage.
     */
    fun getDeviationPercent(currentPower: Int, targetPower: Int): Double {
        if (targetPower <= 0) return 0.0
        return ((currentPower - targetPower).toDouble() / targetPower) * 100
    }

    /**
     * Calculate target power for a given zone and FTP.
     */
    fun getTargetPowerForZone(zone: PowerZone, ftp: Int): IntRange {
        val min = (ftp * zone.minPercent).toInt()
        val max = if (zone.maxPercent < 100) (ftp * zone.maxPercent).toInt() else Int.MAX_VALUE
        return min..max
    }

    /**
     * Get formula string for display.
     */
    fun getFormulaString(
        protocol: ProtocolType,
        value: Int,
        method: FtpCalcMethod,
        result: Int
    ): String {
        val coefficient = when (protocol) {
            ProtocolType.RAMP -> when (method) {
                FtpCalcMethod.CONSERVATIVE -> 0.72
                FtpCalcMethod.STANDARD -> 0.75
                FtpCalcMethod.AGGRESSIVE -> 0.77
            }
            ProtocolType.TWENTY_MINUTE -> when (method) {
                FtpCalcMethod.CONSERVATIVE -> 0.93
                FtpCalcMethod.STANDARD -> 0.95
                FtpCalcMethod.AGGRESSIVE -> 0.97
            }
            ProtocolType.EIGHT_MINUTE -> when (method) {
                FtpCalcMethod.CONSERVATIVE -> 0.88
                FtpCalcMethod.STANDARD -> 0.90
                FtpCalcMethod.AGGRESSIVE -> 0.92
            }
        }
        return "$value x $coefficient = $result"
    }

    /**
     * Calculate rolling 1-minute average from power samples.
     * Assumes 1 sample per second.
     */
    fun calculateOneMinuteAverage(samples: List<Int>): Double {
        if (samples.size < 60) return samples.average()
        return samples.takeLast(60).average()
    }

    /**
     * Find max 1-minute power from a list of samples.
     * Assumes 1 sample per second.
     */
    fun findMaxOneMinutePower(samples: List<Int>): Double {
        if (samples.size < 60) return samples.average()

        var maxAvg = 0.0
        for (i in 0..(samples.size - 60)) {
            val windowAvg = samples.subList(i, i + 60).average()
            if (windowAvg > maxAvg) {
                maxAvg = windowAvg
            }
        }
        return maxAvg
    }
}
