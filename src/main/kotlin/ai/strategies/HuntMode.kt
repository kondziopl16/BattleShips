package ai.strategies

import core.*

/**
 * Hunt Mode - szukanie statków (strzelanie w ciemno)
 *
 * Strategie:
 * 1. ADAPTACYJNY WZÓR:
 *    - Duże statki (≥3): wzór 2×3 (x % 3 == y % 3) - pokrywa ~33%
 *    - Małe statki (≤2): szachownica ((x + y) % 2 == 0) - pokrywa 50%
 * 2. Heatmapa - oblicz prawdopodobieństwo dla każdego pola
 * 3. Dynamiczne zawężanie - ignoruj obszary gdzie statki nie zmieszczą się
 */
class HuntMode(
    private val board: Array<Array<CellState>>,
    private val remainingShips: List<Int>
) {
    // Określ który wzór użyć na podstawie pozostałych statków
    private val patternMode = determinePatternMode()

    private fun determinePatternMode(): PatternMode {
        val largestShip = remainingShips.maxOrNull() ?: 2
        return if (largestShip >= 3) {
            PatternMode.PATTERN_2X3  // Dla dużych statków
        } else {
            PatternMode.CHECKERBOARD // Dla małych statków (tylko 2-polowe)
        }
    }

    private enum class PatternMode {
        PATTERN_2X3,    // x % 3 == y % 3, pokrywa ~33%
        CHECKERBOARD    // (x + y) % 2 == 0, pokrywa 50%
    }

    fun findBestShot(): Coordinate {
        val calculator = ProbabilityCalculator(board, remainingShips)
        val probMap = calculator.calculate()

        var bestCoord: Coordinate? = null
        var bestScore = -1

        // Określ preferowaną parzystość dla szachownicy (która ma więcej pól)
        val preferredCheckerboardParity = if (patternMode == PatternMode.CHECKERBOARD) {
            determinePreferredParity()
        } else 0

        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                if (board[y][x] != CellState.UNKNOWN) continue

                // Sprawdź czy w tym miejscu może zmieścić się jakiś statek
                val canFitShip = canAnyShipFitThrough(x, y)
                if (!canFitShip) continue

                // Sprawdź czy pole pasuje do ADAPTACYJNEGO wzoru
                val isPatternCell = when (patternMode) {
                    PatternMode.PATTERN_2X3 -> (x % 3) == (y % 3)
                    PatternMode.CHECKERBOARD -> (x + y) % 2 == preferredCheckerboardParity
                }

                // Oblicz score: heatmapa + bonus za wzór
                var score = probMap[y][x]

                if (isPatternCell) {
                    score *= 10 // Silny bonus dla pól zgodnych ze wzorem
                }

                if (score > bestScore) {
                    bestScore = score
                    bestCoord = Coordinate(x, y)
                }
            }
        }

        if (bestCoord == null) {
            bestCoord = findAnyAvailableCell()
        }

        return bestCoord ?: Coordinate(0, 0)
    }

    /**
     * Określ preferowaną parzystość szachownicy
     * Wybierz tę, która ma więcej dostępnych pól
     */
    private fun determinePreferredParity(): Int {
        var parity0Count = 0
        var parity1Count = 0

        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                if (board[y][x] == CellState.UNKNOWN) {
                    if ((x + y) % 2 == 0) parity0Count++ else parity1Count++
                }
            }
        }

        return if (parity0Count >= parity1Count) 0 else 1
    }

    /**
     * Sprawdza czy przez pole (x, y) może przechodzić jakikolwiek pozostały statek
     * To jest "dynamiczne zawężanie obszarów" ze strategii
     */
    private fun canAnyShipFitThrough(x: Int, y: Int): Boolean {
        val smallestShip = remainingShips.minOrNull() ?: return false

        // Sprawdź poziomo - czy jest ciągły odcinek długości >= smallestShip zawierający (x,y)
        val horizontalSpace = countContiguousSpace(x, y, dx = 1, dy = 0) +
                              countContiguousSpace(x, y, dx = -1, dy = 0) + 1
        if (horizontalSpace >= smallestShip) return true

        // Sprawdź pionowo
        val verticalSpace = countContiguousSpace(x, y, dx = 0, dy = 1) +
                            countContiguousSpace(x, y, dx = 0, dy = -1) + 1
        if (verticalSpace >= smallestShip) return true

        return false
    }

    /**
     * Liczy ciągłą przestrzeń w danym kierunku (pola UNKNOWN)
     */
    private fun countContiguousSpace(startX: Int, startY: Int, dx: Int, dy: Int): Int {
        var count = 0
        var x = startX + dx
        var y = startY + dy

        while (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE) {
            val state = board[y][x]
            // Pola UNKNOWN lub HIT (bo statek może tam być) są przestrzenią
            if (state == CellState.UNKNOWN || state == CellState.HIT) {
                count++
                x += dx
                y += dy
            } else {
                break
            }
        }

        return count
    }

    private fun findAnyAvailableCell(): Coordinate? {
        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                if (board[y][x] == CellState.UNKNOWN) {
                    return Coordinate(x, y)
                }
            }
        }
        return null
    }

    /**
     * Wariant z HIT-aware probability (używany gdy Target Mode nie może znaleźć celu)
     * Statki MUSZĄ przechodzić przez aktywne trafienia!
     */
    fun findBestShotWithHits(activeHits: List<Coordinate>): Coordinate {
        // Użyj HIT-aware kalkulatora - to kluczowe ulepszenie!
        val calculator = ProbabilityCalculator(board, remainingShips, activeHits)
        val probMap = calculator.calculate()

        var bestCoord: Coordinate? = null
        var bestProb = -1

        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                if (board[y][x] == CellState.UNKNOWN && probMap[y][x] > bestProb) {
                    bestProb = probMap[y][x]
                    bestCoord = Coordinate(x, y)
                }
            }
        }

        return bestCoord ?: findAnyAvailableCell() ?: Coordinate(0, 0)
    }
}
