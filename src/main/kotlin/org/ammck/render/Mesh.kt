package org.ammck.render

import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
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

class Mesh (vertices: FloatArray) {

    private val vaoId: Int
    private val vboId: Int
    private val vertexCount: Int

    companion object{
        private const val POSITION_ATTRIBUTE_LOCATION = 0
        private const val COLOR_ATTRIBUTE_LOCATION = 1
        private const val POSITION_COMPONENTS = 3 // x, y, z
        private const val COLOR_COMPONENTS = 3 // r, g, b

        private const val VERTEX_FLOAT_COUNT = POSITION_COMPONENTS + COLOR_COMPONENTS
        private const val STRIDE = VERTEX_FLOAT_COUNT * Float.SIZE_BYTES
        private const val COLOR_OFFSET = (POSITION_COMPONENTS * Float.SIZE_BYTES).toLong()
    }

    init{
        vertexCount = vertices.size / VERTEX_FLOAT_COUNT

        vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)

        vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

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

}