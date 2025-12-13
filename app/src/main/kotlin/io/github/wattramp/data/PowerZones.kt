package io.github.wattramp.data

import androidx.compose.ui.graphics.Color

/**
 * Power zone model based on Coggan's classic 7-zone system.
 */
data class PowerZone(
    val number: Int,
    val name: String,
    val shortName: String,
    val lowerPercent: Double,
    val upperPercent: Double,
    val color: Color,
    val description: String
) {
    fun getLowerWatts(ftp: Int): Int = (ftp * lowerPercent / 100).toInt()
    fun getUpperWatts(ftp: Int): Int = if (upperPercent >= 150) 9999 else (ftp * upperPercent / 100).toInt()

    fun getRange(ftp: Int): String {
        val lower = getLowerWatts(ftp)
        val upper = getUpperWatts(ftp)
        return if (upper >= 9999) "${lower}W+" else "${lower}-${upper}W"
    }

    companion object {
        /**
         * Classic Coggan 7-zone power model.
         * Colors are passed as parameters to support theme switching.
         */
        fun getZones(
            zone1: Color,
            zone2: Color,
            zone3: Color,
            zone4: Color,
            zone5: Color,
            zone6: Color,
            zone7: Color
        ): List<PowerZone> = listOf(
            PowerZone(
                number = 1,
                name = "Active Recovery",
                shortName = "Recovery",
                lowerPercent = 0.0,
                upperPercent = 55.0,
                color = zone1,
                description = "Easy spinning, recovery rides"
            ),
            PowerZone(
                number = 2,
                name = "Endurance",
                shortName = "Endurance",
                lowerPercent = 56.0,
                upperPercent = 75.0,
                color = zone2,
                description = "Long rides, base building"
            ),
            PowerZone(
                number = 3,
                name = "Tempo",
                shortName = "Tempo",
                lowerPercent = 76.0,
                upperPercent = 90.0,
                color = zone3,
                description = "Brisk group rides, pace work"
            ),
            PowerZone(
                number = 4,
                name = "Lactate Threshold",
                shortName = "Threshold",
                lowerPercent = 91.0,
                upperPercent = 105.0,
                color = zone4,
                description = "Time trial efforts, FTP work"
            ),
            PowerZone(
                number = 5,
                name = "VO2max",
                shortName = "VO2max",
                lowerPercent = 106.0,
                upperPercent = 120.0,
                color = zone5,
                description = "Hard intervals, 3-8 min efforts"
            ),
            PowerZone(
                number = 6,
                name = "Anaerobic Capacity",
                shortName = "Anaerobic",
                lowerPercent = 121.0,
                upperPercent = 150.0,
                color = zone6,
                description = "Short hard efforts, 30s-2min"
            ),
            PowerZone(
                number = 7,
                name = "Neuromuscular Power",
                shortName = "Sprint",
                lowerPercent = 151.0,
                upperPercent = 999.0,
                color = zone7,
                description = "Sprints, max efforts <30s"
            )
        )

        /**
         * Get zone for a given power and FTP.
         * Colors are passed as parameters to support theme switching.
         */
        fun getZoneForPower(
            power: Int,
            ftp: Int,
            zone1: Color,
            zone2: Color,
            zone3: Color,
            zone4: Color,
            zone5: Color,
            zone6: Color,
            zone7: Color
        ): PowerZone? {
            if (ftp <= 0) return null
            val percentOfFtp = (power.toDouble() / ftp) * 100
            return getZones(zone1, zone2, zone3, zone4, zone5, zone6, zone7)
                .find { percentOfFtp >= it.lowerPercent && percentOfFtp <= it.upperPercent }
        }
    }
}
