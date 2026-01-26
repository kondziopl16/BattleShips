package ai.strategies

import core.*

/**
 * Target Mode - polowanie po trafieniu (precyzyjne zatapianie)
 *
 * Strategie zgodne z dokumentacją:
 * 2.1. Zapisz punkt trafienia do kolejki "frontier"
 * 2.2. Sprawdź cztery kierunki
 * 2.3. Ustal orientację statku po drugim trafieniu
 * 2.4. Zatapianie liniowe - idź w jednym kierunku aż do wody/zatopienia
 */
class TargetMode(
    private val board: Array<Array<CellState>>,
    private val remainingShips: List<Int>
) {
    // Kierunki do sprawdzenia: prawo, lewo, dół, góra
    private val directions = listOf(
        Direction(1, 0),   // prawo
        Direction(-1, 0),  // lewo
        Direction(0, 1),   // dół
        Direction(0, -1)   // góra
    )

    data class Direction(val dx: Int, val dy: Int)

    fun findBestTarget(activeHits: List<Coordinate>): Coordinate? {
        if (activeHits.isEmpty()) return null

        return when {
            activeHits.size == 1 -> singleHitStrategy(activeHits[0])
            else -> multipleHitsStrategy(activeHits)
        }
    }

    /**
     * 2.2. Po pojedynczym trafieniu - sprawdź cztery kierunki
     * ULEPSZENIE: Używa HIT-aware heatmapy do wyboru najlepszego kierunku
     */
    private fun singleHitStrategy(hit: Coordinate): Coordinate? {
        // Użyj HIT-aware kalkulatora dla lepszego wyboru kierunku
        val calculator = ProbabilityCalculator(board, remainingShips, listOf(hit))
        val probMap = calculator.calculate()

        val candidates = mutableListOf<Triple<Coordinate, Int, Int>>() // coord, probability, space

        for (dir in directions) {
            val neighbor = Coordinate(hit.x + dir.dx, hit.y + dir.dy)

            if (canShoot(neighbor)) {
                // Pobierz prawdopodobieństwo z HIT-aware heatmapy
                val probability = probMap[neighbor.y][neighbor.x]

                // Oblicz też przestrzeń jako secondary factor
                val spaceInDirection = countSpaceInDirection(hit, dir)
                val spaceOpposite = countSpaceInDirection(hit, Direction(-dir.dx, -dir.dy))
                val totalSpace = spaceInDirection + spaceOpposite + 1

                candidates.add(Triple(neighbor, probability, totalSpace))
            }
        }

        // Wybierz kierunek z najwyższym prawdopodobieństwem
        // Przy remisie wybierz ten z większą przestrzenią
        return candidates
            .sortedWith(compareByDescending<Triple<Coordinate, Int, Int>> { it.second }
                .thenByDescending { it.third })
            .firstOrNull()?.first
    }

    /**
     * 2.3 & 2.4. Po wielu trafieniach - ustal orientację i zatapiaj liniowo
     */
    private fun multipleHitsStrategy(hits: List<Coordinate>): Coordinate? {
        // Określ orientację statku
        val orientation = determineOrientation(hits)

        return when (orientation) {
            Orientation.HORIZONTAL -> extendHorizontally(hits)
            Orientation.VERTICAL -> extendVertically(hits)
            Orientation.UNKNOWN -> handleMixedHits(hits)
        }
    }

    /**
     * 2.3. Ustal orientację statku:
     * - jeśli x się zmienia → statek poziomy
     * - jeśli y się zmienia → statek pionowy
     */
    private fun determineOrientation(hits: List<Coordinate>): Orientation {
        if (hits.size < 2) return Orientation.UNKNOWN

        val firstHit = hits[0]
        val secondHit = hits[1]

        return when {
            firstHit.y == secondHit.y && firstHit.x != secondHit.x -> Orientation.HORIZONTAL
            firstHit.x == secondHit.x && firstHit.y != secondHit.y -> Orientation.VERTICAL
            else -> {
                // Sprawdź czy wszystkie trafienia są w linii
                val allSameY = hits.all { it.y == firstHit.y }
                val allSameX = hits.all { it.x == firstHit.x }

                when {
                    allSameY -> Orientation.HORIZONTAL
                    allSameX -> Orientation.VERTICAL
                    else -> Orientation.UNKNOWN // Wiele statków
                }
            }
        }
    }

    /**
     * 2.4. Zatapianie liniowe poziomo
     * Idź w jednym kierunku aż trafisz wodę, potem zmień kierunek
     */
    private fun extendHorizontally(hits: List<Coordinate>): Coordinate? {
        val sortedByX = hits.sortedBy { it.x }
        val y = hits[0].y
        val minX = sortedByX.first().x
        val maxX = sortedByX.last().x

        // Sprawdź czy są luki między trafieniami
        for (x in minX..maxX) {
            val coord = Coordinate(x, y)
            if (canShoot(coord)) {
                return coord // Wypełnij lukę
            }
        }

        // Próbuj rozszerzyć w prawo
        val rightCandidate = Coordinate(maxX + 1, y)
        if (canShoot(rightCandidate)) return rightCandidate

        // Próbuj rozszerzyć w lewo
        val leftCandidate = Coordinate(minX - 1, y)
        if (canShoot(leftCandidate)) return leftCandidate

        // Oba końce zablokowane - może to wiele statków
        return null
    }

    /**
     * 2.4. Zatapianie liniowe pionowo
     */
    private fun extendVertically(hits: List<Coordinate>): Coordinate? {
        val sortedByY = hits.sortedBy { it.y }
        val x = hits[0].x
        val minY = sortedByY.first().y
        val maxY = sortedByY.last().y

        // Sprawdź czy są luki między trafieniami
        for (y in minY..maxY) {
            val coord = Coordinate(x, y)
            if (canShoot(coord)) {
                return coord // Wypełnij lukę
            }
        }

        // Próbuj rozszerzyć w dół
        val downCandidate = Coordinate(x, maxY + 1)
        if (canShoot(downCandidate)) return downCandidate

        // Próbuj rozszerzyć w górę
        val upCandidate = Coordinate(x, minY - 1)
        if (canShoot(upCandidate)) return upCandidate

        return null
    }

    /**
     * Obsługa gdy trafienia nie są w linii - prawdopodobnie wiele statków
     */
    private fun handleMixedHits(hits: List<Coordinate>): Coordinate? {
        // Znajdź największą grupę liniową
        val horizontalGroups = hits.groupBy { it.y }
        val verticalGroups = hits.groupBy { it.x }

        val bestHorizontal = horizontalGroups.values.maxByOrNull { it.size }
        val bestVertical = verticalGroups.values.maxByOrNull { it.size }

        val horizontalSize = bestHorizontal?.size ?: 0
        val verticalSize = bestVertical?.size ?: 0

        // Priorytetyzuj większą grupę
        return when {
            horizontalSize >= 2 && horizontalSize >= verticalSize -> {
                extendHorizontally(bestHorizontal!!)
            }
            verticalSize >= 2 -> {
                extendVertically(bestVertical!!)
            }
            else -> {
                // Wszystkie pojedyncze - wybierz pierwsze trafienie i sprawdź kierunki
                singleHitStrategy(hits.first())
            }
        }
    }

    /**
     * Liczy dostępną przestrzeń w danym kierunku
     */
    private fun countSpaceInDirection(start: Coordinate, dir: Direction): Int {
        var count = 0
        var x = start.x + dir.dx
        var y = start.y + dir.dy

        while (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE) {
            val state = board[y][x]
            when (state) {
                CellState.UNKNOWN -> {
                    count++
                    x += dir.dx
                    y += dir.dy
                }
                CellState.HIT -> {
                    count++
                    x += dir.dx
                    y += dir.dy
                }
                else -> break // MISS, SUNK, BLOCKED - koniec
            }
        }

        return count
    }

    private fun canShoot(coord: Coordinate): Boolean {
        return coord.isValid() && board[coord.y][coord.x] == CellState.UNKNOWN
    }

    private enum class Orientation {
        HORIZONTAL,
        VERTICAL,
        UNKNOWN
    }
}
