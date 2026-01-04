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
import java.util.Vector

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

private data class ObjVertexData(
    val position: List<Float>,
    val uv: List<Float>,
    val normal: List<Float>
)

object ModelLoader {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        decodeEnumsCaseInsensitive = true
    }

    fun load(resourcePath: String): Model {
        return if(resourcePath.endsWith(".obj", ignoreCase = true)){
            loadObj(resourcePath)
        } else {
            loadJson(resourcePath)
        }
    }

    private fun loadObj(resourcePath: String): Model{
        val fileContent = FileUtil.readResourceAsString(resourcePath)
        val fileLines = fileContent.lines()
        val parentPath = resourcePath.substringBeforeLast('/', "")

        val rawVertices = mutableListOf<List<Float>>()
        val rawTexCoords = mutableListOf<List<Float>>()
        val rawNormals = mutableListOf<List<Float>>()
        val vertexList = mutableListOf<Float>()

        val socketData = mutableMapOf<String, MutableList<ObjVertexData>>()
        var currentObjectName = "default"
        var isSocketObject = false

        val defaultColor = listOf(1f, 1f, 1f)
        var texturePath: String? = null

        for(line in fileLines){
            val tokens = line.trim().split("\\s+".toRegex())
            if(tokens.isEmpty()) continue

            when (tokens[0]){
                "mtllib" -> {
                    val mtlFileName = tokens[1]
                    val mtlFullPath = if(parentPath.isEmpty()) mtlFileName else "$parentPath/$mtlFileName"

                    val textureFileName = parseTextureFromMtl(mtlFullPath)

                    if(textureFileName != null){
                        texturePath = if(parentPath.isEmpty()) textureFileName else "$parentPath/$textureFileName"
                    }
                }
                "o" -> {
                    //Object ObjectName
                    currentObjectName = tokens[1]
                    isSocketObject = currentObjectName.startsWith("SOCKET_", ignoreCase = true)

                    if (isSocketObject && !socketData.containsKey(currentObjectName)){
                        socketData[currentObjectName] = mutableListOf()
                    }
                }
                "v" -> {
                    //Vertex v 1.0 1.0 1.0
                    rawVertices.add(listOf(
                        tokens[1].toFloat(),
                        tokens[2].toFloat(),
                        tokens[3].toFloat()
                    ))
                }
                "vt" -> {
                    //Texture vt 0.5 0.5
                    rawTexCoords.add(listOf(
                        tokens[1].toFloat(),
                        tokens[2].toFloat()
                    ))
                }
                "vn" -> {
                    rawNormals.add(listOf(
                        tokens[1].toFloat(),
                        tokens[2].toFloat(),
                        tokens[3].toFloat()
                    ))
                }
                "f" -> {
                    //Face f v1/vt1 v2/vt2 v3/vt3
                    val parts = tokens.drop(1)
                    val v1 = parseObjVertex(parts[0], rawVertices, rawTexCoords, rawNormals)

                    for ( i in 1 until parts.size - 1){
                        val v2 = parseObjVertex(parts[i], rawVertices, rawTexCoords, rawNormals)
                        val v3 = parseObjVertex(parts[i+1], rawVertices, rawTexCoords, rawNormals)

                        if (isSocketObject){
                            socketData[currentObjectName]?.add(v1)
                            socketData[currentObjectName]?.add(v2)
                            socketData[currentObjectName]?.add(v3)
                        } else{
                            vertexList.addAll(v1.position)
                            vertexList.addAll(defaultColor)
                            vertexList.addAll(v1.uv)
                            vertexList.addAll(v1.normal)

                            vertexList.addAll(v2.position)
                            vertexList.addAll(defaultColor)
                            vertexList.addAll(v2.uv)
                            vertexList.addAll(v2.normal)

                            vertexList.addAll(v3.position)
                            vertexList.addAll(defaultColor)
                            vertexList.addAll(v3.uv)
                            vertexList.addAll(v3.normal)
                        }
                    }
                }
            }
        }
        val mesh = Mesh(resourcePath, vertexList.toFloatArray())

        val attachmentPoints = convertSocketsToAttachmentPoints(socketData)

        return Model(mesh, attachmentPoints, texturePath)
    }

    private fun parseObjVertex(
        token: String,
        vertices: List<List<Float>>,
        uvs: List<List<Float>>,
        normals: List<List<Float>>
    ): ObjVertexData{
        val indices = token.split("/")

        val vIndex = indices[0].toInt() - 1
        val position = vertices[vIndex]

        val uv = if (indices.size > 1 && indices[1].isNotEmpty()){
            val tIndex = indices[1].toInt() - 1
            val rawUv = uvs[tIndex]
            listOf(rawUv[0], rawUv[1])
        } else {
            listOf(0f, 0f)
        }

        val normal = if (indices.size > 2 && indices[2].isNotEmpty()){
            val nIndex = indices[2].toInt() - 1
            normals[nIndex]
        } else{
            listOf(0f, 1f, 0f)
        }

        return ObjVertexData(position, uv, normal)
    }

    private fun parseTextureFromMtl(mtlPath: String): String?{
        return try{
            val content = FileUtil.readResourceAsString(mtlPath)
            content.lines().forEach { line ->
                val tokens = line.trim().split("\\s+".toRegex())
                if (tokens.isNotEmpty() && tokens[0] == "map_Kd"){
                    return tokens[1]
                }
            }
            null
        } catch(e: Exception){
            null
        }
    }

    private fun convertSocketsToAttachmentPoints(
        socketData: Map<String, List<ObjVertexData>>
    ): Map<AttachmentType, Transform>{
        val attachmentPoints = mutableMapOf<AttachmentType, Transform>()

        for ((name, vertices) in socketData){
            if (vertices.isEmpty()) continue

            val avgPos = Vector3f(0f, 0f, 0f)
            val avgNormal = Vector3f(0f, 0f, 0f)

            vertices.forEach { v ->
                avgPos.add(v.position[0], v.position[1], v.position[2])
                avgNormal.add(v.normal[0], v.normal[1], v.normal[2])
            }
            avgPos.div(vertices.size.toFloat())
            if (avgNormal.lengthSquared() > 0){
                avgNormal.normalize()
            } else{
                avgNormal.set(0f, 1f, 0f)
            }

            val rotation = Quaternionf().lookAlong(avgNormal, Vector3f(0f, 1f, 0f))

            val typeName = name.removePrefix("SOCKET_")
            try{
                val type = AttachmentType.entries.find { it.name.equals(typeName, ignoreCase = true )}
                if(type != null){
                    attachmentPoints[type] = Transform(avgPos, rotation, Vector3f(0.1f))
                }
            } catch(e: Exception){
                println("WARNING - Could not map socket object '$name' to an attachment type")
            }
        }
        return attachmentPoints
    }

    private fun loadJson(resourcePath: String): Model {
        val fileContent = FileUtil.readResourceAsString(resourcePath)
        val modelData = jsonParser.decodeFromString<ModelData>(fileContent)
        val vertexList = mutableListOf<Float>()

        val defaultNormal = listOf(0f, 1f, 0f)

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
                vertexList.addAll(defaultNormal)
                vertexList.addAll(MathUtil.scaleFloats(modelData.scale,v2))
                vertexList.addAll(colorData)
                vertexList.addAll(t2)
                vertexList.addAll(defaultNormal)
                vertexList.addAll(MathUtil.scaleFloats(modelData.scale,v3))
                vertexList.addAll(colorData)
                vertexList.addAll(t3)
                vertexList.addAll(defaultNormal)
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