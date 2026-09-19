plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace="com.hyouka.zombieshooter"
    compileSdk=35
    defaultConfig { applicationId="com.hyouka.zombieshooter"; minSdk=23; targetSdk=35; versionCode=1; versionName="1.0" }
    buildTypes { release { isMinifyEnabled=false } }
}