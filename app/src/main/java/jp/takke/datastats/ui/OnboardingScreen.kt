package jp.takke.datastats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.takke.datastats.R

/**
 * オンボーディング画面の UI 状態。
 */
data class OnboardingUiState(
  val overlayGranted: Boolean = false,
  val notificationGranted: Boolean = false,
  val notificationRequired: Boolean = false,
  val batteryOptimizationIgnored: Boolean = false,
) {
  /**
   * 開始できるかどうか。必須はオーバーレイ権限のみ。
   * 通知権限は常駐通知のボタン操作に使うだけでサービス動作には必須でないため、
   * 拒否(特に「今後表示しない」の恒久拒否)されていてもブロックしない。
   */
  val canProceed: Boolean
    get() = overlayGranted
}

/**
 * オンボーディング画面のコールバック集約。
 */
data class OnboardingCallbacks(
  val onGrantOverlay: () -> Unit = {},
  val onGrantNotification: () -> Unit = {},
  val onConfigureBattery: () -> Unit = {},
  val onComplete: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
  state: OnboardingUiState,
  callbacks: OnboardingCallbacks,
) {
  Scaffold(
    topBar = {
      TopAppBar(title = { Text(stringResource(R.string.onboarding_title)) })
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        stringResource(R.string.onboarding_intro),
        style = MaterialTheme.typography.bodyMedium,
      )

      StepCard(
        title = stringResource(R.string.onboarding_step_overlay_title),
        description = stringResource(R.string.onboarding_step_overlay_desc),
        granted = state.overlayGranted,
        actionLabel = stringResource(R.string.onboarding_grant),
        onAction = callbacks.onGrantOverlay,
      )

      if (state.notificationRequired) {
        StepCard(
          title = stringResource(R.string.onboarding_step_notification_title),
          description = stringResource(R.string.onboarding_step_notification_desc),
          granted = state.notificationGranted,
          actionLabel = stringResource(R.string.onboarding_grant),
          onAction = callbacks.onGrantNotification,
        )
      }

      StepCard(
        title = stringResource(R.string.onboarding_step_battery_title),
        description = stringResource(R.string.onboarding_step_battery_desc),
        granted = state.batteryOptimizationIgnored,
        actionLabel = stringResource(R.string.onboarding_configure),
        onAction = callbacks.onConfigureBattery,
      )

      Spacer(Modifier.height(8.dp))

      if (!state.canProceed) {
        Text(
          stringResource(R.string.onboarding_required_missing),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Button(
        onClick = callbacks.onComplete,
        enabled = state.canProceed,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.onboarding_start))
      }
    }
  }
}

@Composable
private fun StepCard(
  title: String,
  description: String,
  granted: Boolean,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
        )
        if (granted) {
          Icon(
            Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.onboarding_granted),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      Row {
        Spacer(Modifier.weight(1f))
        if (granted) {
          Text(
            stringResource(R.string.onboarding_granted),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          )
        } else {
          OutlinedButton(onClick = onAction) {
            Text(actionLabel)
          }
        }
      }
    }
  }
}
