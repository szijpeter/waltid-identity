package id.walt.mobilewallet.di

import id.walt.mobilewallet.ui.MobileWalletStateMachine
import org.koin.dsl.module

val uiModule = module {
    single {
        MobileWalletStateMachine(
            loginUseCase = get(),
            listCredentialsUseCase = get(),
            getCredentialUseCase = get(),
            handleScannedRequestUseCase = get(),
            resolveIssuanceUseCase = get(),
            acceptIssuanceUseCase = get(),
            resolvePresentationUseCase = get(),
            submitPresentationUseCase = get(),
            listDidsUseCase = get(),
            setDefaultDidUseCase = get(),
            listKeysUseCase = get(),
            signVerifyUseCase = get(),
        )
    }
}
