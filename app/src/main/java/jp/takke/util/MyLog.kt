package jp.takke.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date

object MyLog {

  private var sContextRef: WeakReference<Context>? = null

  fun d(msg: String) {
    if (TkConfig.debugMode || TkUtil.isEmulator) {
      Log.d(TkConsts.LOG_NAME, msg)
    }
    dumpToExternalLogFile(Log.DEBUG, msg)
  }

  /**
   * デバッグモード / エミュレータ時のみ msg ラムダを評価する。
   * 非デバッグ時は文字列組み立てコストが完全に発生しないため、ホットパスから呼ぶ場合に有効。
   *
   * 使い方: MyLog.d { "value: $x" }
   */
  inline fun d(msg: () -> String) {
    if (TkConfig.debugMode || TkUtil.isEmulator) {
      d(msg())
    }
  }

  fun d(msg: String, th: Throwable) {
    if (TkConfig.debugMode || TkUtil.isEmulator) {
      Log.d(TkConsts.LOG_NAME, msg, th)
    }
    dumpToExternalLogFile(Log.DEBUG, msg)
    dumpToExternalLogFile(Log.DEBUG, Log.getStackTraceString(th))
  }

  fun i(msg: String) {
    Log.i(TkConsts.LOG_NAME, msg)
    dumpToExternalLogFile(Log.INFO, msg)
  }

  fun w(msg: String) {
    Log.w(TkConsts.LOG_NAME, msg)
    dumpToExternalLogFile(Log.WARN, msg)
  }

  fun e(th: Throwable) {
    Log.e(TkConsts.LOG_NAME, th.message, th)
    dumpToExternalLogFile(Log.ERROR, Log.getStackTraceString(th))
  }

  /**
   * 外部ストレージ(通常はSDカード)にログを出力する
   */
  @SuppressLint("SimpleDateFormat")
  @Synchronized
  private fun dumpToExternalLogFile(error: Int, msg: String) {

    // 外部ストレージ出力条件確認
    // DEBUG ログはデバッグモードのみ出力する
    if (!TkConfig.debugMode) return

    try {
      // 保存先の決定
      val fout = IOUtil.getInternalStorageAppFilesDirectoryAsFile(sContextRef?.get())
        ?: return  // メディア非マウントなど
      val path = fout.absolutePath + "/" + TkConsts.EXTERNAL_LOG_FILENAME

      // ファイルに書き込む (append)
      FileOutputStream(path, true).use { out ->
        BufferedOutputStream(out).use { bout ->
          // 日付時刻
          val sdf = SimpleDateFormat("yyyy/MM/dd\tHH:mm:ss.SSS")
          bout.write(sdf.format(Date()).toByteArray())
          bout.write("\t".toByteArray())

          // エラーレベル
          val levelTag = when (error) {
            Log.INFO -> "[INFO]"
            Log.WARN -> "[WARN]"
            Log.ERROR -> "[ERROR]"
            Log.DEBUG -> "[DEBUG]"
            else -> ""
          }
          bout.write(levelTag.toByteArray())

          // ログ本文
          bout.write("\t".toByteArray())
          bout.write(msg.toByteArray(Charsets.UTF_8))
          bout.write("\n".toByteArray())

          bout.flush()
        }
      }
    } catch (e: Exception) {
      Log.e(TkConsts.LOG_NAME, e.message, e)
    }
  }

  /**
   * 外部ストレージのログファイルがある一定サイズ以上の場合に削除する
   *
   * 通常は起動時にチェックさせる
   */
  fun deleteBigExternalLogFile() {

    if (!TkConfig.debugMode) return

    try {
      val fout = IOUtil.getInternalStorageAppFilesDirectoryAsFile(sContextRef?.get())
        ?: return
      val path = fout.absolutePath + "/" + TkConsts.EXTERNAL_LOG_FILENAME

      val file = File(path)
      val maxFileSize = 1 * 1024 * 1024L  // [MB]

      Log.i(
        TkConsts.LOG_NAME,
        "external log size check, size[${file.length()}], limit[$maxFileSize]",
      )

      if (file.length() > maxFileSize) {
        file.delete()
      }
    } catch (e: Exception) {
      Log.e(TkConsts.LOG_NAME, e.message, e)
    }
  }

  fun setContext(context: Context) {
    sContextRef = WeakReference(context)
  }

  fun close() {
    // no-op
  }
}
