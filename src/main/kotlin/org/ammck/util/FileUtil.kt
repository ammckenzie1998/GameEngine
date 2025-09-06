package org.ammck.util

import java.io.File

object FileUtil {

    fun readResourceFile(path: String): String{
        val resource = this::class.java.classLoader.getResource(path)
            ?: throw RuntimeException("Can't find resource file: $path")
        return File(resource.toURI()).readText()
    }

}