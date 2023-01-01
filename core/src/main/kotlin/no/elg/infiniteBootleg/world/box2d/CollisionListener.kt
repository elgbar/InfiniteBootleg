package no.elg.infiniteBootleg.world.box2d

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.Contact
import no.elg.infiniteBootleg.world.subgrid.Entity
import no.elg.infiniteBootleg.world.subgrid.contact.ContactHandler
import no.elg.infiniteBootleg.world.subgrid.contact.ContactType

class CollisionListener : ContactHandler {

  /**
   * Must be called while synchronized under BOX2D_LOCK
   *
   * @param type    The type of contact
   * @param contact Contact made
   */
  override fun contact(type: ContactType, contact: Contact) {
    if (contact.fixtureA.filterData.categoryBits == Filters.GROUND_CATEGORY) {
      if (type == ContactType.BEGIN_CONTACT) {
        // newest pos is needed to accurately check if this is on ground
        updatePos()
        val y = MathUtils.floor(posCache.y - getHalfBox2dHeight() - Entity.GROUND_CHECK_OFFSET)
        val leftX = MathUtils.ceil(posCache.x - 2 * getHalfBox2dWidth())
        val middleX = MathUtils.floor(posCache.x - getHalfBox2dWidth())
        val rightX = MathUtils.ceil(posCache.x - Entity.GROUND_CHECK_OFFSET)
        var detected = 0
        if (!world.isAirBlock(leftX, y)) {
          detected++
        }
        if (!world.isAirBlock(middleX, y)) {
          detected++
        }
        if (!world.isAirBlock(rightX, y)) {
          detected++
        }
        if (detected > 0) {
          groundContacts++
        }
      } else if (type == ContactType.END_CONTACT) {
        groundContacts--
        if (groundContacts < 0) {
          groundContacts = 0
        }
      }
    }
  }
}
