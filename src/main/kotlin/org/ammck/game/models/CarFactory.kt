package org.ammck.game.models

import org.ammck.Game
import org.ammck.engine.Transform
import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.ModelLoader
import org.ammck.engine.physics.AxisAlignedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.render.Mesh
import org.joml.Vector3f

object CarFactory {

    fun createPlayerCar(initialTransform: Transform, chassisMesh: Mesh, wheelMesh: Mesh): GameObject{
        val playerBoundingBox = AxisAlignedBoundingBox(initialTransform.position, Vector3f(2.0f, 1.0f, 1.0f))
        val playerBody = PhysicsBody(playerBoundingBox)
        val playerGameObject = GameObject(initialTransform, chassisMesh, playerBody)

        val flWheel = createWheelObject(Vector3f(-0.8f,-0.2f,2f), wheelMesh)
        val frWheel = createWheelObject(Vector3f(0.8f,-0.2f,2f), wheelMesh)
        val rlWheel = createWheelObject(Vector3f(-0.8f,-0.2f,-2f), wheelMesh)
        val rrWheel = createWheelObject(Vector3f(0.8f,-0.2f,-2f), wheelMesh)

        playerGameObject.addChildren(flWheel, frWheel, rlWheel, rrWheel)

        return playerGameObject
    }

    private fun createWheelObject(localPosition: Vector3f, wheelMesh: Mesh): GameObject{
        val transform = Transform(position = localPosition, scale = Vector3f(0.1f, 0.1f, 0.1f))
        val dummyBoundingBox = AxisAlignedBoundingBox(transform.position, Vector3f())
        val dummyBody = PhysicsBody(dummyBoundingBox, isStatic = true)
        return GameObject(transform, wheelMesh, dummyBody)
    }
}