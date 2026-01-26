import ai.RandomAIPlayer
import ai.SmartAIPlayer
import engine.GameEngine
import engine.GameRunner
import logging.GameLogger

fun main(args: Array<String>) {
    when {
        args.contains("--benchmark") -> runBenchmark()
        args.contains("--stats") -> runStatistics()
        else -> runSingleGame()
    }
}

fun runSingleGame() {
    println("Starting Battleships Game...")
    println()

    val player1 = SmartAIPlayer()
    val player2 = SmartAIPlayer()
    val logger = GameLogger()

    val engine = GameEngine(player1, player2, logger)
    val result = engine.playGame()

    println()
    println("=== Game Result ===")
    println("Winner: Player ${result.winner}")
    println("Player 1 shots: ${result.player1Shots}")
    println("Player 2 shots: ${result.player2Shots}")
    println()
    println("Log file: ${logger.getFilePath()}")
}

fun runBenchmark() {
    println("Running Benchmark: SmartAI vs RandomAI (1000 games)")
    println()

    val runner = GameRunner()
    val stats = runner.runGames(
        player1Factory = { SmartAIPlayer() },
        player2Factory = { RandomAIPlayer() },
        numGames = 1000,
        withLogging = true  // First 5 games will be logged
    )

    println()
    println(stats)
}

fun runStatistics() {
    println("Running Statistics: SmartAI vs SmartAI (100 games)")
    println()

    val runner = GameRunner()
    val stats = runner.runGames(
        player1Factory = { SmartAIPlayer() },
        player2Factory = { SmartAIPlayer() },
        numGames = 100,
        withLogging = true
    )

    println()
    println(stats)
}
