import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.google.protobuf.gradle.GenerateProtoTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  java
  alias(libs.plugins.kotlin.jvm).apply(false)
  alias(libs.plugins.kotlin.allopen).apply(false)
  alias(libs.plugins.spotless)
  alias(libs.plugins.versions)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.stacktracedecoroutinator).apply(false)
}

tasks.named<Wrapper>("wrapper") {
  distributionType = Wrapper.DistributionType.BIN
}

fun getGitVersion(): String {
  val file = File(project.projectDir, "./core/src/main/resources/version")
  if (!file.exists() || !file.canRead()) {
    System.err.println("Failed to read version from file ${file.absolutePath}")
    return "UNKNOWN"
  }
  return file.readLines()[0]
}

allprojects {
  plugins.apply("java")
  plugins.apply("kotlin")
  plugins.apply("com.diffplug.spotless")
  plugins.apply("com.github.ben-manes.versions")

  version = getGitVersion()

  repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    maven { url = uri("https://raw.githubusercontent.com/elgbar/maven2/repo/") }
  }

  spotless {
    format("misc") {
      target("**/*.md", ".gitignore", "**/*.yml", "**/*.yaml", "**/*.proto")
      trimTrailingWhitespace()
      indentWithSpaces(2)
      endWithNewline()
    }
    java {
      targetExclude(fileTree(projectDir) { include("**/generated/**") })
      removeUnusedImports()
      googleJavaFormat().reflowLongStrings()
    }
    kotlin {
      target("**/*.gradle.kts", "**/*.kt")
      targetExclude(fileTree(projectDir) { include("**/generated/**") })
      ktlint().setEditorConfigPath("$rootDir/.editorconfig").editorConfigOverride(
        mapOf(
          "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "5",
          "ktlint_function_signature_body_expression_wrapping" to "multiline",
        )
      )
    }
  }

  tasks.withType<JavaExec> {
    isIgnoreExitValue = true
  }
}

subprojects {
  plugins.apply("idea")
  plugins.apply("kotlin")
  plugins.apply("org.jetbrains.kotlin.jvm")
  plugins.apply("java")
  plugins.apply("dev.reformator.stacktracedecoroutinator")

  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(23))
    }
  }

  ext["assetsDir"] = File("../core/src/main/resources")

  tasks.named("classes") {
    dependsOn("spotlessApply")
  }

  tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
      languageVersion.set(KotlinVersion.KOTLIN_2_2)
      progressiveMode.set(true)
      extraWarnings.set(false)
      optIn.add("kotlin.contracts.ExperimentalContracts")
      freeCompilerArgs.add("-Xdebug")
      freeCompilerArgs.add("-Xwhen-guards") // https://github.com/Kotlin/KEEP/blob/guards/proposals/guards.md
      freeCompilerArgs.add("-Xnon-local-break-continue") // https://github.com/Kotlin/KEEP/blob/guards/proposals/break-continue-in-inline-lambdas.md (beta in 2.1)
      freeCompilerArgs.add("-Xsuppress-warning=UNUSED_VARIABLE") // https://kotlinlang.org/docs/whatsnew21.html#extra-compiler-checks
