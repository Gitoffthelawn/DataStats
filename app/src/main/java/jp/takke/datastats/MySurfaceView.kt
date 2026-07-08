package jp.takke.datastats

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.res.ResourcesCompat
import jp.takke.util.MyLog
import java.util.LinkedList

class MySurfaceView : SurfaceView, SurfaceHolder.Callback, Runnable {

  private var mSurfaceHolder: SurfaceHolder? = null
  @Volatile
  private var mThread: Thread? = null

  @Volatile
  private var mThreadActive: Boolean = false

  private var mScreenWidth: Int = 0
  private var mScreenHeight: Int = 0

  private val mTrafficList = LinkedList<Traffic>()

  private var mDownloadMarkBitmap: Bitmap? = null
  private var mUploadMarkBitmap: Bitmap? = null
  private var uploadDrawable: Drawable? = null
  private var downloadDrawable: Drawable? = null

  // 前のフレームの値
  private var mLastPTx: Int = 0
  private var mLastPRx: Int = 0
  private var mLastTx: Long = 0
  private var mLastRx: Long = 0

  // 描画用オブジェクト(毎フレーム再生成すると GC 圧が大きいためフィールドで再利用)
  private val mPaint = Paint()
  private val mPaintUd = Paint()
  private val mPaintSparkline = Paint()
  private val mPaintNetworkType = Paint()
  private val mUploadMatrix = Matrix()
  private val mDownloadMatrix = Matrix()
  private val mSparklinePath = Path()

  // 現在のネットワーク種別(showNetworkTypeIcon が ON のとき描画する)
  @Volatile
  private var mNetworkType: NetworkTypeMonitor.NetworkType = NetworkTypeMonitor.NetworkType.NONE

  // Resources から取得する値のキャッシュ(不変)
  private var mResCached = false
  private var mBgColor = 0
  private var mUploadBorderColor = 0
  private var mDownloadBorderColor = 0
  private var mPaddingRight = 0
  private var mPaddingLeft = 0
  private var mBarStrokeWidth = 0f


  private inner class Traffic(
    var time: Long,
    var tx: Long,
    var pTx: Int,
    var rx: Long,
    var pRx: Int
  )


  constructor(context: Context) : super(context) {

    init()
  }

  constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {

    init()
  }

  private fun init() {

    mSurfaceHolder = holder
    mSurfaceHolder!!.addCallback(this)

    // make this surface view transparent
    mSurfaceHolder!!.setFormat(PixelFormat.TRANSLUCENT)
    isFocusable = true
    setZOrderOnTop(true)
  }

  fun setSleeping(sleeping: Boolean) {

//        mSleeping = sleeping;

    if (sleeping) {
      stopThread()
    } else {
      startThread()
    }
  }

  fun drawBlank() {

    MyLog.d("drawBlank")

    myDrawFrame(-1, -1, 0, 0)
  }

  /**
   * 現在のネットワーク種別を設定する。
   * `Config.showNetworkTypeIcon` が ON のときに描画するために保持する。
   */
  fun setNetworkType(type: NetworkTypeMonitor.NetworkType) {
    if (mNetworkType == type) return
    mNetworkType = type
    // 補間モードでない場合は次の setTraffic まで再描画されないため、即座に 1 フレーム描く。
    if (!Config.interpolateMode) {
      sForceRedraw = true
      try {
        myDraw()
      } finally {
        sForceRedraw = false
      }
    }
  }

  fun setTraffic(tx: Long, pTx: Int, rx: Long, pRx: Int) {

    synchronized(mTrafficList) {
      mTrafficList.add(Traffic(System.currentTimeMillis(), tx, pTx, rx, pRx))

      // 過去データ削除
      if (mTrafficList.size > TRAFFIC_LIST_COUNT_MAX) {
        val n = mTrafficList.size - TRAFFIC_LIST_COUNT_MAX
        for (i in 0 until n) {
          mTrafficList.removeFirst()
        }
      }
    }

    if (!Config.interpolateMode || sForceRedraw) {
      myDraw()
    }
  }

  override fun run() {

    while (mThread != null && mThreadActive) {

      myDraw()
    }

  }

