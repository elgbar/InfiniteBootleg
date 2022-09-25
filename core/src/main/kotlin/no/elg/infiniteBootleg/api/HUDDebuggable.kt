package no.elg.infiniteBootleg.api

/**
 * @author Elg
 */
interface HUDDebuggable {
  /**
   * @return Information to display on the debug HUD
   */
  fun hudDebug(): String
}
