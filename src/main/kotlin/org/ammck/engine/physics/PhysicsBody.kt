package org.ammck.engine.physics

import org.joml.Vector3f

class PhysicsBody(
    val boundingBox: OrientedBoundingBox,
    val isStatic: Boolean = false,
    val turnSpeed: Float = 2.5f,
    val accelerationFactor: Float = 100f,
    val dragFactor: Float = 0.99f,
    val isRamp: Boolean = false
){

    val velocity: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
    val forces: Vector3f = Vector3f(0.0f,0.0f,0.0f)
    val inverseMass: Float = if (isStatic) 0.0f else 1.0f
    var groundNormal: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)

    var isGrounded: Boolean = false
    var isOnRamp: Boolean = false
    var isRespawning: Boolean = false
}