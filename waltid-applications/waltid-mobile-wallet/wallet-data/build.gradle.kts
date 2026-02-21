plugins {
    id("waltid.multiplatform.library.mobile")
}

group = "id.walt.mobilewallet"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-domain"))

            implementation(identityLibs.kotlinx.serialization.json)
            implementation(identityLibs.kotlinx.coroutines.core)
            implementation(identityLibs.bundles.waltid.ktor.client)

            implementation(project(":waltid-libraries:protocols:waltid-openid4vc"))
            implementation(project(":waltid-libraries:protocols:waltid-openid4vp-wallet"))
            implementation(project(":waltid-libraries:credentials:waltid-digital-credentials"))
            implementation(project(":waltid-libraries:waltid-core-wallet"))
            implementation(project(":waltid-libraries:waltid-did"))
            implementation(project(":waltid-libraries:sdjwt:waltid-sdjwt"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(identityLibs.kotlinx.coroutines.test)
        }
    }
}
