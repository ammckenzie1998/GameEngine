package org.ammck.engine.camera

import org.ammck.engine.Transform
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class FreeFlyCamera(initialPosition: Vector3f) {

    private val moveSpeed = 50.0f
    private val mouseSensitivity = 0.1f

    val transform = Transform(position = initialPosition)

    private val viewMatrix = Matrix4f()
    private val upDirection = Vector3f(0.0f, 1.0f, 0.0f)

    fun update(deltaTime: Float, cameraInput: CameraInput){
        var rightDirection = Vector3f(1f, 0f, 0f).rotate(transform.orientation)
        val yawRotation = Quaternionf().rotateY(Math.toRadians((-cameraInput.mouseDeltaX * mouseSensitivity.toDouble())).toFloat())
        val pitchRotation = Quaternionf()
            .rotateAxis(Math.toRadians((-cameraInput.mouseDeltaY * mouseSensitivity.toDouble())).toFloat(), rightDirection)

        transform.orientation.premul(yawRotation).mul(pitchRotation).normalize()

        val velocity = moveSpeed * deltaTime
        val forwardDirection = Vector3f(0f, 0f, -1f).rotate(transform.orientation)
        rightDirection = Vector3f(1f, 0f, 0f).rotate(transform.orientation)

        if (cameraInput.moveForward) transform.position.add(Vector3f(forwardDirection).mul(velocity))
        if (cameraInput.moveBackward) transform.position.sub(Vector3f(forwardDirection).mul(velocity))
        if (cameraInput.moveRight) transform.position.add(Vector3f(rightDirection).mul(velocity))
        if (cameraInput.moveLeft) transform.position.sub(Vector3f(rightDirection).mul(velocity))
    }

    fun getViewMatrix(): Matrix4f {
        val forwardDirection = Vector3f(0f, 0f, -1f).rotate(transform.orientation)
        val target = Vector3f(transform.position).add(forwardDirection)
        return viewMatrix.identity().lookAt(
            transform.position,
            target,
            upDirection
        )
    }

}