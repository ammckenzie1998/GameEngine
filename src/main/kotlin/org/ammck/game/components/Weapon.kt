package org.ammck.game.components

import org.ammck.engine.Transform
import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.Model
import org.ammck.engine.render.Mesh
import org.joml.Quaternionf
import org.joml.Vector3f

data class Projectile(
    val transform: Transform,
    val velocity: Vector3f,
    val damage: Float,
    val model: Model
)

class Weapon(
    val name: String,
    val fireRate: Float,
    val shotSpeed: Float,
    val damage: Float,
    val range: Float,
    val shotSize: Float,
    val model: Model,
    val projectileModel: Model
) {
    private var cooldownTime = 0f

    fun canFire(currentTime: Float): Boolean{
        println(cooldownTime)
        return cooldownTime == 0f
    }

    fun coolDown(deltaTime: Float){
        cooldownTime -= deltaTime
        if(cooldownTime < 0f){
            cooldownTime = 0f
        }
    }

    fun fire(barrelTransform: Transform, currentTime: Float): Projectile?{
        if (!canFire(currentTime)) return null

        cooldownTime = fireRate

        val forwardDir = barrelTransform.forwardDirection()
        val barrelPos = barrelTransform.position

        val offset = Vector3f(forwardDir).mul(2.0f)
        val spawnPos = Vector3f(barrelPos).add(offset)
        val velocity = Vector3f(forwardDir).mul(shotSpeed)
        val projectileTransform = Transform(
            position = spawnPos,
            orientation = Quaternionf(barrelTransform.orientation),
            scale = Vector3f(shotSize)
        )

        return Projectile(
            transform = projectileTransform,
            velocity = velocity,
            damage = damage,
            model = projectileModel
        )
    }

}