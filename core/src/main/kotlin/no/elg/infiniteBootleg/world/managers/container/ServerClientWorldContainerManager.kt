package no.elg.infiniteBootleg.world.managers.container

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.elg.infiniteBootleg.events.ContainerEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.inventory.container.ContainerOwner.Companion.asProto
import no.elg.infiniteBootleg.inventory.container.OwnedContainer
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.server.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.server.serverBoundContainerRequest
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
      Main.logger().debug("ServerClientWorldContainerManager", "Failed to find a container for $owner")
    }
    internalContainers[owner] = ownedContainer
    EventManager.dispatchEvent(ContainerEvent.Changed(ownedContainer))
  }

  companion object {
    private const val CONTAINER_RESPONSE_TIMEOUT_SECONDS = 1L
  }
}