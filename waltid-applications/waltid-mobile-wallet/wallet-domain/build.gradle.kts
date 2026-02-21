plugins {
    id("waltid.multiplatform.library")
}

group = "id.walt.mobilewallet"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))
            implementation(identityLibs.kotlinx.coroutines.core)
            implementation(identityLibs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(identityLibs.kotlinx.coroutines.test)
        }
    }
}