  private fun myDraw() {

    val startTime = System.currentTimeMillis()

    try {
      myDrawFrame(startTime)
    } catch (e: Exception) {
      MyLog.e(e)
    }

    val now = System.currentTimeMillis()
    var waitMs = 1000 / TARGET_FPS - (now - startTime)
    if (waitMs < 0) {
      waitMs = 1
    }
    SystemClock.sleep(waitMs)

    // for actual FPS
//        mDrawTimes.addLast(now);
//        if (mDrawTimes.size() > 24) {
//            final int n = mDrawTimes.size() - 24;
//            for (int i=0; i<n; i++) {
//                mDrawTimes.removeFirst();
//            }
//        }
  }

  private fun myDrawFrame(now: Long) {

    val t: Traffic
    synchronized(mTrafficList) {
      if (mTrafficList.size <= 0) {
        return
      }
      t = mTrafficList.last
    }
    val tx = t.tx
    val rx = t.rx

    //        MyLog.d(" myDrawFrame: tx[" + tx + "B], rx[" + rx + "B] " + now + "");

    // skip zero
    if (!sForceRedraw &&
      mLastTx == 0L && mLastPTx == 0 && tx == 0L &&
      mLastRx == 0L && mLastPRx == 0 && rx == 0L
    ) {

      // 一度ゼロになったら通信量が発生するまで待機する
      //            MyLog.d("MySurfaceView.myDrawFrame: same frame, zero");
      return
    }


    // 補間実行
    val pTx = if (Config.interpolateMode && Config.logBar) interpolate(t, now, true) else t.pTx
    val pRx = if (Config.interpolateMode && Config.logBar) interpolate(t, now, false) else t.pRx

    // 前回と同じなら再描画しない
    if (!sForceRedraw &&
      pTx == mLastPTx && mLastTx == tx &&
      pRx == mLastPRx && mLastRx == rx
    ) {
//            MyLog.d("MySurfaceView.myDrawFrame: same frame, tx[" + pTx + "=" + tx + "], rx[" + pRx + "=" + rx + "]");
      return
//        } else {
//            MyLog.d("MySurfaceView.myDrawFrame: tx[" + pTx + "], rx[" + pRx + "]");
    }
    mLastPTx = pTx
    mLastPRx = pRx
    mLastTx = tx
    mLastRx = rx

    myDrawFrame(tx, rx, pTx, pRx)
  }

