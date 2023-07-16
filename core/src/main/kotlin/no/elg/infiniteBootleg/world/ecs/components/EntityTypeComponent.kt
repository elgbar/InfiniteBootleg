package no.elg.infiniteBootleg.world.ecs.components

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.optionalPropertyFor
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType
import no.elg.infiniteBootleg.world.ecs.api.EntityParentLoadableMapper
import no.elg.infiniteBootleg.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.world.ecs.with

data class EntityTypeComponent(val entityType: EntityType) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    type = entityType
  }

  companion object : EntityParentLoadableMapper<EntityTypeComponent>() {
    var Entity.entityTypeComponent by propertyFor(mapper)
    var Entity.entityTypeComponentOrNull by optionalPropertyFor(mapper)

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

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity) {
      with(EntityTypeComponent(protoEntity.type))
    }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = true
  }
}
