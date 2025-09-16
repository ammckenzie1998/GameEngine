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
    val vertices: List<List<Float>>,
    val texCoords: List<List<Float>>? = null
)

@Serializable
private data class ModelData(
    val name: String,
    val polygons: List<PolygonData>
)

object ModelLoader {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun load(resourcePath: String): Mesh {
        val fileContent = FileUtil.readResourceAsString(resourcePath)
        val modelData = jsonParser.decodeFromString<ModelData>(fileContent)
        val vertexList = mutableListOf<Float>()

        for(polygon in modelData.polygons){
            val r = polygon.color[0] / 255f
            val g = polygon.color[1] / 255f
            val b = polygon.color[2] / 255f
            val colorData = listOf(r,g,b)

            val v1 = polygon.vertices[0]
            val t1 = polygon.texCoords?.get(0) ?: listOf(0.0f, 0.0f)

            for(i in 1 until polygon.vertices.size -1){
                val v2 = polygon.vertices[i]
                val t2 = polygon.texCoords?.get(i) ?: listOf(0.0f, 0.0f)
                val v3 = polygon.vertices[i+1]
                val t3 = polygon.texCoords?.get(i+1) ?: listOf(0.0f, 0.0f)

                vertexList.addAll(v1)
                vertexList.addAll(colorData)
                vertexList.addAll(t1)
                vertexList.addAll(v2)
                vertexList.addAll(colorData)
                vertexList.addAll(t2)
                vertexList.addAll(v3)
                vertexList.addAll(colorData)
                vertexList.addAll(t3)
            }
        }
        return Mesh(resourcePath, vertexList.toFloatArray())
    }
}