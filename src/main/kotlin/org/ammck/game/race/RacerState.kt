package org.ammck.game.race

data class RacerState(
    var nextCheckpointIndex: Int = 0,
    var currentLap: Int = 1,
    var isEliminated: Boolean = false,
    var isFinished: Boolean = false
)
