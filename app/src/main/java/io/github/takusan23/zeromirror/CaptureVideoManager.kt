package io.github.takusan23.zeromirror

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 生成した動画ファイルを管理する。
 * 保持分を超えたら今までのファイルを消すなど
 *
 * - [parentFolder]
 *  - public
 *      - 生成した動画を入れるフォルダ、クライアントに返してる動画です
 *  - temp
 *      - 一時的に保存する必要のあるファイル
 *
 * 両方とも [deleteParentFolderChildren] を呼ぶと削除されます。
 *
 * @param parentFolder 保存先
 * @param prefixName ファイル名先頭につけるやつ
 * @param isWebM WebMを利用する場合はtrue、mp4を利用する場合はfalse
 */
class CaptureVideoManager(
    private val parentFolder: File,
    private val prefixName: String,
    private val isWebM: Boolean = false,
) {

    /** 作るたびにインクリメントする */
    private var count = 0

    /** 生成したふぁいるの配列 */
    private val fileList = mutableListOf<File>()

    /** 一時作業用フォルダ */
    private val tempFolder = File(parentFolder, TEMP_FOLDER_NAME).apply { mkdir() }

    /** 完成品を公開するフォルダ */
    val outputsFolder = File(parentFolder, OUTPUT_VIDEO_FOLDER_NAME).apply { mkdir() }

    /** 今のファイル */
    var currentFile: File? = null
        private set

    /** [parentFolder]の中のファイルを消す */
    suspend fun deleteParentFolderChildren() = withContext(Dispatchers.IO) {
        outputsFolder.listFiles()?.forEach { it.delete() }
        tempFolder.listFiles()?.forEach { it.delete() }
    }

    /**
     * 連番なファイル名になった[File]を作成する
     *
     * @return [File]
     */
    suspend fun generateNewFile() = withContext(Dispatchers.IO) {
        // deleteNotHoldFile()
        val extension = if (isWebM) "webm" else "mp4"
        currentFile = File(outputsFolder, "$prefixName${count++}.$extension").apply { createNewFile() }
        fileList.add(currentFile!!)
        currentFile!!
    }

    suspend fun createFile(fileName: String) = withContext(Dispatchers.IO) {
        File(outputsFolder, fileName).apply { createNewFile() }
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

    /** 保持数を超えたファイルを消す */
    private suspend fun deleteNotHoldFile() = withContext(Dispatchers.IO) {
        val deleteItemSize = fileList.size - FILE_HOLD_COUNT
        if (deleteItemSize >= 0) {
            fileList.take(deleteItemSize).forEach { it.delete() }
        }
    }

    companion object {
        /** 動画保持数 */
        private const val FILE_HOLD_COUNT = 5

        /** 完成品の動画が入るフォルダの名前 */
        private const val OUTPUT_VIDEO_FOLDER_NAME = "dist"

        /** 一時作業用フォルダ */
        private const val TEMP_FOLDER_NAME = "temp"
    }
}