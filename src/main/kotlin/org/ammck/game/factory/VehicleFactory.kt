package org.ammck.game.factory

import org.ammck.engine.Transform
import org.ammck.engine.objects.AttachmentType
import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.Model
import org.ammck.engine.physics.OrientedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.Suspension
import org.ammck.engine.render.Mesh
import org.ammck.game.components.Vehicle
import org.joml.Vector3f

object VehicleFactory {

    fun createVehicle(id: String, initialTransform: Transform, chassisModel: Model, wheelMesh: Mesh): Vehicle {
        val boundingBox = OrientedBoundingBox(initialTransform, Vector3f(2.0f, 1.5f, 4.0f))
        val body = PhysicsBody(boundingBox)

        val gameObject = GameObject(id, initialTransform, chassisModel.mesh, body)

        val wheelAttachmentPoints = chassisModel.attachmentPoints.filterKeys { it.name.startsWith("WHEEL") }
        val suspensionPositions = mutableListOf<Vector3f>()
        for(ap in wheelAttachmentPoints){
            val wheelTransform = Transform(
                position = ap.value.position.mul(0.1f, 0.1f, 0.1f),
                orientation = ap.value.orientation,
                scale = ap.value.scale
            )
            println(wheelTransform)
            val wheelObject = GameObject(ap.key.name, wheelTransform, wheelMesh)
            suspensionPositions.add(ap.value.position)
            gameObject.addChild(wheelObject)
        }

        val suspension = Suspension(
            wheelPositions = suspensionPositions,
            stiffness = 150f,
            damping = 15f,
            height = 0.5f
        )
        gameObject.suspension = suspension

        return Vehicle(gameObject)
    }
}