package no.elg.infiniteBootleg.world.blocks.traits

import box2dLight.PointLight
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.PointLightPool

/**
 * @author Elg
 */
interface LightTrait : BlockTrait, Disposable {

  var light: PointLight?

  fun customizeLight(light: PointLight) {
    //Default point light
  }

  fun canCreateLight(): Boolean = true

  override fun dispose() {
    if (light != null) {
      PointLightPool.getPool(block.world).free(light)
    }
  }

  companion object {

    fun LightTrait.createLight() {
      if (Settings.renderLight && light == null && block.chunk.chunkBody.hasBody() && canCreateLight()) {
        light = PointLightPool.getPool(block.world).obtain(block.worldX + 0.5f, block.worldY + 0.5f).also {
          it.isStaticLight = true
          customizeLight(it)
        }
      } else if (this is TickingTrait) {
        setShouldTick(true)
      }
    }

    fun LightTrait.tryCreateLight() {
      if (light == null) {
        createLight()
      }
    }
  }

}
