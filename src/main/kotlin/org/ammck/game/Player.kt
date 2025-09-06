package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.joml.Math.cos
import org.joml.Math.sin

class Player (private val gameObject: GameObject) {

    private val ACCELERATION = 100.0f
    private val DRAG = 0.99f
    private val TURN_SPEED = 2.5f


    fun update(
        deltaTime: Float,
        playerInput: PlayerInput
    ){
        val physicsBody = gameObject.physicsBody!!
        //Apply turning
        if(playerInput.isTurnLeftPressed){
            gameObject.transform.rotationY += TURN_SPEED * deltaTime
        }
        if(playerInput.isTurnRightPressed){
            gameObject.transform.rotationY -= TURN_SPEED * deltaTime
        }

        //Apply acceleration
        if(playerInput.isMoveForwardPressed || playerInput.isMoveBackwardPressed){
            val directionX = sin(gameObject.transform.rotationY)
            val directionZ = cos(gameObject.transform.rotationY)

            if (playerInput.isMoveForwardPressed){
                physicsBody.velocity.x += directionX * ACCELERATION * deltaTime
                physicsBody.velocity.z += directionZ * ACCELERATION * deltaTime
            }
            if (playerInput.isMoveBackwardPressed){
                physicsBody.velocity.x -= directionX * ACCELERATION * deltaTime
                physicsBody.velocity.z -= directionZ * ACCELERATION * deltaTime
            }
        }

        if (physicsBody.velocity.x != 0.0f) physicsBody.velocity.x *= DRAG
        if (physicsBody.velocity.z != 0.0f) physicsBody.velocity.z *= DRAG

    }

}