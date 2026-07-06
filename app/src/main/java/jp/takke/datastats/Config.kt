package jp.takke.datastats

import android.content.Context
import androidx.preference.PreferenceManager
import jp.takke.util.MyLog
import jp.takke.util.TkConfig
import kotlin.math.pow

object Config {

  // 文字色変更基準[Bytes]
  var highLimit: Long = 0
  var middleLimit: Long = 0

  var xPos: Int = 90  // [0, 100]
  var barMaxKB: Int = 100
  var unitTypeBps: Boolean = false

  var logBar: Boolean = true
  var intervalMs: Int = 1000
  var hideWhenInFullscreen: Boolean = true

  var interpolateMode: Boolean = false

  /** 直近履歴のスパークライン(ミニグラフ)をオーバーレイに重ねて描画するかどうか */
  var sparklineMode: Boolean = false

  var textSizeSp: Int = C.DEFAULT_TEXT_SIZE_SP


  fun loadPreferences(context: Context) {

    MyLog.d("Config.loadPreferences")

    val pref = PreferenceManager.getDefaultSharedPreferences(context)
    TkConfig.debugMode = pref.getBoolean(C.PREF_KEY_DEBUG_MODE, false)
    xPos = pref.getInt(C.PREF_KEY_X_POS, 100)
    intervalMs = pref.getInt(C.PREF_KEY_INTERVAL_MSEC, 1000)
    barMaxKB = pref.getInt(C.PREF_KEY_BAR_MAX_SPEED_KB, 10240)
    unitTypeBps = pref.getBoolean(C.PREF_KEY_UNIT_TYPE_BPS, false)
    logBar = pref.getBoolean(C.PREF_KEY_LOGARITHM_BAR, true)
    hideWhenInFullscreen = pref.getBoolean(C.PREF_KEY_HIDE_WHEN_IN_FULLSCREEN, true)
    interpolateMode = pref.getBoolean(C.PREF_KEY_INTERPOLATE_MODE, false)
    sparklineMode = pref.getBoolean(C.PREF_KEY_SPARKLINE_MODE, false)
    textSizeSp = pref.getInt(C.PREF_KEY_TEXT_SIZE_SP, C.DEFAULT_TEXT_SIZE_SP)

    // 文字色変更基準の再計算
    if (logBar) {
      // 「バー全体の (pXxxLimit*100) [%] を超えたらカラーを変更する」基準値を計算する
      // 例: max=10MB/s ⇒ 30% は 3,238[B]
      val pMiddleLimit = 0.3  // [0, 1]
      middleLimit = (barMaxKB / 100.0 * 10.0.pow(pMiddleLimit * 5.0)).toLong()

      // 例: max=10MB/s ⇒ 60% は 100[KB]
      val pHighLimit = 0.6  // [0, 1]
      highLimit = (barMaxKB / 100.0 * 10.0.pow(pHighLimit * 5.0)).toLong()
    } else {
      middleLimit = (10 * 1024).toLong()
      highLimit = (100 * 1024).toLong()
    }
    MyLog.d("loadPreferences: update limit for colors: middle[${middleLimit}B], high[${highLimit}B]")
  }
}
