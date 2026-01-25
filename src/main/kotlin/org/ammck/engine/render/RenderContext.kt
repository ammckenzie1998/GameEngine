package org.ammck.engine.render

import org.lwjgl.opengl.GL11.GL_BACK
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_CULL_FACE
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_FILL
import org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL11.GL_ONE
import org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL
import org.lwjgl.opengl.GL11.GL_ZERO
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glPolygonMode
import org.lwjgl.opengl.GL11.glPolygonOffset
import kotlin.PublishedApi

object RenderContext {
    @PublishedApi internal var isBlendEnabled = false
    @PublishedApi internal var isCullFaceEnabled = false
    @PublishedApi internal var isDepthTestEnabled = false
    @PublishedApi internal var isDepthMaskEnabled = true
    @PublishedApi internal var isPolygonOffsetFillEnabled = false

    @PublishedApi internal var currentPolygonMode = GL_FILL
    @PublishedApi internal var currentBlendSrc = GL_ONE
    @PublishedApi internal var currentBlendDst = GL_ZERO
    @PublishedApi internal var currentCullFaceMode = GL_BACK
    @PublishedApi internal var currentOffsetFactor = 0.0f
    @PublishedApi internal var currentOffsetUnits = 0.0f

    fun init(){
        if (isDepthTestEnabled) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (isCullFaceEnabled) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        if (isBlendEnabled) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        if (isPolygonOffsetFillEnabled) glEnable(GL_POLYGON_OFFSET_FILL) else glDisable(GL_POLYGON_OFFSET_FILL)
        glDepthMask(isDepthMaskEnabled)

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        currentPolygonMode = GL_FILL
        glBlendFunc(currentBlendSrc, currentBlendDst)
        glCullFace(currentCullFaceMode)
        glPolygonOffset(currentOffsetFactor, currentOffsetUnits)
    }

    fun enableDepthTest(enable: Boolean){
        if (isDepthTestEnabled != enable){
            isDepthTestEnabled = enable
            if (enable) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        }
    }

    fun enableDepthMask(enable: Boolean){
        if (isDepthMaskEnabled != enable){
            isDepthMaskEnabled = enable
            glDepthMask(enable)
        }
    }

    fun enableCullFace(enable: Boolean){
        if (isCullFaceEnabled != enable){
            isCullFaceEnabled = enable
            if (enable) glEnable(GL_CULL_FACE) else glDisable(GL_CULL_FACE)
        }
    }

    fun enableBlend(enable: Boolean){
        if (isBlendEnabled != enable){
            isBlendEnabled = enable
            if (enable) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        }
    }

    fun enablePolygonOffsetFill(enable: Boolean){
        if (isPolygonOffsetFillEnabled != enable){
            isPolygonOffsetFillEnabled = enable
            if (enable) glEnable(GL_POLYGON_OFFSET_FILL) else glDisable(GL_POLYGON_OFFSET_FILL)
        }
    }

    fun setBlendFunc(sFactor: Int, dFactor: Int){
        if (currentBlendSrc != sFactor || currentBlendDst != dFactor){
            currentBlendSrc = sFactor
            currentBlendDst = dFactor
            glBlendFunc(sFactor, dFactor)
        }
    }

    fun setCullFaceMode(mode: Int){
        if (currentCullFaceMode != mode){
            currentCullFaceMode = mode
            glCullFace(mode)
        }
    }

    fun setPolygonOffset(factor: Float, units: Float){
        if (currentOffsetFactor != factor || currentOffsetUnits != units){
            currentOffsetFactor = factor
            currentOffsetUnits = units
            glPolygonOffset(factor, units)
        }
    }

    inline fun withState(
        depthTest: Boolean? = null,
        depthMask: Boolean? = null,
        cullFace: Boolean? = null,
        cullMode: Int? = null,
        blend: Boolean? = null,
        polygonOffset: Boolean? = null,
        block: () -> Unit
    ){
        val prevDepthTest = if(depthTest != null) isDepthTestEnabled else false
        val prevDepthMask = if(depthMask != null) isDepthMaskEnabled else false
        val prevCullFace = if(cullFace != null) isCullFaceEnabled else false
        val prevCullMode = if(cullMode != null) currentCullFaceMode else GL_BACK
        val prevBlend = if(blend != null) isBlendEnabled else false
        val prevPolyOffset = if(polygonOffset != null) isPolygonOffsetFillEnabled else false

        if (depthTest != null) enableDepthTest(depthTest)
        if (depthMask != null) enableDepthMask(depthMask)
        if (cullFace != null) enableCullFace(cullFace)
        if (cullMode != null) setCullFaceMode(cullMode)
        if (blend != null) enableBlend(blend)
        if (polygonOffset != null) enablePolygonOffsetFill(polygonOffset)

        try{
            block()
        } finally{
            if (depthTest != null) enableDepthTest(prevDepthTest)
            if (depthMask != null) enableDepthMask(prevDepthMask)
            if (cullFace != null) enableCullFace(prevCullFace)
            if (cullMode != null) setCullFaceMode(prevCullMode)
            if (blend != null) enableBlend(prevBlend)
            if (polygonOffset != null) enablePolygonOffsetFill(prevPolyOffset)
        }
    }

}