package org.ammck.engine.objects

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.ammck.engine.render.Mesh
import org.ammck.util.FileUtil

@Serializable
private data class PolygonData(
    val description: String,
    val color: List<Int>,
    val vertices: List<List<Float>>
)

@Serializable
private data class ModelData(
    val name: String,
    val polygons: List<PolygonData>
)

object ModelLoader {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun load(resourcePath: String): Mesh {
        val fileContent = FileUtil.readResourceFile(resourcePath)
        val modelData = jsonParser.decodeFromString<ModelData>(fileContent)

        val vertexList = mutableListOf<Float>()
        for(polygon in modelData.polygons){
            val r = polygon.color[0] / 255f
            val g = polygon.color[1] / 255f
            val b = polygon.color[2] / 255f
            val colorData = listOf(r, g, b)

            //triangulate the polygons
            val v1 = polygon.vertices[0]
            for(i in 1 until polygon.vertices.size -1){
                val v2 = polygon.vertices[i]
                val v3 = polygon.vertices[i+1]

                vertexList.addAll(v1)
                vertexList.addAll(colorData)
                vertexList.addAll(v2)
                vertexList.addAll(colorData)
                vertexList.addAll(v3)
                vertexList.addAll(colorData)
            }
        }
        return Mesh(vertexList.toFloatArray())
    }
}