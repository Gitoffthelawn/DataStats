package jp.takke.util

import android.content.Context
import java.io.File

object IOUtil {

  /**
   * 内部ストレージのアプリ領域のディレクトリを取得する
   *
   * 例) /storage/sdcard0/Android/data/XXX/files
   */
  fun getInternalStorageAppFilesDirectoryAsFile(context: Context?): File? {
    if (context == null) return null
    val externalFilesDir = context.getExternalFilesDir(null) ?: return null
    // 初回は存在していないので作成しておく
    externalFilesDir.mkdirs()
    return externalFilesDir
  }
}
