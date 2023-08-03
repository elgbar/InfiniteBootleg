package no.elg.infiniteBootleg.world.ecs.creation

import com.badlogic.ashley.core.Engine
import ktx.ashley.with
import no.elg.infiniteBootleg.KAssets
import no.elg.infiniteBootleg.util.with
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.chunks.Chunk
import no.elg.infiniteBootleg.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.DoorComponent
import no.elg.infiniteBootleg.world.ecs.components.additional.OccupyingBlocksComponent
import no.elg.infiniteBootleg.world.ecs.components.events.PhysicsEventQueue
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

    createDoorBodyComponent(world, worldX, worldY)
  }
