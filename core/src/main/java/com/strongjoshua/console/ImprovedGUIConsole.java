package com.strongjoshua.console;

/*
 * Copyright 2018 StrongJoshua (strongjoshua@hotmail.com)
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragScrollListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SnapshotArray;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A simple console that allows live logging, and live execution of methods, from within an
 * application. Please see the <a href="https://github.com/StrongJoshua/libgdx-inGameConsole">GitHub
 * Repository</a> for more information.
 *
 * <p>Improved by only refresh the console when it is visible. This must be done by setting the max
 * log entries to a finite number, {@code 5 000} is a good number.
 *
 * @author StrongJoshua
 * @author Elgbar
 */
public class ImprovedGUIConsole extends AbstractConsole {
  private int keyID;

  private final ConsoleDisplay display;
  private boolean hidden = true;
  private boolean usesMultiplexer;
  private InputProcessor appInput;
  private final Stage stage;
  private final CommandHistory commandHistory;
  private final CommandCompleter commandCompleter;
  private Window consoleWindow;
  private boolean hasHover;
  private Color hoverColor, noHoverColor;
  private final Vector3 stageCoords = new Vector3();
  private ScrollPane scroll;

  private final Class<? extends Table> tableClass;
  private final String tableBackground;

  private final Class<? extends TextField> textFieldClass;
  private final Class<? extends TextButton> textButtonClass;
  private final Class<? extends Label> labelClass;
  private final Class<? extends ScrollPane> scrollPaneClass;

  private Constructor<? extends Label> labelConstructor;

  /**
   * Creates the console using the default skin.<br>
   * <b>***IMPORTANT***</b> Call {@link Console#dispose()} to make your {@link InputProcessor} the
   * default processor again (this console uses a multiplexer to circumvent it). <br>
   * Default key toggle is apostrophe: '
   *
   * @see Console#dispose()
   */
  public ImprovedGUIConsole() {
    this(new Skin(Gdx.files.classpath("default_skin/uiskin.json")));
  }

  /**
   * Creates the console.<br>
   * <b>***IMPORTANT***</b> Call {@link Console#dispose()} to make your {@link InputProcessor} the
   * default processor again (this console uses a multiplexer to circumvent it). <br>
   * Default key toggle is apostrophe: '
   *
   * @param skin Uses skins for Label, TextField, and Table. Skin <b>must</b> contain a font called
   *     'default-font'.
   * @see Console#dispose()
   */
  public ImprovedGUIConsole(Skin skin) {
    this(skin, true);
  }

  /**
   * Creates the console.<br>
   * <b>***IMPORTANT***</b> Call {@link Console#dispose()} to make your {@link InputProcessor} the
   * default processor again (this console uses a multiplexer to circumvent it). <br>
   * Default key toggle is apostrophe: '
   *
   * @param useMultiplexer If internal multiplexer should be used
   * @see Console#dispose()
   */
  public ImprovedGUIConsole(boolean useMultiplexer) {
    this(new Skin(Gdx.files.classpath("default_skin/uiskin.json")), useMultiplexer);
  }

  /**
   * Creates the console.<br>
   * <b>***IMPORTANT***</b> Call {@link Console#dispose()} to make your {@link InputProcessor} the
   * default processor again (this console uses a multiplexer to circumvent it). <br>
   * Default key toggle is apostrophe: '
   *
   * @param skin Uses skins for Label, TextField, and Table. Skin <b>must</b> contain a font called
   *     'default-font'.
   * @param useMultiplexer If internal multiplexer should be used
   * @see Console#dispose()
   */
  public ImprovedGUIConsole(Skin skin, boolean useMultiplexer) {
    this(skin, useMultiplexer, Keys.APOSTROPHE);
  }

  /**
   * Creates the console.<br>
   * <b>***IMPORTANT***</b> Call {@link Console#dispose()} to make your {@link InputProcessor} the
   * default processor again (this console uses a multiplexer to circumvent it).
   *
   * @param skin Uses skins for Label, TextField, and Table. Skin <b>must</b> contain a font called
   *     'default-font'.
   * @param useMultiplexer If internal multiplexer should be used
   * @param keyID Sets the key used to open/close the console (default is apostrophe: ')
   * @see Console#dispose()
   */
  public ImprovedGUIConsole(Skin skin, boolean useMultiplexer, int keyID) {
    this(
        skin,
        useMultiplexer,
        keyID,
        Window.class,
        Table.class,
        "default-rect",
        TextField.class,
        TextButton.class,
        Label.class,
        ScrollPane.class);
  }

