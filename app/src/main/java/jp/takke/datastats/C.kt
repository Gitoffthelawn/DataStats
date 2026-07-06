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
  const val PREF_KEY_ONBOARDING_DONE = "onboardingDone"

  const val DEFAULT_TEXT_SIZE_SP = 8

  // 初期Alarmの遅延時間[ms]
  const val ALARM_STARTUP_DELAY_MSEC = 1000

  // Service維持のためのAlarmの更新間隔[ms]
  const val ALARM_INTERVAL_MSEC = 60 * 1000

  const val SCREEN_ON_LOGIC_DELAY_MSEC = 3000
  const val SCREEN_OFF_LOGIC_DELAY_MSEC = 3000
}
