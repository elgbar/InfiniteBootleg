package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.api.Resizable
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.util.safeUse

class ScreenRenderer : Disposable, Resizable {
  private val spacing = FONT_SIZE * ClientMain.SCALE / 2
  val font: BitmapFont = Main.inst().assets.font20pt
  val batch: SpriteBatch = SpriteBatch().also {
    it.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
  }

  fun drawTop(text: String, line: Float) {
    try {
      font.draw(batch, text, spacing.toFloat(), Gdx.graphics.height - spacing * line)
    } catch (ignore: ArrayIndexOutOfBoundsException) {
    } catch (ignore: NullPointerException) {
    }
  }

  fun drawBottom(text: String, line: Float) {
    try {
      font.draw(batch, text, spacing.toFloat(), spacing * (line + 1f))
    } catch (ignore: ArrayIndexOutOfBoundsException) {
    } catch (ignore: NullPointerException) {
    }
  }

  fun use(action: () -> Unit) = batch.safeUse { action() }

  fun resetFontColor() {
    font.color = Color.WHITE
  }

  override fun dispose() {
    batch.dispose()
  }

  override fun resize(width: Int, height: Int) {
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
  }

  companion object {
    const val FONT_SIZE = 20
  }
}
