package engine

import ai.Player
import core.*
import logging.GameLogger

data class GameResult(
    val winner: Int,  // 1 or 2
    val player1Shots: Int,
    val player2Shots: Int
)

class GameEngine(
    private val player1: Player,
    private val player2: Player,
    private val logger: GameLogger? = null
) {
    private val board1 = Board()  // Player 1's ships
    private val board2 = Board()  // Player 2's ships
    private var player1Shots = 0
    private var player2Shots = 0

    fun playGame(): GameResult {
        // Phase 1: Ship placement
        placeShips()

        // Phase 2: Battle
        var currentPlayer = 1
        while (!isGameOver()) {
            playTurn(currentPlayer)
            currentPlayer = if (currentPlayer == 1) 2 else 1
        }

        // Phase 3: Determine winner and log results
        return finalizeGame()
    }

    private fun placeShips() {
        // Player 1 places ships
        val placements1 = player1.placeShips()
        if (!board1.placeShips(placements1)) {
            throw IllegalStateException("Player 1 provided invalid ship placement")
        }

        // Log player 1's placements
        logger?.let { log ->
            for (placement in placements1) {
                log.logShipPlacement(placement)
            }
        }

        // Player 2 places ships
        val placements2 = player2.placeShips()
        if (!board2.placeShips(placements2)) {
            throw IllegalStateException("Player 2 provided invalid ship placement")
        }
    }

    private fun playTurn(currentPlayer: Int) {
        val shooter = if (currentPlayer == 1) player1 else player2
        val targetBoard = if (currentPlayer == 1) board2 else board1
        val opponent = if (currentPlayer == 1) player2 else player1

        val shot = shooter.getNextShot()
        val result = targetBoard.receiveShot(shot)

        // Update shot counts
        if (currentPlayer == 1) player1Shots++ else player2Shots++

        // Notify players
        shooter.onShotResult(shot, result)
        opponent.onOpponentShot(shot, result)

        // Log
        logger?.let { log ->
            if (currentPlayer == 1) {
                log.logShot(shot, result)
            } else {
                log.logEnemyShot(shot, result)
            }
        }
    }

    private fun isGameOver(): Boolean {
        return board1.allShipsSunk() || board2.allShipsSunk()
    }

    private fun finalizeGame(): GameResult {
        val winner = if (board2.allShipsSunk()) 1 else 2
        val player1Won = winner == 1

        // Log game over
        logger?.logGameOver(player1Won, player1Shots, player2Shots)

        // Log enemy ships (player 2's ships from player 1's perspective)
        logger?.let { log ->
            for (ship in board2.getShips()) {
                log.logEnemyShip(ShipPlacement(ship.size, ship.position, ship.direction))
            }
        }

        // Flush log
        logger?.flush()

        return GameResult(winner, player1Shots, player2Shots)
    }
}

// Helper for running multiple games and calculating statistics
class GameRunner {
    fun runGames(
        player1Factory: () -> Player,
        player2Factory: () -> Player,
        numGames: Int,
        withLogging: Boolean = false
    ): Statistics {
        var player1Wins = 0
        var totalPlayer1Shots = 0
        var totalPlayer2Shots = 0

        for (i in 1..numGames) {
            val p1 = player1Factory()
            val p2 = player2Factory()
            val logger = if (withLogging && i <= 5) GameLogger() else null

            val engine = GameEngine(p1, p2, logger)
            val result = engine.playGame()

            if (result.winner == 1) player1Wins++
            totalPlayer1Shots += result.player1Shots
            totalPlayer2Shots += result.player2Shots

            if (i % 100 == 0) {
                println("Completed $i games...")
            }
        }

        return Statistics(
            totalGames = numGames,
            player1Wins = player1Wins,
            player2Wins = numGames - player1Wins,
            avgPlayer1Shots = totalPlayer1Shots.toDouble() / numGames,
            avgPlayer2Shots = totalPlayer2Shots.toDouble() / numGames
        )
    }
}

data class Statistics(
    val totalGames: Int,
    val player1Wins: Int,
    val player2Wins: Int,
    val avgPlayer1Shots: Double,
    val avgPlayer2Shots: Double
) {
    val player1WinRate: Double get() = player1Wins.toDouble() / totalGames * 100

    override fun toString(): String = buildString {
        appendLine("=== Game Statistics ===")
        appendLine("Total Games: $totalGames")
        appendLine("Player 1 Wins: $player1Wins (${String.format("%.1f", player1WinRate)}%)")
        appendLine("Player 2 Wins: $player2Wins (${String.format("%.1f", 100 - player1WinRate)}%)")
        appendLine("Avg Player 1 Shots: ${String.format("%.1f", avgPlayer1Shots)}")
        appendLine("Avg Player 2 Shots: ${String.format("%.1f", avgPlayer2Shots)}")
    }
}
