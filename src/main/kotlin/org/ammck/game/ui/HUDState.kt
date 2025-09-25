package org.ammck.game.ui

data class HUDState(
    var healthPercentage: Float = 0f,
    var stylePercentage: Float = 0f,
    var speedKPH: Int = 0,
    var currentLap: Int = 0,
    var totalLaps: Int = 0,
    var currentPos: Int = 0,
    var totalRacers: Int = 0,
    var eliminatedRacers: Int = 0,
)
