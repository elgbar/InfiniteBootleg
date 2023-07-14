package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.ApplicationListener
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.console.ConsoleHandler
import no.elg.infiniteBootleg.console.ConsoleLogger
import no.elg.infiniteBootleg.util.CancellableThreadScheduler
import no.elg.infiniteBootleg.world.world.World
import java.io.File

/**
 * @author Elg
 */
interface Main : ApplicationListener {
  val consoleLogger: ConsoleLogger
  val console: ConsoleHandler
  val scheduler: CancellableThreadScheduler
  val engine: Engine?
  val isNotTest: Boolean

  /**
   * Might not return any world if the player is in a menu.
   *
   * @return The current world
   */
  val world: World?
  val renderThreadName: String

  companion object {
    fun logger(): ConsoleLogger = inst().consoleLogger

    fun inst(): Main = CommonMain.instField

    val isServerClient: Boolean
      /**
       * @return If this is a client of a server
       */
      get() = Settings.client && ClientMain.inst().serverClient != null

    val isServer: Boolean
      /**
       * @return If this is a server instance (i.e., is NOT rendering)
       */
      get() = !Settings.client

    val isClient: Boolean
      /**
       * @return If this is a client instance (i.e., is rendering)
       */
      get() = Settings.client

    val isSingleplayer: Boolean
      /**
       * @return If this is a singleplayer instance
       */
      get() = isClient && ClientMain.inst().isSinglePlayer
    val isMultiplayer: Boolean
      /**
       * @return If the current instance is multiplayer (either as the server or a client of a server)
       */
      get() = isServer || ClientMain.inst().isMultiplayer
    val isAuthoritative: Boolean
      /**
       * @return If this instance is authoritative (i.e., have the final say)
       */
      get() = isServer || isSingleplayer
    private val EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar
    val WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar
    const val VERSION_FILE = "version"

    val INST_LOCK = Any()
  }
}
