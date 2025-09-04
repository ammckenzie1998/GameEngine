package org.ammck.engine

import org.ammck.render.Mesh

class GameObject(
    val transform: Transform,
    val mesh: Mesh,
    val physicsBody: PhysicsBody
)