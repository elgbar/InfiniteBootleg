package no.elg.infiniteBootleg.client.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.util.inputMouseLocator
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundBreakingBlock
import no.elg.infiniteBootleg.core.util.breakableLocs
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsync
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.localPlayerFamily

object MineBlockSystem : IteratingSystem(localPlayerFamily, UPDATE_PRIORITY_DEFAULT) {

  fun CurrentlyBreakingComponent.reset() {
    sendCurrentProgress(true)
    breaking.clear()
  }

  fun CurrentlyBreakingComponent.sendCurrentProgress(zeroProgress: Boolean = false) {
    if (Main.Companion.isServerClient && breaking.isNotEmpty()) {
      ClientMain.inst().serverClient.sendServerBoundPacket {
        val progresses = breaking.values.map { it.toBreakingProgress(zeroProgress) }
        serverBoundBreakingBlock(progresses)
      }
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val controls = entity.locallyControlledComponent
    if (!controls.isBreaking(entity)) {
      entity.currentlyBreakingComponentOrNull?.reset()
      return
    }

    val breakingComponent = entity.currentlyBreakingComponentOrNull ?: entity.safeWith { CurrentlyBreakingComponent() } ?: return
    val world = entity.world
    val currentLocs = entity
      .breakableLocs(world, inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)

    val evaluatedCurrentLocs = if (breakingComponent.breaking.isNotEmpty()) {
      // must be a set otherwise it kills the performance
      val currentLocsSet = LongOpenHashSet(currentLocs.iterator())
      breakingComponent.breaking.long2ObjectEntrySet().removeIf { (loc, breaking) ->
        loc !in currentLocsSet || world.getMaterial(loc) != breaking.block.material
      }
      // we might as well use the result of the forced set above to save some duplicate work
      currentLocsSet.asSequence()
    } else {
      currentLocs
    }

    evaluatedCurrentLocs
      .filterNot { breakingComponent.breaking.containsKey(it) }
      .mapNotNull { world.getBlock(it) }
      .forEach { block ->
        breakingComponent.breaking.put(block.compactWorldLoc, CurrentlyBreakingComponent.CurrentlyBreaking(block))
      }

    val justDone = breakingComponent.breaking.values
      .asSequence()
      .filter { breaking -> breaking.progressHandler.update(deltaTime) }
      .mapTo(LongOpenHashSet()) { it.block.compactWorldLoc }

    breakingComponent.sendCurrentProgress()

    val heldStaff = entity.selectedItem?.element as? Tool
    val size = justDone.size
    val leftOver = heldStaff?.let { entity.containerOrNull?.remove(it, size.toUInt()) } ?: 0u
    val validJustDone = if (leftOver == 0u) {
      justDone
    } else {
      // Just take the number the pickaxe can mine, not more
      justDone.take(size - leftOver.toInt())
    }
    launchOnMultithreadedAsync { world.removeBlocks(validJustDone, giveTo = entity, prioritize = true) }
  }
}
