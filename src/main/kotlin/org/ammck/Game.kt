package org.ammck

import org.ammck.engine.AxisAlignedBoundingBox
import org.ammck.engine.Camera
import org.ammck.engine.GameObject
import org.ammck.engine.PhysicsBody
import org.ammck.engine.PhysicsEngine
import org.ammck.engine.Transform
import org.ammck.game.Player
import org.ammck.game.PlayerInput
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

    private lateinit var shaderProgram: ShaderProgram
    private lateinit var camera: Camera
    private lateinit var physicsEngine: PhysicsEngine

    private lateinit var player: Player
    private val gameObjects = mutableListOf<GameObject>()

    private val projectionMatrix = Matrix4f()
    private val modelMatrix = Matrix4f()
    private var lastFrameTime = 0.0
    private var deltaTime = 0.0f

    @JvmStatic
    fun main(vararg args: String){
        init()
        loop()
        destroy()
    }

    private fun init(){
        initWindow()
        shaderProgram = ShaderProgram("shaders/default.vert", "shaders/default.frag")

        physicsEngine = PhysicsEngine()

        val playerTransform = Transform(position = Vector3f(0f, 10f, 0f))
        val playerBoundingBox = AxisAlignedBoundingBox(playerTransform.position, Vector3f(2.0f, 1.0f, 1.0f))
        val playerBody = PhysicsBody(playerTransform, playerBoundingBox)
        val playerMesh = defineCube()
        val playerGameObject = GameObject(playerTransform, playerMesh, playerBody)
        player = Player(playerGameObject)
        gameObjects.add(playerGameObject)

        val groundTransform = Transform()
        val groundBoundingBox = AxisAlignedBoundingBox(groundTransform.position, Vector3f(100f, 0.01f, 100f))
        val groundBody = PhysicsBody(groundTransform, groundBoundingBox, true)
        val groundMesh = defineGround()
        val groundGameObject = GameObject(groundTransform, groundMesh, groundBody)
        gameObjects.add(groundGameObject)

        val cubeTransform = Transform()
        val cubeBoundingBox = AxisAlignedBoundingBox(cubeTransform.position, Vector3f(1.0f, 1.0f, 1.0f))
        val cubeBody = PhysicsBody(groundTransform, cubeBoundingBox, true)
        val cubeMesh = defineCube()
        val cubeGameObject = GameObject(cubeTransform, cubeMesh, cubeBody)
        gameObjects.add(cubeGameObject)

        physicsEngine.addBodies(playerBody, groundBody, cubeBody)

        camera = Camera(playerBody.transform)
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

            physicsEngine.update(deltaTime)
            camera.update()

            renderScene()

            glfwPollEvents()
            glfwSwapBuffers(window)

            if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS){
                glfwSetWindowShouldClose(window, true)
            }
        }
    }

    private fun destroy(){
        shaderProgram.cleanup()
        gameObjects.forEach { it.mesh.cleanup() }

        glfwDestroyWindow(window)
        glfwTerminate()
    }

    private fun initWindow(){
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
    }

    private fun renderScene(){
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        shaderProgram.bind()

        val viewMatrix = camera.getViewMatrix()

        shaderProgram.setUniform("projection", projectionMatrix)
        shaderProgram.setUniform("view", viewMatrix)

        for(gameObject in gameObjects){
            modelMatrix.identity()
                .translate(gameObject.transform.position)
                .rotateY(gameObject.transform.rotationY)
            shaderProgram.setUniform("model", modelMatrix)
            gameObject.mesh.draw()
        }

        shaderProgram.unbind()
    }

    private fun handleInput(){
        val playerInput = PlayerInput(
            glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS
        )
        player.update(deltaTime, playerInput)
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