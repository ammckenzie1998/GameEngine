package org.ammck.game.editor

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
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glPolygonOffset
import kotlin.math.roundToInt

class LevelEditor(val gizmoShader: ShaderProgram, val axisMesh: Mesh) {

    var selectedObject: GameObject? = null
    var hoveredObject: GameObject? = null

    private val gizmo = Gizmo(axisMesh)
    var hoveredAxis = Axis.NONE

    private val selectedColor = Vector3f(1f, 1f, 0f)

    private val selectedAlpha = 0.2f
    private val hoveredAlpha = 0.4f

    var isDragging = false
    private var dragStartPos = Vector3f()
    private var objectStartPos = Vector3f()

    var isGridSnapEnabled = true
    var gridSize = 1.0f

    fun update(ray: Ray, gameObjects: List<GameObject>){
        if (selectedObject != null){
            gizmo.setPosition(selectedObject!!.transform.position)

            if (!isDragging){
                gizmo.checkHover(ray)
            }
        }

        if (gizmo.hoveredAxis == Axis.NONE && !isDragging){
            val obj = getObjectUnderMouse(ray, gameObjects)
            hoveredObject = if (obj != selectedObject) obj else null
        } else{
            hoveredObject = null
        }

        if(isDragging){
            updateDrag(ray)
        }
    }

    fun selectObject(ray: Ray, gameObjects: List<GameObject>){
        if (gizmo.hoveredAxis != Axis.NONE){
            startDrag(ray)
        } else{
            selectedObject = getObjectUnderMouse(ray, gameObjects)
        }

    }

    fun render(viewMatrix: Matrix4f, projectionMatrix: Matrix4f){
        if (selectedObject != null){
            renderOverlay(selectedObject!!, viewMatrix, projectionMatrix, selectedColor, selectedAlpha)
            gizmo.render(gizmoShader, viewMatrix, projectionMatrix)
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
        glCullFace(GL_BACK)
        glDisable(GL_POLYGON_OFFSET_FILL)
        glDepthMask(true)
        glDisable(GL_BLEND)

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

    private fun startDrag(ray: Ray){
        if(selectedObject != null){
            isDragging = true
            objectStartPos.set(selectedObject!!.transform.position)
            dragStartPos = getNearestPointOnRay(ray, objectStartPos)
        }
    }

    private fun updateDrag(ray: Ray){
        val axis = gizmo.hoveredAxis

        val currentRayPoint = getNearestPointOnRay(ray, objectStartPos)
        val rawDelta = Vector3f(currentRayPoint).sub(dragStartPos)
        val targetPos = Vector3f(objectStartPos).add(rawDelta)

        if (isGridSnapEnabled){
            when (axis) {
                Axis.X -> targetPos.x = (targetPos.x / gridSize).roundToInt() * gridSize
                Axis.Y -> targetPos.y = (targetPos.y / gridSize).roundToInt() * gridSize
                Axis.Z -> targetPos.z = (targetPos.z / gridSize).roundToInt() * gridSize
                else -> {}
            }
        }

        when (axis){
            Axis.X -> {
                targetPos.y = objectStartPos.y
                targetPos.z = objectStartPos.z
            }
            Axis.Y -> {
                targetPos.x = objectStartPos.x
                targetPos.z = objectStartPos.z
            }
            Axis.Z -> {
                targetPos.x = objectStartPos.x
                targetPos.y = objectStartPos.y
            }
            else -> {}
        }

        if(selectedObject != null) {
            selectedObject!!.transform.position.set(targetPos)
            selectedObject!!.physicsBody?.boundingBox?.transform?.position?.set(targetPos)
        }
        gizmo.setPosition(targetPos)
    }

    fun endDrag(){
        isDragging = false
    }

    private fun getNearestPointOnRay(ray: Ray, point: Vector3f): Vector3f{
        val pointToOrigin = Vector3f(point).sub(ray.origin)
        val projection = pointToOrigin.dot(ray.direction)
        return Vector3f(ray.direction).mul(projection).add(ray.origin)
    }

}

enum class Axis {
    NONE,
    X,
    Y,
    Z
}