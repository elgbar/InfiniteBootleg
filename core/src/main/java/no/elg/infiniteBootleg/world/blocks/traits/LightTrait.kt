package no.elg.infiniteBootleg.world.blocks.traits

import box2dLight.PointLight
import no.elg.infiniteBootleg.CheckableDisposable

/**
 * @author Elg
 */
interface LightTrait : Trait, CheckableDisposable {

  /**
   * The current light
   */
  val light: PointLight?

  /**
   * Default customization to do when creating the light
   */
  fun customizeLight(light: PointLight) {
    // Default point light
  }

  fun canCreateLight(): Boolean = true

  /**
   * Try to create light if it has not been created yet
   *
   * @param customizer Additional customization of the light
   */
  fun tryCreateLight(customizer: (light: PointLight) -> Unit = {})

  /**
   * Release the light back to the point light pool
   */
  fun releaseLight()

  /**
   * Release, then create the light again.
   */
  fun recreateLight(customizer: (light: PointLight) -> Unit = {})
}
