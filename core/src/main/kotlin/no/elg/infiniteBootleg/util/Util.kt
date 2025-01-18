package no.elg.infiniteBootleg.util

import com.badlogic.gdx.Gdx
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.main.Main
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val logger = KotlinLogging.logger {}

object Util {

  private const val DEFAULT_HASH: String = "UNKNOWN"
  private const val DEFAULT_COMMIT_COUNT: Int = 0
  private const val FALLBACK_VERSION: String = DEFAULT_HASH
  private const val CIRCLE_DEG: Int = 360
  const val RELATIVE_TIME: String = "relative"
  private val ZULU_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm'Z'")

  val version: String by lazy {
    val calcHash = "#${countCommits()}-${getLastGitCommitID()}@${getLastCommitDate("iso8601-strict")}"
    val savedHash = try {
      Gdx.files.internal(Main.VERSION_FILE).readString()
    } catch (e: Exception) {
      FALLBACK_VERSION
    }
    if (savedHash == FALLBACK_VERSION && calcHash == FALLBACK_VERSION) {
      logger.error { "Failed to get the current version" }
      return@lazy FALLBACK_VERSION
    }
    if (savedHash != calcHash && FALLBACK_VERSION != calcHash) {
      val versionFile = Gdx.files.absolute(Main.VERSION_FILE)
      try {
        versionFile.writeString(calcHash, false)
      } catch (e: Exception) {
        logger.error(e) { "Failed to write new version to file" }
      }
    }
    if (calcHash == FALLBACK_VERSION) {
      savedHash
    } else {
      calcHash
    }
  }

  private fun getLastGitCommitID(): String {
    val result = executeCommand("git", "log", "-1", "--format=%h") ?: return DEFAULT_HASH
    return result.uppercase(Locale.getDefault())
  }

  /**
   * @return The number of commits in this repository
   */
  private fun countCommits(): Int {
    val result = executeCommand("git", "rev-list", "HEAD", "--count")
      ?: return DEFAULT_COMMIT_COUNT
    return try {
      result.toInt()
    } catch (e: NumberFormatException) {
      DEFAULT_COMMIT_COUNT
    }
  }

  /**
   * @return The number of commits in this repository
   */
  fun getLastCommitDate(dateFormat: String): String? {
    val date = executeCommand("git", "log", "-1", "--format=%cd", "--date=$dateFormat")
    if (date == null || RELATIVE_TIME == dateFormat) {
      return date
    }
    try {
      val parse = ZonedDateTime.parse(date).withZoneSameInstant(ZoneOffset.UTC)
      return parse.format(ZULU_FORMATTER)
    } catch (e: DateTimeParseException) {
      return null
    }
  }

  private fun executeCommand(vararg command: String): String? {
    try {
      val processBuilder = ProcessBuilder()
      processBuilder.command(*command)
      val p = processBuilder.start()
      p.waitFor()

      val inputStream = p.inputStream
      InputStreamReader(inputStream, StandardCharsets.UTF_8).use { `in` ->
        BufferedReader(`in`).use { bufferedReader ->
          return bufferedReader.readLine()
        }
      }
    } catch (e: Exception) {
      return null
    }
  }

  /**
   * @param orgDir The original direction
   * @return The direction normalized within 0 (inclusive) to 360 (exclusive)
   */
  fun normalizedDir(orgDir: Float): Float {
    if (orgDir >= CIRCLE_DEG) {
      return orgDir % CIRCLE_DEG
    } else if (orgDir < 0) {
      val mult = (-orgDir / CIRCLE_DEG).toInt() + 1
      return mult * CIRCLE_DEG + orgDir
    }
    return orgDir // is within [0,360)
  }
}
