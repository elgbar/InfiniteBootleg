package no.elg.infiniteBootleg.util

import com.badlogic.gdx.files.FileHandle
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files

private val logger = KotlinLogging.logger {}
fun deleteOrLogFile(file: FileHandle): Boolean =
  try {
    Files.delete(file.file().toPath())
    true
  } catch (e: Exception) {
    logger.error(e) { "failed to delete file ${file.path()}" }
    false
  }
