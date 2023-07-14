package no.elg.infiniteBootleg.console

import com.badlogic.gdx.utils.reflect.Annotation
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.Method
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.Console
import com.strongjoshua.console.ConsoleUtils
import com.strongjoshua.console.LogLevel
import com.strongjoshua.console.annotation.ConsoleDoc
import no.elg.infiniteBootleg.main.Main

object HelpfulConsoleHelpUtil {

  fun printHelp(console: Console, exec: CommandExecutor, command: String) {
    val methods = getRelevantMethods(console, exec, command)
    if (methods.isEmpty()) {
      console.log("Command does not exist.")
      return
    }
    for (m in methods) {
      val sb = createCmdPrefix(m)
      val annotation: Annotation? = m.getDeclaredAnnotation(ConsoleDoc::class.java)
      if (annotation != null) {
        val doc: ConsoleDoc = annotation.getAnnotation(ConsoleDoc::class.java) ?: continue
        sb.append(doc.description)
        val params = m.parameterTypes
        val names = getArgNames(m, params)
        for (i in params.indices) {
          sb.appendLine()
          // using spaces this way works with monotype fonts
          sb.append(" ".repeat((m.name.length + 2)))
          sb.cmdArgs(params, names, i)
          if (i < doc.paramDescriptions.size) {
            sb.append(doc.paramDescriptions[i])
          }
        }
      } else {
        sb.appendCmdSignature(m)
      }
      console.log(sb.toString())
      if (!allowedToExecute(m)) {
        console.log(
          "[WARN] Note that you cannot execute command '" + m.name + "'",
          LogLevel.ERROR
        )
      }
    }
  }

  fun printCommands(console: Console, exec: CommandExecutor) {
    for (method in getRelevantMethods(console, exec, null)) {
      if (allowedToExecute(method)) {
        val sb = createCmdPrefix(method)
        sb.appendCmdSignature(method)
        console.log(sb.toString())
      }
    }
  }

  fun allowedToExecute(method: Method): Boolean = !Main.isServer || !method.isAnnotationPresent(ClientsideOnly::class.java)

  private fun StringBuilder.appendCmdSignature(method: Method): StringBuilder {
    val params = method.parameterTypes
    val names = getArgNames(method, params)
    for (i in params.indices) {
      this.cmdArgs(params, names, i)
    }
    return this
  }

  private fun StringBuilder.cmdArgs(params: Array<Class<*>>, names: Array<String>?, argNr: Int): StringBuilder {
    append('<').append(params[argNr].simpleName)
    if (names != null && argNr < names.size) {
      append(' ').append(names[argNr].replace('_', '-').replace(' ', '-'))
    }
    return append("> ")
  }

  fun generateCommandSignature(method: Method): String = createCmdPrefix(method).appendCmdSignature(method).toString()

  private fun getArgNames(method: Method, params: Array<Class<*>>): Array<String>? {
    val annotation = method.getDeclaredAnnotation(CmdArgNames::class.java)
    if (annotation != null) {
      val names: Array<out String> = annotation.getAnnotation(CmdArgNames::class.java).value
      if (names.size != params.size) {
        Main.logger()
          .warn(
            "Command argument names annotation is present on command '${method.name}', " +
              "but there are too ${if (names.size < params.size) "few" else "many"} names. Expected ${params.size} names found ${names.size}"
          )
      }
    }
    return null
  }

  private fun createCmdPrefix(method: Method): StringBuilder = StringBuilder().append(method.name).append(" ")

  /**
   * @param console The console used
   * @param exec The executor to get the methods from, will look at all superclasses
   * @param command Command method to get, if null no filtering will be done
   * @return All relevant command method
   */
  private fun getRelevantMethods(console: Console, exec: CommandExecutor, command: String?): Set<Method> {
    val methods: MutableSet<Method> = HashSet()
    var clazz: Class<*> = exec.javaClass
    while (clazz != Any::class.java) {
      for (method in ClassReflection.getDeclaredMethods(clazz)) {
        if (method.isPublic && ConsoleUtils.canDisplayCommand(console, method) && method.name.equals(command, ignoreCase = true)) {
          methods.add(method)
        }
      }
      clazz = clazz.superclass
    }
    return methods
  }
}
