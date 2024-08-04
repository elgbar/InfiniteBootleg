package no.elg.infiniteBootleg.world.managers.interfaces

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.actors.isShown
import no.elg.infiniteBootleg.events.InterfaceEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.inventory.container.InterfaceId
import no.elg.infiniteBootleg.util.IBVisWindow
import no.elg.infiniteBootleg.util.launchOnMain
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class InterfaceManager : Disposable {

  private val interfaces: MutableMap<InterfaceId, IBVisWindow> = ConcurrentHashMap()

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
    }.also {
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
  }
}
