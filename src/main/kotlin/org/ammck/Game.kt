package org.ammck

import org.ammck.engine.physics.OrientedBoundingBox
import org.ammck.engine.camera.Camera
import org.ammck.engine.objects.GameObject
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.physics.PhysicsEngine
import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.camera.CameraInput
import org.ammck.engine.camera.FreeFlyCamera
import org.ammck.game.Player
import org.ammck.engine.render.Mesh
import org.ammck.engine.render.ShaderProgram
import org.ammck.engine.render.Texture
import org.ammck.game.AIController
import org.ammck.game.GameState
import org.ammck.game.PlayerInput
import org.ammck.game.race.RaceManager
import org.ammck.game.Vehicle
import org.ammck.game.WaypointType
import org.ammck.game.factory.VehicleFactory
import org.ammck.game.ui.HUDState
import org.ammck.game.ui.HudManager
import org.ammck.games.Waypoint
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
import org.lwjgl.glfw.GLFW.GLFW_KEY_F2
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
import kotlin.math.sqrt

object Game{

    private var window: Long = 0
    private var gameStates = mutableMapOf<GameState, Boolean>()
    private var framesTilNextModeSwitch = 0
    private const val MAX_FRAMES_TIL_NEXT_MODE_SWITCH = 60
    private const val INITIAL_WINDOW_WIDTH = 800
    private const val INITIAL_WINDOW_HEIGHT = 600
    private const val WINDOW_TITLE = "Game Engine"
    private const val TICK_RATE = 250f
    private const val FIXED_DELTA_TIME = 1.0f / TICK_RATE
    private var accumulator = 0.0f

    private var currentLevelPath = "levels/level1.amlevel"

    private const val MAX_DELTA_TIME = 0.1f
    private val SPAWN_POINT = Vector3f(0f, 1f, 0f)

    private lateinit var shaderProgram: ShaderProgram
    private lateinit var debugShaderProgram: ShaderProgram
    private lateinit var hudShaderProgram: ShaderProgram

    private var hudState: HUDState = HUDState()
    private lateinit var hudManager: HudManager

    private lateinit var playerCamera: Camera
    private lateinit var editCamera: FreeFlyCamera
    private lateinit var physicsEngine: PhysicsEngine

    private lateinit var player: Player
    private val gameObjects = mutableListOf<GameObject>()
    private val aiControllers = mutableListOf<AIController>()

    private lateinit var wheelMesh: Mesh
    private lateinit var cubeMesh: Mesh
    private lateinit var checkpointMesh: Mesh

    private lateinit var raceManager: RaceManager

    private lateinit var groundTexture: Texture
    private lateinit var defaultTexture: Texture

    private val projectionMatrix = Matrix4f()
    private val orthologicalMatrix = Matrix4f()
    private var lastFrameTime = 0.0

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
        hudShaderProgram = ShaderProgram("shaders/hud.vert", "shaders/hud.frag")

        setupMatrices()

