package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.google.common.base.Preconditions;
import com.strongjoshua.console.LogLevel;
import no.elg.infiniteBootleg.Main;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class Util {

    public static final String DEFAULT_HASH = "UNKNOWN";
    public static final int DEFAULT_COMMIT_COUNT = -1;
    public static final String VERSION_DELIMITER = "-";
    public static final String FALLBACK_VERSION = DEFAULT_HASH + VERSION_DELIMITER + DEFAULT_COMMIT_COUNT;

    /**
     * Based on https://gist.github.com/steen919/8a079f4dadf88d4197bb/d732449eb74321207b4b189a3bcbf47a83c5db65
     * Converts the given hex color in 0xAARRGGBB format to a {@link Color} that can be used in a LibGdx application
     */
    public static Color convert(final String str) {
        final long hex = Long.decode(str);
        final float a = (hex & 0xFF000000L) >> 24;
        final float r = (hex & 0x00FF0000L) >> 16;
        final float g = (hex & 0x0000FF00L) >> 8;
        final float b = (hex & 0x000000FFL);
        return new Color(r / 255F, g / 255F, b / 255F, a / 255F);
    }

    /**
     * @param min
     *     The minimum value to return (inclusive)
     * @param val
     *     The value to verify is between {@code min} and {@code max}
     * @param max
     *     The maximum value to return (inclusive)
     *
     * @return {@code val} if between {@code min} and {@code max}, if not return {@code min} or {@code max} respectively
     */
    public static <T extends Comparable<T>> T clamp(final T min, final T val, final T max) {
        Preconditions.checkArgument(min != null && val != null && max != null, "None of the parameters can be null");
        Preconditions.checkArgument(min.compareTo(max) <= 0,
                                    "Minimum argument must be less than or equal to the maximum argument");
        if (val.compareTo(min) < 0) {
            return min;
        }
        else if (val.compareTo(max) > 0) {
            return max;
        }
        return val;
    }

    /**
     * @param min
     *     The minimum value to check (inclusive)
     * @param val
     *     The value to verify is between {@code min} and {@code max}
     * @param max
     *     The maximum value to check (exclusive)
     *
     * @return if {@code val} is between {@code min} (inclusive) and {@code max} (exclusive)
     */
    public static <T extends Comparable<T>> boolean isBetween(final T min, final T val, final T max) {
        Preconditions.checkArgument(min != null && val != null && max != null, "None of the parameters can be null");
        Preconditions.checkArgument(min.compareTo(max) <= 0, "Minimum argument (" + min +
                                                             ") must be less than or equal to the maximum argument(" +
                                                             max + ")");
        if (val.compareTo(min) < 0) { return false; }
        else { return val.compareTo(max) < 0; }
    }

    /**
     * @param min
     *     The minimum value to check (inclusive)
     * @param val
     *     The value to verify is between {@code min} and {@code max}
     * @param max
     *     The maximum value to check (exclusive)
     *
     * @return if {@code val} is between {@code min} (inclusive) and {@code max} (exclusive)
     */
    public static boolean isBetween(float min, float val, float max) {
        return val >= min && val < max;
    }


    /**
     * <a href="https://stackoverflow.com/questions/1670862/obtaining-a-powerset-of-a-set-in-java#1670871">original
     * found here</a>
     *
     * @return The powerset of a set
     */
    public static <T> Set<Set<T>> powerSet(final Set<T> originalSet) {
        final Set<Set<T>> sets = new HashSet<>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }
        final List<T> list = new ArrayList<>(originalSet);
        final T head = list.get(0);
        final Set<T> rest = new HashSet<>(list.subList(1, list.size()));
        for (final Set<T> set : powerSet(rest)) {
            final Set<T> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }


    /**
     * @param string
     *     The string to convert to title case
     *
     * @return The title case version of the given string
     */
    public static String toTitleCase(final String string) {return toTitleCase(true, string);}

    /**
     * @param string
     *     The string to convert to title case
     *
     * @return The title case version of the given string
     */
    public static String toTitleCase(boolean capFirst, final String string) {
        final StringBuilder sb = new StringBuilder();

        final String ACTIONABLE_DELIMITERS = " '-/";
        boolean capNext = capFirst;

        for (char c : string.toCharArray()) {
            c = (capNext) ? Character.toTitleCase(c) : Character.toLowerCase(c);
            sb.append(c);
            capNext = ACTIONABLE_DELIMITERS.indexOf(c) >= 0;
        }

        return sb.toString();
    }

    /**
     * The key in the map is a pair of the character given before a equal sign (=) or end of string and a boolean that
     * is {@code true } if the key started with two dash (-) as prefix. The value of is a String containing the
     * substring from after (not including the first) equal sign to the end of the string.
     *
     * @param args
     *     The system args to interpret
     *
     * @return A Map of the interpreted args
     */
    @NotNull
    public static Map<Pair<String, Boolean>, String> interpreterArgs(final String[] args) {
        final HashMap<Pair<String, Boolean>, String> argsMap = new HashMap<>();

        for (final String arg : args) {
            if (!arg.startsWith("-")) {
                Main.logger().log(LogLevel.ERROR, "Failed to interpret argument " + arg);
            }
            else {
                //we only care about the first equals sign, the rest is a part of the value
                final int equal = arg.indexOf('=');

                boolean doubleDash = arg.startsWith("--");
                int cutoff = doubleDash ? 2 : 1;

                List<String> inArgs = new ArrayList<>();
                String key = equal == -1 ? arg.substring(cutoff) : arg.substring(cutoff, equal);
                if (doubleDash) {
                    inArgs.add(key);
                }
                else {
                    //If this is a single dash (ie switch) then each char before the equal is a switch on its own
                    for (char c : key.toCharArray()) {
                        inArgs.add(String.valueOf(c));
                    }
                }

                for (int i = 0, size = inArgs.size(); i < size; i++) {
                    String inArg = inArgs.get(i);
                    //if there is no equal sign there is no value
                    String val = null;
                    if (equal != -1 && i >= size - 1) {
                        //find the key and value from the index of the first equal sign, but do not include it in the
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
        String calcHash = getLastGitCommitID(false) + VERSION_DELIMITER + commitCount();
        String savedHash;
        try {
            savedHash = Gdx.files.internal(Main.VERSION_FILE).readString();
        } catch (final Exception e) {
            savedHash = FALLBACK_VERSION;
        }
        if (savedHash.equals(FALLBACK_VERSION) && calcHash.equals(FALLBACK_VERSION)) {
            Main.logger().log(LogLevel.ERROR, "Failed to get the current version");
            return FALLBACK_VERSION;
        }
        if (!savedHash.equals(calcHash) && !FALLBACK_VERSION.equals(calcHash)) {
            FileHandle versionFile = Gdx.files.absolute(Main.VERSION_FILE);
            try {
                versionFile.writeString(calcHash, false);
            } catch (Exception ignore) {
                Main.logger().log(LogLevel.ERROR, "Failed to write new version to file");
            }
        }
        return calcHash.equals(FALLBACK_VERSION) ? savedHash : calcHash;
    }

    /**
     * @return The number of commits in this repository
     */
    public static int commitCount() {
        final String countCommand = "git rev-list master --count";
        try {
            final Process p = Runtime.getRuntime().exec(countCommand);
            p.waitFor();
            String countStr = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
            return Integer.parseInt(countStr);
        } catch (Exception e) {
            return DEFAULT_COMMIT_COUNT;
        }
    }

    /**
     * @return The latest commit ID in the current repo
     */
    public static String getLastGitCommitID(final boolean full) {
        final String hashCommand = "git log --format=%H -n 1";
        try {
            final Process p = Runtime.getRuntime().exec(hashCommand);
            p.waitFor();
            String hash = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
            if (!full) {
                hash = hash.substring(0, 6);
            }
            return hash.toUpperCase();
        } catch (final Exception e) {
            return DEFAULT_HASH;
        }
    }

    public static boolean hasSuperClass(Class<?> impl, Class<?> aClass) {
        if (impl == aClass) { return true; }
        if (impl == Object.class) { return false; }
        return hasSuperClass(impl.getSuperclass(), aClass);
    }

    /**
     * @param orgDir
     *     The original direction
     *
     * @return The direction normalized within 0 (inclusive) to 360 (exclusive)
     */
    public static float normalizedDir(float orgDir) {
        if (orgDir >= 360) {
            return orgDir % 360;
        }
        else if (orgDir < 0) {
            int mult = (int) (-orgDir / 360) + 1;
            return mult * 360 + orgDir;
        }
        return orgDir; //is within [0,360)
    }
}
