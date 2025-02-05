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
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

object ServerWorldLoader {

  private const val PLAYERS_PATH = "players"

  private fun getServerPlayerFile(world: World, playerId: String): FileHandle? {
    return world.worldFolder?.child(PLAYERS_PATH)?.child(playerId)
  }

  fun spawnServerPlayer(world: ServerWorld, entityId: String, username: String, sharedInformation: SharedInformation): CompletableFuture<Entity> {
    val fileHandle = getServerPlayerFile(world, entityId)
    logger.debug { "Persisted player profile file ${fileHandle?.path()}" }

    val serverPlayerConfig: Entity.() -> Unit = {
      safeWith { SharedInformationComponent(sharedInformation) }
      isTransientEntity = true
    }

    fun createNewServerPlayer(): CompletableFuture<Entity> {
      logger.debug { "Creating fresh player profile for $entityId" }
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
      val proto = try {
        ProtoWorld.Entity.parseFrom(fileHandle.readBytes())
      } catch (e: Exception) {
        logger.error(e) { "Failed to parse server profile for $entityId" }
        return createNewServerPlayer()
      }

      // Handle async as recovering from exception joins a future
      return world.load(proto, configure = serverPlayerConfig).handleAsync { entity, ex ->
        if (ex != null || entity == null) {
          logger.error(ex) { "Failed to load server profile for $entityId" }
          createNewServerPlayer().get(10, TimeUnit.SECONDS)
        } else {
          logger.debug { "Loaded persisted player profile for $entityId" }
          entity
        }
      }
    }
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
