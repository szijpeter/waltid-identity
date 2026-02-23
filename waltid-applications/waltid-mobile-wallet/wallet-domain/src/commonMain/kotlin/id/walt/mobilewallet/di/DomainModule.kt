package id.walt.mobilewallet.di

import org.koin.core.module.dsl.factoryOf

import id.walt.mobilewallet.domain.AcceptIssuanceUseCase
import id.walt.mobilewallet.domain.GetCredentialUseCase
import id.walt.mobilewallet.domain.HandleScannedRequestUseCase
import id.walt.mobilewallet.domain.ListCredentialsUseCase
import id.walt.mobilewallet.domain.ListDidsUseCase
import id.walt.mobilewallet.domain.ListKeysUseCase
import id.walt.mobilewallet.domain.LoginUseCase
import id.walt.mobilewallet.domain.ResolveIssuanceUseCase
import id.walt.mobilewallet.domain.ResolvePresentationUseCase
import id.walt.mobilewallet.domain.SetDefaultDidUseCase
import id.walt.mobilewallet.domain.SignVerifyUseCase
import id.walt.mobilewallet.domain.SubmitPresentationUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { LoginUseCase(get()) }
    factory { ListCredentialsUseCase(get()) }
    factory { GetCredentialUseCase(get()) }
    factory { HandleScannedRequestUseCase() }
    factory { ResolveIssuanceUseCase(exchangeRepository = get(), didRepository = get()) }
    factory { AcceptIssuanceUseCase(get()) }
    factory { ResolvePresentationUseCase(get()) }
    factory { SubmitPresentationUseCase(get()) }
    factory { ListDidsUseCase(get()) }
    factory { SetDefaultDidUseCase(get()) }
    factory { ListKeysUseCase(get()) }
    factory { SignVerifyUseCase(get()) }
}
