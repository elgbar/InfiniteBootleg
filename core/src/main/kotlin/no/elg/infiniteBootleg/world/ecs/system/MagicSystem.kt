package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.ashley.allOf
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.world.Staff
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.api.restriction.ClientSystem
import no.elg.infiniteBootleg.world.ecs.components.LocallyControlledComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent

object MagicSystem :
  IteratingSystem(
    allOf(
      WorldComponent::class,
      LocallyControlledComponent::class,
      SelectedInventoryItemComponent::class,
      PositionComponent::class
    ).get(),
    UPDATE_PRIORITY_DEFAULT
  ),
  ClientSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val held: Staff = entity.selectedInventoryItemComponentOrNull?.element as? Staff ?: return
    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
      Main.logger().warn("Casting spell with $held")
    }
  }
}
