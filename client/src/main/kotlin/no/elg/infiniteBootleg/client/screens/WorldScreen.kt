package no.elg.infiniteBootleg.client.screens

import com.badlogic.gdx.Gdx
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.assets.dispose
import no.elg.infiniteBootleg.client.main.ClientMain
import no.elg.infiniteBootleg.client.screens.hud.DEBUG_MENU_ID
import no.elg.infiniteBootleg.client.screens.hud.STAFF_CREATOR_ID
import no.elg.infiniteBootleg.client.world.world.ClientWorld
import no.elg.infiniteBootleg.core.events.WorldLoadedEvent
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.main.Main

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class WorldScreen(val world: ClientWorld, val load: Boolean = true) : StageScreen() {

  var hud: HUDRenderer = HUDRenderer()
    private set

  private var worldFinishedLoading = false

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
        world.recalculateLights()
        worldFinishedLoading = true
        logger.info { "Finished loading $world in ${System.currentTimeMillis() - showed}ms" }
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
    super.dispose()
    ClientMain.inst().updateStatus(null)
    world.dispose { it.printStackTrace() }
    hud.dispose()
  }

  val isAnyDebugMenuVisible: Boolean
    get() {
      val interfaceManager = world.render.interfaceManager
      return interfaceManager.isOpen(DEBUG_MENU_ID) || interfaceManager.isOpen(STAFF_CREATOR_ID)
    }
}