  public ImprovedGUIConsole(
      Skin skin,
      boolean useMultiplexer,
      int keyID,
      Class<? extends Window> windowClass,
      Class<? extends Table> tableClass,
      String tableBackground,
      Class<? extends TextField> textFieldClass,
      Class<? extends TextButton> textButtonClass,
      Class<? extends Label> labelClass,
      Class<? extends ScrollPane> scrollPaneClass) {

    this.tableClass = tableClass;
    this.tableBackground = tableBackground;
    this.textFieldClass = textFieldClass;
    this.textButtonClass = textButtonClass;
    this.labelClass = labelClass;
    try {
      labelConstructor = labelClass.getConstructor(CharSequence.class, String.class, Color.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          "Label class does not support (<String>, <String>, <Color>) constructor", e);
    }
    this.scrollPaneClass = scrollPaneClass;

    this.keyID = keyID;
    stage = new Stage();
    display = new ImprovedGUIConsole.ConsoleDisplay(skin);
    commandHistory = new CommandHistory();
    commandCompleter = new CommandCompleter();
    logToSystem = false;

    usesMultiplexer = useMultiplexer;
    if (useMultiplexer) {
      resetInputProcessing();
    }

    display.root.pad(4);
    display.root.padTop(22);
    display.root.setFillParent(true);
    display.showSubmit(false);

    try {
      consoleWindow =
          windowClass.getConstructor(String.class, Skin.class).newInstance("Console", skin);
    } catch (Exception e) {
      try {
        consoleWindow = windowClass.getConstructor(String.class).newInstance("Console");
      } catch (Exception e2) {
        throw new RuntimeException(
            "Window class does not support either (<String>, <Skin>) or (<String>) constructors.");
      }
    }
    consoleWindow.setMovable(true);
    consoleWindow.setResizable(true);
    consoleWindow.setKeepWithinStage(true);
    consoleWindow.addActor(display.root);
    consoleWindow.setTouchable(Touchable.disabled);

    hoverColor = new Color(1, 1, 1, 1);
    noHoverColor = new Color(1, 1, 1, 1);

    stage.addListener(new ImprovedGUIConsole.DisplayListener());
    stage.addActor(consoleWindow);
    stage.setKeyboardFocus(display.root);

    setSizePercent(100, 60);
    setPositionPercent(50, 50);
  }

  @Override
  public void setMaxEntries(int numEntries) {
    if (numEntries > 0 || numEntries == UNLIMITED_ENTRIES) {
      log.setMaxEntries(numEntries);
    } else {
      throw new IllegalArgumentException(
          "Maximum entries must be greater than 0 or use Console.UNLIMITED_ENTRIES.");
    }
  }

  @Override
  public void clear() {
    log.getLogEntries().clear();
    display.refresh();
  }

