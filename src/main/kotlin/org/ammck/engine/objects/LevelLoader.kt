package org.ammck.engine.objects

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ammck.engine.Transform
import org.ammck.engine.assets.AssetManager
import org.ammck.engine.physics.OrientedBoundingBox
import org.ammck.engine.physics.PhysicsBody
import org.ammck.engine.render.Mesh
import org.ammck.game.WaypointType
import org.ammck.games.Waypoint
import org.ammck.util.FileUtil
import org.joml.Quaternionf
import org.joml.Vector3f

@Serializable
private data class LevelData(
    val name: String,
    val gameObjects: List<ObjectData>
)

@Serializable
private data class ObjectData(
    val name: String,
    val mesh: String,
    val positions: List<List<Double>>,
    val isSolid: Boolean = true
)


object LevelLoader {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun load(resourcePath: String): List<GameObject>{
        val fileContent = FileUtil.readResourceAsString(resourcePath)
        val levelData = jsonParser.decodeFromString<LevelData>(fileContent)
        val objectList = mutableListOf<GameObject>()

        for (goData in levelData.gameObjects){
            val mesh = AssetManager.getMesh(goData.mesh)
            var index = 0
            for (pos in goData.positions){
                val x = pos[0].toFloat()
                val y = pos[1].toFloat()
                val z = pos[2].toFloat()
                val angle = pos[3]
                val objectTransform = Transform(
                    position=Vector3f(x, y, z),
                    orientation = Quaternionf().rotateY(Math.toRadians(angle).toFloat()))

                val waypoint = if(goData.name == "Checkpoint")
                    Waypoint(WaypointType.RACE_CHECKPOINT, index)
                else null

                val boundingBox = OrientedBoundingBox(objectTransform, mesh.upperBounds)
                val physicsBody = PhysicsBody(boundingBox, isStatic = true)

                val gameObject = GameObject(
                    id = goData.name,
                    transform = objectTransform,
                    model = mesh,
                    physicsBody = physicsBody,
                    waypoint = waypoint,
                    isSolid = goData.isSolid
                )
                objectList.add(gameObject)
                index++
            }
        }
        return objectList
    }
}