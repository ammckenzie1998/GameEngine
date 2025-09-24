package org.ammck.game.race

import org.ammck.game.Vehicle

data class RaceState(
    var inProgress: Boolean = true,
    var leaderboard: List<Vehicle> = mutableListOf<Vehicle>()
)
