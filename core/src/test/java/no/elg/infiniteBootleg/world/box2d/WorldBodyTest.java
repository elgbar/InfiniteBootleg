package no.elg.infiniteBootleg.world.box2d;

import static no.elg.infiniteBootleg.world.box2d.WorldBody.WORLD_MOVE_OFFSET_THRESHOLD;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Elg
 */
public class WorldBodyTest {

    @Test
    public void shift_when_going_outside_threshold__no_offset() {
        assertEquals(0f, WorldBody.calculateShift(WORLD_MOVE_OFFSET_THRESHOLD - 1f), 0f);
        assertEquals(0f, WorldBody.calculateShift(WORLD_MOVE_OFFSET_THRESHOLD), 0f);
        assertEquals(0f, WorldBody.calculateShift(-WORLD_MOVE_OFFSET_THRESHOLD), 0f);
        assertEquals(-WORLD_MOVE_OFFSET_THRESHOLD, WorldBody.calculateShift(WORLD_MOVE_OFFSET_THRESHOLD + 1f), 0f);
    }

    @Test
    public void shift_multiple_thresholds() {
        assertEquals(-(WORLD_MOVE_OFFSET_THRESHOLD * 72f), WorldBody.calculateShift(WORLD_MOVE_OFFSET_THRESHOLD * 72f + 1f), 0f);
        assertEquals(WORLD_MOVE_OFFSET_THRESHOLD * 15f, WorldBody.calculateShift(-(WORLD_MOVE_OFFSET_THRESHOLD * 15f) - 1f), 0f);
    }
}
