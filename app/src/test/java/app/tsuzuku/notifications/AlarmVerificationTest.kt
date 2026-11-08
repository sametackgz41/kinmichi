package app.tsuzuku.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AlarmVerificationTest {
    @Test
    fun twoOfEachAndNeverTheSameKindTwiceInARow() {
        repeat(200) { seed ->
            val order = verificationOrder(Random(seed))
            assertEquals(6, order.size)
            AlarmTask.Default.forEach { task -> assertEquals(2, order.count { it == task }) }
            assertTrue(order.zipWithNext().none { (a, b) -> a == b })
        }
    }

    @Test
    fun mathProblemsHaveTheirAnswer() {
        val random = Random(1)
        repeat(100) {
            val p = mathProblem(random)
            assertTrue(p.answer >= 0)
        }
    }
}
