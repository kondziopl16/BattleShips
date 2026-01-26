package ai.strategies

import core.*

/**
 * Kalkulator prawdopodobieństwa z obsługą HIT-aware.
 *
 * KLUCZOWA ZASADA: Jeśli są aktywne trafienia (HIT), to każdy pozostały statek
 * MUSI przechodzić przez co najmniej jedno z tych trafień.
 */
class ProbabilityCalculator(
    private val board: Array<Array<CellState>>,
    private val remainingShips: List<Int>,
    private val activeHits: List<Coordinate> = emptyList()
) {

    /**
     * Oblicza mapę prawdopodobieństwa.
     * Jeśli są aktywne trafienia - liczy tylko ustawienia przechodzące przez nie.
     */
    fun calculate(): Array<IntArray> {
        return if (activeHits.isEmpty()) {
            calculateStandard()
        } else {
            calculateHitAware()
        }
    }

    /**
     * Standardowe obliczanie gdy nie ma aktywnych trafień (Hunt Mode)
     */
    private fun calculateStandard(): Array<IntArray> {
        val probMap = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { 0 } }

        for (shipSize in remainingShips) {
            // Horizontal placements
            for (y in 0 until BOARD_SIZE) {
                for (x in 0..(BOARD_SIZE - shipSize)) {
                    if (canPlaceHypothetically(x, y, shipSize, Direction.HORIZONTAL)) {
                        for (i in 0 until shipSize) {
                            probMap[y][x + i]++
                        }
                    }
                }
            }

            // Vertical placements
            for (y in 0..(BOARD_SIZE - shipSize)) {
                for (x in 0 until BOARD_SIZE) {
                    if (canPlaceHypothetically(x, y, shipSize, Direction.VERTICAL)) {
                        for (i in 0 until shipSize) {
                            probMap[y + i][x]++
                        }
                    }
                }
            }
        }

        // Zero out non-shootable cells
        zeroOutNonShootable(probMap)
        return probMap
    }

    /**
     * HIT-aware obliczanie - statki MUSZĄ przechodzić przez trafienia.
     * To jest kluczowe ulepszenie!
     */
    private fun calculateHitAware(): Array<IntArray> {
        val probMap = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) { 0 } }

        for (shipSize in remainingShips) {
            // Horizontal placements - MUSZĄ pokrywać co najmniej jeden HIT
            for (y in 0 until BOARD_SIZE) {
                for (x in 0..(BOARD_SIZE - shipSize)) {
                    if (canPlaceHypothetically(x, y, shipSize, Direction.HORIZONTAL)) {
                        val shipCells = getShipCells(x, y, shipSize, Direction.HORIZONTAL)

                        // Sprawdź czy to ustawienie pokrywa jakiś aktywny HIT
                        val coversHit = shipCells.any { (cx, cy) ->
                            activeHits.any { it.x == cx && it.y == cy }
                        }

                        if (coversHit) {
                            // Tylko wtedy liczymy to ustawienie
                            for ((cx, cy) in shipCells) {
                                probMap[cy][cx]++
                            }
                        }
                    }
                }
            }

            // Vertical placements - MUSZĄ pokrywać co najmniej jeden HIT
            for (y in 0..(BOARD_SIZE - shipSize)) {
                for (x in 0 until BOARD_SIZE) {
                    if (canPlaceHypothetically(x, y, shipSize, Direction.VERTICAL)) {
                        val shipCells = getShipCells(x, y, shipSize, Direction.VERTICAL)

                        val coversHit = shipCells.any { (cx, cy) ->
                            activeHits.any { it.x == cx && it.y == cy }
                        }

                        if (coversHit) {
                            for ((cx, cy) in shipCells) {
                                probMap[cy][cx]++
                            }
                        }
                    }
                }
            }
        }

        // Zero out non-shootable cells
        zeroOutNonShootable(probMap)
        return probMap
    }

    /**
     * Wersja z boostem dla sąsiadów trafień (kompatybilność wsteczna)
     */
    fun calculateWithHitBoost(hits: List<Coordinate>): Array<IntArray> {
        // Użyj HIT-aware calculation
        val calculator = ProbabilityCalculator(board, remainingShips, hits)
        return calculator.calculate()
    }

    private fun canPlaceHypothetically(startX: Int, startY: Int, size: Int, direction: Direction): Boolean {
        val cells = getShipCells(startX, startY, size, direction)

        for ((x, y) in cells) {
            if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
                return false
            }

            val state = board[y][x]

            // Nie można umieścić na MISS, SUNK, lub BLOCKED
            if (state == CellState.MISS || state == CellState.SUNK || state == CellState.BLOCKED) {
                return false
            }

            // Sprawdź sąsiadów ortogonalnych dla SUNK (zasada no-touching)
            for (neighbor in Coordinate(x, y).orthogonalNeighbors()) {
                if (board[neighbor.y][neighbor.x] == CellState.SUNK) {
                    return false
                }
            }
        }

        return true
    }

    private fun getShipCells(startX: Int, startY: Int, size: Int, direction: Direction): List<Pair<Int, Int>> {
        return (0 until size).map { i ->
            when (direction) {
                Direction.HORIZONTAL -> Pair(startX + i, startY)
                Direction.VERTICAL -> Pair(startX, startY + i)
            }
        }
    }

    private fun zeroOutNonShootable(probMap: Array<IntArray>) {
        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                if (board[y][x] != CellState.UNKNOWN) {
                    probMap[y][x] = 0
                }
            }
        }
    }

    /**
     * Stosuje optymalizację wzoru 2×3 (parzystość rozszerzona)
     */
    fun applyPatternOptimization(probMap: Array<IntArray>): Array<IntArray> {
        val result = Array(BOARD_SIZE) { y -> IntArray(BOARD_SIZE) { x -> probMap[y][x] } }

        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                val isPatternCell = (x % 3) == (y % 3)
                if (!isPatternCell) {
                    result[y][x] = result[y][x] / 10
                }
            }
        }

        return result
    }
}
