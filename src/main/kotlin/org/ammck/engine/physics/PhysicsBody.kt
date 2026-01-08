package org.ammck.engine.physics

import org.joml.Vector3f

class PhysicsBody(
    val boundingBox: OrientedBoundingBox,
    val isStatic: Boolean = false,
    val turnSpeed: Float = 2.5f,
    val accelerationFactor: Float = 100f,
    val linearDrag: Float = 0.99f,
    val angularDrag: Float = 0.98f
){

    var velocity: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
    val forces: Vector3f = Vector3f(0.0f,0.0f,0.0f)
    val torques: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)

    val inverseMass: Float = if (isStatic) 0.0f else 1.0f

    val angularVelocity: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
    val inverseInertia: Float = if(isStatic) 0.0f
    else{
        val size = boundingBox.size
        val inertia = (1.0f / 12.0f) * (size.y * size.y + size.z * size.z)
        if(inertia > 0) 1.0f else 0.0f
    }

    var groundNormal: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)

    var isGrounded: Boolean = false
    var isOnRamp: Boolean = false
    var isRespawning: Boolean = false
    
    var lastImpactImpulse: Float = 0.0f
}