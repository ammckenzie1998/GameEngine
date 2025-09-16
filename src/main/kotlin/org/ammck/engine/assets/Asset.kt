package org.ammck.engine.assets

import java.io.File

data class Asset<T>(
    val path: String,
    var data: T,
    private val file: File
){
    private var lastModified: Long = file.lastModified()

    fun hasBeenModified(): Boolean{
        return file.lastModified() > this.lastModified
    }

    fun updateTimestamp(){
        this.lastModified = file.lastModified()
    }
}
