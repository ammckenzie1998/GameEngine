package org.ammck.engine.physics

import org.ammck.engine.Transform
import org.joml.Vector3f

class PhysicsBody(
    val boundingBox: AxisAlignedBoundingBox,
    val isStatic: Boolean = false,
    val turnSpeed: Float = 2.5f,
    val accelerationFactor: Float = 100f,
    val dragFactor: Float = 0.99f
){

    val velocity: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
    val inverseMass: Float = if (isStatic) 0.0f else 1.0f
    var isGrounded: Boolean = false
}