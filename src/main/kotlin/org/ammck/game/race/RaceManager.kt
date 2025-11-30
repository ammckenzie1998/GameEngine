package org.ammck.game.race

import org.ammck.engine.objects.GameObject
import org.ammck.game.components.Vehicle
import org.ammck.game.WaypointType

class RaceManager(
    val racers: List<Vehicle>,
    val gameObjects: List<GameObject>,
    val totalLaps: Int = 3) {

    val raceState: RaceState = RaceState()

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
        if(checkpoints.isEmpty()) raceState.inProgress = false

        if(!raceState.inProgress) return

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
                        raceState.inProgress = false
                        return
                    }
                }
            }
        }

        val activeRacers = racers.filter { racerProgress[it]?.isEliminated == false }
        if (activeRacers.size == 1 && racers.size > 1){
            raceState.inProgress = false
        }
        updateLeaderboard()
    }

    private fun updateLeaderboard(){
        val sortedRacers = racers.filter { racerProgress[it]?.isEliminated == false }
            .sortedWith(compareByDescending<Vehicle> { racerProgress[it]?.currentLap }
                .thenByDescending { racerProgress[it]?.nextCheckpointIndex }
                .thenBy{
                    val target = getRacerTarget(it)
                    if(target != null){
                        val target = getRacerTarget(it)
                        it.gameObject.getPosition().distanceSquared(target!!.getPosition())
                    } else{
                        Float.MAX_VALUE
                    }
                }
            )
        raceState.leaderboard = sortedRacers
    }

    fun getRacerTarget(vehicle: Vehicle): GameObject?{
        val state = racerProgress[vehicle] ?: return null
        return checkpoints[state.nextCheckpointIndex]
    }

    fun getRacerState(vehicle: Vehicle): RacerState?{
        return racerProgress[vehicle]
    }

    fun getRacerPosition(vehicle: Vehicle): Int{
        return raceState.leaderboard.indexOf(vehicle) + 1
    }
}