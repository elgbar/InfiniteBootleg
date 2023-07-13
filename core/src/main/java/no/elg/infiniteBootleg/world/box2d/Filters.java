package no.elg.infiniteBootleg.world.box2d;

import com.badlogic.gdx.physics.box2d.Filter;

public final class Filters {

  private Filters() {}

  public static final short GROUND_CATEGORY = 0x1;
  public static final short FALLING_BLOCK_CATEGORY = 0x2;
  public static final short ENTITY_CATEGORY = 0x4;

  public static final Filter NON_INTERACTIVE_FILTER = new Filter();

  public static final Filter NON_INTERACTIVE__GROUND_FILTER = new Filter();
  public static final Filter GR__GROUND_FILTER = new Filter();
  public static final Filter EN__GROUND_FILTER = new Filter();
  public static final Filter GR_FB__GROUND_FILTER = new Filter();
  public static final Filter GR_EN__GROUND_FILTER = new Filter();
  public static final Filter GR_FB_EN__GROUND_FILTER = new Filter();

  public static final Filter GR__ENTITY_FILTER = new Filter();
  public static final Filter GR_FB__ENTITY_FILTER = new Filter();
  public static final Filter GR_EN_ENTITY_FILTER = new Filter();

  public static final Filter GR_FB__FALLING_BLOCK_FILTER = new Filter();

  static {
    NON_INTERACTIVE_FILTER.categoryBits = 0;
    NON_INTERACTIVE_FILTER.maskBits = 0;

    // Ground

    NON_INTERACTIVE__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    NON_INTERACTIVE__GROUND_FILTER.maskBits = 0;

    GR__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    GR__GROUND_FILTER.maskBits = GROUND_CATEGORY;

    EN__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    EN__GROUND_FILTER.maskBits = ENTITY_CATEGORY;

    GR_FB__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    GR_FB__GROUND_FILTER.maskBits = GROUND_CATEGORY | FALLING_BLOCK_CATEGORY;

    GR_EN__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    GR_EN__GROUND_FILTER.maskBits = GROUND_CATEGORY | ENTITY_CATEGORY;

    GR_FB_EN__GROUND_FILTER.categoryBits = GROUND_CATEGORY;
    GR_FB_EN__GROUND_FILTER.maskBits = GROUND_CATEGORY | FALLING_BLOCK_CATEGORY | ENTITY_CATEGORY;

    // Entity

    GR__ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    GR__ENTITY_FILTER.maskBits = GROUND_CATEGORY;

    GR_FB__ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    GR_FB__ENTITY_FILTER.maskBits = GROUND_CATEGORY | FALLING_BLOCK_CATEGORY;

    GR_EN_ENTITY_FILTER.categoryBits = ENTITY_CATEGORY;
    GR_EN_ENTITY_FILTER.maskBits = GROUND_CATEGORY | ENTITY_CATEGORY;

    // Falling block

    GR_FB__FALLING_BLOCK_FILTER.categoryBits = FALLING_BLOCK_CATEGORY;
    GR_FB__FALLING_BLOCK_FILTER.maskBits = GROUND_CATEGORY | FALLING_BLOCK_CATEGORY;
  }
}
