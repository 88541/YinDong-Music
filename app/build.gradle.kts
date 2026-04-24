import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
fun String.escapeForBuildConfig(): String = replace("\\", "\\\\").replace("\"", "\\\"")
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { this.load(it) }
    }
}

val apiAuthKey = (project.findProperty("API_AUTH_KEY") as String?)?.trim().orEmpty()
val apiAuthKeyFromEnv = System.getenv("API_AUTH_KEY")?.trim().orEmpty()
val apiAuthKeyFromLocal = localProps.getProperty("API_AUTH_KEY")?.trim().orEmpty()
val apiAppIdFromProject = (project.findProperty("API_APP_ID") as String?)?.trim().orEmpty()
val apiAppIdFromLocal = localProps.getProperty("API_APP_ID")?.trim().orEmpty()
val apiAppId = apiAppIdFromProject.ifBlank { apiAppIdFromLocal }.ifBlank { "cloud_music" }
val expectedSignatureHashFromProject = (project.findProperty("EXPECTED_SIGNATURE_HASH") as String?)?.trim().orEmpty()
val expectedSignatureHashFromEnv = System.getenv("EXPECTED_SIGNATURE_HASH")?.trim().orEmpty()
val expectedSignatureHashFromLocal = localProps.getProperty("EXPECTED_SIGNATURE_HASH")?.trim().orEmpty()
val resolvedExpectedSignatureHash = expectedSignatureHashFromProject
    .ifBlank { expectedSignatureHashFromEnv }
    .ifBlank { expectedSignatureHashFromLocal }
    .lowercase()
val enforceReleaseSignatureHash = (
    (project.findProperty("ENFORCE_RELEASE_SIGNATURE_HASH") as String?)?.trim()
        ?: System.getenv("ENFORCE_RELEASE_SIGNATURE_HASH")?.trim()
        ?: localProps.getProperty("ENFORCE_RELEASE_SIGNATURE_HASH")?.trim()
        ?: "false"
).equals("true", ignoreCase = true)
val isReleasePackageBuildRequested = gradle.startParameter.taskNames.any {
    val t = it.lowercase()
    (t.contains("assemble") || t.contains("bundle") || t.contains("install")) && t.contains("release")
}
val isApkBuildRequested = gradle.startParameter.taskNames.any {
    val t = it.lowercase()
    t.contains("assemble") || t.contains("bundle") || t.contains("install")
}
val resolvedApiAuthKey = apiAuthKey
    .ifBlank { apiAuthKeyFromEnv }
    .ifBlank { apiAuthKeyFromLocal }
if (isApkBuildRequested && resolvedApiAuthKey.isBlank()) {
    throw org.gradle.api.GradleException(
        "API_AUTH_KEY is required for APK build. " +
        "Use -PAPI_AUTH_KEY=<KEY>, environment variable API_AUTH_KEY, " +
        "or set API_AUTH_KEY in local.properties."
    )
}
if (isReleasePackageBuildRequested) {
    if (resolvedExpectedSignatureHash.isBlank()) {
        if (enforceReleaseSignatureHash) {
            throw org.gradle.api.GradleException(
                "EXPECTED_SIGNATURE_HASH is required for release package build. " +
                "Use -PEXPECTED_SIGNATURE_HASH=<SHA256_HEX>, environment variable EXPECTED_SIGNATURE_HASH, " +
                "or set EXPECTED_SIGNATURE_HASH in local.properties."
            )
        } else {
            logger.warn(
                "EXPECTED_SIGNATURE_HASH is empty for release package build. " +
                "Signature runtime check will be skipped."
            )
        }
    }
    val sha256Pattern = Regex("^[a-f0-9]{64}$")
    if (resolvedExpectedSignatureHash.isNotBlank() && !sha256Pattern.matches(resolvedExpectedSignatureHash)) {
        throw org.gradle.api.GradleException(
            "EXPECTED_SIGNATURE_HASH must be 64 lowercase hex chars (SHA-256)."
        )
    }
}

android {
    namespace = "com.yindong.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yindong.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 26
        versionName = "2.5.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // Debug 模式: 不启用安全检查的强制终止
            buildConfigField("boolean", "SECURITY_KILL_ON_RISK", "false")
            // 签名哈希留空 = 跳过签名校验 (debug 签名每台机器不同)
            buildConfigField("String", "EXPECTED_SIGNATURE", "\"\"")
            buildConfigField("String", "API_APP_ID", "\"${apiAppId.escapeForBuildConfig()}\"")
            buildConfigField("String", "API_AUTH_KEY", "\"${resolvedApiAuthKey.escapeForBuildConfig()}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release 模式: 高危风险直接终止进程
            buildConfigField("boolean", "SECURITY_KILL_ON_RISK", "true")
            buildConfigField("String", "EXPECTED_SIGNATURE", "\"${resolvedExpectedSignatureHash.escapeForBuildConfig()}\"")
            buildConfigField("String", "API_APP_ID", "\"${apiAppId.escapeForBuildConfig()}\"")
            buildConfigField("String", "API_AUTH_KEY", "\"${resolvedApiAuthKey.escapeForBuildConfig()}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 ExoPlayer - 音频播放
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    // Media3 Session - 通知栏/灵动岛/锁屏媒体控制
    implementation("androidx.media3:media3-session:1.2.1")

    // OkHttp - 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // QuickJS - JavaScript 引擎
    implementation("wang.harlon.quickjs:wrapper-android:3.2.3")

    // Coil - 图片加载 (专辑封面/歌手头像)
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.palette:palette-ktx:1.0.0")

    // DataStore - 数据持久化（主题设置等）
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 加密存储 (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
