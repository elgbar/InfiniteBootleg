package no.elg.infiniteBootleg.client.screens

import com.badlogic.gdx.utils.Disposable
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.hud.ContainerChangeRenderer
import no.elg.infiniteBootleg.client.screens.hud.DebugGraph
import no.elg.infiniteBootleg.client.screens.hud.DebugGraph.render
import no.elg.infiniteBootleg.client.screens.hud.DebugText.chunk
import no.elg.infiniteBootleg.client.screens.hud.DebugText.counters
import no.elg.infiniteBootleg.client.screens.hud.DebugText.ents
import no.elg.infiniteBootleg.client.screens.hud.DebugText.fpsString
import no.elg.infiniteBootleg.client.screens.hud.DebugText.lights
import no.elg.infiniteBootleg.client.screens.hud.DebugText.pointing
import no.elg.infiniteBootleg.client.screens.hud.DebugText.pos
import no.elg.infiniteBootleg.client.screens.hud.DebugText.time
import no.elg.infiniteBootleg.client.screens.hud.DebugText.viewChunk
import no.elg.infiniteBootleg.client.screens.hud.HeldBlockRenderer
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.api.Renderer
import no.elg.infiniteBootleg.core.api.Resizable

/**
 * @author Elg
 */
class HUDRenderer :
  Renderer,
  Resizable,
  Disposable {
  private var modus = DISPLAY_CURRENT_BLOCK or DISPLAY_CONTAINER_CHANGE
  private val builder = StringBuilder()
  private val containerChangeRenderer = ContainerChangeRenderer()

  init {
    modus = modus or if (Settings.debug) DISPLAY_DEBUG else DISPLAY_NOTHING
  }

  override fun render() {
    if (modus == DISPLAY_NOTHING) {
      return
    }
    val main = ClientMain.inst()
    val world = main.world
    val sr = ClientMain.inst().screenRenderer
    reset()
    sr.use {
      if (hasMode(DISPLAY_MINIMAL_DEBUG or DISPLAY_DEBUG)) {
        fpsString(builder, world)
      }
      if (hasMode(DISPLAY_DEBUG) && world != null) {
        val mouseBlockX = main.mouseLocator.mouseBlockX
        val mouseBlockY = main.mouseLocator.mouseBlockY
        val controlled = world.controlledPlayerEntities
        builder.appendLine()
        pointing(builder, world, mouseBlockX, mouseBlockY)
        builder.appendLine()
        chunk(builder, world, mouseBlockX, mouseBlockY)
        builder.appendLine()
        viewChunk(builder, world)
        builder.appendLine()
        pos(builder, controlled)
        builder.appendLine()
        lights(builder, world, mouseBlockX, mouseBlockY)
        builder.appendLine()
        time(builder, world)
        builder.appendLine()
        counters(builder, world)
        builder.appendLine()
        ents(builder, world, main.mouseLocator.mouseBlockX, main.mouseLocator.mouseBlockY)
      }
      if (builder.isNotEmpty()) {
        sr.drawTop(builder.toString(), 1f)
      }
      if (hasMode(DISPLAY_CURRENT_BLOCK) && world != null) {
        HeldBlockRenderer.render(sr, world)
      }
      if (hasMode(DISPLAY_CONTAINER_CHANGE) && world != null) {
        containerChangeRenderer.render()
      }
      if (hasMode(DISPLAY_GRAPH_FPS)) {
        render(sr, world)
      }
    }
  }

  private fun reset() = builder.setLength(0)

  fun hasMode(mode: Int): Boolean = modus and mode > 0

  fun displayNothing() {
    modus = DISPLAY_NOTHING
  }

  fun enableMode(mode: Int) {
    modus = modus or mode
  }

  fun disableMode(mode: Int) {
    modus = modus and mode.inv()
  }

  fun toggleMode(mode: Int) {
    modus = modus xor mode
  }

  override fun resize(width: Int, height: Int) {
    DebugGraph.resize(width, height)
  }

  override fun dispose() {
    containerChangeRenderer.dispose()
  }

  companion object {
    const val DISPLAY_NOTHING = 0
    const val DISPLAY_CURRENT_BLOCK = 0b0001
    const val DISPLAY_MINIMAL_DEBUG = 0b0010
    const val DISPLAY_DEBUG = 0b0100
    const val DISPLAY_GRAPH_FPS = 0b1000
    const val DISPLAY_CONTAINER_CHANGE = 0b0001_0000
  }
}
