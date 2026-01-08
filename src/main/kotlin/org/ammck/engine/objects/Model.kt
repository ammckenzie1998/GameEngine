package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.render.Mesh
import org.joml.Vector3f

data class Model(
    var mesh: Mesh,
    val attachmentPoints: Map<AttachmentType, Transform>?,
    val texturePath: String? = null,
    val upperBounds: Vector3f = Vector3f(0f, 0f, 0f),
    val lowerBounds: Vector3f = Vector3f(0f, 0f, 0f)
)
