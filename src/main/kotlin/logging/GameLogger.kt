package logging

import core.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GameLogger {
    private val buffer = mutableListOf<String>()
    private val filePath: String
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    init {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        filePath = "ships-game-$timestamp.log"
    }

    private fun now(): String = LocalDateTime.now().format(timeFormatter)

    fun logShipPlacement(placement: ShipPlacement) {
        val pos = placement.position
        val dir = placement.direction.toLogString()
        buffer.add("${now()} place-ship: size=${placement.size} pos=(${pos.x},${pos.y}) dir=$dir")
    }

    fun logShot(position: Coordinate, result: ShotResult) {
        buffer.add("${now()} shot: pos=(${position.x},${position.y}) ${result.toLogString()}")
    }

    fun logEnemyShot(position: Coordinate, result: ShotResult) {
        buffer.add("${now()} enemy-shot: pos=(${position.x},${position.y}) ${result.toLogString()}")
    }

    fun logGameOver(won: Boolean, totalShots: Int, enemyTotalShots: Int) {
        val result = if (won) "win" else "loss"
        buffer.add("${now()} game-over: result=$result total-shots=$totalShots enemy-total-shots=$enemyTotalShots")
    }

    fun logEnemyShip(placement: ShipPlacement) {
        val pos = placement.position
        val dir = placement.direction.toLogString()
        buffer.add("${now()} enemy-ship: size=${placement.size} pos=(${pos.x},${pos.y}) dir=$dir")
    }

    fun flush() {
        File(filePath).writeText(buffer.joinToString("\n"))
        println("Game log written to: $filePath")
    }

    fun getFilePath(): String = filePath
}
