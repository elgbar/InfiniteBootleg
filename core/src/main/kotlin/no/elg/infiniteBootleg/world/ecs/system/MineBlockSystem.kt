package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.allOf
import no.elg.infiniteBootleg.util.breakableBlocks
import no.elg.infiniteBootleg.util.inputMouseLocator
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent.Companion.locallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

object MineBlockSystem : IteratingSystem(allOf(WorldComponent::class, LocallyControlledComponent::class).get(), UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val controls = entity.locallyControlledComponent
    if (!controls.isBreaking(entity) || inputMouseLocator.isOverNewBlock) {
      controls.breakingProgress.reset()
      return
    }

    val justDone = controls.breakingProgress.update(deltaTime)
    if (justDone) {
      val world = entity.world
      val blocksBreaking = entity.breakableBlocks(world, inputMouseLocator.mouseBlockX, inputMouseLocator.mouseBlockY, controls.brushSize, controls.interactRadius)
      world.removeBlocks(blocksBreaking.asIterable(), prioritize = true)
    }
  }
}