  private fun myDrawFrame(tx: Long, rx: Long, pTx: Int, pRx: Int) {

    //--------------------------------------------------
    // draw start
    //--------------------------------------------------
    val canvas = mSurfaceHolder!!.lockCanvas() ?: return

    val resources = resources
    ensureResourcesCached()
    val paint = mPaint

    // clear
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    // Background
    canvas.drawColor(mBgColor)


    //--------------------------------------------------
    // upload, download
    //--------------------------------------------------
    val xDownloadStart = mScreenWidth / 2

    // upload gradient
    if (uploadDrawable == null) {
      uploadDrawable = ResourcesCompat.getDrawable(resources, R.drawable.upload_background, null)
    }
    val xUploadEnd = (pTx / 1000f * xDownloadStart).toInt()
    uploadDrawable!!.setBounds(0, 0, xUploadEnd, mScreenHeight)
    uploadDrawable!!.draw(canvas)
    if (pTx > 0) {
      paint.color = mUploadBorderColor
      paint.strokeWidth = mBarStrokeWidth
      canvas.drawLine(
        xUploadEnd.toFloat(),
        0f,
        xUploadEnd.toFloat(),
        mScreenHeight.toFloat(),
        paint
      )
    }

    // download gradient
    if (downloadDrawable == null) {
      downloadDrawable =
        ResourcesCompat.getDrawable(resources, R.drawable.download_background, null)
    }
    val xDownloadEnd = (xDownloadStart + pRx / 1000f * xDownloadStart).toInt()
    downloadDrawable!!.setBounds(xDownloadStart, 0, xDownloadEnd, mScreenHeight)
    downloadDrawable!!.draw(canvas)
    if (pRx > 0) {
      paint.color = mDownloadBorderColor
      paint.strokeWidth = mBarStrokeWidth
      canvas.drawLine(
        xDownloadEnd.toFloat(),
        0f,
        xDownloadEnd.toFloat(),
        mScreenHeight.toFloat(),
        paint
      )
    }


    val scaledDensity = resources.displayMetrics.scaledDensity
    val textSizeSp = Config.textSizeSp
    val textSizePx = textSizeSp * scaledDensity

    // upload text
    paint.typeface = Typeface.MONOSPACE
    paint.color = MyTrafficUtil.getTextColorByBytes(resources, tx)
    paint.setShadowLayer(1.5f, 1.5f, 1.5f, MyTrafficUtil.getTextShadowColorByBytes(resources, tx))
    paint.textAlign = Paint.Align.RIGHT
    paint.textSize = textSizePx
    canvas.drawText(
      getReadableUDText(tx),
      (xDownloadStart - mPaddingRight).toFloat(),
      paint.textSize,
      paint
    )

    // download text
    paint.color = MyTrafficUtil.getTextColorByBytes(resources, rx)
    paint.setShadowLayer(1.5f, 1.5f, 1.5f, MyTrafficUtil.getTextShadowColorByBytes(resources, rx))
    canvas.drawText(
      getReadableUDText(rx),
      (mScreenWidth - mPaddingRight).toFloat(),
      paint.textSize,
      paint
    )

    paint.shader = null

    // upload/download mark
    val paintUd = mPaintUd
    val udMarkSize = (textSizeSp + 2) * scaledDensity
    run {
      if (mUploadMarkBitmap == null) {
        mUploadMarkBitmap =
          BitmapFactory.decodeResource(resources, R.drawable.ic_find_previous_holo_dark)
      }
      val bmp = mUploadMarkBitmap!!
      val s = udMarkSize / bmp.width
      mUploadMatrix.setScale(s, s)
      mUploadMatrix.postTranslate(mPaddingLeft.toFloat(), 0f)
      canvas.drawBitmap(bmp, mUploadMatrix, paintUd)
    }
    run {
      if (mDownloadMarkBitmap == null) {
        mDownloadMarkBitmap =
          BitmapFactory.decodeResource(resources, R.drawable.ic_find_next_holo_dark)
      }
      val bmp = mDownloadMarkBitmap!!
      val s = udMarkSize / bmp.width
      mDownloadMatrix.setScale(s, s)
      mDownloadMatrix.postTranslate((xDownloadStart + mPaddingLeft).toFloat(), 0f)
      canvas.drawBitmap(bmp, mDownloadMatrix, paintUd)
    }

    // FPS
//        {
//            paint.setColor(Color.rgb(0x80, 0x80, 0x80));
//            paint.setTextSize(20);
//            paint.setTextAlign(Paint.Align.RIGHT);
//            final float fps = calcCurrentFps(startTime);
//            final long now = System.currentTimeMillis();
//            canvas.drawText(((int) fps) + "." + (int) ((fps * 10) % 10) + "[fps] " + (now - startTime) + "ms",
//                    mScreenWidth-paddingRight, mScreenHeight-10, paint);
//        }

    //--------------------------------------------------
    // sparkline (direct 60s trend overlay)
    //--------------------------------------------------
    if (Config.sparklineMode) {
      drawSparklines(canvas, xDownloadStart)
    }

    //--------------------------------------------------
    // network type badge (W / M / E / V)
    //--------------------------------------------------
    if (Config.showNetworkTypeIcon) {
      drawNetworkTypeBadge(canvas, xDownloadStart, textSizePx)
    }

    mSurfaceHolder!!.unlockCanvasAndPost(canvas)
  }

