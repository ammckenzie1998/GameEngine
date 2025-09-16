package org.ammck

import org.ammck.engine.physics.AxisAlignedBoundingBox
import org.ammck.engine.camera.Camera
import org.ammck.engine.objects.GameObject
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.PhysicsEngine
import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.camera.CameraInput
import org.ammck.engine.camera.FreeFlyCamera
import org.ammck.engine.objects.ModelLoader
import org.ammck.game.Player
import org.ammck.game.VehicleCommands
import org.ammck.engine.render.Mesh
import org.ammck.engine.render.ShaderProgram
import org.ammck.engine.render.Texture
import org.ammck.game.GameMode
import org.ammck.game.PlayerInput
import org.ammck.game.models.CarFactory
import org.ammck.game.models.WorldFactory
import org.joml.Math.min
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_KEY_E
import org.lwjgl.glfw.GLFW.GLFW_KEY_F1
import org.lwjgl.glfw.GLFW.GLFW_KEY_Q
import org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.glfwGetMouseButton
import org.lwjgl.glfw.GLFW.glfwGetTime
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.system.MemoryUtil

object Game{

    private var window: Long = 0
    private var currentMode = GameMode.PLAY
    private const val INITIAL_WINDOW_WIDTH = 800
    private const val INITIAL_WINDOW_HEIGHT = 600
    private const val WINDOW_TITLE = "Game Engine"

    private const val MAX_DELTA_TIME = 0.1f
    private const val RESPAWN_Y_THRESHOLD = -20f
    private val SPAWN_POINT = Vector3f(0f, 10f, 0f)

    private lateinit var shaderProgram: ShaderProgram
    private lateinit var debugShaderProgram: ShaderProgram

    private lateinit var playerCamera: Camera
    private lateinit var editCamera: FreeFlyCamera
    private lateinit var physicsEngine: PhysicsEngine

    private lateinit var player: Player
    private val gameObjects = mutableListOf<GameObject>()
    private lateinit var wheelMesh: Mesh
    private lateinit var cubeMesh: Mesh

    private lateinit var groundTexture: Texture
    private lateinit var defaultTexture: Texture

    private val projectionMatrix = Matrix4f()
    private val modelMatrix = Matrix4f()
    private var lastFrameTime = 0.0
    private var deltaTime = 0.0f

    private var mouseDeltaX = 0.0f
    private var mouseDeltaY = 0.0f
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var firstMouse = true

    @JvmStatic
    fun main(vararg args: String){
        init()
        loop()
        destroy()
    }

    private fun init(){
        initWindow()
        shaderProgram = ShaderProgram("shaders/default.vert", "shaders/default.frag")
        debugShaderProgram = ShaderProgram("shaders/debug.vert", "shaders/debug.frag")

        physicsEngine = PhysicsEngine()

        wheelMesh = AssetManager.getMesh("models/wheel.ammodel")
        val chassisMesh = AssetManager.getMesh("models/car.ammodel")
        val groundMesh = defineGround()
        cubeMesh = AssetManager.getMesh("models/cube.ammodel")
        val rampMesh = AssetManager.getMesh("models/ramp.ammodel")

        groundTexture = Texture("textures/grass.png")
        defaultTexture = Texture("textures/default.png")

        val playerGameObject = CarFactory.createPlayerCar(
            Transform(SPAWN_POINT), chassisMesh, wheelMesh)
        player = Player(playerGameObject)
        gameObjects.add(playerGameObject)

        val ground = createGameObject(
            groundMesh,
            isStatic = true,
            position=Vector3f(0f,0f,0f),
            boundingBoxSize=Vector3f(0f, -10f, 0f))

        createGameObject(
            cubeMesh,
            isStatic = true,
            position=Vector3f(20f, 0f, 0f),
            boundingBoxSize=Vector3f(1.0f, 1.0f, 1.0f))

        val ramp = createGameObject(
            rampMesh,
            isStatic = true,
            position = Vector3f(0f, -0.5f, -30f),
            boundingBoxSize=Vector3f(0f, 0f, 0f)
        )
        ramp.transform.orientation.rotateY(Math.toRadians(180.0).toFloat())

        physicsEngine.addObjects(*gameObjects.toTypedArray())
        physicsEngine.addWorldObjects(ramp, ground)

        playerCamera = Camera(playerGameObject.transform, distance = 12.0f)
        editCamera = FreeFlyCamera(Vector3f(0f, 10f, 0f))

        setupMatrices()

        glfwSetCursorPosCallback(window) {
            _, xpos, ypos ->
                if (firstMouse){
                    lastMouseX = xpos
                    lastMouseY = ypos
                    firstMouse = false
                }
                mouseDeltaX = (xpos - lastMouseX).toFloat()
                mouseDeltaY = (lastMouseY - ypos).toFloat()
                lastMouseX = xpos
                lastMouseY = ypos
        }

        lastFrameTime = glfwGetTime()
    }

