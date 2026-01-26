import ai.SmartAIPlayer
import ai.RandomAIPlayer
import core.*
import engine.GameEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.random.Random

class SmartAIPlayerTest {

    @Test
    fun `AI generates valid ship placement`() {
        val ai = SmartAIPlayer(Random(42))
        val placements = ai.placeShips()

        // Check correct number of ships
        assertEquals(10, placements.size)

        // Check correct sizes
        val expectedSizes = SHIP_SIZES.sorted()
        val actualSizes = placements.map { it.size }.sorted()
        assertEquals(expectedSizes, actualSizes)

        // Check all placements are valid
        val ships = placements.map { it.toShip() }
        assertTrue(GameRules.validateShipConfiguration(ships))
    }

    @Test
    fun `AI chooses valid shots`() {
        val ai = SmartAIPlayer(Random(42))
        val shotsFired = mutableSetOf<Coordinate>()

        // Fire 50 shots and verify all are valid and unique
        repeat(50) {
            val shot = ai.getNextShot()

            assertTrue(shot.isValid(), "Shot $shot should be valid")
            assertFalse(shot in shotsFired, "Shot $shot should be unique")

            shotsFired.add(shot)
            ai.onShotResult(shot, ShotResult.Miss)
        }
    }

    @Test
    fun `AI targets cells near hits`() {
        val ai = SmartAIPlayer(Random(42))

        // Simulate a hit at (5,5)
        val firstShot = Coordinate(5, 5)
        ai.onShotResult(firstShot, ShotResult.Hit)

        // Next shot should be adjacent to (5,5)
        val nextShot = ai.getNextShot()
        val adjacentCells = listOf(
            Coordinate(4, 5), Coordinate(6, 5),
            Coordinate(5, 4), Coordinate(5, 6)
        )

        assertTrue(nextShot in adjacentCells,
            "After hit at (5,5), next shot $nextShot should be adjacent")
    }

    @Test
    fun `AI continues in same direction after multiple hits`() {
        val ai = SmartAIPlayer(Random(42))

        // Simulate hits at (5,5) and (5,6) - horizontal pattern
        ai.onShotResult(Coordinate(5, 5), ShotResult.Hit)
        ai.onShotResult(Coordinate(5, 6), ShotResult.Hit)

        val nextShot = ai.getNextShot()

        // Should extend horizontally: (5,4) or (5,7)
        val expectedCells = listOf(Coordinate(5, 4), Coordinate(5, 7))
        assertTrue(nextShot in expectedCells,
            "After horizontal hits, next shot $nextShot should extend the line")
    }
}

class IntegrationTest {

    @Test
    fun `complete game finishes with a winner`() {
        val player1 = SmartAIPlayer(Random(123))
        val player2 = SmartAIPlayer(Random(456))

        val engine = GameEngine(player1, player2, null)
        val result = engine.playGame()

        assertTrue(result.winner in listOf(1, 2), "Game should have a winner")
        assertTrue(result.player1Shots > 0, "Player 1 should have fired shots")
        assertTrue(result.player2Shots > 0, "Player 2 should have fired shots")
    }

    @Test
    fun `SmartAI beats RandomAI most of the time`() {
        var smartWins = 0
        val totalGames = 20

        repeat(totalGames) { i ->
            val smartAI = SmartAIPlayer(Random(i * 1000))
            val randomAI = RandomAIPlayer(Random(i * 2000))

            val engine = GameEngine(smartAI, randomAI, null)
            val result = engine.playGame()

            if (result.winner == 1) smartWins++
        }

        val winRate = smartWins.toDouble() / totalGames
        assertTrue(winRate > 0.7, "SmartAI should win >70% against RandomAI, actual: ${winRate * 100}%")
    }
}
