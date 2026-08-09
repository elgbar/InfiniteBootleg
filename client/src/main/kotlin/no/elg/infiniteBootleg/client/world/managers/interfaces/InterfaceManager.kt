package no.elg.infiniteBootleg.client.world.managers.interfaces

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Disposable
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import ktx.actors.isShown
import ktx.assets.disposeSafely
import ktx.async.onRenderingThread
import no.elg.infiniteBootleg.client.util.IBVisWindow
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.events.BlockChangedEvent
import no.elg.infiniteBootleg.core.events.InterfaceEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.events.chunks.ChunkUnloadedEvent
import no.elg.infiniteBootleg.core.inventory.container.ContainerOwner
import no.elg.infiniteBootleg.core.inventory.container.InterfaceId
import no.elg.infiniteBootleg.core.util.launchOnMainSuspendable
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.ownedContainerOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.contract

private val logger = KotlinLogging.logger {}

class InterfaceManager(private val world: ClientWorld) : Disposable {

  private val interfaces: MutableMap<InterfaceId, IBVisWindow> = ConcurrentHashMap()

  // Make sure the interface close when block is changed/removed
  private val containerDestroyedEvent = EventManager.registerListener<BlockChangedEvent> { event ->
    val block = event.oldOrNewBlock ?: return@registerListener
    if (block.world === world) {
      val interfaceId = ContainerOwner.toInterfaceId(block)
      removeInterface(interfaceId)
    }
  }

  private val containerChunkUnloadedEvent = EventManager.registerListener<ChunkUnloadedEvent> { event ->
    if (event.chunk.world === world) {
      event.chunk.asSequence()
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
    interfaces.remove(interfaceId)?.also { removedWindow ->
      removedWindow.close()
      logger.debug { "Removing interface id $interfaceId, closing and removing old window" }
      EventManager.dispatchEvent(InterfaceEvent.Removed(interfaceId))
      removedWindow.disposeSafely()
    }
  }

  fun closeAllInterfaces() {
    interfaces.keys.forEach(::closeInterface)
  }

  /**
   * @return The number of interfaces removed
   */
  fun clearInterfaces(): Int {
    val keys = interfaces.keys
    val size = keys.size
    keys.forEach(::removeInterface)
    return size
  }

  fun areAnyOpen(): Boolean = interfaces.keys.any(::isOpen)

  fun isOpen(interfaceId: InterfaceId): Boolean = get(interfaceId)?.isShown() ?: false

  suspend fun getOrCreate(interfaceId: InterfaceId, createIfMissing: suspend () -> IBVisWindow): IBVisWindow {
    contract {
      returnsResultOf(createIfMissing)
    }
    return get(interfaceId) ?: onRenderingThread {
      // Extra get-check to make sure we do not create duplicated interfaces
      get(interfaceId) ?: createIfMissing()
    }
  }

  fun openInterface(interfaceId: InterfaceId, stage: Stage, createIfMissing: suspend () -> IBVisWindow): Job =
    launchOnMainSuspendable {
      val window = getOrCreate(interfaceId, createIfMissing)
      window.show(stage)
    }

  fun closeInterface(interfaceId: InterfaceId) {
    // No point in creating a new interface if we are just to close it
    get(interfaceId)?.close() ?: run {
      logger.debug { "Failed to close unknown interface with id $interfaceId" }
    }
  }

  fun toggleInterface(interfaceId: InterfaceId, stage: Stage, createIfMissing: suspend () -> IBVisWindow): Job =
    launchOnMainSuspendable {
      val window = getOrCreate(interfaceId, createIfMissing)
      window.toggleShown(stage)
    }

  operator fun get(interfaceId: InterfaceId): IBVisWindow? = interfaces[interfaceId]

  override fun dispose() {
    containerDestroyedEvent.removeListener()
    containerChunkUnloadedEvent.removeListener()
    // Clear after we remove listeners to not having interfaces we haven't cleared
    val _ = clearInterfaces()
  }
}
