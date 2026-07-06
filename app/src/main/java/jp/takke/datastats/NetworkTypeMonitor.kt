package jp.takke.datastats

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import jp.takke.util.MyLog

/**
 * 現在のデフォルトネットワークの transport 種別を追跡する。
 *
 * `ConnectivityManager.NetworkCallback` で以下を監視:
 *  - Wi-Fi / モバイル / イーサネット / VPN / 未接続 の遷移
 *  - `onAvailable` / `onLost` / `onCapabilitiesChanged` すべてで最新種別を再判定
 *
 * `start()` / `stop()` はライフサイクル(Service.onCreate / onDestroy 等)から明示的に呼ぶ。
 */
class NetworkTypeMonitor(context: Context) {

  enum class NetworkType {
    WIFI, MOBILE, ETHERNET, VPN, NONE,
  }

  private val cm =
    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private var callback: ConnectivityManager.NetworkCallback? = null

  @Volatile
  var currentType: NetworkType = detectCurrent()
    private set

  /** 種別が変化したときに呼ばれる。callback スレッド(main とは限らない)から呼ばれる可能性がある。 */
  var onChangedListener: ((NetworkType) -> Unit)? = null

  fun start() {
    if (callback != null) return
    val cb = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) = refresh()
      override fun onLost(network: Network) = refresh()
      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refresh()
    }
    callback = cb
    try {
      // minSdk 23 対応のため NetworkRequest 経由で登録(registerDefaultNetworkCallback は API 24+)
      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
      cm.registerNetworkCallback(request, cb)
    } catch (e: Exception) {
      MyLog.e(e)
    }
    refresh()
  }

  fun stop() {
    val cb = callback ?: return
    try {
      cm.unregisterNetworkCallback(cb)
    } catch (e: Exception) {
      MyLog.e(e)
    }
    callback = null
  }

  private fun refresh() {
    val newType = detectCurrent()
    if (newType != currentType) {
      currentType = newType
      MyLog.d { "NetworkTypeMonitor: $newType" }
      onChangedListener?.invoke(newType)
    }
  }

  private fun detectCurrent(): NetworkType {
    // ACCESS_NETWORK_STATE が実行時に無効化されているケース等の保険。
    return try {
      val network = cm.activeNetwork ?: return NetworkType.NONE
      val caps = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE
      // VPN が最優先(モバイル/WiFi の上に VPN が張られると activeNetwork は VPN になる)
      when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        else -> NetworkType.NONE
      }
    } catch (e: SecurityException) {
      MyLog.e(e)
      NetworkType.NONE
    }
  }
}
