package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ApplicationListener
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.assets.InfAssets
import no.elg.infiniteBootleg.console.InGameConsoleHandler
import no.elg.infiniteBootleg.util.CancellableThreadScheduler
import no.elg.infiniteBootleg.world.world.World
import java.io.File

/**
 * @author Elg
 */
interface Main : ApplicationListener {
  val console: InGameConsoleHandler
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
  val assets: InfAssets

  /**
   * If we are allowed to make changes or move a given entity
   *
   * As a client we are only allowed to change our own entity and as a server or while in singleplayer we are allowed to change any entity
   */
  fun isAuthorizedToChange(entity: Entity): Boolean

  companion object {

    @Suppress("NOTHING_TO_INLINE")
    inline fun inst(): Main = CommonMain.instField!!

    /**
     * @return If this is a client of a server
     */
    val isServerClient: Boolean
      get() = Settings.client && ClientMain.inst().serverClient != null

    /**
     * @return If this is a server instance (i.e., is NOT rendering)
     */
    val isServer: Boolean
      get() = !Settings.client

    /**
     * @return If this is a client instance (i.e., is rendering)
     */
    val isClient: Boolean
      get() = Settings.client

    /**
     * @return If this is a singleplayer instance
     */
    val isSingleplayer: Boolean
      get() = isClient && ClientMain.inst().isSingleplayer

    /**
     * @return If the current instance is multiplayer (either as the server or a client of a server)
     */
    val isMultiplayer: Boolean
      get() = isServer || ClientMain.inst().isMultiplayer

    /**
     * @return If this instance is authoritative (i.e., have the final say)
     */
    val isAuthoritative: Boolean
      get() = isServer || isSingleplayer

    /**
     * @return If this instance is not authoritative (i.e., does not have the final say)
     */
    val isNotAuthoritative: Boolean
      get() = isServerClient

    private val EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar
    val WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar
    const val VERSION_FILE = "version"
  }
}
