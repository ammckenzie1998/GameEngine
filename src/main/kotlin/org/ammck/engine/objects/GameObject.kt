package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
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
){
    var parent: GameObject? = null
    var children = mutableListOf<GameObject>()

    val globalMatrix = Matrix4f()

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
        if (parent != null) {
            val parentPos = parent!!.globalMatrix.getTranslation(Vector3f())
            val parentRot = parent!!.globalMatrix.getNormalizedRotation(Quaternionf())
            val parentScale = parent!!.globalMatrix.getScale(Vector3f())

            val scaledLocalPos = Vector3f(transform.position).mul(parentScale)

            val worldPos = scaledLocalPos.rotate(parentRot).add(parentPos)
            val worldRot = Quaternionf(parentRot).mul(transform.orientation)
            val worldScale = Vector3f(transform.scale).mul(parentScale)

            globalMatrix.identity()
                .translate(worldPos)
                .rotate(worldRot)
                .scale(worldScale)
        } else {
            globalMatrix.identity()
                .translate(transform.position)
                .rotate(transform.orientation)
                .scale(transform.scale)
        }

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
}