package org.ammck

import org.ammck.engine.Camera
import org.ammck.render.Mesh
import org.ammck.render.ShaderProgram
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.system.MemoryUtil

object Game{

    private var window: Long = 0
    private const val INITIAL_WINDOW_WIDTH = 800
    private const val INITIAL_WINDOW_HEIGHT = 600
    private const val WINDOW_TITLE = "Game Engine"

    private const val MOVEMENT_SPEED = 20.0f
    private const val TURN_SPEED = 2.5f

    private lateinit var shaderProgram: ShaderProgram
    private lateinit var camera: Camera

    private lateinit var cubeMesh: Mesh
    private lateinit var groundMesh: Mesh
    private lateinit var originMesh: Mesh

    private val projectionMatrix = Matrix4f()
    private val modelMatrix = Matrix4f()

    private val cubePosition = Vector3f(0.0f, 0.0f, 0.0f)
    private var cubeRotationY = 0.0f
    private var lastFrameTime = 0.0
    private var deltaTime = 0.0f

    @JvmStatic
    fun main(vararg args: String){
        init()
        loop()
        destroy()
    }

    private fun init(){
        if(!glfwInit()){
            throw IllegalStateException("Unable to initialize GLFW")
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        window = glfwCreateWindow(
            INITIAL_WINDOW_WIDTH,
            INITIAL_WINDOW_HEIGHT,
            WINDOW_TITLE,
            MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if(window == MemoryUtil.NULL){
            throw RuntimeException("Failed to create the GLFW window")
        }

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) //V-sync
        glfwShowWindow(window)

        createCapabilities()
        glEnable(GL_DEPTH_TEST)

        shaderProgram = ShaderProgram("shaders/default.vert", "shaders/default.frag")
        groundMesh = defineGround()
        cubeMesh = defineCube()
        originMesh = defineCube()
        camera = Camera(cubePosition)

        setupMatrices()
        lastFrameTime = glfwGetTime()
    }

    private fun loop(){
        glClearColor(0.7f, 0.7f, 1.0f, 0.0f)

        while(!glfwWindowShouldClose(window)){
            val currentFrameTime = glfwGetTime()
            deltaTime = (currentFrameTime - lastFrameTime).toFloat()
            lastFrameTime = currentFrameTime


            handleInput()
            camera.update(cubeRotationY)
            val pos = cubePosition
            println("Cube position: X=${pos.x} Y=${pos.y} Z=${pos.z}")

            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            shaderProgram.bind()


            val viewMatrix = camera.getViewMatrix()

            shaderProgram.setUniform("projection", projectionMatrix)
            shaderProgram.setUniform("view", viewMatrix)

            modelMatrix.identity()
            shaderProgram.setUniform("model", modelMatrix)
            groundMesh.draw()

            modelMatrix.identity()
            shaderProgram.setUniform("model", modelMatrix)
            originMesh.draw()

            modelMatrix.identity()
                .translate(cubePosition)
                .rotateY(cubeRotationY)
            shaderProgram.setUniform("model", modelMatrix)
            cubeMesh.draw()

            shaderProgram.unbind()

            glfwPollEvents()
            glfwSwapBuffers(window)

            if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS){
                glfwSetWindowShouldClose(window, true)
            }
        }
    }

    private fun destroy(){
        shaderProgram.cleanup()
        cubeMesh.cleanup()
        groundMesh.cleanup()
        originMesh.cleanup()

        glfwDestroyWindow(window)
        glfwTerminate()
    }

    private fun handleInput(){
        if(glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS){
            cubeRotationY += TURN_SPEED * deltaTime
        }
        if(glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS){
            cubeRotationY -= TURN_SPEED * deltaTime
        }

        val move = MOVEMENT_SPEED * deltaTime
        val directionX = sin(cubeRotationY)
        val directionZ = cos(cubeRotationY)

        if(glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS){
            cubePosition.x += directionX * move
            cubePosition.z += directionZ * move
        }
        if(glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS){
            cubePosition.x -= directionX * move
            cubePosition.z -= directionZ * move
        }
    }

    private fun setupMatrices(){
        val aspectRatio = INITIAL_WINDOW_WIDTH.toFloat() / INITIAL_WINDOW_HEIGHT.toFloat()
        projectionMatrix.perspective(Math.toRadians(45.0).toFloat(), aspectRatio, 0.1f, 100.0f)
    }

    private fun defineGround(): Mesh{
        val groundVertices = floatArrayOf(
            // Positions             // Colors (a dark green)
            -50.0f, -0.75f, -50.0f,  0.2f, 0.4f, 0.2f,
            50.0f, -0.75f, -50.0f,  0.2f, 0.4f, 0.2f,
            50.0f, -0.75f,  50.0f,  0.2f, 0.4f, 0.2f,

            50.0f, -0.75f,  50.0f,  0.2f, 0.4f, 0.2f,
            -50.0f, -0.75f,  50.0f,  0.2f, 0.4f, 0.2f,
            -50.0f, -0.75f, -50.0f,  0.2f, 0.4f, 0.2f
        )
        return Mesh(groundVertices)
    }

    private fun defineCube(): Mesh{
        val cubeVertices = floatArrayOf(
            // Positions          // Colors
            // Back face (Red)
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,
            0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 0.0f,
            0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,

            // Front face (Green)
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
            0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
            0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
            0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 0.0f,

            // Left face (Blue)
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,

            // Right face (Yellow)
            0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 0.0f,
            0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f,
            0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,
            0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,
            0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.0f,
            0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 0.0f,

            // Bottom face (Magenta)
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
            0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
            0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 1.0f,

            // Top face (Cyan)
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 1.0f,
            0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 1.0f,
            0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
            0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 1.0f
        )
        return Mesh(cubeVertices)
    }

}