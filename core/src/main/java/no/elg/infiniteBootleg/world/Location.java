package no.elg.infiniteBootleg.world;

import com.badlogic.gdx.math.Vector2;
import no.elg.infiniteBootleg.protobuf.ProtoWorld;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable 2D vector in integer space
 *
 * @author Elg
 */
public class Location {

    public final int x;
    public final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Location relative(int x, int y, @NotNull Direction dir) {
        return new Location(x + dir.dx, y + dir.dy);
    }

    public Location scl(int x, int y) {
        return new Location(this.x * x, this.y * y);
    }

    public double dist(@NotNull Location loc) {
        return Math.sqrt(distCubed(loc));
    }

    public long distCubed(@NotNull Location loc) {
        return distCubed(x, y, loc.x, loc.y);
    }

    public static long distCubed(int x1, int y1, int x2, int y2) {
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    }

    public Vector2 toVector2() {
        return new Vector2(x, y);
    }

    public Location relative(@NotNull Direction dir) {
        return new Location(x + dir.dx, y + dir.dy);
    }

    public static Location fromVector2i(@NotNull ProtoWorld.Vector2i vector2i) {
        return new Location(vector2i.getX(), vector2i.getY());
    }

    public ProtoWorld.Vector2i toVector2i() {
        return ProtoWorld.Vector2i.newBuilder().setX(x).setY(y).build();
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o instanceof Location location) {
            return y == location.y && x == location.x;
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