    private fun loop(){
        glClearColor(0.992f, 0.369f, 0.325f, 0.0f)

        while(!glfwWindowShouldClose(window)){
            val currentFrameTime = glfwGetTime()
            val rawDeltaTime = (currentFrameTime - lastFrameTime).toFloat()
            deltaTime = min(rawDeltaTime, MAX_DELTA_TIME)
            lastFrameTime = currentFrameTime

            if(currentMode == GameMode.EDITOR){
                val reloadMeshPaths = AssetManager.update()
                if(reloadMeshPaths.isNotEmpty()){
                    for(gameObject in gameObjects){
                        gameObject.updateMesh(reloadMeshPaths)
                    }
                }
            }

            handleInput()
            for(gameObject in gameObjects) {
                gameObject.update()
            }

            physicsEngine.update(deltaTime)

            when(player.gameObject.physicsBody?.isRespawning){
                true -> {playerCamera.reset()}
                false, null -> {playerCamera.update(deltaTime)}
            }

            renderScene()
            renderDebugVisuals()

            mouseDeltaX = 0.0f
            mouseDeltaY = 0.0f

            glfwPollEvents()
            glfwSwapBuffers(window)

            if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS){
                glfwSetWindowShouldClose(window, true)
            }
        }
    }

    private fun destroy(){
        shaderProgram.cleanup()
        AssetManager.cleanup()
        groundTexture.cleanup()
        defaultTexture.cleanup()

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

        var viewMatrix = Matrix4f()
        when(currentMode){
            GameMode.PLAY -> {viewMatrix = playerCamera.getViewMatrix()}
            GameMode.EDITOR -> {viewMatrix = editCamera.getViewMatrix()}
        }


        shaderProgram.setUniform("projection", projectionMatrix)
        shaderProgram.setUniform("view", viewMatrix)
        shaderProgram.setUniform("textureSampler", 0)

        for(gameObject in gameObjects){
            if(gameObject.mesh === (gameObjects.find { it.physicsBody?.isStatic == true && it.transform.position.y == 0.0f}?.mesh)){
                glActiveTexture(GL_TEXTURE0)
                groundTexture.bind()
            } else{
                glActiveTexture(GL_TEXTURE0)
                defaultTexture.bind()
            }
            drawGameObject(gameObject)
        }

        shaderProgram.unbind()
    }

    private fun renderDebugVisuals(){
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        debugShaderProgram.bind()

        var viewMatrix = Matrix4f()
        when(currentMode){
            GameMode.PLAY -> {viewMatrix = playerCamera.getViewMatrix()}
            GameMode.EDITOR -> {viewMatrix = editCamera.getViewMatrix()}
        }

        debugShaderProgram.setUniform("projection", projectionMatrix)
        debugShaderProgram.setUniform("view", viewMatrix)

        val rayStartSize = Vector3f(0.1f)
        val rayHitSize = Vector3f(1f)
        val rayMissLength = 0.5f

        for (debugData in physicsEngine.debugRaycastResults){
            val ray = debugData.ray
            val hit = debugData.hit
            val color = debugData.color
            val startMatrix = Matrix4f().translate(ray.origin).scale(rayStartSize)
            debugShaderProgram.setUniform("model", startMatrix)
            cubeMesh.draw()

            if (hit != null){
                val hitMatrix = Matrix4f().translate(hit.point).scale(rayHitSize)
                debugShaderProgram.setUniform("model", hitMatrix)
                debugShaderProgram.setUniform("debugColor", color)
                cubeMesh.draw()
            } else {
                val endPoint = Vector3f(ray.origin).add(Vector3f(ray.direction).mul(rayMissLength))
                val missMatrix = Matrix4f().translate(endPoint).scale(rayStartSize)
                debugShaderProgram.setUniform("model", missMatrix)
                debugShaderProgram.setUniform("debugColor", color)
                cubeMesh.draw()
            }
        }

        debugShaderProgram.unbind()
        glDisable(GL_BLEND)
    }

    private fun drawGameObject(gameObject: GameObject){
        shaderProgram.setUniform("model", gameObject.globalMatrix)
        gameObject.mesh.draw()
        for(child in gameObject.children){
            drawGameObject(child)
        }
    }

    private fun handleInput(){
        when(currentMode){
            GameMode.PLAY -> {
                val playerInput = PlayerInput(
                    glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS,
                )
                player.update(deltaTime, playerInput)
                if(glfwGetKey(window, GLFW_KEY_F1) == GLFW_PRESS){
                    currentMode = GameMode.EDITOR
                }
            }
            GameMode.EDITOR -> {
                val isDragging = glfwGetMouseButton(window,GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                val cameraInput = CameraInput(
                    glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS,
                    glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS,
                    mouseDeltaX = if (isDragging) mouseDeltaX else 0.0f,
                    mouseDeltaY = if (isDragging) mouseDeltaY else 0.0f
                )
                editCamera.update(deltaTime, cameraInput)
                if(glfwGetKey(window, GLFW_KEY_F1) == GLFW_PRESS){
                    currentMode = GameMode.PLAY
                }
            }
        }

    }

    private fun setupMatrices(){
        val aspectRatio = INITIAL_WINDOW_WIDTH.toFloat() / INITIAL_WINDOW_HEIGHT.toFloat()
        projectionMatrix.perspective(Math.toRadians(45.0).toFloat(), aspectRatio, 0.1f, 100.0f)
    }

    private fun createGameObject (
        mesh: Mesh,
        isStatic: Boolean,
        position: Vector3f,
        boundingBoxSize: Vector3f,
    ): GameObject{
        val objectTransform = Transform(position)
        val objectBoundingBox = AxisAlignedBoundingBox(position, boundingBoxSize)
        val objectBody = PhysicsBody(objectBoundingBox, isStatic)
        val gameObject = GameObject("Object", objectTransform, mesh, objectBody)
        gameObjects.add(gameObject)
        return gameObject
    }

    private fun defineGround(): Mesh{
        val groundVertices = floatArrayOf(
            // Positions          // Colors (tint)     // Texture Coords (UVs)
            -50.0f, -0.75f, -500.0f,  1.0f, 1.0f, 1.0f,   0.0f, 25.0f,
            50.0f, -0.75f, -500.0f,   1.0f, 1.0f, 1.0f,  25.0f, 25.0f,
            50.0f, -0.75f,  500.0f,   1.0f, 1.0f, 1.0f,  25.0f, 0.0f,

            50.0f, -0.75f,  500.0f,   1.0f, 1.0f, 1.0f,  25.0f, 0.0f,
            -50.0f, -0.75f,  500.0f,  1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
            -50.0f, -0.75f, -500.0f,  1.0f, 1.0f, 1.0f,   0.0f, 25.0f
        )
        return Mesh(null, groundVertices)
    }

}