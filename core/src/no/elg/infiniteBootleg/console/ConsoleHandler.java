package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.Console;
import com.strongjoshua.console.GUIConsole;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.util.Resizable;
import no.kh498.util.Reflection;

public class ConsoleHandler implements ConsoleLogger, Disposable, Resizable {

    private Console console;
    private Window consoleWindow;

    public ConsoleHandler() {
        if (Main.renderGraphic) {
            console = new GUIConsole(VisUI.getSkin(), false, Input.Keys.APOSTROPHE);
            console.setLoggingToSystem(true);
            Main.getInputMultiplexer().addProcessor(console.getInputProcessor());
            try {
                consoleWindow = (Window) Reflection.getField(console, "consoleWindow");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        else {
            console = new StdConsole();
        }

        console.setConsoleStackTrace(true);
        console.setCommandExecutor(new Commands(this));
    }

    public void setAlpha(float a) {
        if (!Main.renderGraphic) {
            log(LogLevel.ERROR, "Cannot change alpha value of console when in headless mode");
            return;
        }
        Color color = consoleWindow.getColor();
        color.a = a;
        consoleWindow.setColor(color);

    }

    public float getAlpha() {
        return Main.renderGraphic ? consoleWindow.getColor().a : 1;
    }

    public boolean isVisible() {
        return console.isVisible();
    }

    public void draw() {
        console.draw();
    }

    /**
     * @see com.strongjoshua.console.Console#log(String, LogLevel)
     */
    @Override
    public void log(LogLevel level, String msg) {
        try {
            console.log(msg, level);
        } catch (Exception ex) {
            System.err.printf("Failed to log the message '%s' with level %s due to the exception %s: %s%n", msg, level.toString(),
                              ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    @Override
    public void dispose() {
        console.dispose();
    }

    @Override
    public void resize(int width, int height) {
        console.refresh();
    }
}
