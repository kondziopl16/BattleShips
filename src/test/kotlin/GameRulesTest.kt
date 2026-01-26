import core.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GameRulesTest {

    @Test
    fun `ship can be placed on empty board`() {
        val ship = Ship(3, Coordinate(0, 0), Direction.HORIZONTAL)
        val result = GameRules.canPlaceShip(ship, emptyList())
        assertTrue(result)
    }

    @Test
    fun `ship cannot be placed outside board horizontally`() {
        val ship = Ship(3, Coordinate(8, 0), Direction.HORIZONTAL)
        val result = GameRules.canPlaceShip(ship, emptyList())
        assertFalse(result)
    }

    @Test
    fun `ship cannot be placed outside board vertically`() {
        val ship = Ship(4, Coordinate(0, 8), Direction.VERTICAL)
        val result = GameRules.canPlaceShip(ship, emptyList())
        assertFalse(result)
    }

    @Test
    fun `ships cannot overlap`() {
        val ship1 = Ship(3, Coordinate(0, 0), Direction.HORIZONTAL)
        val ship2 = Ship(2, Coordinate(1, 0), Direction.HORIZONTAL)

        assertTrue(GameRules.canPlaceShip(ship1, emptyList()))
        assertFalse(GameRules.canPlaceShip(ship2, listOf(ship1)))
    }

    @Test
    fun `ships cannot touch orthogonally`() {
        val ship1 = Ship(3, Coordinate(0, 0), Direction.HORIZONTAL)
        val ship2 = Ship(2, Coordinate(0, 1), Direction.HORIZONTAL) // Adjacent below

        assertTrue(GameRules.canPlaceShip(ship1, emptyList()))
        assertFalse(GameRules.canPlaceShip(ship2, listOf(ship1)))
    }

    @Test
    fun `ships can touch diagonally`() {
        val ship1 = Ship(3, Coordinate(0, 0), Direction.HORIZONTAL)
        val ship2 = Ship(2, Coordinate(3, 1), Direction.HORIZONTAL) // Diagonal from end of ship1

        assertTrue(GameRules.canPlaceShip(ship1, emptyList()))
        assertTrue(GameRules.canPlaceShip(ship2, listOf(ship1)))
    }

    @Test
    fun `valid configuration passes validation`() {
        val ships = listOf(
            Ship(5, Coordinate(0, 0), Direction.HORIZONTAL),
            Ship(4, Coordinate(0, 2), Direction.HORIZONTAL),
            Ship(4, Coordinate(0, 4), Direction.HORIZONTAL),
            Ship(3, Coordinate(0, 6), Direction.HORIZONTAL),
            Ship(3, Coordinate(0, 8), Direction.HORIZONTAL),
            Ship(3, Coordinate(5, 0), Direction.VERTICAL),
            Ship(2, Coordinate(7, 0), Direction.VERTICAL),
            Ship(2, Coordinate(7, 3), Direction.VERTICAL),
            Ship(2, Coordinate(7, 6), Direction.VERTICAL),
            Ship(2, Coordinate(9, 0), Direction.VERTICAL)
        )

        assertTrue(GameRules.validateShipConfiguration(ships))
    }
}
