package com.choota.googlefilamenttry

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.view.Choreographer
import android.view.SurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelRenderer {
    private lateinit var surfaceView: SurfaceView
    private lateinit var lifecycle: Lifecycle
    private lateinit var engine: Engine

    private lateinit var choreographer: Choreographer
    private lateinit var uiHelper: UiHelper

    private lateinit var modelViewer: ModelViewer

    private lateinit var assets: AssetManager

    private val frameScheduler = FrameCallback()

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            choreographer.postFrameCallback(frameScheduler)
        }

        override fun onPause(owner: LifecycleOwner) {
            choreographer.removeFrameCallback(frameScheduler)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            choreographer.removeFrameCallback(frameScheduler)
            lifecycle.removeObserver(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun onSurfaceAvailable(surfaceView: SurfaceView, lifecycle: Lifecycle) {
        this.surfaceView = surfaceView
        this.lifecycle = lifecycle
        this.engine = Engine.create()

        assets = surfaceView.context.assets

        lifecycle.addObserver(lifecycleObserver)
        choreographer = Choreographer.getInstance()
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            // This is needed to make the background transparent
            isOpaque = false
        }

        modelViewer = ModelViewer(surfaceView = surfaceView, uiHelper = uiHelper)

        // This is needed so we can move the camera in the rendering
        surfaceView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)
            true
        }

        // This is the other code needed to make the background transparent
        modelViewer.view.blendMode = View.BlendMode.OPAQUE
        modelViewer.scene.skybox = null
        modelViewer.renderer.clearOptions = modelViewer.renderer.clearOptions.apply {
            clear = false
        }


        // This part defines the quality of your model, feel free to change it or
        // add other options
        modelViewer.view.apply {
            renderQuality = renderQuality.apply {
                hdrColorBuffer = View.QualityLevel.HIGH
            }
        }

        createRenderables()
    }

    private fun createRenderables() {
        val buffer = assets.open("models/bukit_pengembara.glb").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }

        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(frameTimeNanos)
        }
    }
}