package org.ammck.engine.physics

import org.joml.Vector3f

data class Suspension(
    val stiffness: Float = 100f,
    val damping: Float = 10f,
    val height: Float = 0.5f,
    val wheelPositions: List<Vector3f> = emptyList()
)
