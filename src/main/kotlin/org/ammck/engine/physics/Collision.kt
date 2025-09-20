package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject
import org.joml.Vector3f

data class Collision(
    val firstObject: GameObject,
    val secondObject: GameObject,
    val normal: Vector3f,
    val penetration: Float
){
    fun getCollisionPoint(): Vector3f{
        return Vector3f(firstObject.getPosition()).lerp(secondObject.getPosition(), 0.5f)
    }
}