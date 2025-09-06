package org.ammck.engine

import org.joml.Vector3f

data class Transform (
    val position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var rotationY: Float = 0.0f,
    var rotationX: Float = 0.0f,
    val scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f)
)