  /**
   * オーバーレイ中央(upload と download の境界)に小さくネットワーク種別文字を描画する。
   * "W" = Wi-Fi / "M" = Mobile / "E" = Ethernet / "V" = VPN
   */
  private fun drawNetworkTypeBadge(canvas: Canvas, xDownloadStart: Int, textSizePx: Float) {
    val label = when (mNetworkType) {
      NetworkTypeMonitor.NetworkType.WIFI -> "W"
      NetworkTypeMonitor.NetworkType.MOBILE -> "M"
      NetworkTypeMonitor.NetworkType.ETHERNET -> "E"
      NetworkTypeMonitor.NetworkType.VPN -> "V"
      NetworkTypeMonitor.NetworkType.NONE -> return
    }
    val paint = mPaintNetworkType
    paint.color = 0xFFFFFF00.toInt()   // 視認性の高い黄
    paint.setShadowLayer(1.5f, 0f, 0f, 0xFF000000.toInt())
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = textSizePx * 0.75f
    paint.typeface = Typeface.MONOSPACE
    paint.isAntiAlias = true
    // 上端に寄せる(mScreenHeight 内で baseline は textSize 分下がるので少し余白)
    canvas.drawText(label, xDownloadStart.toFloat(), textSizePx * 0.85f, paint)
    paint.setShadowLayer(0f, 0f, 0f, 0)
  }

  /**
   * 直近履歴を左右半分に分けてスパークライン(上り: 左, 下り: 右)として描画する。
   * 上り/下りバー本体には触れず、上に重ねて描く。
   */
  private fun drawSparklines(canvas: Canvas, xDownloadStart: Int) {
    // 履歴をスレッド安全にスナップショット
    val snapshot: List<Traffic>
    synchronized(mTrafficList) {
      if (mTrafficList.size < 2) return
      snapshot = ArrayList(mTrafficList)
    }

    drawOneSparkline(canvas, snapshot, xStart = 0, xEnd = xDownloadStart, isTx = true)
    drawOneSparkline(canvas, snapshot, xStart = xDownloadStart, xEnd = mScreenWidth, isTx = false)
  }

