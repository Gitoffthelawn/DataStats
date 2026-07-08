package jp.takke.datastats

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import jp.takke.util.MyLog

/**
 * 現在のデフォルトネットワークの transport 種別を追跡する。
 *
 * - API 24+: `registerDefaultNetworkCallback` で監視する。
 *   VPN がデフォルトになった場合も通知される(NetworkRequest 版はデフォルトで
 *   NOT_VPN capability が付くため VPN 遷移を受け取れない)。
 *   また `onCapabilitiesChanged` で配信された caps から直接判定するため、
 *   イベントごとの binder 往復(activeNetwork / getNetworkCapabilities)が発生しない。
 * - API 23: NetworkRequest 版で登録し、イベント時に activeNetwork を再判定する
 *   (VPN 遷移は取りこぼす可能性があるが、旧 API の制約として許容する)。
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

      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          // default network callback: 配信された caps から直接判定(binder 往復なし)
          update(classify(caps))
        } else {
          refresh()
        }
      }
    }
    callback = cb
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        cm.registerDefaultNetworkCallback(cb)
      } else {
        val request = NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build()
        cm.registerNetworkCallback(request, cb)
      }
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
    update(detectCurrent())
  }

  private fun update(newType: NetworkType) {
    if (newType != currentType) {
      currentType = newType
      MyLog.d { "NetworkTypeMonitor: $newType" }
      onChangedListener?.invoke(newType)
    }
  }

  private fun classify(caps: NetworkCapabilities): NetworkType {
    // VPN が最優先(モバイル/WiFi の上に VPN が張られると activeNetwork は VPN になる)
    return when {
      caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
      else -> NetworkType.NONE
    }
  }

  private fun detectCurrent(): NetworkType {
    // ACCESS_NETWORK_STATE が実行時に無効化されているケース等の保険。
    return try {
      val network = cm.activeNetwork ?: return NetworkType.NONE
      val caps = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE
      classify(caps)
    } catch (e: SecurityException) {
      MyLog.e(e)
      NetworkType.NONE
    }
  }
}
