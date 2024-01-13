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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
class TextureRenderer {

    private val mTriangleVertices: FloatBuffer
    private val mMVPMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)
    private var mProgram = 0
    private var muMVPMatrixHandle = 0
    private var muSTMatrixHandle = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0
    private val rotationAngle = 0

    // Uniform 変数へのハンドル
    private var uAltImageTextureHandle = 0
    private var uTextureHandle = 0
    private var uDrawAltImageHandle = 0

    var screenRecordTextureId = -1234567
        private set

    private var altImageTextureId = -1234567

    init {
        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTriangleVertices.put(mTriangleVerticesData).position(0)
        Matrix.setIdentityM(mSTMatrix, 0)
    }

    /** [drawAltImage] で表示する画像の設定 */
    fun setAltImageTexture(bitmap: Bitmap) {
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        // AltImage テクスチャユニットは GLES20.GL_TEXTURE1 なので 1
        GLES20.glUniform1i(uAltImageTextureHandle, 1)
        checkGlError("glUniform1i uAltImageTextureHandle")
        // テクスチャを初期化
        // 第二引数の 1 って何、、、（GLES20.GL_TEXTURE1 だから？）
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        checkGlError("glActiveTexture GL_TEXTURE1")
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, altImageTextureId)
        checkGlError("glBindTexture altImageTextureId")
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        checkGlError("GLUtils.texImage2D altImageTextureId")
    }

    /** 録画映像の代わりに代替画像を描画する */
    fun drawAltImage() {
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        // AltImage を描画する
        GLES20.glUniform1i(uDrawAltImageHandle, 1)
        checkGlError("glUniform1i uDrawAltImageHandle")
        // 描画する
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
    }

    /** 画面録画の映像を描画する */
    fun drawFrame(st: SurfaceTexture) {
        checkGlError("onDrawFrame start")
        st.getTransformMatrix(mSTMatrix)
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // SurfaceTexture テクスチャユニットは GLES20.GL_TEXTURE0 なので 0
        GLES20.glUniform1i(uTextureHandle, 0)
        checkGlError("glUniform1i uTextureHandle")
        // AltImage ではなく SurfaceTexture を描画する
        GLES20.glUniform1i(uDrawAltImageHandle, 0)
        checkGlError("glUniform1i uDrawAltImageHandle")
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, screenRecordTextureId)
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(maTextureHandle)
        checkGlError("glEnableVertexAttribArray maTextureHandle")
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
    }

    /** シェーダーのコンパイルとかする */
    fun surfaceCreated() {
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (maPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (maTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (muMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uMVPMatrix")
        }
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix")
        checkGlError("glGetUniformLocation uSTMatrix")
        if (muSTMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uSTMatrix")
        }
        val textures = IntArray(2)
        GLES20.glGenTextures(2, textures, 0)

        screenRecordTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, screenRecordTextureId)
        checkGlError("glBindTexture screenRecordTextureId")
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameter")

        altImageTextureId = textures[1]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, altImageTextureId)
        checkGlError("glBindTexture altImageTextureId")

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // OpenGL の Uniform変数 へアクセスするハンドル取得
        uTextureHandle = GLES20.glGetUniformLocation(mProgram, "sTexture")
        checkGlError("glGetUniformLocation uTextureHandle")
        uAltImageTextureHandle = GLES20.glGetUniformLocation(mProgram, "uAltImage")
        checkGlError("glGetUniformLocation uAltImageTextureHandle")
        uDrawAltImageHandle = GLES20.glGetUniformLocation(mProgram, "uDrawAltImage")
        checkGlError("glGetUniformLocation uDrawAltImageHandle")

        Matrix.setIdentityM(mMVPMatrix, 0)
        // if (rotationAngle != 0) {
        //     Matrix.rotateM(mMVPMatrix, 0, rotationAngle, 0, 0, 1)
        // }
    }

    fun changeFragmentShader(fragmentShader: String) {
        GLES20.glDeleteProgram(mProgram)
        mProgram = createProgram(VERTEX_SHADER, fragmentShader)
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return 0
        }
        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            return 0
        }
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES20.glAttachShader(program, pixelShader)
        checkGlError("glAttachShader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    fun checkGlError(op: String) {
        var error: Int
        if (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
    }

    companion object {
        private const val FLOAT_SIZE_BYTES = 4
        private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

        private val mTriangleVerticesData = floatArrayOf(
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f
        )

        private const val VERTEX_SHADER = """
uniform mat4 uMVPMatrix;
uniform mat4 uSTMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
  gl_Position = uMVPMatrix * aPosition;
  vTextureCoord = (uSTMatrix * aTextureCoord).xy;
}
"""
        private const val FRAGMENT_SHADER = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform sampler2D uAltImage;

// 映像を描画するのか、画像を表示するのか
uniform int uDrawAltImage;

void main() {
  if (bool(uDrawAltImage)) {
    gl_FragColor = texture2D(uAltImage, vTextureCoord);
  } else {
    gl_FragColor = texture2D(sTexture, vTextureCoord);  
  }
}
"""
    }
}