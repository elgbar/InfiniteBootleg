package no.elg.infiniteBootleg.world.subgrid.box2d;

import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;

public enum ContactType {

    BEGIN_CONTACT(null),
    END_CONTACT(null),
    PRE_SOLVE(Manifold.class),
    POST_SOLVE(ContactImpulse.class);

    private Class<?> secondClass;

    ContactType(Class<?> secondClass) {

        this.secondClass = secondClass;
    }

    public Class<?> getSecondClass() {
        return secondClass;
    }
}
