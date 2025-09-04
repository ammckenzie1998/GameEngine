package org.ammck.engine

import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Matrix4f
import org.joml.Vector3f

class Camera (
    private val targetTransform: Transform,
    initialDistance: Float = 8.0f,
    initialHeight: Float = 1.5f) {

    var distanceFromTarget: Float = initialDistance
    var heightAboveTarget: Float = initialHeight
    val position = Vector3f(0.0f, 0.0f, 0.0f)

    private val viewMatrix = Matrix4f()
    private val upDirection = Vector3f(0.0f, 1.0f, 0.0f)

    fun update(){

        val targetPosition = targetTransform.position
        val targetRotationY = targetTransform.rotationY

        val horizontalDistance = distanceFromTarget * cos(targetRotationY)
        val verticalDistance = distanceFromTarget * sin(targetRotationY)

        position.x = targetPosition.x - verticalDistance
        position.y = targetPosition.y + heightAboveTarget
        position.z = targetPosition.z - horizontalDistance
    }

    fun getViewMatrix(): Matrix4f{
        return viewMatrix.identity().lookAt(
            position,
            targetTransform.position,
            upDirection
        )
    }
}