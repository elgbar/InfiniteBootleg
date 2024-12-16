package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.kotcrab.vis.ui.VisUI
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.input.GlobalInputListener
import no.elg.infiniteBootleg.input.MouseLocator
import no.elg.infiniteBootleg.main.Main.Companion.isAuthoritative
import no.elg.infiniteBootleg.main.Main.Companion.isClient
import no.elg.infiniteBootleg.main.Main.Companion.isServer
import no.elg.infiniteBootleg.screens.AbstractScreen
import no.elg.infiniteBootleg.screens.MainMenuScreen
import no.elg.infiniteBootleg.screens.ScreenRenderer
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.ServerClient
import no.elg.infiniteBootleg.util.FailureWatchdog
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld
import java.awt.Toolkit

private val logger = KotlinLogging.logger {}

class ClientMain(progArgs: ProgramArgs) : CommonMain(progArgs) {
  val inputMultiplexer: InputMultiplexer = InputMultiplexer()

  lateinit var screenRenderer: ScreenRenderer
    private set

  var screen: Screen = ScreenAdapter() // Dummy value
    set(value) {
      field.hide()
      field = value

      // clean up any mess the previous screen have made
      inputMultiplexer.clear()
      inputMultiplexer.addProcessor(GlobalInputListener)
      Gdx.input.setOnscreenKeyboardVisible(false)
      logger.debug { "Loading new screen ${value.javaClass.simpleName}" }
      updateStatus(world)

      (screen as? AbstractScreen)?.tryCreate()
      screen.show()
      screen.resize(Gdx.graphics.width, Gdx.graphics.height)
    }

  /**
   * @return If the player is singleplayer
   */
  var isSingleplayer = false
    private set

  /**
   * @return If the client is connected to a server
   */
  var isMultiplayer = false
    private set

  override fun isAuthorizedToChange(entity: Entity): Boolean = super.isAuthorizedToChange(entity) || entity.id == serverClient?.uuid

  override lateinit var renderThreadName: String
    private set

  val mouseLocator: MouseLocator = MouseLocator()

  /**
   * @return Either the world we're connected to or the singleplayer world, whichever is more
   * correct. If the client is not in a world null will be returned
   */
  override val world: ClientWorld? get() = worldScreen?.world

  private val worldScreen: WorldScreen? get() = screen as? WorldScreen
  val serverClient: ServerClient? get() = (world as? ServerClientWorld)?.serverClient

  private val watchdog = FailureWatchdog("render")

  init {
    check(!isServer) { "Cannot create client main as a server!" }
  }

  override fun create() {
    VisUI.load(if (scale > 1) VisUI.SkinScale.X2 else VisUI.SkinScale.X1)
    // must load VisUI first
    super.create()
    renderThreadName = Thread.currentThread().name
    Gdx.input.inputProcessor = inputMultiplexer
    logger.info {
      """Controls:
  WASD to control the player
  T to teleport player to current mouse pos
  Apostrophe (') to open console (type help for help)"""
    }
    screenRenderer = ScreenRenderer()
    screen = MainMenuScreen

    Runtime.getRuntime().addShutdownHook(
      Thread {
        if (isAuthoritative) {
          world?.save()
          world?.dispose()
        } else if (isClient) {
          serverClient?.dispose()
        }
      }
    )
  }

  override fun resize(rawWidth: Int, rawHeight: Int) {
    val width = rawWidth.coerceAtLeast(1)
    val height = rawHeight.coerceAtLeast(1)
    logger.trace { "Resizing client to $width x $height" }
    screen.resize(width, height)
    console.resize(width, height)
    screenRenderer.resize(width, height)
  }

  override fun render() {
    if (isServer) {
      return
    }
    Gdx.gl.glClearColor(CLEAR_COLOR_R, CLEAR_COLOR_G, CLEAR_COLOR_B, CLEAR_COLOR_A)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    world?.also {
      mouseLocator.update(it)
    }
    watchdog.watch {
      screen.render(Gdx.graphics.deltaTime)
    }
  }

  override fun dispose() {
    super.dispose()
    screen.dispose()
    screenRenderer.dispose()
    VisUI.dispose()
  }

  fun updateStatus(world: ClientWorld?) {
    isSingleplayer = world is SinglePlayerWorld
    isMultiplayer = world is ServerClientWorld
    logger.debug { "World status updated: Multiplayer? $isMultiplayer, Singleplayer? $isSingleplayer" }
  }

  fun shouldNotIgnoreWorldInput(): Boolean = !shouldIgnoreWorldInput()

  fun shouldIgnoreWorldInput(): Boolean {
    val debugMenuVisible = worldScreen?.isDebugMenuVisible ?: false
    val consoleVisible = console.isVisible
    return consoleVisible || debugMenuVisible
  }

  companion object {
    /** Only use this when a server is present  */
    val scale = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1
    const val CLEAR_COLOR_R = 0.2f
    const val CLEAR_COLOR_G = (68.0 / 255.0).toFloat()
    const val CLEAR_COLOR_B = 1f
    const val CLEAR_COLOR_A = 1f

    fun inst(): ClientMain = Main.inst() as? ClientMain ?: throw IllegalStateException("Cannot get client main as a server")
  }
}
