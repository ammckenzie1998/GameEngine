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
import org.ammck.game.components.Weapon
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.NVMemoryAttachment
import kotlin.math.abs

object VehicleFactory {

    fun createVehicle(id: String, initialTransform: Transform, chassisModel: Model, wheelMesh: Mesh): Vehicle {
        val width = chassisModel.upperBounds.x + abs(chassisModel.lowerBounds.x)
        val height = chassisModel.upperBounds.y + abs(chassisModel.lowerBounds.y)
        val length = chassisModel.upperBounds.z + abs(chassisModel.lowerBounds.z)
        val boundingBox = OrientedBoundingBox(initialTransform, Vector3f(width, height, length))
        val body = PhysicsBody(boundingBox)

        val gameObject = GameObject(id, initialTransform, chassisModel, body)

        val wheelAttachmentPoints = chassisModel.attachmentPoints!!.filterKeys { it.name.startsWith("WHEEL") }
        val suspensionPositions = mutableListOf<Vector3f>()
        for(ap in wheelAttachmentPoints){
            val wheelTransform = Transform(
                position = Vector3f(ap.value.position),
                orientation = Quaternionf(ap.value.orientation),
                scale = Vector3f(ap.value.scale)
            )
            val wheelObject = GameObject(ap.key.name, wheelTransform, Model(wheelMesh, null))
            suspensionPositions.add(ap.value.position)
            gameObject.addChild(wheelObject)
        }

        val suspension = Suspension(
            wheelPositions = suspensionPositions,
            stiffness = 150f,
            damping = 15f,
            height = 0.3f
        )
        gameObject.suspension = suspension

        return Vehicle(gameObject)
    }

    fun attachWeapon(vehicle: Vehicle, weapon: Weapon, attachmentType: AttachmentType){
        if(vehicle.gameObject.model.attachmentPoints != null) {
            val apTransform = vehicle.gameObject.model.attachmentPoints!!.get(attachmentType)
            val weaponObject = GameObject(attachmentType.toString(), apTransform!!, weapon.model)
            weaponObject.weapon = weapon
            vehicle.gameObject.addChild(weaponObject)
        }
    }
}