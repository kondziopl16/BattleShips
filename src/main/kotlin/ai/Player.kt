package ai

import core.Coordinate
import core.ShipPlacement
import core.ShotResult

interface Player {
    fun placeShips(): List<ShipPlacement>
    fun getNextShot(): Coordinate
    fun onShotResult(position: Coordinate, result: ShotResult)
    fun onOpponentShot(position: Coordinate, result: ShotResult)
    fun reset()
}
