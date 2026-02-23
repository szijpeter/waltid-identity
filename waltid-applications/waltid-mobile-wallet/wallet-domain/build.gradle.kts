plugins {
    id("waltid.multiplatform.library.mobile")
}

group = "id.walt.mobilewallet"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))
            implementation(identityLibs.kotlinx.coroutines.core)
            implementation(identityLibs.kotlinx.datetime)
            implementation(identityLibs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(identityLibs.kotlinx.coroutines.test)
        }
    }
}
