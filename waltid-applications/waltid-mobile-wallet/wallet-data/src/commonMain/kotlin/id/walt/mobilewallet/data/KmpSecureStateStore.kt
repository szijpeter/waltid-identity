package id.walt.mobilewallet.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class KmpSecureStateStore(
    private val settings: Settings
) : SecureStateStore {
    override suspend fun put(key: String, value: String) {
        settings[key] = value
    }

    override suspend fun get(key: String): String? {
        return settings.getStringOrNull(key)
    }

    override suspend fun remove(key: String) {
        settings.remove(key)
    }
}
