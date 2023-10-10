package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import no.elg.infiniteBootleg.util.LongMapUtil.component1
import no.elg.infiniteBootleg.util.LongMapUtil.component2
import no.elg.infiniteBootleg.util.breakableLocs
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.blocks.Block.Companion.compactWorldLoc
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.CurrentlyBreakingComponent.Companion.currentlyBreakingComponentOrNull
import no.elg.infiniteBootleg.world.ecs.system.restriction.ClientSystem

object MineBlockSystem :
  IteratingSystem(allOf(WorldComponent::class, LocallyControlledComponent::class, SelectedInventoryItemComponent::class).get(), UPDATE_PRIORITY_DEFAULT),
  ClientSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val controls = entity.locallyControlledComponent
    val world = entity.world
    val currentLocs = entity.breakableLocs(world, inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)

    if (!controls.isBreaking(entity)) {
      entity.currentlyBreakingComponentOrNull?.reset()
      return
    }

    val breakingComponent = entity.currentlyBreakingComponentOrNull ?: entity.with(CurrentlyBreakingComponent())

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

    world.removeBlocks(justDone, prioritize = true)
  }
}
