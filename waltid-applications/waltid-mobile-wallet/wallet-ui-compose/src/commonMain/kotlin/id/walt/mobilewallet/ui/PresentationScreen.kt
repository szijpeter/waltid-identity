package id.walt.mobilewallet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.WalletCredential

@Composable
fun PresentationScreen(
    matchedCredentials: List<WalletCredential>,
    onSubmit: (List<CredentialId>) -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = "Presentation", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    if (matchedCredentials.isEmpty()) {
        Text("No matching credentials found for this request.")
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onCancel) { Text("Back to dashboard") }
        return
    }

    var selected by remember(matchedCredentials) {
        mutableStateOf(matchedCredentials.map { it.id }.toSet())
    }

    matchedCredentials.forEach { credential ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = selected.contains(credential.id),
                    onValueChange = { checked ->
                        selected = if (checked) {
                            selected + credential.id
                        } else {
                            selected - credential.id
                        }
                    },
                    role = Role.Checkbox,
                )
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = selected.contains(credential.id),
                onCheckedChange = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(credential.id.value, fontWeight = FontWeight.Medium)
                Text(
                    text = credential.format.name,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        HorizontalDivider()
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { onSubmit(selected.toList()) }, enabled = selected.isNotEmpty()) {
            Text("Submit presentation")
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
