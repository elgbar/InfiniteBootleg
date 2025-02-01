package no.elg.infiniteBootleg.client.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.core.api.Resizable
import no.elg.infiniteBootleg.core.util.safeUse

class ScreenRenderer : Disposable, Resizable {
  val font: BitmapFont = ClientMain.inst().assets.font16pt
  private val spacing = font.lineHeight * ClientMain.scale / 2
  val batch: SpriteBatch = SpriteBatch().also {
    it.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
  }

  fun drawTop(text: String, line: Float) {
    try {
      font.draw(batch, text, spacing, Gdx.graphics.height - spacing * line)
    } catch (ignore: ArrayIndexOutOfBoundsException) {
    } catch (ignore: NullPointerException) {
    }
  }

  fun drawBottom(text: String, line: Float) {
    try {
      font.draw(batch, text, spacing, spacing * (line + 1f))
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
}
