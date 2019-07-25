package no.elg.infiniteBootleg;

import com.badlogic.gdx.utils.Disposable;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.render.WorldRender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs implements ConsoleLogger, Disposable {

    private CancellableThreadScheduler scheduler;

    public static void executeArgs(String[] args) {
        ProgramArgs pa = new ProgramArgs(args);
        pa.scheduler.scheduleSync(pa::dispose, 50);
    }

    public ProgramArgs(String[] args) {
        scheduler = new CancellableThreadScheduler(1);
        Map<String, String> options = Util.interpreterArgs(args);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            try {
                Method method = ProgramArgs.class.getDeclaredMethod(entry.getKey().toLowerCase(), String.class);
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
    }


    @Override
    public void log(LogLevel level, String msg) {
        scheduler.scheduleAsync(() -> Main.inst().getConsoleLogger().log(level, msg), 2);
    }


    /*
     * Below this comment all arguments are computed.
     * A argument parser has a couple of requirement
     *
     * - It must NOT be static
     * - It must have String as its one and only argument
     * - The name of the method must be lowercase
     *
     * - If any errors occur do NOT throw an exception, rather print out an error on std err
     *
     */


    /**
     * Do not render the graphics
     */
    private void headless(String val) {
        Main.renderGraphic = false;
        log("Graphics is disabled");
    }

    /**
     * Do not load the worlds from disk
     */
    private void no_load(String val) {
        Main.loadWorldFromDisk = false;
        log("Worlds will not be loaded/saved from/to disk");
    }

    /**
     * Change the default world seed of the default world loaded
     *
     * @param val
     *     The world seed
     */
    public void world_seed(String val) {
        if (val == null) {
            log(LogLevel.ERROR,
                "The seed must be provided when using world_Seed " + "argument.\nExample: -world_seed=test");

            return;
        }
        Main.worldSeed = val.hashCode();
        logf("World seed set to '%s'", val);
    }

    /**
     * Disable Box2DLights
     */
    public void no_lights(String val) {
        log("Lights are disabled. To dynamically enable this use command 'lights true'");
        WorldRender.lights = false;
    }

    /**
     * Enable debug rendering (ie box2d)
     */
    public void debug(String val) {
        log("Debug view is enabled. To disable this at runtime use command 'debug'");
        WorldRender.debugBox2d = true;
    }

    public boolean threads(String val) {
        if (val == null) {
            log(LogLevel.ERROR,
                "Specify the number of secondary threads. Must be an integer greater than or equal to 0");
            return false;
        }
        try {
            int threads = Integer.valueOf(val);
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

    @Override
    public void dispose() {
        scheduler.shutdown();
    }
}
