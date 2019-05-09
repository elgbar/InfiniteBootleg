package no.elg.infiniteBootleg.world;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Location location = (Location) o;

        if (x != location.x) { return false; }
        return y == location.y;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
