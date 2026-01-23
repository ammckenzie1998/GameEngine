package org.ammck.game.components

import org.ammck.engine.objects.GameObject
import org.ammck.engine.physics.OrientedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.ammck.game.VehicleCommands
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

class Vehicle (val gameObject: GameObject) {

    private val WHEEL_RADIUS = 1.0f
    private val MIN_SPEED_TO_TURN = 5f
    private val MAX_STEER_ANGLE = 0.5f
    private val STEERING_RESPONSE_SPEED = 10.0f
    private val WHEEL_RETURN_TO_ORIGIN_SPEED = 8.0f
    private val AERIAL_ROTATION_SPEED = 10.0f
    private val GROUND_ALIGNMENT_SPEED = 8.0f

    private val POINTS_PER_SECOND_AIRTIME = 1.0f
    private val POINTS_PER_RADIAN_ROTATION = 1.0f
    private val FULL_FLIP_BONUS = 1.0f

    val MAX_HEALTH = 100.0f
    var currentHealth = MAX_HEALTH
        private set
    var isDestroyed: Boolean = false
        private set

    val MAX_STYLEPOINTS = 100.0f
    private val STYLEPOINT_DECAY_RATE = 5.0f
    var currentStylePoints = 0.0f
        private set

    private var currentAirtime = 0.0f
    private var totalPitchRotation = 0.0f
    private var totalRollRotation = 0.0f
    private var lastFrameOrientation = Quaternionf()

    private var wasAirborne = false
    private var currentSteeringAmount = 0.0f

    private val maxSpeed = 55.0f

    fun update(
        deltaTime: Float,
        vehicleCommands: VehicleCommands,
        onSpawn: ((GameObject) -> Unit)? = null
    ) {
        val body = gameObject.physicsBody ?: return

        val justLanded = wasAirborne && body.isGrounded
        val justTookOff = !wasAirborne && !body.isGrounded

        if(justLanded){
            calculateStuntBonus()
        }
        if(justTookOff){
            currentAirtime = 0.0f
            totalPitchRotation = 0.0f
            totalRollRotation = 0.0f
            lastFrameOrientation.set(gameObject.transform.orientation)
        }

        if(currentHealth > 0) {
            processSteeringInput(deltaTime, vehicleCommands)
            when (body.isGrounded) {
                true -> {
                    groundControl(deltaTime, vehicleCommands)
                    currentStylePoints = max(0f, currentStylePoints - STYLEPOINT_DECAY_RATE * deltaTime)
                }

                false -> {
                    aerialControl(deltaTime, vehicleCommands)
                }
            }
            animateSteering(deltaTime, vehicleCommands)
            if (body.lastImpactImpulse > 0) applyDamage(body.lastImpactImpulse)
        }

        for(child in gameObject.children){
            if(child.weapon != null){
                child.weapon!!.coolDown(deltaTime)
                if(vehicleCommands.weaponTrigger) {
                    val projectile = child.weapon!!.fire(child.getWorldTransform(), deltaTime)
                    if(projectile != null){
                        val boundingBox = OrientedBoundingBox(projectile.transform, projectile.transform.scale)
                        val bulletBody = PhysicsBody(boundingBox)
                        val carVel = body.velocity
                        carVel.y = 0f
                        bulletBody.velocity = Vector3f(projectile.velocity).add(carVel)
                        val projectileObject = GameObject(
                            id = "projectile_${System.currentTimeMillis()}",
                            respawnable = false,
                            transform = projectile.transform,
                            model = projectile.model,
                            physicsBody = bulletBody
                        )
                        onSpawn!!(projectileObject)
                    }
                }
            }
        }

        wasAirborne = !body.isGrounded
    }

    fun applyDamage(damageAmount: Float){
        if (isDestroyed) return

        val styleMultiplier = currentStylePoints / MAX_STYLEPOINTS
        val damageReduction = damageAmount * styleMultiplier
        val damageFinal = damageAmount - damageReduction

        currentHealth -= damageFinal
        if(currentHealth <= 0){
            currentHealth = 0f
            isDestroyed = true
        }
    }

    fun addHealthFlat(healthAmount: Float){
        currentHealth += healthAmount
        currentHealth = max(currentHealth, MAX_HEALTH)
    }

    fun addHealthPercentage(healthAmount: Float){
        currentHealth += (healthAmount * MAX_HEALTH)
        currentHealth = max(currentHealth, MAX_HEALTH)
    }

