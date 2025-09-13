package org.ammck.engine.physics

import org.joml.Vector3f

data class Ray(
    val origin: Vector3f = Vector3f(),
    val direction: Vector3f = Vector3f()
)