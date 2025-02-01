package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.net.ServerClient.Companion.sendServerBoundPacket
import no.elg.infiniteBootleg.net.serverBoundBreakingBlock
import no.elg.infiniteBootleg.util.LongMapUtil.component1
import no.elg.infiniteBootleg.util.LongMapUtil.component2
import no.elg.infiniteBootleg.util.breakableLocs
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.Tool
import no.elg.infiniteBootleg.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.inventory.ContainerComponent.Companion.containerOrNull
import no.elg.infiniteBootleg.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.localPlayerFamily

object MineBlockSystem : IteratingSystem(localPlayerFamily, UPDATE_PRIORITY_DEFAULT) {

  fun CurrentlyBreakingComponent.reset() {
    sendCurrentProgress(true)
    breaking.clear()
  }

  fun CurrentlyBreakingComponent.sendCurrentProgress(zeroProgress: Boolean = false) {
    if (Main.Companion.isServerClient && breaking.size > 0) {
      ClientMain.inst().serverClient.sendServerBoundPacket {
        val progresses = breaking.values().map { it.toBreakingProgress(zeroProgress) }
        serverBoundBreakingBlock(progresses)
      }
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val controls = entity.locallyControlledComponent
    val world = entity.world
    val currentLocs = entity.breakableLocs(world, inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)

    if (!controls.isBreaking(entity)) {
      entity.currentlyBreakingComponentOrNull?.reset()
      return
    }

    val breakingComponent = entity.currentlyBreakingComponentOrNull ?: entity.safeWith { CurrentlyBreakingComponent() } ?: return

    breakingComponent.breaking.removeAll { (loc, breaking) ->
      loc !in currentLocs || world.getMaterial(loc) != breaking.block.material
    }
    currentLocs
      .filterNot { breakingComponent.breaking.containsKey(it) }
      .mapNotNull { world.getBlock(it) }
      .forEach { block ->
        breakingComponent.breaking.put(block.compactWorldLoc, CurrentlyBreakingComponent.CurrentlyBreaking(block))
      }

    // MUST use .values() as the entries iterator is returning the same instance (fuck me)
    val justDone = breakingComponent.breaking.values()
      .filter { breaking -> breaking.progressHandler.update(deltaTime) }
      .mapTo(mutableSetOf()) { it.block.compactWorldLoc }

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
    world.removeBlocks(validJustDone, giveTo = entity, prioritize = true)
  }
}
