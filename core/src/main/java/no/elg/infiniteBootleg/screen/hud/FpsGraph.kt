package no.elg.infiniteBootleg.screen.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import no.elg.infiniteBootleg.screen.ScreenRenderer
import no.elg.infiniteBootleg.util.Resizable

object FpsGraph : Resizable {

  private lateinit var pixmap: Pixmap
  private lateinit var texture: Texture
  private var width: Int = 0
  private var height: Int = 0
  private var index: Int = 0

  init {
    resize(Gdx.graphics.width, Gdx.graphics.height)
  }

  fun render(sr: ScreenRenderer) {
    newFrame()
    texture.load(texture.textureData)
    sr.batch.draw(texture, 0f, 0f)
  }

  private fun newFrame() {
    if (index >= width) {
      index = 0
    } else {
      index++
    }

    val delta = Gdx.graphics.deltaTime * 10_000
    val pillarHeight = height - delta.toInt()

    pixmap.setColor(Color.CLEAR)
    pixmap.drawLine(index, 0, index, pillarHeight)

    pixmap.setColor(Color(0.1f + (delta / height), 0.9f - (delta / height), 0f, 1f))
    pixmap.drawLine(index, height, index, pillarHeight)
  }

  private fun clear() {
    pixmap.setColor(Color.CLEAR)
    pixmap.fill()
    pixmap.setColor(Color.GREEN)
  }

  private fun buffer(): Pixmap {
    val pixmap = Pixmap(width, height, Pixmap.Format.RGBA4444)
    pixmap.blending = Pixmap.Blending.None
    pixmap.filter = Pixmap.Filter.NearestNeighbour

    return pixmap
  }

  override fun resize(width: Int, height: Int) {
    FpsGraph.width = width
    FpsGraph.height = height / 3
    index = Int.MAX_VALUE

    if (::pixmap.isInitialized) {
      pixmap.dispose()
    }
    if (::texture.isInitialized) {
      texture.dispose()
    }
    pixmap = buffer()
    texture = Texture(pixmap)

    clear()
  }
}
