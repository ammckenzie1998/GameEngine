package org.ammck.util

import java.io.File
import java.nio.ByteBuffer

object FileUtil {

    fun readResourceAsString(path: String): String{
        return readResourceFile(path).readText()
    }

    fun readResourceAsByteBuffer(path: String): ByteBuffer{
        val file = readResourceFile(path)
        val bytes = file.readBytes()
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes).flip()
        return buffer
    }

    private fun readResourceFile(path: String): File{
        val resource = this::class.java.classLoader.getResource(path)
            ?: throw RuntimeException("Can't find resource file: $path")
        return File(resource.toURI())
    }

}