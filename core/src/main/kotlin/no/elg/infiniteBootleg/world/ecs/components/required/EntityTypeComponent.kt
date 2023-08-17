package no.elg.infiniteBootleg.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent

data class EntityTypeComponent(val entityType: EntityType) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    entityType = this@EntityTypeComponent.entityType
  }

  companion object : EntityLoadableMapper<EntityTypeComponent>() {
    val Entity.entityTypeComponent by propertyFor(mapper)

    private val PLAYER_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.PLAYER)
    private val BLOCK_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.BLOCK)
    private val FALLING_BLOCK_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.FALLING_BLOCK)

    fun getType(entityType: EntityType): EntityTypeComponent {
      return when (entityType) {
        EntityType.BLOCK -> BLOCK_ENTITY_TYPE_COMPONENT
        EntityType.PLAYER -> PLAYER_ENTITY_TYPE_COMPONENT
        EntityType.FALLING_BLOCK -> FALLING_BLOCK_ENTITY_TYPE_COMPONENT
        EntityType.UNRECOGNIZED -> error("Unrecognized entity type")
        EntityType.GENERIC_ENTITY -> error("Generic entities are not supported")
      }
    }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): EntityTypeComponent = with(EntityTypeComponent(protoEntity.entityType))

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = true
  }
}
