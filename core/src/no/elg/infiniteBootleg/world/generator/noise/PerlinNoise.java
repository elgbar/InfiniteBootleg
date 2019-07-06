package no.elg.infiniteBootleg.world.generator.noise;

import com.badlogic.gdx.math.RandomXS128;

import java.util.Random;

/**
 * JAVA REFERENCE IMPLEMENTATION OF IMPROVED NOISE - COPYRIGHT 2002 KEN PERLIN.
 * <p>
 * <a href="https://web.archive.org/web/20190426093413/https://mrl.nyu.edu/~perlin/noise/">source</a>
 */
public class PerlinNoise {

    private static final int[] permutation =
        {151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10,
            23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56,
            87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
            60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76,
            132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226,
            250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223,
            183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108,
            110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162,
            241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45,
            127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180};

    private final int[] p = new int[512];

    /**
     * @param seed
     *     The random seed used to swap elements around randomly
     */
    public PerlinNoise(int seed) {
        Random random = new RandomXS128(seed);
        for (int i = 0; i < 256; i++) {
            p[256 + i] = p[i] = permutation[i];
        }
        //randomize
        for (int i = 0; i < 1000; i++) {
            swap(p, random.nextInt(p.length), random.nextInt(p.length));
        }
    }

    private static void swap(int[] as, int x, int y) {
        int i = as[x];
        as[x] = as[y];
        as[y] = i;
    }

    /**
     * <a href="https://gist.github.com/Flafla2/f0260a861be0ebdeef76">All credits for this goes to Flafla2</a>
     *
     * @param x
     *     x coordinate
     * @param y
     *     y coordinate
     * @param z
     *     z coordinate
     * @param octaves
     *     How refined the result should be
     * @param persistence
     *     How different each octave is
     *
     * @return The perlin noise at the given location modified by the number of octaves and the persistence
     */
    public double octaveNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;  // Used for normalizing result to 0.0 - 1.0
        for (int i = 0; i < octaves; i++) {
            total += noise(x, y, z, amplitude, frequency);

            maxValue += amplitude;

            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    /**
     * @param x
     *     x coordinate
     * @param y
     *     y coordinate
     * @param z
     *     z coordinate
     * @param amplitude
     *     How far it goes up and down (think wave amplitude)
     * @param frequency
     *     How fast it goes up and down (think wave frequency)
     *
     * @return The perlin noise at the given location modified by the frequency and amplitude
     */
    public double noise(double x, double y, double z, double amplitude, double frequency) {
        return noise(x * frequency, y * frequency, z * frequency) * amplitude;
    }


    /**
     * @param x
     *     x coordinate
     * @param y
     *     y coordinate
     * @param z
     *     z coordinate
     *
     * @return The perlin noise at the given location
     */
    public double noise(double x, double y, double z) {
        // FIND UNIT CUBE THAT CONTAINS POINT.
        int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255, Z = (int) Math.floor(z) & 255;
        // FIND RELATIVE X,Y,Z OF POINT IN CUBE.
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        // COMPUTE FADE CURVES FOR EACH OF X,Y,Z.
        double u = fade(x), v = fade(y), w = fade(z);
        // HASH COORDINATES OF THE 8 CUBE CORNERS,
        int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z, B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;
        // AND ADD BLENDED RESULTS FROM 8 CORNERS OF CUBE

        //@formatter:off
        return lerp(w, lerp(v, lerp(u, grad(p[AA  ], x  , y  , z   ),
                                       grad(p[BA  ], x-1, y  , z   )),
                               lerp(u, grad(p[AB  ], x  , y-1, z   ),
                                       grad(p[BB  ], x-1, y-1, z   ))),
                       lerp(v, lerp(u, grad(p[AA+1], x  , y  , z-1 ),
                                       grad(p[BA+1], x-1, y  , z-1 )),
                               lerp(u, grad(p[AB+1], x  , y-1, z-1 ),
                                       grad(p[BB+1], x-1, y-1, z-1 ))));
        //@formatter:on
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private static double lerp(double t, double a, double b) { return a + t * (b - a); }

    private static double grad(int hash, double x, double y, double z) {
        // CONVERT LO 4 BITS OF HASH CODE INTO 12 GRADIENT DIRECTIONS.
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
