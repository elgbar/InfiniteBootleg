package no.elg.infiniteBootleg.core.api

/**
 * @author Elg
 */
interface HUDDebuggable {
  /**
   * @return Information to display on the debug HUD
   */
  fun hudDebug(): String
}
