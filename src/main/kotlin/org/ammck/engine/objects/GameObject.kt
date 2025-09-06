package org.ammck.engine.objects

import org.ammck.engine.Transform
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.render.Mesh
import org.joml.Matrix4f

class GameObject(
    val transform: Transform,
    val mesh: Mesh,
    val physicsBody: PhysicsBody?
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
            .rotateY(transform.rotationY)
            .rotateX(transform.rotationX)
            .scale(transform.scale)

        val parentGlobalMatrix = parent?.globalMatrix ?: Matrix4f().identity()
        globalMatrix.set(parentGlobalMatrix).mul(localMatrix)

        for(child in children){
            child.update()
        }
    }
}