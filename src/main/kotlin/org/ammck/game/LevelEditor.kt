package org.ammck.game

import org.ammck.engine.objects.GameObject
import org.ammck.engine.physics.Ray
import org.ammck.engine.physics.Raycaster
import org.ammck.engine.render.Mesh
import org.ammck.engine.render.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_BACK
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_CULL_FACE
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_FRONT
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glPolygonOffset

class LevelEditor(val gizmoShader: ShaderProgram, val axisMesh: Mesh) {

    var selectedObject: GameObject? = null
    var hoveredObject: GameObject? = null

    private val gizmoLength = 2.0f
    private val gizmoThickness = 0.1f

    private val xColor = Vector3f(1f, 0f, 0f)
    private val yColor = Vector3f(0f, 1f, 0f)
    private val zColor = Vector3f(0f, 0f, 1f)
    private val selectedColor = Vector3f(1f, 1f, 0f)

    private val selectedAlpha = 0.2f
    private val hoveredAlpha = 0.4f

    var mouseOverAxis = Axis.NONE

    fun selectObject(ray: Ray, worldObjects: List<GameObject>){
        selectedObject = getObjectUnderMouse(ray, worldObjects)
    }

    fun render(viewMatrix: Matrix4f, projectionMatrix: Matrix4f){
        if (selectedObject != null){
            renderGizmo(selectedObject!!, viewMatrix, projectionMatrix)
            renderOverlay(selectedObject!!, viewMatrix, projectionMatrix, selectedColor, selectedAlpha)
        }

        if (hoveredObject != null){
            renderOverlay(hoveredObject!!, viewMatrix, projectionMatrix, selectedColor, hoveredAlpha)
        }
    }

    fun hoverObjects(ray: Ray, worldObjects: List<GameObject>){
        val obj = getObjectUnderMouse(ray, worldObjects)
        hoveredObject = if (obj != selectedObject) obj  else null
    }

    fun renderOverlay(obj: GameObject, viewMatrix: Matrix4f, projectionMatrix: Matrix4f, color: Vector3f, alpha: Float){
        gizmoShader.bind()
        gizmoShader.setUniform("view", viewMatrix)
        gizmoShader.setUniform("projection", projectionMatrix)
        gizmoShader.setUniform("model", obj.globalMatrix)

        gizmoShader.setUniform("debugColor", color)
        gizmoShader.setUniform("alpha", alpha)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(false)
        glEnable(GL_POLYGON_OFFSET_FILL)
        glPolygonOffset(-1.0f, -1.0f)
        glDisable(GL_CULL_FACE)

        obj.model.mesh.draw()

        glEnable(GL_CULL_FACE)
        glDisable(GL_POLYGON_OFFSET_FILL)
        glDepthMask(true)
        glDisable(GL_BLEND)

        gizmoShader.unbind()
    }

    fun renderGizmo(obj: GameObject, viewMatrix: Matrix4f, projectionMatrix: Matrix4f){
        gizmoShader.bind()
        gizmoShader.setUniform("view", viewMatrix)
        gizmoShader.setUniform("projection", projectionMatrix)
        gizmoShader.setUniform("alpha", 1.0f)

        glDisable(GL_DEPTH_TEST)

        val pos = obj.transform.position

        val colX = if (mouseOverAxis == Axis.X) selectedColor else xColor
        val colY = if (mouseOverAxis == Axis.Y) selectedColor else yColor
        val colZ = if (mouseOverAxis == Axis.Z) selectedColor else zColor

        gizmoShader.setUniform("debugColor", colX)
        drawAxis(pos, Vector3f(gizmoLength, gizmoThickness, gizmoThickness), Vector3f(gizmoLength/2f, 0f, 0f))
        gizmoShader.setUniform("debugColor", colY)
        drawAxis(pos, Vector3f(gizmoThickness, gizmoLength, gizmoThickness), Vector3f(0f, gizmoLength/2f, 0f))
        gizmoShader.setUniform("debugColor", colZ)
        drawAxis(pos, Vector3f(gizmoThickness, gizmoThickness, gizmoLength), Vector3f(0f, 0f, gizmoLength/2f))

        glEnable(GL_DEPTH_TEST)
        gizmoShader.unbind()
    }

    private fun getObjectUnderMouse(ray: Ray, worldObjects: List<GameObject>): GameObject?{
        var closestDist = Float.MAX_VALUE
        var closestObj: GameObject? = null

        for (obj in worldObjects){
            val hit = Raycaster.castRay(ray, obj)
            if (hit != null && hit.distance < closestDist){
                closestDist = hit.distance
                closestObj = obj
            }
        }
        return closestObj
    }

    private fun drawAxis(center: Vector3f, scale: Vector3f, offset: Vector3f){
        val model = Matrix4f()
            .translate(center)
            .translate(offset)
            .scale(scale)

        gizmoShader.setUniform("model", model)
        axisMesh.draw()
    }

}

enum class Axis {
    NONE,
    X,
    Y,
    Z
}