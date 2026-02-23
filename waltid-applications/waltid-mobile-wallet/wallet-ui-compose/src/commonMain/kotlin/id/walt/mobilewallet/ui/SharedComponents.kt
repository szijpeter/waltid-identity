package id.walt.mobilewallet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.model.CredentialSummary

@Composable
fun CredentialSummaryCard(
    credential: CredentialSummary,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = credential.title, fontWeight = FontWeight.SemiBold)
            Text(text = credential.id.value, style = MaterialTheme.typography.bodySmall)
            Text(text = credential.format.name, style = MaterialTheme.typography.labelSmall)
            credential.issuerName?.let {
                Text(text = "Issuer: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ErrorCard(
    error: WalletError,
    onDismiss: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(error.toDisplayMessage())
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

fun WalletError.toDisplayMessage(): String = when (this) {
    is WalletError.Validation -> buildString {
        append(message)
        issues.forEach { issue ->
            append("\n- ")
            append(issue.path?.let { "$it: " }.orEmpty())
            append(issue.message)
        }
    }

    is WalletError.Network -> {
        if (statusCode != null) "$message (HTTP $statusCode)" else message
    }

    else -> message
}
