package no.elg.infiniteBootleg

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.IllegalAction
import no.elg.infiniteBootleg.util.asWorldSeed
import no.elg.infiniteBootleg.world.ticker.TickerImpl.Companion.DEFAULT_TICKS_PER_SECOND
import java.awt.GraphicsEnvironment

/**
 * @author Elg
 */
object Settings {

  private const val DEFAULT_PORT = 8558

  /**
   * If worlds should be loaded from disk
   *
   * Use [no.elg.infiniteBootleg.world.world.World.isTransient] to check if the world is transient
   */
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
  var worldSeed: Long = "0".asWorldSeed()

  /** If general debug variable. Use this and-ed with your specific debug variable  */
  @JvmField
  var debug = false

  /**
   * The ticks per seconds to use by default. Changing this will only apply to new instances
   * created.
   */
  @JvmField
  var tps = DEFAULT_TICKS_PER_SECOND

  var dayTicking = false

  @JvmField
  var renderLight = true

  var renderBox2dDebug = false

  var debugEntityLight = false

  var debugBlockLight = false

  var debugEntityMarkerBlocks = false

  var renderChunkBounds = false

  var renderChunkUpdates = false

  var renderBlockLightUpdates = false

  var renderLeafDecay = false

  var renderAirBlocks = false

  var renderTopBlockChanges = false

  var renderClosestBlockToPlayerChunk = false

  /**
   * Make sure entities have unique uuid when added to the engine
   */
  var enableUniquenessEntityIdCheck = true

  /**
   * Log packets between client and server
   */
  var logPackets = true

  /**
   * Log files saved to and loaded from disk
   */
  var logPersistence = false

  var validateEntityFamilies = true

  var handleWrongThreadAsyncEvents: IllegalAction = IllegalAction.STACKTRACE
  var handleChangingBlockInDeposedChunk: IllegalAction = IllegalAction.STACKTRACE

  var enableCameraFollowLerp = true

  var enableEventStatistics = true

  @JvmField
  var port = DEFAULT_PORT

  var host = ""
  var stageDebug = false
  var viewDistance = 6 // ish max chunks when fully zoomed out
  var chunksToRenderEachFrame = 10

  var vsync = true
    set(value) {
      field = value
      Gdx.graphics.setVSync(field)
    }

  /**
   * Default value is max hz of displays reasonably to expect
   */
  var foregroundFPS: Int = 361
    set(value) {
      field = value.coerceAtLeast(0)
      Gdx.graphics.setForegroundFPS(field)
    }

  /**
   * How often the world should be saved in seconds
   */
  var savePeriodSeconds: Float = 60f
    set(value) {
      field = value.coerceAtLeast(1f)
      Main.inst().world?.updateSavePeriod()
    }
}
