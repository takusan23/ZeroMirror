package io.github.takusan23.zeromirror

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 生成した動画ファイルを管理する。
 * 保持分を超えたら今までのファイルを消すなど
 *
 * @param parentFile 保存先
 * @param baseName ファイル名先頭につけるやつ
 * @param isWebM WebMを利用する場合はtrue(VP9)、mp4を利用する場合はfalse(mp4)
 */
class CaptureVideoManager(
    private val parentFile: File,
    private val baseName: String,
    private val isWebM: Boolean = false,
) {

    /** 作るたびにインクリメントする */
    private var count = 0

    /** 生成したふぁいるの配列 */
    private val fileList = mutableListOf<File>()

    /** 今のファイル */
    var currentFile: File? = null
        private set

    /** [parentFile]の中のファイルを消す */
    fun deleteParentFolderChildren() {
        parentFile.listFiles()?.forEach { it.delete() }
    }

    /**
     * 連番なファイル名になった[File]を作成する
     *
     * @return [File]
     */
    suspend fun generateFile(): File {
        deleteNotHoldFile()
        val extension = if (isWebM) "webm" else "mp4"
        currentFile = File(parentFile, "$baseName${count++}.$extension").apply { createNewFile() }
        fileList.add(currentFile!!)
        return currentFile!!
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
    }
}