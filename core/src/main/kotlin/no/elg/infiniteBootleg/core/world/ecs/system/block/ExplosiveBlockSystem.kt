package no.elg.infiniteBootleg.core.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import ktx.ashley.hasNot
import ktx.ashley.remove
import ktx.collections.gdxSetOf
import no.elg.infiniteBootleg.core.util.WorldCoord
import no.elg.infiniteBootleg.core.util.distCubed
import no.elg.infiniteBootleg.core.util.launchOnAsync
import no.elg.infiniteBootleg.core.world.Material
import no.elg.infiniteBootleg.core.world.blocks.Block
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.core.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.core.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.core.world.ecs.api.restriction.system.AuthoritativeSystem
import no.elg.infiniteBootleg.core.world.ecs.components.ExplosiveComponent
import no.elg.infiniteBootleg.core.world.ecs.components.ExplosiveComponent.Companion.explosiveComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.core.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.core.world.ecs.explosiveBlockFamily
import no.elg.infiniteBootleg.core.world.world.World
import kotlin.math.abs

object ExplosiveBlockSystem : IteratingSystem(explosiveBlockFamily, UPDATE_PRIORITY_DEFAULT), AuthoritativeSystem {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val explosiveComponent = entity.explosiveComponent
    explosiveComponent.fuse -= deltaTime
    if (explosiveComponent.fuse <= 0) {
      entity.remove<ExplosiveComponent>()

      val positionComponent = entity.positionComponent
      val worldX = positionComponent.blockX
      val worldY = positionComponent.blockY
      val world = entity.world
      launchOnAsync { explosiveComponent.explode(world, worldX, worldY) }
    }
  }

  private fun ExplosiveComponent.explode(world: World, worldX: WorldCoord, worldY: WorldCoord) {
    val destroyed = gdxSetOf<Block>()
    var x = MathUtils.floor(worldX - strength)
    while (x < worldX + strength) {
      var y = MathUtils.floor(worldY - strength)
      while (y < worldY + strength) {
        val block = world.getRawBlock(x, y, true)
        val mat = block.materialOrAir()
        val hardness = mat.hardness
        if (mat != Material.AIR && hardness >= 0 && block != null) {
          val dist = distCubed(
            worldX,
            worldY,
            block.worldX,
            block.worldY
          ) * hardness * abs(MathUtils.random.nextGaussian() + ExplosiveComponent.Companion.RESISTANCE)
          val otherBlockEntity = block.entity
          if (dist < strength * strength && (otherBlockEntity == null || otherBlockEntity.hasNot(ExplosiveComponent.Companion.mapper))) {
            destroyed.add(block)
          }
        }
        y++
      }
      x++
    }
    world.removeBlocks(destroyed)
  }
}
