package no.elg.infiniteBootleg.args;

import com.badlogic.gdx.utils.Disposable;
import com.strongjoshua.console.LogLevel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs implements ConsoleLogger, Disposable {

  private final CancellableThreadScheduler scheduler = new CancellableThreadScheduler(1);

  public ProgramArgs(String[] args) {
    Map<Pair<String, Boolean>, String> options = Util.interpreterArgs(args);

    for (Map.Entry<Pair<String, Boolean>, String> entry : options.entrySet()) {
      Pair<String, Boolean> key = entry.getKey();

      String name = null;
      if (Boolean.TRUE.equals(key.getValue())) {
        name = key.getKey().toLowerCase().replace('-', '_');
      } else {
        char altKey = key.getKey().charAt(0);
        // we need to find the correct method name, as this is an alt
        for (Method method : ProgramArgs.class.getDeclaredMethods()) {
          Argument a = method.getAnnotation(Argument.class);
          if (a != null && a.alt() == altKey) {
            name = method.getName();
            break;
          }
        }
        if (name == null) {
          log("ProgramArgs", "Failed to find a valid argument with with the alt '" + altKey + "'");
          continue;
        }
      }

      try {
        Method method = ProgramArgs.class.getDeclaredMethod(name, String.class);
        method.invoke(this, entry.getValue());
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    scheduler.scheduleSync(500, this::dispose);
  }

  @Override
  public void log(@NotNull LogLevel level, @NotNull String msg) {
    System.out.println("[" + level + "] " + msg);
  }

  @Override
  public void dispose() {
    scheduler.shutdown();
  }

  /*
   * Below this comment all arguments are computed.
   * An argument parser has a couple of requirement
   *
   * - It must NOT be static
   * - It must have String as its one and only argument
   * - The name of the method must be lowercase
   *
   * - If any errors occur do NOT throw an exception, rather print error with the logger
   *
   */

  @Argument(value = "Run commands after init has completed, split commands by ';'", alt = 'c')
  private void run_cmd(String val) {
    log("Running commands '" + val + "' as initial commands");

    scheduler.scheduleAsync(
        10,
        () -> {
          for (String cmd : val.split(";")) {
            Main.inst().getConsole().execCommand(cmd);
          }
        });
  }

  /** Do not render the graphics */
  @Argument(value = "Disable rendering of graphics", alt = 'h')
  private void headless(String val) {
    Settings.client = false;
    log("Graphics is disabled");
  }

  /** Do not load the worlds from disk */
  @Argument(value = "Do not save nor load the world to and from disk", alt = 'l')
  private void no_load(String val) {
    Settings.loadWorldFromDisk = false;
    log("Worlds will not be loaded/saved from/to disk");
  }

  /**
   * Change the default world seed of the default world loaded
   *
   * @param val The world seed
   */
  @Argument(value = "Set the default world seed. Example: --world_seed=test", alt = 's')
  private void world_seed(String val) {
    if (val == null) {
      log(
          LogLevel.ERROR,
          "The seed must be provided when using world_seed argument.\nExample: --world_seed=test");

      return;
    }
    Settings.worldSeed = val.hashCode();
    logf("World seed set to '%s'", val);
  }

  /** Disable Box2DLights */
  @Argument(value = "Disable rendering of lights", alt = 'L')
  private void no_lights(String val) {
    log("Lights are disabled. To dynamically enable this use command 'lights'");
    Settings.renderLight = false;
  }

  /** Enable debug rendering (ie box2d) */
  @Argument(value = "Enable debugging, including debug rendering for box2d", alt = 'd')
  private void debug(String val) {
    log("Debug view is enabled. To disable this at runtime use command 'debug'");
    Settings.renderBox2dDebug = true;
    Settings.debug = true;
  }

  @Argument(
      value = "The number of secondary threads. Must be an non-negative integer (>= 0)",
      alt = 't')
  public boolean threads(String val) {
    if (val == null) {
      log(
          LogLevel.ERROR,
          "Specify the number of secondary threads. Must be an integer greater than or equal to 0");
      return false;
    }
    try {
      int threads = Integer.parseInt(val);
      if (threads < 0) {
        log(LogLevel.ERROR, "Argument must be an integer greater than or equal to 0, got " + val);
        return false;
      }
      Settings.schedulerThreads = threads;
      return true;
    } catch (NumberFormatException e) {
      log(LogLevel.ERROR, "Argument must be an integer greater than or equal to 0, got " + val);
      return false;
    }
  }

  @Argument(
      value = "Specify physics updates per seconds. Must be a positive integer (> 0)",
      alt = 'T')
  public boolean tps(String val) {
    if (val == null) {
      log(
          LogLevel.ERROR,
          "Specify the of physics updates per seconds. Must be an integer greater than to 0");
      return false;
    }
    try {
      int tps = Integer.parseInt(val);
      if (tps <= 0) {
        log(LogLevel.ERROR, "Argument must be an integer greater than 0, got " + val);
        return false;
      }
      Settings.tps = tps;
      return true;
    } catch (NumberFormatException e) {
      log(LogLevel.ERROR, "Argument must be an integer greater than 0, got " + val);
      return false;
    }
  }

  @Argument(value = "Print out available arguments and exit", alt = '?')
  public void help(String val) {
    System.out.println("List of program arguments:");

    // find the maximum length of the argument methods
    // @formatter:off
    int maxNameSize =
        Arrays.stream(ProgramArgs.class.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Argument.class))
            .mapToInt(m -> m.getName().length())
            .max()
            .orElse(0);

    List<Method> methods =
        Arrays.stream(ProgramArgs.class.getDeclaredMethods())
            .sorted(Comparator.comparing(Method::getName))
            .toList();
    // @formatter:on
    for (Method method : methods) {
      Argument arg = method.getAnnotation(Argument.class);
      if (arg != null) {
        String singleFlag = arg.alt() != '\0' ? "-" + arg.alt() : "  ";
        System.out.printf(
            MessageFormat.format(" --%-{0}s %s  %s%n", maxNameSize),
            method.getName().replace('_', '-'),
            singleFlag,
            arg.value());
      }
    }
    System.exit(0);
  }

  public boolean server(String val) {
    Settings.client = false;
    try {
      int port = Integer.parseInt(val);
      if (port < 0 || port >= 65535) {
        log(
            LogLevel.ERROR,
            "Argument must be an integer greater than or equal to 0 and less than 65535, got "
                + val);
        return false;
      }
      Settings.port = port;
    } catch (NumberFormatException e) {
      Settings.port = 8558;
    }
    return true;
  }
}
