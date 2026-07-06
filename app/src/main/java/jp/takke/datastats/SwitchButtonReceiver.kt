package jp.takke.datastats

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

import jp.takke.util.MyLog

/**
 * 通知のカスタムボタンからの押下イベントレシーバー
 */
class SwitchButtonReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {

    val action = intent?.action
    MyLog.d("SwitchButtonReceiver.onReceive [$action]")


    // action を引き継いで LayerService.onStartCommand に投げる
    // startService() だとサービス停止直後のタイミング等でバックグラウンド起動制限に触れ
    // IllegalStateException が発生し得るため startForegroundService に統一する。
    val serviceIntent = Intent(context, LayerService::class.java)
    serviceIntent.action = action
    ContextCompat.startForegroundService(context, serviceIntent)
  }
}
