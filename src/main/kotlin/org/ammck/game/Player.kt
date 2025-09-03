package org.ammck.game

import org.ammck.Game
import org.ammck.render.Mesh
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Vector3f
import kotlin.times

class Player (val mesh: Mesh) {

    private val MOVEMENT_SPEED = 20.0f
    private val TURN_SPEED = 2.5f
    private val GRAVITY = -9.8f
    private val GROUND_HEIGHT = 0f


    val state = State(
        Vector3f(0f,10f,0f),
        0f,
        Vector3f(0f,0f,0f)
    )

    fun update(
        deltaTime: Float,
        playerInput: PlayerInput
    ){
        state.velocity.y += GRAVITY * deltaTime
        state.position.add(Vector3f(state.velocity).mul(deltaTime))
        if(state.position.y < GROUND_HEIGHT){
            state.position.y = GROUND_HEIGHT
            state.velocity.y = 0f
        }

        if(playerInput.isTurnLeftPressed){
            state.rotationY += TURN_SPEED * deltaTime
        }
        if(playerInput.isTurnRightPressed){
            state.rotationY -= TURN_SPEED * deltaTime
        }
        state.velocity.x = 0f
        state.velocity.z = 0f

        if(playerInput.isMoveForwardPressed || playerInput.isMoveBackwardPressed){
            val directionX = sin(state.rotationY)
            val directionZ = cos(state.rotationY)

            if (playerInput.isMoveForwardPressed){
                state.velocity.x += directionX * MOVEMENT_SPEED
                state.velocity.z += directionZ * MOVEMENT_SPEED
            }
            if (playerInput.isMoveBackwardPressed){
                state.velocity.x -= directionX * MOVEMENT_SPEED
                state.velocity.z -= directionZ * MOVEMENT_SPEED
            }
        }

    }

}