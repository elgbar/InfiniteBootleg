package no.elg.infiniteBootleg;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Elg
 */
public class ProgramArgsTest extends TestGraphic {

    @Test
    public void headless() {
        Main.renderGraphic = true;
        ProgramArgs.executeArgs(new String[] {"-headless"});
        assertFalse(Main.renderGraphic);
    }

    @Test
    public void noLoad() {
        Main.loadWorldFromDisk = true;
        ProgramArgs.executeArgs(new String[] {"-no_load"});
        assertFalse(Main.loadWorldFromDisk);
    }

    @Test
    public void worldSeed() {
        ProgramArgs.executeArgs(new String[] {"-world_seed=test123"});
        assertEquals("test123".hashCode(), Main.worldSeed);
    }

    @Test
    public void handlesRandomCasing() {
        Main.renderGraphic = true;
        ProgramArgs.executeArgs(new String[] {"-hEadlESS"});
        assertFalse(Main.renderGraphic);
    }

    @Test
    public void handleInvalidArgs() {
        ProgramArgs.executeArgs(new String[] {"-hue", "he"});
    }

    @Test
    public void threads() {
        ProgramArgs pa = new ProgramArgs(new String[0]);
        assertFalse(pa.threads("0"));
        assertTrue(pa.threads("1"));
        assertTrue(pa.threads("32"));
        assertFalse(pa.threads("-32"));
        assertFalse(pa.threads("aaa"));
        assertFalse(pa.threads(""));
        pa.dispose();
    }
}
