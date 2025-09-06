package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject
import org.joml.Vector3f

class PhysicsEngine {

    private val GRAVITY = -9.8f
    private val RESPAWN_Y_THRESHOLD = -20f
    private val RESPAWN_POSITION = Vector3f(0f, 10f, 0f)

    private val physicsObjects = mutableListOf<GameObject>()
    private val collisions = mutableListOf<Collision>()

    fun addObject(vararg newObjects: GameObject) {
        physicsObjects.addAll(newObjects)
    }

    fun update(deltaTime: Float): PhysicsStateReport {
        val report = PhysicsStateReport()

        //1. Gravity
        for (obj in physicsObjects) {
            val physicsBody = obj.physicsBody ?: continue
            if (!physicsBody.isStatic) {
                physicsBody.isGrounded = false
                physicsBody.velocity.y += GRAVITY * deltaTime
                obj.transform.position.add(Vector3f(physicsBody.velocity).mul(deltaTime))
            }
        }

        //2. Collisions
        collisions.clear()
        for (i in physicsObjects.indices) {
            for (j in i + 1 until physicsObjects.size) {
                val objA = physicsObjects[i]
                val objB = physicsObjects[j]

                val bodyA = objA.physicsBody ?: continue
                val bodyB = objB.physicsBody ?: continue

                if (bodyA.isStatic && bodyB.isStatic) continue

                bodyA.boundingBox.center.set(objA.transform.position)
                bodyB.boundingBox.center.set(objB.transform.position)

                val mtv = bodyA.boundingBox.getCollisionResponse(bodyB.boundingBox)
                if (mtv != null) {
                    val penetration = mtv.length()
                    val normal = Vector3f(mtv).normalize()
                    collisions.add(Collision(objA, objB, normal, penetration))
                }
            }
        }

        for (obj in physicsObjects){
            val body = obj.physicsBody ?: continue
            if(!body.isStatic && obj.transform.position.y < RESPAWN_Y_THRESHOLD){
                obj.transform.position.set(RESPAWN_POSITION)
                body.velocity.set(0f, 0f, 0f)

                report.objectReport.getOrPut(PhysicsState.OBJECT_RESPAWN) { mutableListOf() }.add(obj)
            }
        }

        for (collision in collisions) {
            resolveCollision(collision)
        }
        return report
    }

    private fun resolveCollision(collision: Collision) {
        val bodyA = collision.firstObject.physicsBody!!
        val bodyB = collision.secondObject.physicsBody!!
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
            collision.firstObject.transform.position.sub(Vector3f(correction).mul(bodyA.inverseMass))
        }
        if (!bodyB.isStatic) {
            collision.secondObject.transform.position.add(Vector3f(correction).mul(bodyB.inverseMass))
        }
        if(normal.y < 0.5f && !bodyA.isStatic){
            bodyA.isGrounded = true
        }
        if(normal.y > -0.5f && !bodyB.isStatic){
            bodyB.isGrounded = true
        }
    }
}