package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.ApplicationLogger;
import com.strongjoshua.console.LogLevel;

/**
 * @author Elg
 */
public interface ConsoleLogger extends ApplicationLogger {
    
    /**
     * @param level
     *     The level to log at
     * @param msg
     *     The message to log
     */
    void log(LogLevel level, String msg);

    /**
     * Log a level with {@link LogLevel#DEFAULT} loglevel
     *
     * @param msg
     *     The message to log
     * @param objs
     *     The object to format
     *
     * @see String#format(String, Object...)
     */
    default void logf(String msg, Object... objs) {
        logf(LogLevel.DEFAULT, msg, objs);
    }

    /**
     * @param level
     *     The level to log at
     * @param msg
     *     The message to log
     * @param objs
     *     The object to format
     *
     * @see String#format(String, Object...)
     */
    default void logf(LogLevel level, String msg, Object... objs) {
        log(level, String.format(msg, objs));
    }

    /**
     * Log a level with {@link LogLevel#DEFAULT} loglevel
     *
     * @param msg
     *     The message to log
     */
    default void log(String msg) {
        log(LogLevel.DEFAULT, msg);
    }

    @Override
    default void log(String tag, String message) {
        log("[" + tag + "] " + message);
    }

    @Override
    default void log(String tag, String message, Throwable exception) {
        log(tag, message);
        exception.printStackTrace(System.out);
    }

    @Override
    default void error(String tag, String message) {
        log(LogLevel.ERROR, "[" + tag + "] " + message);
    }

    @Override
    default void error(String tag, String message, Throwable exception) {
        error(tag, message);
        exception.printStackTrace(System.err);
    }

    @Override
    default void debug(String tag, String message) {
        log(LogLevel.DEFAULT, "DBG [" + tag + "] " + message);
    }

    @Override
    default void debug(String tag, String message, Throwable exception) {
        debug(tag, message);
        exception.printStackTrace(System.out);
    }
}
