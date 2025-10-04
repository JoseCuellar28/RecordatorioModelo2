plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.example.recordatoriomodelo2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.recordatoriomodelo2"
        minSdk = 24
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")
    
    // JavaMail para envío de correos
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    
    // Manejo de imágenes
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    
    // HTTP client para ImgBB API
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Seguridad y encriptación
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
}

// Funciones para manejo automático de versiones
fun getVersionCode(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (process.exitValue() == 0) {
            output.toInt()
        } else {
            1 // Fallback si no hay git
        }
    } catch (e: Exception) {
        println("Error obteniendo version code: ${e.message}")
        1 // Fallback en caso de error
    }
}

fun getVersionName(): String {
    return try {
        // Intentar obtener el último tag de git
        val tagProcess = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .directory(rootDir)
            .start()
        val tagOutput = tagProcess.inputStream.bufferedReader().readText().trim()
        tagProcess.waitFor()
        
        if (tagProcess.exitValue() == 0 && tagOutput.isNotEmpty()) {
            // Si hay un tag, usarlo como base
            val commitProcess = ProcessBuilder("git", "rev-list", "--count", "$tagOutput..HEAD")
                .directory(rootDir)
                .start()
            val commitCount = commitProcess.inputStream.bufferedReader().readText().trim()
            commitProcess.waitFor()
            
            if (commitProcess.exitValue() == 0 && commitCount.toInt() > 0) {
                "$tagOutput-dev.$commitCount"
            } else {
                tagOutput
            }
        } else {
            // Formato basado en el contexto del proyecto:
            // Versión 2 (rediseño), Historia 2 (HU-2), commits en la rama
            val branchProcess = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .directory(rootDir)
                .start()
            val branchName = branchProcess.inputStream.bufferedReader().readText().trim()
            branchProcess.waitFor()
            
            val commitProcess = ProcessBuilder("git", "rev-list", "--count", "HEAD")
                .directory(rootDir)
                .start()
            val totalCommits = commitProcess.inputStream.bufferedReader().readText().trim()
            commitProcess.waitFor()
            
            if (branchProcess.exitValue() == 0 && commitProcess.exitValue() == 0) {
                when {
                    branchName.contains("feature/hu-2") -> {
                        // Estamos en HU-2, contar commits específicos de esta rama
                        val featureCommitsProcess = ProcessBuilder("git", "rev-list", "--count", "HEAD", "^origin/main")
                            .directory(rootDir)
                            .start()
                        val featureCommits = featureCommitsProcess.inputStream.bufferedReader().readText().trim()
                        featureCommitsProcess.waitFor()
                        
                        if (featureCommitsProcess.exitValue() == 0 && featureCommits.isNotEmpty()) {
                            "2.2.${featureCommits}"
                        } else {
                            "2.2.2" // Fallback basado en tu contexto actual
                        }
                    }
                    branchName.contains("feature/hu-3") -> "2.3.1"
                    branchName.contains("feature/hu-4") -> "2.4.1"
                    branchName.contains("main") || branchName.contains("master") -> "2.0.0"
                    else -> "2.2.2" // Fallback para HU-2 actual
                }
            } else {
                "2.2.2" // Fallback basado en tu contexto actual
            }
        }
    } catch (e: Exception) {
        println("Error obteniendo version name: ${e.message}")
        "2.2.2" // Fallback basado en tu contexto actual
    }
}