package no.elg.infiniteBootleg.main

import com.badlogic.gdx.Gdx
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.server.Server
import no.elg.infiniteBootleg.server.broadcast
import no.elg.infiniteBootleg.server.clientBoundDisconnectPlayerPacket
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.world.ServerWorld
import no.elg.infiniteBootleg.world.world.World

/**
 * @author Elg
 */
class ServerMain(test: Boolean, progArgs: ProgramArgs?) : CommonMain(test, progArgs) {

  private val server: Server = Server()

  lateinit var serverWorld: ServerWorld
    private set

  override lateinit var renderThreadName: String

  init {
    synchronized(Main.INST_LOCK) {
      check(Companion::instField.isLateinit) { "A server main instance have already be declared" }
      instField = this
    }
    val onShutdown = Runnable {
      broadcast(clientBoundDisconnectPlayerPacket("Server closed"))
      serverWorld.save()
      serverWorld.dispose()
      dispose()
      scheduler.shutdown() // make sure scheduler threads are dead
    }
    Runtime.getRuntime().addShutdownHook(Thread(onShutdown))
  }

  override fun render() {
    serverWorld.render.render()
  }

  override fun create() {
    super.create()
    renderThreadName = Thread.currentThread().name
    val runnable = {
      try {
        server.start()
      } catch (e: InterruptedException) {
        console.log("SERVER", "Server interruption received", e)
        Gdx.app.exit()
      }
    }
    Thread(runnable, "Server").also {
      it.isDaemon = true
      it.start()
    }

    console.log("SERVER", "Starting server on port ${Settings.port}")

    // TODO load world name from some config
    serverWorld = ServerWorld(PerlinChunkGenerator(Settings.worldSeed.toLong()), Settings.worldSeed.toLong(), "Server World")
    serverWorld.initialize()
  }

  override val world: World? get() = if (::serverWorld.isInitialized) serverWorld else null

  override fun dispose() {
    super.dispose()
    serverWorld.dispose()
  }

  companion object {
    private lateinit var instField: ServerMain
    fun inst(): ServerMain {
      if (!Settings.client) {
        return instField
      }
      throw IllegalStateException("Cannot get server main as a client")
    }
  }
}
