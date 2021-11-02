package no.elg.infiniteBootleg.world.blocks

import box2dLight.PointLight
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.traits.FallingTrait
import no.elg.infiniteBootleg.world.blocks.traits.FallingTrait.Companion.tryFall
import no.elg.infiniteBootleg.world.blocks.traits.LightTrait.Companion.tryCreateLight

/**
 * A block that lights up the surrounding area
 */
class Torch(world: World, chunk: Chunk, localX: Int, localY: Int, material: Material) : LightBlock(world, chunk, localX, localY, material), FallingTrait {
  override var falling = false

  override fun customizeLight(light: PointLight) {
    light.setColor(244 / 255f, 178 / 255f, 153 / 255f, .75f)
  }

  override fun tick() {
    this.tryCreateLight()
    this.tryFall()
  }
}
