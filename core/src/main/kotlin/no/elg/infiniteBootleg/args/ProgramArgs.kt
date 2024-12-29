package no.elg.infiniteBootleg.args

import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.main.Main.Companion.inst
import no.elg.infiniteBootleg.util.asWorldSeed
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * @author Elg
 */
class ProgramArgs(args: Array<String>) {
  data class ProgramArgument(val desc: String, val alt: Char = DEFAULT_ALT_CHAR, val func: (String?) -> Unit)

  private val arguments = mutableMapOf<String, ProgramArgument>()
  private val executeAfterCreate: MutableSet<() -> Unit> = mutableSetOf()

  init {
    createArguments()
    executeArguments(args)
  }

  fun onCreate() {
    executeAfterCreate.forEach { it.invoke() }
    executeAfterCreate.clear()
  }

  private fun executeArguments(args: Array<String>) {
    for ((key, value) in interpreterArgs(args)) {
      val (arg, isDoubleDash) = key
      val programArgument: ProgramArgument =
        if (isDoubleDash) {
          val name = arg.lowercase().replace('-', '_')
          val programArgument = arguments[name]
          if (programArgument == null) {
            logger.error { "Unknown argument '$name' with value '$value'" }
            continue
          }
          programArgument
        } else {
          val altKey = arg[0]
          val method = arguments.filterValues { (_, arg) -> altKey == arg }
          if (method.isEmpty()) {
            logger.info { "Failed to find a valid argument with with the alt '$altKey'" }
            continue
          }
          method.toList().first().second
        }

      try {
        programArgument.func(value)
      } catch (e: Exception) {
        logger.error(e) { "Error while executing program argument '$arg'" }
        exitProcess(2)
      }
    }
    arguments.clear()
  }

