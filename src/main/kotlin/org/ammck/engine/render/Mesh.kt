package org.ammck.engine.render

import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL30.glGenVertexArrays
import javax.swing.text.Position
import kotlin.math.sqrt

class Mesh (val resourcePath: String?, var vertices: FloatArray) {

    private val vaoId: Int
    private val vboId: Int
    val vertexCount: Int

    companion object{
        private const val POSITION_ATTRIBUTE_LOCATION = 0
        private const val COLOR_ATTRIBUTE_LOCATION = 1
        private const val TEX_COORD_ATTRIBUTE_LOCATION = 2
        private const val NORMAL_ATTRIBUTE_LOCATION = 3

        private const val POSITION_COMPONENTS = 3 // x, y, z
        private const val COLOR_COMPONENTS = 3 // r, g, b
        private const val TEX_COORD_COMPONENTS = 2 // u, v
        private const val NORMAL_COMPONENTS = 3 // x, y, z

        private const val VERTEX_FLOAT_COUNT = POSITION_COMPONENTS + COLOR_COMPONENTS + TEX_COORD_COMPONENTS + NORMAL_COMPONENTS
        private const val STRIDE = VERTEX_FLOAT_COUNT * Float.SIZE_BYTES

        private const val COLOR_OFFSET = (POSITION_COMPONENTS * Float.SIZE_BYTES).toLong()
        private const val TEX_COORD_OFFSET = ((POSITION_COMPONENTS + COLOR_COMPONENTS) * Float.SIZE_BYTES).toLong()
        private const val NORMAL_OFFSET = ((POSITION_COMPONENTS + COLOR_COMPONENTS + TEX_COORD_COMPONENTS) * Float.SIZE_BYTES).toLong()

    }

    init{
        vertexCount = vertices.size / VERTEX_FLOAT_COUNT

        vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)

        vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW)

        glVertexAttribPointer(
            POSITION_ATTRIBUTE_LOCATION,
            POSITION_COMPONENTS,
            GL_FLOAT,
            false,
            STRIDE,
            0
        )
        glEnableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION)

        glVertexAttribPointer(
            COLOR_ATTRIBUTE_LOCATION,
            COLOR_COMPONENTS,
            GL_FLOAT,
            false,
            STRIDE,
            COLOR_OFFSET
        )
        glEnableVertexAttribArray(COLOR_ATTRIBUTE_LOCATION)

        glVertexAttribPointer(
            TEX_COORD_ATTRIBUTE_LOCATION,
            TEX_COORD_COMPONENTS,
            GL_FLOAT,
            false,
            STRIDE,
            TEX_COORD_OFFSET
        )
        glEnableVertexAttribArray(TEX_COORD_ATTRIBUTE_LOCATION)

        glVertexAttribPointer(
            NORMAL_ATTRIBUTE_LOCATION,
            NORMAL_COMPONENTS,
            GL_FLOAT,
            false,
            STRIDE,
            NORMAL_OFFSET
        )
        glEnableVertexAttribArray(NORMAL_ATTRIBUTE_LOCATION)
    }

    fun draw(){
        glBindVertexArray(vaoId)
        glDrawArrays(GL_TRIANGLES, 0, vertexCount)
        glBindVertexArray(0)
    }

    fun cleanup(){
        glDeleteBuffers(vboId)
        glDeleteVertexArrays(vaoId)
    }

    fun getVertexPosition(position: Int): Vector3f{
        val ix = VERTEX_FLOAT_COUNT * position + POSITION_ATTRIBUTE_LOCATION
        val iy = ix + 1
        val iz = iy + 1

        return Vector3f(
            vertices[ix],
            vertices[iy],
            vertices[iz]
        )
    }

    fun updateVertexBuffer() {
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun clone(): Mesh{
        return Mesh(resourcePath, vertices.clone())
    }

    fun applyDeformation (impactPoint: Vector3f, force: Float, radius: Float){
        var modified = false
        val rSquared = radius * radius

        for (i in vertices.indices step VERTEX_FLOAT_COUNT){
            val vx = vertices[i]
            val vy = vertices[i+1]
            val vz = vertices[i+2]

            val dx = vx - impactPoint.x
            val dy = vy - impactPoint.y
            val dz = vz - impactPoint.z
            val dSquared = (dx * dx) + (dy * dy) + (dz * dz)

            if (dSquared < rSquared){
                val distance = sqrt(dSquared.toDouble()).toFloat()
                val intensity = (radius - distance) / radius
                val amount = force * intensity * 0.05f

                val currentPos = Vector3f(vx, vy, vz)
                val toCenter = Vector3f(0f, 0f, 0f).sub(currentPos).normalize()

                val noise = (Math.random().toFloat() - 0.5f) * 0.05f

                vertices[i] += (toCenter.x * amount) + noise
                vertices[i+1] += (toCenter.y * amount) + noise
                vertices[i+2] += (toCenter.z * amount) + noise

                modified = true
            }
        }

        if (modified){
            updateVertexBuffer()
        }
    }

}