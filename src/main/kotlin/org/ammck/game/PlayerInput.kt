package org.ammck.game

data class PlayerInput(
    val isMoveForwardPressed: Boolean,
    val isMoveBackwardPressed: Boolean,
    val isTurnLeftPressed: Boolean,
    val isTurnRightPressed: Boolean,
    val isPitchPressed: Boolean,
    val isFirePressed: Boolean
)