package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.utils.LongMap;
import no.elg.infiniteBootleg.util.CoordUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public enum Direction {
  CENTER(0, 0),
  NORTH(0, 1),
  NORTH_EAST(1, 1),
  EAST(1, 0),
  SOUTH_EAST(1, -1),
  SOUTH(0, -1),
  SOUTH_WEST(-1, -1),
  WEST(-1, 0),
  NORTH_WEST(-1, 1),
  ;

  public static final Direction[] CARDINAL = {NORTH, EAST, SOUTH, WEST};
  public static final Direction[] NON_CARDINAL = {NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};
  public static final Direction[] NEIGHBORS = {
    NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST
  };
  public static final LongMap<Direction> directionMap = new LongMap<>();
  public final int dx;
  public final int dy;

  Direction(int dx, int dy) {
    this.dx = dx;
    this.dy = dy;
  }

  static {
    for (Direction dir : Direction.values()) {
      directionMap.put(CoordUtil.compactLoc(dir.dx, dir.dy), dir);
    }
  }

  public static Direction fromAngleCardinal(double angle) {
    if (angle < 0 || angle >= 360) {
      throw new IllegalArgumentException(
          "Angle must be between 0 and 360 (both inclusive), got " + angle);
    }
    if (angle > 315 || angle <= 45) {
      return EAST;
    } else if (angle > 45 && angle <= 135) {
      return NORTH;
    } else if (angle > 135 && angle <= 225) {
      return WEST;
    } else if (angle > 225 && angle <= 315) {
      return SOUTH;
    }
    throw new IllegalArgumentException("Cannot find angle of " + angle);
  }

  public static Direction fromAngle(double angle) {
    if (angle < 0 || angle >= 360) {
      throw new IllegalArgumentException(
          "Angle must be between 0 and 360 (both inclusive), got " + angle);
    }
    if (angle > 330 || angle <= 30) {
      return EAST;
    } else if (angle > 30 && angle <= 60) {
      return NORTH_EAST;
    } else if (angle > 60 && angle <= 120) {
      return NORTH;
    } else if (angle > 120 && angle <= 150) {
      return NORTH_WEST;
    } else if (angle > 150 && angle <= 210) {
      return WEST;
    } else if (angle > 210 && angle <= 240) {
      return SOUTH_WEST;
    } else if (angle > 240 && angle <= 300) {
      return SOUTH;
    } else if (angle > 300 /* && angle <= 330*/) {
      return SOUTH_EAST;
    }
    throw new IllegalArgumentException("Cannot find angle of " + angle);
  }

  public boolean isCardinal() {
    return this == Direction.NORTH
        || this == Direction.EAST
        || this == Direction.SOUTH
        || this == Direction.WEST;
  }

  @NotNull
  public static Direction direction(int fromX, int fromY, int toX, int toY) {
    var diffX = (int) Math.signum(toX - fromX);
    var diffY = (int) Math.signum(toY - fromY);
    return directionMap.get(CoordUtil.compactLoc(diffX, diffY));
  }

  @Override
  public String toString() {
    return "Direction[" + name() + "]{" + "dx=" + dx + ", dy=" + dy + '}';
  }
}
