package org.ammck.engine.assets

import org.ammck.engine.objects.GameObject
import org.ammck.engine.objects.LevelLoader
import org.ammck.engine.objects.ModelLoader
import org.ammck.engine.render.Mesh
import org.ammck.util.FileUtil
import java.io.File

object AssetManager {

    private val meshCache = mutableMapOf<String, Asset<Mesh>>()
    private val levelCache = mutableMapOf<String, Asset<List<GameObject>>>()

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

    fun getLevelData(resourcePath: String): List<GameObject>{
        val cachedAsset = levelCache[resourcePath]

        if(cachedAsset == null){
            val levelData = LevelLoader.load(resourcePath)
            val file = FileUtil.getResourceFile(resourcePath)
            levelCache[resourcePath] = Asset(resourcePath, levelData, file)
            return levelData
        } else{
            return cachedAsset.data
        }
    }

    fun update(): Pair<List<String>, List<String>>{
        val reloadMeshes = mutableListOf<String>()
        val reloadLevels = mutableListOf<String>()
        for((path, asset) in meshCache){
            if(asset.hasBeenModified()){
                asset.data.cleanup()
                val newMesh = ModelLoader.load(path)
                asset.data = newMesh
                asset.updateTimestamp()
                reloadMeshes.add(path)
            }
        }
        for((path, asset) in levelCache){
            if(asset.hasBeenModified()){
                val newLevelData = LevelLoader.load(path)
                asset.data = newLevelData
                asset.updateTimestamp()
                reloadLevels.add(path)
            }
        }
        return Pair(reloadMeshes, reloadLevels)
    }

    fun cleanup(){
        meshCache.values.forEach { it.data.cleanup() }
    }
}