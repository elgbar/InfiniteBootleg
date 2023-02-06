package no.elg.infiniteBootleg.world.box2d;

import com.badlogic.gdx.physics.box2d.Filter;

public final class Filters {

  private Filters() {}

  public static final short GROUND_CATEGORY = 0x1;
  public static final short LIGHTS_CATEGORY = 0x2;
  public static final short ENTITY_CATEGORY = 0x4;

  // for falling blocks and players foot
  public static final Filter GR__ENTITY_FILTER = new Filter();
  // for falling blocks which blocks light
  public static final Filter GR_LI__ENTITY_FILTER = new Filter();
  // ie glass & open door
  public static final Filter EN_GR__GROUND_FILTER = new Filter();
  // ie torch
  public static final Filter GR__GROUND_FILTER = new Filter();
  // base filter for entities
  public static final Filter EN_GR__ENTITY_FILTER = new Filter();
  // closed door
  public static final Filter EN_GR_LI__GROUND_FILTER = new Filter();
  // Frozen body, should not interact with anything
  public static final Filter NON_INTERACTIVE_FILTER = new Filter();
  // Frozen body, should not interact with anything
  public static final Filter NON_INTERACTIVE__GROUND_FILTER = new Filter();

  static {
    NON_INTERACTIVE_FILTER.categoryBits = 0;
    NON_INTERACTIVE_FILTER.maskBits = 0;

    // Entity

    EN_GR__ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    EN_GR__ENTITY_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

    GR__ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    GR__ENTITY_FILTER.maskBits = GROUND_CATEGORY;

    GR_LI__ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    GR_LI__ENTITY_FILTER.maskBits = GROUND_CATEGORY | LIGHTS_CATEGORY;

    // Ground

    NON_INTERACTIVE__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    NON_INTERACTIVE__GROUND_FILTER.maskBits = 0;

    GR__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    GR__GROUND_FILTER.maskBits = GROUND_CATEGORY;

    EN_GR__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    EN_GR__GROUND_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY;

    EN_GR_LI__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    EN_GR_LI__GROUND_FILTER.maskBits = ENTITY_CATEGORY | GROUND_CATEGORY | LIGHTS_CATEGORY;
  }
}
