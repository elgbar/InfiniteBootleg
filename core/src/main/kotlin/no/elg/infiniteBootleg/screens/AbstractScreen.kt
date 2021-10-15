package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/** @author Elg */
abstract class AbstractScreen(val yDown: Boolean = true) : ScreenAdapter() {

  val batch: SpriteBatch by lazy { SpriteBatch() }
  protected val lineRenderer: ShapeRenderer by lazy { ShapeRenderer() }

  var isDisposed = false
    private set

  val camera: OrthographicCamera by lazy {
    OrthographicCamera().apply { setToOrtho(yDown) }
  }

  abstract override fun render(delta: Float)

  fun updateCamera() {
    camera.update()
    batch.projectionMatrix = camera.combined
    lineRenderer.projectionMatrix = camera.combined
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(yDown, width.toFloat(), height.toFloat())
    updateCamera()
  }

  override fun dispose() {
    if (isDisposed) {
      Gdx.app.error("DISPOSE WARN", "The screen ${this::class.simpleName} is already disposed")
      return
    }
    batch.dispose()
    lineRenderer.dispose()
    isDisposed = true
  }
}