        loadLevel(currentLevelPath)

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
        gameStates[GameState.PLAY] = true
        editCamera = FreeFlyCamera(Vector3f(0f, 10f, 0f))
    }

    private fun loadLevel(levelPath: String){
        physicsEngine = PhysicsEngine()

        wheelMesh = AssetManager.getMesh("models/wheel.ammodel")
        val chassisMesh = AssetManager.getMesh("models/car.ammodel")
        val groundMesh = defineGround()
        cubeMesh = AssetManager.getMesh("models/cube.ammodel")

        groundTexture = Texture("textures/grass.png")
        defaultTexture = Texture("textures/default.png")

        val playerVehicle = VehicleFactory.createVehicle(
            "Player", Transform(SPAWN_POINT), chassisMesh, wheelMesh)
        val playerGameObject = playerVehicle.gameObject
        hudManager = HudManager(hudState, hudShaderProgram, orthologicalMatrix, defaultTexture)

        val aiPos1 = Transform(position = Vector3f(5f, 1f, -5f))
        val aiPos2 = Transform(position = Vector3f(-5f, 1f, -5f))
        val aiPos3 = Transform(position = Vector3f(0f, 1f, -10f))
        val aiTransforms = listOf(aiPos1, aiPos2, aiPos3)
        val aiVehicles = mutableListOf<Vehicle>()
        for (i in 0 until aiTransforms.size) {
            val aiVehicle = VehicleFactory.createVehicle("AI-${i}", aiTransforms[i], chassisMesh, wheelMesh)
            val aiObject = aiVehicle.gameObject
            aiVehicles.add(aiVehicle)
            gameObjects.add(aiObject)
        }

        player = Player(playerVehicle)
        gameObjects.add(playerGameObject)

        val ground = createGameObject(
            "Ground",
            groundMesh,
            isStatic = true,
            position=Vector3f(0f,0f,0f),
            boundingBoxSize=null,
            null)

        val levelObjects = AssetManager.getLevelData(levelPath)
        gameObjects.addAll(levelObjects)
        physicsEngine.addWorldObjects(*levelObjects.toTypedArray(), ground)

        raceManager = RaceManager(aiVehicles + playerVehicle, gameObjects)
        for(ai in aiVehicles){
            val aiController = AIController(ai, raceManager)
            aiControllers.add(aiController)
        }

        physicsEngine.addObjects(*gameObjects.toTypedArray())

        playerCamera = Camera(playerGameObject.transform, distance = 12.0f)
    }

    private fun clearWorld(){
        physicsEngine.clear()
        aiControllers.clear()
        gameObjects.clear()
    }

    private fun loop(){
        glClearColor(0.5f, 0.5f, 0.9f, 0.0f)


        while(!glfwWindowShouldClose(window)){
            val currentFrameTime = glfwGetTime()
            var frameTime = (currentFrameTime - lastFrameTime).toFloat()
            if(frameTime > 0.25f) frameTime = 0.25f
            lastFrameTime = currentFrameTime
            accumulator += frameTime

            handleGlobalInput()
            val playerInput = getPlayerInput()
            val editorInput = getEditorInput()

            if(gameStates[GameState.EDITOR] == true){
                val (reloadMeshes, reloadLevels) = AssetManager.update()
                if(reloadLevels.contains(currentLevelPath)){
                    clearWorld()
                    loadLevel(currentLevelPath)
                }

                if(reloadMeshes.isNotEmpty()){
                    for(gameObject in gameObjects){
                        gameObject.updateMesh(reloadMeshes)
                    }
                }
            }

            while(accumulator >= FIXED_DELTA_TIME) {
                if(gameStates[GameState.PLAY] == true) {
                    player.update(FIXED_DELTA_TIME, playerInput)
                    for (ai in aiControllers) {
                        ai.update(FIXED_DELTA_TIME)
                    }
                    physicsEngine.update(FIXED_DELTA_TIME)

                    raceManager.update()
                    if(!raceManager.raceState.inProgress){
                        gameStates[GameState.RACE_OVER] = true
                    }

                } else if (gameStates[GameState.EDITOR] == true){
                    editCamera.update(FIXED_DELTA_TIME, editorInput)
                }
                accumulator -= FIXED_DELTA_TIME
            }

            for(gameObject in gameObjects) {
                gameObject.update()
            }

            when (player.vehicle.gameObject.physicsBody?.isRespawning) {
                true -> playerCamera.reset()
                false, null -> playerCamera.update(frameTime, player.vehicle.gameObject.physicsBody!!.isGrounded)

            }

            renderScene()
            if(gameStates[GameState.PLAY] == true) renderHUD()
            if(gameStates[GameState.DEBUG] == true) renderDebugVisuals()

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
        hudManager.cleanup()

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

        viewMatrix = if(gameStates[GameState.PLAY] == true) playerCamera.getViewMatrix()
        else editCamera.getViewMatrix()

        shaderProgram.setUniform("projection", projectionMatrix)
        shaderProgram.setUniform("view", viewMatrix)
        shaderProgram.setUniform("textureSampler", 0)

        for(gameObject in gameObjects){
            if(gameObject.id == "Ground"){
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
        viewMatrix = if(gameStates[GameState.PLAY] == true) playerCamera.getViewMatrix()
        else editCamera.getViewMatrix()

        debugShaderProgram.setUniform("projection", projectionMatrix)
        debugShaderProgram.setUniform("view", viewMatrix)
        for(debugObject in gameObjects){
            debugObject.physicsBody?.let{ body ->
                val hitbox = body.boundingBox

                val debugModelMatrix = Matrix4f()
                    .translate(hitbox.transform.position)
                    .rotate(hitbox.transform.orientation)
                    .scale(hitbox.size)
                debugShaderProgram.setUniform("debugColor", Vector3f(1f, 1f, 0f))
                debugShaderProgram.setUniform("model", debugModelMatrix)
                cubeMesh.draw()
            }
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

    private fun renderHUD(){
        val playerPhysicsBody = player.vehicle.gameObject.physicsBody
        val v = playerPhysicsBody?.velocity ?: Vector3f()
        hudState.speedKPH = sqrt(v.x * v.x + v.z * v.z).toInt()
        hudState.healthPercentage = player.vehicle.currentHealth / player.vehicle.MAX_HEALTH
        hudState.stylePercentage = player.vehicle.currentStylePoints / player.vehicle.MAX_STYLEPOINTS

        val playerRaceState = raceManager.getRacerState(player.vehicle)
        hudState.currentLap = playerRaceState?.currentLap ?: 1
        hudState.totalLaps = raceManager.totalLaps

        hudState.currentPos = raceManager.getRacerPosition(player.vehicle)
        hudState.totalRacers = raceManager.racers.size
        hudState.eliminatedRacers = hudState.totalRacers - raceManager.raceState.leaderboard.size

        hudManager.draw()
    }

    private fun drawGameObject(gameObject: GameObject){
        shaderProgram.setUniform("model", gameObject.globalMatrix)
        gameObject.mesh.draw()
        for(child in gameObject.children){
            drawGameObject(child)
        }
    }

    private fun handleGlobalInput(){

        if (framesTilNextModeSwitch == 0 && glfwGetKey(window, GLFW_KEY_F1) == GLFW_PRESS){
            if(gameStates[GameState.PLAY] == true){
                gameStates[GameState.PLAY] = false
                gameStates[GameState.EDITOR] = true
            } else{
                gameStates[GameState.PLAY] = true
                gameStates[GameState.EDITOR] = false
            }
            framesTilNextModeSwitch = MAX_FRAMES_TIL_NEXT_MODE_SWITCH
        }

        if(glfwGetKey(window, GLFW_KEY_F2) == GLFW_PRESS){
            gameStates[GameState.DEBUG] = gameStates[GameState.DEBUG] != true
        }

        if (framesTilNextModeSwitch > 0) framesTilNextModeSwitch--
    }

    private fun getPlayerInput(): PlayerInput{
        return PlayerInput(
            glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS,
        )
    }

    private fun getEditorInput(): CameraInput{
        val isDragging = glfwGetMouseButton(window,GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        return CameraInput(
            glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS,
            glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS,
            mouseDeltaX = if (isDragging) mouseDeltaX else 0.0f,
            mouseDeltaY = if (isDragging) mouseDeltaY else 0.0f
        )
    }

    private fun setupMatrices(){
        val aspectRatio = INITIAL_WINDOW_WIDTH.toFloat() / INITIAL_WINDOW_HEIGHT.toFloat()
        projectionMatrix.perspective(Math.toRadians(45.0).toFloat(), aspectRatio, 0.1f, 1000.0f)
        orthologicalMatrix.ortho(0.0f, INITIAL_WINDOW_WIDTH.toFloat(), 0.0f, INITIAL_WINDOW_HEIGHT.toFloat(), -1.0f, 1.0f)
    }

    private fun createGameObject (
        id: String,
        mesh: Mesh,
        isStatic: Boolean,
        position: Vector3f,
        boundingBoxSize: Vector3f?,
        waypoint: Waypoint?
    ): GameObject{
        val objectTransform = Transform(position)
        val gameObject = GameObject(id, transform=objectTransform, mesh=mesh, waypoint=waypoint)
        if(boundingBoxSize != null) {
            val objectBoundingBox = OrientedBoundingBox(objectTransform, boundingBoxSize)
            val objectBody = PhysicsBody(objectBoundingBox, isStatic)
            gameObject.physicsBody = objectBody
        }
        gameObjects.add(gameObject)
        return gameObject
    }

    private fun createCheckpoint(index: Int, position: Vector3f): GameObject{
        val checkpoint = Waypoint(WaypointType.RACE_CHECKPOINT, index)
        val result =  createGameObject(
            "Checkpoint$index",
            checkpointMesh,
            isStatic = true,
            position=position,
            boundingBoxSize = null,
            waypoint=checkpoint)
        return result
    }

    private fun defineGround(): Mesh{
        val groundVertices = floatArrayOf(
            // Positions          // Colors (tint)     // Texture Coords (UVs)
            -500.0f, 0f, -500.0f,  1.0f, 1.0f, 1.0f,   0.0f, 25.0f,
            500.0f, 0f, -500.0f,   1.0f, 1.0f, 1.0f,  25.0f, 25.0f,
            500.0f, 0f,  500.0f,   1.0f, 1.0f, 1.0f,  25.0f, 0.0f,

            500.0f, 0f,  500.0f,   1.0f, 1.0f, 1.0f,  25.0f, 0.0f,
            -500.0f, 0f,  500.0f,  1.0f, 1.0f, 1.0f,   0.0f, 0.0f,
            -500.0f, 0f, -500.0f,  1.0f, 1.0f, 1.0f,   0.0f, 25.0f
        )
        return Mesh(null, groundVertices)
    }

}