  private fun createArguments() {
    arguments["run_cmd"] = ProgramArgument(desc = "Run commands after init has completed, split commands by ';'", alt = 'c') { value ->
      if (value == null) {
        logger.warn { "No commands given to run" }
        return@ProgramArgument
      }
      logger.info { "Running commands '$value' as initial commands" }
      for (cmd in value.split(";").dropLastWhile { it.isEmpty() }) {
        executeAfterCreate.add {
          inst().console.execCommand(cmd)
        }
      }
    }

    arguments["headless"] = ProgramArgument(desc = "Disable rendering of graphics", alt = 'h') {
      Settings.client = false
      logger.info { "Graphics is disabled" }
    }

    arguments["no_load"] = ProgramArgument(desc = "Do not save nor load the world to and from disk", alt = 'l') {
      Settings.loadWorldFromDisk = false
      if (Settings.ignoreWorldLock) {
        logger.warn { "The world lock have no effect when not loading worlds. The --force-load argument is useless in with the --no-load argument" }
      }
      logger.info { "Worlds will not be loaded/saved from/to disk" }
    }

    arguments["force_load"] = ProgramArgument(desc = "Force load world from disk, even if it is already in use", alt = 'f') {
      Settings.ignoreWorldLock = true
      if (!Settings.loadWorldFromDisk) {
        logger.warn { "The world lock have no effect when not loading worlds. The --force-load argument is useless in with the --no-load argument" }
      }
      logger.info { "World will be loaded, even if it is already in use" }
    }

    arguments["world_seed"] = ProgramArgument(desc = "Set the default world seed. Example: --world_seed=test", alt = 's') { value ->
      if (value == null) {
        logger.error { "The seed must be provided when using world_seed argument." }
        logger.error { "Example: --world_seed=test" }
        return@ProgramArgument
      }
      Settings.worldSeed = value.asWorldSeed()
      logger.info { "World seed set to '$value'" }
    }

    arguments["no_lights"] = ProgramArgument(desc = "Disable rendering of lights", alt = 'L') {
      logger.info { "Lights are disabled. To dynamically enable this use command 'lights'" }
      Settings.renderLight = false
    }

    arguments["debug"] = ProgramArgument(desc = "Enable debugging, including debug rendering for box2d", alt = 'd') {
      logger.info { "Debug is enabled. To disable this at runtime use command 'debug'" }
      Settings.debug = true
    }

    arguments["tps"] = ProgramArgument(desc = "Specify physics updates per seconds. Must be a positive integer (> 0)", alt = 'T') { value ->
      if (value == null) {
        logger.error { "Specify the of physics updates per seconds. Must be an integer greater than to 0" }
        return@ProgramArgument
      }
      try {
        val tps = value.toInt()
        if (tps <= 0) {
          logger.error { "Argument must be an integer greater than 0, got $value" }
          return@ProgramArgument
        }
        Settings.tps = tps.toLong()
        return@ProgramArgument
      } catch (e: NumberFormatException) {
        logger.error { "Argument must be an integer greater than 0, got $value" }
        return@ProgramArgument
      }
    }

    arguments["help"] = ProgramArgument(desc = "Print out available arguments and exit", alt = '?') {
      logger.info { "List of program arguments:" }
      // find the maximum length of the argument methods
      val sortedArguments = arguments.toSortedMap()
      val maxNameSize = sortedArguments.keys.maxOfOrNull { it.length } ?: 0

      for ((name, prgArg) in sortedArguments) {
        val (desc, alt) = prgArg
        val singleFlag = if (alt != DEFAULT_ALT_CHAR) "-$alt" else "  "
        logger.info { String.format(" --%-${maxNameSize}s %s  %s", name.replace('_', '-'), singleFlag, desc) }
      }
      exitProcess(0)
    }

    arguments["server"] = ProgramArgument(desc = "Start instance as server, implies --headless, argument is port to start on", alt = 'S') { value ->
      Settings.client = false
      if (value != null) {
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
      return@ProgramArgument
    }
  }

  /**
   * The key in the map is a pair of the character given before an equal sign (=) or end of string
   * and a boolean that is `true` if the key started with two dash (-) as prefix. Values in
   * the map are `String`s containing the substring from after (not including the first) equal
   * sign to the end of the string.
   *
   * <h2>Example</h2>
   *
   *
   * Given `new String{"--key=value" "--toggle"}` will return `mapOf(new
   * Pair("key",true) to "value", new Pair("toggle", true), null)`
   *
   * @param args The system args to interpret
   * @return A Map of the interpreted args
   */
  fun interpreterArgs(args: Array<String>): Map<Pair<String, Boolean>, String?> {
    val argsMap: MutableMap<Pair<String, Boolean>, String?> = HashMap()

    for (arg in args) {
      if (!arg.startsWith("-")) {
        logger.error { "Failed to interpret argument $arg" }
      } else {
        // we only care about the first equals sign, the rest is a part of the value
        val equalIndex = arg.indexOf('=')
        val hasEqualSign = equalIndex != -1

        val isDoubleDash = arg.startsWith("--")
        val cutoff = if (isDoubleDash) 2 else 1

        val key = if (hasEqualSign) {
          arg.substring(cutoff, equalIndex)
        } else {
          arg.substring(cutoff)
        }

        val inArgs = mutableListOf<String>()
        if (isDoubleDash) {
          inArgs += key
        } else {
          // If this is a single dash (ie switch) then each char before the equal is a switch on its own
          inArgs += key.toCharArray().map(Char::toString)
        }

        for ((index, inArg) in inArgs.withIndex()) {
          // if there is no equal sign there is no value
          var value: String? = null
          if (hasEqualSign && index >= inArgs.size - 1) {
            // find the key and value from the index of the first equal sign, but do not include it
            // in the
            // key or value
            value = arg.substring(equalIndex + 1)
          }
          argsMap[Pair(inArg, isDoubleDash)] = value
        }
      }
    }
    return argsMap
  }

  companion object {
    const val DEFAULT_ALT_CHAR = '\u0000'
  }
}
