package no.elg.infiniteBootleg.world.loader

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.SharedInformation
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.SharedInformationComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.world.ecs.creation.createNewProtoPlayer
import no.elg.infiniteBootleg.world.ecs.load
import no.elg.infiniteBootleg.world.ecs.save
import no.elg.infiniteBootleg.world.generator.chunk.ChunkGenerator
import no.elg.infiniteBootleg.world.generator.chunk.EmptyChunkGenerator
import no.elg.infiniteBootleg.world.generator.chunk.FlatChunkGenerator
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.world.ServerWorld
import no.elg.infiniteBootleg.world.world.World
import java.nio.file.Files
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

    val serverPlayerConfig: Entity.() -> Unit = {
      safeWith { SharedInformationComponent(sharedInformation) }
      isTransientEntity = true
    }

    fun createNewServerPlayer(): CompletableFuture<Entity> {
      val protoEntity = world.createNewProtoPlayer(controlled = true) {
        this.uuid = uuid
        this.name = username
      }
      return world.load(protoEntity, configure = serverPlayerConfig).thenApply {
        saveServerPlayer(it)
        Main.logger().debug("SERVER", "Created persisted player profile for $uuid")
        it
      }
    }

    if (fileHandle != null && fileHandle.exists()) {
      Main.logger().debug("SERVER", "Trying to load persisted player profile for $uuid")
      try {
        val proto = ProtoWorld.Entity.parseFrom(fileHandle.readBytes())
        return world.load(proto, configure = serverPlayerConfig).handle { entity, ex ->
          if (ex != null) {
            Main.logger().warn("SERVER", "Failed to load player profile, creating new player", ex)
            return@handle createNewServerPlayer().join()
          } else {
            Main.logger().debug("SERVER", "Loaded persisted player profile for $uuid")
          }
          return@handle entity
        }
      } catch (e: Exception) {
        Main.logger().warn("SERVER", "Failed to load player profile", e)
        // fall through
      }
    }
    Main.logger().debug("SERVER", "Creating fresh player profile for $uuid")
    return createNewServerPlayer()
  }

  fun saveServerPlayer(player: Entity) {
    val protoPlayer = player.save(toAuthoritative = true, ignoreTransient = true) ?: run {
      Main.logger().warn("Failed to create protobuf entity of player ${player.id}")
      return
    }
    val fileHandle = getServerPlayerFile(player.world, player.id) ?: run {
      Main.logger().warn("Failed to get server player file for ${player.id}")
      return
    }
    try {
      fileHandle.writeBytes(protoPlayer.toByteArray(), false)
    } catch (e: Exception) {
      Main.logger().error("Failed to write player save to disk for ${player.id}", e)
    }
  }

  fun getWorldFolder(uuid: String): FileHandle {
    return Gdx.files.external(Main.WORLD_FOLDER + uuid)
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
        Main.logger().warn("World lock file for $uuid was a directory")
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
        Main.logger().warn("World lock file for $uuid still existed for a non-existing process (PID $lockPID), an old lock file found")
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

  private fun deleteOrLogFile(file: FileHandle): Boolean =
    try {
      Files.delete(file.file().toPath())
      true
    } catch (e: Exception) {
      Main.logger().error("Failed to delete world lock file ${file.path()}", e)
      false
    }

  fun generatorFromProto(protoWorld: ProtoWorld.World): ChunkGenerator {
    return when (protoWorld.generator) {
      ProtoWorld.World.Generator.PERLIN, ProtoWorld.World.Generator.UNRECOGNIZED, null -> PerlinChunkGenerator(protoWorld.seed)
      ProtoWorld.World.Generator.FLAT -> FlatChunkGenerator()
      ProtoWorld.World.Generator.EMPTY -> EmptyChunkGenerator()
    }
  }
}
