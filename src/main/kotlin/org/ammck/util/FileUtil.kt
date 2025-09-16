package org.ammck.util

import java.io.File
import java.net.URL
import java.nio.ByteBuffer

object FileUtil {

    fun readResourceAsString(path: String): String{
        return getResourceFile(path).readText()
    }

    fun readResourceAsByteBuffer(path: String): ByteBuffer{
        val file = getResourceFile(path)
        val bytes = file.readBytes()
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes).flip()
        return buffer
    }

    fun getResourceUrl(path: String): URL {
        return this::class.java.classLoader.getResource(path)
            ?: throw RuntimeException("Can't find resource file: $path")
    }

    fun getResourceFile(path: String): File{
        val resource = getResourceUrl(path)
        return File(resource.toURI())
    }

}