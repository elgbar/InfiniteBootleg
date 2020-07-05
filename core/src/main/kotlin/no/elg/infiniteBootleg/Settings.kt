package no.elg.infiniteBootleg

import no.elg.infiniteBootleg.util.Ticker

/**
 * @author Elg
 */
object Settings {
  /**
   * If worlds should be loaded from disk
   */
  @JvmField
  var loadWorldFromDisk = true

  /**
   * If graphics should be rendered
   */
  @JvmField
  var renderGraphic = true

  /**
   * Seed of the world loaded
   */
  @JvmField
  var worldSeed = 0

  /**
   * If general debug variable. Use this and-ed with your specific debug variable
   */
  @JvmField
  var debug = false

  @JvmField
  var schedulerThreads = -1

  @JvmField
  var tps = Ticker.DEFAULT_TICKS_PER_SECOND
}
