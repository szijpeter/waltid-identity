package id.walt.mobilewallet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun WalletAppRoot(
    incomingRequests: Flow<String>,
) {
    val machine: MobileWalletStateMachine = koinInject()
    val state by machine.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(machine) {
        incomingRequests.collect { rawRequest ->
            if (machine.state.value.walletId != null) {
                machine.submitIncomingRequest(rawRequest)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = "walt.id Mobile Wallet", style = MaterialTheme.typography.headlineSmall)
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
                is WalletRoute.Login -> LoginScreen(
                    email = route.email,
                    password = route.password,
                    isLoading = state.isLoading,
                    error = state.lastError,
                    onEmailChanged = machine::updateLoginEmail,
                    onPasswordChanged = machine::updateLoginPassword,
                    onSubmit = { scope.launch { machine.submitLogin() } },
                    onDismissError = machine::dismissError,
                )

                WalletRoute.Dashboard -> DashboardScreen(
                    state = state,
                    onScanRequest = { 
                        machine.updateScanInput("")
                    },
                    onRefresh = { scope.launch { machine.refreshDashboard() } },
                    onOpenCredential = { credentialId ->
                        scope.launch { machine.openCredentialDetail(credentialId) }
                    },
                )

                is WalletRoute.Scan -> ScanScreen(
                    scanInput = route.scanInput,
                    isLoading = state.isLoading,
                    onInputChanged = machine::updateScanInput,
                    onSubmit = { scope.launch { machine.handleScanInput() } },
                    onCancel = machine::cancelCurrentFlow,
                )

                is WalletRoute.Issuance -> IssuanceScreen(
                    preview = route.pendingIssuance,
                    dids = state.dids,
                    onAccept = { didId -> scope.launch { machine.acceptIssuance(didId) } },
                    onCancel = machine::cancelCurrentFlow,
                )

                is WalletRoute.Presentation -> PresentationScreen(
                    matchedCredentials = route.pendingPresentation?.matchedCredentials.orEmpty(),
                    onSubmit = { selected ->
                        scope.launch { machine.submitPresentation(selected, disclosures = emptyMap()) }
                    },
                    onCancel = machine::cancelCurrentFlow,
                )

                is WalletRoute.CredentialDetail -> CredentialDetailScreen(
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
}
