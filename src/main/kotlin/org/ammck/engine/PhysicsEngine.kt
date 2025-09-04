package org.ammck.engine

import org.joml.Vector3f

class PhysicsEngine {

    private val GRAVITY = -9.8f
    private val bodies = mutableListOf<PhysicsBody>()
    private val collisions = mutableListOf<Collision>()

    fun addBodies(vararg newBodies: PhysicsBody) {
        bodies.addAll(newBodies)
    }

    fun update(deltaTime: Float) {
        //1. Gravity
        for (body in bodies) {
            if (!body.isStatic) {
                body.velocity.y += GRAVITY * deltaTime
                body.transform.position.add(Vector3f(body.velocity).mul(deltaTime))
            }
        }

        //2. Collisions
        collisions.clear()
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val bodyA = bodies[i]
                val bodyB = bodies[j]

                if (bodyA.isStatic && bodyB.isStatic) continue

                bodyA.boundingBox.center.set(bodyA.transform.position)
                bodyB.boundingBox.center.set(bodyB.transform.position)

                val mtv = bodyA.boundingBox.getCollisionResponse(bodyB.boundingBox)
                if (mtv != null) {
                    val penetration = mtv.length()
                    val normal = Vector3f(mtv).normalize()
                    collisions.add(Collision(bodyA, bodyB, normal, penetration))
                }
            }
        }

        for (collision in collisions) {
            resolveCollision(collision)
        }

    }

    private fun resolveCollision(collision: Collision) {
        val bodyA = collision.firstBody
        val bodyB = collision.secondBody
        val normal = collision.normal

        // --- 1. Calculate Relative Velocity ---
        val relativeVelocity = Vector3f(bodyB.velocity).sub(bodyA.velocity)
        val velocityAlongNormal = relativeVelocity.dot(normal)
        if (velocityAlongNormal > 0) return

        // --- 2. Calculate and Apply Impulse (Velocity Correction) ---
        val e = 0.0f // Bounciness
        var j = -(1 + e) * velocityAlongNormal
        j /= (bodyA.inverseMass + bodyB.inverseMass)
        val impulse = Vector3f(normal).mul(j)
        if (!bodyA.isStatic) bodyA.velocity.sub(Vector3f(impulse).mul(bodyA.inverseMass))
        if (!bodyB.isStatic) bodyB.velocity.add(Vector3f(impulse).mul(bodyB.inverseMass))

        // --- 3. Positional Correction (The Jitter and Floating Fix) ---
        // We apply the push-out directly, scaled by inverse mass. No percentages.
        val totalInverseMass = bodyA.inverseMass + bodyB.inverseMass
        if (totalInverseMass <= 0) return // Both objects are static/immovable

        val correction = Vector3f(normal).mul(collision.penetration / totalInverseMass)
        if (!bodyA.isStatic) {
            bodyA.transform.position.sub(Vector3f(correction).mul(bodyA.inverseMass))
        }
        if (!bodyB.isStatic) {
            bodyB.transform.position.add(Vector3f(correction).mul(bodyB.inverseMass))
        }
    }
}