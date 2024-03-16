package no.elg.infiniteBootleg.world.managers.container

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import ktx.actors.isShown
import no.elg.infiniteBootleg.inventory.container.Container
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.server.serverBoundContainerRequest
import no.elg.infiniteBootleg.util.WorldCompactLoc
import no.elg.infiniteBootleg.util.WorldCoord
import no.elg.infiniteBootleg.util.compactLoc
import no.elg.infiniteBootleg.util.vector2iOf
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ServerClientWorldContainerManager(val world: ServerClientWorld) : WorldContainerManager() {

  private val timeoutSeconds = 1L

  private val awaitedFuturesCache: Cache<WorldCompactLoc, CompletableFuture<Container>> by lazy {
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(timeoutSeconds))
      .build()
  }

  fun updateContainer(worldCompactLoc: WorldCompactLoc, serverContainer: Container, overwrite: Boolean) {
    val existingContainer = containers[worldCompactLoc]
    if (existingContainer == null) {
      // Doesn't exist client side, simply add it
      containers[worldCompactLoc] = serverContainer
    } else if (overwrite || existingContainer.isEmpty()) {
      if (existingContainer.size != serverContainer.size) {
        // different size we must overwrite the client container
        containers[worldCompactLoc] = serverContainer
        reopen(existingContainer, serverContainer)
      } else {
        // Copy the server container to the client container
        // This is done for a smoother transition if the container is open
        for ((index, item) in serverContainer) {
          existingContainer.put(index, item)
        }
      }
    } else {
      // TODO merge the two containers
      containers[worldCompactLoc] = serverContainer
      reopen(existingContainer, serverContainer)
    }
    awaitedFuturesCache.getIfPresent(worldCompactLoc)?.complete(serverContainer)
    awaitedFuturesCache.invalidate(worldCompactLoc)
  }

  private fun reopen(oldContainer: Container, newContainer: Container) {
    world.render.getContainerActor(oldContainer)?.thenApply { (oldWindow, _) ->
      if (oldWindow.isShown()) {
        oldWindow.close()
        world.render.getContainerActor(newContainer)?.thenApply { (newWindow, stage) -> newWindow.show(stage) }
      }
    }
  }

  override fun find(worldX: WorldCoord, worldY: WorldCoord): CompletableFuture<Container> {
    val compact = compactLoc(worldX, worldY)
    val container = containers[compact]

    return if (container == null) {
      awaitedFuturesCache.get(compact) {
        world.serverClient.sendServerBoundPacket { serverBoundContainerRequest(vector2iOf(worldX, worldY)) }
        CompletableFuture<Container>().orTimeout(timeoutSeconds, TimeUnit.SECONDS)
      }
    } else {
      CompletableFuture.completedFuture(container)
    }
  }
}
