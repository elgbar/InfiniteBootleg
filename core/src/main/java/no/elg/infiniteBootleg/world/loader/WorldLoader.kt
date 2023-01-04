package no.elg.infiniteBootleg.world.loader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.generator.ChunkGenerator
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator
import no.elg.infiniteBootleg.world.generator.FlatChunkGenerator
import no.elg.infiniteBootleg.world.generator.PerlinChunkGenerator

/**
 * @author Elg
 */
object WorldLoader {
  private const val LOCK_FILE_NAME = ".locked"
  private val WORLD_LOCK_LOCK = Any()
  const val WORLD_INFO_PATH = "world.dat"
  private const val PLAYERS_PATH = "players"

  fun getServerPlayerFile(world: World, playerId: String): FileHandle? {
    return world.worldFolder?.child(PLAYERS_PATH)?.child(playerId)
  }

//  fun spawnServerPlayer(world: ServerWorld, playerId: String): Entity {
//    val fileHandle = getServerPlayerFile(world, playerId)
//    if (fileHandle != null && fileHandle.exists()) {
//      try {
//        val proto = ProtoWorld.Entity.parseFrom(fileHandle.readBytes())
//        val player = Player(world, proto)
//        player.disableGravity()
//        world.addEntity(player)
//        if (!player.isDisposed) {
//          Main.logger().debug("SERVER", "Loading persisted player profile for $playerId")
//          return player
//        } else {
//          Main.logger().error("SERVER", "Invalid player parsed")
//          // fall through
//        }
//      } catch (e: Exception) {
//        Main.logger().error("SERVER", "Invalid entity protocol", e)
//        // fall through
//      }
//    }
//    Main.logger().debug("SERVER", "Creating fresh player profile for $playerId")
//    Invalid / non - existing
//    var data: player
//    val player = world.createNewPlayer(playerId)
//    saveServerPlayer(player)
//    return player
//  }

//  fun saveServerPlayer(player: Entity) {
//    val fileHandle = getServerPlayerFile(player.world, player.uuid)
//    if (fileHandle != null) {
//      try {
//        fileHandle.writeBytes(player.save().build().toByteArray(), false)
//      } catch (e: Exception) {
//        e.printStackTrace()
//      }
//    }
//  }

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
        Main.logger()
          .warn(
            "World lock file for $uuid did not contain a valid pid, read: $lockInfo"
          )
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
        Main.logger()
          .warn(
            "World lock file for " +
              uuid +
              " still existed for a non-existing process, an old lock file found"
          )
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
      worldLockFile.writeString(ProcessHandle.current().pid().toString() + "", false)
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
      ProtoWorld.World.Generator.PERLIN, ProtoWorld.World.Generator.UNRECOGNIZED -> PerlinChunkGenerator(protoWorld.seed)
      ProtoWorld.World.Generator.FLAT -> FlatChunkGenerator()
      ProtoWorld.World.Generator.EMPTY -> EmptyChunkGenerator()
    }
  }
}
