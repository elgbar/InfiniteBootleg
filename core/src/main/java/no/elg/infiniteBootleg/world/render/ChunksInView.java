package no.elg.infiniteBootleg.world.render;

/** @author Elg */
public final class ChunksInView {

  public int horizontalStart;
  public int horizontalEnd;
  public int verticalStart;
  public int verticalEnd;

  public int getHorizontalLength() {
    return horizontalEnd - horizontalStart;
  }

  public int getVerticalLength() {
    return verticalEnd - verticalStart;
  }

  public int getChunksInView() {
    return getHorizontalLength() * getVerticalLength();
  }

  public boolean isOutOfView(int chunkX, int chunkY) {
    return chunkX < horizontalStart
        || chunkX >= horizontalEnd
        || chunkY < verticalStart
        || chunkY >= verticalEnd;
  }
}
