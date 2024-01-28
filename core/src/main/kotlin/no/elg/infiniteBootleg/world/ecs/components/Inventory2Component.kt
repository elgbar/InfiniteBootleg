package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.inventory.container.Inventory
import no.elg.infiniteBootleg.inventory.container.impl.ContainerImpl
import no.elg.infiniteBootleg.inventory.container.impl.InventoryImpl
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.safeWith
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.restriction.UniversalSystem

class Inventory2Component(val inventory: Inventory) : EntitySavableComponent, UniversalSystem {

  companion object : EntityLoadableMapper<Inventory2Component>() {
    val Entity.inventory2Component by propertyFor(Inventory2Component.mapper)
    var Entity.inventory2ComponentOrNull by optionalPropertyFor(Inventory2Component.mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) =
      safeWith {
        Inventory2Component(InventoryImpl(this.entity, ContainerImpl(40)))
      }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = true // hasInventory()
  }

  override fun EntityKt.Dsl.save() {
  }
}
