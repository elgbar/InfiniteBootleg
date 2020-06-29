package no.elg.infiniteBootleg.console.consoles;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.strongjoshua.console.GUIConsole;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import org.jetbrains.annotations.NotNull;

public class CGUIConsole extends GUIConsole {

    private final ConsoleHandler consoleHandler;

    public CGUIConsole(@NotNull ConsoleHandler consoleHandler, @NotNull Skin skin, boolean useMultiplexer, int keyID) {
        super(skin, useMultiplexer, keyID);
        this.consoleHandler = consoleHandler;
    }

    @Override
    public void execCommand(String command) {
        consoleHandler.execCommand(command);
    }
}
