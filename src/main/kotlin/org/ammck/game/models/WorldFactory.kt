package org.ammck.game.models

import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.ModelLoader
import org.ammck.engine.physics.AxisAlignedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.joml.Quaternionf
import org.joml.Vector3f

object WorldFactory {

    private const val TILE_LENGTH = 16.0f

    fun createStraightRoad(
        startingPosition: Vector3f,
        angle: Double = 0.0,
        length: Int = 1
    ): List<GameObject>{
        val roadObjects = mutableListOf<GameObject>()
        val roadMesh = AssetManager.getMesh("models/road_straight.ammodel")
        val angleRadians = Math.toRadians(angle).toFloat()
        val orientation = Quaternionf().rotateY(angleRadians)
        val direction = Vector3f(0f, 0f, -1f).rotate(orientation)

        for(i in 0 until length){
            val offset = Vector3f(direction).mul(TILE_LENGTH * i)
            val position = Vector3f(startingPosition).add(offset)

            val transform = Transform(
                position = position,
                orientation = orientation
            )
            val roadObject = GameObject(
                id = "road",
                transform = transform,
                mesh = roadMesh
            )
            roadObjects.add(roadObject)
        }

        return roadObjects
    }

}