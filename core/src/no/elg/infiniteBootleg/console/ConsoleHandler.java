package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.GUIConsole;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Util;
import no.kh498.util.Reflection;

public class ConsoleHandler extends GUIConsole implements ConsoleLogger {

    private Window consoleWindow;

    public ConsoleHandler() {
        super(VisUI.getSkin(), false, Input.Keys.APOSTROPHE);
//        setCommandExecutor(new CommandHandler(this));
        Main.getInputMultiplexer().addProcessor(getInputProcessor());
        logToSystem = true;
        consoleTrace = true;

        try {
            consoleWindow = (Window) Reflection.getSuperField(this, "consoleWindow");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        setCommandExecutor(new Commands(this));
        log(LogLevel.SUCCESS, "Version #" + Util.getVersion());
    }

    public void setAlpha(float a) {
        Color color = consoleWindow.getColor();
        color.a = a;
        consoleWindow.setColor(color);
    }

    public float getAlpha() {
        return consoleWindow.getColor().a;
    }

    @Override
    public void logf(final String msg, final Object... objs) {
        logf(LogLevel.DEFAULT, msg, objs);
    }

    @Override
    public void logf(final LogLevel level, final String msg, final Object... objs) {
        log(level, String.format(msg, objs));
    }

    /**
     * @see com.strongjoshua.console.Console#log(String, LogLevel)
     */
    @Override
    public void log(LogLevel level, String msg) {
        try {
            super.log(msg, level);
        } catch (Exception ex) {
            System.err.printf("Failed to log the message '%s' with level %s due to the exception %s: %s%n", msg, level.toString(),
                              ex.getClass().getSimpleName(), ex.getMessage());
        }
    }


    @Override
    public void log(String msg) {
        log(LogLevel.DEFAULT, msg);
    }
}
