package core

data class Coordinate(val x: Int, val y: Int) {
    fun isValid(): Boolean = x in 0..9 && y in 0..9

    fun orthogonalNeighbors(): List<Coordinate> = listOf(
        Coordinate(x - 1, y),
        Coordinate(x + 1, y),
        Coordinate(x, y - 1),
        Coordinate(x, y + 1)
    ).filter { it.isValid() }

    fun allNeighbors(): List<Coordinate> = listOf(
        Coordinate(x - 1, y - 1), Coordinate(x, y - 1), Coordinate(x + 1, y - 1),
        Coordinate(x - 1, y),                           Coordinate(x + 1, y),
        Coordinate(x - 1, y + 1), Coordinate(x, y + 1), Coordinate(x + 1, y + 1)
    ).filter { it.isValid() }

    override fun toString(): String = "($x,$y)"
}

enum class Direction {
    HORIZONTAL,
    VERTICAL;

    fun toLogString(): String = when (this) {
        HORIZONTAL -> "horizontal"
        VERTICAL -> "vertical"
    }
}

enum class CellState {
    UNKNOWN,    // Not yet shot
    HIT,        // Shot and hit a ship
    MISS,       // Shot and missed
    SUNK,       // Part of a sunk ship
    BLOCKED     // Known to be empty (adjacent to sunk ship)
}

sealed class ShotResult {
    data object Miss : ShotResult()
    data object Hit : ShotResult()
    data class Sunk(val shipSize: Int) : ShotResult()

    fun toLogString(): String = when (this) {
        is Miss -> "result=miss"
        is Hit -> "result=hit"
        is Sunk -> "result=sunk ship-size=$shipSize"
    }
}

const val BOARD_SIZE = 10

val SHIP_SIZES = listOf(5, 4, 4, 3, 3, 3, 2, 2, 2, 2)
