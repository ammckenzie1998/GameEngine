package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.joml.Math.cos
import org.joml.Math.lerp
import org.joml.Math.sin
import org.joml.Math.sqrt

class Player (val gameObject: GameObject) {

    private val vehicleController = VehicleController(gameObject)

    fun update(
        deltaTime: Float,
        playerInput: PlayerInput
    ){
        val throttle = when {
            playerInput.isMoveForwardPressed -> 1.0f
            playerInput.isMoveBackwardPressed -> -1.0f
            else -> 0.0f
        }
        val steerDirection = when {
            playerInput.isTurnLeftPressed -> -1.0f
            playerInput.isTurnRightPressed -> 1.0f
            else -> 0.0f
        }
        val pitchMode = playerInput.isPitchPressed

        val commands = VehicleCommands(throttle, steerDirection, pitchMode)
        vehicleController.update(deltaTime, commands)
    }

}