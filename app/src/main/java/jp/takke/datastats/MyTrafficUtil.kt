package jp.takke.datastats

import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import java.io.BufferedReader
import java.io.FileReader
import kotlin.math.log10

object MyTrafficUtil {

  /**
   * 速度[バイト/秒]を表示用文字列に変換する。
   * オーバーレイ(MySurfaceView)と設定画面のライブプレビューで共通に使う。
   * `bps` / `autoScale` は省略時に Config の設定値を使う(テストでは明示指定する)。
   *
   * 例: "12.3KB/s" / "1.5MB/s" / "8.0Kbps"
   */
  fun formatSpeedText(
    bytesPerSec: Long,
    bps: Boolean = Config.unitTypeBps,
    autoScale: Boolean = Config.autoUnitScale,
  ): String {
    if (bytesPerSec < 0) {
      return ""
    }

    // bps モードでは bits に変換して桁判定する
    val value = if (bps) bytesPerSec * 8 else bytesPerSec

    // 単位接頭辞と除数を決定
    // autoScale = false の場合、既存互換のため常に K(KB/s or Kbps)を使う
    val kb = 1024L
    val mb = kb * 1024L
    val gb = mb * 1024L
    val divisor: Long
    val prefix: String
    when {
      autoScale && value >= gb -> {
        divisor = gb; prefix = "G"
      }
      autoScale && value >= mb -> {
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

  /**
   * 通信量[バイト]をバー表示用の千分率 [0, 1000] に変換する。
   *
   * @param logBar true なら対数スケール、false なら線形スケール
   * @param barMaxKB バーが振り切れる速度[KB]
   */
  fun convertBytesToPerThousand(bytes: Long, logBar: Boolean, barMaxKB: Int): Int {
    if (!logBar) {
      return if (bytes / 1024 > barMaxKB) 1000 else (bytes / barMaxKB).toInt()   // [0, 1000]
    } else {
      // 100KB基準値
      val normalBytes = bytes * 100 / barMaxKB
      return if (normalBytes < 1) {
        0
      } else {
        // max=100KB
        //   1KB -> 300
        //  10KB -> 400
        // 100KB -> 500
        val log = (log10(normalBytes.toDouble()) * 100).toInt()

        // max=100KB -> 500*2 = 1000
        log * 2
      }
    }
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
