package id.walt.mobilewallet.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.walt.mobilewallet.model.CredentialId

@Composable
fun DashboardScreen(
    state: WalletUiState,
    onScanRequest: () -> Unit,
    onRefresh: () -> Unit,
    onOpenCredential: (CredentialId) -> Unit,
) {
    Text(text = "Wallet: ${state.walletId?.value ?: "uninitialized"}", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onRefresh, enabled = !state.isLoading) {
            Text("Refresh")
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onScanRequest, enabled = !state.isLoading) {
            Text("Scan / Enter Request")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(text = "Credentials", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    if (state.credentials.isEmpty()) {
        Text(text = "No credentials available.")
        return
    }

    state.credentials.forEach { credential ->
        CredentialSummaryCard(
            credential = credential,
            onOpen = { onOpenCredential(credential.id) },
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
