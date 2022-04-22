package no.elg.infiniteBootleg.world.blocks.traits

import ktx.collections.GdxSet
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.world.Block
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.World
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
    Main.inst().scheduler.executeAsync {
      val south = Location.relativeCompact(originWorldX, originWorldY, Direction.SOUTH)
      if (world.isAirBlock(south)) {
        falling = true

        var dy = 0
        val blocks = GdxSet<FallingTrait>()

        while (true) {
          val block = world.getBlock(originWorldX, originWorldY + dy) ?: break
          dy++
          if (block is FallingTrait) {
            blocks.add(block)
          } else {
            break
          }
        }

        world.removeBlocks(blocks, true)

        for (block in blocks) {
          block.createEntityFromBlock()
        }
      }
    }
  }

  fun createEntityFromBlock(block: Block): FallingBlockEntity? {
    // Do not update world straight away as if there are sand blocks above this it will begin to fall on the same tick
    val fallingBlockEntity = FallingBlockEntity(world, block)
    if (fallingBlockEntity.isDisposed) {
      falling = false // try again later
      return null
    }
    world.addEntity(fallingBlockEntity)
    return fallingBlockEntity
  }
}
