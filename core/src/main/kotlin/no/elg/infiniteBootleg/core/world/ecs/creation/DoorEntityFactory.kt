package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import ktx.ashley.with
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionComponent
import no.elg.infiniteBootleg.core.world.world.World

const val DOOR_WIDTH: Int = 2
const val DOOR_HEIGHT: Int = 4

fun Engine.createDoorBlockEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material) =
  createBlockEntity(world, worldX, worldY, material) {
    safeWith { TextureRegionComponent(InfAssets.Companion.DOOR_CLOSED_TEXTURE) }
    // This entity will handle input events
    with<DoorComponent>()
    with<PhysicsEventQueueComponent>()
    with<OccupyingBlocksComponent>()

    createDoorBodyComponent(world, worldX, worldY)
  }
