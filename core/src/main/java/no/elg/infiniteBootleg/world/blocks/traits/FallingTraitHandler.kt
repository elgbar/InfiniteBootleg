package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.subgrid.enitites.FallingBlockEntity

class FallingTraitHandler(
  trait: FallingTrait,
  val world: World,
  private val originWorldX: Int,
  private val originWorldY: Int,
) : TraitHandler<FallingTrait> {

  init {
    trait.handlers.set<FallingTrait>(this)
  }

  var falling: Boolean = false
    private set

  fun tryFall() {
    if (falling || Main.isServerClient()) {
      return
    }
    val blockBelow = Location.relativeCompact(originWorldX, originWorldY, Direction.SOUTH)
    if (world.isAirBlock(blockBelow)) {
      val block = world.getRawBlock(originWorldX, originWorldY) ?: return
      falling = true
      val blockAbove = world.getRawBlock(originWorldX, originWorldY + 1)
      if (blockAbove is TickingTrait) {
        blockAbove.delayedShouldTick(Settings.tps / 5)
      }

      world.worldBody.postBox2dRunnable {
        val fallingBlockEntity = FallingBlockEntity(world, block)
        if (fallingBlockEntity.isDisposed) {
          Main.logger().error("Failed to create falling block entity at $originWorldX, $originWorldY")
          return@postBox2dRunnable
        }
        block.destroy(true)
        world.addEntity(fallingBlockEntity)
      }
    }
  }
}
