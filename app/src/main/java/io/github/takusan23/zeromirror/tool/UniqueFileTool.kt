package io.github.takusan23.zeromirror.tool

import java.io.File

/**
 * ファイルを作成する。ファイル名が連番になるように
 *
 * @param parentFile 保存先
 * @param baseName ファイル名先頭につけるやつ
 * @param extension 拡張子
 */
class UniqueFileTool(
    private val parentFile: File,
    private val baseName: String,
    private val extension: String,
) {

    /** 作るたびにインクリメントする */
    private var count = 0

    /** [parentFile]の中のファイルを消す */
    fun deleteParentFolderChildren() {
        parentFile.listFiles()?.forEach { it.delete() }
    }

    /**
     * 連番なファイル名になった[File]を作成する
     *
     * @return [File]
     */
    fun generateFile(): File {
        return File(parentFile, "$baseName${count++}.$extension").apply {
            createNewFile()
        }
    }

    /**
     * 現在の連番ファイルを取得する
     *
     * @return [File]
     */
    fun currentFile(): File {
        return File(parentFile, "$baseName${count}.$extension")
    }

}