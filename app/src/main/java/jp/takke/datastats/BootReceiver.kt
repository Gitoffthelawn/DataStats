package jp.takke.datastats

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import jp.takke.util.MyLog

class BootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {

    MyLog.i("BootReceiver.onReceive")

    if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
      // 端末起動時の処理

      // 自動起動の確認
      val pref = PreferenceManager.getDefaultSharedPreferences(context)
      val startOnBoot = pref.getBoolean(C.PREF_KEY_START_ON_BOOT, true)

      MyLog.i("start on boot[" + (if (startOnBoot) "YES" else "NO") + "]")

      if (startOnBoot) {
        // サービス起動
        val serviceIntent = Intent(context, LayerService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
          context.startForegroundService(serviceIntent)
        } else {
          context.startService(serviceIntent)
        }
      }
    }

  }
}
