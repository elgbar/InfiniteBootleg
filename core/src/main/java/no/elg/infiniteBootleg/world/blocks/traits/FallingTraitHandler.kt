package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.TickingBlock
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlockEntity

class FallingTraitHandler(
  val world: World,
  private val originWorldX: Int,
  private val originWorldY: Int,
) : TraitHandler<FallingTrait> {

  var falling: Boolean = false
    private set

  fun tryFall() {
    if (falling || Main.isServerClient()) {
      return
    }
    val south = Location.relative(originWorldX, originWorldY, Direction.SOUTH)
    if (world.isAirBlock(south)) {
      falling = true

      Main.inst().scheduler.executeAsync {
        val block = world.getBlock(originWorldX, originWorldY) ?: return@executeAsync
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
