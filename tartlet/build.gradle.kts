import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.yumemi"
version = libs.versions.tartlet

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "io.yumemi.tartlet"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    if (System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername") != null) {
        signAllPublications()
    }

    coordinates(group.toString(), "tartlet", version.toString())

    pom {
        name = "Tartlet"
        description = "A Kotlin Multiplatform library."
        inceptionYear = "2025"
        url = "https://github.com/yumemi-inc/Tartlet/"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
                distribution = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "yumemi-inc"
                name = "YUMEMI Inc."
                url = "https://github.com/yumemi-inc/"
            }
        }
        scm {
            url = "https://github.com/yumemi-inc/Tartlet/"
            connection = "scm:git:git://github.com/yumemi-inc/Tartlet.git"
            developerConnection = "scm:git:git://github.com/yumemi-inc/Tartlet.git"
        }
    }
}
