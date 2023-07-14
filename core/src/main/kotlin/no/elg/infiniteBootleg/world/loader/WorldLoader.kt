package no.elg.infiniteBootleg.world.loader

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.SharedInformation
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.createMPServerPlayerEntity
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.generator.chunk.EmptyChunkGenerator
import no.elg.infiniteBootleg.world.generator.chunk.FlatChunkGenerator
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.world.ServerWorld
import no.elg.infiniteBootleg.world.world.World
import java.util.concurrent.CompletableFuture

/**
 * @author Elg
 */
object WorldLoader {
  private const val LOCK_FILE_NAME = ".locked"
  private val WORLD_LOCK_LOCK = Any()
  const val WORLD_INFO_PATH = "world.dat"
  private const val PLAYERS_PATH = "players"

  private fun getServerPlayerFile(world: World, playerId: String): FileHandle? {
    return world.worldFolder?.child(PLAYERS_PATH)?.child(playerId)
  }

  fun spawnServerPlayer(world: ServerWorld, uuid: String, username: String, sharedInformation: SharedInformation): CompletableFuture<Entity> {
    val fileHandle = getServerPlayerFile(world, uuid)

    if (fileHandle != null && fileHandle.exists()) {
      Main.logger().debug("SERVER", "Trying to load persisted player profile for $uuid")
      try {
        val proto = ProtoWorld.Entity.parseFrom(fileHandle.readBytes())
        val future = world.engine.createMPServerPlayerEntity(world, proto, sharedInformation)
        future.thenAccept {
          Main.logger().debug("SERVER", "Loaded persisted player profile for $uuid")
        }
        return future
      } catch (e: Exception) {
        Main.logger().warn("SERVER", "Failed to load player profile", e)
        // fall through
      }
    }
    Main.logger().debug("SERVER", "Creating fresh player profile for $uuid")
    val spawn = world.spawn
    val future = world.engine.createMPServerPlayerEntity(world, spawn.x.toFloat(), spawn.y.toFloat(), 0f, 0f, username, uuid, sharedInformation, null)
    future.thenAccept {
      saveServerPlayer(it)
      Main.logger().debug("SERVER", "Created persisted player profile for $uuid")
    }
    return future
  }

  fun saveServerPlayer(player: Entity) {
    val fileHandle = getServerPlayerFile(player.world, player.id)
    if (fileHandle != null) {
      try {
        fileHandle.writeBytes(player.save().toByteArray(), false)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  @JvmStatic
  fun getWorldFolder(uuid: String): FileHandle {
    return Gdx.files.external(Main.WORLD_FOLDER + uuid)
  }

  fun getWorldLockFile(uuid: String): FileHandle {
    return getWorldFolder(uuid).child(LOCK_FILE_NAME)
  }

  @JvmStatic
  fun canWriteToWorld(uuid: String): Boolean {
    synchronized(WORLD_LOCK_LOCK) {
      if (Settings.ignoreWorldLock) {
        return true
      }
      val worldLockFile = getWorldLockFile(uuid)
      if (worldLockFile.isDirectory) {
        // Invalid format, allow writing
        Main.logger().warn("World lock file for $uuid was a directory")
        worldLockFile.deleteDirectory()
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
        Main.logger().warn("World lock file for $uuid did not contain a valid pid, read: $lockInfo")
        worldLockFile.delete()
        // Invalid pid, allow writing
        return true
      }
      if (lockPID == ProcessHandle.current().pid()) {
        // We own the lock
        return true
      }
      if (ProcessHandle.of(lockPID).isEmpty) {
        // If there is no process with the read pid, it was probably left from an old instance
        Main.logger().warn("World lock file for $uuid still existed for a non-existing process, an old lock file found")
        worldLockFile.delete()
        return true
      }
      return false
    }
  }

  @JvmStatic
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

  @JvmStatic
  fun deleteLockFile(uuid: String): Boolean {
    synchronized(WORLD_LOCK_LOCK) {
      if (!canWriteToWorld(uuid)) {
        return false
      }
      getWorldLockFile(uuid).delete()
      return true
    }
  }

  @JvmStatic
  fun generatorFromProto(protoWorld: ProtoWorld.World): ChunkGenerator {
    return when (protoWorld.generator) {
      ProtoWorld.World.Generator.PERLIN, ProtoWorld.World.Generator.UNRECOGNIZED, null -> PerlinChunkGenerator(protoWorld.seed)
      ProtoWorld.World.Generator.FLAT -> FlatChunkGenerator()
      ProtoWorld.World.Generator.EMPTY -> EmptyChunkGenerator()
    }
  }
}
