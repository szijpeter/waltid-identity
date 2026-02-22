package id.walt.mobilewallet.domain

import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidCreateRequest
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.DidMethod
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
import id.walt.mobilewallet.model.ScannedRequest
import id.walt.mobilewallet.model.ScannedRequestKind
import id.walt.mobilewallet.model.ValidationIssue
import id.walt.mobilewallet.model.WalletId

data class ScannedRequestClassification(
    val kind: ScannedRequestKind,
    val protocolMode: id.walt.mobilewallet.model.ProtocolMode,
)

class HandleScannedRequestUseCase {
    fun classify(rawRequest: String): ScannedRequestClassification {
        val normalized = rawRequest.trim()
        val lower = normalized.lowercase()
        return ScannedRequestClassification(
            kind = detectKind(lower),
            protocolMode = detectProtocolMode(lower),
        )
    }

    operator fun invoke(rawRequest: String): WalletResult<ScannedRequest> {
        val normalized = rawRequest.trim()
        if (normalized.isEmpty()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_request",
                            message = "Scanned request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }

        val kind = classify(normalized).kind
        return ScannedRequest(raw = normalized, kind = kind).validate().toWalletResult()
    }

    private fun detectKind(lower: String): ScannedRequestKind {
        return when {
            lower.startsWith("openid-initiate-issuance://") -> ScannedRequestKind.ISSUANCE
            lower.startsWith("openid-credential-offer://") -> ScannedRequestKind.ISSUANCE
            lower.contains("credential_offer=") -> ScannedRequestKind.ISSUANCE
            lower.startsWith("openid4vp://") -> ScannedRequestKind.PRESENTATION
            lower.contains("response_type=vp_token") -> ScannedRequestKind.PRESENTATION
            lower.contains("presentation_definition=") -> ScannedRequestKind.PRESENTATION
            else -> ScannedRequestKind.UNKNOWN
        }
    }

    private fun detectProtocolMode(lower: String): id.walt.mobilewallet.model.ProtocolMode = when {
        lower.contains("dcql_query=") -> id.walt.mobilewallet.model.ProtocolMode.OPENID4VP_1_0
        lower.contains("client_id_scheme=") -> id.walt.mobilewallet.model.ProtocolMode.OPENID4VP_1_0
        lower.contains("presentation_definition_uri=") -> id.walt.mobilewallet.model.ProtocolMode.OPENID4VP_1_0
        else -> id.walt.mobilewallet.model.ProtocolMode.DRAFT_COMPAT
    }
}

class ResolveIssuanceUseCase(
    private val exchangeRepository: ExchangeRepository,
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(request: IssuanceRequest): WalletResult<IssuancePreview> {
        if (request.rawRequest.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_issuance_request",
                            message = "Issuance request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }

        val preview = exchangeRepository.resolveIssuance(request)
        val dids = didRepository.listDids(request.walletId).getOrNull().orEmpty()
        return preview.map { resolved ->
            if (resolved.didOptions.isEmpty() && dids.isNotEmpty()) {
                resolved.copy(didOptions = dids)
            } else {
                resolved
            }
        }
    }
}

class AcceptIssuanceUseCase(
    private val exchangeRepository: ExchangeRepository,
) {
    suspend operator fun invoke(request: IssuanceRequest): WalletResult<List<CredentialId>> {
        if (request.rawRequest.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_issuance_request",
                            message = "Issuance request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }
        return exchangeRepository.acceptIssuance(request)
    }
}

class ResolvePresentationUseCase(
    private val exchangeRepository: ExchangeRepository,
) {
    suspend operator fun invoke(request: PresentationRequest): WalletResult<PresentationResolution> {
        if (request.rawRequest.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_presentation_request",
                            message = "Presentation request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }

        return exchangeRepository.resolvePresentation(request).map { matched ->
            PresentationResolution(request = request, matchedCredentials = matched)
        }
    }
}

class SubmitPresentationUseCase(
    private val exchangeRepository: ExchangeRepository,
) {
    suspend operator fun invoke(selection: PresentationSelection): WalletResult<PresentationSubmissionResult> {
        return when (val validation = selection.validate()) {
            is id.walt.mobilewallet.model.ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is id.walt.mobilewallet.model.ValidationResult.Valid ->
                exchangeRepository.submitPresentation(validation.value)
        }
    }
}

class ListCredentialsUseCase(
    private val credentialRepository: CredentialRepository,
) {
    suspend operator fun invoke(walletId: WalletId): WalletResult<List<CredentialSummary>> =
        credentialRepository.listCredentials(walletId)
}

class GetCredentialUseCase(
    private val credentialRepository: CredentialRepository,
) {
    suspend operator fun invoke(walletId: WalletId, credentialId: CredentialId): WalletResult<CredentialDetail> =
        credentialRepository.getCredential(walletId, credentialId)
}

class ListDidsUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId): WalletResult<List<DidDescriptor>> = didRepository.listDids(walletId)
}

class CreateDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: DidCreateRequest): WalletResult<DidDescriptor> {
        val supportedMethods = setOf(DidMethod.KEY, DidMethod.JWK, DidMethod.WEB)
        if (request.method !in supportedMethods) {
            return WalletResult.Failure(
                WalletError.Unsupported(
                    message = "DID method ${request.method.name.lowercase()} is not in phase-1 scope."
                )
            )
        }
        return didRepository.createDid(walletId, request)
    }
}

class ImportDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: DidImportRequest): WalletResult<DidDescriptor> =
        when (val validation = request.validate()) {
            is id.walt.mobilewallet.model.ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is id.walt.mobilewallet.model.ValidationResult.Valid ->
                didRepository.importDid(walletId, validation.value)
        }
}

class DeleteDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, didId: DidId): WalletResult<Boolean> =
        didRepository.deleteDid(walletId, didId)
}

class SetDefaultDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, didId: DidId): WalletResult<Boolean> =
        didRepository.setDefaultDid(walletId, didId)
}

class ListKeysUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId): WalletResult<List<KeyDescriptor>> = keyRepository.listKeys(walletId)
}

class GenerateKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: KeyGenerateRequest): WalletResult<KeyDescriptor> =
        keyRepository.generateKey(walletId, request)
}

class ImportKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: KeyImportRequest): WalletResult<KeyDescriptor> =
        when (val validation = request.validate()) {
            is id.walt.mobilewallet.model.ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is id.walt.mobilewallet.model.ValidationResult.Valid ->
                keyRepository.importKey(walletId, validation.value)
        }
}

class ExportKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: KeyExportRequest): WalletResult<String> =
        keyRepository.exportKey(walletId, request)
}

class SignVerifyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend fun sign(walletId: WalletId, request: KeySignRequest): WalletResult<String> {
        if (request.payload.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_sign_payload",
                            message = "Payload must not be blank.",
                            path = "payload"
                        )
                    )
                )
            )
        }
        return keyRepository.sign(walletId, request)
    }

    suspend fun verify(walletId: WalletId, request: KeyVerifyRequest): WalletResult<KeyVerifyResult> {
        if (request.jwk.isBlank() || request.signedPayload.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "invalid_verify_request",
                            message = "JWK and signed payload are required.",
                            path = "verifyRequest"
                        )
                    )
                )
            )
        }
        return keyRepository.verify(walletId, request)
    }
}

private fun <T> id.walt.mobilewallet.model.ValidationResult<T>.toWalletResult(): WalletResult<T> = when (this) {
    is id.walt.mobilewallet.model.ValidationResult.Valid -> WalletResult.Success(value)
    is id.walt.mobilewallet.model.ValidationResult.Invalid -> WalletResult.Failure(WalletError.Validation(issues))
}
