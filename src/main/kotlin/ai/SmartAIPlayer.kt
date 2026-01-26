package ai

import ai.strategies.*
import core.*
import kotlin.random.Random

class SmartAIPlayer(
    private val random: Random = Random.Default
) : Player {
    private val trackingBoard = TrackingBoard()
    private val placementStrategy = PlacementStrategy(random)
    private var myPlacements: List<ShipPlacement> = emptyList()

    override fun placeShips(): List<ShipPlacement> {
        myPlacements = placementStrategy.generateOptimalPlacement()
        return myPlacements
    }

    override fun getNextShot(): Coordinate {
        val activeHits = trackingBoard.getActiveHits()

        return if (activeHits.isEmpty()) {
            // Hunt mode - używaj wszystkich pozostałych statków
            val remainingShips = trackingBoard.getRemainingShips()
            val huntMode = HuntMode(trackingBoard.state, remainingShips)
            huntMode.findBestShot()
        } else {
            // Target mode - ULEPSZENIE: używaj tylko statków które pasują do trafień
            // Jeśli mamy 3 trafienia w linii, statek ma min. 4 pola
            val filteredShips = trackingBoard.getFilteredRemainingShips()
            val targetMode = TargetMode(trackingBoard.state, filteredShips)
            val target = targetMode.findBestTarget(activeHits)

            if (target != null) {
                target
            } else {
                // Fallback to hunt mode with hit boost
                val huntMode = HuntMode(trackingBoard.state, filteredShips)
                huntMode.findBestShotWithHits(activeHits)
            }
        }
    }

    override fun onShotResult(position: Coordinate, result: ShotResult) {
        trackingBoard.recordShot(position, result)
    }

    override fun onOpponentShot(position: Coordinate, result: ShotResult) {
        // We could track opponent's strategy here for future improvements
    }

    override fun reset() {
        trackingBoard.reset()
    }

    fun getTrackingBoard(): TrackingBoard = trackingBoard
}

// Simple random AI for testing
class RandomAIPlayer(
    private val random: Random = Random.Default
) : Player {
    private val shotsFired = mutableSetOf<Coordinate>()

    override fun placeShips(): List<ShipPlacement> {
        return PlacementStrategy(random).generateOptimalPlacement()
    }

    override fun getNextShot(): Coordinate {
        val available = mutableListOf<Coordinate>()
        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                val coord = Coordinate(x, y)
                if (coord !in shotsFired) {
                    available.add(coord)
                }
            }
        }

        val shot = available[random.nextInt(available.size)]
        shotsFired.add(shot)
        return shot
    }

    override fun onShotResult(position: Coordinate, result: ShotResult) {
        shotsFired.add(position)
    }

    override fun onOpponentShot(position: Coordinate, result: ShotResult) {
        // Ignore
    }

    override fun reset() {
        shotsFired.clear()
    }
}
