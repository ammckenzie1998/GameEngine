package org.ammck.engine.physics

import org.joml.Vector3f

data class RaycastHit(
    val distance: Float,
    val point: Vector3f = Vector3f(),
    val normal: Vector3f = Vector3f()
)