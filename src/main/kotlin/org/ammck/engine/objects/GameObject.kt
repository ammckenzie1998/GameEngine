package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.physics.OrientedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.Suspension
import org.ammck.engine.render.Mesh
import org.ammck.game.components.Weapon
import org.ammck.games.Waypoint
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class GameObject(
    var id: String,
    val transform: Transform,
    var model: Model,
    var physicsBody: PhysicsBody? = null,
    var suspension: Suspension? = null,
    val waypoint: Waypoint? = null,
    var weapon: Weapon? = null,
    val respawnable: Boolean = true,
    val isSolid: Boolean = true,
    val renderPriority: Int = 0
){
    var parent: GameObject? = null
    var children = mutableListOf<GameObject>()

    val baseMatrix = Matrix4f()
    val globalMatrix = Matrix4f()
    val visualOrientation = Quaternionf()
    var attachVisualsToParent: Boolean = true

    fun addChild(child: GameObject){
        child.parent = this
        children.add(child)
    }

    fun addChildren(vararg children: GameObject){
        for(child in children){
            addChild(child)
        }
    }

    fun update(){

        var parentMatrix = Matrix4f()

        if (parent != null) {
            parentMatrix =
                if (attachVisualsToParent) parent!!.globalMatrix
                else parent!!.baseMatrix
        }


        val parentPos = parentMatrix.getTranslation(Vector3f())
        val parentRot = parentMatrix.getNormalizedRotation(Quaternionf())
        val parentScale = parentMatrix.getScale(Vector3f())

        val scaledLocalPos = Vector3f(transform.position).mul(parentScale)
        val worldPos = scaledLocalPos.rotate(parentRot).add(parentPos)
        val worldRot = Quaternionf(parentRot).mul(transform.orientation)
        val worldScale = Vector3f(transform.scale).mul(parentScale)

        baseMatrix.identity()
            .translate(worldPos)
            .rotate(worldRot)
            .scale(worldScale)

        globalMatrix.set(baseMatrix)
        globalMatrix.rotate(visualOrientation)

        for(child in children){
            child.update()
        }

    }

    fun updateMesh(reloadedPaths: List<String>){
       model.mesh.resourcePath?.let { path ->
            if(reloadedPaths.contains(path)){
                this.model.mesh = AssetManager.getMesh(path).mesh
            }
        }
        for (child in children){
            child.updateMesh(reloadedPaths)
        }
    }

    fun getPosition(): Vector3f {
        return this.transform.position
    }

    fun getWorldTransform(): Transform{
        return Transform(
            globalMatrix.getTranslation(Vector3f()),
            globalMatrix.getUnnormalizedRotation(Quaternionf()),
            globalMatrix.getScale(Vector3f())
        )
    }

    fun copy(id: String): GameObject{
        val newTransform = transform.copy()

        val newBody = physicsBody?.let { oldBody ->
            val newBounds = OrientedBoundingBox(newTransform, Vector3f(oldBody.boundingBox.size).div(transform.scale))
            PhysicsBody(newBounds, oldBody.isStatic)
        }

        val newObject = GameObject(
            id,
            newTransform,
            model,
            newBody,
            isSolid = isSolid,
            renderPriority = renderPriority
        )

        return newObject
    }
}