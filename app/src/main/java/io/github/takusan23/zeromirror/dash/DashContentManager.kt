package io.github.takusan23.zeromirror.dash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MPEG-DASH 用ファイル管理クラス
 *
 * - [parentFolder]
 *  - public
 *      - 生成した動画を入れるフォルダ、クライアントに返してる動画です
 *  - temp
 *      - 一時的に保存する必要のあるファイル
 *
 * @param parentFolder 保存先
 * @param audioPrefixName 音声ファイルの先頭につける文字列
 * @param videoPrefixName 映像ファイルの先頭につける文字列
 */
class DashContentManager(
    private val parentFolder: File,
    private val audioPrefixName: String,
    private val videoPrefixName: String,
) {
    // TODO ここなんか連番なファイルにするクラスにしたい
    /** 作るたびにインクリメントする、音声版 */
    private var audioCount = 0

    /** 作るたびにインクリメントする、映像版 */
    private var videoCount = 0

    /** 一時作業用フォルダ */
    private val tempFolder = File(parentFolder, TEMP_FOLDER_NAME).apply { mkdir() }

    /** 完成品を公開するフォルダ */
    val outputFolder = File(parentFolder, OUTPUT_VIDEO_FOLDER_NAME).apply { mkdir() }

    /**
     * 連番な音声ファイルを作る
     *
     * @return [File]
     */
    suspend fun createIncrementAudioFile() = withContext(Dispatchers.IO) {
        File(outputFolder, "$audioPrefixName${audioCount++}.$WEBM_EXTENSION").apply {
            createNewFile()
        }
    }

    /**
     * 連番な映像ファイルを作る
     *
     * @return [File]
     */
    suspend fun createIncrementVideoFile() = withContext(Dispatchers.IO) {
        File(outputFolder, "$videoPrefixName${videoCount++}.$WEBM_EXTENSION").apply {
            createNewFile()
        }
    }

    /**
     * ファイルを生成する関数。
     * ファイル名変更が出来る以外は [createIncrementAudioFile] [createIncrementVideoFile] と同じ。
     *
     * @param fileName ファイル名
     * @return [File]
     */
    suspend fun createFile(fileName: String) = withContext(Dispatchers.IO) {
        File(outputFolder, fileName).apply {
            createNewFile()
        }
    }

    /** 生成したファイルを削除する */
    suspend fun deleteGenerateFile() = withContext(Dispatchers.IO) {
        tempFolder.listFiles()?.forEach { it.delete() }
        outputFolder.listFiles()?.forEach { it.delete() }
    }

    companion object {
        /** 拡張子 */
        private const val WEBM_EXTENSION = "webm"

        /** 完成品の動画が入るフォルダの名前 */
        private const val OUTPUT_VIDEO_FOLDER_NAME = "dist"

        /** 一時作業用フォルダ */
        private const val TEMP_FOLDER_NAME = "temp"
    }

}