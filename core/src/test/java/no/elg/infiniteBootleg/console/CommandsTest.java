package no.elg.infiniteBootleg.console;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.render.HUDRenderer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommandsTest extends TestGraphic {

    private ConsoleHandler cmds;

    @Before
    public void before() {
        cmds = new ConsoleHandler();
    }

    @Test
    public void skyColorName() {
        assertTrue(cmds.execCommand("skycolor red"));
    }

    @Test
    public void skyColorRGBA() {
        assertTrue(cmds.execCommand("skycolor 0 0 0 0"));
    }

    @Test
    public void lights() {
        assertTrue(cmds.execCommand("lights"));
    }

    @Test
    public void reload() {
        assertTrue(cmds.execCommand("reload"));
    }

    @Test
    public void reloadForce() {
        assertTrue(cmds.execCommand("reload true"));
        assertTrue(cmds.execCommand("reload false"));
    }

    @Test
    public void fly() {
        assertTrue(cmds.execCommand("fly"));
    }

    @Test
    public void pause() {
        assertTrue(cmds.execCommand("pause"));
    }

    @Test
    public void resume() {
        assertTrue(cmds.execCommand("resume"));
    }

    @Test
    public void debug() {
        assertTrue(cmds.execCommand("debug"));
    }

    @Test
    public void tp() {
        assertTrue(cmds.execCommand("tp 1 1"));
    }

    @Test
    public void lightQuality() {
        assertTrue(cmds.execCommand("lightQuality 0"));
    }

    @Test
    public void time() {
        assertTrue(cmds.execCommand("time 45"));
    }

    @Test
    public void hud() {
        for (HUDRenderer.HUDModus modus : HUDRenderer.HUDModus.values()) {
            assertTrue(cmds.execCommand("hud " + modus.name()));
        }
    }

    @Test
    public void zoom() {
        assertTrue(cmds.execCommand("zoom 1000"));
    }
}
