package no.elg.infiniteBootleg;

import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.util.Util;
import no.elg.infiniteBootleg.world.render.WorldRender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Elg
 */
@SuppressWarnings("unused")
public class ProgramArgs {


    public static void executeArgs(String[] args) {
        Map<String, String> options = Util.interpreterArgs(args);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            try {
                Method method = ProgramArgs.class.getDeclaredMethod(entry.getKey().toLowerCase(), String.class);
                if (method != null) {
                    method.invoke(null, entry.getValue());
                }
                else {
                    Main.inst().getConsoleLogger().logf(LogLevel.ERROR, "Unknown argument '%s'", entry.getKey());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }


    /*
     * Below this comment all arguments are computed.
     * A argument parser has a couple of requirement
     *
     * - It must be static
     * - It must have String as its one and only argument
     * - The name of the method must be lowercase
     *
     * - If any errors occur do NOT throw an exception, rather print out an error on std err
     *
     */


    /**
     * Do not render the graphics
     */
    private static void headless(String val) {
        Main.renderGraphic = false;
        Main.inst().getConsoleLogger().log("Graphics are disabled");
    }

    /**
     * Do not load the worlds from disk
     */
    private static void no_load(String val) {
        Main.loadWorldFromDisk = false;
        Main.inst().getConsoleLogger().log("Worlds will not be loaded/saved from disk");
    }

    /**
     * Change the default world seed of the default world loaded
     *
     * @param val
     *     The world seed
     */
    public static void world_seed(String val) {
        if (val == null) {
            Main.inst().getConsoleLogger()
                .log(LogLevel.ERROR, "The seed must be provided when using world_Seed " + "argument.\nExample: -world_seed=test");

            return;
        }
        Main.worldSeed = val.hashCode();
        Main.inst().getConsoleLogger().logf("World seed set to '%s'", val);
    }

    /**
     * Disable box2dlights
     */
    public static void no_lights(String val) {
        Main.inst().getConsoleLogger().log("Lights are disabled. To dynamically enable this use command 'lights true'");
        WorldRender.lights = false;
    }

    /**
     * Enable debug rendering (ie box2d)
     */
    public static void debug(String val) {
        Main.inst().getConsoleLogger().log("Debug view is enabled. To disable this at runtime use command 'debugBox2d'");
        WorldRender.debugBox2d = true;
    }
}
