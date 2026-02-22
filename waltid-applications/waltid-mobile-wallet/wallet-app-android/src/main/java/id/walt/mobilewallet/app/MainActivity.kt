package id.walt.mobilewallet.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.WalletCredential
import id.walt.mobilewallet.ui.WalletRoute
import id.walt.mobilewallet.ui.WalletUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val incomingRequests = Channel<String>(capacity = Channel.BUFFERED)
    private var dependencies: MobileWalletDependencies? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupError = runCatching {
            val runtimeConfig = MobileWalletRuntimeConfig.fromBuildConfig()
            dependencies = MobileWalletDependencies.create(runtimeConfig)
        }.exceptionOrNull()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val message = startupError?.message
                if (message != null || dependencies == null) {
                    StartupErrorScreen(
                        message = message ?: "Unable to initialize mobile wallet dependencies.",
                    )
                } else {
                    WalletAppRoot(
                        dependencies = dependencies!!,
                        incomingRequests = incomingRequests.receiveAsFlow(),
                    )
                }
            }
        }
        queueIncomingRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        queueIncomingRequest(intent)
    }

    override fun onDestroy() {
        dependencies?.close()
        dependencies = null
        super.onDestroy()
    }

    private fun queueIncomingRequest(intent: Intent?) {
        val rawRequest = intent?.dataString?.trim().orEmpty()
        if (rawRequest.isNotBlank()) {
            incomingRequests.trySend(rawRequest)
        }
    }
}

@Composable
private fun WalletAppRoot(
    dependencies: MobileWalletDependencies,
    incomingRequests: Flow<String>,
) {
    val machine = dependencies.stateMachine
    val state by machine.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(dependencies.walletId) {
        machine.bootstrap(dependencies.walletId)
    }

    LaunchedEffect(machine, dependencies.walletId) {
        incomingRequests.collect { rawRequest ->
            if (machine.state.value.walletId == null) {
                machine.bootstrap(dependencies.walletId)
            }
            machine.submitIncomingRequest(rawRequest)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "walt.id Mobile Wallet", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Android fast path", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

        state.lastError?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = machine::dismissError,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (val route = state.route) {
            WalletRoute.Dashboard -> DashboardScreen(
                state = state,
                onScanRequest = { machine.updateScanInput(state.scanInput) },
                onRefresh = { scope.launch { machine.refreshDashboard() } },
                onOpenCredential = { credentialId ->
                    scope.launch { machine.openCredentialDetail(credentialId) }
                },
            )

            WalletRoute.Scan -> ScanScreen(
                state = state,
                onInputChanged = machine::updateScanInput,
                onSubmit = { scope.launch { machine.handleScanInput() } },
                onCancel = machine::cancelCurrentFlow,
            )

            WalletRoute.Issuance -> IssuanceScreen(
                preview = state.pendingIssuance,
                dids = state.dids,
                onAccept = { didId -> scope.launch { machine.acceptIssuance(didId) } },
                onCancel = machine::cancelCurrentFlow,
            )

            WalletRoute.Presentation -> PresentationScreen(
                matchedCredentials = state.pendingPresentation?.matchedCredentials.orEmpty(),
                onSubmit = { selected ->
                    scope.launch { machine.submitPresentation(selected, disclosures = emptyMap()) }
                },
                onCancel = machine::cancelCurrentFlow,
            )

            is WalletRoute.CredentialDetail -> CredentialDetailScreen(
                state = state,
                route = route,
                onBack = { scope.launch { machine.refreshDashboard() } },
            )

            WalletRoute.DidSettings -> {
                Text("DID settings are currently managed via issuance DID selection in this fast path.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = machine::cancelCurrentFlow) { Text("Back to dashboard") }
            }

            WalletRoute.KeySettings -> {
                Text("Key settings are deferred in this fast path.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = machine::cancelCurrentFlow) { Text("Back to dashboard") }
            }

            WalletRoute.Security -> {
                Text("Security screen is deferred in this fast path.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = machine::cancelCurrentFlow) { Text("Back to dashboard") }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
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

@Composable
private fun ScanScreen(
    state: WalletUiState,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(text = "Request intake", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.scanInput,
        onValueChange = onInputChanged,
        label = { Text("OpenID request URL") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading && state.scanInput.isNotBlank(),
        ) {
            Text("Process request")
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel, enabled = !state.isLoading) {
            Text("Cancel")
        }
    }
}

@Composable
private fun IssuanceScreen(
    preview: IssuancePreview?,
    dids: List<DidDescriptor>,
    onAccept: (id.walt.mobilewallet.model.DidId?) -> Unit,
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

@Composable
private fun PresentationScreen(
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

@Composable
private fun CredentialDetailScreen(
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

@Composable
private fun CredentialSummaryCard(
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
private fun ErrorCard(
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

@Composable
private fun StartupErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Wallet startup error", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Set runtime values using Gradle properties mobileWalletBaseUrl/mobileWalletId (and optional mobileWalletBearerToken).")
    }
}

private fun WalletError.toDisplayMessage(): String = when (this) {
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
