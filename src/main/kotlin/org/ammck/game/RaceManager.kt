package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.ammck.games.Waypoint

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
                println(distanceToCheckpoint)
                val checkpointRadius = 2f

                if (distanceToCheckpoint < checkpointRadius){
                    println("Checkpoint $nextCheckpointIndex passed!")
                    racerProgress[racer] = nextCheckpointIndex + 1

                    if(racerProgress[racer]!! >= checkpoints.size){
                        println("Lap complete!")
                        racerProgress[racer] = 0
                    }
                }
            }
        }
    }
}