    private fun calculateStuntBonus(){
        var bonus = 0.0f
        bonus += currentAirtime * POINTS_PER_SECOND_AIRTIME

        val totalRotation = totalPitchRotation + totalRollRotation
        bonus += totalRotation * POINTS_PER_RADIAN_ROTATION

        val fullFlips = (totalPitchRotation / (2 * Math.PI)).toInt()
        val fullRolls = (totalRollRotation / (2 * Math.PI)).toInt()
        bonus += (fullFlips + fullRolls) * FULL_FLIP_BONUS

        if(bonus > 5) {
            if (gameObject.id == "Player") println("Stunt bonus! Airtime: $currentAirtime, Flips: $fullFlips, Rolls: $fullRolls, Award: $bonus")
            currentStylePoints = java.lang.Float.min(MAX_STYLEPOINTS, currentStylePoints + bonus)
        }
    }

    private fun groundControl(deltaTime: Float, commands: VehicleCommands){
        val body = gameObject.physicsBody!!
        val transform = gameObject.transform

        val currentSpeed = org.joml.Math.sqrt(body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)
        if (currentSpeed > MIN_SPEED_TO_TURN) {
            transform.orientation.rotateAxis(currentSteeringAmount * body.turnSpeed * deltaTime, 0f, 1f, 0f)
        }

        if (body.groundNormal.lengthSquared() > 0.1f) {
            val currentUp = Vector3f(0f, 1f, 0f).rotate(transform.orientation)
            val alignRotation = Quaternionf().rotationTo(currentUp, body.groundNormal)
            transform.orientation.slerp(alignRotation.mul(transform.orientation, Quaternionf()), GROUND_ALIGNMENT_SPEED * deltaTime)
        }

        val velocity = body.velocity
        val horizontalVelocity = (velocity.x * velocity.x) + (velocity.z * velocity.z)

        if(commands.throttle != 0.0f && horizontalVelocity < (maxSpeed * maxSpeed)){
            val forward = Vector3f(0f, 0f, -1f).rotate(transform.orientation)
            val styleMultiplier = 1.0f + (currentStylePoints / MAX_STYLEPOINTS)
            val accel = body.accelerationFactor * styleMultiplier
            body.forces.add(forward.mul(accel * commands.throttle))
        }

        val finalSpeed = org.joml.Math.sqrt(body.velocity.x * body.velocity.x + body.velocity.z * body.velocity.z)

        if(commands.handbrake){
            body.lateralGrip = 0.99f
            body.linearDrag = 0.99f
        } else{
            body.lateralGrip = 0.90f
            body.linearDrag = 0.995f
        }
        animateWheels(deltaTime, finalSpeed)
    }

    private fun aerialControl(deltaTime: Float, commands: VehicleCommands){
        val transform = gameObject.transform
        currentAirtime += deltaTime

        val deltaRotation = Quaternionf(lastFrameOrientation).invert().mul(transform.orientation)
        val eulerDelta = deltaRotation.getEulerAnglesXYZ(Vector3f())

        totalPitchRotation += abs(eulerDelta.x)
        totalRollRotation += abs(eulerDelta.z)
        lastFrameOrientation.set(transform.orientation)

        if(commands.pitchMode){
            transform.orientation.rotateX(-commands.throttle * AERIAL_ROTATION_SPEED * deltaTime)
            transform.orientation.rotateZ(-commands.steerDirection * AERIAL_ROTATION_SPEED * deltaTime)
        } else{
            transform.orientation.rotateY(-commands.steerDirection * AERIAL_ROTATION_SPEED * deltaTime)
        }
    }

    private fun processSteeringInput(deltaTime: Float, commands: VehicleCommands){
        val targetInput = -commands.steerDirection
        val diff = targetInput - currentSteeringAmount
        val step = STEERING_RESPONSE_SPEED * deltaTime

        if(abs(diff) < step){
            currentSteeringAmount = targetInput
        } else{
            currentSteeringAmount += sign(diff) * step
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

        for(wheel in gameObject.children.filter { it.id.contains("WHEEL")}){
            wheel.transform.orientation.rotateX(rotationDelta)
        }
    }

    private fun animateSteering(deltaTime: Float, commands: VehicleCommands){
        val targetAngle = when{
            commands.steerDirection < 0.0f -> MAX_STEER_ANGLE
            commands.steerDirection > 0.0f -> -MAX_STEER_ANGLE
            else -> 0.0f
        }

        if(gameObject.children.size > 0){
            val flWheel = gameObject.children.filter { it.id.contains("WHEEL_FL") }[0]
            val frWheel = gameObject.children.filter { it.id.contains("WHEEL_FR") }[0]

            val t = WHEEL_RETURN_TO_ORIGIN_SPEED * deltaTime
            val targetOrientation = Quaternionf().rotateY(targetAngle)
            flWheel.transform.orientation.slerp(targetOrientation, t)
            frWheel.transform.orientation.slerp(targetOrientation, t)
        }
    }
}