// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Base64
import java.io.File

val base64File = file("debug.keystore.base64")
val keystoreFile = file("debug.keystore")
if (base64File.exists() && !keystoreFile.exists()) {
    try {
        val base64Content = base64File.readText().trim()
        val decodedBytes = Base64.getDecoder().decode(base64Content)
        keystoreFile.writeBytes(decodedBytes)
        println("Successfully decoded debug.keystore from base64!")
    } catch (e: Exception) {
        println("Failed to decode debug.keystore: ${e.message}")
    }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

