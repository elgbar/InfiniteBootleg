package no.elg.infiniteBootleg.world.ecs.system.block

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import ktx.ashley.hasNot
import ktx.ashley.remove
import ktx.collections.gdxSetOf
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.blocks.Block
import no.elg.infiniteBootleg.world.blocks.Block.Companion.materialOrAir
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldX
import no.elg.infiniteBootleg.world.blocks.Block.Companion.worldY
import no.elg.infiniteBootleg.world.ecs.UPDATE_PRIORITY_DEFAULT
import no.elg.infiniteBootleg.world.ecs.components.block.ExplosiveComponent
import no.elg.infiniteBootleg.world.ecs.components.block.ExplosiveComponent.Companion.explosiveComponent
import no.elg.infiniteBootleg.world.ecs.components.required.PositionComponent.Companion.positionComponent
import no.elg.infiniteBootleg.world.ecs.components.required.WorldComponent.Companion.world
import no.elg.infiniteBootleg.world.ecs.explosiveBlockFamily
import kotlin.math.abs

object ExplosiveBlockSystem : IteratingSystem(explosiveBlockFamily, UPDATE_PRIORITY_DEFAULT) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val explosiveComponent = entity.explosiveComponent
    explosiveComponent.fuse -= deltaTime
    if (explosiveComponent.fuse <= 0) {
      entity.remove<ExplosiveComponent>()
      Main.inst().scheduler.executeAsync { explosiveComponent.explode(entity) }
    }
  }

  private fun ExplosiveComponent.explode(entity: Entity) {
    val destroyed = gdxSetOf<Block>()
    val positionComponent = entity.positionComponent
    val worldX = positionComponent.blockX
    val worldY = positionComponent.blockY
    val world = entity.world
    var x = MathUtils.floor(worldX - strength)
    while (x < worldX + strength) {
      var y = MathUtils.floor(worldY - strength)
      while (y < worldY + strength) {
        val block = world.getRawBlock(x, y, true)
        val mat = block.materialOrAir()
        val hardness = mat.hardness
        if (mat != Material.AIR && hardness >= 0 && block != null) {
          val dist = (Location.distCubed(worldX, worldY, block.worldX, block.worldY) * hardness * abs(MathUtils.random.nextGaussian() + ExplosiveComponent.RESISTANCE))
          val otherBlockEntity = block.entity
          if (dist < strength * strength && (otherBlockEntity == null || otherBlockEntity.hasNot(ExplosiveComponent.mapper))) {
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
