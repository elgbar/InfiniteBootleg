package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.material
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.ecs.api.EntityParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.api.SerializableComponent

data class MaterialComponent(val material: Material) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    material = this@MaterialComponent.asProto()
  }

  companion object : SerializableComponent<MaterialComponent, ProtoWorld.Material>, EntityParentLoadableMapper<MaterialComponent>() {
    val Entity.material get() = materialComponent.material
    val Entity.materialOrNull get() = materialComponentOrNull?.material
    val Entity.materialComponent by propertyFor(mapper)
    var Entity.materialComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = with(MaterialComponent(Material.entries[protoEntity.material.ordinal]))

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasMaterial()

    override fun fromProto(protoComponent: ProtoWorld.Material): MaterialComponent {
      return MaterialComponent(Material.entries[protoComponent.ordinal])
    }

    fun Material.asProto(): ProtoWorld.Material = material {
      ordinal = this@asProto.ordinal
    }

    override fun MaterialComponent.asProto(): ProtoWorld.Material = this.material.asProto()
  }
}
