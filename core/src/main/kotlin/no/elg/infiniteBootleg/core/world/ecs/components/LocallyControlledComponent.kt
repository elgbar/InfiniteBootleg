package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.BlockUnitF
import no.elg.infiniteBootleg.core.util.INITIAL_INSTANT_BREAK
import no.elg.infiniteBootleg.core.util.INITIAL_PLACE_RADIUS
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
  var instantBreak: Boolean = INITIAL_INSTANT_BREAK,
  var placeRadius: BlockUnitF = INITIAL_PLACE_RADIUS
) : EntitySavableComponent, AuthoritativeOnlyComponent {

  /**
   * Bare minimum check if we are breaking a block
   */
  fun isBreaking(entity: Entity) = !instantBreak && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && entity.selectedItem.element is Tool<*>

  override fun hudDebug(): String = "instantBreak: $instantBreak"

  companion object : EntityLoadableMapper<LocallyControlledComponent>() {
    var Entity.locallyControlledComponent by propertyFor(mapper)
    var Entity.locallyControlledComponentOrNull by optionalPropertyFor(mapper)
    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): LocallyControlledComponent? {
      val state = protoEntity.locallyControlled
      return safeWith {
        LocallyControlledComponent(
          instantBreak = state.instantBreak,
          placeRadius = state.placeRadius
        )
      }
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasLocallyControlled()
  }

  override fun EntityKt.Dsl.save() {
    locallyControlled = locallyControlled {
      instantBreak = this@LocallyControlledComponent.instantBreak
      placeRadius = this@LocallyControlledComponent.placeRadius
    }
  }
}
