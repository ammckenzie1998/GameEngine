package org.ammck.engine.physics

import org.ammck.engine.Transform
import org.joml.Math.min
import org.joml.Matrix3f
import org.joml.Vector3f
import java.lang.Float.max

class OrientedBoundingBox(val transform: Transform, val size: Vector3f) {

    fun getCollisionResponse(other: OrientedBoundingBox): Vector3f? {
        val projectionAxes = getProjectionAxes(other)
        var minimumOverlap = Float.MAX_VALUE
        var mtvAxis: Vector3f? = null

        for (axis in projectionAxes) {
            val p1 = project(axis)
            val p2 = other.project(axis)
            if (p1.max < p2.min || p2.max < p1.min) {
                return null
            }

            val overlap = min(p1.max, p2.max) - max(p1.min, p2.min)
            if (overlap < minimumOverlap) {
                minimumOverlap = overlap
                mtvAxis = axis
            }
        }
        if (mtvAxis == null) return null

        val direction = Vector3f(other.transform.position).sub(this.transform.position)
        if (direction.dot(mtvAxis) < 0) {
            mtvAxis.negate()
        }

        return Vector3f(mtvAxis).normalize().mul(minimumOverlap)
    }

    private fun getProjectionAxes(other: OrientedBoundingBox): List<Vector3f> {
        val axes = mutableListOf<Vector3f>()
        val thisRotation = getRotation(this.transform)
        val otherRotation = getRotation(other.transform)
        val thisAxes = getAxes(thisRotation)
        val otherAxes = getAxes(otherRotation)

        axes.addAll(thisAxes)
        axes.addAll(otherAxes)

        for (t in thisAxes) {
            for (o in otherAxes) {
                val crossProduct = Vector3f(t).cross(o)
                if (crossProduct.lengthSquared() > 0.000001f) { // Avoid parallel axes
                    axes.add(crossProduct.normalize())
                }
            }
        }
        return axes
    }

    private fun project(axis: Vector3f): Projection {
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        val corners = getCorners()

        for (corner in corners) {
            val dotProduct = corner.dot(axis)
            min = minOf(min, dotProduct)
            max = maxOf(max, dotProduct)
        }
        return Projection(min, max)
    }

    private fun getCorners(): Array<Vector3f> {
        val corners = Array(8) { Vector3f() }
        val center = transform.position
        val halfSize = Vector3f(size).div(2.0f)

        val rotation = getRotation(this.transform)
        val axes = getAxes(rotation)

        val dx = Vector3f(axes[0]).mul(halfSize.x)
        val dy = Vector3f(axes[1]).mul(halfSize.y)
        val dz = Vector3f(axes[2]).mul(halfSize.z)

        corners[0].set(center).add(dx).add(dy).add(dz)
        corners[1].set(center).add(dx).add(dy).sub(dz)
        corners[2].set(center).add(dx).sub(dy).add(dz)
        corners[3].set(center).add(dx).sub(dy).sub(dz)
        corners[4].set(center).sub(dx).add(dy).add(dz)
        corners[5].set(center).sub(dx).add(dy).sub(dz)
        corners[6].set(center).sub(dx).sub(dy).add(dz)
        corners[7].set(center).sub(dx).sub(dy).sub(dz)

        return corners
    }

    private fun getRotation(transform: Transform): Matrix3f {
        return Matrix3f().set(transform.orientation)
    }

    private fun getAxes(rotation: Matrix3f): Array<Vector3f> {
        return arrayOf(
            Vector3f(1f, 0f, 0f).mul(rotation),
            Vector3f(0f, 1f, 0f).mul(rotation),
            Vector3f(0f, 0f, 1f).mul(rotation)
        )
    }
}

private data class Projection(val min: Float, val max: Float)