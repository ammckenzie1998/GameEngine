package org.ammck.game.factory

import org.ammck.engine.Transform
import org.ammck.engine.objects.GameObject
import org.ammck.engine.physics.AxisAlignedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.Suspension
import org.ammck.engine.render.Mesh
import org.ammck.game.Vehicle
import org.joml.Vector3f

object VehicleFactory {

    fun createVehicle(initialTransform: Transform, chassisMesh: Mesh, wheelMesh: Mesh): Vehicle {
        val boundingBox = AxisAlignedBoundingBox(initialTransform.position, Vector3f(2.0f, 1.0f, 1.0f))
        val body = PhysicsBody(boundingBox)

        val flWheel = createWheelObject(Vector3f(-0.8f,-0.2f,1.5f), wheelMesh)
        val frWheel = createWheelObject(Vector3f(0.8f,-0.2f,1.5f), wheelMesh)
        val rlWheel = createWheelObject(Vector3f(-0.8f,-0.2f,-1.5f), wheelMesh)
        val rrWheel = createWheelObject(Vector3f(0.8f,-0.2f,-1.5f), wheelMesh)

        val suspension = Suspension(
            wheelPositions = listOf(
                frWheel.transform.position,
                flWheel.transform.position,
                rrWheel.transform.position,
                rlWheel.transform.position
            ),
            stiffness = 150f,
            damping = 15f,
            height = 0.5f
        )

        val gameObject = GameObject("placeholder", initialTransform, chassisMesh, body, suspension)

        gameObject.addChildren(frWheel, flWheel, rrWheel, rlWheel)

        return Vehicle(gameObject)
    }

    private fun createWheelObject(localPosition: Vector3f, wheelMesh: Mesh): GameObject{
        val transform = Transform(position = localPosition, scale = Vector3f(0.1f, 0.1f, 0.1f))
        val dummyBoundingBox = AxisAlignedBoundingBox(transform.position, Vector3f())
        val dummyBody = PhysicsBody(dummyBoundingBox, isStatic = true)
        return GameObject("Wheel", transform, wheelMesh, dummyBody)
    }
}