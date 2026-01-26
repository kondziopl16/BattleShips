package core

object GameRules {

    fun canPlaceShip(
        ship: Ship,
        existingShips: List<Ship>
    ): Boolean {
        // Check if all cells are within bounds
        if (!ship.isValid()) return false

        // Check if ship overlaps with existing ships
        val occupiedCells = existingShips.flatMap { it.cells }.toSet()
        if (ship.cells.any { it in occupiedCells }) return false

        // Check no-touching rule: ships cannot touch orthogonally
        val forbiddenCells = existingShips.flatMap { it.getOrthogonalNeighbors() }.toSet()
        if (ship.cells.any { it in forbiddenCells }) return false

        return true
    }

    fun canPlaceShipOnBoard(
        ship: Ship,
        board: Array<Array<CellState>>
    ): Boolean {
        // Check bounds
        if (!ship.isValid()) return false

        // Check each cell
        for (cell in ship.cells) {
            val state = board[cell.y][cell.x]
            // Cannot place on MISS, SUNK, or BLOCKED cells
            if (state == CellState.MISS || state == CellState.SUNK || state == CellState.BLOCKED) {
                return false
            }

            // Check orthogonal neighbors for SUNK cells (no-touching rule)
            for (neighbor in cell.orthogonalNeighbors()) {
                if (board[neighbor.y][neighbor.x] == CellState.SUNK) {
                    return false
                }
            }
        }

        return true
    }

    fun validateShipConfiguration(ships: List<Ship>): Boolean {
        // Check correct number and sizes of ships
        val expectedSizes = SHIP_SIZES.sorted()
        val actualSizes = ships.map { it.size }.sorted()
        if (expectedSizes != actualSizes) return false

        // Check each ship is valid and doesn't violate rules
        for ((index, ship) in ships.withIndex()) {
            val otherShips = ships.take(index)
            if (!canPlaceShip(ship, otherShips)) return false
        }

        return true
    }
}
