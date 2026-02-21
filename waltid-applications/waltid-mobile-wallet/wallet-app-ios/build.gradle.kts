plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization")
}

group = "id.walt.mobilewallet.app"

kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "walt.id mobile wallet iOS host module"
        homepage = "https://walt.id"
        version = "0.1.0"
        ios.deploymentTarget = "15.4"
        framework {
            baseName = "walletAppIos"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-ui-compose"))
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-domain"))
            implementation(project(":waltid-applications:waltid-mobile-wallet:wallet-model"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}
