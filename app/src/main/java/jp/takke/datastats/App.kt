package jp.takke.datastats

import android.app.Application

import com.google.android.material.color.DynamicColors
import jp.takke.util.MyLog

@Suppress("unused")
class App : Application() {

  override fun onCreate() {
    super.onCreate()

    // ログの設定
    MyLog.setContext(this)

    // Material You (Android 12+) の動的カラーを全 Activity に適用する
    DynamicColors.applyToActivitiesIfAvailable(this)

    MyLog.i("start")
  }

  override fun onTerminate() {
    super.onTerminate()

    MyLog.close()
  }
}
