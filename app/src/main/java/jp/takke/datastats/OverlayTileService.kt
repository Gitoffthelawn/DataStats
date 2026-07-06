package jp.takke.datastats

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import jp.takke.util.MyLog

/**
 * クイック設定パネルからオーバーレイの表示 / 非表示を切り替えるタイル。
 *
 * - タイル状態は `C.PREF_KEY_USER_WANTS_VISIBLE` の値と紐づく
 *   (LayerService の `mUserWantsVisible` と同期)。
 * - タップ時は `LayerService` に `show` / `hide` action を送るだけで、
 *   実際の状態遷移と prefs 更新は `LayerService.setUserWantsVisible()` 側で行う。
 * - オーバーレイ権限が付与されていない場合は `STATE_UNAVAILABLE` にし、
 *   タップ時に権限リクエスト画面を開く。
 */
class OverlayTileService : TileService() {

  override fun onStartListening() {
    super.onStartListening()
    updateTileState()
  }

  override fun onClick() {
    super.onClick()

    if (!OverlayUtil.checkOverlayPermission(this)) {
      // 権限がない場合は設定画面へ導線
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
      ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      startActivityAndCollapse(intent)
      return
    }

    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    val newVisible = !pref.getBoolean(C.PREF_KEY_USER_WANTS_VISIBLE, true)

    // 状態遷移は LayerService 側で prefs / mUserWantsVisible / 通知を一元管理するため
    // タイルは prefs を書かず、action だけ投げる。
    val serviceIntent = Intent(this, LayerService::class.java).apply {
      action = if (newVisible) "show" else "hide"
    }
    ContextCompat.startForegroundService(this, serviceIntent)

    // 楽観的にタイル表示を更新(直後に LayerService から requestTileUpdate() でも呼ばれる)
    setTileState(newVisible)
  }

  private fun updateTileState() {
    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    val visible = pref.getBoolean(C.PREF_KEY_USER_WANTS_VISIBLE, true)
    val hasPermission = OverlayUtil.checkOverlayPermission(this)
    setTileState(visible, hasPermission)
  }

  private fun setTileState(visible: Boolean, hasPermission: Boolean = true) {
    val tile = qsTile ?: return
    tile.state = when {
      !hasPermission -> Tile.STATE_UNAVAILABLE
      visible -> Tile.STATE_ACTIVE
      else -> Tile.STATE_INACTIVE
    }
    tile.label = getString(R.string.tile_label)
    tile.updateTile()
  }

  companion object {
    /**
     * 別コンポーネント(LayerService 等)から呼ばれる。
     * タイルが listening 中であれば `onStartListening()` が再度発火し、
     * 最新の prefs 値でタイルが更新される。
     */
    fun requestTileUpdate(context: Context) {
      try {
        TileService.requestListeningState(
          context.applicationContext,
          ComponentName(context, OverlayTileService::class.java),
        )
      } catch (e: Exception) {
        MyLog.e(e)
      }
    }
  }
}
