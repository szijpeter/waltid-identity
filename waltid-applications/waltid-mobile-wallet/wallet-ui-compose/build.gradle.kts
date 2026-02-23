plugins {
    id("waltid.multiplatform.library.mobile")
    alias(identityLibs.plugins.compose.multiplatform)
    alias(identityLibs.plugins.compose.compiler)
}

group = "id.walt.mobilewallet"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-domain"))
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-data"))
            implementation(identityLibs.kotlinx.coroutines.core)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(identityLibs.koin.core)
            implementation(identityLibs.koin.compose)
            implementation(identityLibs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(identityLibs.kotlinx.coroutines.test)
            implementation(identityLibs.ktor.client.core)
            implementation(identityLibs.koin.test)
        }
    }
}
