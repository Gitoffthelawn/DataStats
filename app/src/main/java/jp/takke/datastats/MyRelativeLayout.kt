package jp.takke.datastats

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import jp.takke.util.MyLog

/**
 * オーバーレイのルートレイアウト。全画面(イマーシブ)状態の検出を担当する。
 *
 * - API 30+: `rootWindowInsets` の system bars 可視状態をリアルタイム参照する。
 *   `TYPE_APPLICATION_OVERLAY` ではウィンドウサイズが全画面遷移で変化しなくなり、
 *   `setOnApplyWindowInsetsListener` も他アプリの immersive 切替では発火しない環境が
 *   あるため、参照時に都度取得するポーリング方式を採用する。
 * - API 23〜29: 従来どおり「ビューの高さ == ディスプレイ実高さ」で判定する。
 *
 * すべての判定で「ステータスバーとナビゲーションバーのどちらか片方でも非表示なら
 * イマーシブとみなす」ルール。動画プレイヤーやゲームは通常両方を隠す。
 */
class MyRelativeLayout : RelativeLayout {

  /** API 29 以下の onSizeChanged 判定結果、および直近通知済みの値。 */
  private var mLastFullScreen: Boolean = false

  /** 全画面状態が変化したときに呼ばれる(メインスレッド) */
  var onFullScreenChangedListener: ((Boolean) -> Unit)? = null

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // 発火するデバイスではリアルタイムに反映させる(ポーリングを待たなくても反応する)
      setOnApplyWindowInsetsListener { _, insets ->
        notifyIfChanged(computeFullScreenFromInsets(insets))
        insets
      }
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // attach 直後に最新の insets を受け取るため明示的に要求する
      requestApplyInsets()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    // API 30+ は insets ベースの判定を使う
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return
    }

    if (oldh == 0) {
      return
    }

    notifyIfChanged(h == realHeight)
  }

  /**
   * 現時点の全画面状態を返す。
   *
   * API 30+ ではリスナーの発火に依存せず、都度 `rootWindowInsets` を参照する。
   */
  val isFullScreen: Boolean
    get() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insets = rootWindowInsets
        val fullScreen = if (insets != null) computeFullScreenFromInsets(insets) else mLastFullScreen
        // 参照時に state が変わっていればリスナーへも波及させる
        if (fullScreen != mLastFullScreen) {
          notifyIfChanged(fullScreen)
        }
        return fullScreen
      }
      return mLastFullScreen
    }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun computeFullScreenFromInsets(insets: WindowInsets): Boolean {
    // ステータスバー or ナビゲーションバーのどちらかが非表示 = イマーシブ / 全画面アプリ
    val statusHidden = !insets.isVisible(WindowInsets.Type.statusBars())
    val navHidden = !insets.isVisible(WindowInsets.Type.navigationBars())
    return statusHidden || navHidden
  }

  private fun notifyIfChanged(fullScreen: Boolean) {
    if (mLastFullScreen == fullScreen) return
    mLastFullScreen = fullScreen
    MyLog.d("MyRelativeLayout: fullscreen[$fullScreen]")
    onFullScreenChangedListener?.invoke(fullScreen)
  }

  @Suppress("DEPRECATION")
  private val realHeight: Int
    get() {
      // API 29 以下専用のレガシー経路
      val display =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
      val point = Point()
      display.getRealSize(point)
      return point.y
    }
}
