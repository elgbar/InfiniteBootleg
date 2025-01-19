package no.elg.infiniteBootleg.main

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Collections
import io.github.oshai.kotlinlogging.KotlinLogging
import ktx.async.KtxAsync
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs
import no.elg.infiniteBootleg.assets.InfAssets
import no.elg.infiniteBootleg.assets.InfAssetsImpl
import no.elg.infiniteBootleg.console.InGameConsoleHandler
import no.elg.infiniteBootleg.logging.Slf4jApplicationLogger
import no.elg.infiniteBootleg.main.Main.Companion.isAuthoritative
import no.elg.infiniteBootleg.server.ClientPacketBroadcaster
import no.elg.infiniteBootleg.server.PacketBroadcaster
import no.elg.infiniteBootleg.util.Util
import no.elg.infiniteBootleg.util.diffTimePretty
import org.fusesource.jansi.AnsiConsole
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
abstract class CommonMain(private val progArgs: ProgramArgs, override val startTime: Instant) : ApplicationAdapter(), Main {
  override lateinit var console: InGameConsoleHandler
    protected set

  override val assets: InfAssets = InfAssetsImpl()

  final override lateinit var renderThreadName: String
    private set

  override val packetBroadcaster: PacketBroadcaster
    get() = ClientPacketBroadcaster

  init {
    instField = this
  }

  override fun create() {
    AnsiConsole.systemInstall()
    if (!AnsiConsole.isInstalled()) {
      logger.warn { "Failed to install jansi" }
    }

    KtxAsync.initiate()
    renderThreadName = Thread.currentThread().name
    console = InGameConsoleHandler().apply {
      alpha = 0.85f
    }
    Gdx.app.applicationLogger = Slf4jApplicationLogger()
    Gdx.app.logLevel = if (Settings.debug) Application.LOG_DEBUG else Application.LOG_INFO
    assets.loadAssets()
    logger.info { "Version ${Util.version}" }
    Util.getLastCommitDate(Util.RELATIVE_TIME)?.also {
      logger.debug { "Last commit created $it" }
    }
    logger.info { "You can also start the program with arguments for '--help' or '-?' as arg to see all possible options" }
    Runtime.getRuntime().addShutdownHook(
      Thread {
        if (isAuthoritative) {
          val activeWorld = world
          activeWorld?.save()
          activeWorld?.dispose()
        }
      }
    )
  }

  protected fun afterCreate() {
    progArgs.onCreate()
    logger.info {
      val processStartupTime = ProcessHandle.current().info().startInstant().map(::diffTimePretty).orElseGet { "???" }
      val userStartupTime = diffTimePretty(startTime)
      "Create in $userStartupTime (process: $processStartupTime)"
    }
  }

  override val engine: Engine? get() = world?.engine

  override fun isAuthorizedToChange(entity: Entity): Boolean = isAuthoritative

  override fun dispose() {
    console.dispose()
  }

  companion object {

    private val INST_LOCK = Any()

    /**
     * Backing nullable field of main
     */
    @JvmStatic
    var instField: Main? = null
      private set(value) {
        synchronized(INST_LOCK) {
          check(field == null) { "A common main instance have already be declared" }
          field = value
        }
      }

    init {
      Collections.allocateIterators = true
    }
  }
}
