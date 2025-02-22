package no.elg.infiniteBootleg.client.world.managers.interfaces

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.actors.isShown
import no.elg.infiniteBootleg.client.util.IBVisWindow
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.events.BlockChangedEvent
import no.elg.infiniteBootleg.core.events.InterfaceEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkUnloadedEvent
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.InterfaceId
import no.elg.infiniteBootleg.core.util.launchOnMain
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class InterfaceManager(private val world: ClientWorld) : Disposable {

  private val interfaces: MutableMap<InterfaceId, IBVisWindow> = ConcurrentHashMap()

  // Make sure the interface close when block is changed/removed
  private val containerDestroyedEvent = EventManager.registerListener<BlockChangedEvent> {
    val block = it.oldOrNewBlock ?: return@registerListener
    if (block.world === world) {
      val interfaceId = ContainerOwner.toInterfaceId(block)
      removeInterface(interfaceId)
    }
  }

  private val containerChunkUnloadedEvent = EventManager.registerListener<ChunkUnloadedEvent> {
    if (it.chunk.world === world) {
      it.chunk.asSequence()
        .mapNotNull { it?.entity?.ownedContainerOrNull }
        .forEach { removeInterface(it.owner.toInterfaceId()) }
    }
  }

  fun addInterface(interfaceId: InterfaceId, interfaceWindow: IBVisWindow) {
    interfaces.compute(interfaceId) { _, maybeOldWindow ->
      maybeOldWindow?.let { oldWindow ->
        logger.warn { "Duplicate interface id $interfaceId, closing and removing old window" }
        oldWindow.close()
      }
      logger.debug { "Added new interface with id $interfaceId" }
      interfaceWindow
    }.also {
      EventManager.dispatchEvent(InterfaceEvent.Added(interfaceId))
    }
  }

  fun removeInterface(interfaceId: InterfaceId) {
    interfaces.remove(interfaceId)?.let { removedWindow ->
      removedWindow.close()
      logger.debug { "Removing interface id $interfaceId, closing and removing old window" }
      EventManager.dispatchEvent(InterfaceEvent.Removed(interfaceId))
    }
  }

  fun areAnyOpen(): Boolean = interfaces.keys.any(::isOpen)

  fun isOpen(interfaceId: InterfaceId): Boolean = interfaces[interfaceId]?.isShown() ?: false

  private fun getOrCreate(interfaceId: InterfaceId, createIfMissing: () -> IBVisWindow?): CompletableFuture<IBVisWindow?> =
    CompletableFuture<IBVisWindow?>().also { future ->
      launchOnMain {
        synchronized(interfaces) {
          interfaces[interfaceId] ?: createIfMissing()
        }.also { future.complete(it) }
      }
    }

  fun openInterface(interfaceId: InterfaceId, stage: Stage, createIfMissing: () -> IBVisWindow? = { null }) {
    getOrCreate(interfaceId, createIfMissing).thenApply {
      it?.show(stage) ?: run {
        logger.debug { "Failed to open unknown interface with id $interfaceId" }
      }
    }
  }

  fun closeInterface(interfaceId: InterfaceId) {
    // No point in creating a new interface if we are just to close it
    interfaces[interfaceId]?.close() ?: run {
      logger.debug { "Failed to close unknown interface with id $interfaceId" }
    }
  }

  fun toggleInterface(interfaceId: InterfaceId, stage: Stage, createIfMissing: () -> IBVisWindow? = { null }) {
    getOrCreate(interfaceId, createIfMissing).thenApply {
      it?.toggleShown(stage) ?: run {
        logger.debug { "Failed to toggle unknown interface with id $interfaceId" }
      }
    }
  }

  fun getInterface(interfaceId: InterfaceId): IBVisWindow? = interfaces[interfaceId]

  override fun dispose() {
    interfaces.values.forEach(IBVisWindow::dispose)
    interfaces.clear()
    containerDestroyedEvent.removeListener()
    containerChunkUnloadedEvent.removeListener()
  }
}
