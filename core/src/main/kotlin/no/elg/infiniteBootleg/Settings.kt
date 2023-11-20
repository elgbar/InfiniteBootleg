package no.elg.infiniteBootleg

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.protobuf.Packets
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

  var dayTicking = true

  @JvmField
  var renderLight = true

  var renderBox2dDebug = false

  var debugEntityLight = false

  var debugBlockLight = false

  var debugEntityMarkerBlocks = false

  var renderChunkBounds = false

  var renderChunkUpdates = false

  var renderBlockLightUpdates = false

  var renderAirBlocks = false

  var renderTopBlockChanges = false

  var logPackets = true

  var validateEntityFamilies = true

  /**
   * Never log these packets
   */
  val filterOutPackets = mutableListOf<Packets.Packet.Type>().also {
    it += Packets.Packet.Type.DX_HEARTBEAT
  }

  enum class WrongThreadAsyncEventAction {
    /**
     * Throw an exception when an async event is handled on the wrong thread
     */
    THROW,

    /**
     * Print a stacktrace when an async event is handled on the wrong thread
     */
    STACKTRACE,

    /**
     * Log a warning when an async event is handled on the wrong thread
     */
    LOG
  }

  var handleWrongThreadAsyncEvents: WrongThreadAsyncEventAction = WrongThreadAsyncEventAction.STACKTRACE

  @JvmField
  var enableCameraFollowLerp = true

  @JvmField
  var port = DEFAULT_PORT

  var host = ""
  var stageDebug = false
  var viewDistance = 6 // ish max chunks when fully zoomed out

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
  var savePeriodSeconds: Long = 15
    set(value) {
      field = value.coerceAtLeast(1)
      Main.inst().world?.updateSavePeriod()
    }
}
