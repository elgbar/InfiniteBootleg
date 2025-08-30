package no.elg.infiniteBootleg.core.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.Material.Companion.asProto
import no.elg.infiniteBootleg.core.world.Material.Companion.fromProto
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld

data class MaterialComponent(val material: Material) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    material = this@MaterialComponent.material.asProto()
  }

  override fun hudDebug(): String = "material ${material.displayName}"

  companion object : EntityLoadableMapper<MaterialComponent>() {
    val Entity.material get() = materialComponent.material
    val Entity.materialOrNull get() = materialComponentOrNull?.material
    val Entity.materialComponent by propertyFor(mapper)
    var Entity.materialComponentOrNull by optionalPropertyFor(mapper)

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) = safeWith { MaterialComponent(protoEntity.material.fromProto()) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = hasMaterial()
  }
}
