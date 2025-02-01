package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.core.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.core.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Tool
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.component.AuthoritativeOnlyComponent
import no.elg.infiniteBootleg.core.world.ecs.components.inventory.HotbarComponent.Companion.selectedItem
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.locallyControlled
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class LocallyControlledComponent(
  var brushSize: Float = INITIAL_BRUSH_SIZE,
  var interactRadius: Float = INITIAL_INTERACT_RADIUS,
  var instantBreak: Boolean = INITIAL_INSTANT_BREAK
) : EntitySavableComponent, AuthoritativeOnlyComponent {

  fun isBreaking(entity: Entity) = !instantBreak && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && entity.selectedItem?.element is Tool

  override fun hudDebug(): String = "brush size: $brushSize, interactRadius: $interactRadius, instantBreak: $instantBreak"

  companion object : EntityLoadableMapper<LocallyControlledComponent>() {
    var Entity.locallyControlledComponent by propertyFor(mapper)
    var Entity.locallyControlledComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): LocallyControlledComponent? {
      val state = protoEntity.locallyControlled
      return safeWith {
        LocallyControlledComponent(
          instantBreak = state.instantBreak,
          brushSize = state.brushRadius,
          interactRadius = state.interactRadius
        )
      }
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasLocallyControlled()
  }

  override fun EntityKt.Dsl.save() {
    locallyControlled = locallyControlled {
      instantBreak = this@LocallyControlledComponent.instantBreak
      brushRadius = this@LocallyControlledComponent.brushSize
      interactRadius = this@LocallyControlledComponent.interactRadius
    }
  }
}
