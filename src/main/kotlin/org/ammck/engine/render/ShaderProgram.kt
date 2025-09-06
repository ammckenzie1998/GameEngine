package org.ammck.engine.render

import org.ammck.util.FileUtil
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glDeleteProgram
import org.lwjgl.opengl.GL20.glDeleteShader
import org.lwjgl.opengl.GL20.glDetachShader
import org.lwjgl.opengl.GL20.glGetProgramInfoLog
import org.lwjgl.opengl.GL20.glGetProgrami
import org.lwjgl.opengl.GL20.glGetShaderInfoLog
import org.lwjgl.opengl.GL20.glGetShaderi
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glLinkProgram
import org.lwjgl.opengl.GL20.glShaderSource
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL20.glUseProgram
import java.io.File
import java.nio.FloatBuffer

class ShaderProgram (vertexPath: String, fragmentPath: String) {

    private val programId: Int
    private var vertexShaderId: Int = 0
    private var fragmentShaderId: Int = 0
    private val matrixBuffer: FloatBuffer = BufferUtils.createFloatBuffer(16)

    init{
        val vertexCode = FileUtil.readResourceAsString(vertexPath)
        val fragmentCode = FileUtil.readResourceAsString(fragmentPath)

        vertexShaderId = compileShader(vertexCode, GL_VERTEX_SHADER)
        fragmentShaderId = compileShader(fragmentCode, GL_FRAGMENT_SHADER)

        programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0){
            throw RuntimeException("Error linking shader program: ${glGetProgramInfoLog(programId, 1024)}")
        }

        glDetachShader(programId, vertexShaderId)
        glDetachShader(programId, fragmentShaderId)
        glDeleteShader(vertexShaderId)
        glDeleteShader(fragmentShaderId)
    }

    fun bind(){
        glUseProgram(programId)
    }

    fun unbind(){
        glUseProgram(0)
    }

    fun cleanup(){
        unbind()
        if (programId != 0){
            glDeleteProgram(programId)
        }
    }

    fun setUniform(uniformName: String, value: Matrix4f){
        val location = glGetUniformLocation(programId, uniformName)

        if(location != -1){
            value.get(matrixBuffer)
            glUniformMatrix4fv(location, false, matrixBuffer)
        }
    }

    fun setUniform(uniformName: String, value: Int){
        val location = glGetUniformLocation(programId, uniformName)
        if(location != -1){
            glUniform1i(location, value)
        }
    }

    private fun compileShader(shaderCode: String, shaderType: Int): Int{
        val shaderId = glCreateShader(shaderType)
        if(shaderId == 0){
            throw RuntimeException("Error creating shader of type $shaderType")
        }
        glShaderSource(shaderId, shaderCode)
        glCompileShader(shaderId)
        if(glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0){
            throw RuntimeException("Error compiling shader: ${glGetShaderInfoLog(shaderId, 1024)}")
        }
        return shaderId
    }

}