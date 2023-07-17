package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Constants
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.box2d.Filters
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
import no.elg.infiniteBootleg.world.ecs.components.transients.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.transients.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.doorEntityFamily
import no.elg.infiniteBootleg.world.ecs.drawableEntitiesFamily
import no.elg.infiniteBootleg.world.ecs.entityWithPhysicsEventFamily
import no.elg.infiniteBootleg.world.ecs.standaloneGridOccupyingBlocksFamily
import no.elg.infiniteBootleg.world.world.World

const val DOOR_WIDTH: Int = 2
const val DOOR_HEIGHT: Int = 4

fun Engine.createDoorBlockEntity(world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material) =
  createBlockEntity(world, chunk, worldX, worldY, material) {
    with(TextureRegionComponent(KAssets.doorClosedTexture))
    // This entity will handle input events
    with<DoorComponent>()
    with<PhysicsEventQueue>()
    with<OccupyingBlocksComponent>()

    createBody2DBodyComponent(
      entity,
      world,
      worldX.toFloat() + DOOR_WIDTH / 2f,
      worldY.toFloat() + DOOR_HEIGHT / 2f,
      0f,
      0f,
      DOOR_WIDTH.toFloat(),
      DOOR_HEIGHT.toFloat(),
      arrayOf(
        drawableEntitiesFamily to "drawableEntitiesFamily",
        blockEntityFamily to "blockEntityFamily",
        doorEntityFamily to "doorEntityFamily",
        entityWithPhysicsEventFamily to "entityWithPhysicsEventFamily",
        standaloneGridOccupyingBlocksFamily to "standaloneGridOccupyingBlocksFamily"
      ),
      BodyDef.BodyType.StaticBody
    ) {
      val shape = PolygonShape()
      shape.setAsBox(DOOR_WIDTH / 2f, DOOR_HEIGHT / 2f)

      val def = FixtureDef()
      def.shape = shape
      def.density = Constants.DEFAULT_FIXTURE_DENSITY
      def.friction = Constants.DEFAULT_FIXTURE_FRICTION
      def.restitution = 0f

      val fix: Fixture = it.createFixture(def)
      fix.filterData = Filters.EN__GROUND_FILTER
      fix.userData = it.userData
      fix.isSensor = true

      shape.dispose()
    }
  }
