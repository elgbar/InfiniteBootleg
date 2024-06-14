package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.elg.infiniteBootleg.main.Main;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

  private static final Logger logger = LoggerFactory.getLogger(Util.class);

  public static final String DEFAULT_HASH = "UNKNOWN";
  public static final int DEFAULT_COMMIT_COUNT = 0;
  public static final String FALLBACK_VERSION = DEFAULT_HASH;
  public static final int CIRCLE_DEG = 360;
  public static final String RELATIVE_TIME = "relative";
  private static final DateTimeFormatter ZULU_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm'Z'");

  /**
   * The key in the map is a pair of the character given before an equal sign (=) or end of string
   * and a boolean that is {@code true} if the key started with two dash (-) as prefix. Values in
   * the map are {@code String}s containing the substring from after (not including the first) equal
   * sign to the end of the string.
   *
   * <h2>Example</h2>
   *
   * <p>Given {@code new String{"--key=value" "--toggle"}} will return {@code mapOf(new
   * Pair("key",true) to "value", new Pair("toggle", true), null)}
   *
   * @param args The system args to interpret
   * @return A Map of the interpreted args
   */
  @NotNull
  public static Map<@NotNull Pair<@NotNull String, @NotNull Boolean>, @Nullable String>
  interpreterArgs(String[] args) {
    Map<Pair<String, Boolean>, String> argsMap = new HashMap<>();

    for (String arg : args) {
      if (!arg.startsWith("-")) {
        logger.error("Failed to interpret argument " + arg);
      } else {
        // we only care about the first equals sign, the rest is a part of the value
        int equal = arg.indexOf('=');

        boolean doubleDash = arg.startsWith("--");
        int cutoff = doubleDash ? 2 : 1;

        List<String> inArgs = new ArrayList<>();
        String key = equal == -1 ? arg.substring(cutoff) : arg.substring(cutoff, equal);
        if (doubleDash) {
          inArgs.add(key);
        } else {
          // If this is a single dash (ie switch) then each char before the equal is a switch on its
          // own
          for (char c : key.toCharArray()) {
            inArgs.add(String.valueOf(c));
          }
        }

        for (int i = 0, size = inArgs.size(); i < size; i++) {
          String inArg = inArgs.get(i);
          // if there is no equal sign there is no value
          String val = null;
          if (equal != -1 && i >= size - 1) {
            // find the key and value from the index of the first equal sign, but do not include it
            // in the
            // key or value
            val = arg.substring(equal + 1);
          }
          argsMap.put(new ImmutablePair<>(inArg, doubleDash), val);
        }
      }
    }
    return argsMap;
  }

  public static String getVersion() {
    String calcHash =
      "#"
        + commitCount()
        + "-"
        + getLastGitCommitID()
        + "@"
        + getLastCommitDate("iso8601-strict");
    String savedHash;
    try {
      savedHash = Gdx.files.internal(Main.VERSION_FILE).readString();
    } catch (Exception e) {
      savedHash = FALLBACK_VERSION;
    }
    if (savedHash.equals(FALLBACK_VERSION) && calcHash.equals(FALLBACK_VERSION)) {
      logger.error("Failed to get the current version");
      return FALLBACK_VERSION;
    }
    if (!savedHash.equals(calcHash) && !FALLBACK_VERSION.equals(calcHash)) {
      FileHandle versionFile = Gdx.files.absolute(Main.VERSION_FILE);
      try {
        versionFile.writeString(calcHash, false);
      } catch (Exception ignore) {
        logger.error("Failed to write new version to file");
      }
    }
    return calcHash.equals(FALLBACK_VERSION) ? savedHash : calcHash;
  }

  /**
   * @return The latest commit ID in the current repo
   */
  @NotNull
  public static String getLastGitCommitID() {
    String result = executeCommand("git", "log", "-1", "--format=%h");
    if (result == null) {
      return DEFAULT_HASH;
    }
    return result.toUpperCase();
  }

  /**
   * @return The number of commits in this repository
   */
  public static int commitCount() {
    String result = executeCommand("git", "rev-list", "HEAD", "--count");
    if (result == null) {
      return DEFAULT_COMMIT_COUNT;
    }
    try {
      return Integer.parseInt(result);
    } catch (NumberFormatException e) {
      return DEFAULT_COMMIT_COUNT;
    }
  }

  /**
   * @return The number of commits in this repository
   */
  @Nullable
  public static String getLastCommitDate(String dateFormat) {
    String date = executeCommand("git", "log", "-1", "--format=%cd", "--date=" + dateFormat);
    if (date == null || RELATIVE_TIME.equals(dateFormat)) {
      return date;
    }
    try {
      ZonedDateTime parse = ZonedDateTime.parse(date).withZoneSameInstant(ZoneOffset.UTC);
      return parse.format(ZULU_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  @Nullable
  private static String executeCommand(String... command) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command(command);
      Process p = processBuilder.start();
      p.waitFor();

      InputStream inputStream = p.getInputStream();
      try (InputStreamReader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        try (BufferedReader bufferedReader = new BufferedReader(in)) {
          return bufferedReader.readLine();
        }
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * @param orgDir The original direction
   * @return The direction normalized within 0 (inclusive) to 360 (exclusive)
   */
  public static float normalizedDir(float orgDir) {
    if (orgDir >= CIRCLE_DEG) {
      return orgDir % CIRCLE_DEG;
    } else if (orgDir < 0) {
      int mult = (int) (-orgDir / CIRCLE_DEG) + 1;
      return mult * CIRCLE_DEG + orgDir;
    }
    return orgDir; // is within [0,360)
  }
}
