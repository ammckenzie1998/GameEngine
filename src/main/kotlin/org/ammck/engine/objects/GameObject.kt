package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.Suspension
import org.ammck.engine.render.Mesh
import org.ammck.games.Waypoint
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class GameObject(
    var id: String,
    val transform: Transform,
    var mesh: Mesh,
    var physicsBody: PhysicsBody? = null,
    var suspension: Suspension? = null,
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
        if (parent != null) {
            // --- THE DEFINITIVE FIX: Decompose and Recompose for Stable Hierarchy ---
            // 1. Decompose the parent's final global matrix into its core components.
            val parentPos = parent!!.globalMatrix.getTranslation(Vector3f())
            val parentRot = parent!!.globalMatrix.getNormalizedRotation(Quaternionf())
            val parentScale = parent!!.globalMatrix.getScale(Vector3f())

            // 2. Calculate this object's final world position.
            //    We apply our local position, rotated by the parent's orientation, to the parent's position.
            //    This calculation is NOT affected by the parent's scale.
            val worldPos = Vector3f(transform.position).rotate(parentRot).add(parentPos)

            // 3. Calculate this object's final world orientation.
            val worldRot = Quaternionf(parentRot).mul(transform.orientation)

            // 4. Calculate this object's final world scale.
            val worldScale = Vector3f(parentScale).mul(transform.scale)

            // 5. Recompose the final matrix from these correct, stable components.
            globalMatrix.identity().translate(worldPos).rotate(worldRot).scale(worldScale)
            // --- END OF FIX ---
        } else {
            // Root objects are simple.
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
       mesh.resourcePath?.let { path ->
            if(reloadedPaths.contains(path)){
                this.mesh = AssetManager.getMesh(path).mesh
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