package no.elg.infiniteBootleg.world.blocks.traits

import box2dLight.PointLight
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.PointLightPool
import no.elg.infiniteBootleg.world.ClientWorld

/**
 * @author Elg
 */
interface LightTrait : BlockTrait, Disposable {

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
      if (light != null && Settings.renderLight && Settings.client && light == null && block.chunk.chunkBody.hasBody() && canCreateLight()) {
        return
      }
      val world = block.world
      if (world is ClientWorld && block is LightTrait) {
        releaseLight()
        light = PointLightPool.getPool(world).obtain(block.worldX + 0.5f, block.worldY + 0.5f).also {
          it.isStaticLight = true
          customizeLight(it)
        }
      } else if (this is TickingTrait) {
        setShouldTick(true)
      }
    }

    fun LightTrait.releaseLight() {
      val pointLight = light
      if (pointLight != null) {
        synchronized(this) {
          PointLightPool.getPool(block.world)?.free(pointLight)
          light = null
        }
      }
    }
  }
}
