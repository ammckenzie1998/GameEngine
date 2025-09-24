package org.ammck.engine.assets

import org.ammck.engine.render.Mesh
import org.ammck.engine.render.ShaderProgram
import org.ammck.engine.render.Texture
import org.ammck.util.FileUtil
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTTBakedChar
import org.lwjgl.stb.STBTruetype.stbtt_BakeFontBitmap
import org.lwjgl.stb.STBTruetype.stbtt_GetBakedQuad
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class Font(resourcePath: String, fontHeight: Float = 32f) {

    val texture: Texture
    private val charData: STBTTBakedChar.Buffer

    private val BITMAP_WIDTH = 512
    private val BITMAP_HEIGHT = 512

    private val MIN_ASCII_CODE = 32
    private val MAX_ASCII_CODE = 128


    init{
        val bytes = FileUtil.readResourceAsByteBuffer(resourcePath)
        val bitmap = ByteBuffer.allocateDirect(BITMAP_WIDTH * BITMAP_HEIGHT)
        charData = STBTTBakedChar.malloc(MAX_ASCII_CODE - MIN_ASCII_CODE)

        stbtt_BakeFontBitmap(bytes, fontHeight, bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, MIN_ASCII_CODE, charData)

        texture = Texture(BITMAP_WIDTH, BITMAP_HEIGHT, bitmap)
    }

    fun drawText(text: String, x: Float, y: Float, color: Vector3f, shader: ShaderProgram, quadMesh: Mesh){
        texture.bind()
        shader.setUniform("uColor", color)

        MemoryStack.stackPush().use { stack ->
            val xPos = stack.floats(x)
            val yPos = stack.floats(y)
            val quad = STBTTAlignedQuad.malloc(stack)

            for(char in text){
                if (char.code in MIN_ASCII_CODE until MAX_ASCII_CODE){
                    stbtt_GetBakedQuad(charData, BITMAP_WIDTH, BITMAP_HEIGHT, char.code - MIN_ASCII_CODE, xPos, yPos, quad, true)

                    val width = quad.x1() - quad.x0()
                    val height = quad.y1() - quad.y0()
                    val modelMatrix = Matrix4f()
                        .translate(quad.x0(), quad.y0(), 0f)
                        .scale(width, height, 1f)

                    shader.setUniform("model", modelMatrix)

                    val texOffset = Vector2f(quad.s0(), quad.t1())
                    val texScale = Vector2f(quad.s1() - quad.s0(), quad.t0() - quad.t1())
                    shader.setUniform("uTexOffset", texOffset)
                    shader.setUniform("uTexScale", texScale)

                    quadMesh.draw()
                }
            }
        }
        texture.unbind()
    }

    fun cleanup(){
        texture.cleanup()
        charData.free()
    }

}