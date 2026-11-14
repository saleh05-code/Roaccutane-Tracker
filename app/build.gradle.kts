import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.roaccutanetracking.ndsqpx"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.vertexai)
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.zxing.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

abstract class CopyApkTask : DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    abstract val sourceApk: Property<File>

    @get:org.gradle.api.tasks.OutputFile
    abstract val targetApk1: Property<File>

    @get:org.gradle.api.tasks.OutputFile
    abstract val targetApk2: Property<File>

    @get:org.gradle.api.tasks.OutputFile
    abstract val targetZip1: Property<File>

    @get:org.gradle.api.tasks.OutputFile
    abstract val targetZip2: Property<File>

    @get:org.gradle.api.tasks.OutputFile
    abstract val targetPlatformApk: Property<File>

    private fun writeRealZip(source: File, targetZip: File) {
        val tempDir = targetZip.parentFile
        if (tempDir != null && !tempDir.exists()) {
            tempDir.mkdirs()
        }
        FileOutputStream(targetZip).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // Entry 1: RoaccutaneTracker.apk
                val entryApk = ZipEntry("RoaccutaneTracker.apk")
                zos.putNextEntry(entryApk)
                source.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()

                // Entry 2: RoaccutaneTracker_rename_me_to_apk.bin
                val entryBin = ZipEntry("RoaccutaneTracker_rename_me_to_apk.bin")
                zos.putNextEntry(entryBin)
                source.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    @org.gradle.api.tasks.TaskAction
    fun copyApk() {
        val src = sourceApk.get()
        if (src.exists()) {
            val t1 = targetApk1.get()
            val t2 = targetApk2.get()
            val z1 = targetZip1.get()
            val z2 = targetZip2.get()
            val p1 = targetPlatformApk.get()

            listOf(t1, t2, z1, z2, p1).forEach { it.parentFile?.mkdirs() }

            // Copy raw APKs
            src.copyTo(t1, overwrite = true)
            src.copyTo(t2, overwrite = true)
            src.copyTo(p1, overwrite = true)

            // Write real valid ZIPs containing the files
            writeRealZip(src, z1)
            writeRealZip(src, z2)

            println("Successfully copied APK of size ${src.length()} bytes.")
            println("- Copied raw APK to: ${t1.absolutePath}")
            println("- Copied raw APK to: ${t2.absolutePath}")
            println("- Created REAL ZIP Archive: ${z1.absolutePath} (Size: ${z1.length()} bytes)")
            println("- Created REAL ZIP Archive: ${z2.absolutePath} (Size: ${z2.length()} bytes)")
            println("- Copied to platform build-outputs: ${p1.absolutePath}")
        } else {
            println("Warning: Source APK not found at ${src.absolutePath}")
        }
    }
}

tasks.register("listRootFiles") {
    doLast {
        println("--- Files in Project Root (${rootDir.absolutePath}) ---")
        rootDir.listFiles()?.forEach { file ->
            val sizeStr = if (file.isDirectory) "<DIR>" else "${file.length()} bytes (${String.format("%.2f", file.length() / (1024.0 * 1024.0))} MB)"
            println("- ${file.name}: $sizeStr")
        }
        val buildOutputDir = rootDir.resolve("Build output")
        if (buildOutputDir.exists() && buildOutputDir.isDirectory) {
            println("--- Files in Build output/ ---")
            buildOutputDir.listFiles()?.forEach { file ->
                val sizeStr = if (file.isDirectory) "<DIR>" else "${file.length()} bytes (${String.format("%.2f", file.length() / (1024.0 * 1024.0))} MB)"
                println("  - ${file.name}: $sizeStr")
            }
        }
    }
}

val copyApkToOutputs by tasks.registering(CopyApkTask::class) {
    sourceApk.set(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").map { it.asFile })
    targetApk1.set(rootDir.resolve("RoaccutaneTracker.apk"))
    targetApk2.set(rootDir.resolve("Build output/RoaccutaneTracker.apk"))
    targetZip1.set(rootDir.resolve("RoaccutaneTracker_Installer.zip"))
    targetZip2.set(rootDir.resolve("Build output/RoaccutaneTracker_Installer.zip"))
    targetPlatformApk.set(file("/.build-outputs/app-debug.apk"))
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy(copyApkToOutputs)
}

