package org.ammck.game.models

import org.ammck.engine.Transform
import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.ModelLoader
import org.ammck.engine.physics.AxisAlignedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.joml.Vector3f

object WorldFactory {

    fun createStaircaseRamp(
        position: Vector3f,
        dimensions: Vector3f = Vector3f(4.0f, 1.0f, 6.0f),
        stepCount: Int = 5
    ): List<GameObject>{
        val rampObjects = mutableListOf<GameObject>()

        val rampWidth = dimensions.x
        val rampHeight = dimensions.y
        val rampLength = dimensions.z

        val stepDepth = rampLength / stepCount
        val stepHeight = rampHeight / stepCount

        val stepMesh = ModelLoader.load("models/cube.ammodel")

        for(i in 0 until stepCount){
            val stepY = (i * stepHeight)
            val stepZ = (i * stepDepth)
            val stepPosition = Vector3f(position.x, position.y + stepY, position.z + stepZ)
            val stepSize = Vector3f(rampWidth, stepHeight, stepDepth)

            val transform = Transform(position = stepPosition, scale = stepSize)
            val boundingBox = AxisAlignedBoundingBox(transform.position, stepSize)
            val body = PhysicsBody(boundingBox, isStatic = true)
            val stepObject = GameObject(transform, stepMesh, body)
            rampObjects.add(stepObject)
        }

        return rampObjects
    }

}