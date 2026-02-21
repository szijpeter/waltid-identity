package id.walt.mobilewallet.domain

import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DeviceIntegritySignal
import id.walt.mobilewallet.model.DidCreateRequest
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.KeyExportRequest
import id.walt.mobilewallet.model.KeyGenerateRequest
import id.walt.mobilewallet.model.KeyImportRequest
import id.walt.mobilewallet.model.KeySignRequest
import id.walt.mobilewallet.model.KeyVerifyRequest
import id.walt.mobilewallet.model.KeyVerifyResult
import id.walt.mobilewallet.model.PresentationRequest
import id.walt.mobilewallet.model.PresentationSelection
import id.walt.mobilewallet.model.PresentationSubmissionResult
import id.walt.mobilewallet.model.SecuritySettings
import id.walt.mobilewallet.model.WalletCredential
import id.walt.mobilewallet.model.WalletId

interface CredentialRepository {
    suspend fun listCredentials(walletId: WalletId): WalletResult<List<CredentialSummary>>
    suspend fun getCredential(walletId: WalletId, credentialId: CredentialId): WalletResult<CredentialDetail>
    suspend fun listWalletCredentials(walletId: WalletId): WalletResult<List<WalletCredential>>
}

interface DidRepository {
    suspend fun listDids(walletId: WalletId): WalletResult<List<DidDescriptor>>
    suspend fun createDid(walletId: WalletId, request: DidCreateRequest): WalletResult<DidDescriptor>
    suspend fun importDid(walletId: WalletId, request: DidImportRequest): WalletResult<DidDescriptor>
    suspend fun deleteDid(walletId: WalletId, didId: DidId): WalletResult<Boolean>
    suspend fun setDefaultDid(walletId: WalletId, didId: DidId): WalletResult<Boolean>
}

interface KeyRepository {
    suspend fun listKeys(walletId: WalletId): WalletResult<List<KeyDescriptor>>
    suspend fun generateKey(walletId: WalletId, request: KeyGenerateRequest): WalletResult<KeyDescriptor>
    suspend fun importKey(walletId: WalletId, request: KeyImportRequest): WalletResult<KeyDescriptor>
    suspend fun exportKey(walletId: WalletId, request: KeyExportRequest): WalletResult<String>
    suspend fun sign(walletId: WalletId, request: KeySignRequest): WalletResult<String>
    suspend fun verify(walletId: WalletId, request: KeyVerifyRequest): WalletResult<KeyVerifyResult>
}

interface ExchangeRepository {
    suspend fun resolveIssuance(request: IssuanceRequest): WalletResult<IssuancePreview>
    suspend fun acceptIssuance(request: IssuanceRequest): WalletResult<List<CredentialId>>
    suspend fun resolvePresentation(request: PresentationRequest): WalletResult<List<WalletCredential>>
    suspend fun submitPresentation(selection: PresentationSelection): WalletResult<PresentationSubmissionResult>
}

interface SecurityRepository {
    suspend fun getSecuritySettings(walletId: WalletId): WalletResult<SecuritySettings>
    suspend fun setSecuritySettings(walletId: WalletId, settings: SecuritySettings): WalletResult<SecuritySettings>
    suspend fun getDeviceIntegritySignal(): WalletResult<DeviceIntegritySignal>
    suspend fun requireBiometricGate(reason: String): WalletResult<Boolean>
}

data class PresentationResolution(
    val request: PresentationRequest,
    val matchedCredentials: List<WalletCredential>,
)
