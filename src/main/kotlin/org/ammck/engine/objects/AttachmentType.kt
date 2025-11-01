package org.ammck.engine.objects

import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentType{
    HOOD_MOUNT,
    ROOF_MOUNT,
    WHEEL_FL,
    WHEEL_FR,
    WHEEL_RL,
    WHEEL_RR
}