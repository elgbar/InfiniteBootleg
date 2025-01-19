package no.elg.infiniteBootleg.server

import com.badlogic.gdx.Gdx
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.events.WorldLoadedEvent
import no.elg.infiniteBootleg.events.api.EventManager
import no.elg.infiniteBootleg.main.CommonMain
import no.elg.infiniteBootleg.main.Main
import no.elg.infiniteBootleg.net.PacketBroadcaster
import no.elg.infiniteBootleg.net.clientBoundDisconnectPlayerPacket
import no.elg.infiniteBootleg.server.net.NettyServer
import no.elg.infiniteBootleg.server.net.ServerPacketBroadcaster
import no.elg.infiniteBootleg.server.world.ServerWorld
import no.elg.infiniteBootleg.world.generator.chunk.PerlinChunkGenerator
import no.elg.infiniteBootleg.world.world.World
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ServerMain(progArgs: ProgramArgs, startTime: Instant) : CommonMain(progArgs, startTime) {

  override lateinit var packetBroadcaster: PacketBroadcaster
    private set

  lateinit var serverWorld: ServerWorld
    private set

  init {
    val onShutdown = Runnable {
      packetBroadcaster.broadcast(clientBoundDisconnectPlayerPacket("Server closed"))
      serverWorld.save()
      serverWorld.dispose()
      dispose()
    }
    Runtime.getRuntime().addShutdownHook(Thread(onShutdown))
  }

  override fun render() {
    serverWorld.render.render()
  }

  override fun create() {
    super.create()

    // TODO load world name from some config
    val serverWorld1 = ServerWorld(PerlinChunkGenerator(Settings.worldSeed), Settings.worldSeed, "Server World")
    serverWorld = serverWorld1
    serverWorld.initialize()

    packetBroadcaster = ServerPacketBroadcaster(serverWorld1)

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
