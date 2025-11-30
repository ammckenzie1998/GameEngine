package org.ammck.game.race

import org.ammck.game.components.Vehicle

data class RaceState(
    var inProgress: Boolean = true,
    var leaderboard: List<Vehicle> = mutableListOf<Vehicle>()
)
