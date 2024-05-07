package no.elg.infiniteBootleg.util

import com.badlogic.gdx.files.FileHandle
import no.elg.infiniteBootleg.main.Main
import java.nio.file.Files

fun deleteOrLogFile(file: FileHandle): Boolean =
  try {
    Files.delete(file.file().toPath())
    true
  } catch (e: Exception) {
    Main.logger().error("Failed to delete file ${file.path()}", e)
    false
  }
