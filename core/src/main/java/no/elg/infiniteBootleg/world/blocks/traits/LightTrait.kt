package no.elg.infiniteBootleg.world.blocks.traits

import box2dLight.PointLight
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.PointLightPool
import no.elg.infiniteBootleg.world.ClientWorld

/**
 * @author Elg
 */
interface LightTrait : BlockTrait, CheckableDisposable {

  var light: PointLight?

  fun customizeLight(light: PointLight) {
    // Default point light
  }

  fun canCreateLight(): Boolean = true

  override fun dispose() {
    releaseLight()
  }

  companion object {
    fun LightTrait.recreateLight() {
      releaseLight()
      createLight()
    }

    fun LightTrait.createLight() {
      if (!Settings.renderLight || Main.isServer()) {
        return
      }
      synchronized(this) {
        if (disposed || light != null) {
          return
        }
        val world = block.world
        if (world is ClientWorld && block is LightTrait && block.chunk.chunkBody.hasBody() && canCreateLight()) {
          releaseLight()
          light = PointLightPool.getPool(world).obtain(block.worldX + 0.5f, block.worldY + 0.5f).also {
            it.isStaticLight = true
            customizeLight(it)
          }
        } else if (this is TickingTrait) {
          setShouldTick(true)
        }
      }
    }

    fun LightTrait.releaseLight() {
      synchronized(this) {
        val pointLight = light
        if (pointLight != null) { // && pointLight.isActive
          PointLightPool.getPool(block.world)?.free(pointLight)
          light = null
        }
      }
    }
  }
}
