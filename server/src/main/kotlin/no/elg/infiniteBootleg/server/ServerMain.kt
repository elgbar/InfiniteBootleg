package no.elg.infiniteBootleg.server

import com.badlogic.gdx.Gdx
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.core.events.api.EventManager
import no.elg.infiniteBootleg.core.main.Main
import no.elg.infiniteBootleg.core.world.world.World
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.main.CommonMain
import no.elg.infiniteBootleg.net.clientBoundDisconnectPlayerPacket
import no.elg.infiniteBootleg.server.console.HeadlessGameConsoleHandler
import no.elg.infiniteBootleg.server.net.NettyServer
import no.elg.infiniteBootleg.server.net.ServerPacketSender
import no.elg.infiniteBootleg.server.world.ServerWorld
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ServerMain(progArgs: ProgramArgs, startTime: Instant) : CommonMain<HeadlessGameConsoleHandler>(progArgs, startTime) {

  override lateinit var packetSender: ServerPacketSender
    private set
  override val isSingleplayer: Boolean
    get() = false
  override val isMultiplayer: Boolean
    get() = true

  lateinit var serverWorld: ServerWorld
    private set

  init {
    val onShutdown = Runnable {
      packetSender.broadcast(clientBoundDisconnectPlayerPacket("Server closed"))
      serverWorld.save()
      serverWorld.dispose()
      dispose()
    }
    Runtime.getRuntime().addShutdownHook(Thread(onShutdown))
  }

  override fun render() {
    serverWorld.render.render()
  }

  override fun createConsole(): HeadlessGameConsoleHandler = HeadlessGameConsoleHandler()

  override fun create() {
    super.create()

    // TODO load world name from some config
    val serverWorld1 = ServerWorld(PerlinChunkGenerator(Settings.worldSeed), Settings.worldSeed, "Server World")
    serverWorld = serverWorld1
    serverWorld.initialize()

    packetSender = ServerPacketSender(serverWorld1)

    val serverThread: Thread = Thread.ofPlatform()
      .name("Server")
      .daemon()
      .priority(Thread.MAX_PRIORITY)
      .uncaughtExceptionHandler { thread, e ->
        try {
          logger.error(e) { "Unhandled exception interruption received on thread $thread" }
        } finally {
          Gdx.app.exit()
        }
      }
      .unstarted {
        try {
          NettyServer().start()
        } catch (e: InterruptedException) {
          logger.warn(e) { "Server interruption received" }
        } finally {
          Gdx.app.exit()
        }
      }

    EventManager.oneShotListener { event: WorldLoadedEvent ->
      if (event.world === serverWorld1) {
        logger.info { "Server world is ready, starting server thread" }
        serverThread.start()
      }
    }
    afterCreate()
  }

  override val world: World? get() = if (::serverWorld.isInitialized) serverWorld else null

  override fun dispose() {
    super.dispose()
    serverWorld.dispose()
  }

  companion object {

    fun inst(): ServerMain = Main.Companion.inst() as? ServerMain ?: throw IllegalStateException("Cannot get server main as a client")
  }
}
