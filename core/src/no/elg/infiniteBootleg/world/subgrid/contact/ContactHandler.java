package no.elg.infiniteBootleg.world.subgrid.contact;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactListener;
import org.jetbrains.annotations.NotNull;

public interface ContactHandler {

    /**
     * {@link Contact#getFixtureB()} is this entity
     * {@link Contact#getFixtureA()} is the a object collided with
     *
     * @param type
     *     The type of contact
     * @param contact
     *     Contact made
     *
     * @see ContactListener
     */
    void contact(@NotNull ContactType type, @NotNull Contact contact);
}
