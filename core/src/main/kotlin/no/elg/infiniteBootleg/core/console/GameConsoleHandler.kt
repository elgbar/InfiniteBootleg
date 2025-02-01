package no.elg.infiniteBootleg.core.console

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Method
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.Console
import com.strongjoshua.console.ConsoleUtils
import com.strongjoshua.console.LogLevel
import io.github.oshai.kotlinlogging.KotlinLogging
import no.elg.infiniteBootleg.core.api.Resizable
import no.elg.infiniteBootleg.core.console.GameConsoleLogger.Companion
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KParameter.Kind
import kotlin.reflect.full.memberFunctions

private val logger = KotlinLogging.logger {}

abstract class GameConsoleHandler() : GameConsoleLogger, Disposable, Resizable {
  protected abstract val console: Console
  abstract val exec: CommandExecutor

  private val consoleReader: SystemConsoleReader = SystemConsoleReader(this)
  private var disposed = false

  abstract var openConsoleKey: Int

  abstract var alpha: Float

  open fun addToInputMultiplexer() = Unit

  open fun create() {
    console.setCommandExecutor(exec)
    consoleReader.start()
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
      .mapNotNull { method: Method -> HelpfulConsoleHelpUtil.generateCommandSignature(method) }
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
    if (numArgs == 0) {
      try {
        // Try to execute with default params if no args are given
        val filter = exec.javaClass.kotlin.memberFunctions.filter {
          it.name.startsWith(commandPart, ignoreCase = true) && // Find the function with the same name
            it.parameters.filter { it.kind == Kind.VALUE }.all { it.isOptional && !it.isVararg } // All parameters must be optional and not vararg
        }
        if (filter.size == 1) {
          val func = filter.first()
          // Call the function with all default parameters (i.e., they are omitted) and the instance as the first parameter
          func.callBy(mapOf(func.parameters[0] to exec))
        } else {
          logger.error { "Multiple functions with the same name found, will not execute default" }
        }
        return true
      } catch (e: Exception) {
        logger.debug(e) { "Error executing function with all default parameters" }
      }
    }
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
            logger.debug(e) { "Error parsing parameter" }
            continue
          }
          if (HelpfulConsoleHelpUtil.allowedToExecute(method)) {
            method.isAccessible = true
            method.invoke(exec, *args.toTypedArray())
          } else {
            logger.error { "You cannot execute this command" }
            return true
          }
          return true
        } catch (e: ReflectionException) {
          if (argNr > 0) {
            logger.error {
              "Failed to execute command ${method.name}(${method.parameterTypes.joinToString(",") { it.simpleName }})' with args ${commandArgs.take(argNr).joinToString(" ")}"
            }
          } else {
            logger.error { "Failed to execute command: ${method.name}" }
          }
          logger.error {
            val pw = PrintWriter(StringWriter())
            e.cause?.cause?.printStackTrace(pw) ?: e.printStackTrace(pw)
            pw.toString()
          }
          return false
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
   * If the message starts with [Companion.DEBUG_PREFIX] it will not be logged to the in-game console
   */
  override fun log(level: LogLevel, msg: String) {
    if (disposed) {
      return
    }
    if (msg.startsWith(GameConsoleLogger.Companion.DEBUG_PREFIX)) {
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
}
