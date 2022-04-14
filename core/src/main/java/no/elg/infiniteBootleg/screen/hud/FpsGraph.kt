package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.util.Resizable

object FpsGraph : Resizable {

  private lateinit var fbo: FrameBuffer

  private var width: Int = 0
  private var height: Int = 0
  private var index: Int = 0
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

  fun render(sr: ScreenRenderer) {
    newFrame()
    sr.batch.draw(fbo.colorBufferTexture, 0f, 0f)
  }

  private fun newFrame() {
    if (index >= width) {
      index = 0
    } else {
      index += COL_WIDTH
    }

    val delta = Gdx.graphics.deltaTime * 10_000
    val pillarHeight = (height - delta.toInt()).coerceAtLeast(0)

    val colorIndex = (delta / height * COLOR_SIZE.toFloat()).toInt()
    val color = colors.getOrNull(colorIndex) ?: Color.RED

    fbo.begin()

    // https://www.khronos.org/opengl/wiki/Scissor_Test
    Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)

    // 0 is top, height is bottom, pillarHeight is how far up the pillar should go
    // Fill from the top to pillarHeight with transparent pixels
    Gdx.gl.glScissor(index, 0, COL_WIDTH + 1, height)
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    // Then fill the rest (from pillarHeight to bottom) with colored pixels
    Gdx.gl.glScissor(index, pillarHeight, COL_WIDTH, height)
    Gdx.gl.glClearColor(color.r, color.g, color.b, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)

    fbo.end()
  }

  override fun resize(width: Int, height: Int) {
    FpsGraph.width = width
    FpsGraph.height = height / 3
    index = Int.MAX_VALUE

    if (::fbo.isInitialized) {
      fbo.dispose()
    }
    fbo = FrameBuffer(Pixmap.Format.RGBA4444, FpsGraph.width, FpsGraph.height, false)
  }
}
