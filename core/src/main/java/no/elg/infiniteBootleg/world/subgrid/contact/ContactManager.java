package no.elg.infiniteBootleg.world.subgrid.contact;

import static no.elg.infiniteBootleg.world.GlobalLockKt.BOX2D_LOCK;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import no.elg.infiniteBootleg.world.subgrid.Entity;

public class ContactManager implements ContactListener {

  private void handleContact(ContactType type, Contact contact) {
    // Must be within a box2d lock to make sure the contact is valid
    synchronized (BOX2D_LOCK) {
      Body body = contact.getFixtureB().getBody();
      Object data = body.getUserData();
      if (data instanceof Entity entity && !entity.isDisposed()) {
        entity.contact(type, contact);
      }
    }
  }

  @Override
  public void beginContact(Contact contact) {
    handleContact(ContactType.BEGIN_CONTACT, contact);
  }

  @Override
  public void endContact(Contact contact) {
    handleContact(ContactType.END_CONTACT, contact);
  }

  @Override
  public void preSolve(Contact contact, Manifold oldManifold) {}

  @Override
  public void postSolve(Contact contact, ContactImpulse impulse) {}
}
