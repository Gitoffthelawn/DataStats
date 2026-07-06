package jp.takke.datastats

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import jp.takke.datastats.ui.AppTheme
import jp.takke.datastats.ui.ConfigUiState
import jp.takke.datastats.ui.MainScreen
import jp.takke.datastats.ui.MainScreenCallbacks
import jp.takke.util.MyLog
import jp.takke.util.TkConfig


class MainActivity : ComponentActivity() {

  private var mServiceIF: ILayerService? = null

  // bindService() 呼び出し済みかどうか。
  // onServiceConnected が呼ばれる前に onDestroy が来ても unbind が漏れないよう
  // 接続状態(mServiceIF)ではなく bind 状態で unbind の判定を行う
  private var mBound = false

  // Compose UI 状態
  private var mUiState by mutableStateOf(ConfigUiState())

  private val mServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {

      MyLog.d("onServiceConnected[$name]")

      mServiceIF = ILayerService.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName) {

      MyLog.d("onServiceDisconnected[$name]")

      mServiceIF = null
    }
  }

  private val overlayPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (OverlayUtil.checkOverlayPermission(this)) {
        MyLog.i("MainActivity: overlay permission OK")

        // restart service
        doStopService()
        doRestartService()
      } else {
        MyLog.i("MainActivity: overlay permission NG")
        finish()
      }
    }

  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
      if (isGranted) {
        MyLog.d("PreviewActivity: POST_NOTIFICATION: 通知許可")
      } else {
        MyLog.i("PreviewActivity: POST_NOTIFICATION: 通知許可しない")
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          Toast.makeText(this, "通知を受け取るには許可が必要です", Toast.LENGTH_LONG).show()
        } else {
          MyLog.i("PreviewActivity: POST_NOTIFICATION: 通知許可しない(永続的)")

          NotificationPermissionUtil.showNotificationPermissionRationaleDialog(
            this,
            onOk = { MyLog.d("通知権限: OK") },
            onCancel = { MyLog.d("通知権限: キャンセル") },
          )
        }
      }
    }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun checkNotificationPermission() {
    val notificationPermission =
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
    val notificationRationale =
      shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    if (notificationPermission == PackageManager.PERMISSION_GRANTED) {
      MyLog.d("PreviewActivity: POST_NOTIFICATION: 通知許可済み")
    } else {
      if (notificationRationale) {
        MyLog.i("PreviewActivity: POST_NOTIFICATION: 以前に「許可をしない」を選択済み")

        NotificationPermissionUtil.showNotificationPermissionRationaleDialog(
          this,
          onOk = {
            MyLog.d("通知権限: OK")
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          },
          onCancel = { MyLog.d("通知権限: キャンセル") },
        )
      } else {
        MyLog.d("PreviewActivity: POST_NOTIFICATION: 通知許可リクエスト")
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      checkNotificationPermission()
    }

    MyLog.d("MainActivity.onCreate")

    loadUiStateFromPrefs()

    setContent {
      AppTheme {
        MainScreen(state = mUiState, callbacks = buildCallbacks())
      }
    }

    if (!OverlayUtil.checkOverlayPermission(this)) {
      requestOverlayPermission()
    } else {
      doBindService()
    }

    MyLog.deleteBigExternalLogFile()
  }

  private fun loadUiStateFromPrefs() {
    Config.loadPreferences(this)
    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    mUiState = ConfigUiState(
      autoStartOnBoot = pref.getBoolean(C.PREF_KEY_START_ON_BOOT, true),
      hideWhenInFullscreen = Config.hideWhenInFullscreen,
      logBar = Config.logBar,
      interpolateMode = Config.interpolateMode,
      textSizeSp = Config.textSizeSp,
      xPos = Config.xPos,
      intervalMs = Config.intervalMs,
      barMaxKB = Config.barMaxKB,
      unitTypeBps = Config.unitTypeBps,
      debugMode = TkConfig.debugMode,
      previewLabel = "-",
      previewSlider = 0,
    )
  }

  private fun buildCallbacks(): MainScreenCallbacks = MainScreenCallbacks(
    onAutoStartOnBootChange = { checked ->
      savePref { putBoolean(C.PREF_KEY_START_ON_BOOT, checked) }
      mUiState = mUiState.copy(autoStartOnBoot = checked)
    },
    onHideWhenInFullscreenChange = { checked ->
      savePref { putBoolean(C.PREF_KEY_HIDE_WHEN_IN_FULLSCREEN, checked) }
      mUiState = mUiState.copy(hideWhenInFullscreen = checked)
    },
    onLogBarChange = { checked ->
      savePref { putBoolean(C.PREF_KEY_LOGARITHM_BAR, checked) }
      mUiState = mUiState.copy(logBar = checked)
      doRestartService()
    },
    onInterpolateChange = { checked ->
      savePref { putBoolean(C.PREF_KEY_INTERPOLATE_MODE, checked) }
      mUiState = mUiState.copy(interpolateMode = checked)
      // Surface を作り直すため一度停止してから再起動
      doStopService()
      doRestartService()
    },
    onTextSizeDelta = { delta -> updateTextSize(delta) },
    onXPosChange = { pos ->
      if (pos == mUiState.xPos) return@MainScreenCallbacks
      savePref { putInt(C.PREF_KEY_X_POS, pos) }
      mUiState = mUiState.copy(xPos = pos)
      doRestartService()
    },
    onIntervalChange = { interval ->
      if (interval == mUiState.intervalMs) return@MainScreenCallbacks
      savePref { putInt(C.PREF_KEY_INTERVAL_MSEC, interval) }
      mUiState = mUiState.copy(intervalMs = interval)
      doRestartService()
    },
    onBarMaxChange = { speed ->
      if (speed == mUiState.barMaxKB) return@MainScreenCallbacks
      savePref { putInt(C.PREF_KEY_BAR_MAX_SPEED_KB, speed) }
      mUiState = mUiState.copy(barMaxKB = speed)
      doRestartService()
    },
    onUnitTypeChange = { bps ->
      if (bps == mUiState.unitTypeBps) return@MainScreenCallbacks
      savePref { putBoolean(C.PREF_KEY_UNIT_TYPE_BPS, bps) }
      mUiState = mUiState.copy(unitTypeBps = bps)
      doRestartService()
    },
    onPreviewSliderChange = { progress ->
      val kb = progress / 10
      val kbd1 = progress % 10
      mUiState = mUiState.copy(
        previewSlider = progress,
        previewLabel = "$kb.${kbd1}KB",
      )
      startSnapshot(kb.toLong() * 1024 + kbd1.toLong() * 100)
    },
    onSampleClick = { kb ->
      val progress = kb * 10
      mUiState = mUiState.copy(
        previewSlider = progress,
        previewLabel = "$kb.0KB",
      )
      startSnapshot(kb.toLong() * 1024)
    },
    onStart = { doRestartService() },
    onStop = { doStopService() },
    onRestart = {
      doStopService()
      doRestartService()
    },
    onToggleDebug = {
      val newValue = !mUiState.debugMode
      TkConfig.debugMode = newValue
      savePref { putBoolean(C.PREF_KEY_DEBUG_MODE, newValue) }
      mUiState = mUiState.copy(debugMode = newValue)
    },
  )

  private inline fun savePref(action: android.content.SharedPreferences.Editor.() -> Unit) {
    PreferenceManager.getDefaultSharedPreferences(this).edit(commit = false, action = action)
  }

  private fun doBindService() {

    val serviceIntent = Intent(this, LayerService::class.java)

    MyLog.d("MainActivity: startService of LayerService")
    if (Build.VERSION.SDK_INT >= 26) {
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }

    MyLog.d("MainActivity: bindService of LayerService")
    if (bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
      mBound = true
    }
  }

  private fun requestOverlayPermission() {
    val intent =
      Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
    overlayPermissionLauncher.launch(intent)
  }

  override fun onPause() {

    // プレビュー状態の解除
    if (mServiceIF != null) {
      try {
        mServiceIF!!.restart()
      } catch (e: RemoteException) {
        MyLog.e(e)
      }
    }

    super.onPause()
  }

  override fun onDestroy() {

    // onServiceConnected 前に onDestroy が来た場合、mServiceIF が null でも bind は生きているため
    // mBound で判定する(接続状態ではリークが発生する)
    if (mBound) {
      unbindService(mServiceConnection)
      mBound = false
      mServiceIF = null
    }

    super.onDestroy()
  }

  private fun doStopService() {

    if (mServiceIF != null) {
      try {
        mServiceIF!!.stop()
      } catch (e: RemoteException) {
        MyLog.e(e)
      }
    }

    if (mBound) {
      unbindService(mServiceConnection)
      mBound = false
      mServiceIF = null
    }
  }

  private fun doRestartService() {

    MyLog.d("MainActivity.doRestartService")

    if (mServiceIF != null) {
      try {
        mServiceIF!!.restart()
      } catch (e: RemoteException) {
        MyLog.e(e)
      }
    } else {
      // rebind
      doBindService()
    }

    mUiState = mUiState.copy(previewLabel = "-")
  }

  @SuppressLint("SetTextI18n")
  private fun updateTextSize(delta: Int) {

    val newSize = Config.textSizeSp + delta
    if (newSize < 6 || newSize > 24) return
    Config.textSizeSp = newSize

    savePref { putInt(C.PREF_KEY_TEXT_SIZE_SP, Config.textSizeSp) }
    mUiState = mUiState.copy(textSizeSp = Config.textSizeSp)

    Config.loadPreferences(this)

    // 直接 static 変数を書き換える裏口的な結合を避けるため AIDL 経由で強制再描画を伝達する
    forceRedraw(1)

    Handler(Looper.getMainLooper()).postDelayed({

      forceRedraw(1)

      doRestartService()
    }, 1)
  }

  private fun forceRedraw(previewBytes: Long) {
    if (mServiceIF != null) {
      try {
        mServiceIF!!.forceRedraw(previewBytes)
      } catch (e: RemoteException) {
        MyLog.e(e)
      }
    }
  }

  private fun startSnapshot(previewBytes: Long) {
    if (mServiceIF != null) {
      try {
        mServiceIF!!.startSnapshot(previewBytes)
      } catch (e: RemoteException) {
        MyLog.e(e)
      }
    }
  }
}
