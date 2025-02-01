package no.elg.infiniteBootleg.server.world.loader

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.files.FileHandle
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.net.SharedInformation
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.toProtoEntityRef
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.SharedInformationComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.tags.TransientEntityTag.Companion.isTransientEntity
import no.elg.infiniteBootleg.core.world.ecs.creation.createNewProtoPlayer
import no.elg.infiniteBootleg.core.world.ecs.load
import no.elg.infiniteBootleg.core.world.ecs.save
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.server.world.ServerWorld
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

object ServerWorldLoader {

  private const val PLAYERS_PATH = "players"

  private fun getServerPlayerFile(world: World, playerId: String): FileHandle? {
    return world.worldFolder?.child(PLAYERS_PATH)?.child(playerId)
  }

  fun spawnServerPlayer(world: ServerWorld, entityId: String, username: String, sharedInformation: SharedInformation): CompletableFuture<Entity> {
    val fileHandle = getServerPlayerFile(world, entityId)

    val serverPlayerConfig: Entity.() -> Unit = {
      safeWith { SharedInformationComponent(sharedInformation) }
      isTransientEntity = true
    }

    fun createNewServerPlayer(): CompletableFuture<Entity> {
      val protoEntity = world.createNewProtoPlayer(controlled = true) {
        this.ref = entityId.toProtoEntityRef()
        this.name = username
      }
      return world.load(protoEntity, configure = serverPlayerConfig).thenApply {
        saveServerPlayer(it)
        logger.debug { "Created persisted player profile for $entityId" }
        it
      }
    }

    if (fileHandle != null && fileHandle.exists()) {
      logger.debug { "Trying to load persisted player profile for $entityId" }
      try {
        val proto = ProtoWorld.Entity.parseFrom(fileHandle.readBytes())
        return world.load(proto, configure = serverPlayerConfig).handle { entity, ex ->
          if (ex != null) {
            logger.warn(ex) { "Exception when loading world" }
            return@handle createNewServerPlayer().join()
          } else {
            logger.debug { "Loaded persisted player profile for $entityId" }
          }
          return@handle entity
        }
      } catch (e: Exception) {
        logger.warn(e) { "Exception when loading world" }
        // fall through
      }
    }
    logger.debug { "Creating fresh player profile for $entityId" }
    return createNewServerPlayer()
  }

  fun saveServerPlayer(player: Entity) {
    val protoPlayer = player.save(toAuthoritative = true, ignoreTransient = true) ?: run {
      logger.warn { "Failed to create protobuf entity of player ${player.id}" }
      return
    }
    val fileHandle = getServerPlayerFile(player.world, player.id) ?: run {
      logger.warn { "Failed to get server player file for ${player.id}" }
      return
    }
    try {
      fileHandle.writeBytes(protoPlayer.toByteArray(), false)
    } catch (e: Exception) {
      logger.error(e) { "Exception when saving server player file for ${player.id}" }
    }
  }
}
