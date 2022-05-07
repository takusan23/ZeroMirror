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
 */
class CaptureVideoManager(
    private val parentFile: File,
    private val baseName: String,
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
        currentFile = File(parentFile, "$baseName${count++}.mp4").apply { createNewFile() }
        fileList.add(currentFile!!)
        return currentFile!!
    }

    /** 保持数を超えたファイルを消す */
    private suspend fun deleteNotHoldFile() = withContext(Dispatchers.IO) {
        val size = fileList.size
        if (FILE_HOLD_COUNT < size) {
            fileList.subList(FILE_HOLD_COUNT, size).forEach { it.delete() }
        }
    }

    companion object {
        /** 動画保持数 */
        private const val FILE_HOLD_COUNT = 5
    }
}