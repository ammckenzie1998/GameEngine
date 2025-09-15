package org.ammck.engine.camera

class CameraInput (
    val moveForward: Boolean = false,
    val moveBackward: Boolean = false,
    val moveLeft: Boolean = false,
    val moveRight: Boolean = false,
    val mouseDeltaX: Float = 0.0f,
    val mouseDeltaY: Float = 0.0f
)