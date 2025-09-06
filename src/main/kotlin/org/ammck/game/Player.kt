package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.joml.Math.cos
import org.joml.Math.lerp
import org.joml.Math.sin
import org.joml.Math.sqrt

class Player (private val gameObject: GameObject) {

    private val WHEEL_RADIUS = 1.0f

    private val MIN_SPEED_TO_TURN = 5f
    private val MAX_STEER_ANGLE = 0.5f
    private val WHEEL_RETURN_TO_ORIGIN_SPEED = 8.0f

    fun update(
        deltaTime: Float,
        playerInput: PlayerInput
    ){
        val body = gameObject.physicsBody ?: return
        val currentSpeed = sqrt(body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        //Apply turning
        if(currentSpeed > MIN_SPEED_TO_TURN && body.isGrounded) {
            if (playerInput.isTurnLeftPressed) {
                gameObject.transform.rotationY += body.turnSpeed * deltaTime
            }
            if (playerInput.isTurnRightPressed) {
                gameObject.transform.rotationY -= body.turnSpeed * deltaTime
            }
        }

        //Apply acceleration
        if(playerInput.isMoveForwardPressed || playerInput.isMoveBackwardPressed){
            val directionX = sin(gameObject.transform.rotationY)
            val directionZ = cos(gameObject.transform.rotationY)

            if (playerInput.isMoveForwardPressed){
                body.velocity.x += directionX * body.accelerationFactor * deltaTime
                body.velocity.z += directionZ * body.accelerationFactor * deltaTime
            }
            if (playerInput.isMoveBackwardPressed){
                body.velocity.x -= directionX * body.accelerationFactor * deltaTime
                body.velocity.z -= directionZ * body.accelerationFactor * deltaTime
            }
        }

        if (body.velocity.x != 0.0f) body.velocity.x *= body.dragFactor
        if (body.velocity.z != 0.0f) body.velocity.z *= body.dragFactor

        val finalSpeed = (body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        animateWheels(deltaTime, finalSpeed)
        animateSteering(deltaTime, playerInput)
    }

    private fun animateWheels(deltaTime: Float, speed: Float){
        val body = gameObject.physicsBody ?: return
        if(speed < 0.1f) return

        val forwardX = sin(gameObject.transform.rotationY)
        val forwardZ = cos(gameObject.transform.rotationY)
        val dotProduct = (body.velocity.x * forwardX) + (body.velocity.z * forwardZ)
        val rotationDirection = if (dotProduct > 0) 1.0f else -1.0f

        val distanceTraveled = speed * deltaTime
        val rotationDelta = (distanceTraveled / WHEEL_RADIUS) * rotationDirection

        for(wheel in gameObject.children){
            wheel.transform.rotationX += rotationDelta
        }
    }

    private fun animateSteering(deltaTime: Float, playerInput: PlayerInput){
        val targetAngle = when{
            playerInput.isTurnLeftPressed -> MAX_STEER_ANGLE
            playerInput.isTurnRightPressed -> -MAX_STEER_ANGLE
            else -> 0.0f
        }

        //First 2 children turn. Should not assume this later on
        if(gameObject.children.size >= 2){
            val flWheel = gameObject.children[0]
            val frWheel = gameObject.children[1]

            val t = WHEEL_RETURN_TO_ORIGIN_SPEED * deltaTime
            flWheel.transform.rotationY = lerp(flWheel.transform.rotationY, targetAngle, t)
            frWheel.transform.rotationY = lerp(flWheel.transform.rotationY, targetAngle, t)
        }
    }

}