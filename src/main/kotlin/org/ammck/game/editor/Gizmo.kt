package org.ammck.game.editor

import org.ammck.engine.Transform
import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.Model
import org.ammck.engine.physics.Ray
import org.ammck.engine.physics.Raycaster
import org.ammck.engine.render.Mesh
import org.ammck.engine.render.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_CULL_FACE
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_FILL
import org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glPolygonMode

class Gizmo(val mesh: Mesh) {

    private val gizmoX: GameObject
    private val gizmoY: GameObject
    private val gizmoZ: GameObject

    val GIZMO_OFFSET = 0.5f

    var hoveredAxis = Axis.NONE

    private val activeColor = Vector3f(1f, 1f, 0f)
    private val inactiveColors = mapOf(
        Axis.X to Vector3f(1f, 0f, 0f),
        Axis.Y to Vector3f(0f, 1f, 0f),
        Axis.Z to Vector3f(0f, 0f, 1f)
    )

    init {
        val axisModel = Model(mesh, null)

        val transformX = Transform(scale = Vector3f(2.0f, 0.2f, 0.2f))
        val transformY = Transform(scale = Vector3f(0.2f, 2.0f, 0.2f))
        val transformZ = Transform(scale = Vector3f(0.2f, 0.2f, 2.0f))

        gizmoX = GameObject("GIZMO_X", transformX, axisModel)
        gizmoY = GameObject("GIZMO_Y", transformY, axisModel)
        gizmoZ = GameObject("GIZMO_Z", transformZ, axisModel)
    }

    fun setPosition(position: Vector3f){
        gizmoX.transform.position.set(position).add(GIZMO_OFFSET, 0f, 0f)
        gizmoY.transform.position.set(position).add(0f, GIZMO_OFFSET, 0f)
        gizmoZ.transform.position.set(position).add(0f, 0f,GIZMO_OFFSET)

        gizmoX.update()
        gizmoY.update()
        gizmoZ.update()
    }

    fun checkHover(ray: Ray){
        hoveredAxis = Axis.NONE

        if (Raycaster.castRay(ray, gizmoX) != null) hoveredAxis = Axis.X
        else if (Raycaster.castRay(ray, gizmoY) != null) hoveredAxis = Axis.Y
        else if (Raycaster.castRay(ray, gizmoZ) != null) hoveredAxis = Axis.Z
    }

    fun render(shader: ShaderProgram, view: Matrix4f, projection: Matrix4f){
        shader.bind()
        shader.setUniform("view", view)
        shader.setUniform("projection", projection)
        shader.setUniform("alpha", 1.0f)

        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
        glClear(GL_DEPTH_BUFFER_BIT)
        glDisable(GL_CULL_FACE)

        renderAxis(shader, gizmoX, Axis.X)
        renderAxis(shader, gizmoY, Axis.Y)
        renderAxis(shader, gizmoZ, Axis.Z)

        glEnable(GL_CULL_FACE)

        shader.unbind()
    }

    private fun renderAxis(shader: ShaderProgram, axisObject: GameObject, axis: Axis){
        val color = if (hoveredAxis == axis) activeColor else inactiveColors[axis]
        shader.setUniform("debugColor", color!!)
        shader.setUniform("model", axisObject.globalMatrix)
        axisObject.model.mesh.draw()
    }
}
