package ai.strategies

import core.*
import kotlin.random.Random

/**
 * Strategia rozmieszczania statków zgodna z dokumentacją (punkt 7):
 * - Generuj losowe ustawienia
 * - Odrzucaj zbyt symetryczne
 * - Odrzucaj z statkami zbyt blisko siebie (rogi OK)
 * - Wybierz ustawienie o najmniejszej przewidywalności (entropia)
 */
class PlacementStrategy(private val random: Random = Random.Default) {

    companion object {
        private const val CANDIDATES_TO_GENERATE = 1000
        private const val TOP_N_TO_CHOOSE_FROM = 10  // NOWE: wybieramy losowo z top N
        private const val MIN_SHIP_SEPARATION = 2
    }

    /**
     * ULEPSZENIE: Losowy wybór z top-10
     * Zamiast zawsze wybierać najlepsze rozmieszczenie (przewidywalne),
     * wybieramy losowo z 10 najlepszych (nieprzewidywalne).
     */
    fun generateOptimalPlacement(): List<ShipPlacement> {
        val candidates = mutableListOf<Pair<List<ShipPlacement>, Double>>()

        repeat(CANDIDATES_TO_GENERATE) {
            val placement = generateRandomValidPlacement()
            if (placement != null) {
                val score = evaluatePlacement(placement)
                if (score > 0) {
                    candidates.add(Pair(placement, score))
                }
            }
        }

        if (candidates.isEmpty()) {
            // Fallback: generuj dopóki nie znajdziemy czegokolwiek
            while (true) {
                val placement = generateRandomValidPlacement()
                if (placement != null) return placement
            }
        }

        // NOWE: Sortuj po score malejąco i weź top N
        val topCandidates = candidates
            .sortedByDescending { it.second }
            .take(TOP_N_TO_CHOOSE_FROM)

        // NOWE: Losowo wybierz jedno z top N (zamiast zawsze najlepsze)
        val selectedIndex = random.nextInt(topCandidates.size)
        return topCandidates[selectedIndex].first
    }

    private fun generateRandomValidPlacement(): List<ShipPlacement>? {
        val ships = mutableListOf<Ship>()
        val sizes = SHIP_SIZES.sortedDescending() // Największe statki najpierw

        for (size in sizes) {
            val placement = findValidPlacementForShip(size, ships)
            if (placement != null) {
                ships.add(placement.toShip())
            } else {
                return null // Nie udało się umieścić statku
            }
        }

        return ships.map { ShipPlacement(it.size, it.position, it.direction) }
    }

