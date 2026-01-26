package core

class Board {
    private val ships = mutableListOf<Ship>()
    private val shotsFired = mutableSetOf<Coordinate>()

    fun placeShips(placements: List<ShipPlacement>): Boolean {
        val newShips = placements.map { it.toShip() }
        if (!GameRules.validateShipConfiguration(newShips)) {
            return false
        }
        ships.clear()
        ships.addAll(newShips)
        return true
    }

    fun getShips(): List<Ship> = ships.toList()

    fun receiveShot(coord: Coordinate): ShotResult {
        if (coord in shotsFired) {
            throw IllegalStateException("Already shot at $coord")
        }
        shotsFired.add(coord)

        for (ship in ships) {
            if (ship.isHit(coord)) {
                ship.recordHit(coord)
                return if (ship.isSunk()) {
                    ShotResult.Sunk(ship.size)
                } else {
                    ShotResult.Hit
                }
            }
        }

        return ShotResult.Miss
    }

    fun allShipsSunk(): Boolean = ships.all { it.isSunk() }

    fun getShotsFired(): Set<Coordinate> = shotsFired.toSet()
}

class TrackingBoard {
    val state: Array<Array<CellState>> = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellState.UNKNOWN } }
    private val activeHits = mutableListOf<Coordinate>()
    private val remainingShips = SHIP_SIZES.toMutableList()

    fun recordShot(coord: Coordinate, result: ShotResult) {
        when (result) {
            is ShotResult.Miss -> {
                state[coord.y][coord.x] = CellState.MISS
            }
            is ShotResult.Hit -> {
                state[coord.y][coord.x] = CellState.HIT
                activeHits.add(coord)
            }
            is ShotResult.Sunk -> {
                state[coord.y][coord.x] = CellState.HIT
                activeHits.add(coord)
                handleSunk(coord, result.shipSize)
            }
        }
    }

    private fun handleSunk(lastHit: Coordinate, shipSize: Int) {
        // Remove ship size from remaining
        remainingShips.remove(shipSize)

        // Find all cells that form this sunk ship
        val sunkCells = reconstructSunkShip(lastHit, shipSize)

        // Mark cells as SUNK and remove from active hits
        for (cell in sunkCells) {
            state[cell.y][cell.x] = CellState.SUNK
            activeHits.remove(cell)
        }

        // Mark orthogonal neighbors as BLOCKED
        for (cell in sunkCells) {
            for (neighbor in cell.orthogonalNeighbors()) {
                if (state[neighbor.y][neighbor.x] == CellState.UNKNOWN) {
                    state[neighbor.y][neighbor.x] = CellState.BLOCKED
                }
            }
        }
    }

    private fun reconstructSunkShip(lastHit: Coordinate, shipSize: Int): List<Coordinate> {
        // Find connected HIT cells that form a ship of the given size
        val candidates = mutableListOf<Coordinate>()
        val visited = mutableSetOf<Coordinate>()
        val queue = ArrayDeque<Coordinate>()

        queue.add(lastHit)
        visited.add(lastHit)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (state[current.y][current.x] == CellState.HIT) {
                candidates.add(current)
            }

            for (neighbor in current.orthogonalNeighbors()) {
                if (neighbor !in visited && state[neighbor.y][neighbor.x] == CellState.HIT) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        // If we found exactly the right number, return them
        if (candidates.size == shipSize) {
            return candidates
        }

        // Otherwise try to find a collinear subset of the right size
        // Check horizontal line
        val horizontalCells = candidates.filter { it.y == lastHit.y }.sortedBy { it.x }
        if (horizontalCells.size >= shipSize) {
            val consecutive = findConsecutiveSequence(horizontalCells.map { it.x }, shipSize)
            if (consecutive != null) {
                return consecutive.map { Coordinate(it, lastHit.y) }
            }
        }

        // Check vertical line
        val verticalCells = candidates.filter { it.x == lastHit.x }.sortedBy { it.y }
        if (verticalCells.size >= shipSize) {
            val consecutive = findConsecutiveSequence(verticalCells.map { it.y }, shipSize)
            if (consecutive != null) {
                return consecutive.map { Coordinate(lastHit.x, it) }
            }
        }

        // Fallback: return any candidates up to ship size
        return candidates.take(shipSize)
    }

    private fun findConsecutiveSequence(values: List<Int>, length: Int): List<Int>? {
        if (values.size < length) return null

        for (i in 0..values.size - length) {
            val window = values.subList(i, i + length)
            if (window.last() - window.first() == length - 1) {
                // Check all values are consecutive
                val isConsecutive = window.zipWithNext().all { (a, b) -> b - a == 1 }
                if (isConsecutive) return window
            }
        }
        return null
    }

    fun getActiveHits(): List<Coordinate> = activeHits.toList()

    fun getRemainingShips(): List<Int> = remainingShips.toList()

    /**
     * ULEPSZENIE: Wnioskowanie minimalnego rozmiaru statku
     * Jeśli mamy N kolejnych trafień bez zatopienia → statek ma co najmniej N+1 pól
     */
    fun inferMinimumShipSize(): Int {
        if (activeHits.isEmpty()) return 1

        // Znajdź najdłuższy ciąg trafień w linii
        val maxConsecutive = findLongestConsecutiveHits()

        // Jeśli mamy N trafień w linii, statek ma co najmniej N+1 pól
        // (bo gdyby miał N, byłby już zatopiony)
        return maxConsecutive + 1
    }

    /**
     * Znajduje najdłuższy ciąg kolejnych trafień (poziomo lub pionowo)
     */
    private fun findLongestConsecutiveHits(): Int {
        if (activeHits.isEmpty()) return 0
        if (activeHits.size == 1) return 1

        var maxLength = 1

        // Sprawdź grupy poziome
        val horizontalGroups = activeHits.groupBy { it.y }
        for ((_, hits) in horizontalGroups) {
            if (hits.size >= 2) {
                val sortedX = hits.map { it.x }.sorted()
                var currentLength = 1
                for (i in 1 until sortedX.size) {
                    if (sortedX[i] == sortedX[i - 1] + 1) {
                        currentLength++
                        maxLength = maxOf(maxLength, currentLength)
                    } else {
                        currentLength = 1
                    }
                }
            }
        }

        // Sprawdź grupy pionowe
        val verticalGroups = activeHits.groupBy { it.x }
        for ((_, hits) in verticalGroups) {
            if (hits.size >= 2) {
                val sortedY = hits.map { it.y }.sorted()
                var currentLength = 1
                for (i in 1 until sortedY.size) {
                    if (sortedY[i] == sortedY[i - 1] + 1) {
                        currentLength++
                        maxLength = maxOf(maxLength, currentLength)
                    } else {
                        currentLength = 1
                    }
                }
            }
        }

        return maxLength
    }

    /**
     * Zwraca tylko statki które mogą pasować do obecnych trafień
     * (rozmiar >= inferowany minimalny rozmiar)
     */
    fun getFilteredRemainingShips(): List<Int> {
        val minSize = inferMinimumShipSize()
        return remainingShips.filter { it >= minSize }
    }

    fun isUnknown(coord: Coordinate): Boolean {
        return coord.isValid() && state[coord.y][coord.x] == CellState.UNKNOWN
    }

    fun canShoot(coord: Coordinate): Boolean {
        return coord.isValid() && state[coord.y][coord.x] in listOf(CellState.UNKNOWN)
    }

    fun reset() {
        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                state[y][x] = CellState.UNKNOWN
            }
        }
        activeHits.clear()
        remainingShips.clear()
        remainingShips.addAll(SHIP_SIZES)
    }
}
