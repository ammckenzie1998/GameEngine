package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.joml.Math.cos
import org.joml.Math.lerp
import org.joml.Math.sin
import org.joml.Math.sqrt
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.Vector

class Vehicle (val gameObject: GameObject) {

    private val WHEEL_RADIUS = 1.0f
    private val MIN_SPEED_TO_TURN = 5f
    private val MAX_STEER_ANGLE = 0.5f
    private val WHEEL_RETURN_TO_ORIGIN_SPEED = 8.0f
    private val AERIAL_ROTATION_SPEED = 10.0f
    private val GROUND_ALIGNMENT_SPEED = 8.0f

    private var wasAirborne = false

    fun update(
        deltaTime: Float,
        vehicleCommands: VehicleCommands
    ) {
        val body = gameObject.physicsBody ?: return
        when (body.isGrounded) {
            true -> {
                groundControl(deltaTime, vehicleCommands)
            }
            false -> {
                aerialControl(deltaTime, vehicleCommands)
            }
        }
        animateSteering(deltaTime, vehicleCommands)
        wasAirborne = !body.isGrounded
    }

    private fun groundControl(deltaTime: Float, commands: VehicleCommands){
        val body = gameObject.physicsBody!!
        val transform = gameObject.transform

        val currentSpeed = sqrt(body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        if (currentSpeed > MIN_SPEED_TO_TURN) {
            transform.orientation.rotateAxis(-commands.steerDirection * body.turnSpeed * deltaTime, 0f, 1f, 0f)
        }

        if (body.groundNormal.lengthSquared() > 0.1f) {
            val currentUp = Vector3f(0f, 1f, 0f).rotate(transform.orientation)
            val alignRotation = Quaternionf().rotationTo(currentUp, body.groundNormal)
            transform.orientation.slerp(alignRotation.mul(transform.orientation, Quaternionf()), GROUND_ALIGNMENT_SPEED * deltaTime)
        }


        if(commands.throttle != 0.0f){
            val forward = Vector3f(0f, 0f, -1f).rotate(transform.orientation)
            body.forces.add(forward.mul(body.accelerationFactor * commands.throttle))
        }

        val finalSpeed = sqrt(body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        animateWheels(deltaTime, finalSpeed)
    }

    private fun aerialControl(deltaTime: Float, commands: VehicleCommands){
        val transform = gameObject.transform
        if(commands.pitchMode){
            transform.orientation.rotateX(-commands.throttle * AERIAL_ROTATION_SPEED * deltaTime)
            transform.orientation.rotateZ(-commands.steerDirection * AERIAL_ROTATION_SPEED * deltaTime)
        } else{
            transform.orientation.rotateY(-commands.steerDirection * AERIAL_ROTATION_SPEED * deltaTime)
        }
    }

    private fun animateWheels(deltaTime: Float, speed: Float){
        val body = gameObject.physicsBody ?: return
        if(speed < 0.1f) return

        val forward = Vector3f(0f, 0f, -1f).rotate(gameObject.transform.orientation)
        val dotProduct = body.velocity.dot(forward)
        val rotationDirection = if (dotProduct > 0) -1.0f else 1.0f

        val distanceTraveled = speed * deltaTime
        val rotationDelta = (distanceTraveled / WHEEL_RADIUS) * rotationDirection

        for(wheel in gameObject.children){
            wheel.transform.orientation.rotateX(rotationDelta)
        }
    }

    private fun animateSteering(deltaTime: Float, commands: VehicleCommands){
        val targetAngle = when{
            commands.steerDirection < 0.0f -> MAX_STEER_ANGLE
            commands.steerDirection > 0.0f -> -MAX_STEER_ANGLE
            else -> 0.0f
        }

        //First 2 children turn. Should not assume this later on
        if(gameObject.children.size >= 2){
            val flWheel = gameObject.children[2]
            val frWheel = gameObject.children[3]

            val t = WHEEL_RETURN_TO_ORIGIN_SPEED * deltaTime
            val targetOrientation = Quaternionf().rotateY(targetAngle)
            flWheel.transform.orientation.slerp(targetOrientation, t)
            frWheel.transform.orientation.slerp(targetOrientation, t)
        }
    }
}