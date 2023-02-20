package no.elg.infiniteBootleg.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import no.elg.infiniteBootleg.ClientMain;
import no.elg.infiniteBootleg.KAssets;
import no.elg.infiniteBootleg.api.Resizable;
import org.jetbrains.annotations.NotNull;

public class ScreenRenderer implements Disposable, Resizable {

  public static final int FONT_SIZE = 20;

  private final int spacing;
  private final BitmapFont font; // font managed by KAssets
  private final SpriteBatch batch;

  public ScreenRenderer() {
    font = KAssets.INSTANCE.getFont();

    spacing = (FONT_SIZE * ClientMain.SCALE) / 2;

    batch = new SpriteBatch();
    batch.setProjectionMatrix(
        new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
  }

  public void drawTop(@NotNull String text, float line) {
    try {
      font.draw(batch, text, spacing, Gdx.graphics.getHeight() - spacing * line);
    } catch (ArrayIndexOutOfBoundsException | NullPointerException ignore) {
    }
  }

  public void drawBottom(@NotNull String text, float line) {
    try {
      font.draw(batch, text, spacing, spacing * (line + 1f));
    } catch (ArrayIndexOutOfBoundsException | NullPointerException ignore) {
    }
  }

  public void begin() {
    batch.begin();
  }

  public void end() {
    batch.end();
  }

  @NotNull
  public SpriteBatch getBatch() {
    return batch;
  }

  @NotNull
  public BitmapFont getFont() {
    return font;
  }

  public void resetFontColor() {
    font.setColor(Color.WHITE);
  }

  public int getSpacing() {
    return spacing;
  }

  @Override
  public void dispose() {
    batch.dispose();
  }

  @Override
  public void resize(int width, int height) {
    batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
  }
}
