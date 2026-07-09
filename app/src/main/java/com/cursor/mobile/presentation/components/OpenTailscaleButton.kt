package com.cursor.mobile.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cursor.mobile.core.platform.TailscaleHelper

enum class TailscaleButtonStyle {
    FILLED,
    TONAL,
    OUTLINED
}

@Composable
fun OpenTailscaleButton(
    modifier: Modifier = Modifier,
    style: TailscaleButtonStyle = TailscaleButtonStyle.OUTLINED,
    fillWidth: Boolean = true
) {
    val context = LocalContext.current
    val onClick: () -> Unit = { TailscaleHelper.openTailscale(context) }
    val buttonModifier = if (fillWidth) modifier.fillMaxWidth() else modifier

    when (style) {
        TailscaleButtonStyle.FILLED -> Button(
            onClick = onClick,
            modifier = buttonModifier
        ) {
            TailscaleButtonContent()
        }
        TailscaleButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier
        ) {
            TailscaleButtonContent()
        }
        TailscaleButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier
        ) {
            TailscaleButtonContent()
        }
    }
}

@Composable
private fun TailscaleButtonContent() {
    Icon(Icons.Default.VpnKey, contentDescription = null)
    Spacer(modifier = Modifier.width(8.dp))
    Text("Open Tailscale")
}
