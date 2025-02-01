package no.elg.infiniteBootleg.client.args

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.Settings
import no.elg.infiniteBootleg.core.args.ProgramArgs

private val logger = KotlinLogging.logger {}

class ClientProgramArgs(args: Array<String>) : ProgramArgs(args) {

  override fun createSpecificArguments(arguments: MutableMap<String, ProgramArgument>) {
    arguments["no_lights"] = ProgramArgument(desc = "Disable rendering of lights", alt = 'L') {
      logger.info { "Lights are disabled. To dynamically enable this use command 'lights'" }
      Settings.renderLight = false
    }
  }
}
