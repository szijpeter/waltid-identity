plugins {
    id("waltid.multiplatform.library.common")

    id("love.forte.plugin.suspend-transform")
}

fun getSetting(name: String) = providers.gradleProperty(name).orNull.toBoolean()
val enableIosBuild = getSetting("enableIosBuild")

kotlin {
    jvm()

    if (enableIosBuild) {
        iosArm64()
        iosSimulatorArm64()
    }
}

suspendTransformPlugin {
    enabled = true
    includeRuntime = true
    transformers { useDefault() }

    //includeAnnotation = false // Required in the current version to avoid "compileOnly" warning
}
