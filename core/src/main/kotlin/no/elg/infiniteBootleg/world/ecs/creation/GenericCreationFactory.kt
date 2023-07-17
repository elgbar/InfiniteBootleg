package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import ktx.ashley.EngineEntity
import ktx.ashley.entity
import ktx.ashley.plusAssign
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.ProtoWorld
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.basicRequiredEntityFamily
import no.elg.infiniteBootleg.world.ecs.basicStandaloneEntityFamily
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.EntityTypeComponent
import no.elg.infiniteBootleg.world.ecs.components.MaterialComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.ChunkComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.WorldComponent
import no.elg.infiniteBootleg.world.world.World

const val ESSENTIALLY_ZERO = 0.001f

internal fun createBody2DBodyComponent(
  entity: Entity,
  world: World,
  worldX: Float,
  worldY: Float,
  dx: Float,
  dy: Float,
  width: Float,
  height: Float,
  wantedFamilies: Array<Pair<Family, String>> = emptyArray(),
  bodyType: BodyDef.BodyType = BodyDef.BodyType.DynamicBody,
  afterBodyCreated: (Entity) -> Unit = {},
  createBody: (Body) -> Unit
) {
  val bodyDef = BodyDef()
  bodyDef.type = bodyType
  bodyDef.position.set(worldX, worldY)
  bodyDef.linearVelocity.set(dx, dy)
  bodyDef.linearDamping = 0.5f
  bodyDef.fixedRotation = true

  world.worldBody.createBody(bodyDef) {
    it.gravityScale = Constants.DEFAULT_GRAVITY_SCALE
    it.userData = entity

    createBody(it)
    entity += Box2DBodyComponent(it, width, height)
    if (Settings.debug) {
      check(basicStandaloneEntityFamily.matches(entity)) { "Finished entity does not match the basic entity family" }
      checkFamilies(entity, wantedFamilies)
      Main.logger().log("Finishing setting up box2d entity")
    }
    afterBodyCreated(entity)
  }
}

internal fun checkFamilies(entity: Entity, wantedFamilies: Array<Pair<Family, String>>) {
  check(basicRequiredEntityFamily.matches(entity)) { "Finished entity does not match the required entity family" }
  wantedFamilies.forEach { (family: Family, errorStr) ->
    check(family.matches(entity)) {
      val currComponents = entity.components.map { c -> c::class.simpleName }
      "Finished entity does not match $errorStr, current components: $currComponents"
    }
  }
}

fun EngineEntity.withRequiredComponents(entityType: ProtoWorld.Entity.EntityType, world: World, worldX: Number, worldY: Number, id: String? = null) {
  with(EntityTypeComponent.getType(entityType))
  with(id?.let { IdComponent(it) } ?: IdComponent.createRandomId())
  with(WorldComponent(world))
  with(PositionComponent(worldX.toFloat(), worldY.toFloat()))
}

/**
 * Baseline static entities which have some system attached
 */
fun Engine.createBlockEntity(
  world: World,
  chunk: Chunk,
  worldX: Int,
  worldY: Int,
  material: Material,
  wantedFamilies: Array<Pair<Family, String>> = emptyArray(),
  additionalConfiguration: EngineEntity.() -> Unit = {}
) = entity {
  withRequiredComponents(ProtoWorld.Entity.EntityType.BLOCK, world, worldX, worldY)
  with(ChunkComponent(chunk))
  with(MaterialComponent(material))
  additionalConfiguration()
  checkFamilies(entity, arrayOf(blockEntityFamily to "blockEntityFamily", *wantedFamilies))
}
