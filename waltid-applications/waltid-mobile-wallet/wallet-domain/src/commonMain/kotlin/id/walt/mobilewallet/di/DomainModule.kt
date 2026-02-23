package id.walt.mobilewallet.di

import org.koin.core.module.dsl.factoryOf

import id.walt.mobilewallet.domain.usecase.AcceptIssuanceUseCase
import id.walt.mobilewallet.domain.usecase.GetCredentialUseCase
import id.walt.mobilewallet.domain.usecase.HandleScannedRequestUseCase
import id.walt.mobilewallet.domain.usecase.ListCredentialsUseCase
import id.walt.mobilewallet.domain.usecase.ListDidsUseCase
import id.walt.mobilewallet.domain.usecase.ListKeysUseCase
import id.walt.mobilewallet.domain.LoginUseCase
import id.walt.mobilewallet.domain.usecase.ResolveIssuanceUseCase
import id.walt.mobilewallet.domain.usecase.ResolvePresentationUseCase
import id.walt.mobilewallet.domain.usecase.SetDefaultDidUseCase
import id.walt.mobilewallet.domain.usecase.SignVerifyUseCase
import id.walt.mobilewallet.domain.usecase.SubmitPresentationUseCase
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
