package no.elg.infiniteBootleg.screens

import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Renderer
import no.elg.infiniteBootleg.api.Resizable
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.hud.CurrentBlockHUDRenderer
import no.elg.infiniteBootleg.screens.hud.DebugGraph
import no.elg.infiniteBootleg.screens.hud.DebugGraph.render
import no.elg.infiniteBootleg.screens.hud.DebugText.chunk
import no.elg.infiniteBootleg.screens.hud.DebugText.counters
import no.elg.infiniteBootleg.screens.hud.DebugText.ents
import no.elg.infiniteBootleg.screens.hud.DebugText.fpsString
import no.elg.infiniteBootleg.screens.hud.DebugText.lights
import no.elg.infiniteBootleg.screens.hud.DebugText.pointing
import no.elg.infiniteBootleg.screens.hud.DebugText.pos
import no.elg.infiniteBootleg.screens.hud.DebugText.time
import no.elg.infiniteBootleg.screens.hud.DebugText.viewChunk

/**
 * @author Elg
 */
class HUDRenderer : Renderer, Resizable {
  private var modus = DISPLAY_CURRENT_BLOCK
  private val builder = StringBuilder()

  init {
    modus = modus or if (Settings.debug) DISPLAY_DEBUG else DISPLAY_CURRENT_BLOCK
  }

  override fun render() {
    if (modus == DISPLAY_NOTHING || Main.isServer) {
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
        CurrentBlockHUDRenderer.render(sr, world)
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

  companion object {
    const val DISPLAY_NOTHING = 0
    const val DISPLAY_CURRENT_BLOCK = 1
    const val DISPLAY_MINIMAL_DEBUG = 2
    const val DISPLAY_DEBUG = 4
    const val DISPLAY_GRAPH_FPS = 8
  }
}
