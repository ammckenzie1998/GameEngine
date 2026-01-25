package org.ammck.engine

import org.joml.Quaternionf
import org.joml.Vector3f

data class Transform (
    val position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    val orientation: Quaternionf = Quaternionf(),
    val scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f)
){
    fun forwardDirection(): Vector3f{
        return Vector3f(0f, 0f, -1f).rotate(this.orientation)
    }

    fun copy(): Transform{
        return Transform(
            Vector3f(position),
            Quaternionf(orientation),
            Vector3f(scale)
        )
    }
}