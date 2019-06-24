package no.elg.infiniteBootleg.world.subgrid.box2d;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ContactHandler {

    /**
     * @param type
     *     The type of contact
     * @param contact
     *     Contact made
     * @param data
     *     Extra data
     *
     * @see ContactListener
     * @see ContactType#getSecondClass()
     */
    void contact(@NotNull ContactType type, @NotNull Contact contact, @Nullable Object data);
}
