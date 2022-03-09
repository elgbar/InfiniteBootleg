package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.Console;
import com.strongjoshua.console.ConsoleUtils;
import com.strongjoshua.console.annotation.ConsoleDoc;
import java.util.HashSet;
import java.util.Set;
import no.elg.infiniteBootleg.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HelpfulConsoleHelpUtil {

  public static void printHelp(Console console, CommandExecutor exec, String command) {

    Set<Method> methods = getRelevantMethods(console, exec, command);
    if (methods.isEmpty()) {
      Main.logger().log("Command does not exist.");
      return;
    }

    for (Method m : methods) {
      StringBuilder sb = createCmdPrefix(m);
      Annotation annotation = m.getDeclaredAnnotation(ConsoleDoc.class);
      if (annotation != null) {

        ConsoleDoc doc = annotation.getAnnotation(ConsoleDoc.class);
        sb.append(doc.description());

        Class<?>[] params = m.getParameterTypes();
        String[] names = getArgNames(m, params);

        for (int i = 0; i < params.length; i++) {
          sb.append("\n");
          for (int j = 0; j < m.getName().length() + 2; j++) {
            // using spaces this way works with monotype fonts
            sb.append(" ");
          }
          sb.append('<');
          sb.append(params[i].getSimpleName());
          if (names != null && i < names.length) {
            sb.append(' ');
            sb.append(names[i]);
          }
          sb.append("> ");
          if (i < doc.paramDescriptions().length) {
            sb.append(doc.paramDescriptions()[i]);
          }
        }
      } else {
        appendCmdSignature(sb, m);
      }

      Main.logger().log(sb.toString());
    }
  }

  public static void printCommands(Console console, CommandExecutor exec) {
    for (Method method : getRelevantMethods(console, exec, null)) {
      if (canExecute(method)) {
        StringBuilder sb = createCmdPrefix(method);
        appendCmdSignature(sb, method);
        Main.logger().log(sb.toString());
      }
    }
  }

  public static boolean canExecute(Method method) {
    return !Main.isServer() || !method.isAnnotationPresent(ClientsideOnly.class);
  }

  public static void appendCmdSignature(StringBuilder sb, Method method) {
    Class<?>[] params = method.getParameterTypes();
    String[] names = getArgNames(method, params);

    for (int i = 0; i < params.length; i++) {

      sb.append('<');
      sb.append(params[i].getSimpleName());

      if (names != null && i < names.length) {
        sb.append(' ');
        sb.append(names[i]);
      }
      sb.append('>');

      if (i < params.length - 1) {
        sb.append(' ');
      }
    }
  }

  public static String generateCommandSignature(Method method) {
    StringBuilder sb = createCmdPrefix(method);
    appendCmdSignature(sb, method);
    return sb.toString();
  }

  private static String[] getArgNames(Method method, Class<?>[] params) {
    Annotation annotation = method.getDeclaredAnnotation(CmdArgNames.class);
    String[] names = null;
    if (annotation != null) {
      names = annotation.getAnnotation(CmdArgNames.class).value();

      if (names.length != params.length) {
        Main.logger()
            .warn(
                "Command argument names annotation is present on command '%s', but there are too %s names. "
                    + "Expected %d "
                    + "names "
                    + "found %d",
                method.getName(),
                (names.length < params.length) ? "few" : "many",
                params.length,
                names.length);
      }
    }
    return names;
  }

  private static StringBuilder createCmdPrefix(Method method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getName());
    sb.append(" ");
    return sb;
  }

  /**
   * @param console The console used
   * @param exec The executor to get the methods from, will look at all superclasses
   * @param command Command method to get, if null no filtering will be done
   * @return All relevant command method
   */
  public static Set<Method> getRelevantMethods(
      @NotNull Console console, @NotNull CommandExecutor exec, @Nullable String command) {
    Set<Method> methods = new HashSet<>();
    Class<?> c = exec.getClass();
    while (c != Object.class) {

      for (Method method : ClassReflection.getDeclaredMethods(c)) {
        if (method.isPublic()
            && //
            ConsoleUtils.canDisplayCommand(console, method)
            && //
            (command == null || method.getName().equalsIgnoreCase(command))) {

          methods.add(method);
        }
      }
      c = c.getSuperclass();
    }
    return methods;
  }
}
