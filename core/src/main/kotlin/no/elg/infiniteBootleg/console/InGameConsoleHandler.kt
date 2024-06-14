package no.elg.infiniteBootleg.console

import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Method
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.kotcrab.vis.ui.VisUI
import com.strongjoshua.console.Console
import com.strongjoshua.console.ConsoleUtils
import com.strongjoshua.console.LogLevel
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.Settings
import no.elg.infiniteBootleg.api.Resizable
import no.elg.infiniteBootleg.console.InGameConsoleLogger.Companion.DEBUG_PREFIX
import no.elg.infiniteBootleg.console.commands.Commands
import no.elg.infiniteBootleg.console.consoles.CGUIConsole
import no.elg.infiniteBootleg.console.consoles.StdConsole
import no.elg.infiniteBootleg.main.ClientMain
import java.io.PrintWriter
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

class InGameConsoleHandler @JvmOverloads constructor(val inGameConsole: Boolean = Settings.client) : InGameConsoleLogger, Disposable, Resizable {
  private val console: Console
  val exec: Commands = Commands()
  private val consoleReader: SystemConsoleReader
  private var disposed = false

  init {
    if (inGameConsole) {
      console = CGUIConsole(this, VisUI.getSkin(), false, Input.Keys.APOSTROPHE)
      console.setLoggingToSystem(false)
    } else {
      console = StdConsole()
      console.setConsoleStackTrace(true)
      console.setLoggingToSystem(true)
    }
    console.setCommandExecutor(exec)

    consoleReader = SystemConsoleReader(this)
    consoleReader.start()
  }

  var alpha: Float
    get() = if (inGameConsole) console.window.color.a else 1f
    set(a) {
      if (inGameConsole) {
        console.window.color.a = a
      }
    }

  var isVisible: Boolean
    get() = console.isVisible
    set(visible) {
      console.isVisible = visible
    }

  @Synchronized
  fun draw() {
    console.draw()
  }

  fun execCommand(command: String): Boolean {
    if (console.isDisabled) {
      return false
    }
    logger.info { "> $command" }
    val parts = command.trim().split(" ")
    if (parts.isEmpty()) {
      return false
    }
    val commandPart = parts.first()
    val commandArgs: List<String> = parts.drop(1)

    val methods: Array<Method> = ClassReflection.getMethods(exec.javaClass)
    val potentialMethods = methods.asSequence()
      .filter { method: Method -> HelpfulConsoleHelpUtil.allowedToExecute(method) && method.name.startsWith(commandPart, ignoreCase = true) }
      .map { method: Method -> HelpfulConsoleHelpUtil.generateCommandSignature(method) }
      .toList()

    val possible = IntArray(false, 8)
    for (i in methods.indices) {
      val method = methods[i]
      if (method.name.equals(commandPart, ignoreCase = true) && ConsoleUtils.canExecuteCommand(console, method)) {
        possible.add(i)
      }
    }
    if (possible.isEmpty) {
      if (potentialMethods.isEmpty() || commandPart.isBlank()) {
        logger.error { "Unknown command: '$commandPart'" }
      } else {
        logger.error { "Unknown command. Perhaps you meant" }
        for (potentialMethod in potentialMethods) {
          logger.error { potentialMethod }
        }
      }
      return false
    }
    val size = possible.size
    val numArgs = commandArgs.size
    for (argNr in 0 until size) {
      val method = methods[possible[argNr]]
      val params: Array<Class<*>> = method.parameterTypes
      if (numArgs == params.size) {
        return try {
          val args = mutableListOf<Any>()
          try {
            for (j in params.indices) {
              val value = commandArgs[j]
              args += when (params[j]) {
                String::class.java -> value
                Boolean::class.javaPrimitiveType -> value.toBoolean()
                Byte::class.javaPrimitiveType -> value.toByte()
                Short::class.javaPrimitiveType -> value.toShort()
                Int::class.javaPrimitiveType -> value.toInt()
                Long::class.javaPrimitiveType -> value.toLong()
                Float::class.javaPrimitiveType -> value.toFloat()
                Double::class.javaPrimitiveType -> value.toDouble()
                else -> continue
              }
            }
          } catch (e: Exception) {
            // Error occurred trying to parse parameter, continue
            // to next function
            continue
          }
          if (HelpfulConsoleHelpUtil.allowedToExecute(method)) {
            method.isAccessible = true
            method.invoke(exec, *args.toTypedArray())
          } else {
            logger.error { "You cannot execute this command" }
            return true
          }
          true
        } catch (e: ReflectionException) {
          val sw = StringWriter()
          e.cause?.cause?.printStackTrace(PrintWriter(sw)) ?: e.printStackTrace(PrintWriter(sw))
          if (numArgs > 0) {
            logger.error {
              "Failed to execute command ${method.name}(${method.parameterTypes.joinToString(",") { it.simpleName }})' with args ${commandArgs.joinToString(" ")}"
            }
          } else {
            logger.error { "Failed to execute command: ${method.name}" }
          }
          logger.error { sw.toString() }
          false
        }
      }
    }
    if (potentialMethods.isEmpty()) {
      logger.error { "Bad parameters. Check your code." }
    } else {
      logger.error { "Unknown parameters. Did you perhaps mean?" }
      for (method in potentialMethods) {
        logger.error { method }
      }
    }
    return false
  }

  /**
   * Log a message to the in game console
   * If the message starts with [DEBUG_PREFIX] it will not be logged to the in-game console
   */
  override fun log(level: LogLevel, msg: String) {
    if (disposed) {
      return
    }
    if (msg.startsWith(DEBUG_PREFIX)) {
      // Do not log debug messages to in-game console since it will be spammed
      return
    }
    try {
      synchronized(this) {
        console.log(msg, level)
      }
    } catch (ex: Exception) {
      System.err.printf(
        "Failed to log the message '%s' with level %s due to the exception %s: %s%n",
        msg,
        level,
        ex.javaClass.simpleName,
        ex.message
      )
    }
  }

  @Synchronized
  override fun dispose() {
    if (disposed) {
      return
    }
    disposed = true
    console.dispose()
    consoleReader.dispose()
  }

  override fun resize(width: Int, height: Int) {
    console.refresh(false)
  }

  fun addToInputMultiplexer() {
    ClientMain.inst().inputMultiplexer.addProcessor(console.inputProcessor)
  }
}
