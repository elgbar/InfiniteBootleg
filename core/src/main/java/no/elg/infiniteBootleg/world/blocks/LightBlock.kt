package no.elg.infiniteBootleg.world.blocks

import box2dLight.PointLight
import no.elg.infiniteBootleg.util.CoordUtil
import no.elg.infiniteBootleg.world.Chunk
import no.elg.infiniteBootleg.world.Material
import no.elg.infiniteBootleg.world.World
import no.elg.infiniteBootleg.world.blocks.traits.LightTrait
import no.elg.infiniteBootleg.world.blocks.traits.LightTraitHandler

abstract class LightBlock(
  world: World,
  chunk: Chunk,
  localX: Int,
  localY: Int,
  material: Material
) : TickingBlock(world, chunk, localX, localY, material), LightTrait {

  private val lightTraitHandler = LightTraitHandler(
    world,
    CoordUtil.chunkToWorld(chunk.chunkX, localX),
    CoordUtil.chunkToWorld(chunk.chunkY, localY),
    this
  )

  override val light: PointLight? get() = lightTraitHandler.light

  override fun tick() {
    lightTraitHandler.tryCreateLight()
  }

  override fun tryCreateLight(customizer: (light: PointLight) -> Unit) {
    lightTraitHandler.tryCreateLight(customizer)
  }

  override fun releaseLight() {
    lightTraitHandler.releaseLight()
  }

  override fun recreateLight(customizer: (light: PointLight) -> Unit) {
    lightTraitHandler.recreateLight(customizer)
  }

  override fun dispose() {
    if (isDisposed) return
    super.dispose()
    lightTraitHandler.dispose()
  }
}
