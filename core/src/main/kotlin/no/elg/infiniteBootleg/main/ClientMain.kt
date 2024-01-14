package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.kotcrab.vis.ui.VisUI
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.input.GlobalInputListener
import no.elg.infiniteBootleg.input.MouseLocator
import no.elg.infiniteBootleg.main.Main.Companion.isAuthoritative
import no.elg.infiniteBootleg.main.Main.Companion.isClient
import no.elg.infiniteBootleg.main.Main.Companion.isServer
import no.elg.infiniteBootleg.main.Main.Companion.logger
import no.elg.infiniteBootleg.screens.AbstractScreen
import no.elg.infiniteBootleg.screens.MainMenuScreen
import no.elg.infiniteBootleg.screens.ScreenRenderer
import no.elg.infiniteBootleg.screens.WorldScreen
import no.elg.infiniteBootleg.server.ServerClient
import no.elg.infiniteBootleg.server.serverBoundClientDisconnectPacket
import no.elg.infiniteBootleg.world.ecs.components.required.IdComponent.Companion.id
import no.elg.infiniteBootleg.world.world.ClientWorld
import no.elg.infiniteBootleg.world.world.ServerClientWorld
import no.elg.infiniteBootleg.world.world.SinglePlayerWorld
import java.awt.Toolkit

class ClientMain(test: Boolean, progArgs: ProgramArgs?) : CommonMain(test, progArgs) {
  val inputMultiplexer: InputMultiplexer = InputMultiplexer()

  private var renderFailuresInARow = 0

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
      Gdx.app.debug("SCREEN", "Loading new screen ${value.javaClass.simpleName}")
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

  var mouseLocator: MouseLocator = MouseLocator()
    private set

  /**
   * @return Either the world we're connected to or the singleplayer world, whichever is more
   * correct. If the client is not in a world null will be returned
   */
  override val world: ClientWorld? get() = worldScreen?.world

  private val worldScreen: WorldScreen? get() = screen as? WorldScreen
  val serverClient: ServerClient? get() = (world as? ServerClientWorld)?.serverClient

  init {
    check(!isServer) { "Cannot create client main as a server!" }
    synchronized(Main.INST_LOCK) {
      check(Companion::instField.isLateinit) { "A client main instance have already be declared" }
      instField = this
    }
  }

  override fun create() {
    VisUI.load(if (SCALE > 1) VisUI.SkinScale.X2 else VisUI.SkinScale.X1)
    // must load VisUI first
    super.create()
    renderThreadName = Thread.currentThread().name
    Gdx.input.inputProcessor = inputMultiplexer
    console.log(
      """Controls:
  WASD to control the camera
  arrow-keys to control the player
  T to teleport player to current mouse pos
  Apostrophe (') to open console (type help for help)"""
    )
    screenRenderer = ScreenRenderer()
    screen = MainMenuScreen

    Runtime.getRuntime().addShutdownHook(
      Thread {
        if (isAuthoritative) {
          world?.save()
          world?.dispose()
        } else if (isClient) {
          serverClient?.apply {
            ctx.writeAndFlush(serverBoundClientDisconnectPacket("Client shutdown"))
          }
        }
        scheduler.shutdown()
      }
    )
  }

  override fun resize(rawWidth: Int, rawHeight: Int) {
    val width = rawWidth.coerceAtLeast(1)
    val height = rawHeight.coerceAtLeast(1)
    logger().log("Resizing client to $width x $height")
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
    try {
      screen.render(Gdx.graphics.deltaTime)
      renderFailuresInARow = 0
    } catch (e: Exception) {
      renderFailuresInARow++
      if (renderFailuresInARow >= MAX_RENDER_FAILURES_IN_A_ROW) {
        throw RuntimeException("Render failed $MAX_RENDER_FAILURES_IN_A_ROW times in a row", e)
      } else {
        logger().warn("Render failed (failed render attempt $renderFailuresInARow)", e)
      }
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
    logger().debug("STATUS", "World status updated: Multiplayer? $isMultiplayer, Singleplayer? $isSingleplayer")
  }

  fun shouldNotIgnoreWorldInput(): Boolean = !shouldIgnoreWorldInput()

  fun shouldIgnoreWorldInput(): Boolean {
    val debugMenuVisible = worldScreen?.isDebugMenuVisible ?: false
    val consoleVisible = console.isVisible
    return consoleVisible || debugMenuVisible
  }

  companion object {
    /** Only use this when a server is present  */
    val SCALE = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1
    const val CLEAR_COLOR_R = 0.2f
    const val CLEAR_COLOR_G = (68.0 / 255.0).toFloat()
    const val CLEAR_COLOR_B = 1f
    const val CLEAR_COLOR_A = 1f

    private const val MAX_RENDER_FAILURES_IN_A_ROW = 5

    private lateinit var instField: ClientMain
    fun inst(): ClientMain {
      if (Settings.client) {
        return instField
      }
      throw IllegalStateException("Cannot get client main as a server")
    }
  }
}
