package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.util.Ticker;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class Settings {

    /**
     * If worlds should be loaded from disk
     */
    public static boolean loadWorldFromDisk = true;

    /**
     * If graphics should be rendered, false implies this should be a server
     */
    public static boolean client = true;

    public static boolean isServer() {
        return !client;
    }

    /**
     * Seed of the world loaded
     */
    public static int worldSeed;

    /**
     * If general debug variable. Use this and-ed with your specific debug variable
     */
    public static boolean debug;

    public static int schedulerThreads = -1;

    /**
     * The ticks per seconds to use by default. Changing this will only apply to new instances created.
     */
    public static long tps = Ticker.DEFAULT_TICKS_PER_SECOND;

    public static boolean dayTicking = true;

    public static boolean renderLight = true;

    public static boolean renderBox2dDebug;

    public static boolean enableCameraFollowLerp = true;

    public static int port = -1;

    @NotNull
    public static String host = "";

    public static boolean stageDebug;

    public static int chunkRadius = 10; //ish max chunks when fully zoomed out

    private Settings() { }

}
