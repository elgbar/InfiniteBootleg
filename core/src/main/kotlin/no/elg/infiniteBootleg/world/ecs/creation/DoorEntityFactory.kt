package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
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
import no.elg.infiniteBootleg.world.ecs.with
import no.elg.infiniteBootleg.world.world.World

fun Engine.createDoorBlockEntity(world: World, chunk: Chunk, worldX: Int, worldY: Int, material: Material) =
  createBlockEntity(world, chunk, worldX, worldY, material) {
    with(TextureRegionComponent(KAssets.doorClosedTexture))
    // This entity will handle input events
    with<DoorComponent>()
    with<PhysicsEventQueue>()
    with<OccupyingBlocksComponent>()

    val width = 2f
    val height = 4f

    createBody2DBodyComponent(
      entity,
      world,
      worldX.toFloat(),
      worldY.toFloat(),
      0f,
      0f,
      width,
      height,
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
      shape.setAsBox((width / 2f).coerceAtLeast(ESSENTIALLY_ZERO), (height / 2f).coerceAtLeast(ESSENTIALLY_ZERO))

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
