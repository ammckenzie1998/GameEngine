package org.ammck.game.manager

import org.ammck.engine.objects.GameObject
import org.ammck.game.Vehicle
import org.ammck.game.WaypointType

class RaceManager(
    val racers: List<Vehicle>,
    val gameObjects: List<GameObject>) {

    private val checkpoints = gameObjects
        .filter { it.waypoint?.type == WaypointType.RACE_CHECKPOINT }
        .sortedBy { it.waypoint!!.index }

    private val racerProgress = mutableMapOf<Vehicle, Int>()

    init {
        for(racer in racers){
            racerProgress[racer] = 0
        }
    }

    fun update(){
        if (checkpoints.isNotEmpty()){
            for(racer in racers){
                val nextCheckpointIndex = racerProgress[racer] ?: continue
                if (nextCheckpointIndex >= checkpoints.size) continue

                val targetCheckpoint = checkpoints[nextCheckpointIndex]
                val distanceToCheckpoint = racer.gameObject.getPosition().distance(targetCheckpoint.getPosition())
                val checkpointRadius = 8f

                if (distanceToCheckpoint < checkpointRadius){
                    racerProgress[racer] = nextCheckpointIndex + 1

                    if(racerProgress[racer]!! >= checkpoints.size){
                        racerProgress[racer] = 0
                    }
                }
            }
        }
    }

    fun getRacerTarget(vehicle: Vehicle): GameObject?{
        val nextCheckpoint = racerProgress[vehicle] ?: return null
        return checkpoints[nextCheckpoint]
    }
}