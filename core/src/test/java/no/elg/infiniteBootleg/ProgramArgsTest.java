package no.elg.infiniteBootleg;

import no.elg.infiniteBootleg.args.ProgramArgs;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Elg
 */
public class ProgramArgsTest extends TestGraphic {

    @Test
    public void headless() {
        Main.renderGraphic = true;
        ProgramArgs.executeArgs(new String[] {"-headless"});
        Assert.assertFalse(Main.renderGraphic);
    }

    @Test
    public void noLoad() {
        Main.loadWorldFromDisk = true;
        ProgramArgs.executeArgs(new String[] {"-no_load"});
        Assert.assertFalse(Main.loadWorldFromDisk);
    }

    @Test
    public void worldSeed() {
        ProgramArgs.executeArgs(new String[] {"-world_seed=test123"});
        Assert.assertEquals("test123".hashCode(), Main.worldSeed);
    }

    @Test
    public void handlesRandomCasing() {
        Main.renderGraphic = true;
        ProgramArgs.executeArgs(new String[] {"-hEadlESS"});
        Assert.assertFalse(Main.renderGraphic);
    }

    @Test
    public void handleInvalidArgs() {
        ProgramArgs.executeArgs(new String[] {"-hue", "he"});
    }

    @Test
    public void threads() {
        ProgramArgs pa = new ProgramArgs(new String[0]);
        Assert.assertTrue(pa.threads("0"));
        Assert.assertTrue(pa.threads("1"));
        Assert.assertTrue(pa.threads("32"));
        Assert.assertFalse(pa.threads("-32"));
        Assert.assertFalse(pa.threads("aaa"));
        Assert.assertFalse(pa.threads(""));
        pa.dispose();
    }
}
