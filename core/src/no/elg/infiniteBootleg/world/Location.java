package no.elg.infiniteBootleg.world;

import org.jetbrains.annotations.NotNull;

/**
 * @author Elg
 */
public class Location {

    public final int x;
    public final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Location mult(int x, int y) {
        return new Location(this.x * x, this.y * y);
    }

    public long distCubed(@NotNull Location loc) {
        return (loc.x - x) * (loc.x - x) + (loc.y - y) * (loc.y - y);
    }

    public double dist(@NotNull Location loc) {
        return Math.sqrt(distCubed(loc));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Location location = (Location) o;
        return y == location.y && x == location.x;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
