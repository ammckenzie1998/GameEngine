package org.ammck.game

import org.ammck.engine.objects.GameObject

class Player (val gameObject: GameObject) {

    private val vehicle = Vehicle(gameObject)

    fun update(
        deltaTime: Float,
        playerInput: PlayerInput
    ){
        val throttle = when {
            playerInput.isMoveForwardPressed -> -1.0f
            playerInput.isMoveBackwardPressed -> 1.0f
            else -> 0.0f
        }
        val steerDirection = when {
            playerInput.isTurnLeftPressed -> -1.0f
            playerInput.isTurnRightPressed -> 1.0f
            else -> 0.0f
        }
        val pitchMode = playerInput.isPitchPressed

        val commands = VehicleCommands(throttle, steerDirection, pitchMode)
        vehicle.update(deltaTime, commands)
    }

}