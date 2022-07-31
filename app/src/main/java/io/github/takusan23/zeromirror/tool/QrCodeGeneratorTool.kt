package io.github.takusan23.zeromirror.tool

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

/** QRコードを作る */
object QrCodeGeneratorTool {

    /**
     * QRコードを作るライブラリを使ってQRコードを生成する
     *
     * @param data データ
     * @return QRコードのBitmap
     */
    fun generateQrCode(data: String): Bitmap {
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400)
    }
}