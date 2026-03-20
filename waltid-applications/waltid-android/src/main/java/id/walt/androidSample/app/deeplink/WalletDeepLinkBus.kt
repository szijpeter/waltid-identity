package id.walt.androidSample.app.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WalletDeepLinkBus {
    private val mutableEvents = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 16)
    val events: SharedFlow<String> = mutableEvents.asSharedFlow()

    fun publish(uri: String?) {
        val normalized = uri?.trim().orEmpty()
        if (normalized.isNotEmpty()) {
            mutableEvents.tryEmit(normalized)
        }
    }
}
