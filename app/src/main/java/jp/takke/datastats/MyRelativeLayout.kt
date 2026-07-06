package jp.takke.datastats

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.RelativeLayout
import jp.takke.util.MyLog

/**
 * オーバーレイのルートレイアウト。全画面(イマーシブ)状態の検出を担当する。
 *
 * - API 30+: WindowInsets のステータスバー可視状態で判定する。
 *   TYPE_APPLICATION_OVERLAY ではウィンドウサイズが全画面遷移で変化しなくなったため、
 *   従来のサイズ比較では検出できない。
 * - API 23〜29: 従来どおり「ビューの高さ == ディスプレイ実高さ」で判定する。
 */
class MyRelativeLayout : RelativeLayout {

  var isFullScreen = false
    private set

  /** 全画面状態が変化したときに呼ばれる(メインスレッド) */
  var onFullScreenChangedListener: ((Boolean) -> Unit)? = null

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // ステータスバーが非表示 = 全画面アプリ表示中とみなす
      setOnApplyWindowInsetsListener { _, insets ->
        updateFullScreen(!insets.isVisible(WindowInsets.Type.statusBars()))
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

    updateFullScreen(h == realHeight)
  }

  private fun updateFullScreen(fullScreen: Boolean) {
    if (isFullScreen == fullScreen) {
      return
    }
    isFullScreen = fullScreen
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
