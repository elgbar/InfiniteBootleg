package no.elg.infiniteBootleg.screens

import com.badlogic.gdx.Gdx
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.assets.disposeSafely
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.main.ClientMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.screens.hud.DebugWindow
import no.elg.infiniteBootleg.screens.hud.addDebugOverlay
import no.elg.infiniteBootleg.screens.hud.addStaffCreatorOverlay
import no.elg.infiniteBootleg.util.IBVisWindow
import no.elg.infiniteBootleg.world.world.ClientWorld

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class WorldScreen(val world: ClientWorld, val load: Boolean = true) : StageScreen() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  private var worldFinishedLoading = false

  lateinit var debugMenu: DebugWindow
    private set
  lateinit var staffMenu: IBVisWindow
    private set

  override fun render(delta: Float) {
    if (worldFinishedLoading) {
      //noinspection ConstantConditions
      world.render.render()
      hud.render()
      super.render(delta)
    }
    Main.inst().console.draw()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    world.resize(width, height)
    hud.resize(width, height)
  }

  override fun show() {
    super.show()
    require(!worldFinishedLoading) { "Same world screen can not be shown twice!" }
    Gdx.graphics.isContinuousRendering = true

    val showed = System.currentTimeMillis()
    EventManager.oneShotListener { event: WorldLoadedEvent ->
      if (event.world === world) {
        worldFinishedLoading = true
        logger.info { "Finished loading world ${world.name} (${world.uuid}) in ${System.currentTimeMillis() - showed}ms" }
      }
    }

    ClientMain.inst().updateStatus(world)
    if (load) {
      world.initialize()
    }
    Main.inst().console.addToInputMultiplexer()

    ClientMain.inst().inputMultiplexer.addProcessor(world.input)
  }

  override fun hide() {
    super.hide()
    dispose()
  }

  override fun dispose() {
    world.save()
    super.dispose()
    ClientMain.inst().updateStatus(null)
    world.disposeSafely()
  }

  val isDebugMenuVisible: Boolean get() = ::debugMenu.isInitialized && debugMenu.isDebugMenuVisible

  override fun create() {
    super.create()
    staffMenu = addStaffCreatorOverlay(world)
    debugMenu = stage.addDebugOverlay(world, staffMenu)
  }
}
