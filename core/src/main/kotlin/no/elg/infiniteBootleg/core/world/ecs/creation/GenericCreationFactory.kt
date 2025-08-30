package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import ktx.ashley.EngineEntity
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.WorldCoordNumber
import no.elg.infiniteBootleg.core.util.futureEntity
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.util.toComponentsString
import no.elg.infiniteBootleg.core.util.toProtoEntityRef
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.basicRequiredEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.core.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.EntityTypeComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.protobuf.EntityKt
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.protobuf.vector2f
import java.util.UUID
import java.util.concurrent.CompletableFuture

const val ESSENTIALLY_ZERO = 0.001f
const val A_LITTLE_MORE_THAN_ESSENTIALLY_ZERO = 0.01f
const val A_LITTLE_BIT = 0.1f

internal fun checkFamilies(entity: Entity, wantedFamilies: Array<Pair<Family, String>>) {
  check(basicRequiredEntityFamily.matches(entity)) { "Finished entity does not match the required entity family" }
  wantedFamilies.forEach { (family: Family, errorStr) ->
    check(family.matches(entity)) { "Finished entity does not match $errorStr, current components: ${entity.toComponentsString()}" }
  }
}

fun EngineEntity.withRequiredComponents(
  entityType: ProtoWorld.Entity.EntityType,
  world: World,
  worldX: WorldCoordNumber,
  worldY: WorldCoordNumber,
  id: String? = null
) {
  safeWith { EntityTypeComponent.getType(entityType) }
  safeWith { id?.let { IdComponent(it) } ?: IdComponent.createRandomId() }
  safeWith { WorldComponent(world) }
  safeWith { PositionComponent(worldX.toFloat(), worldY.toFloat()) }
}

fun EntityKt.Dsl.withRequiredComponents(
  entityType: ProtoWorld.Entity.EntityType,
  world: World,
  worldX: WorldCoordNumber,
  worldY: WorldCoordNumber,
  id: String? = null
) {
  this.entityType = entityType
  ref = (id ?: UUID.randomUUID().toString()).toProtoEntityRef()
  worldUUID = world.uuid
  position = vector2f {
    x = worldX.toFloat()
    y = worldY.toFloat()
  }
}

/**
 * Baseline static entities which have some system attached
 */
fun Engine.createBlockEntity(
  world: World,
  worldX: WorldCoord,
  worldY: WorldCoord,
  material: Material,
  wantedFamilies: Array<Pair<Family, String>> = emptyArray(),
  additionalConfiguration: EngineEntity.() -> Unit = {}
): CompletableFuture<Entity> =
  futureEntity { future ->
    withRequiredComponents(ProtoWorld.Entity.EntityType.BLOCK, world, worldX, worldY)
    safeWith { MaterialComponent(material) }
    additionalConfiguration()
    checkFamilies(entity, arrayOf(blockEntityFamily to "blockEntityFamily", *wantedFamilies))

    // OK to complete now, as this entity does not contain a box2d component
    future.complete(Unit)
  }
