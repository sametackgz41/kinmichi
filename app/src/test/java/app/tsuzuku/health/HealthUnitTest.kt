package app.tsuzuku.health

import app.tsuzuku.model.HealthMetric
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthUnitTest {
    @Test
    fun distanceIsKilometresUnlessTheHabitCountsMetres() {
        assertEquals(5.2, healthValueInUnit(HealthMetric.Distance, 5200.0, "km"), 0.001)
        assertEquals(5200.0, healthValueInUnit(HealthMetric.Distance, 5200.0, "m"), 0.001)
        assertEquals(5.2, healthValueInUnit(HealthMetric.Distance, 5200.0, ""), 0.001)
    }

    @Test
    fun durationsAreMinutesUnlessTheHabitCountsHours() {
        assertEquals(450.0, healthValueInUnit(HealthMetric.SleepDuration, 450.0, "dk"), 0.001)
        assertEquals(7.5, healthValueInUnit(HealthMetric.SleepDuration, 450.0, "saat"), 0.001)
        assertEquals(0.5, healthValueInUnit(HealthMetric.ExerciseDuration, 30.0, "h"), 0.001)
    }

    @Test
    fun stepsAndCaloriesPassThrough() {
        assertEquals(8000.0, healthValueInUnit(HealthMetric.Steps, 8000.0, "adım"), 0.001)
        assertEquals(420.0, healthValueInUnit(HealthMetric.Calories, 420.0, "kcal"), 0.001)
    }
}
