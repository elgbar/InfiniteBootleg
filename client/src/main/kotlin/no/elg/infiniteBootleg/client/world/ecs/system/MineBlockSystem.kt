package no.elg.infiniteBootleg.client.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.util.inputMouseLocator
import no.elg.infiniteBootleg.core.items.ToolItem
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.core.net.serverBoundBreakingBlock
import no.elg.infiniteBootleg.core.util.isValid
import no.elg.infiniteBootleg.core.util.launchOnMultithreadedAsyncSuspendable
import no.elg.infiniteBootleg.core.util.partitionMap
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent.BreakingUpdateState.BROKEN_GIVE_BLOCK
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent.BreakingUpdateState.CONTINUE_BREAKING
import no.elg.infiniteBootleg.core.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.core.world.ecs.localPlayerFamily

object MineBlockSystem : IteratingSystem(localPlayerFamily, UPDATE_PRIORITY_DEFAULT) {

  fun CurrentlyBreakingComponent.reset() {
    sendCurrentProgress(true)
    breaking.clear()
  }

  fun CurrentlyBreakingComponent.sendCurrentProgress(zeroProgress: Boolean = false) {
    if (Main.isServerClient && breaking.isNotEmpty()) {
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

    val breakingItem: ToolItem = entity.selectedItem as? ToolItem ?: return
    val breakingTool: Tool = breakingItem.element

    val breakingComponent = entity.currentlyBreakingComponentOrNull ?: entity.safeWith { CurrentlyBreakingComponent() } ?: return
    val world = entity.world
    val currentLocs = breakingTool
      .breakableLocs(entity, world, inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)

    val evaluatedCurrentLocs = if (breakingComponent.breaking.isNotEmpty()) {
      // must be a set otherwise it kills the performance
      val currentLocsSet = LongOpenHashSet(currentLocs.iterator())
      breakingComponent.breaking.long2ObjectEntrySet().removeIf { (loc, breaking) ->
        // remove progress if its no longer in range, no longer the same material, or the held item changed
        loc !in currentLocsSet || world.getMaterial(loc) != breaking.block.material || breakingItem != breaking.item
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
        breakingComponent.breaking.put(block.compactWorldLoc, CurrentlyBreakingComponent.CurrentlyBreaking(block, breakingItem))
      }

    val (justMinedGive, justMinedDiscard) = breakingComponent.breaking.values
      .asSequence()
      .mapNotNull {
        val breakState = it.update(deltaTime)
        if (breakState == CONTINUE_BREAKING) {
          null
        } else {
          it to breakState
        }
      }.partitionMap({ it.second == BROKEN_GIVE_BLOCK }, { it.first.block.compactWorldLoc })

    breakingComponent.sendCurrentProgress()

    launchOnMultithreadedAsyncSuspendable {
      if (entity.isValid) {
        if (justMinedGive.isNotEmpty() || justMinedDiscard.isNotEmpty()) {
          val selectedItem = entity.selectedItem?.element as? Tool ?: return@launchOnMultithreadedAsyncSuspendable
          val container = entity.containerOrNull ?: return@launchOnMultithreadedAsyncSuspendable

          // first remove those blocks which the tool is effective against
          val justMinedGiveSize = justMinedGive.size.toUInt()
          val toolCount = container.count(selectedItem)
          val validJustMinedGive = if (toolCount >= justMinedGiveSize) {
            justMinedGive
          } else {
            // Just take the number the tool can break, not more
            val canBeRemoved = toolCount.coerceAtMost(justMinedGiveSize)
            justMinedGive.take(canBeRemoved.toInt())
          }
          val removedGiven = world.removeBlocks(validJustMinedGive, giveTo = entity, prioritize = true)

          // Then if the tool have any leftover we remove those blocks the tool is ineffective against
          val justMinedDiscardSize = justMinedDiscard.size.toUInt()
          val updatedToolCount = toolCount - removedGiven.size.toUInt()
          val validJustDoneDiscard = if (updatedToolCount >= justMinedDiscardSize) {
            justMinedDiscard
          } else {
            // Just take the number the tool can break, not more
            val canBeRemoved = updatedToolCount.coerceAtMost(justMinedDiscardSize)
            justMinedDiscard.take((canBeRemoved).toInt())
          }
          val removedDiscard = world.removeBlocks(validJustDoneDiscard, prioritize = true)

          container.remove(selectedItem, (removedGiven.size + removedDiscard.size).toUInt())
        }
      } else {
        // just in case
        breakingComponent.reset()
      }
    }
  }
}
