package no.elg.infiniteBootleg

import no.elg.infiniteBootleg.world.ticker.TickerImpl.Companion.DEFAULT_TICKS_PER_SECOND
import java.awt.GraphicsEnvironment

/**
 * @author Elg
 */
object Settings {

  private const val DEFAULT_PORT = 8558

  /** If worlds should be loaded from disk  */
  @JvmField
  var loadWorldFromDisk = true

  /** Ignore the world lock  */
  @JvmField
  var ignoreWorldLock = false

  /**
   * If graphics should be rendered, false implies this should be a server
   *
   *
   * If [GraphicsEnvironment.isHeadless] is `false` this will always be `false`.
   */
  @JvmField
  var client = !GraphicsEnvironment.isHeadless()

  /** Seed of the world loaded  */
  @JvmField
  var worldSeed = 0

  /** If general debug variable. Use this and-ed with your specific debug variable  */
  @JvmField
  var debug = false

  @JvmField
  var schedulerThreads = -1

  /**
   * The ticks per seconds to use by default. Changing this will only apply to new instances
   * created.
   */
  @JvmField
  var tps = DEFAULT_TICKS_PER_SECOND

  @JvmField
  var dayTicking = true

  @JvmField
  var renderLight = true

  @JvmField
  var renderBox2dDebug = false

  @JvmField
  var debugEntityLight = false

  @JvmField
  var debugBlockLight = false

  var debugEntityMarkerBlocks = false

  @JvmField
  var renderChunkBounds = false

  var renderChunkUpdates = false

  @JvmField
  var enableCameraFollowLerp = true

  @JvmField
  var port = DEFAULT_PORT

  var host = ""
  var stageDebug = false
  var viewDistance = 4 // ish max chunks when fully zoomed out
}
