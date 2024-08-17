package no.elg.infiniteBootleg.api.render

import no.elg.infiniteBootleg.api.Renderer

/**
 * A renderer that can be overlayed on top of the world
 */
interface OverlayRenderer : Renderer {

  /**
   * If this renderer should be rendered
   */
  val isActive: Boolean

  companion object {
    /**
     * If a renderer should be rendered
     */
    val Renderer.isInactive: Boolean get() = this is OverlayRenderer && !isActive
  }
}
