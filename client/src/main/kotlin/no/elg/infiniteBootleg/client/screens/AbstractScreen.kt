package no.elg.infiniteBootleg.client.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import no.elg.infiniteBootleg.core.util.CheckableDisposable

/** @author Elg */
abstract class AbstractScreen(private val yDown: Boolean = true) :
  ScreenAdapter(),
  CheckableDisposable {

  val batch: SpriteBatch by lazy { SpriteBatch() }
  private val lineRenderer: ShapeRenderer by lazy { ShapeRenderer() }

  override var isDisposed = false
    protected set
  private var hasBeenShown = false

  val camera: OrthographicCamera by lazy {
    OrthographicCamera().apply { setToOrtho(yDown) }
  }

  abstract override fun render(delta: Float)

  abstract fun create()

  fun tryCreate() {
    if (!hasBeenShown) {
      create()
      hasBeenShown = true
    }
  }

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
