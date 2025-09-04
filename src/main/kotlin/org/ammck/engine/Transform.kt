package org.ammck.engine

import org.joml.Vector3f

data class Transform (
    val position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var rotationY: Float = 0.0f,
)