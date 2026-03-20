package id.walt.androidSample.app.features.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import id.walt.androidSample.app.deeplink.WalletDeepLinkBus
import id.walt.crypto.keys.KeyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDashboardScreen(
    initialDeepLink: String? = null,
    onOpenWalkthrough: () -> Unit = {},
    viewModel: WalletDashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(WalletTab.EXCHANGE) }
    var exchangeInput by rememberSaveable { mutableStateOf("") }
    var didInput by rememberSaveable { mutableStateOf("") }
    var selectedKeyType by rememberSaveable { mutableStateOf(KeyType.Ed25519) }

    LaunchedEffect(initialDeepLink) {
        WalletDeepLinkBus.publish(initialDeepLink)
    }
    LaunchedEffect(Unit) {
        WalletDeepLinkBus.events.collect { deepLink ->
            viewModel.handleExchangeUri(deepLink)
        }
    }

    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Ultimate Wallet (Walt Libraries First)",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "This dashboard uses waltid libraries directly (DidService, AndroidKey, CoreWalletOpenId4VCI).",
            style = MaterialTheme.typography.bodySmall,
        )

        state.infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::clearMessages) { Text("Clear messages") }
            Button(onClick = onOpenWalkthrough) { Text("Open walkthrough") }
        }

        SingleChoiceSegmentedButtonRow(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
            WalletTab.entries.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, WalletTab.entries.size),
                ) {
                    Text(tab.label)
                }
            }
        }

        when (selectedTab) {
            WalletTab.EXCHANGE -> ExchangeTab(
                value = exchangeInput,
                onValueChange = { exchangeInput = it },
                events = state.exchangeEvents,
                onProcessUri = { viewModel.handleExchangeUri(exchangeInput) },
            )

            WalletTab.DIDS -> DidsTab(
                didInput = didInput,
                onDidInput = { didInput = it },
                dids = state.dids,
                defaultDid = state.defaultDid,
                onAddOrResolveDid = {
                    viewModel.addOrResolveDid(didInput)
                    didInput = ""
                },
                onSetDefault = viewModel::setDefaultDid,
                onDeleteDid = viewModel::deleteDid,
            )

            WalletTab.KEYS -> KeysTab(
                keys = state.keys,
                selectedKeyType = selectedKeyType,
                onSelectedKeyType = { selectedKeyType = it },
                onAddKey = { viewModel.addKey(selectedKeyType) },
                onDeleteKey = viewModel::deleteKey,
            )

            WalletTab.HISTORY -> ListCard(title = "History", entries = state.history)
            WalletTab.SETTINGS -> SettingsTab()
        }
    }
}

@Composable
private fun ExchangeTab(
    value: String,
    onValueChange: (String) -> Unit,
    events: List<String>,
    onProcessUri: () -> Unit,
) {
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Exchange", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                label = { Text("Offer / presentation URI") },
            )
            Button(onClick = onProcessUri) { Text("Process URI") }
            Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
            Text("Events", style = MaterialTheme.typography.titleSmall)
            if (events.isEmpty()) {
                Text("No exchange events yet")
            } else {
                events.takeLast(10).forEach { Text("• $it") }
            }
        }
    }
}

@Composable
private fun DidsTab(
    didInput: String,
    onDidInput: (String) -> Unit,
    dids: List<String>,
    defaultDid: String?,
    onAddOrResolveDid: () -> Unit,
    onSetDefault: (String) -> Unit,
    onDeleteDid: (String) -> Unit,
) {
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("DIDs", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = didInput,
                onValueChange = onDidInput,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                label = { Text("DID (optional) — blank creates did:key") },
            )
            Button(onClick = onAddOrResolveDid) { Text("Add / Resolve DID") }
            if (dids.isEmpty()) {
                Text("No DIDs yet")
            } else {
                dids.forEach { did ->
                    Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (did == defaultDid) "$did (default)" else did,
                            modifier = androidx.compose.ui.Modifier.weight(1f),
                        )
                        if (did != defaultDid) {
                            Button(onClick = { onSetDefault(did) }) { Text("Default") }
                        }
                        Button(onClick = { onDeleteDid(did) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeysTab(
    keys: List<KeyEntry>,
    selectedKeyType: KeyType,
    onSelectedKeyType: (KeyType) -> Unit,
    onAddKey: () -> Unit,
    onDeleteKey: (String) -> Unit,
) {
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Keys", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                SupportedKeyTypes.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selectedKeyType == option.keyType,
                        onClick = { onSelectedKeyType(option.keyType) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, SupportedKeyTypes.entries.size),
                    ) {
                        Text(option.label)
                    }
                }
            }
            Button(onClick = onAddKey) { Text("Generate key") }
            if (keys.isEmpty()) {
                Text("No keys yet")
            } else {
                LazyColumn(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                    items(keys) { key ->
                        Column(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            Text("${key.keyId} (${key.keyType.name})")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = key.publicJwk.take(70) + if (key.publicJwk.length > 70) "..." else "",
                                    modifier = androidx.compose.ui.Modifier.weight(1f),
                                )
                                Button(onClick = { onDeleteKey(key.keyId) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListCard(title: String, entries: List<String>) {
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (entries.isEmpty()) {
                Text("No entries")
            } else {
                entries.takeLast(50).forEach { Text(it) }
            }
        }
    }
}

@Composable
private fun SettingsTab() {
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            Text("Mode: walt-libraries-first")
            Text("Next: iOS companion + OpenID4VP/VCI full flow on top of waltid-core-wallet.")
        }
    }
}

private enum class WalletTab(val label: String) {
    EXCHANGE("Exchange"),
    DIDS("DIDs"),
    KEYS("Keys"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

private enum class SupportedKeyTypes(val keyType: KeyType, val label: String) {
    ED25519(KeyType.Ed25519, "Ed25519"),
    P256(KeyType.secp256r1, "P-256"),
    RSA(KeyType.RSA, "RSA"),
}
