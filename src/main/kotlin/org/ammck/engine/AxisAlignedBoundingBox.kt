package org.ammck.engine

import org.joml.Math.abs
import org.joml.Vector3f

class AxisAlignedBoundingBox(var center: Vector3f, val size: Vector3f) {

    val halfSize = Vector3f(size).div(2.0f)

    fun getCollisionResponse(other: AxisAlignedBoundingBox): Vector3f?{
        val distance = Vector3f(other.center).sub(this.center)
        val combinedHalfSizes = Vector3f(this.halfSize).add(other.halfSize)

        val overlapX = combinedHalfSizes.x - abs(distance.x)
        val overlapY = combinedHalfSizes.y - abs(distance.y)
        val overlapZ = combinedHalfSizes.z - abs(distance.z)

        if (overlapX >0 && overlapY > 0 && overlapZ > 0){
            return when{
                (overlapX < overlapY && overlapX < overlapZ) -> {
                    val pushDirection = if(distance.x < 0) -1.0f else 1.0f
                    Vector3f(overlapX * pushDirection, 0f, 0f)
                }
                (overlapY < overlapZ) -> {
                    val pushDirection = if(distance.y < 0) -1.0f else 1.0f
                    Vector3f(0f, overlapY * pushDirection, 0f)
                }
                else -> {
                    val pushDirection = if(distance.z < 0) -1.0f else 1.0f
                    Vector3f(0f, 0f, overlapZ * pushDirection)
                }
            }
        }
        return null
    }
}