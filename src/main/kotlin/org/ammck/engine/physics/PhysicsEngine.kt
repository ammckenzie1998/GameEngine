package org.ammck.engine.physics

import org.ammck.engine.objects.GameObject
import org.ammck.engine.render.Mesh
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.Vector
import kotlin.math.abs
import kotlin.math.sign

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
                physicsBody.lastImpactImpulse = 0.0f

                obj.suspension?.let {
                        suspension ->  applySuspensionForces(obj, physicsBody, suspension)
                }
                physicsBody.forces.add(GRAVITY_FORCE)

                val acceleration = Vector3f(physicsBody.forces).mul(physicsBody.inverseMass)
                physicsBody.velocity.add(acceleration.mul(deltaTime))

                val angularAcceleration = Vector3f(physicsBody.torques).mul(physicsBody.inverseInertia)
                physicsBody.angularVelocity.add(angularAcceleration.mul(deltaTime))
            }
        }

        //2. Motion
        for (obj in physicsObjects){
            val body = obj.physicsBody ?: continue
            if(!body.isStatic){
                if (body.isGrounded){
                    val inverseRotation = Quaternionf(obj.transform.orientation).conjugate()
                    val localVelocity = Vector3f(body.velocity).rotate(inverseRotation)

                    localVelocity.z *= body.linearDrag
                    localVelocity.x *= body.lateralGrip

                    body.velocity.set(localVelocity.rotate(obj.transform.orientation))
                }
                body.angularVelocity.mul(body.angularDrag)

                obj.transform.position.add(Vector3f(body.velocity).mul(deltaTime))

                if(body.angularVelocity.lengthSquared() > 0.0001f) {
                    val deltaRotation = Quaternionf().fromAxisAngleRad(
                        body.angularVelocity,
                        body.angularVelocity.length() * deltaTime
                    )
                    deltaRotation.mul(obj.transform.orientation, obj.transform.orientation)
                    obj.transform.orientation.normalize()
                }

                val acceleration = Vector3f(body.velocity).sub(body.previousVelocity).div(deltaTime)
                val inverseOrientation = Quaternionf(obj.transform.orientation).conjugate()
                val localAccel = acceleration.rotate(inverseOrientation)

                var pitchForce = localAccel.z
                if(pitchForce > 0){
                    val dragThreshold = 50.0f
                    pitchForce = (pitchForce - dragThreshold).coerceAtLeast(0f)
                } else{
                    pitchForce *= 1.0f
                }

                var lateralForce = localAccel.x
                val lateralDeadzone = 30.0f

                if (abs(lateralForce) < lateralDeadzone){
                    lateralForce = 0.0f
                } else{
                    lateralForce -= sign(lateralForce) * lateralDeadzone
                }

                body.previousVelocity.set(body.velocity)

                val maxBodyRoll = 0.15f
                val rollSensitivity = 0.05f
                val targetBodyRoll = (lateralForce * rollSensitivity).coerceIn(-maxBodyRoll, maxBodyRoll)

                val maxBodyPitch = 0.10f
                val pitchSensitivity = -0.01f
                val targetBodyPitch = (pitchForce * pitchSensitivity).coerceIn(-maxBodyPitch, maxBodyPitch)

                val targetVisualRot = Quaternionf()
                    .rotateX(targetBodyPitch)
                    .rotateZ(targetBodyRoll)

                val isCentering = abs(targetBodyRoll) < 0.01f
                val rollSpeed = if(isCentering) 10.0f else 4.0f

                obj.visualOrientation.slerp(targetVisualRot, rollSpeed * deltaTime)
            }
            resolveChassisCollision(obj)
            body.forces.set(0f, 0f, 0f)
            body.torques.set(0f, 0f, 0f)
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

                bodyA.boundingBox.transform.position.set(objA.transform.position)
                bodyB.boundingBox.transform.position.set(objB.transform.position)

                val mtv = bodyA.boundingBox.getCollisionResponse(bodyB.boundingBox)
                if (mtv != null && mtv.length() > 0.00001) {
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
        for (obj in physicsObjects.filter{ it.respawnable }){
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

        val totalInverseMass = bodyA.inverseMass + bodyB.inverseMass
        if (totalInverseMass <= 0) return

        val correction = Vector3f(normal).mul(collision.penetration / totalInverseMass)
        if (!bodyA.isStatic) {
            collision.firstObject.transform.position.sub(Vector3f(correction).mul(bodyA.inverseMass))
        }
        if (!bodyB.isStatic) {
            collision.secondObject.transform.position.add(Vector3f(correction).mul(bodyB.inverseMass))
        }

        val collisionPoint = collision.getCollisionPoint()
        val ra = Vector3f(collisionPoint).sub(collision.firstObject.getPosition())
        val rb = Vector3f(collisionPoint).sub(collision.secondObject.getPosition())
        val velocityA = Vector3f(bodyA.velocity).add(Vector3f(bodyA.angularVelocity).cross(ra))
        val velocityB = Vector3f(bodyB.velocity).add(Vector3f(bodyB.angularVelocity).cross(rb))

        val relativeVelocity = Vector3f(velocityB).sub(velocityA)
        val velocityAlongNormal = relativeVelocity.dot(normal)
        if (velocityAlongNormal > 0) return


        val e = 0.5f // Bounciness
        var j = -(1 + e) * velocityAlongNormal
        val impactRadius = (j * 0.05f).coerceIn(0.5f, 2.5f)
        val impactForce = j * 0.1f

        val termA = if (bodyA.inverseInertia > 0) Vector3f(ra).cross(normal).lengthSquared() * bodyA.inverseInertia else 0.0f
        val termB = if (bodyB.inverseInertia > 0) Vector3f(rb).cross(normal).lengthSquared() * bodyB.inverseInertia else 0.0f
        if(totalInverseMass + termA + termB > 0) j /= (totalInverseMass + termA + termB) else j = 0.0f
        val impulse = Vector3f(normal).mul(j)
        if (!bodyA.isStatic) {
            bodyA.velocity.sub(Vector3f(impulse).mul(bodyA.inverseMass))
            bodyA.angularVelocity.sub(Vector3f(ra).cross(impulse).mul(bodyA.inverseInertia))
            bodyA.lastImpactImpulse += j

            val worldToLocal = Matrix4f(collision.firstObject.globalMatrix).invert()
            val localHit = Vector3f(collisionPoint).mulPosition(worldToLocal)
            collision.firstObject.model.mesh.applyDeformation(localHit, impactForce, impactRadius)
        }
        if (!bodyB.isStatic) {
            bodyB.velocity.add(Vector3f(impulse).mul(bodyB.inverseMass))
            bodyB.angularVelocity.add(Vector3f(rb).cross(impulse).mul(bodyB.inverseInertia))
            bodyB.lastImpactImpulse += j

            val worldToLocal = Matrix4f(collision.secondObject.globalMatrix).invert()
            val localHit = Vector3f(collisionPoint).mulPosition(worldToLocal)
            collision.secondObject.model.mesh.applyDeformation(localHit, impactForce, impactRadius)
        }

        if(normal.y < 0.5f && !bodyA.isStatic){
            bodyA.isGrounded = true
        }
        if(normal.y > -0.5f && !bodyB.isStatic){
            bodyB.isGrounded = true
        }
    }

    //TEMPORARY METHOD TO PREVENT FALLING THROUGH GROUND ON BAD LANDING - TO BE REPLACED
    private fun resolveChassisCollision(obj: GameObject) {
        val body = obj.physicsBody ?: return
        val chassisRadius = 0.3f
        val ray = Ray(obj.transform.position, Vector3f(0f, -1f, 0f))

        for(worldObj in worldObjects) {
            val hit = Raycaster.castRay(ray, worldObj)

            if (hit != null && hit.distance < chassisRadius) {
                val penetration = chassisRadius - hit.distance
                val correction = Vector3f(0f, 1f, 0f).mul(penetration)
                obj.transform.position.add(correction)

                if (body.velocity.y < 0) {
                    body.velocity.y = 0f
                }
                body.isGrounded = true

                body.velocity.x *= 0.998f
                body.velocity.z *= 0.998f
            }
        }
    }

    private fun applySuspensionForces(gameObject: GameObject, body: PhysicsBody, suspension: Suspension){
        var groundedWheels = 0
        var combinedNormal = Vector3f()
        val rotationMatrix = Matrix3f().set(gameObject.transform.orientation)

        val wheelDebugColors = listOf(
            Vector3f(1f, 0f, 0f),
            Vector3f(0f, 1f, 0f),
            Vector3f(0f, 0f, 1f),
            Vector3f(0f, 1f, 1f)
        )

        val offsets = listOf(
            Vector3f(0f, 0f, 0f),
            Vector3f(0.3f, 0f, 0f),
            Vector3f(-0.3f, 0f, 0f),
            Vector3f(0f, 0f, 0.3f),
            Vector3f(0f, 0f, -0.3f),
        )

        for (i in 0 until suspension.wheelPositions.size){
            val wheelPos = suspension.wheelPositions[i]
            val wheelWorldPos = Vector3f(wheelPos).mul(rotationMatrix).add(gameObject.transform.position)
            var closestHit: RaycastHit? = null
            var ray: Ray? = null

            for(o in offsets){
                val rotatedOffset = Vector3f(o).mul(rotationMatrix)
                val offsetPosition = Vector3f(wheelWorldPos).add(rotatedOffset)
                val rayDir = Vector3f(0f, -1f, 0f).mul(rotationMatrix).normalize()
                val rayOrigin = Vector3f(offsetPosition)
                val r = Ray(rayOrigin, rayDir)

                for(obj in worldObjects){
                    val hit = Raycaster.castRay(r, obj)
                    if (hit != null && (closestHit == null || hit.distance < closestHit.distance)){
                        closestHit = hit
                        ray = r
                    }
                    debugRaycastResults.add(DebugRaycast(r, closestHit, wheelDebugColors[i]))
                }
            }

            if (closestHit != null && ray != null){
                val distance = closestHit.distance
                if(distance < suspension.height && distance >= 0) {
                    groundedWheels++
                    if (closestHit.normal.dot(ray.direction) > 0) {
                        closestHit.normal.negate()
                    }
                    combinedNormal.add(closestHit.normal)

                    val compression = suspension.height - distance
                    val springForce = compression * suspension.stiffness
                    val springDir = Vector3f(ray.direction).negate()

                    val arm = Vector3f(wheelPos).mul(rotationMatrix)
                    val pointVelocity = Vector3f()
                    body.angularVelocity.cross(arm, pointVelocity)
                    pointVelocity.add(body.velocity)
                    val velocityAlongNormal = pointVelocity.dot(springDir)

                    val dampingForce = velocityAlongNormal * suspension.damping
                    val suspensionForce = springForce - dampingForce
                    val suspensionForceVector = springDir.mul(suspensionForce)
                    body.forces.add(suspensionForceVector)

                    val torque = Vector3f()
                    arm.cross(suspensionForceVector, torque)
                    body.torques.add(torque)
                }
            }
        }
        if(groundedWheels > 0){
            body.isGrounded = true
            if(combinedNormal.lengthSquared() > 0.001f){
                body.groundNormal.set(combinedNormal.normalize())
            } else{
                body.groundNormal.set(0f, 1f, 0f)
            }
        } else{
            body.groundNormal.set(0f, 1f, 0f)
        }
    }

    fun clear(){
        physicsObjects.clear()
        worldObjects.clear()
        collisions.clear()
        debugRaycastResults.clear()
    }
}