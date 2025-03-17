plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val DEFAULT_LIFE_CYCLE_RUNTIME_VERSION = "2.8.7"
val DEFAULT_LIFE_CYCLE_EXTENSIONS_VERSION = "2.2.0"

fun safeExtGet(prop: String, fallback: String): String {
    return if (rootProject.hasProperty(prop)) rootProject.property(prop) as String else fallback
}

android {
    namespace = "com.transistorsoft.flutter.backgroundfetch"
    compileSdk = safeExtGet("compileSdkVersion", "34").toInt()

    defaultConfig {
        minSdk = 16
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    val lifeCycleRuntimeVersion = safeExtGet("lifeCycleRuntimeVersion", DEFAULT_LIFE_CYCLE_RUNTIME_VERSION)
    val lifeCycleExtensionsVersion = safeExtGet("lifeCycleExtensionsVersion", DEFAULT_LIFE_CYCLE_EXTENSIONS_VERSION)

    implementation("com.transistorsoft:tsbackgroundfetch:+")
    implementation("androidx.lifecycle:lifecycle-runtime:$lifeCycleRuntimeVersion")
    implementation("androidx.lifecycle:lifecycle-extensions:$lifeCycleExtensionsVersion")
}