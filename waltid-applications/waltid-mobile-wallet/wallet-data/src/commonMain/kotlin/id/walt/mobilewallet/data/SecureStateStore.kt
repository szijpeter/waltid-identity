package id.walt.mobilewallet.data

import id.walt.mobilewallet.domain.SecurityRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.DeviceIntegrityLevel
import id.walt.mobilewallet.model.DeviceIntegritySignal
import id.walt.mobilewallet.model.SecuritySettings
import id.walt.mobilewallet.model.WalletId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SecureStateStore {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
}

interface StateCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}

object NoOpStateCipher : StateCipher {
    override fun encrypt(plaintext: String): String = plaintext
    override fun decrypt(ciphertext: String): String = ciphertext
}

class InMemorySecureStateStore : SecureStateStore {
    private val lock = Mutex()
    private val state = mutableMapOf<String, String>()

    override suspend fun put(key: String, value: String) {
        lock.withLock {
            state[key] = value
        }
    }

    override suspend fun get(key: String): String? = lock.withLock { state[key] }

    override suspend fun remove(key: String) {
        lock.withLock {
            state.remove(key)
        }
    }
}

class EncryptedSecureStateStore(
    private val delegate: SecureStateStore,
    private val cipher: StateCipher = NoOpStateCipher,
) : SecureStateStore {
    override suspend fun put(key: String, value: String) {
        delegate.put(key, cipher.encrypt(value))
    }

    override suspend fun get(key: String): String? {
        return delegate.get(key)?.let(cipher::decrypt)
    }

    override suspend fun remove(key: String) {
        delegate.remove(key)
    }
}

class InMemorySecurityRepository(
    private val biometricAvailable: Boolean = false,
) : SecurityRepository {
    private val lock = Mutex()
    private val settings = mutableMapOf<String, SecuritySettings>()

    override suspend fun getSecuritySettings(walletId: WalletId): WalletResult<SecuritySettings> = lock.withLock {
        WalletResult.Success(settings[walletId.value] ?: SecuritySettings())
    }

    override suspend fun setSecuritySettings(
        walletId: WalletId,
        settings: SecuritySettings,
    ): WalletResult<SecuritySettings> = lock.withLock {
        this.settings[walletId.value] = settings
        WalletResult.Success(settings)
    }

    override suspend fun getDeviceIntegritySignal(): WalletResult<DeviceIntegritySignal> =
        WalletResult.Success(DeviceIntegritySignal(level = DeviceIntegrityLevel.UNKNOWN))

    override suspend fun requireBiometricGate(reason: String): WalletResult<Boolean> =
        WalletResult.Success(biometricAvailable)
}
