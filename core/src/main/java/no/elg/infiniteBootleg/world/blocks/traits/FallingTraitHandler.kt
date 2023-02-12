package no.elg.infiniteBootleg.world.blocks.traits

import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.world.Direction
import no.elg.infiniteBootleg.world.Location
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.ecs.createFallingBlockEntity

class FallingTraitHandler(
  trait: FallingTrait,
  val world: World,
  private val originWorldX: Int,
  private val originWorldY: Int
) : TraitHandler<FallingTrait> {

  init {
    trait.handlers.set(this)
  }

  var falling: Boolean = false
    private set

  fun tryFall() {
    if (falling || Main.isServerClient()) {
      return
    }
    val blockBelow = Location.relativeCompact(originWorldX, originWorldY, Direction.SOUTH)
    if (world.isAirBlock(blockBelow)) {
      val block = world.getRawBlock(originWorldX, originWorldY, true) ?: return
      falling = true
      val blockAbove = world.getRawBlock(originWorldX, originWorldY + 1, true)
      if (blockAbove is TickingTrait) {
        blockAbove.delayedShouldTick(Settings.tps / 10)
      }

      world.postBox2dRunnable {
        val material = block.material
        block.destroy(true)
        world.engine.createFallingBlockEntity(world, block.worldX + 0.5f, block.worldY + 0.5f, 0f, 0f, material)
      }
    }
  }
}
