package id.walt.mobilewallet.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(
    scanInput: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = "Request intake", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = scanInput,
        onValueChange = onInputChanged,
        label = { Text("OpenID request URL") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSubmit,
            enabled = !isLoading && scanInput.isNotBlank(),
        ) {
            Text("Process request")
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel, enabled = !isLoading) {
            Text("Cancel")
        }
    }
}
