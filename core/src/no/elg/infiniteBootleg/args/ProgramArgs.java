package no.elg.infiniteBootleg.args;

import com.badlogic.gdx.utils.Disposable;
import com.google.common.base.Preconditions;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.render.WorldRender;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs implements ConsoleLogger, Disposable {

    private CancellableThreadScheduler scheduler;

    public ProgramArgs(String[] args) {
        Preconditions.checkNotNull(Main.inst(), "Main not initiated");
        Preconditions.checkNotNull(Main.inst().getConsoleLogger(), "The console logger should not be null");
        scheduler = new CancellableThreadScheduler(1);
        Map<Pair<String, Boolean>, String> options = Util.interpreterArgs(args);

        for (Map.Entry<Pair<String, Boolean>, String> entry : options.entrySet()) {
            Pair<String, Boolean> key = entry.getKey();

            String name = null;
            if (key.getValue()) {
                name = Util.toTitleCase(false, key.getKey().toLowerCase().replace('_', ' ').replace('-', ' ')).replace(
                    " ", "");
            }
            else {
                char altKey = key.getKey().charAt(0);
                //we need to find the correct method name, as this is an alt
                for (Method method : ProgramArgs.class.getDeclaredMethods()) {
                    Argument a = method.getAnnotation(Argument.class);
                    if (a != null && a.alt() == altKey) {
                        name = method.getName();
                        break;
                    }
                }
                if (name == null) {
                    Main.inst().getConsoleLogger().logf(LogLevel.ERROR,
                                                        "Failed to find a valid argument with with the alt '%s'",
                                                        altKey);
                    continue;
                }
            }

            try {
                Method method = ProgramArgs.class.getDeclaredMethod(name, String.class);
                if (method != null) {
                    method.invoke(this, entry.getValue());
                }
                else {
                    Main.inst().getConsoleLogger().logf(LogLevel.ERROR, "Unknown argument '%s'", entry.getKey());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        scheduler.scheduleSync(this::dispose, 500);
    }

    @Override
    public void log(@NotNull LogLevel level, @NotNull String msg) {
        scheduler.scheduleAsync(() -> Main.inst().getConsoleLogger().log(level, msg), 2);
    }

    @Override
    public void dispose() {
        scheduler.shutdown();
    }

    /*
     * Below this comment all arguments are computed.
     * A argument parser has a couple of requirement
     *
     * - It must NOT be static
     * - It must have String as its one and only argument
     * - The name of the method must be lowercase
     *
     * - If any errors occur do NOT throw an exception, rather print error with the logger
     *
     */


    /**
     * Do not render the graphics
     */
    @Argument(value = "Disable rendering of graphics.", alt = 'h')
    private void headless(String val) {
        Main.renderGraphic = false;
        log("Graphics is disabled");
    }

    /**
     * Do not load the worlds from disk
     */
    @Argument(value = "The world will not be loaded from disk", alt = 'l')
    private void noLoad(String val) {
        Main.loadWorldFromDisk = false;
        log("Worlds will not be loaded/saved from/to disk");
    }

    /**
     * Change the default world seed of the default world loaded
     *
     * @param val
     *     The world seed
     */
    @Argument(value = "Set the default world seed, a value must be specified. Example: --world_seed=test", alt = 's')
    private void worldSeed(String val) {
        if (val == null) {
            log(LogLevel.ERROR,
                "The seed must be provided when using world_Seed argument.\nExample: --world_seed=test");

            return;
        }
        Main.worldSeed = val.hashCode();
        logf("World seed set to '%s'", val);
    }

    /**
     * Disable Box2DLights
     */

    @Argument(value = "Disable rendering of lights", alt = 'L')
    private void noLights(String val) {
        log("Lights are disabled. To dynamically enable this use command 'lights true'");
        WorldRender.lights = false;
    }

    /**
     * Enable debug rendering (ie box2d)
     */
    @Argument(value = "Enable debugging including debug rendering for box2d", alt = 'd')
    private void debug(String val) {
        log("Debug view is enabled. To disable this at runtime use command 'debug'");
        WorldRender.debugBox2d = true;
        Main.debug = true;
    }

    @Argument(value = "Specify the number of secondary threads. Must be an integer greater than or equal to 0",
              alt = 't')
    public boolean threads(String val) {
        if (val == null) {
            log(LogLevel.ERROR,
                "Specify the number of secondary threads. Must be an integer greater than or equal to 0");
            return false;
        }
        try {
            int threads = Integer.parseInt(val);
            if (threads < 0) {
                log(LogLevel.ERROR, "Argument must be an integer greater than or equal to 0");
                return false;
            }
            Main.schedulerThreads = threads;
            return true;
        } catch (NumberFormatException e) {
            log(LogLevel.ERROR, "Argument must be an integer greater than or equal to 0");
            return false;
        }
    }

    @Argument(value = "Print out available arguments and exit", alt = '?')
    public void help(String val) {
        System.out.println("List of program arguments:");

        //find the maximum length of the argument methods
        //@formatter:off
        int maxNameSize = Arrays.stream(ProgramArgs.class.getDeclaredMethods()).
            filter(m -> m.isAnnotationPresent(Argument.class)).
            mapToInt(m -> m.getName().length()).
            max().orElse(0);
        //@formatter:on

        for (Method method : ProgramArgs.class.getDeclaredMethods()) {
            Argument arg = method.getAnnotation(Argument.class);
            if (arg != null) {
                String singleFlag = arg.alt() != '\0' ? "-" + arg.alt() : "  ";
                System.out.printf(MessageFormat.format(" --%-{0}s %s  %s%n", maxNameSize), method.getName(), singleFlag,
                                  arg.value());
            }
        }
        System.exit(0);
    }
}
