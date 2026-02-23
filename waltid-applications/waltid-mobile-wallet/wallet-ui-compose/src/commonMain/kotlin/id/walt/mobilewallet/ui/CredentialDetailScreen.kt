package id.walt.mobilewallet.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CredentialDetailScreen(
    state: WalletUiState,
    route: WalletRoute.CredentialDetail,
    onBack: () -> Unit,
) {
    Text(text = "Credential detail", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    val detail = state.credentialDetail
    if (detail == null || detail.summary.id != route.credentialId) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Loading credential detail...")
    } else {
        Text(detail.summary.title, fontWeight = FontWeight.SemiBold)
        Text("ID: ${detail.summary.id.value}")
        Text("Format: ${detail.summary.format}")
        detail.summary.issuerName?.let { Text("Issuer: $it") }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = detail.credential.document,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = onBack) {
        Text("Back to dashboard")
    }
}
