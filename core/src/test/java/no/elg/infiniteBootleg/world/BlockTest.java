package no.elg.infiniteBootleg.world;

import no.elg.infiniteBootleg.TestGraphic;
import no.elg.infiniteBootleg.world.generator.EmptyChunkGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Elg
 */
public class BlockTest extends TestGraphic {


    @Test
    public void correctType() {
        for (Material mat : Material.values()) {
            Block b = mat.create(0, 0, new World(new EmptyChunkGenerator()));
            Assert.assertEquals(mat, b.getMaterial());
        }

    }
}
