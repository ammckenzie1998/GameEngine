package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject

data class PhysicsStateReport(
    val objectReport: MutableMap<GameObject, MutableList<PhysicsState>> = mutableMapOf()
)

enum class PhysicsState{
    OBJECT_RESPAWN,
    RAMP_CONTACT
}