  @Override
  public void setSize(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Pixel size must be greater than 0.");
    }
    consoleWindow.setSize(width, height);
  }

  @Override
  public void setSizePercent(float wPct, float hPct) {
    if (wPct <= 0 || hPct <= 0) {
      throw new IllegalArgumentException("Size percentage must be greater than 0.");
    }
    if (wPct > 100 || hPct > 100) {
      throw new IllegalArgumentException("Size percentage cannot be greater than 100.");
    }
    float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
    consoleWindow.setSize(w * wPct / 100.0f, h * hPct / 100.0f);
  }

  @Override
  public void setPosition(int x, int y) {
    consoleWindow.setPosition(x, y);
  }

  @Override
  public void setPositionPercent(float xPosPct, float yPosPct) {
    if (xPosPct > 100 || yPosPct > 100) {
      throw new IllegalArgumentException(
          "Error: The console would be drawn outside of the screen.");
    }
    float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
    consoleWindow.setPosition(w * xPosPct / 100.0f, h * yPosPct / 100.0f);
  }

  @Override
  public void resetInputProcessing() {
    usesMultiplexer = true;
    appInput = Gdx.input.getInputProcessor();
    if (appInput != null) {
      if (hasStage(appInput)) {
        log("Console already added to input processor!", LogLevel.ERROR);
        Gdx.app.log("Console", "Already added to input processor!");
        return;
      }
      InputMultiplexer multiplexer = new InputMultiplexer();
      multiplexer.addProcessor(stage);
      multiplexer.addProcessor(appInput);
      Gdx.input.setInputProcessor(multiplexer);
    } else {
      Gdx.input.setInputProcessor(stage);
    }
  }

  /**
   * Compares the given processor to the console's stage. If given a multiplexer, it is iterated
   * through recursively to check all of the multiplexer's processors for comparison.
   *
   * @return processor == this.stage
   */
  private boolean hasStage(InputProcessor processor) {
    if (!(processor instanceof InputMultiplexer im)) {
      return processor == stage;
    }
    SnapshotArray<InputProcessor> ips = im.getProcessors();
    for (InputProcessor ip : ips) {
      if (hasStage(ip)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public InputProcessor getInputProcessor() {
    return stage;
  }

  @Override
  public void draw() {
    if (disabled) {
      return;
    }
    stage.act();

    if (hidden) {
      return;
    }
    stage.draw();
  }

  @Override
  public void refresh() {
    refresh(true);
  }

  @Override
  public void refresh(boolean retain) {
    float oldWPct = 0, oldHPct = 0, oldXPosPct = 0, oldYPosPct = 0;
    if (retain) {
      oldWPct = consoleWindow.getWidth() / stage.getWidth() * 100;
      oldHPct = consoleWindow.getHeight() / stage.getHeight() * 100;
      oldXPosPct = consoleWindow.getX() / stage.getWidth() * 100;
      oldYPosPct = consoleWindow.getY() / stage.getHeight() * 100;
    }
    int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
    stage.getViewport().setWorldSize(width, height);
    stage.getViewport().update(width, height, true);
    if (retain) {
      setSizePercent(oldWPct, oldHPct);
      setPositionPercent(oldXPosPct, oldYPosPct);
    }
  }

  @Override
  public void log(String msg, LogLevel level) {
    super.log(msg, level);
    display.refresh();
  }

  @Override
  public void setDisabled(boolean disabled) {
    if (disabled) {
      display.setHidden(true);
    }
    this.disabled = disabled;
  }

  @Override
  public int getDisplayKeyID() {
    return keyID;
  }

  @Override
  public void setDisplayKeyID(int code) {
    if (code == Keys.ENTER) {
      return;
    }
    keyID = code;
  }

  @Override
  public boolean hitsConsole(float screenX, float screenY) {
    if (disabled || hidden) {
      return false;
    }
    stage.getCamera().unproject(stageCoords.set(screenX, screenY, 0));
    return stage.hit(stageCoords.x, stageCoords.y, true) != null;
  }

  @Override
  public void dispose() {
    if (usesMultiplexer && appInput != null) {
      Gdx.input.setInputProcessor(appInput);
    }
    stage.dispose();
  }

  @Override
  public boolean isVisible() {
    return !hidden;
  }

  @Override
  public void setVisible(boolean visible) {
    display.setHidden(!visible);
  }

  @Override
  public void select() {
    display.select();
  }

  @Override
  public void deselect() {
    display.deselect();
  }

  @Override
  public void setTitle(String title) {
    consoleWindow.getTitleLabel().setText(title);
  }

  private void refreshWindowColor() {
    consoleWindow.setColor(hasHover ? hoverColor : noHoverColor);
  }

  @Override
  public void setHoverAlpha(float alpha) {
    hoverColor.a = alpha;
    refreshWindowColor();
  }

  @Override
  public void setNoHoverAlpha(float alpha) {
    noHoverColor.a = alpha;
    refreshWindowColor();
  }

  @Override
  public void setHoverColor(Color color) {
    hoverColor = color;
    refreshWindowColor();
  }

  @Override
  public void setNoHoverColor(Color color) {
    noHoverColor = color;
    refreshWindowColor();
  }

  @Override
  public void enableSubmitButton(boolean enable) {
    display.showSubmit(enable);
  }

  @Override
  public void setSubmitText(String text) {
    display.setSubmitText(text);
  }

  @Override
  public Window getWindow() {
    return consoleWindow;
  }

  public class ConsoleDisplay {
    private final Table root;
    private final Table logEntries;
    private final TextField input;
    private TextButton submit;
    private final Skin skin;
    private final Array<Label> labels;
    private final String fontName;
    private boolean selected = true;
    private final ConsoleContext context;
    private final Cell<TextButton> submitCell;

    ConsoleDisplay(Skin skin) {
      try {
        root = tableClass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException("Table class does not support empty constructor.");
      }
      this.skin = skin;
      context = new ConsoleContext(tableClass, labelClass, skin, tableBackground);

      if (skin.has("console-font", BitmapFont.class)) fontName = "console-font";
      else fontName = "default-font";

      TextFieldStyle tfs = skin.get(TextFieldStyle.class);
      tfs.font = skin.getFont(fontName);

      labels = new Array<>();

      try {
        logEntries = tableClass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException("Table class does not support empty constructor.");
      }

      try {
        input =
            textFieldClass.getConstructor(String.class, TextFieldStyle.class).newInstance("", tfs);
      } catch (Exception e) {
        throw new RuntimeException(
            "TextField class does not support (<String>, <Skin>) constructor.");
      }
      input.setTextFieldListener(new ImprovedGUIConsole.FieldListener());

      try {
        submit =
            textButtonClass.getConstructor(String.class, Skin.class).newInstance("Submit", skin);
      } catch (Exception e) {
        try {
          submit = textButtonClass.getConstructor(String.class).newInstance("Submit");
        } catch (Exception e2) {
          throw new RuntimeException(
              "TextButton class does not support either (<String>, <Skin>) or (<String>)"
                  + " constructors.");
        }
      }
      submit.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              submit();
            }
          });

      try {
        scroll =
            scrollPaneClass.getConstructor(Actor.class, Skin.class).newInstance(logEntries, skin);
      } catch (Exception e) {
        try {
          scroll = scrollPaneClass.getConstructor(Actor.class).newInstance(logEntries);
        } catch (Exception e2) {
          throw new RuntimeException(
              "ScrollPane class does not support either (<Actor>, <Skin>) or (<Actor>)"
                  + " constructors.");
        }
      }
      scroll.setFadeScrollBars(false);
      scroll.setScrollbarsOnTop(false);
      scroll.setOverscroll(false, false);
      scroll.addListener(
          new DragScrollListener(scroll) {
            @Override
            public boolean scrolled(
                InputEvent event, float x, float y, float amountX, float amountY) {
              return super.scrolled(event, x, y, amountX, amountY);
            }
          });

      root.add(scroll).colspan(2).expand().fill().pad(4).row();
      root.add(input).expandX().fillX().pad(4);
      submitCell = root.add(submit);
      root.addListener(new ImprovedGUIConsole.KeyListener(input));
    }

    private Label getLogEntryLabel(int index) {
      Label label;
      // recycle the labels, so we don't create new ones every refresh
      if (labels.size > index) {
        label = labels.get(index);
      } else {
        try {
          label = labelConstructor.newInstance("", fontName, LogLevel.DEFAULT.getColor());
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
          throw new RuntimeException(
              "Failed to create new instance of label class " + labelClass.getSimpleName(), e);
        }
        label.setWrap(true);
        labels.add(label);
        label.addListener(
            new ImprovedGUIConsole.LogListener(label, skin.getDrawable(tableBackground)));
      }
      return label;
    }

    void refresh() {
      if (disabled || hidden) {
        return;
      }
      Array<LogEntry> entries = log.getLogEntries();
      logEntries.clear();

      // expand first so labels start at the bottom
      logEntries.add().expand().fill().row();
      int size = entries.size;
      for (int i = 0; i < size; i++) {
        LogEntry le = entries.get(i);
        Label label = getLogEntryLabel(i);

        label.setText(" " + le.toConsoleString());
        label.setColor(le.getColor());
        logEntries.add(label).expandX().fillX().top().left().row();
      }
      try {
        scroll.validate();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      scroll.setScrollPercentY(1);
    }

    private void setHidden(boolean hide) {
      hidden = hide;
      if (hidden) {
        consoleWindow.setTouchable(Touchable.disabled);
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
      } else {
        input.setText("");
        consoleWindow.setTouchable(Touchable.enabled);
        if (selected) {
          select();
        }
        display.refresh();
      }
    }

    void select() {
      selected = true;
      if (!hidden) {
        stage.setKeyboardFocus(input);
        stage.setScrollFocus(scroll);
      }
    }

    void deselect() {
      selected = false;
      stage.setKeyboardFocus(null);
      stage.setScrollFocus(null);
    }

    void openContext(Label label, float x, float y) {
      context.setLabel(label);
      context.setPosition(x, y);
      context.setStage(stage);
    }

    void closeContext() {
      context.remove();
    }

    boolean submit() {
      String s = input.getText();
      if (s.isEmpty() || s.split(" ").length == 0) {
        return false;
      }
      if (exec != null) {
        commandHistory.store(s);
        execCommand(s);
      } else {
        log(
            "No command executor has been set. "
                + "Please call setCommandExecutor for this console in your code and restart.",
            LogLevel.ERROR);
      }
      input.setText("");
      return true;
    }

    void showSubmit(boolean show) {
      submit.setVisible(show);
      submitCell.size(show ? submit.getPrefWidth() : 0, show ? submit.getPrefHeight() : 0);
    }

    void setSubmitText(String text) {
      submit.setText(text);
      showSubmit(submit.isVisible());
    }
  }

  private class FieldListener implements TextFieldListener {
    @Override
    public void keyTyped(TextField textField, char c) {
      if (("" + c).equalsIgnoreCase(Keys.toString(keyID))) {
        String s = textField.getText();
        textField.setText(s.substring(0, s.length() - 1));
      }
    }
  }

  private class KeyListener extends InputListener {
    private final TextField input;

    protected KeyListener(TextField tf) {
      input = tf;
    }

    @Override
    public boolean keyDown(InputEvent event, int keycode) {
      if (disabled) return false;

      // reset command completer because input string may have changed
      if (keycode != Keys.TAB) {
        commandCompleter.reset();
      }

      if (keycode == Keys.ENTER && !hidden) {
        commandHistory
            .getNextCommand(); // Makes up arrow key repeat the same command after pressing enter
        return display.submit();
      } else if (keycode == Keys.UP && !hidden) {
        input.setText(commandHistory.getPreviousCommand());
        input.setCursorPosition(input.getText().length());
        return true;
      } else if (keycode == Keys.DOWN && !hidden) {
        input.setText(commandHistory.getNextCommand());
        input.setCursorPosition(input.getText().length());
        return true;
      } else if (keycode == Keys.TAB && !hidden) {
        String s = input.getText();
        if (s.isEmpty()) {
          return false;
        }
        if (commandCompleter.isNew()) {
          commandCompleter.set(exec, s);
        }
        input.setText(commandCompleter.next());
        input.setCursorPosition(input.getText().length());
        return true;
      }
      return false;
    }
  }

  private class DisplayListener extends InputListener {
    @Override
    public boolean keyDown(InputEvent event, int keycode) {
      if (disabled) return false;
      if (keycode == keyID) {
        display.setHidden(!hidden);
        return true;
      }
      return false;
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
      if (pointer != -1) return;
      hasHover = true;
      refreshWindowColor();
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
      if (pointer != -1) return;
      hasHover = false;
      refreshWindowColor();
    }
  }

  private class LogListener extends ClickListener {
    private final Label self;
    private final Drawable highlighted;

    LogListener(Label label, Drawable highlighted) {
      self = label;
      this.highlighted = highlighted;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
      Vector2 pos = self.localToStageCoordinates(new Vector2(x, y));
      display.openContext(self, pos.x, pos.y);
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
      if (pointer != -1) return;
      self.getStyle().background = highlighted;
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
      if (pointer != -1) return;
      self.getStyle().background = null;
    }
  }
}
