package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.items.ItemType
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.EntityKt.locallyControlled
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.INITIAL_BRUSH_SIZE
import no.elg.infiniteBootleg.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.util.INITIAL_INTERACT_RADIUS
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.components.SelectedInventoryItemComponent.Companion.selectedInventoryItemComponentOrNull

data class LocallyControlledComponent(
  var brushSize: Float = INITIAL_BRUSH_SIZE,
  var interactRadius: Float = INITIAL_INTERACT_RADIUS,
  var instantBreak: Boolean = INITIAL_INSTANT_BREAK
) : EntitySavableComponent {

  fun isBreaking(entity: Entity) = !instantBreak && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && entity.selectedInventoryItemComponentOrNull?.element?.itemType == ItemType.TOOL

  companion object : EntityLoadableMapper<LocallyControlledComponent>() {
    var Entity.locallyControlledComponent by propertyFor(mapper)
    var Entity.locallyControlledComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): LocallyControlledComponent {
      val state = protoEntity.locallyControlled
      return with(
        LocallyControlledComponent(
          instantBreak = state.instantBreak,
          brushSize = state.brushRadius,
          interactRadius = state.interactRadius
        )
      )
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