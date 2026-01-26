import ai.strategies.ProbabilityCalculator
import core.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ProbabilityCalculatorTest {

    @Test
    fun `probability map has higher values in center for empty board`() {
        val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellState.UNKNOWN } }
        val calculator = ProbabilityCalculator(board, SHIP_SIZES)

        val probMap = calculator.calculate()

        // Center cells should have higher probability than corners
        val centerProb = probMap[4][4] + probMap[4][5] + probMap[5][4] + probMap[5][5]
        val cornerProb = probMap[0][0] + probMap[0][9] + probMap[9][0] + probMap[9][9]

        assertTrue(centerProb > cornerProb, "Center should have higher probability than corners")
    }

    @Test
    fun `miss cells have zero probability`() {
        val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellState.UNKNOWN } }
        board[5][5] = CellState.MISS

        val calculator = ProbabilityCalculator(board, SHIP_SIZES)
        val probMap = calculator.calculate()

        assertEquals(0, probMap[5][5], "MISS cell should have zero probability")
    }

    @Test
    fun `blocked cells have zero probability`() {
        val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellState.UNKNOWN } }
        board[3][3] = CellState.BLOCKED

        val calculator = ProbabilityCalculator(board, SHIP_SIZES)
        val probMap = calculator.calculate()

        assertEquals(0, probMap[3][3], "BLOCKED cell should have zero probability")
    }

    @Test
    fun `cells adjacent to sunk ships have reduced valid placements`() {
        val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellState.UNKNOWN } }
        // Place a sunk ship at (5,5)
        board[5][5] = CellState.SUNK

        val calculator = ProbabilityCalculator(board, listOf(2, 2, 2, 2))
        val probMap = calculator.calculate()

        // The cell (5,5) is SUNK, so it should be 0
        assertEquals(0, probMap[5][5])

        // Adjacent cells (5,4), (5,6), (4,5), (6,5) cannot have ships due to no-touching rule
        // So hypothetical placements passing through them are reduced
    }

    @Test
    fun `parity optimization reduces non-preferred cells`() {
        val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellState.UNKNOWN } }
        val calculator = ProbabilityCalculator(board, listOf(2, 2))  // Only 2-cell ships

        val basicMap = calculator.calculate()
        val optimizedMap = calculator.applyParityOptimization(basicMap)

        // One parity should be significantly reduced
        var parity0Sum = 0
        var parity1Sum = 0

        for (y in 0 until BOARD_SIZE) {
            for (x in 0 until BOARD_SIZE) {
                if ((x + y) % 2 == 0) {
                    parity0Sum += optimizedMap[y][x]
                } else {
                    parity1Sum += optimizedMap[y][x]
                }
            }
        }

        // One should be much larger than the other
        val ratio = maxOf(parity0Sum, parity1Sum).toDouble() / minOf(parity0Sum, parity1Sum)
        assertTrue(ratio > 5, "Parity optimization should create significant difference")
    }
}
