package no.elg.infiniteBootleg.args;

import com.badlogic.gdx.utils.Disposable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import no.elg.infiniteBootleg.Settings;
import no.elg.infiniteBootleg.main.Main;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs implements Disposable {

  private final CancellableThreadScheduler scheduler = new CancellableThreadScheduler(1);

  private static final Logger logger = LoggerFactory.getLogger(ProgramArgs.class);

  public ProgramArgs(String[] args) {
    Map<Pair<String, Boolean>, String> options = Util.interpreterArgs(args);

    for (Map.Entry<Pair<String, Boolean>, String> entry : options.entrySet()) {
      Pair<String, Boolean> key = entry.getKey();

      String name = null;
      if (key.getValue()) {
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
          logger.info("Failed to find a valid argument with with the alt '" + altKey + "'");
          continue;
        }
      }

      try {
        Method method = ProgramArgs.class.getDeclaredMethod(name, String.class);
        method.invoke(this, entry.getValue());
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        logger.error("Unknown argument '" + name + "' with value '" + entry.getValue() + "'");
        System.exit(2);
      }
    }
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
    logger.info("Running commands '" + val + "' as initial commands");

    scheduler.scheduleAsync(
        10,
        () -> {
          for (String cmd : val.split(";")) {
            Main.Companion.inst().getConsole().execCommand(cmd);
          }
        });
  }

  /** Do not render the graphics */
  @Argument(value = "Disable rendering of graphics", alt = 'h')
  private void headless(@Nullable String val) {
    Settings.client = false;
    logger.info("Graphics is disabled");
  }

  /** Do not load the worlds from disk */
  @Argument(value = "Do not save nor load the world to and from disk", alt = 'l')
  private void no_load(@Nullable String val) {
    Settings.loadWorldFromDisk = false;
    if (Settings.ignoreWorldLock) {
      logger.warn(
          "The world lock have no effect when not loading worlds. The --force-load argument is"
              + " useless in with the --no-load argument");
    }
    logger.info("Worlds will not be loaded/saved from/to disk");
  }

  /** Do not load the worlds from disk */
  @Argument(value = "Force load world from disk, even if it is already in use", alt = 'f')
  private void force_load(@Nullable String val) {
    Settings.ignoreWorldLock = true;
    if (!Settings.loadWorldFromDisk) {
      logger.warn(
          "The world lock have no effect when not loading worlds. The --force-load argument is"
              + " useless in with the --no-load argument");
    }
    logger.info("World will be loaded, even if it is already in use");
  }

  /**
   * Change the default world seed of the default world loaded
   *
   * @param val The world seed
   */
  @Argument(value = "Set the default world seed. Example: --world_seed=test", alt = 's')
  private void world_seed(@Nullable String val) {
    if (val == null) {
      logger.error(
          "The seed must be provided when using world_seed argument.\nExample: --world_seed=test");

      return;
    }
    Settings.worldSeed = val.hashCode();
    logger.info("World seed set to '{}'", val);
  }

  /** Disable Box2DLights */
  @Argument(value = "Disable rendering of lights", alt = 'L')
  private void no_lights(@Nullable String val) {
    logger.info("Lights are disabled. To dynamically enable this use command 'lights'");
    Settings.renderLight = false;
  }

  /** Enable debug rendering (ie box2d) */
  @Argument(value = "Enable debugging, including debug rendering for box2d", alt = 'd')
  private void debug(@Nullable String val) {
    logger.info("Debug is enabled. To disable this at runtime use command 'debug'");
    Settings.debug = true;
  }

  @Argument(
      value = "The number of secondary threads. Must be an non-negative integer (>= 0)",
      alt = 't')
  public boolean threads(@Nullable String val) {
    if (val == null) {
      logger.error(
          "Specify the number of secondary threads. Must be an integer greater than or equal to 0");
      return false;
    }
    try {
      int threads = Integer.parseInt(val);
      if (threads < 0) {
        logger.error("Argument must be an integer greater than or equal to 0, got " + val);
        return false;
      }
      Settings.schedulerThreads = threads;
      return true;
    } catch (NumberFormatException e) {
      logger.error("Argument must be an integer greater than or equal to 0, got " + val);
      return false;
    }
  }

  @Argument(
      value = "Specify physics updates per seconds. Must be a positive integer (> 0)",
      alt = 'T')
  public boolean tps(@Nullable String val) {
    if (val == null) {
      logger.error(
          "Specify the of physics updates per seconds. Must be an integer greater than to 0");
      return false;
    }
    try {
      int tps = Integer.parseInt(val);
      if (tps <= 0) {
        logger.error("Argument must be an integer greater than 0, got " + val);
        return false;
      }
      Settings.tps = tps;
      return true;
    } catch (NumberFormatException e) {
      logger.error("Argument must be an integer greater than 0, got " + val);
      return false;
    }
  }

  @Argument(value = "Print out available arguments and exit", alt = '?')
  public void help(@Nullable String val) {
    logger.info("List of program arguments:");

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
        logger.info(
            MessageFormat.format(" --%-{0}s %s  %s%n", maxNameSize),
            method.getName().replace('_', '-'),
            singleFlag,
            arg.value());
      }
    }
    System.exit(0);
  }

  @Argument(
      value = "Start instance as server, implies --headless, argument is port to start on",
      alt = 'S')
  public boolean server(@Nullable String val) {
    Settings.client = false;
    if (val != null) {
      try {
        int port = Integer.parseInt(val);
        if (port < 0 || port >= 65535) {
          logger.error(
              "Argument must be an integer greater than or equal to 0 and less than 65535, got {}",
              val);
          return false;
        }
        Settings.port = port;
      } catch (NumberFormatException ignore) {
      }
    }
    return true;
  }
}
