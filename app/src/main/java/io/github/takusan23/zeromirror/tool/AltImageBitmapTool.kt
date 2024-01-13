package io.github.takusan23.zeromirror.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** [io.github.takusan23.zeromirror.media.VideoEncoder.isDrawAltImage] で使う代替画像を作る */
object AltImageBitmapTool {

    /**
     * 代替画像を作る
     *
     * @param width 画像の幅
     * @param height 画像の高さ
     * @return [Bitmap]
     */
    fun generateAltImageBitmap(
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
        }

        // 描画する文字列
        // TODO ローカライズ
        // TODO TOP に設置する
        val text = """
            現在アプリが画面に表示されていません。
            （他のアプリが画面に写っています）
            
            アプリが画面に表示されるようになると、
            ミラーリングが再開されます。
        """.trimIndent()

        // お絵かきする
        canvas.drawColor(Color.BLACK)
        // 複数行
        var yPos = 100f
        text
            .lines()
            .forEach { textLine ->
                // 真ん中に出す
                val textWidth = paint.measureText(textLine)
                val xPos = (width - textWidth) / 2
                canvas.drawText(textLine, xPos, yPos, paint)
                yPos += paint.textSize
            }
        return bitmap
    }

}