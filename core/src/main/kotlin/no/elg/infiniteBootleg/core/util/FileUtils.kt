package no.elg.infiniteBootleg.core.util

import com.badlogic.gdx.files.FileHandle
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files

private val logger = KotlinLogging.logger {}

fun deleteOrLogFile(file: FileHandle): Boolean =
  try {
    val path = file.file().toPath()
    if (Files.exists(path)) {
      Files.delete(path)
      true
    } else {
      logger.debug { "file ${file.path()} does not exist, nothing to delete" }
      true
    }
  } catch (e: Exception) {
    logger.error(e) { "failed to delete file ${file.path()}, the lock is likely still valid then" }
    false
  }
