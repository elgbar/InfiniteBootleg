package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.with
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
import no.elg.infiniteBootleg.core.world.world.World
import java.util.concurrent.CompletableFuture

const val DOOR_WIDTH: Int = 2
const val DOOR_HEIGHT: Int = 4

fun Engine.createDoorBlockEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material): CompletableFuture<Entity> =
  createBlockEntity(world, worldX, worldY, material) {
    safeWith { TextureRegionNameComponent(InfAssets.DOOR_CLOSED_TEXTURE) }
    // This entity will handle input events
    with<DoorComponent>()
//    with<PhysicsEventQueueComponent>()
    with<OccupyingBlocksComponent>()

    createDoorBodyComponent(world, worldX, worldY)
  }
