/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.takusan23.zeromirror.media.opengl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InputSurface(
    private val surface: Surface,
    private val textureRenderer: TextureRenderer
) : SurfaceTexture.OnFrameAvailableListener {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLSurface = EGL14.EGL_NO_SURFACE
    private var surfaceTexture: SurfaceTexture? = null

    /** MediaCodecのデコーダーSurfaceとしてこれを使う */
    var drawSurface: Surface? = null
        private set

    /**
     * [SurfaceTexture.OnFrameAvailableListener.onFrameAvailable] と [awaitIsNewFrameAvailable] からそれぞれ別スレッドで [isNewFrameAvailable] にアクセスするため、
     * 同時アクセスできないように制御する [Mutex]。
     */
    private val frameSyncMutex = Mutex()

    /** 新しい映像フレームが来ていれば true */
    private var isNewFrameAvailable = false

    init {
        eglSetup()
    }

    fun createRender(width: Int, height: Int) {
        textureRenderer.surfaceCreated()
        surfaceTexture = SurfaceTexture(textureRenderer.screenRecordTextureId)
        surfaceTexture?.setDefaultBufferSize(width, height)
        surfaceTexture?.setOnFrameAvailableListener(this)
        drawSurface = Surface(surfaceTexture)
    }

    /**
     * 新しい映像フレームが来ているか
     *
     * @return true の場合は[updateTexImage] [drawImage] [swapBuffers]を呼び出して描画する。
     */
    suspend fun awaitIsNewFrameAvailable(): Boolean {
        return frameSyncMutex.withLock {
            if (isNewFrameAvailable) {
                // onFrameAvailable が来るまで倒しておく
                isNewFrameAvailable = false
                // 描画すべきなので true
                true
            } else {
                // まだ来てない
                false
            }
        }
    }

    /** これは UI Thread から呼ばれる */
    override fun onFrameAvailable(st: SurfaceTexture) {
        scope.launch {
            frameSyncMutex.withLock {
                // 新しい映像フレームが来たら true
                isNewFrameAvailable = true
            }
        }
    }

    fun updateTexImage() {
        // SurfaceTexture のテクスチャを更新する
        textureRenderer.checkGlError("before updateTexImage")
        surfaceTexture?.updateTexImage()
    }

    fun drawImage() {
        val surfaceTexture = surfaceTexture ?: return
        textureRenderer.drawFrame(surfaceTexture)
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    fun release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        surface.release()
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLSurface = EGL14.EGL_NO_SURFACE
        scope.cancel()
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
        checkEglError("eglMakeCurrent")
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    fun swapBuffers(): Boolean {
        val result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)
        checkEglError("eglSwapBuffers")
        return result
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs)
        checkEglError("eglPresentationTimeANDROID")
    }

    /** [drawAltImage] で表示する画像を設定する */
    fun setAltImageTexture(bitmap: Bitmap) {
        textureRenderer.setAltImageTexture(bitmap)
    }

    /** 映像の代わりに代替画像を描画する */
    fun drawAltImage() {
        textureRenderer.drawAltImage()
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private fun eglSetup() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }
        // Configure EGL for recording and OpenGL ES 2.0.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        checkEglError("eglCreateContext RGB888+recordable ES2")

        // Configure context for OpenGL ES 2.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        mEGLContext = EGL14.eglCreateContext(
            mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        checkEglError("eglCreateContext")

        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], surface, surfaceAttribs, 0)
        checkEglError("eglCreateWindowSurface")
    }

    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
        }
    }

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

}