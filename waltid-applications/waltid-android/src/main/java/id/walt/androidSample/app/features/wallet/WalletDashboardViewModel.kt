package id.walt.androidSample.app.features.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.walt.crypto.keys.AndroidKey
import id.walt.crypto.keys.KeyType
import id.walt.did.dids.DidService
import id.walt.wallet.CoreWalletOpenId4VCI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

data class KeyEntry(
    val keyId: String,
    val keyType: KeyType,
    val publicJwk: String,
)

data class WalletDashboardUiState(
    val dids: List<String> = emptyList(),
    val defaultDid: String? = null,
    val keys: List<KeyEntry> = emptyList(),
    val exchangeEvents: List<String> = emptyList(),
    val history: List<String> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

class WalletDashboardViewModel : ViewModel() {
    private val didInitMutex = Mutex()
    private var didServiceInitialized = false

    private val inMemoryKeys = LinkedHashMap<String, AndroidKey>()

    private val mutableState = MutableStateFlow(WalletDashboardUiState())
    val state: StateFlow<WalletDashboardUiState> = mutableState.asStateFlow()

    fun clearMessages() {
        mutableState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun addKey(type: KeyType) {
        viewModelScope.launch {
            runCatching {
                val key = AndroidKey.generate(type)
                val keyId = key.getKeyId()
                val publicJwk = key.getPublicKey().exportJWK()
                inMemoryKeys[keyId] = key

                mutableState.update {
                    it.copy(
                        keys = it.keys + KeyEntry(
                            keyId = keyId,
                            keyType = type,
                            publicJwk = publicJwk,
                        ),
                        infoMessage = "Key generated using waltid-crypto-android: $keyId",
                        errorMessage = null,
                    )
                }
                appendHistory("key.add", "$keyId (${type.name})")
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to generate key",
                    )
                }
            }
        }
    }

    fun deleteKey(keyId: String) {
        inMemoryKeys.remove(keyId)
        mutableState.update {
            it.copy(
                keys = it.keys.filterNot { key -> key.keyId == keyId },
                infoMessage = "Key removed: $keyId",
                errorMessage = null,
            )
        }
        appendHistory("key.delete", keyId)
    }

    fun addOrResolveDid(input: String) {
        viewModelScope.launch {
            runCatching {
                ensureDidServiceInitialized()
                val candidate = input.trim()
                if (candidate.isNotEmpty()) {
                    DidService.resolve(candidate).getOrThrow()
                    candidate
                } else {
                    val key = inMemoryKeys.values.lastOrNull() ?: AndroidKey.generate(KeyType.Ed25519).also {
                        val keyId = it.getKeyId()
                        inMemoryKeys[keyId] = it
                        mutableState.update { state ->
                            state.copy(
                                keys = state.keys + KeyEntry(
                                    keyId = keyId,
                                    keyType = KeyType.Ed25519,
                                    publicJwk = it.getPublicKey().exportJWK(),
                                ),
                            )
                        }
                    }
                    DidService.registerByKey("key", key).did
                }
            }.onSuccess { did ->
                mutableState.update { current ->
                    val dids = if (did in current.dids) current.dids else current.dids + did
                    current.copy(
                        dids = dids,
                        defaultDid = current.defaultDid ?: did,
                        infoMessage = "DID ready via waltid-did: $did",
                        errorMessage = null,
                    )
                }
                appendHistory("did.add", did)
            }.onFailure { error ->
                mutableState.update {
                    it.copy(errorMessage = error.message ?: "Failed to add DID")
                }
            }
        }
    }

    fun setDefaultDid(did: String) {
        mutableState.update {
            it.copy(
                defaultDid = did,
                infoMessage = "Default DID set: $did",
                errorMessage = null,
            )
        }
        appendHistory("did.default", did)
    }

    fun deleteDid(did: String) {
        mutableState.update { current ->
            val newDids = current.dids.filterNot { it == did }
            current.copy(
                dids = newDids,
                defaultDid = when {
                    current.defaultDid == did -> newDids.firstOrNull()
                    else -> current.defaultDid
                },
                infoMessage = "DID removed: $did",
                errorMessage = null,
            )
        }
        appendHistory("did.delete", did)
    }

    fun handleExchangeUri(uri: String) {
        val normalized = uri.trim()
        if (normalized.isEmpty()) return

        viewModelScope.launch {
            when (classifyExchangeUri(normalized)) {
                ExchangeUriKind.CREDENTIAL_OFFER -> resolveCredentialOffer(normalized)
                ExchangeUriKind.PRESENTATION_REQUEST -> {
                    mutableState.update {
                        it.copy(
                            exchangeEvents = it.exchangeEvents + "Presentation request deeplink received",
                            infoMessage = "Presentation URI captured",
                            errorMessage = null,
                        )
                    }
                    appendHistory("exchange.presentation", normalized.take(160))
                }

                ExchangeUriKind.UNKNOWN -> {
                    mutableState.update {
                        it.copy(errorMessage = "Unsupported wallet URI")
                    }
                }
            }
        }
    }

    private suspend fun resolveCredentialOffer(uri: String) {
        runCatching {
            val offer = CoreWalletOpenId4VCI.resolveCredentialOffer(uri)
            val issuer = offer.credentialIssuer ?: "unknown-issuer"
            val offered = when {
                offer.draft13 != null -> offer.draft13!!.credentialConfigurationIds.joinToString { it.toString() }
                offer.draft11 != null -> offer.draft11!!.credentials.joinToString { it.toString() }
                else -> "unknown-credential"
            }.ifEmpty { "unknown-credential" }
            "Credential offer resolved: issuer=$issuer credentials=$offered"
        }.onSuccess { message ->
            mutableState.update {
                it.copy(
                    exchangeEvents = it.exchangeEvents + message,
                    infoMessage = "Credential offer resolved via waltid-core-wallet",
                    errorMessage = null,
                )
            }
            appendHistory("exchange.offer", message)
        }.onFailure { error ->
            mutableState.update {
                it.copy(
                    errorMessage = error.message ?: "Failed to resolve credential offer",
                )
            }
        }
    }

    private suspend fun ensureDidServiceInitialized() {
        didInitMutex.withLock {
            if (!didServiceInitialized) {
                DidService.minimalInit()
                didServiceInitialized = true
            }
        }
    }

    private fun appendHistory(action: String, details: String) {
        val timestamp = Clock.System.now().toString()
        mutableState.update {
            it.copy(history = it.history + "$timestamp | $action | $details")
        }
    }
}

private enum class ExchangeUriKind {
    CREDENTIAL_OFFER,
    PRESENTATION_REQUEST,
    UNKNOWN,
}

private fun classifyExchangeUri(uri: String): ExchangeUriKind {
    val normalized = uri.lowercase()
    return when {
        normalized.startsWith("openid-credential-offer:") ||
            normalized.startsWith("openid-initiate-issuance:") ||
            normalized.contains("credential_offer") ||
            normalized.contains("credential_offer_uri") -> ExchangeUriKind.CREDENTIAL_OFFER

        normalized.startsWith("openid4vp:") ||
            normalized.startsWith("openid-vp:") ||
            normalized.contains("presentation_definition") ||
            normalized.contains("/verification-session/") -> ExchangeUriKind.PRESENTATION_REQUEST

        else -> ExchangeUriKind.UNKNOWN
    }
}
