plugins {
    id("waltid.android.app")
    kotlin("plugin.serialization")
}

group = "id.walt.mobilewallet.app"

android {
    namespace = "id.walt.mobilewallet.app"

    defaultConfig {
        applicationId = "id.walt.mobilewallet.app"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-ui-compose"))
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-domain"))
    implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
