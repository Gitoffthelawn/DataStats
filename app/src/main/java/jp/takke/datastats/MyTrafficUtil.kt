package jp.takke.datastats

import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import java.io.BufferedReader
import java.io.FileReader

object MyTrafficUtil {

  /**
   * 速度[バイト/秒]を表示用文字列に変換する。
   * `Config.unitTypeBps` / `Config.autoUnitScale` を反映し、
   * オーバーレイ(MySurfaceView)と設定画面のライブプレビューで共通に使う。
   *
   * 例: "12.3KB/s" / "1.5MB/s" / "8.0Kbps"
   */
  fun formatSpeedText(bytesPerSec: Long): String {
    if (bytesPerSec < 0) {
      return ""
    }

    val bps = Config.unitTypeBps
    // bps モードでは bits に変換して桁判定する
    val value = if (bps) bytesPerSec * 8 else bytesPerSec

    // 単位接頭辞と除数を決定
    // autoUnitScale = false の場合、既存互換のため常に K(KB/s or Kbps)を使う
    val kb = 1024L
    val mb = kb * 1024L
    val gb = mb * 1024L
    val divisor: Long
    val prefix: String
    when {
      Config.autoUnitScale && value >= gb -> {
        divisor = gb; prefix = "G"
      }
      Config.autoUnitScale && value >= mb -> {
        divisor = mb; prefix = "M"
      }
      else -> {
        divisor = kb; prefix = "K"
      }
    }

    val whole = value / divisor
    var dec = value * 10 / divisor % 10   // 小数第 1 位
    // 微小トラフィックが「0.0」表示にならないよう最低 0.1 を保証する
    // (通信が発生していることを視認できるようにする旧実装からの互換挙動)
    if (value > 0 && whole == 0L && dec == 0L) {
      dec = 1
    }
    val suffix = if (bps) "bps" else "B/s"
    return "$whole.$dec$prefix$suffix"
  }


  fun getTextShadowColorByBytes(resources: Resources, bytes: Long): Int {
    if (bytes < Config.middleLimit) {
      return ResourcesCompat.getColor(resources, R.color.textShadowColorLow, null)
    }
    if (bytes < Config.highLimit) {
      return ResourcesCompat.getColor(resources, R.color.textShadowColorMiddle, null)
    }
    return ResourcesCompat.getColor(resources, R.color.textShadowColorHigh, null)
  }


  fun getTextColorByBytes(resources: Resources, bytes: Long): Int {
    if (bytes < Config.middleLimit) {
      return ResourcesCompat.getColor(resources, R.color.textColorLow, null)
    }
    if (bytes < Config.highLimit) {
      return ResourcesCompat.getColor(resources, R.color.textColorMiddle, null)
    }
    return ResourcesCompat.getColor(resources, R.color.textColorHigh, null)
  }


  val loopbackRxBytes: Long
    get() = readLongValueFromFile("/sys/class/net/lo/statistics/rx_bytes")


  val loopbackTxBytes: Long
    get() = readLongValueFromFile("/sys/class/net/lo/statistics/tx_bytes")


  private fun readLongValueFromFile(path: String): Long {
    try {
      val `in` = FileReader(path)
      val br = BufferedReader(`in`)

      val line = br.readLine()

      br.close()
      `in`.close()

      if (line == null) {
        return 0
      }
      return line.toLong()
    } catch (ignored: Throwable) {
      return 0
    }
  }
}
