package no.elg.infiniteBootleg.console;

import static no.elg.infiniteBootleg.console.HelpfulConsoleHelpUtil.canExecute;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.Console;
import com.strongjoshua.console.ConsoleUtils;
import com.strongjoshua.console.LogLevel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.console.consoles.CGUIConsole;
import no.elg.infiniteBootleg.console.consoles.StdConsole;
import no.elg.infiniteBootleg.util.Resizable;
import org.jetbrains.annotations.NotNull;

public class ConsoleHandler implements ConsoleLogger, Disposable, Resizable {

  private final boolean inGameConsole;
  private final Console console;
  private final CommandExecutor exec;
  private final SystemConsoleReader consoleReader;
  private boolean disposed;

  public ConsoleHandler() {
    this(Settings.client);
  }

  public ConsoleHandler(boolean inGameConsole) {
    this.inGameConsole = inGameConsole;
    if (inGameConsole) {
      console = new CGUIConsole(this, VisUI.getSkin(), false, Input.Keys.APOSTROPHE);
      console.setLoggingToSystem(true);
    } else {
      console = new StdConsole();
    }
    consoleReader = new SystemConsoleReader(this);
    console.setConsoleStackTrace(true);
    exec = new Commands(this);
    console.setCommandExecutor(exec);
  }

  public float getAlpha() {
    return inGameConsole ? console.getWindow().getColor().a : 1;
  }

  public void setAlpha(float a) {
    if (inGameConsole) {
      console.getWindow().getColor().a = a;
    }
  }

  public boolean isVisible() {
    return console.isVisible();
  }

  public synchronized void draw() {
    console.draw();
  }

  public boolean execCommand(@NotNull String command) {
    if (console.isDisabled()) {
      return false;
    }
    log(LogLevel.COMMAND, command);
    String[] parts = command.split(" ");
    String methodName = parts[0];
    String[] sArgs = null;
    if (parts.length > 1) {
      sArgs = new String[parts.length - 1];
      System.arraycopy(parts, 1, sArgs, 0, parts.length - 1);
    }

    Method[] methods = ClassReflection.getMethods(exec.getClass());

    Set<String> potentialMethods =
        Arrays.stream(methods)
            .filter(
                m ->
                    canExecute(m) && m.getName().toLowerCase().startsWith(methodName.toLowerCase()))
            .map(HelpfulConsoleHelpUtil::generateCommandSignature)
            .collect(Collectors.toSet());

    Array<Integer> possible = new Array<>(false, 8);
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (method.getName().equalsIgnoreCase(methodName)
          && ConsoleUtils.canExecuteCommand(console, method)) {
        possible.add(i);
      }
    }

    if (possible.size <= 0) {
      if (potentialMethods.isEmpty() || methodName.isBlank()) {
        log(LogLevel.ERROR, "Unknown command: '" + methodName + "'");
      } else {
        log(LogLevel.ERROR, "Unknown command. Perhaps you meant");
        for (String methodSig : potentialMethods) {
          log(LogLevel.ERROR, methodSig);
        }
      }
      return false;
    }

    int size = possible.size;
    int numArgs = sArgs == null ? 0 : sArgs.length;
    for (int i = 0; i < size; i++) {
      Method m = methods[possible.get(i)];
      Class<?>[] params = m.getParameterTypes();
      if (numArgs == params.length) {
        try {
          Object[] args = null;

          try {
            if (sArgs != null) {
              args = new Object[numArgs];

              for (int j = 0; j < params.length; j++) {
                Class<?> param = params[j];
                final String value = sArgs[j];

                if (param == String.class) {
                  args[j] = value;
                } else if (param == boolean.class) {
                  args[j] = Boolean.parseBoolean(value);
                } else if (param == byte.class) {
                  args[j] = Byte.parseByte(value);
                } else if (param == short.class) {
                  args[j] = Short.parseShort(value);
                } else if (param == int.class) {
                  args[j] = Integer.parseInt(value);
                } else if (param == long.class) {
                  args[j] = Long.parseLong(value);
                } else if (param == float.class) {
                  args[j] = Float.parseFloat(value);
                } else if (param == double.class) {
                  args[j] = Double.parseDouble(value);
                }
              }
            }
          } catch (Exception e) {
            // Error occurred trying to parse parameter, continue
            // to next function
            continue;
          }
          if (canExecute(m)) {
            m.setAccessible(true);
            m.invoke(exec, args);
          } else {
            log(LogLevel.ERROR, "This command can only be executed client side");
            return true;
          }
          return true;
        } catch (ReflectionException e) {
          StringWriter sw = new StringWriter();
          if (e.getCause() != null && e.getCause().getCause() != null) {
            e.getCause().getCause().printStackTrace(new PrintWriter(sw));
          } else {
            e.printStackTrace(new PrintWriter(sw));
          }
          if (numArgs > 0) {
            log(
                LogLevel.ERROR,
                "Failed to execute command "
                    + m.getName()
                    + "("
                    + Arrays.stream(m.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "))
                    + ")' with args "
                    + Arrays.toString(sArgs));
          } else {
            log(LogLevel.ERROR, "Failed to execute command: " + m.getName());
          }
          log(LogLevel.ERROR, sw.toString());
          return false;
        }
      }
    }

    if (potentialMethods.isEmpty()) {
      log(LogLevel.ERROR, "Bad parameters. Check your code.");
    } else {
      log(LogLevel.ERROR, "Unknown parameters. Did you perhaps mean?");
      for (String method : potentialMethods) {
        log(LogLevel.ERROR, method);
      }
    }
    return false;
  }

  /**
   * @see com.strongjoshua.console.Console#log(String, LogLevel)
   */
  @Override
  public synchronized void log(@NotNull LogLevel level, @NotNull String msg) {
    if (disposed) {
      System.out.println("[POST DISPOSED LOGGING] <" + level.name() + "> " + msg);
      return;
    }
    try {
      console.log(msg, level);
    } catch (Exception ex) {
      System.err.printf(
          "Failed to log the message '%s' with level %s due to the exception %s: %s%n",
          msg, level, ex.getClass().getSimpleName(), ex.getMessage());
    }
  }

  private boolean isClientsideOnly(@NotNull Method method) {
    return method.isAnnotationPresent(ClientsideOnly.class);
  }

  @Override
  public synchronized void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    console.dispose();
    consoleReader.dispose();
  }

  @Override
  public void resize(int width, int height) {
    console.refresh();
  }

  public void addToInputMultiplexer() {
    ClientMain.inst().getInputMultiplexer().addProcessor(console.getInputProcessor());
  }
}
