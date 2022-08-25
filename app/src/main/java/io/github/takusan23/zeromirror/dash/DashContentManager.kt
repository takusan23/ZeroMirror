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
 * @param prefixName ファイルの先頭につける文字列
 */
class DashContentManager(
    private val parentFolder: File,
    private val prefixName: String,
) {
    /** 作るたびにインクリメントする */
    private var count = 0

    /** 一時作業用フォルダ */
    private val tempFolder = File(parentFolder, TEMP_FOLDER_NAME).apply { mkdir() }

    /** 完成品を公開するフォルダ */
    val outputFolder = File(parentFolder, OUTPUT_VIDEO_FOLDER_NAME).apply { mkdir() }

    /**
     * 連番なファイル名になった[File]を作成する
     *
     * @return [File]
     */
    suspend fun createIncrementFile() = withContext(Dispatchers.IO) {
        File(outputFolder, "$prefixName${count++}.$WEBM_EXTENSION").apply {
            createNewFile()
        }
    }

    /**
     * ファイルを生成する関数。
     * ファイル名変更が出来る以外は [createIncrementFile] と同じ。
     *
     * @param fileName ファイル名
     * @return [File]
     */
    suspend fun createFile(fileName: String) = withContext(Dispatchers.IO) {
        File(outputFolder, fileName).apply {
            createNewFile()
        }
    }


    /**
     * 一時ファイルを生成する関数
     *
     * @param fileName ファイル名
     * @return [File]
     */
    suspend fun generateTempFile(fileName: String) = withContext(Dispatchers.IO) {
        File(tempFolder, fileName).apply {
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