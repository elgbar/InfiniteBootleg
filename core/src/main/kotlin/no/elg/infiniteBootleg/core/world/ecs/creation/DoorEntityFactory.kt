package no.elg.infiniteBootleg.core.world.ecs.creation

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import ktx.ashley.with
import no.elg.infiniteBootleg.core.assets.InfAssets
import no.elg.infiniteBootleg.core.util.BlockUnitF
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.safeWith
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.ecs.components.DoorComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OccupyingBlocksComponent
import no.elg.infiniteBootleg.core.world.ecs.components.OffsetPositionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.PhysicsEventQueueComponent
import no.elg.infiniteBootleg.core.world.ecs.components.TextureRegionNameComponent
import no.elg.infiniteBootleg.core.world.world.World
import java.util.concurrent.CompletableFuture

const val DOOR_WIDTH: BlockUnitF = 1.5f
const val DOOR_HEIGHT: BlockUnitF = 3f
const val DOOR_X_OFFSET: BlockUnitF = 0.25f

fun Engine.createDoorBlockEntity(world: World, worldX: WorldCoord, worldY: WorldCoord, material: Material): CompletableFuture<Entity> =
  createBlockEntity(world, worldX, worldY, material) {
    safeWith { TextureRegionNameComponent(InfAssets.DOOR_CLOSED_TEXTURE) }
    // This entity will handle input events
    with<DoorComponent>()
    with<PhysicsEventQueueComponent>()
    safeWith { OccupyingBlocksComponent(hardLink = true) }
    safeWith { OffsetPositionComponent(offsetX = DOOR_X_OFFSET + DOOR_WIDTH / 2f, offsetY = DOOR_HEIGHT / 2f) }

    createDoorBodyComponent(world, worldX, worldY)
  }
