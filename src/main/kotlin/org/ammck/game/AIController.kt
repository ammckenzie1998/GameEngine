package org.ammck.game

import org.joml.Vector3f

class AIController(
    val vehicle: Vehicle,
    private val raceManager: RaceManager) {

    fun update(
        deltaTime: Float
    ){
        val transform = vehicle.gameObject.transform
        val targetCheckpoint = raceManager.getRacerTarget(vehicle)
        val commands: VehicleCommands
        val throttle: Float
        val steerDirection: Float

        if(targetCheckpoint == null){
            throttle = -1.0f
            steerDirection = 0.0f
        } else{
            val targetPosition = targetCheckpoint.getPosition()
            val directionToTarget = Vector3f(targetPosition).sub(transform.position).normalize()
            val rightVector = Vector3f(1f, 0f, 0f).rotate(transform.orientation)
            val dotProduct = rightVector.dot(directionToTarget)

            if(dotProduct > 0.15f){
                steerDirection = 1.0f
            } else if(dotProduct < 0.15f){
                steerDirection = -1.0f
            } else{
                steerDirection = 0.0f
            }
            throttle = 1.0f

        }
        commands = VehicleCommands(throttle, steerDirection)
        vehicle.update(deltaTime, commands)
    }

}