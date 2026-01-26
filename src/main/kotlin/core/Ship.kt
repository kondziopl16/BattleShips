package core

data class Ship(
    val size: Int,
    val position: Coordinate,
    val direction: Direction
) {
    val cells: List<Coordinate> by lazy {
        (0 until size).map { i ->
            when (direction) {
                Direction.HORIZONTAL -> Coordinate(position.x + i, position.y)
                Direction.VERTICAL -> Coordinate(position.x, position.y + i)
            }
        }
    }

    private val hitCells = mutableSetOf<Coordinate>()

    fun isHit(coord: Coordinate): Boolean = coord in cells

    fun recordHit(coord: Coordinate): Boolean {
        if (coord in cells) {
            hitCells.add(coord)
            return true
        }
        return false
    }

    fun isSunk(): Boolean = hitCells.size == size

    fun getOrthogonalNeighbors(): Set<Coordinate> {
        val neighbors = mutableSetOf<Coordinate>()
        for (cell in cells) {
            neighbors.addAll(cell.orthogonalNeighbors().filter { it !in cells })
        }
        return neighbors
    }

    fun getAllNeighbors(): Set<Coordinate> {
        val neighbors = mutableSetOf<Coordinate>()
        for (cell in cells) {
            neighbors.addAll(cell.allNeighbors().filter { it !in cells })
        }
        return neighbors
    }

    fun isValid(): Boolean {
        return cells.all { it.isValid() }
    }
}

data class ShipPlacement(
    val size: Int,
    val position: Coordinate,
    val direction: Direction
) {
    fun toShip(): Ship = Ship(size, position, direction)
}
