package no.elg.infiniteBootleg.world.ecs.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import ktx.collections.plusAssign
import ktx.collections.removeAll
import no.elg.infiniteBootleg.world.Block.Companion.worldX
import no.elg.infiniteBootleg.world.Block.Companion.worldY
import no.elg.infiniteBootleg.world.blocks.EntityMarkerBlock
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.blockEntityFamily
import no.elg.infiniteBootleg.world.ecs.components.OccupyingBlocksComponent.Companion.occupyingLocations
import no.elg.infiniteBootleg.world.ecs.components.required.Box2DBodyComponent.Companion.box2d
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world

object UpdateBlockGridSystem : IteratingSystem(blockEntityFamily, UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val world = entity.world

    val pos = entity.positionComponent
    val halfBox2dWidth = entity.box2d.halfBox2dWidth
    val halfBox2dHeight = entity.box2d.halfBox2dHeight

    val currentOccupations = world.getBlocksAABB(pos.blockX.toFloat(), pos.blockY.toFloat(), halfBox2dWidth, halfBox2dHeight, raw = false, loadChunk = true, includeAir = true)
    entity.occupyingLocations.filter { it !in currentOccupations }.forEach(EntityMarkerBlock::remove)
    entity.occupyingLocations.removeAll { it !in currentOccupations }

    currentOccupations.forEach {
      if (it !is EntityMarkerBlock) {
        val block = EntityMarkerBlock.fromOtherBlock(it, entity)
        world.setBlock(it.worldX, it.worldY, block, updateTexture = false)
        entity.occupyingLocations += block
      }
    }
  }
}