  private fun drawOneSparkline(
    canvas: Canvas,
    snapshot: List<Traffic>,
    xStart: Int,
    xEnd: Int,
    isTx: Boolean,
  ) {
    val n = snapshot.size
    if (n < 2) return

    val width = (xEnd - xStart).toFloat()
    val height = mScreenHeight.toFloat()

    val paint = mPaintSparkline
    paint.style = Paint.Style.STROKE
    // Density に応じた線幅にしたいところだが 20fps 描画のため軽量化を優先し固定値
    paint.strokeWidth = 1.5f
    paint.color = SPARKLINE_COLOR
    paint.isAntiAlias = true

    val path = mSparklinePath
    path.reset()

    // 履歴の等間隔プロット(サンプル間の時間差は無視して、index 位置ベースで描く)
    // 実測ミリ秒差でスケーリングすると Config.intervalMs 変更時に見た目が跳ねやすいため
    val step = width / (n - 1).toFloat()
    for (i in 0 until n) {
      val t = snapshot[i]
      val p = if (isTx) t.pTx else t.pRx  // [0, 1000]
      val x = xStart + step * i
      // 上方向が大きい値になるよう Y を反転
      val y = height - (p / 1000f * height)
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    canvas.drawPath(path, paint)
  }

  /** Resources 由来の値(色・寸法)を一度だけロードしてキャッシュする */
  private fun ensureResourcesCached() {
    if (mResCached) return
    val res = resources
    mBgColor = ResourcesCompat.getColor(res, R.color.textBackgroundColor, null)
    mUploadBorderColor = ResourcesCompat.getColor(res, R.color.uploadBorder, null)
    mDownloadBorderColor = ResourcesCompat.getColor(res, R.color.downloadBorder, null)
    mPaddingRight = res.getDimensionPixelSize(R.dimen.overlay_padding_right)
    mPaddingLeft = res.getDimensionPixelSize(R.dimen.overlay_padding_left)
    mBarStrokeWidth = res.getDimensionPixelSize(R.dimen.updown_bar_right_border_size).toFloat()
    mResCached = true
  }

  private fun getReadableUDText(bytes: Long): String {

    if (bytes < 0) {
      return ""
    }

    val bps = Config.unitTypeBps
    // bps モードでは bits に変換して桁判定する
    val value = if (bps) bytes * 8 else bytes

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
    val dec = value * 10 / divisor % 10   // 小数第 1 位
    val suffix = if (bps) "bps" else "B/s"
    return "$whole.$dec$prefix$suffix"
  }

  private fun interpolate(t: Traffic, now: Long, getTx: Boolean): Int {

    val currentP = if (getTx) t.pTx else t.pRx

    // 直近 2 点の Traffic をスレッド安全に取り出す
    val prev: Traffic
    val curr: Traffic
    synchronized(mTrafficList) {
      val size = mTrafficList.size
      if (size < 2) {
        // 補間できるだけの点数が集まっていない
        return currentP
      }
      curr = mTrafficList[size - 1]
      prev = mTrafficList[size - 2]
    }

    val lastIntervalTime = curr.time - prev.time
    if (lastIntervalTime <= 0) {
      // 想定外(時刻が巻き戻った等)は補間せず現在値を返す
      return currentP
    }

    val elapsed = now - curr.time
    if (elapsed > lastIntervalTime * 3) {
      // 十分時間が経過しているので収束させる
      return currentP
    }

    // 要は lastIntervalTime 分だけ遅れて描画される感じ
    // でも lastIntervalTime だと遅すぎるので少し短くしておく
    val at = now - lastIntervalTime / 2

    val prevValue = if (getTx) prev.pTx else prev.pRx
    val currValue = if (getTx) curr.pTx else curr.pRx

    // at が [prev.time, curr.time] の外側なら端点に張り付ける
    // (2 点線形補間: ラグランジュのような 3 次オーバーシュートが起きないため
    //  従来の「上昇方向で超過」ガードは不要)
    if (at <= prev.time) return prevValue.coerceIn(0, 1000)
    if (at >= curr.time) return currValue.coerceIn(0, 1000)

    val fraction = (at - prev.time).toDouble() / lastIntervalTime.toDouble()
    val p = (prevValue + (currValue - prevValue) * fraction).toInt()
    return p.coerceIn(0, 1000)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {

    MyLog.d("MySurfaceView.surfaceCreated")
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    MyLog.d("MySurfaceView.surfaceChanged[$width,$height]")

    mScreenWidth = width
    mScreenHeight = height

    startThread()

    // 初期描画のために強制的に1フレーム描画する
    sForceRedraw = true
    myDraw()
    sForceRedraw = false
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {

    MyLog.d("MySurfaceView.surfaceDestroyed")

    stopThread()
  }

  private fun startThread() {

    // 補間モードは logMode on の場合のみ有効
    if (Config.interpolateMode && Config.logBar) {

      if (mThread == null) {
        mThread = Thread(this)
        mThreadActive = true
        mThread!!.start()
        MyLog.d("MySurfaceView.startThread: thread start")
      } else {
        MyLog.d("MySurfaceView.startThread: already running")
      }
    }
  }

  /**
   * 現在の Config に合わせて補間スレッドの起動 / 停止を切り替える。
   * 設定 UI で Interpolate を ON/OFF した直後にも呼び、
   * サービス再起動を待たずに描画スレッドを即座に反映する。
   */
  fun applyInterpolationConfig() {
    if (Config.interpolateMode && Config.logBar) {
      startThread()
    } else {
      stopThread()
    }
  }

  private fun stopThread() {

    if (mThreadActive && mThread != null) {
      MyLog.d("MySurfaceView.stopThread")

      mThreadActive = false
      while (true) {
        try {
          mThread!!.join()
          break
        } catch (e: InterruptedException) {
          MyLog.e(e)
        }

      }
      mThread = null
    } else {
      MyLog.d("MySurfaceView.stopThread: no thread")
    }
  }

  companion object {

    private const val TARGET_FPS: Long = 20

    /**
     * 履歴 Traffic の保持数。
     * スパークライン(直近 60 秒分)を描画できるよう十分な余裕を持たせている。
     * 1 秒間隔で 60 秒、0.5 秒間隔で 30 秒相当を保持する。
     * 補間・スパークラインどちらも off の場合でも履歴保持コストは Traffic オブジェクト数十個分で無視できる。
     */
    private const val TRAFFIC_LIST_COUNT_MAX = 60

    // 半透明の淡黄色。バーの赤(upload)・青(download)グラデーション上でも視認しやすい。
    private const val SPARKLINE_COLOR = 0xE6FFF176.toInt()


    @Volatile
    var sForceRedraw = false
  }

}
