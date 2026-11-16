package app.geeflow.app.screensaver

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreensaverTimeTest {
    @Test fun midnightAndNoonHaveTheCorrectPeriod() {
        assertEquals(ScreensaverTime("12:05", true), parseScreensaverTime("00:05"))
        assertEquals(ScreensaverTime("12:05", false), parseScreensaverTime("12:05"))
        assertEquals(ScreensaverTime("12:05", true), parseScreensaverTime("12:05 AM"))
        assertEquals(ScreensaverTime("12:05", false), parseScreensaverTime("12:05 PM"))
    }
    @Test fun koreanAndEnglishPeriodsAreRecognized() {
        assertEquals(ScreensaverTime("09:41", true), parseScreensaverTime("오전 9:41"))
        assertEquals(ScreensaverTime("09:41", false), parseScreensaverTime("오후 9:41"))
        assertEquals(ScreensaverTime("09:41", false), parseScreensaverTime("9.41 p.m."))
    }
    @Test fun twentyFourHourAndLocalizedDigitsUseTwelveHourDisplay() {
        assertEquals(ScreensaverTime("09:41", false), parseScreensaverTime("21:41"))
        assertEquals(ScreensaverTime("03:09", false), parseScreensaverTime("١٥:٠٩"))
    }
}
