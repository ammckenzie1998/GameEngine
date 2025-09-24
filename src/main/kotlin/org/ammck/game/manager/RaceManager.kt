package org.ammck.game.manager

import org.ammck.engine.objects.GameObject
import org.ammck.game.Vehicle
import org.ammck.game.WaypointType

data class RacerState(
    var nextCheckpointIndex: Int = 0,
    var currentLap: Int = 1,
    var isEliminated: Boolean = false,
    var isFinished: Boolean = false
)

class RaceManager(
    val racers: List<Vehicle>,
    val gameObjects: List<GameObject>,
    val totalLaps: Int = 5) {

    private val checkpoints = gameObjects
        .filter { it.waypoint?.type == WaypointType.RACE_CHECKPOINT }
        .sortedBy { it.waypoint!!.index }

    private val racerProgress = mutableMapOf<Vehicle, RacerState>()

    init {
        for(racer in racers){
            racerProgress[racer] = RacerState()
        }
    }

    fun update(){
        if (checkpoints.isNotEmpty()){
            for(racer in racers){
                val state = racerProgress[racer] ?: continue
                if (state.isEliminated || state.isFinished) continue
                if (racer.isDestroyed){
                    println("${racer.gameObject.id} eliminated!")
                    state.isEliminated = true
                    continue
                }

                val targetCheckpoint = checkpoints[state.nextCheckpointIndex]
                val distanceToCheckpoint = racer.gameObject.getPosition().distance(targetCheckpoint.getPosition())
                val checkpointRadius = 8f

                if (distanceToCheckpoint < checkpointRadius){
                    state.nextCheckpointIndex++

                    if(state.nextCheckpointIndex >= checkpoints.size){
                        state.currentLap++
                        state.nextCheckpointIndex = 0

                        if(state.currentLap > totalLaps){
                            state.isFinished = true
                            println("${racer.gameObject.id} finished!")
                        }
                    }
                }
            }
        }
    }

    fun getRacerTarget(vehicle: Vehicle): GameObject?{
        val state = racerProgress[vehicle] ?: return null
        return checkpoints[state.nextCheckpointIndex]
    }

    fun getRacerState(vehicle: Vehicle): RacerState?{
        return racerProgress[vehicle]
    }
}