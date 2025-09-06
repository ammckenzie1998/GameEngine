package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject

data class PhysicsStateReport(
    val objectReport: MutableMap<PhysicsState, MutableList<GameObject>> = mutableMapOf()
)

enum class PhysicsState{
    OBJECT_RESPAWN
}
