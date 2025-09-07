package org.ammck.game

data class VehicleCommands(
    val throttle: Float = 0.0f,
    val steerDirection: Float = 0.0f,
    val pitchMode: Boolean = false
)
