package no.elg.infiniteBootleg.world.blocks.traits

import box2dLight.PointLight
import no.elg.infiniteBootleg.CheckableDisposable
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.util.PointLightPool
import no.elg.infiniteBootleg.world.ClientWorld
import no.elg.infiniteBootleg.world.World

class LightTraitHandler(
  trait: LightTrait,
  val world: World,
  private val originWorldX: Int,
  private val originWorldY: Int,
  private val lightTrait: LightTrait
) : TraitHandler<LightTrait>, CheckableDisposable {

  var light: PointLight? = null
  var disposed = false

  init {
    trait.handlers.set<LightTrait>(this)
  }

  fun tryCreateLight(customizer: (light: PointLight) -> Unit = {}) {
    if (!Settings.renderLight || Main.isServer()) {
      return
    }
    synchronized(this) {
      if (isDisposed || light != null) {
        return
      }
      if (world is ClientWorld && world.getChunkFromWorld(originWorldX, originWorldY)?.isValid != false && lightTrait.canCreateLight()) {
        releaseLight()
        light = PointLightPool.getPool(world).obtain(originWorldX + 0.5f, originWorldY + 0.5f).also {
          it.isStaticLight = true
          lightTrait.customizeLight(it)
          customizer(it)
        }
      }
    }
  }

  fun releaseLight() {
    synchronized(this) {
      val pointLight = light
      if (pointLight != null) { // && pointLight.isActive
        PointLightPool.getPool(world)?.free(pointLight)
        light = null
      }
    }
  }

  fun recreateLight(customizer: (light: PointLight) -> Unit) {
    releaseLight()
    tryCreateLight(customizer)
  }

  override val isDisposed: Boolean
    get() = disposed

  override fun dispose() {
    disposed = true
    releaseLight()
  }
}
