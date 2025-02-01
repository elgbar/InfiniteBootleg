package no.elg.infiniteBootleg.core.world.loader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.deleteOrLogFile
import no.elg.infiniteBootleg.core.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.core.world.generator.chunk.EmptyChunkGenerator
import no.elg.infiniteBootleg.core.world.generator.chunk.FlatChunkGenerator
import no.elg.infiniteBootleg.core.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.protobuf.ProtoWorld

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
object WorldLoader {
  private const val LOCK_FILE_NAME = ".locked"
  private val WORLD_LOCK_LOCK = Any()
  const val WORLD_INFO_PATH = "world.dat"

  fun getWorldFolder(uuid: String): FileHandle {
    return Gdx.files.external(Main.Companion.WORLD_FOLDER + uuid)
  }

  private fun getWorldLockFile(uuid: String): FileHandle {
    return getWorldFolder(uuid).child(LOCK_FILE_NAME)
  }

  fun canWriteToWorld(uuid: String): Boolean {
    synchronized(WORLD_LOCK_LOCK) {
      if (Settings.ignoreWorldLock) {
        return true
      }
      val worldLockFile = getWorldLockFile(uuid)
      if (worldLockFile.isDirectory) {
        // Invalid format, allow writing
        logger.warn { "World lock file for $uuid was a directory" }
        return true
      }
      if (!worldLockFile.exists()) {
        // No lock file, we can ofc write!
        return true
      }
      val lockInfo = worldLockFile.readString()

      val lockPID = try {
        lockInfo.toLong()
      } catch (e: NumberFormatException) {
        logger.warn { "World lock file for $uuid did not contain a valid pid, read: $lockInfo" }
        // Invalid pid, allow writing
        return deleteOrLogFile(worldLockFile)
      }
      if (lockPID == ProcessHandle.current().pid()) {
        // We own the lock
        return true
      }
      val optionalProcessHandle = ProcessHandle.of(lockPID)
      if (!optionalProcessHandle.map(ProcessHandle::isAlive).orElse(false)) {
        // If there is no process with the read pid, it was probably left from an old instance
        logger.warn { "World lock file for $uuid still existed for a non-existing process (PID $lockPID), an old lock file found" }
        return deleteOrLogFile(worldLockFile)
      }
      return false
    }
  }

  fun writeLockFile(uuid: String): Boolean {
    synchronized(WORLD_LOCK_LOCK) {
      if (!canWriteToWorld(uuid)) {
        return false
      }
      val worldLockFile = getWorldLockFile(uuid)
      worldLockFile.writeString(ProcessHandle.current().pid().toString(), false)
      return true
    }
  }

  fun deleteLockFile(uuid: String): Boolean {
    synchronized(WORLD_LOCK_LOCK) {
      if (!canWriteToWorld(uuid)) {
        return false
      }
      return deleteOrLogFile(getWorldLockFile(uuid))
    }
  }

  fun generatorFromProto(protoWorld: ProtoWorld.World): ChunkGenerator {
    return when (protoWorld.generator) {
      ProtoWorld.World.Generator.PERLIN, ProtoWorld.World.Generator.UNRECOGNIZED, null -> PerlinChunkGenerator(
        protoWorld.seed
      )
      ProtoWorld.World.Generator.FLAT -> FlatChunkGenerator()
      ProtoWorld.World.Generator.EMPTY -> EmptyChunkGenerator()
    }
  }
}
