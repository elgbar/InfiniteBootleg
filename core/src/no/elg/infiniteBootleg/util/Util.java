package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.google.common.base.Preconditions;
import no.elg.infiniteBootleg.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Util {

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
        Preconditions
            .checkArgument(min.compareTo(max) <= 0, "Minimum argument must be less than or equal to the maximum argument");
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
        Preconditions.checkArgument(min.compareTo(max) <= 0,
                                    "Minimum argument (" + min + ") must be less than or equal to the maximum argument(" + max +
                                    ")");
        if (val.compareTo(min) < 0) { return false; }
        else { return val.compareTo(max) < 0; }
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
    public static String toTitleCase(final String string) {
        final StringBuilder sb = new StringBuilder();

        final String ACTIONABLE_DELIMITERS = " '-/";
        boolean capNext = true;

        for (char c : string.toCharArray()) {
            c = (capNext) ? Character.toTitleCase(c) : Character.toLowerCase(c);
            sb.append(c);
            capNext = ACTIONABLE_DELIMITERS.indexOf((int) c) >= 0;
        }

        return sb.toString();
    }

    public static Map<String, String> interpreterArgs(final String[] args) {
        final HashMap<String, String> argsMap = new HashMap<>();

        for (final String arg : args) {
            if (!arg.startsWith("-")) {
                System.err.printf("Unknown argument '%s'", arg);
            }
            else {
                //we only care about the first equals sign, the rest is a part of the value
                final int equal = arg.indexOf('=');

                //if there is no equal sign there is no value
                if (equal == -1) {
                    //do not include the dash
                    argsMap.put(arg.substring(1), null);
                }
                else {
                    //find the key and value from the index of the first equal sign, but do not include it in the
                    // key or value
                    final String key = arg.substring(1, equal);
                    final String val = arg.substring(equal + 1);
                    argsMap.put(key, val);
                }
            }
        }
//        System.out.println("argsMap.keySet() = " + argsMap.keySet());
//        System.out.println("argsMap.values() = " + argsMap.values());
        return argsMap;
    }

    /**
     * @return The latest commit ID in the current repo
     */
    public static String getLastGitCommitID(final boolean full) {
        final String command = "git log --format=%H -n 1";
        final String defaultHash = "UNKNOWN";
        try {
            final Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            String hash = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
            String savedHash;
            try {
                savedHash = Gdx.files.internal(Main.VERSION_FILE).readString();
            } catch (final Exception e) {
                savedHash = defaultHash;
            }
            if ((!savedHash.equals(hash) || savedHash.equals(defaultHash))) {
                Gdx.files.absolute(Main.VERSION_FILE).writeString(hash, false);
            }
            if (hash == null) {
                hash = savedHash;
            }

            if (!full) {
                hash = hash.substring(0, 6);
            }
            return hash.toUpperCase();

        } catch (final IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
