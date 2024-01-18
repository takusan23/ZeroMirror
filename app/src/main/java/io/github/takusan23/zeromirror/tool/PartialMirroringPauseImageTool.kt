package io.github.takusan23.zeromirror.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import io.github.takusan23.zeromirror.R

/** [io.github.takusan23.zeromirror.media.VideoEncoder.isDrawAltImage] で使う代替画像を作る */
object PartialMirroringPauseImageTool {

    /**
     * 代替画像を作る
     *
     * @param context [Context]
     * @param width 画像の幅
     * @param height 画像の高さ
     * @return [Bitmap]
     */
    fun generatePartialMirroringPauseImage(
        context: Context,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
        }
        // 描画するY座標
        var yPos = 50f

        // お絵かきする
        canvas.drawColor(Color.BLACK)
        // 画像
        ContextCompat.getDrawable(context, R.drawable.partial_mirroring_pause)?.let { drawable ->
            // 画像は Canvas の高さの 1/3 くらいにする
            val aspectRate = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
            val toHeight = height / 3
            println("aspectRate = $aspectRate / toHeight = $toHeight / ${(toHeight * aspectRate).toInt()}")
            drawable.toBitmap(width = (toHeight * aspectRate).toInt(), height = toHeight)
        }?.also { drawableBitmap ->
            // Canvas に書く
            val xPos = (width - drawableBitmap.width) / 2f
            canvas.drawBitmap(drawableBitmap, xPos, yPos, paint)
            yPos += drawableBitmap.height
        }
        // 文字との間
        yPos += 100
        // 描画する文字列
        context.getString(R.string.zeromirror_service_partial_mirroring_pause).lines().forEach { textLine ->
            // 真ん中に出す
            val textWidth = paint.measureText(textLine)
            val xPos = (width - textWidth) / 2
            canvas.drawText(textLine, xPos, yPos, paint)
            yPos += paint.textSize
        }
        return bitmap
    }

}