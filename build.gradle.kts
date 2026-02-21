//  Uncomment the following to run the license report
// ./gradlew -p waltid-identity aggregateDependencyNotices --no-configuration-cache
//plugins {
//    id("waltid.licensereport")
//}
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.0" apply false
    id("org.jetbrains.kotlin.native.cocoapods") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("com.android.application") version "8.12.3" apply false
    id("com.github.ben-manes.versions") version "0.53.0" apply false
}

//
//subprojects {
//    if (subprojects.isEmpty()) {
//        apply(plugin = "waltid.licensereport")
//    }
//}

allprojects {
    version = "1.0.0-SNAPSHOT"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.waltid.dev/releases")
        maven("https://maven.waltid.dev/snapshots")
    }
}
