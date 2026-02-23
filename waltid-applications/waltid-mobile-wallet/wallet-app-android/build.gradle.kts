plugins {
    id("waltid.android.app")
}

group = "id.walt.mobilewallet.app"

val mobileWalletBaseUrl = ((findProperty("mobileWalletBaseUrl") as String?) ?: System.getenv("MOBILE_WALLET_BASE_URL"))
    ?.takeIf { it.isNotBlank() } ?: "https://wallet.demo.walt.id"

fun String.toBuildConfigStringLiteral(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "id.walt.mobilewallet.app"
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "id.walt.mobilewallet.app"
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "WALLET_BASE_URL", mobileWalletBaseUrl.toBuildConfigStringLiteral())
    }
}

configurations.configureEach {
    exclude(group = "com.soywiz.korlibs.krypto", module = "krypto-android")
    exclude(group = "com.google.crypto.tink", module = "tink")
}

dependencies {
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-ui-compose"))
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-domain"))
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-data"))
    implementation(identityLibs.multiplatform.settings)
    implementation(identityLibs.androidx.security.crypto)
    implementation(identityLibs.koin.android)
    implementation(identityLibs.koin.core)

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-client-auth:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
