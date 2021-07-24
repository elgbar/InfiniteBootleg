package no.elg.infiniteBootleg.console.consoles;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.strongjoshua.console.GUIConsole;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.HelpfulConsoleHelpUtil;
import org.jetbrains.annotations.NotNull;

public class CGUIConsole extends GUIConsole {

    private final ConsoleHandler consoleHandler;

    public CGUIConsole(@NotNull ConsoleHandler consoleHandler, @NotNull Skin skin, boolean useMultiplexer, int keyID) {
        super(skin, useMultiplexer, keyID, VisWindow.class, VisTable.class, "default-pane", TextField.class, VisTextButton.class, VisLabel.class,
              VisScrollPane.class);
        this.consoleHandler = consoleHandler;
    }

    @Override
    public void printHelp(String command) {
        HelpfulConsoleHelpUtil.printHelp(this, exec, command);
    }

    @Override
    public void printCommands() {
        HelpfulConsoleHelpUtil.printCommands(this, exec);
    }

    @Override
    public void execCommand(String command) {
        consoleHandler.execCommand(command);
    }
}
