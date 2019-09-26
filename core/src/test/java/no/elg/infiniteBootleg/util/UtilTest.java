package no.elg.infiniteBootleg.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Elg
 */
public class UtilTest {

    @Test
    public void isBetweenFloat() {
        assertTrue(Util.isBetween(0, 0, 1));
        assertFalse(Util.isBetween(0, 1, 1));
        assertFalse(Util.isBetween(1010, -1, 9999));
        assertFalse(Util.isBetween(1010, 99999, 9999));
        assertTrue(Util.isBetween(1010, 2000, 9999));
    }

    @Test
    public void hasSuperClass() {
        assertTrue(Util.hasSuperClass(Object.class, Object.class));
        assertFalse(Util.hasSuperClass(Object.class, String.class));
        assertTrue(Util.hasSuperClass(String.class, Object.class));
    }
}
