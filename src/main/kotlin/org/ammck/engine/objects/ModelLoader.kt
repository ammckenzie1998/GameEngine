package org.ammck.engine.objects

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.ammck.engine.Transform
import org.ammck.engine.render.Mesh
import org.ammck.util.FileUtil
import org.ammck.util.MathUtil
import org.joml.Quaternionf
import org.joml.Vector3f

@Serializable
private data class PolygonData(
    val description: String,
    val color: List<Int>,
    val vertices: List<List<Float>>,
    val texCoords: List<List<Float>>? = null
)

@Serializable
private data class AttachmentPointData(
    val position: List<Float>,
    val rotation: List<Float>,
    val scale: List<Float>
)

@Serializable
private data class ModelData(
    val name: String,
    val scale: Float = 0.01f,
    val polygons: List<PolygonData>,
    val attachmentPoints: Map<AttachmentType, AttachmentPointData>? = null
)

object ModelLoader {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        decodeEnumsCaseInsensitive = true
    }

    fun load(resourcePath: String): Model {
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

                vertexList.addAll(MathUtil.scaleFloats(modelData.scale,v1))
                vertexList.addAll(colorData)
                vertexList.addAll(t1)
                vertexList.addAll(MathUtil.scaleFloats(modelData.scale,v2))
                vertexList.addAll(colorData)
                vertexList.addAll(t2)
                vertexList.addAll(MathUtil.scaleFloats(modelData.scale,v3))
                vertexList.addAll(colorData)
                vertexList.addAll(t3)
            }
        }

        val mesh = Mesh(resourcePath, vertexList.toFloatArray())

        val attachmentPoints = mutableMapOf<AttachmentType, Transform>()
        if(modelData.attachmentPoints != null) {

            for (ap in modelData.attachmentPoints) {
                val type = ap.key
                val pos = ap.value.position
                val positionVector = Vector3f(pos[0], pos[1], pos[2])
                    .mul(modelData.scale)
                val rotation = ap.value.rotation
                val orientation = Quaternionf()
                    .rotateX(Math.toRadians(rotation[0].toDouble()).toFloat())
                    .rotateY(Math.toRadians(rotation[1].toDouble()).toFloat())
                    .rotateZ(Math.toRadians(rotation[2].toDouble()).toFloat())
                val scale = ap.value.scale
                val scaleVector = Vector3f(scale[0], scale[1], scale[2])
                val transform = Transform(positionVector, orientation, scaleVector)
                attachmentPoints[type] = transform
            }
        }

        return Model(mesh, attachmentPoints)
    }
}