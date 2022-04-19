package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.util.Resizable
import no.elg.infiniteBootleg.world.ClientWorld

object DebugGraph : Resizable {

  private lateinit var fbo: FrameBuffer

  private var fboWidth: Int = 0
  private var fboHeight: Int = 0

  private var fpsIndex: Int = 0
  private var tpsIndex: Int = 0

  private const val COL_WIDTH = 2
  private const val COLOR_SIZE = 6

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
  private var tpsDeltaAcc = 0f
  private var lastTickId = -1L

  fun render(sr: ScreenRenderer, world: ClientWorld?) {
    val worldTicker = world?.worldTicker

    fpsDeltaAcc += Gdx.graphics.deltaTime
    tpsDeltaAcc += (worldTicker?.tpsDelta?.toFloat() ?: 0f)
    val updateFps = Gdx.graphics.frameId % COL_WIDTH == 0L
    val currTick = (worldTicker?.tickId ?: lastTickId)
    val updateTps = lastTickId < currTick

    if (updateFps || updateTps) {
      begin()
    }

    if (updateFps) {
      drawFps()
    }

    if (updateTps) {
      drawTps(currTick)
    }

    if (updateFps || updateTps) {
      end()
    }

    sr.batch.draw(fbo.colorBufferTexture, 0f, 0f)
  }

  private inline fun begin() {
    fbo.begin()
    // https://www.khronos.org/opengl/wiki/Scissor_Test
    Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
  }

  private inline fun end() {
    Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    fbo.end()
  }

  private inline fun drawFps() {
    if (fpsIndex >= fboWidth / 3) {
      fpsIndex = 0
    } else {
      fpsIndex += COL_WIDTH
    }

    val fpsDelta = (fpsDeltaAcc / COL_WIDTH) * 10_000f
    fpsDeltaAcc = 0f

    drawColumn(fpsDelta, fpsIndex)
  }

  private inline fun drawTps(currTick: Long) {
    if (tpsIndex >= fboWidth) {
      tpsIndex = (fboWidth / 3) * 2
    } else {
      tpsIndex += COL_WIDTH
    }

    val ticks = currTick - lastTickId
    lastTickId = currTick
    val tpsDelta = (tpsDeltaAcc / ticks) / 1_000_000f
    tpsDeltaAcc = 0f
    drawColumn(tpsDelta, tpsIndex)
  }

  private inline fun drawColumn(height: Float, index: Int) {
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
    fboHeight = height / 3
    fpsIndex = Int.MAX_VALUE
    tpsIndex = Int.MAX_VALUE

    if (::fbo.isInitialized) {
      fbo.dispose()
    }
    fbo = FrameBuffer(Pixmap.Format.RGBA4444, fboWidth, fboHeight, false)
  }
}
