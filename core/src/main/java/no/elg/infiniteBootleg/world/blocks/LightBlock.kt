package no.elg.infiniteBootleg.world.blocks

import box2dLight.PointLight
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.traits.LightTrait
import no.elg.infiniteBootleg.world.blocks.traits.LightTrait.Companion.createLight

abstract class LightBlock(
  world: World,
  chunk: Chunk,
  localX: Int,
  localY: Int,
  material: Material
) : TickingBlock(world, chunk, localX, localY, material), LightTrait {

  override var light: PointLight? = null
    get() {
      if (field == null) {
        setShouldTick(true)
      }
      return field
    }
    set(value) {
      if (field != null && value != null) error("Cannot set light before releasing old light")
      field = value
    }

  override fun tick() {
    this.createLight()
  }

  override fun dispose() {
    if (isDisposed()) return
    super<TickingBlock>.dispose()
    super<LightTrait>.dispose()
  }
}
