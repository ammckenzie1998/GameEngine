package org.ammck.engine.camera

import org.ammck.engine.Transform
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class Camera (
    private val targetTransform: Transform,
    var distance: Float = 8.0f,
    var height: Float = 1.5f,
    var smoothFactor: Float = 4.0f) {

    val position = Vector3f(0.0f, 0.0f, 0.0f)

    private val viewMatrix = Matrix4f()
    private val upDirection = Vector3f(0.0f, 1.0f, 0.0f)
    private val desiredPosition = Vector3f()

    private var targetOrientation = Quaternionf()

    fun update(deltaTime: Float, isTargetGrounded: Boolean){
        calculateDesiredPosition(isTargetGrounded)
        position.lerp(desiredPosition, smoothFactor * deltaTime)
    }

    private fun calculateDesiredPosition(isTargetGounded: Boolean){
        val targetPosition = targetTransform.position
        if(isTargetGounded){
            targetOrientation.set(targetTransform.orientation)
        }
        val forwardDirection = Vector3f(0f, 0f, -1f).rotate(targetOrientation)

        desiredPosition.set(targetPosition)
            .sub(forwardDirection.mul(distance))
            .add(0f, height, 0f)
    }

    fun reset(){
        calculateDesiredPosition(true)
        position.set(desiredPosition)
    }

    fun getViewMatrix(): Matrix4f {
        return viewMatrix.identity().lookAt(
            position,
            targetTransform.position,
            upDirection
        )
    }
}