package no.elg.infiniteBootleg.world.generator;

import no.elg.infiniteBootleg.world.Material;
import org.junit.Test;

import static no.elg.infiniteBootleg.world.Chunk.CHUNK_HEIGHT;

/**
 * @author Elg
 */
public class FlatWorldGeneratorTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullInConstructor() {
        new FlatWorldGenerator(new Material[CHUNK_HEIGHT]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLayerSize() {
        new FlatWorldGenerator(new Material[0]);
    }
}
