package no.elg.infiniteBootleg.console;

import com.strongjoshua.console.LogLevel;

/**
 * @author Elg
 */
public interface ConsoleLogger {

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

    /**
     * @param level
     *     The level to log at
     * @param msg
     *     The message to log
     */
    void log(LogLevel level, String msg);

}
