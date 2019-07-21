package no.elg.infiniteBootleg.console;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.kotcrab.vis.ui.VisUI;
import com.strongjoshua.console.CommandExecutor;
import com.strongjoshua.console.Console;
import com.strongjoshua.console.ConsoleUtils;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.console.consoles.CGUIConsole;
import no.elg.infiniteBootleg.console.consoles.StdConsole;
import no.elg.infiniteBootleg.util.Resizable;
import no.kh498.util.Reflection;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ConsoleHandler implements ConsoleLogger, Disposable, Resizable {

    private Console console;
    private Window consoleWindow;
    private CommandExecutor exec;

    public ConsoleHandler() {
        if (Main.renderGraphic) {
            console = new CGUIConsole(this, VisUI.getSkin(), false, Input.Keys.APOSTROPHE);
            console.setLoggingToSystem(true);
            Main.getInputMultiplexer().addProcessor(console.getInputProcessor());
            try {
                consoleWindow = (Window) Reflection.getSuperField(console, "consoleWindow");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        else {
            console = new StdConsole(this);
        }

        console.setConsoleStackTrace(true);
        exec = new Commands(this);
        console.setCommandExecutor(exec);
    }

    public void setAlpha(float a) {
        if (!Main.renderGraphic) {
            return;
        }
        consoleWindow.getColor().a = a;
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

    private boolean isClientsideOnly(Method method) {
        return method.isAnnotationPresent(ClientsideOnly.class);
    }

    public boolean execCommand(String command) {
        if (console.isDisabled()) { return false; }

        log(LogLevel.COMMAND, command);

        String[] parts = command.split(" ");
        String methodName = parts[0];
        String[] sArgs = null;
        if (parts.length > 1) {
            sArgs = new String[parts.length - 1];
            System.arraycopy(parts, 1, sArgs, 0, parts.length - 1);
        }

        Method[] methods = ClassReflection.getMethods(exec.getClass());
        Array<Integer> possible = new Array<>(false, 8);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equalsIgnoreCase(methodName) && //
                ConsoleUtils.canExecuteCommand(console, method)) {
                possible.add(i);
            }
        }

        if (possible.size <= 0) {
            log(LogLevel.ERROR, "Unknown command");
            return false;
        }

        int size = possible.size;
        int numArgs = sArgs == null ? 0 : sArgs.length;
        for (int i = 0; i < size; i++) {
            Method m = methods[possible.get(i)];
            Class<?>[] params = m.getParameterTypes();
            if (numArgs == params.length) {
                try {
                    Object[] args = null;

                    try {
                        if (sArgs != null) {
                            args = new Object[numArgs];

                            for (int j = 0; j < params.length; j++) {
                                Class<?> param = params[j];
                                final String value = sArgs[j];

                                if (param == String.class) {
                                    args[j] = value;
                                }
                                else if (param == boolean.class) {
                                    args[j] = Boolean.parseBoolean(value);
                                }
                                else if (param == byte.class) {
                                    args[j] = Byte.parseByte(value);
                                }
                                else if (param == short.class) {
                                    args[j] = Short.parseShort(value);
                                }
                                else if (param == int.class) {
                                    args[j] = Integer.parseInt(value);
                                }
                                else if (param == long.class) {
                                    args[j] = Long.parseLong(value);
                                }
                                else if (param == float.class) {
                                    args[j] = Float.parseFloat(value);
                                }
                                else if (param == double.class) {
                                    args[j] = Double.parseDouble(value);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Error occurred trying to parse parameter, continue
                        // to next function
                        continue;
                    }
                    if (isClientsideOnly(m) && !Main.renderGraphic) {
                        log(LogLevel.ERROR, "This command can only be executed client side");
                        return true;
                    }

                    m.setAccessible(true);
                    m.invoke(exec, args);
                    return true;
                } catch (ReflectionException e) {
                    String msg = e.getMessage();
                    if (msg == null || msg.length() <= 0) {
                        msg = "Unknown Error";
                        e.printStackTrace();
                    }
                    log(LogLevel.ERROR, msg);
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    log(LogLevel.ERROR, sw.toString());
                    return false;
                }
            }
        }

        log(LogLevel.ERROR, "Bad parameters. Check your code.");
        return false;
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
