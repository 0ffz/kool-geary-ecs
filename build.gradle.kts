import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform") version "2.1.20"
}

repositories {
    mavenCentral()
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass.set("LauncherKt")
                if (OperatingSystem.current().isMacOsX) {
                    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
                }
            }
        }
    }
    jvmToolchain(21)

    js {
        binaries.executable()
        browser {
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory.set(File("${rootDir}/dist/js"))
            }
            commonWebpackConfig {
                //mode = KotlinWebpackConfig.Mode.PRODUCTION
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
            }
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            target.set("es2015")
        }
    }

    sourceSets {
        val koolVersion = "0.17.0"
        val lwjglVersion = "3.3.6"
        val physxJniVersion = "2.6.0"

        // JVM target platforms, you can remove entries from the list in case you want to target
        // only a specific platform
        val targetPlatforms = listOf("natives-windows", "natives-linux", "natives-macos", "natives-macos-arm64")

        commonMain {
            dependencies {
                // add additional kotlin multi-platform dependencies here...

                implementation("de.fabmax.kool:kool-core:$koolVersion")
                implementation("de.fabmax.kool:kool-physics:$koolVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        }

        jvmMain {
            dependencies {
                // add additional jvm-specific dependencies here...

                // add required runtime libraries for lwjgl and physx-jni
                for (platform in targetPlatforms) {
                    // lwjgl runtime libs
                    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$platform")
                    listOf("glfw", "opengl", "jemalloc", "nfd", "stb", "vma", "shaderc").forEach { lib ->
                        runtimeOnly("org.lwjgl:lwjgl-$lib:$lwjglVersion:$platform")
                    }

                    // physx-jni runtime libs
                    runtimeOnly("de.fabmax:physx-jni:$physxJniVersion:$platform")
                }
                implementation("com.mineinabyss:geary-core")
            }
        }

        jsMain {
            dependencies {
                // add additional js-specific dependencies here...
            }
        }
    }
}

val clean by tasks.getting(Task::class) {
    doLast {
        delete("${rootDir}/dist")
    }
}
