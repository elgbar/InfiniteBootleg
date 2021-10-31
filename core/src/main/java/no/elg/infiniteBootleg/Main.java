package no.elg.infiniteBootleg;

import com.badlogic.gdx.ApplicationListener;
import java.awt.Toolkit;
import java.io.File;
import no.elg.infiniteBootleg.console.ConsoleHandler;
import no.elg.infiniteBootleg.console.ConsoleLogger;
import no.elg.infiniteBootleg.util.CancellableThreadScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public interface Main extends ApplicationListener {

    String EXTERNAL_FOLDER = ".infiniteBootleg" + File.separatorChar;
    String WORLD_FOLDER = EXTERNAL_FOLDER + "worlds" + File.separatorChar;
    String TEXTURES_FOLDER = "textures" + File.separatorChar;
    String FONTS_FOLDER = "fonts" + File.separatorChar;
    String TEXTURES_BLOCK_FILE = TEXTURES_FOLDER + "blocks.atlas";
    String TEXTURES_ENTITY_FILE = TEXTURES_FOLDER + "entities.atlas";
    String VERSION_FILE = "version";
    int SCALE = Toolkit.getDefaultToolkit().getScreenSize().width > 2560 ? 2 : 1;
    Object INST_LOCK = new Object();

    static @NotNull ConsoleLogger logger() {
        return inst().getConsoleLogger();
    }

    @NotNull
    static Main inst() {
        return CommonMain.inst();
    }

    @NotNull ConsoleLogger getConsoleLogger();

    @NotNull ConsoleHandler getConsole();

    @NotNull CancellableThreadScheduler getScheduler();

    boolean isNotTest();
}
