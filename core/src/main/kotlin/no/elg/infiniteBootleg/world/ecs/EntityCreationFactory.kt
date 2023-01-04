package no.elg.infiniteBootleg.world.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.BodyDef
import ktx.ashley.entity
import ktx.ashley.plusAssign
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.ecs.components.KillableComponent
import no.elg.infiniteBootleg.world.ecs.components.NamedComponent
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.VelocityComponent
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent

const val PLAYER_WIDTH = 2f * Block.BLOCK_SIZE - 1
const val PLAYER_HEIGHT = 4f * Block.BLOCK_SIZE - 1

private fun createBody2DBodyComponent(entity: Entity, world: World, worldX: Float, worldY: Float) {
  val bodyDef = BodyDef()
  bodyDef.type = BodyDef.BodyType.DynamicBody
  bodyDef.position.set(worldX, worldY)
  bodyDef.linearDamping = 1f
  bodyDef.fixedRotation = true

  world.worldBody.createBody(bodyDef) {
    it.gravityScale = Constants.DEFAULT_GRAVITY_SCALE
    it.userData = entity
    entity += Box2DBodyComponent(it, PLAYER_WIDTH, PLAYER_HEIGHT)
  }
}

fun Engine.createPlayerEntity(world: World, worldX: Float, worldY: Float, dx: Float, dy: Float, name: String = "Player", id: String? = null) = entity {
  with(WorldComponent(world))
  with(id?.let { IdComponent(it) } ?: IdComponent.createRandomId())
  with(PositionComponent(worldX, worldY))
  with(VelocityComponent(dx, dy))

  with(NamedComponent(name))
  with<KillableComponent>()
  with(TextureRegionComponent(KAssets.playerTexture))

  createBody2DBodyComponent(entity, world, worldX, worldY)
}
