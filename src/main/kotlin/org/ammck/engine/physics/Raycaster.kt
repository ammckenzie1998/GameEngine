package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject
import org.ammck.engine.render.Mesh
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

object Raycaster {

    private const val EPSILON = 0.000001f

    fun castRay(worldRay: Ray, obj: GameObject): RaycastHit?{
        var closestHit = Float.MAX_VALUE
        var intersectionResult: RaycastHit? = null
        val mesh = obj.model.mesh
        val worldToModelMatrix = obj.globalMatrix.invert(Matrix4f())

        val modelRayOrigin = worldToModelMatrix.transformPosition(Vector3f(worldRay.origin))
        val modelRayDirection = worldToModelMatrix.transformDirection(Vector3f(worldRay.direction)).normalize()
        val modelRay = Ray(modelRayOrigin, modelRayDirection)

        val triangleCount = mesh.vertexCount / 3

        for(i in 0 until triangleCount){
            val v0 = mesh.getVertexPosition(i*3)
            val v1 = mesh.getVertexPosition(i*3+1)
            val v2 = mesh.getVertexPosition(i*3+2)

            val edge1 = Vector3f(v1).sub(v0)
            val edge2 = Vector3f(v2).sub(v0)

            val rayCrossEdge = Vector3f(modelRay.direction).cross(edge2)
            val determinant = edge1.dot(rayCrossEdge)

            if(abs(determinant) < EPSILON) continue

            val inverseDeterminant = 1.0f / determinant
            val distToV0 = Vector3f(modelRay.origin).sub(v0)
            val barycentricCoordU = distToV0.dot(rayCrossEdge) * inverseDeterminant
            if(barycentricCoordU !in 0.0f..1.0f){
                continue
            }
            val distCrossEdge = Vector3f(distToV0).cross(edge1)
            val barycentricCoordV = modelRay.direction.dot(distCrossEdge) * inverseDeterminant
            if(barycentricCoordV < 0.0f || barycentricCoordU + barycentricCoordV > 1.0f){
                continue
            }

            val distToIntersectPoint = edge2.dot(distCrossEdge) * inverseDeterminant

            if(distToIntersectPoint > EPSILON){
                if (distToIntersectPoint < closestHit){
                    closestHit = distToIntersectPoint
                    val hitPointModel = Vector3f(modelRay.origin).add(Vector3f(modelRay.direction).mul(distToIntersectPoint))
                    val hitPointWorld = obj.globalMatrix.transformPosition(hitPointModel)

                    val normalMatrix = worldToModelMatrix.transpose(Matrix4f())
                    val faceNormal = Vector3f(edge1).cross(edge2)

                    val normalWorld = Vector3f()
                    normalMatrix.transformDirection(faceNormal, normalWorld)
                    normalWorld.normalize()

                    val worldDistance = worldRay.origin.distance(hitPointWorld)

                    intersectionResult = RaycastHit(worldDistance, hitPointWorld, normalWorld)
                }
            }
        }
        return intersectionResult
    }

    fun screenPointToRay(mouseX: Float, mouseY: Float, screenWidth: Int, screenHeight: Int, viewMatrix: Matrix4f, projecionMatrix: Matrix4f): Ray{
        val x = (2.0f * mouseX) / screenWidth - 1.0f
        val y = 1.0f - (2.0f * mouseY) / screenHeight
        val z = -1.0f

        val o = Vector4f(x, y, z, 1.0f)

        val inverseProjection = Matrix4f(projecionMatrix).invert()
        val rayPerspective = inverseProjection.transform(o)

        rayPerspective.z = -1.0f
        rayPerspective.w = 0.0f

        val inverseView = Matrix4f(viewMatrix).invert()
        val rayWorld = inverseView.transform(rayPerspective)
        val rayDirection = Vector3f(rayWorld.x, rayWorld.y, rayWorld.z).normalize()

        val rayOrigin = Vector3f()
        inverseView.getTranslation(rayOrigin)

        return Ray(rayOrigin, rayDirection)
    }
}