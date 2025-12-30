package org.ammck.game.factory

import org.ammck.engine.objects.Model
import org.ammck.game.components.Weapon

object WeaponFactory {

    fun createDefaultGun(model: Model, projectileModel: Model): Weapon {
        return Weapon(
            name = "Default",
            fireRate = 1.0f,
            shotSpeed = 200.0f,
            damage = 1.0f,
            range = 1.0f,
            shotSize = 1.0f,
            model = model,
            projectileModel = projectileModel
        )
    }
}