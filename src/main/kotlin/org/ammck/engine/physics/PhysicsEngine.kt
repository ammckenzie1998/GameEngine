package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject
import org.ammck.engine.render.Mesh
import org.joml.Matrix3f
import org.joml.Vector3f

data class DebugRaycast(val ray: Ray, val hit: RaycastHit?, val color: Vector3f)

class PhysicsEngine {

    private val GRAVITY_FORCE = Vector3f(0f, -9.8f, 0f)
    private val RESPAWN_Y_THRESHOLD = -20f
    private val RESPAWN_POSITION = Vector3f(0f, 10f, 0f)

    private val physicsObjects = mutableListOf<GameObject>()
    private val worldObjects = mutableListOf<GameObject>()
    private val collisions = mutableListOf<Collision>()

    val debugRaycastResults = mutableListOf<DebugRaycast>()

    fun addWorldObjects(vararg objects: GameObject){
        worldObjects.addAll(objects)
    }

    fun addObjects(vararg newObjects: GameObject) {
        physicsObjects.addAll(newObjects)
    }

    fun update(deltaTime: Float){
        debugRaycastResults.clear()
        //1. Forces
        for (obj in physicsObjects) {
            val physicsBody = obj.physicsBody ?: continue
            if (!physicsBody.isStatic) {
                physicsBody.isGrounded = false
                physicsBody.isOnRamp = false
                physicsBody.isRespawning = false

                obj.suspension?.let {
                        suspension ->  applySuspensionForces(obj, physicsBody, suspension)
                }
                physicsBody.forces.add(GRAVITY_FORCE)
                val acceleration = Vector3f(physicsBody.forces).mul(physicsBody.inverseMass)
                physicsBody.velocity.add(acceleration.mul(deltaTime))
            }
        }

        //2. Motion
        for (obj in physicsObjects){
//            if(obj.id == "Player") println(obj.physicsBody!!.isGrounded)
            val body = obj.physicsBody ?: continue
            if(!body.isStatic){
                obj.transform.position.add(Vector3f(body.velocity).mul(deltaTime))
            }
            body.forces.set(0f, 0f, 0f)
        }

        //3. Collisions
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
        for (collision in collisions) {
            resolveCollision(collision)
        }

        //4. Respawn check
        for (obj in physicsObjects){
            val body = obj.physicsBody ?: continue
            if(!body.isStatic && obj.transform.position.y < RESPAWN_Y_THRESHOLD){
                obj.transform.position.set(RESPAWN_POSITION)
                body.velocity.set(0f, 0f, 0f)

                body.isRespawning = true
            }
        }
    }

    private fun resolveCollision(collision: Collision) {
        val bodyA = collision.firstObject.physicsBody!!
        val bodyB = collision.secondObject.physicsBody!!
        val normal = collision.normal


        if(bodyA.isRamp && !bodyB.isStatic){
            bodyB.isOnRamp = true
            return
        }
        if(bodyB.isRamp && !bodyA.isStatic){
            bodyA.isOnRamp = true
            return
        }

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

    private fun applySuspensionForces(gameObject: GameObject, body: PhysicsBody, suspension: Suspension){
        var groundedWheels = 0
        var combinedNormal = Vector3f()
        val rotationMatrix = Matrix3f().set(gameObject.transform.orientation)

        val wheelColors = listOf(
            Vector3f(1f, 0f, 0f),
            Vector3f(0f, 1f, 0f),
            Vector3f(0f, 0f, 1f),
            Vector3f(0f, 1f, 1f)
        )

        for (i in 0 until suspension.wheelPositions.size){
            val wheelPos = suspension.wheelPositions[i]
            val wheelWorldPos = Vector3f(wheelPos).mul(rotationMatrix).add(gameObject.transform.position)
            val ray = Ray(wheelWorldPos, Vector3f(0f, -1f, 0f))
            var closestHit: RaycastHit? = null

            for(obj in worldObjects){
                val hit = Raycaster.castRay(ray, obj)
                if (hit != null && (closestHit == null || hit.distance < closestHit.distance)){
                    closestHit = hit
                }
            }

            debugRaycastResults.add(DebugRaycast(ray, closestHit, wheelColors[i]))

            if (closestHit != null && closestHit.distance < suspension.height){
                groundedWheels ++
                if(closestHit.normal.dot(ray.direction) > 0){
                    closestHit.normal.negate()
                }
                combinedNormal.add(closestHit.normal)

                val compression = suspension.height - closestHit.distance
                val springForce = compression * suspension.stiffness
                val velocityAlongNormal = body.velocity.dot(closestHit.normal)
                val dampingForce = velocityAlongNormal * suspension.damping
                val suspensionForce = springForce - dampingForce
                val suspensionForceVector = Vector3f(closestHit.normal).mul(suspensionForce)
                body.forces.add(suspensionForceVector)
            }
        }
        if(groundedWheels > 0){
            body.isGrounded = true
            if(combinedNormal.lengthSquared() > 0.001f){
                body.groundNormal.set(combinedNormal.normalize())
            } else{
                body.groundNormal.set(0f, 1f, 0f)
            }
        }
    }
}