//      allWarningsAsErrors = true
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform {
      includeEngines("junit-jupiter", "junit-vintage")
    }
    jvmArgs = listOf(
      "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED",
      "--add-opens", "java.xml/jdk.xml.internal=ALL-UNNAMED",
      "--add-opens", "java.base/java.lang=ALL-UNNAMED",
      "--add-opens", "java.base/java.io=ALL-UNNAMED",
      "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
      "--add-opens", "java.base/java.util.stream=ALL-UNNAMED",
      "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens", "java.base/java.security=ALL-UNNAMED",
      "--add-opens", "java.base/sun.nio.fs=ALL-UNNAMED",
      "--add-opens", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
      "--add-opens", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
      "--add-opens", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
      "--add-opens", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
      "--add-opens", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
  }

  dependencies {
    implementation(rootProject.libs.gdx.core)
    implementation(rootProject.libs.gdx.box2d)
    implementation(rootProject.libs.gdx.freetype)
    implementation(rootProject.libs.uuid)
    implementation(rootProject.libs.caffeine)
    implementation(rootProject.libs.protobuf.bom)
    implementation(rootProject.libs.protobuf.kotlin)
    implementation(rootProject.libs.visUi)
    implementation(rootProject.libs.inGameConsole)
    implementation(rootProject.libs.ktx.actors)
    implementation(rootProject.libs.ktx.app)
    implementation(rootProject.libs.ktx.assets)
    implementation(rootProject.libs.ktx.async)
    implementation(rootProject.libs.ktx.collections)
    implementation(rootProject.libs.ktx.graphics)
    implementation(rootProject.libs.ktx.style)
    implementation(rootProject.libs.ktx.vis)
    implementation(rootProject.libs.ktx.vis.style)
    implementation(rootProject.libs.ktx.ashley)
    implementation(rootProject.libs.ktx.box2d)
    implementation(rootProject.libs.ktx.math)
    implementation(rootProject.libs.ktx.preferences)
    implementation(rootProject.libs.ktx.log)
    implementation(platform(rootProject.libs.netty.bom))
    implementation(rootProject.libs.netty.all)
    implementation(rootProject.libs.apacheCommons.collections)
    implementation(rootProject.libs.apacheCommons.lang)
    implementation(rootProject.libs.annotations)
    implementation(rootProject.libs.kotlin.reflect)
    implementation(rootProject.libs.kotlin.coroutines)
    implementation(rootProject.libs.fastutil)
    implementation(rootProject.libs.oshaiLogging)
    implementation(platform(rootProject.libs.log4j.bom))
    implementation(rootProject.libs.log4j.api)
    implementation(rootProject.libs.log4j.core)
    implementation(rootProject.libs.log4j.slf4j)
    implementation(rootProject.libs.jansi)

    testImplementation(rootProject.libs.gdx.backend.headless)
    testImplementation(rootProject.libs.mockk)
    testImplementation(rootProject.libs.junit.jupiter.api)
    testImplementation(rootProject.libs.junit.jupiter.engine)
    testImplementation(rootProject.libs.junit.vintage.engine)
    testImplementation(rootProject.libs.junit.jupiter.params)

    testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    testRuntimeOnly(rootProject.libs.gdx.platform)
  }

  fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.contains(it, ignoreCase = true) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !version.matches(regex)
  }

  tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    gradleReleaseChannel = "current"
    checkConstraints = true
    rejectVersionIf {
      isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
  }
}

project(":client") {
  dependencies {
    implementation(project(":core"))
    implementation(rootProject.libs.gdx.backend.lwjgl3)
    implementation(variantOf(rootProject.libs.gdx.box2d.platform) { classifier("natives-desktop") })
    implementation(variantOf(rootProject.libs.gdx.freetype.platform) { classifier("natives-desktop") })
    implementation(variantOf(rootProject.libs.gdx.platform) { classifier("natives-desktop") })
  }
}

project(":server") {
  dependencies {
    implementation(project(":core"))
    implementation(rootProject.libs.gdx.backend.headless)
    implementation(variantOf(rootProject.libs.gdx.box2d.platform) { classifier("natives-desktop") })
    implementation(variantOf(rootProject.libs.gdx.platform) { classifier("natives-desktop") })
  }
}

project(":core") {
  plugins.apply("com.google.protobuf")

  protobuf {
    protoc {
      artifact = rootProject.libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
      project.tasks.withType<GenerateProtoTask> {
        builtins {
          add(GenerateProtoTask.PluginOptions("java"))
          add(GenerateProtoTask.PluginOptions("kotlin"))
        }
      }
    }
  }
}
