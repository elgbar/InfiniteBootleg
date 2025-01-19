package no.elg.infiniteBootleg.server.args

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.args.ProgramArgs

private val logger = KotlinLogging.logger {}

class ServerProgramArgs(args: Array<String>) : ProgramArgs(args) {

  override fun createSpecificArguments(arguments: MutableMap<String, ProgramArgument>) {
    arguments["port"] = ProgramArgument(desc = "What port to start the server on", alt = 'P', default = Settings.port) { value ->
      if (value == null) {
        logger.error { "No port specified, using default port: ${Settings.port}" }
        return@ProgramArgument
      }
      try {
        val port = value.toInt()
        if (port < 0 || port >= 65535) {
          logger.error { "Argument must be an integer greater than or equal to 0 and less than 65535, got $value" }
          return@ProgramArgument
        }
        Settings.port = port
      } catch (e: NumberFormatException) {
        logger.error(e) { "Invalid number for the port" }
        return@ProgramArgument
      }
    }
  }
}
