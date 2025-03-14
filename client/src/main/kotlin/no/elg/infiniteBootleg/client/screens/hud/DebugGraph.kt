package no.elg.infiniteBootleg.client.screens.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import ktx.graphics.use
import no.elg.infiniteBootleg.client.screens.ScreenRenderer
import no.elg.infiniteBootleg.client.screens.hud.helper.TickerGraph
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.api.Resizable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("NOTHING_TO_INLINE")
object DebugGraph : Resizable {

  private lateinit var fbo: FrameBuffer

  var fboWidth: Int = 0
    private set
  var fboHeight: Int = 0
    private set

  private var fpsIndex: Int = 0 // frames per second

  const val COL_WIDTH = 2
  const val COLOR_SIZE = 6

  private val colors = Array<Color>(COLOR_SIZE) {
    when (it) {
      0 -> Color.GREEN
      1 -> Color.LIME
      2 -> Color.YELLOW
      3 -> Color.GOLD
      4 -> Color.ORANGE
      5 -> Color.RED
      else -> Color.BLUE
    }
  }

  private var fpsDeltaAcc = 0f

  private var tps: TickerGraph? = null
  private var pps: TickerGraph? = null

  fun render(sr: ScreenRenderer, world: ClientWorld?) {
    val worldTicker = world?.worldTicker
    if (worldTicker != null && worldTicker != tps?.ticker) {
      tps = TickerGraph(worldTicker, { width -> (width * 0.76).toInt() }, { it })
      pps = TickerGraph(worldTicker.box2DTicker.ticker, { width -> (width * 0.26).toInt() }, { width -> (width * 0.50).toInt() })
    }

    fpsDeltaAcc += Gdx.graphics.deltaTime
    val updateFps = Gdx.graphics.frameId % COL_WIDTH == 0L

    val updateTps = tps?.update ?: false
    val updatePps = pps?.update ?: false

    if (updateFps || updateTps || updatePps) {
      use {
        if (updateFps) {
          drawFps()
        }
        tps?.draw()
        pps?.draw()
      }
    }
    sr.batch.draw(fbo.colorBufferTexture, 0f, 0f)
  }

  @OptIn(ExperimentalContracts::class)
  private fun use(block: (FrameBuffer) -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    fbo.use {
      // https://www.khronos.org/opengl/wiki/Scissor_Test
      Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
      try {
        block(it)
      } finally {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
      }
    }
  }

  private inline fun drawFps() {
    if (fpsIndex >= fboWidth / 4) {
      fpsIndex = 0
    } else {
      fpsIndex += COL_WIDTH
    }

    val fpsDelta = (fpsDeltaAcc / COL_WIDTH) * 5_000f
    fpsDeltaAcc = 0f

    drawColumn(fpsDelta, fpsIndex)
  }

  fun drawColumn(height: Float, index: Int) {
    // inverse height
    val pillarHeight = (fboHeight - height.toInt()).coerceAtLeast(0)

    val colorIndex = (height / fboHeight * COLOR_SIZE.toFloat()).toInt()
    val color = colors.getOrNull(colorIndex) ?: Color.RED

    // 0 is top, height is bottom, pillarHeight is how far up the pillar should go
    // Fill from the top to pillarHeight with transparent pixels
    Gdx.gl.glScissor(index, 0, COL_WIDTH * 2, fboHeight)
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    // Then fill the rest (from pillarHeight to bottom) with colored pixels
    Gdx.gl.glScissor(index, pillarHeight, COL_WIDTH, fboHeight)
    Gdx.gl.glClearColor(color.r, color.g, color.b, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
  }

  override fun resize(width: Int, height: Int) {
    fboWidth = width
    fboHeight = (height / 3).coerceAtLeast(1)
    fpsIndex = Int.MAX_VALUE

    tps?.resize(fboWidth, fboHeight)
    pps?.resize(fboWidth, fboHeight)

    if (DebugGraph::fbo.isInitialized) {
      fbo.dispose()
    }
    fbo = FrameBuffer(Pixmap.Format.RGBA4444, fboWidth, fboHeight, false)
  }
}
