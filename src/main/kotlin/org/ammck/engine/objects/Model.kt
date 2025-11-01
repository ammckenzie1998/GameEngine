package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.render.Mesh

data class Model(
    val mesh: Mesh,
    val attachmentPoints: Map<AttachmentType, Transform>
)