    private fun findValidPlacementForShip(size: Int, existingShips: List<Ship>): ShipPlacement? {
        val validPlacements = mutableListOf<ShipPlacement>()

        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                for (direction in Direction.entries) {
                    val ship = Ship(size, Coordinate(x, y), direction)
                    if (GameRules.canPlaceShip(ship, existingShips)) {
                        validPlacements.add(ShipPlacement(size, Coordinate(x, y), direction))
                    }
                }
            }
        }

        if (validPlacements.isEmpty()) return null

        return validPlacements[random.nextInt(validPlacements.size)]
    }

    /**
     * Ocena rozmieszczenia - wyższy score = lepsze rozmieszczenie
     * Zgodnie ze strategią:
     * - Odrzucaj symetryczne (niski score)
     * - Odrzucaj zbyt blisko siebie (niski score)
     * - Preferuj nieprzewidywalne (wysoki score)
     * - NOWE: Wymuszaj mix orientacji (poziome + pionowe)
     */
    private fun evaluatePlacement(placements: List<ShipPlacement>): Double {
        val ships = placements.map { it.toShip() }
        var score = 100.0

        // 1. Kara za symetrię
        val symmetryPenalty = calculateSymmetryPenalty(ships)
        score -= symmetryPenalty * 20

        // 2. Kara za statki zbyt blisko siebie (ale rogi OK)
        val proximityPenalty = calculateProximityPenalty(ships)
        score -= proximityPenalty * 15

        // 3. Bonus za rozproszenie (entropia pozycji)
        val entropyBonus = calculateEntropyBonus(ships)
        score += entropyBonus * 10

        // 4. Kara za statki przy krawędziach (łatwiejsze do znalezienia)
        val edgePenalty = calculateEdgePenalty(ships)
        score -= edgePenalty * 5

        // 5. NOWE: Bonus/kara za mix orientacji
        val orientationScore = calculateOrientationMixScore(placements)
        score += orientationScore * 15

        // 6. Losowy bonus dla różnorodności
        score += random.nextDouble() * 5

        return score
    }

    /**
     * NOWE: Oblicza score za mix orientacji statków
     * Idealne: ~50% poziomych, ~50% pionowych
     * Kara za wszystkie w jednej orientacji
     */
    private fun calculateOrientationMixScore(placements: List<ShipPlacement>): Double {
        val horizontal = placements.count { it.direction == Direction.HORIZONTAL }
        val total = placements.size

        // Oblicz proporcję poziomych (0.0 do 1.0, gdzie 0.5 = idealny mix)
        val ratio = horizontal.toDouble() / total

        // Najlepszy wynik przy ratio = 0.5 (równy podział)
        // Najgorszy przy ratio = 0.0 lub 1.0 (wszystkie w jednej orientacji)
        // Używamy funkcji: 1 - |ratio - 0.5| * 2
        // Dla ratio=0.5: score=1.0
        // Dla ratio=0.0 lub 1.0: score=0.0
        val mixScore = 1.0 - kotlin.math.abs(ratio - 0.5) * 2

        // Dodatkowy bonus jeśli proporcja jest bliska ideału (40-60%)
        val bonusForGoodMix = if (ratio in 0.35..0.65) 0.5 else 0.0

        return mixScore + bonusForGoodMix
    }

    /**
     * Oblicza karę za symetryczne rozmieszczenie
     */
    private fun calculateSymmetryPenalty(ships: List<Ship>): Double {
        var penalty = 0.0
        val allCells = ships.flatMap { it.cells }

        // Sprawdź symetrię względem środka planszy
        val center = BOARD_SIZE / 2.0

        for (cell in allCells) {
            // Symetria pozioma
            val mirrorX = (2 * center - cell.x).toInt()
            if (allCells.any { it.x == mirrorX && it.y == cell.y }) {
                penalty += 0.5
            }

            // Symetria pionowa
            val mirrorY = (2 * center - cell.y).toInt()
            if (allCells.any { it.x == cell.x && it.y == mirrorY }) {
                penalty += 0.5
            }
        }

        return penalty
    }

    /**
     * Oblicza karę za statki zbyt blisko siebie
     * Rogi są OK, ale ortogonalne sąsiedztwo (poza wymaganym przez zasady) jest złe
     */
    private fun calculateProximityPenalty(ships: List<Ship>): Double {
        var penalty = 0.0

        for (i in ships.indices) {
            for (j in i + 1 until ships.size) {
                val minDist = minManhattanDistance(ships[i], ships[j])
                // Zasady wymagają minDist >= 2 (bo nie mogą się stykać bokami)
                // Ale preferujemy większe odległości
                if (minDist < MIN_SHIP_SEPARATION + 1) {
                    penalty += (MIN_SHIP_SEPARATION + 1 - minDist).toDouble()
                }
            }
        }

        return penalty
    }

    /**
     * Oblicza bonus za entropię - rozproszenie statków po planszy
     */
    private fun calculateEntropyBonus(ships: List<Ship>): Double {
        val allCells = ships.flatMap { it.cells }

        // Oblicz średnią pozycję
        val avgX = allCells.map { it.x }.average()
        val avgY = allCells.map { it.y }.average()

        // Oblicz wariancję (rozproszenie)
        val varianceX = allCells.map { (it.x - avgX) * (it.x - avgX) }.average()
        val varianceY = allCells.map { (it.y - avgY) * (it.y - avgY) }.average()

        // Wyższa wariancja = lepsze rozproszenie
        return (varianceX + varianceY) / 10.0
    }

    /**
     * Oblicza karę za statki przy krawędziach
     */
    private fun calculateEdgePenalty(ships: List<Ship>): Double {
        var penalty = 0.0

        for (ship in ships) {
            for (cell in ship.cells) {
                val edgeDist = minOf(cell.x, cell.y, BOARD_SIZE - 1 - cell.x, BOARD_SIZE - 1 - cell.y)
                if (edgeDist == 0) {
                    penalty += 1.0 // Na samej krawędzi
                } else if (edgeDist == 1) {
                    penalty += 0.3 // Blisko krawędzi
                }
            }
        }

        return penalty
    }

    private fun minManhattanDistance(ship1: Ship, ship2: Ship): Int {
        var minDist = Int.MAX_VALUE
        for (c1 in ship1.cells) {
            for (c2 in ship2.cells) {
                val dist = kotlin.math.abs(c1.x - c2.x) + kotlin.math.abs(c1.y - c2.y)
                minDist = minOf(minDist, dist)
            }
        }
        return minDist
    }
}
