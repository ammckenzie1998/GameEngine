package org.ammck.engine

import org.joml.Vector3f

data class Collision(
    val firstBody: PhysicsBody,
    val secondBody: PhysicsBody,
    val normal: Vector3f,
    val penetration: Float
)