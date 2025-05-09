package no.elg.infiniteBootleg.core.world.ecs.components.required

import com.badlogic.ashley.core.Entity
import ktx.ashley.EngineEntity
import ktx.ashley.propertyFor
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.ecs.api.EntityLoadableMapper
import no.elg.infiniteBootleg.core.world.ecs.api.EntitySavableComponent
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.ProtoWorld.Entity.EntityType

data class EntityTypeComponent(val entityType: EntityType) : EntitySavableComponent {

  override fun EntityKt.Dsl.save() {
    entityType = this@EntityTypeComponent.entityType
  }

  override fun hudDebug(): String = entityType.name

  companion object : EntityLoadableMapper<EntityTypeComponent>() {
    val Entity.entityTypeComponent by propertyFor(mapper)
    fun Entity.isType(entityType: EntityType) = entityTypeComponent.entityType == entityType

    private val PLAYER_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.PLAYER)
    private val BLOCK_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.BLOCK)
    private val FALLING_BLOCK_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.FALLING_BLOCK)
    private val SPELL_ENTITY_TYPE_COMPONENT = EntityTypeComponent(EntityType.SPELL)

    fun getType(entityType: EntityType): EntityTypeComponent =
      when (entityType) {
        EntityType.BLOCK -> BLOCK_ENTITY_TYPE_COMPONENT
        EntityType.PLAYER -> PLAYER_ENTITY_TYPE_COMPONENT
        EntityType.FALLING_BLOCK -> FALLING_BLOCK_ENTITY_TYPE_COMPONENT
        EntityType.SPELL -> SPELL_ENTITY_TYPE_COMPONENT
        EntityType.UNRECOGNIZED -> error("Unrecognized entity type")
        EntityType.GENERIC_ENTITY -> error("Generic entities are not supported")
      }

    override fun EngineEntity.loadInternal(protoEntity: ProtoWorld.Entity): EntityTypeComponent? = safeWith { getType(protoEntity.entityType) }

    override fun ProtoWorld.Entity.checkShouldLoad(): Boolean = true
  }
}
