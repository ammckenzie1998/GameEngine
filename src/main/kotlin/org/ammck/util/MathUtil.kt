package org.ammck.util

object MathUtil {

    fun scaleFloats(scaleFactor: Float, floats: List<Float>): List<Float>{
        val result = mutableListOf<Float>()
        for(float in floats){
            result.add(float * scaleFactor)
        }
        return result
    }

}