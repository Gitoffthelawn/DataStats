package jp.takke.util

import android.app.PendingIntent
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue

object TkUtil {

  /**
   * sp 値を px に変換する。
   * 非推奨の `displayMetrics.scaledDensity` の代替。
   * API 34+ の非線形フォントスケーリング(大きなフォントサイズ設定時の圧縮)にも対応する。
   */
  fun spToPx(sp: Float, metrics: DisplayMetrics): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
  }

  // チェック高速化のためのキャッシュ
  private var isEmulatorChecked = false
  private var isEmulatorCache = false

  // 未チェック(未キャッシュ)の場合にのみ実際にチェックする
//  isEmulatorCache = android.os.Build.MODEL.equals("sdk");
  val isEmulator: Boolean
    get() {
      if (!isEmulatorChecked) {
        // 未チェック(未キャッシュ)の場合にのみ実際にチェックする
//			isEmulatorCache = android.os.Build.MODEL.equals("sdk");
        isEmulatorCache = false
        if (Build.DEVICE == "generic") {
          if (Build.BRAND == "generic") {
            isEmulatorCache = true
          }
        }
        isEmulatorChecked = true
      }
      return isEmulatorCache
    }

  /**
   * M 以降なら PendingIntent.FLAG_IMMUTABLE を返す
   */
  fun getPendingIntentImmutableFlagIfOverM(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_IMMUTABLE
    } else {
      0
    }
  }
}