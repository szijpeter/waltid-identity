package id.walt.mobilewallet.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletModelValidationTest {

    @Test
    fun walletIdValidationRejectsBlank() {
        val result = WalletId.validate("   ")
        assertFalse(result.isValid)
    }

    @Test
    fun presentationSelectionRequiresCredentials() {
        val walletId = WalletId.validate("wallet-1").orThrow()
        val request = PresentationRequest(
            walletId = walletId,
            rawRequest = "openid4vp://request",
            verifierHost = "verifier.example.org",
            requestedCredentialTypes = listOf("OpenBadgeCredential"),
            protocolMode = ProtocolMode.DRAFT_COMPAT,
        )

        val result = PresentationSelection(
            request = request,
            selectedCredentialIds = emptyList(),
        ).validate()

        assertFalse(result.isValid)
    }

    @Test
    fun keyImportValidationAcceptsPem() {
        val result = KeyImportRequest(
            material = "-----BEGIN PRIVATE KEY-----abc-----END PRIVATE KEY-----",
            alias = "Test",
        ).validate()

        assertTrue(result.isValid)
    }

    @Test
    fun didImportValidationRequiresKeyInformation() {
        val result = DidImportRequest(
            did = "did:key:z6Mkp...",
            alias = "Imported DID",
        ).validate()

        assertFalse(result.isValid)
    }
}
