package org.ammck.engine.assets

import org.ammck.engine.objects.ModelLoader
import org.ammck.engine.render.Mesh
import org.ammck.util.FileUtil
import java.io.File

object AssetManager {

    private val meshCache = mutableMapOf<String, Asset<Mesh>>()

    fun getMesh(resourcePath: String): Mesh{
        val cachedAsset = meshCache[resourcePath]

        if(cachedAsset == null){
            val mesh = ModelLoader.load(resourcePath)
            val file = FileUtil.getResourceFile(resourcePath)
            meshCache[resourcePath] = Asset(resourcePath, mesh, file)
            return mesh
        } else{
            return cachedAsset.data
        }
    }

    fun update(): List<String>{
        val reloadPaths = mutableListOf<String>()
        for((path, asset) in meshCache){
            if(asset.hasBeenModified()){
                println("Last modified changed!")
                asset.data.cleanup()
                val newMesh = ModelLoader.load(path)
                asset.data = newMesh
                asset.updateTimestamp()
                reloadPaths.add(path)
            }
        }
        return reloadPaths
    }

    fun cleanup(){
        meshCache.values.forEach { it.data.cleanup() }
    }
}