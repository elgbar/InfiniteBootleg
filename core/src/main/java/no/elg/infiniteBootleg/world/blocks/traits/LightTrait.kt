package no.elg.infiniteBootleg.world.blocks.traits

import box2dLight.PointLight
import no.elg.infiniteBootleg.CheckableDisposable

/**
 * @author Elg
 */
interface LightTrait : Trait, CheckableDisposable {

  val light: PointLight?

  fun customizeLight(light: PointLight) {
    // Default point light
  }

  fun canCreateLight(): Boolean = true

  fun createLight(customizer: (light: PointLight) -> Unit = {})
  fun releaseLight()
  fun recreateLight()
}
