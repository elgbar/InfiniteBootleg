package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlockEntity

/**
 * @author Elg
 */
interface FallingTrait : TickingTrait {

  var falling: Boolean

  override fun tick() {
    tryFall()
  }

  companion object {
    fun FallingTrait.tryFall() {
      if (falling || Main.isServerClient()) {
        return
      }
      val south = Location.relative(block.worldX, block.worldY, Direction.SOUTH)
      val world = block.world
      if (world.isAirBlock(south)) {
        falling = true

        Main.inst().scheduler.executeAsync {
          block.destroy(true)
          Main.inst().scheduler.executeAsync {
            if (!falling) {
              return@executeAsync
            }
            // Do not update world straight away as if there are sand blocks above this it will begin to fall on the same tick
            val fallingBlock = FallingBlockEntity(world, block)
            if (fallingBlock.isInvalid) {
              falling = false // try again later
              return@executeAsync
            }
            world.addEntity(fallingBlock)

            val relative = block.getRelative(Direction.NORTH)
            if (relative is TickingBlock) {
              // Wait a bit to let the falling block gain some momentum
              relative.delayedShouldTick(1)
            }
          }
        }
      }
    }
  }
}
