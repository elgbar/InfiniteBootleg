package no.elg.infiniteBootleg.core

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.util.IllegalAction
import no.elg.infiniteBootleg.core.util.asWorldSeed
import no.elg.infiniteBootleg.core.world.ticker.TickerImpl
import java.awt.GraphicsEnvironment

/**
 * @author Elg
 */
object Settings {

  private const val DEFAULT_PORT = 8558

  /**
   * If worlds should be loaded from disk
   *
   * Use [no.elg.infiniteBootleg.core.world.world.World.isTransient] to check if the world is transient
   */
  var loadWorldFromDisk = true

  /** Ignore the world lock  */
  var ignoreWorldLock = false

  /**
   * If graphics should be rendered, false implies this should be a server
   *
   *
   * If [GraphicsEnvironment.isHeadless] is `false` this will always be `false`.
   */
  var client = !GraphicsEnvironment.isHeadless()

  /** Seed of the world loaded  */
  var worldSeed: Long = "0".asWorldSeed()

  /** If general debug variable. Use this and-ed with your specific debug variable  */
  var debug = false

  /**
   * The ticks per seconds to use by default. Changing this will only apply to new instances
   * created.
   */
  var tps = TickerImpl.DEFAULT_TICKS_PER_SECOND

  var dayTicking = false

  var renderLight = true
  var lightToneMapping: LightToneMapping = REINHARD_JODIE_LUMINANCE_BY_COLOR

  enum class LightToneMapping {
    REINHARD,
    REINHARD_JODIE_LUMINANCE_BY_INTENSITY,
    REINHARD_JODIE_LUMINANCE_BY_COLOR,
    CLAMP
  }

  var lightIntensityMapping: LightIntensityMapping = LightIntensityMapping.SMOOTH_FALLOFF_LINEAR_SPACE

  enum class LightIntensityMapping {
    LINEAR,
    SMOOTH_FALLOFF_LINEAR_SPACE,
    SMOOTH_FALLOFF_SQUARED_SPACE
  }

  var renderBox2dDebug = false

  var debugEntityLight = false

  var debugBlockLight = false

  var debugEntityMarkerBlocks = false

  var renderChunkBounds = false

  var renderChunkUpdates = false

  var renderBlockLightUpdates = false

  var renderLeafDecay = false

  var renderInvisibleBlocks = false

  var renderTopBlockChanges = false

  var renderTopBlocks = false

  var renderClosestBlockToPlayerChunk = false

  var renderEntityPosDifference = false

  var renderGroundedTouchingBlocks = false

  var assertThreadType = true

  /**
   * Make sure entities have unique uuid when added to the engine
   *
   * Not changeable after the engine is created
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
  var handleInvalidBox2dRef: IllegalAction = IllegalAction.STACKTRACE

  var enableCameraFollowLerp = true

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
