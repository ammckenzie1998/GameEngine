package org.ammck.game.manager

import org.ammck.engine.render.Mesh
import org.ammck.engine.render.ShaderProgram
import org.ammck.game.Vehicle
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable

class HudManager(
    private val vehicle: Vehicle,
    private val shader: ShaderProgram,
    private val projectionMatrix: Matrix4f
) {

    private val quadMesh: Mesh = defineUnitQuadMesh()
    private val modelMatrix = Matrix4f()

    fun draw(){
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        shader.bind()
        shader.setUniform("projection", projectionMatrix)

        drawBar(
            x = 20f,
            y = 20f,
            width = 300f,
            height = 20f,
            percentage = vehicle.currentStylePoints / vehicle.MAX_STYLEPOINTS,
            color = Vector3f(0.5f, 0.5f, 1.0f)
        )

        drawBar(
            x = 20f,
            y = 50f,
            width = 300f,
            height = 20f,
            percentage = vehicle.currentHealth / vehicle.MAX_HEALTH,
            color = Vector3f(1.0f, 0.5f, 0.5f)
        )
        shader.unbind()
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    private fun drawBar(x: Float, y: Float, width: Float, height: Float, percentage: Float, color: Vector3f){
        shader.setUniform("uColor", Vector3f(color).mul(0.3f))
        shader.setUniform("uAlpha", 0.7f)
        modelMatrix.identity().translate(x, y, 0f).scale(width, height, 1f)
        shader.setUniform("model", modelMatrix)
        quadMesh.draw()

        if (percentage > 0){
            val barWidth = width * percentage
            shader.setUniform("uColor", color)
            shader.setUniform("uAlpha", 1.0f)
            modelMatrix.identity().translate(x, y, 0f).scale(barWidth, height, 1f)
            shader.setUniform("model", modelMatrix)
            quadMesh.draw()
        }
    }

    private fun defineUnitQuadMesh(): Mesh{
        val vertices = floatArrayOf(
            // Positions        // Colors (tint)     // Texture Coords (UVs)
            0.0f, 0.0f, 0.0f,   0f, 0f, 0f,          0f, 0f,
            0.0f, 1.0f, 0.0f,   0f, 0f, 0f,          0f, 0f,
            1.0f, 0.0f, 0.0f,   0f, 0f, 0f,          0f, 0f,

            0.0f, 1.0f, 0.0f,   0f, 0f, 0f,          0f, 0f,
            1.0f, 1.0f, 0.0f,   0f, 0f, 0f,          0f, 0f,
            1.0f, 0.0f, 0.0f,   0f, 0f, 0f,          0f, 0f,
        )
        return Mesh(null, vertices)
    }

    fun cleanup(){
        quadMesh.cleanup()
    }

}