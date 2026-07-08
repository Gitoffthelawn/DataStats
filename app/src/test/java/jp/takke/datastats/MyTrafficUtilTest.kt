package jp.takke.datastats

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MyTrafficUtil の純粋関数のユニットテスト。
 * Config に依存しないよう bps / autoScale / logBar / barMaxKB は明示的に渡す。
 */
class MyTrafficUtilTest {

  //--------------------------------------------------
  // formatSpeedText: KB/s(既定)
  //--------------------------------------------------

  @Test
  fun formatSpeedText_負値は空文字() {
    assertEquals("", MyTrafficUtil.formatSpeedText(-1, bps = false, autoScale = false))
  }

  @Test
  fun formatSpeedText_ゼロ() {
    assertEquals("0.0KB/s", MyTrafficUtil.formatSpeedText(0, bps = false, autoScale = false))
  }

  @Test
  fun formatSpeedText_微小トラフィックは最低0_1を保証() {
    // 1〜102 バイト程度の通信を「0.0」と表示しない(通信中であることを視認できるように)
    assertEquals("0.1KB/s", MyTrafficUtil.formatSpeedText(1, bps = false, autoScale = false))
    assertEquals("0.1KB/s", MyTrafficUtil.formatSpeedText(50, bps = false, autoScale = false))
  }

  @Test
  fun formatSpeedText_KB表示() {
    assertEquals("1.0KB/s", MyTrafficUtil.formatSpeedText(1024, bps = false, autoScale = false))
    assertEquals("1.5KB/s", MyTrafficUtil.formatSpeedText(1536, bps = false, autoScale = false))
    assertEquals("100.0KB/s", MyTrafficUtil.formatSpeedText(102400, bps = false, autoScale = false))
  }

  @Test
  fun formatSpeedText_autoScaleオフでは1MB以上もKB表示() {
    assertEquals(
      "1024.0KB/s",
      MyTrafficUtil.formatSpeedText(1024L * 1024, bps = false, autoScale = false),
    )
    assertEquals(
      "102400.0KB/s",
      MyTrafficUtil.formatSpeedText(100L * 1024 * 1024, bps = false, autoScale = false),
    )
  }

  //--------------------------------------------------
  // formatSpeedText: 自動スケーリング
  //--------------------------------------------------

  @Test
  fun formatSpeedText_autoScaleでMB表示() {
    assertEquals(
      "1.0MB/s",
      MyTrafficUtil.formatSpeedText(1024L * 1024, bps = false, autoScale = true),
    )
    assertEquals(
      "1.5MB/s",
      MyTrafficUtil.formatSpeedText(1536L * 1024, bps = false, autoScale = true),
    )
  }

  @Test
  fun formatSpeedText_autoScaleでも1MB未満はKB表示() {
    // 1MB ちょうどの直前までは KB のまま
    assertEquals(
      "1023.9KB/s",
      MyTrafficUtil.formatSpeedText(1024L * 1024 - 1, bps = false, autoScale = true),
    )
  }

  @Test
  fun formatSpeedText_autoScaleでGB表示() {
    assertEquals(
      "1.0GB/s",
      MyTrafficUtil.formatSpeedText(1024L * 1024 * 1024, bps = false, autoScale = true),
    )
    assertEquals(
      "2.5GB/s",
      MyTrafficUtil.formatSpeedText(2560L * 1024 * 1024, bps = false, autoScale = true),
    )
  }

  //--------------------------------------------------
  // formatSpeedText: bps モード
  //--------------------------------------------------

  @Test
  fun formatSpeedText_bpsはバイトの8倍のビット値で表示() {
    // 1024 B/s = 8192 bit/s = 8.0Kbps
    assertEquals("8.0Kbps", MyTrafficUtil.formatSpeedText(1024, bps = true, autoScale = false))
  }

  @Test
  fun formatSpeedText_bpsとautoScaleの組み合わせ() {
    // 1 MB/s = 8 Mbit/s
    assertEquals(
      "8.0Mbps",
      MyTrafficUtil.formatSpeedText(1024L * 1024, bps = true, autoScale = true),
    )
  }

  //--------------------------------------------------
  // convertBytesToPerThousand: 線形モード
  //--------------------------------------------------

  @Test
  fun convertBytesToPerThousand_線形_ゼロ() {
    assertEquals(0, MyTrafficUtil.convertBytesToPerThousand(0, logBar = false, barMaxKB = 100))
  }

  @Test
  fun convertBytesToPerThousand_線形_中間値() {
    // 50KB / max 100KB: 51200 / 100 = 512
    assertEquals(
      512,
      MyTrafficUtil.convertBytesToPerThousand(50L * 1024, logBar = false, barMaxKB = 100),
    )
  }

  @Test
  fun convertBytesToPerThousand_線形_最大値超過は1000に張り付く() {
    assertEquals(
      1000,
      MyTrafficUtil.convertBytesToPerThousand(200L * 1024, logBar = false, barMaxKB = 100),
    )
  }

  //--------------------------------------------------
  // convertBytesToPerThousand: 対数モード
  //--------------------------------------------------

  @Test
  fun convertBytesToPerThousand_対数_ゼロ() {
    assertEquals(0, MyTrafficUtil.convertBytesToPerThousand(0, logBar = true, barMaxKB = 100))
  }

  @Test
  fun convertBytesToPerThousand_対数_1バイトは0() {
    // normalBytes = 1 -> log10(1) = 0
    assertEquals(0, MyTrafficUtil.convertBytesToPerThousand(1, logBar = true, barMaxKB = 100))
  }

  @Test
  fun convertBytesToPerThousand_対数_1KB() {
    // normalBytes = 1024 * 100 / 100 = 1024 -> floor(log10(1024) * 100) * 2 = 301 * 2
    assertEquals(
      602,
      MyTrafficUtil.convertBytesToPerThousand(1024, logBar = true, barMaxKB = 100),
    )
  }

  @Test
  fun convertBytesToPerThousand_対数_最大値付近() {
    // normalBytes = 102400 -> floor(log10(102400) * 100) * 2 = 501 * 2
    assertEquals(
      1002,
      MyTrafficUtil.convertBytesToPerThousand(100L * 1024, logBar = true, barMaxKB = 100),
    )
  }

  @Test
  fun convertBytesToPerThousand_対数_barMaxKBに応じてスケールする() {
    // max 10MB(10240KB)のとき 100KB: normalBytes = 102400 * 100 / 10240 = 1000
    // -> floor(log10(1000) * 100) * 2 = 300 * 2
    assertEquals(
      600,
      MyTrafficUtil.convertBytesToPerThousand(100L * 1024, logBar = true, barMaxKB = 10240),
    )
  }
}
