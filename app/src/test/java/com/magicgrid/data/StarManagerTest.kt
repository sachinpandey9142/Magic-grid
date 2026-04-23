package com.magicgrid.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StarManagerTest {
    private val starManager = StarManager()

    @Test
    fun targetTime_usesExpectedBands() {
        assertEquals(120, starManager.getTargetTime(1))
        assertEquals(120, starManager.getTargetTime(30))
        assertEquals(90, starManager.getTargetTime(31))
        assertEquals(90, starManager.getTargetTime(50))
        assertEquals(60, starManager.getTargetTime(51))
        assertEquals(60, starManager.getTargetTime(70))
        assertEquals(45, starManager.getTargetTime(71))
        assertEquals(45, starManager.getTargetTime(85))
        assertEquals(30, starManager.getTargetTime(86))
        assertEquals(30, starManager.getTargetTime(95))
        assertEquals(15, starManager.getTargetTime(96))
        assertEquals(15, starManager.getTargetTime(100))
    }

    @Test
    fun calculateStars_awardsThreeWhenOnTimeAndNoHints() {
        val stars = starManager.calculateStars(
            levelId = 12,
            timeSeconds = 119,
            hintsUsed = 0,
            targetTimeSecond = 120
        )

        assertEquals(3, stars)
    }

    @Test
    fun calculateStars_awardsTwoWhenOnTimeWithHints() {
        val stars = starManager.calculateStars(
            levelId = 12,
            timeSeconds = 110,
            hintsUsed = 2,
            targetTimeSecond = 120
        )

        assertEquals(2, stars)
    }

    @Test
    fun calculateStars_awardsOneWhenOverTime() {
        val stars = starManager.calculateStars(
            levelId = 12,
            timeSeconds = 121,
            hintsUsed = 0,
            targetTimeSecond = 120
        )

        assertEquals(1, stars)
    }

    @Test
    fun formatTime_outputsMmSs() {
        assertEquals("00:00", starManager.formatTime(0))
        assertEquals("00:09", starManager.formatTime(9))
        assertEquals("01:05", starManager.formatTime(65))
        assertEquals("12:34", starManager.formatTime(754))
    }
}
