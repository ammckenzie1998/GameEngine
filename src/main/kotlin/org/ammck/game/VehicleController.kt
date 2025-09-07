package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.ammck.engine.physics.PhysicsState
import org.ammck.engine.physics.PhysicsStateReport
import org.joml.Math.cos
import org.joml.Math.lerp
import org.joml.Math.sin
import org.joml.Math.sqrt

class VehicleController (val gameObject: GameObject) {

    private val WHEEL_RADIUS = 1.0f
    private val MIN_SPEED_TO_TURN = 5f
    private val MAX_STEER_ANGLE = 0.5f
    private val WHEEL_RETURN_TO_ORIGIN_SPEED = 8.0f
    private val AERIAL_ROTATION_SPEED = 10.0f

    private var wasAirborne = false

    fun update(
        deltaTime: Float,
        vehicleCommands: VehicleCommands
    ){
        val body = gameObject.physicsBody ?: return
        if (wasAirborne && body.isGrounded){
            gameObject.transform.rotationX = 0.0f
            gameObject.transform.rotationY = 0.0f
            gameObject.transform.rotationZ = 0.0f
        }
        when(body.isGrounded){
            true -> { groundControl(deltaTime, vehicleCommands) }
            false -> { aerialControl(deltaTime, vehicleCommands) }
        }
        animateSteering(deltaTime, vehicleCommands)
        wasAirborne = !body.isGrounded
    }

    private fun groundControl(deltaTime: Float, commands: VehicleCommands){
        val body = gameObject.physicsBody!!
        val currentSpeed = sqrt(body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        //Apply turning
        if(currentSpeed > MIN_SPEED_TO_TURN) {
            gameObject.transform.rotationY -= commands.steerDirection * body.turnSpeed * deltaTime
        }

        //Apply acceleration
        if(commands.throttle != 0.0f){
            val directionX = sin(gameObject.transform.rotationY)
            val directionZ = cos(gameObject.transform.rotationY)

            body.velocity.x += directionX * body.accelerationFactor * deltaTime * commands.throttle
            body.velocity.z += directionZ * body.accelerationFactor * deltaTime * commands.throttle
        }

        if (body.velocity.x != 0.0f) body.velocity.x *= body.dragFactor
        if (body.velocity.z != 0.0f) body.velocity.z *= body.dragFactor

        val finalSpeed = (body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        animateWheels(deltaTime, finalSpeed)
    }

    private fun aerialControl(deltaTime: Float, commands: VehicleCommands){
        val transform = gameObject.transform
        if(commands.pitchMode){
            if(commands.throttle > 0.0f){
                transform.rotationX += AERIAL_ROTATION_SPEED * deltaTime
            }
            if(commands.throttle < 0.0f){
                transform.rotationX -= AERIAL_ROTATION_SPEED * deltaTime
            }
            if(commands.steerDirection > 0.0f){
                transform.rotationZ += AERIAL_ROTATION_SPEED * deltaTime
            }
            if(commands.steerDirection < 0.0f){
                transform.rotationZ -= AERIAL_ROTATION_SPEED * deltaTime
            }
        } else{
            if(commands.steerDirection < 0.0f){
                transform.rotationY += AERIAL_ROTATION_SPEED * deltaTime
            }
            if(commands.steerDirection > 0.0f){
                transform.rotationY -= AERIAL_ROTATION_SPEED * deltaTime
            }
        }
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

    private fun animateSteering(deltaTime: Float, commands: VehicleCommands){
        val targetAngle = when{
            commands.steerDirection > 0.0f -> MAX_STEER_ANGLE
            commands.steerDirection < 0.0f -> -MAX_STEER_ANGLE
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