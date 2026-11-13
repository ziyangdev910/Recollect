plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.junit5) apply false
}

/**
 * 纯 Kotlin 核心模块不得依赖 Android。
 * 第二道防线是这些模块的 compileClasspath 中没有 android.jar；本任务提供可读的 file:line 证据。
 */
val coreModules = listOf(
    ":core:common",
    ":core:model",
    ":core:scheduler",
    ":core:pagination",
)

tasks.register("verifyCorePurity") {
    group = "verification"
    description = "Fail if a pure Kotlin core module imports android.* or androidx.*"
    doLast {
        val offenders = mutableListOf<String>()
        coreModules.forEach { module ->
            val src = rootDir.resolve(module.removePrefix(":").replace(':', '/')).resolve("src")
            if (src.exists()) {
                src.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { file ->
                        file.readLines().forEachIndexed { index, line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("import android.") ||
                                trimmed.startsWith("import androidx.")
                            ) {
                                offenders += "${file.relativeTo(rootDir)}:${index + 1}: $trimmed"
                            }
                        }
                    }
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Core purity violation(s):\n" + offenders.joinToString(separator = "\n"),
            )
        }
    }
}
