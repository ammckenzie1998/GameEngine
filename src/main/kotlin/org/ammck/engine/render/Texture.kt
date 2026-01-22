package org.ammck.engine.render

import org.ammck.util.FileUtil
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_RED
import org.lwjgl.opengl.GL11.GL_REPEAT
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class Texture{
    val id: Int = glGenTextures()

    constructor(resourcePath: String){
        glBindTexture(GL_TEXTURE_2D, id)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            val imageBytes = FileUtil.readResourceAsByteBuffer(resourcePath)
            stbi_set_flip_vertically_on_load(true)
            val decodedImage: ByteBuffer = stbi_load_from_memory(imageBytes, w, h, channels, 4)
                ?: throw RuntimeException("Failed to load texture image: $resourcePath")

            glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0),
                0, GL_RGBA, GL_UNSIGNED_BYTE, decodedImage
            )
            glGenerateMipmap(GL_TEXTURE_2D)
            stbi_image_free(decodedImage)
        }
    }

    constructor(width: Int, height: Int, bitmap: ByteBuffer){
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap)
        glGenerateMipmap(GL_TEXTURE_2D)
    }

    fun bind(){
        glBindTexture(GL_TEXTURE_2D, id)
    }

    fun unbind(){
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun cleanup(){
        glDeleteTextures(id)
    }
}