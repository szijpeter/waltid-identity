package id.walt.mobilewallet.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.DidId

@Composable
fun IssuanceScreen(
    preview: IssuancePreview?,
    dids: List<DidDescriptor>,
    onAccept: (DidId?) -> Unit,
    onCancel: () -> Unit,
) {
    if (preview == null) {
        Text("No pending issuance request.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCancel) { Text("Back to dashboard") }
        return
    }

    Text(text = "Issuance", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Issuer: ${preview.issuerHost}")
    Text("Credential types: ${preview.credentialTypes.joinToString().ifBlank { "unknown" }}")
    Spacer(modifier = Modifier.height(12.dp))

    val didOptions = if (preview.didOptions.isNotEmpty()) preview.didOptions else dids
    var selectedDidId by remember(preview, didOptions) {
        mutableStateOf(
            didOptions.firstOrNull { it.isDefault }?.id ?: didOptions.firstOrNull()?.id
        )
    }

    if (didOptions.isNotEmpty()) {
        Text("Select DID", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        didOptions.forEach { did ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = selectedDidId == did.id,
                        onValueChange = { selectedDidId = did.id },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = selectedDidId == did.id,
                    onClick = { selectedDidId = did.id },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = did.alias ?: did.id.value)
            }
        }
    } else {
        Text("No DIDs available. Issuance will use backend defaults.")
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { onAccept(selectedDidId) }) {
            Text("Accept issuance")
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
