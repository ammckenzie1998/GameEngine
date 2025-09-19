package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.Suspension
import org.ammck.engine.render.Mesh
import org.ammck.games.Waypoint
import org.joml.Matrix4f
import org.joml.Vector3f

class GameObject(
    var id: String,
    val transform: Transform,
    var mesh: Mesh,
    var physicsBody: PhysicsBody? = null,
    val suspension: Suspension? = null,
    val waypoint: Waypoint? = null,
){
    var parent: GameObject? = null
    var children = mutableListOf<GameObject>()

    val localMatrix = Matrix4f()
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
        localMatrix.identity()
            .translate(transform.position)
            .rotate(transform.orientation)
            .scale(transform.scale)

        val parentGlobalMatrix = parent?.globalMatrix ?: Matrix4f().identity()
        globalMatrix.set(parentGlobalMatrix).mul(localMatrix)

        for(child in children){
            child.update()
        }
    }

    fun updateMesh(reloadedPaths: List<String>){
       mesh.resourcePath?.let { path ->
            if(reloadedPaths.contains(path)){
                this.mesh = AssetManager.getMesh(path)
            }
        }
        for (child in children){
            child.updateMesh(reloadedPaths)
        }
    }

    fun getPosition(): Vector3f {
        return this.transform.position
    }
}