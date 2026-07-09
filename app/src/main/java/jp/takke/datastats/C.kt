package jp.takke.datastats

object C {
  const val PREF_KEY_DEBUG_MODE = "debugMode"
  const val PREF_KEY_X_POS = "xPos"
  const val PREF_KEY_INTERVAL_MSEC = "intervalMsec"
  const val PREF_KEY_BAR_MAX_SPEED_KB = "barMaxSpeedKB"
  const val PREF_KEY_START_ON_BOOT = "startOnBoot"
  const val PREF_KEY_LOGARITHM_BAR = "logBar"
  const val PREF_KEY_HIDE_WHEN_IN_FULLSCREEN = "hideWhenInFullscreen"
  const val PREF_KEY_INTERPOLATE_MODE = "interpolateMode"
  const val PREF_KEY_TEXT_SIZE_SP = "textSizeSp"
  const val PREF_KEY_UNIT_TYPE_BPS = "unitTypeBps"
  const val PREF_KEY_SPARKLINE_MODE = "sparklineMode"
  const val PREF_KEY_SHOW_ONLY_ON_MOBILE = "showOnlyOnMobile"
  const val PREF_KEY_MOBILE_ONLY_METER = "mobileOnlyMeter"
  const val PREF_KEY_SHOW_NETWORK_TYPE_ICON = "showNetworkTypeIcon"
  const val PREF_KEY_AUTO_UNIT_SCALE = "autoUnitScale"
  const val PREF_KEY_OVERLAY_AT_BOTTOM = "overlayAtBottom"
  const val PREF_KEY_OVERLAY_OPACITY = "overlayOpacity"
  const val PREF_KEY_OVERLAY_BG_COLOR = "overlayBgColor"
  const val PREF_KEY_DISPLAY_STYLE = "displayStyle"
  const val PREF_KEY_ONBOARDING_DONE = "onboardingDone"

  // ユーザが「オーバーレイ表示」を望んでいるか(通知ボタン / QS タイルで切替)。
  // QS タイル側から現在状態を参照するため prefs に永続化する。
  const val PREF_KEY_USER_WANTS_VISIBLE = "userWantsVisible"

  // LayerService の表示切替 action(通知ボタン / QS タイルから送られる)
  const val ACTION_SHOW = "show"
  const val ACTION_HIDE = "hide"
  const val ACTION_HIDE_AND_RESUME = "hide_and_resume"

  const val DEFAULT_TEXT_SIZE_SP = 8

  // 表示スタイル(displayStyle)の値
  const val DISPLAY_STYLE_BAR_AND_TEXT = 0
  const val DISPLAY_STYLE_TEXT_ONLY = 1
  const val DISPLAY_STYLE_BAR_ONLY = 2

  // オーバーレイ背景色のデフォルト(res/values/colors.xml の textBackgroundColor と同値)
  const val DEFAULT_OVERLAY_BG_COLOR = 0x66000000.toInt()

  // 不透明度[%] の下限・デフォルト
  const val MIN_OVERLAY_OPACITY = 20
  const val DEFAULT_OVERLAY_OPACITY = 100

  const val SCREEN_ON_LOGIC_DELAY_MSEC = 3000
  const val SCREEN_OFF_LOGIC_DELAY_MSEC = 3000
}
