package no.elg.infiniteBootleg.args;

import no.elg.infiniteBootleg.Main;
import no.elg.infiniteBootleg.TestGraphic;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

public class ProgramArgsTest extends TestGraphic {

    @Test
    public void headless() {
        Main.renderGraphic = true;
        new ProgramArgs(new String[] {"--headless"});
        Assert.assertFalse(Main.renderGraphic);
    }

    @Test
    public void noLoad() {
        Main.loadWorldFromDisk = true;
        new ProgramArgs(new String[] {"--no_load"});
        Assert.assertFalse(Main.loadWorldFromDisk);
    }

    @Test
    public void worldSeed() {
        new ProgramArgs(new String[] {"--world_seed=test123"});
        Assert.assertEquals("test123".hashCode(), Main.worldSeed);
    }

    @Test
    public void handlesRandomCasing() {
        Main.renderGraphic = true;
        new ProgramArgs(new String[] {"--hEadlESS"});
        Assert.assertFalse(Main.renderGraphic);
    }

    @Test
    public void handleInvalidArgs() {
        new ProgramArgs(new String[] {"-hue", "he"});
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

    @Test
    public void noDuplicateAltSwitches() {

        Map<Character, String> known = new HashMap<>();
        for (Method method : ProgramArgs.class.getDeclaredMethods()) {
            Argument a = method.getAnnotation(Argument.class);
            if (a != null) {
                if (a.alt() != '\0' && known.containsKey(a.alt())) {
                    String otherMethod = known.get(a.alt());
                    fail("Duplicate alternative switch found! Both the method '" + method.getName() + "' and '" +
                         otherMethod + "' have the switch '" + a.alt() + "'");
                }
                known.put(a.alt(), method.getName());
            }
        }

    }
}
