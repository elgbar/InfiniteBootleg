package no.elg.infiniteBootleg.client.world.managers.container

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.client.world.world.ServerClientWorld
import no.elg.infiniteBootleg.core.events.ContainerEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner.Companion.asProto
import no.elg.infiniteBootleg.core.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundContainerRequest
import no.elg.infiniteBootleg.core.world.managers.container.WorldContainerManager
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class ServerClientWorldContainerManager(val world: ServerClientWorld) : WorldContainerManager {

  private val internalContainers = ConcurrentHashMap<ContainerOwner, OwnedContainer>()

  private val ownerToContainerCache: Cache<ContainerOwner, CompletableFuture<OwnedContainer>> by lazy {
    Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(CONTAINER_RESPONSE_TIMEOUT_SECONDS))
      .build()
  }

  override fun find(owner: ContainerOwner): CompletableFuture<OwnedContainer> =
    ownerToContainerCache.get(owner) {
      world.serverClient.sendServerBoundPacket { serverBoundContainerRequest(owner.asProto()) }
      CompletableFuture<OwnedContainer>().orTimeout(CONTAINER_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

  fun updateContainerFromServer(ownedContainer: OwnedContainer) {
    val (owner, _) = ownedContainer
    ownerToContainerCache.getIfPresent(owner)?.complete(ownedContainer) ?: run {
      logger.debug { "Failed to find a container for $owner" }
    }
    internalContainers[owner] = ownedContainer
    EventManager.dispatchEvent(ContainerEvent.ContentChanged(ownedContainer))
  }

  companion object {
    private const val CONTAINER_RESPONSE_TIMEOUT_SECONDS = 1L
  }
}
