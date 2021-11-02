package no.elg.infiniteBootleg.world.render;

/** @author Elg */
public final class ChunksInView {

  public int horizontalStart;
  public int horizontalEnd;
  public int verticalStart;
  public int verticalEnd;

  ChunksInView() {}

  public int getHorizontalLength() {
    return horizontalEnd - horizontalStart;
  }

  public int getVerticalLength() {
    return verticalEnd - verticalStart;
  }
}
