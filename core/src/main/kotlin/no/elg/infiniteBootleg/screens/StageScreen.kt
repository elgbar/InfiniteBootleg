package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Scaling.fit
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.kotcrab.vis.ui.widget.VisWindow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actors
import ktx.scene2d.vis.KVisTable
import ktx.scene2d.vis.visTable
import no.elg.infiniteBootleg.ClientMain
import no.elg.infiniteBootleg.Main
import no.elg.infiniteBootleg.Settings

/** @author Elg */
open class StageScreen(val useRootTable: Boolean = true) : AbstractScreen(false) {

  private val stage by lazy { Stage(ScalingViewport(fit, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera)) }
  lateinit var rootTable: KVisTable
    private set

  @Scene2dDsl
  @OptIn(ExperimentalContracts::class)
  inline fun rootTable(init: (@Scene2dDsl KVisTable).() -> Unit) {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    require(useRootTable) { "Root table not enabled" }
    rootTable.init()
  }

  init {
    if (Main.inst().isNotTest) {
      if (Settings.stageDebug) {
        stage.isDebugAll = true
      }
      if (useRootTable) {
        stage.actors {
          rootTable = visTable {
            defaults().pad(20f)
            defaults().space(20f)
            setFillParent(true)
            setRound(false)
          }
        }
      }
    }
  }

  override fun render(delta: Float) {
    with(stage.viewport) {
      HdpiUtils.glViewport(screenX, screenY, screenWidth, screenHeight)
    }
    stage.act()
    stage.draw()
  }

  override fun show() {
    ClientMain.inst().inputMultiplexer.addProcessor(stage)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    stage.viewport.update(width, height, true)
    updateCamera()

    for (actor in stage.actors) {
      if (actor is VisWindow) {
        actor.centerWindow()
      }
    }
  }

  override fun dispose() {
    super.dispose()
    stage.dispose()
  }
}