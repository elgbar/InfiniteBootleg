package no.elg.infiniteBootleg.world.generator.simplex;

import java.util.Random;

public class OctavePerlin {

    private final int numberOfOctaves;
    //    private ImprovedNoise[] octaves;
    private double[] frequencies;
    private double[] amplitudes;
    private Random random;

    private int largestFeature;
    private double persistence;
    private int seed;

//    private ImprovedNoise octave1 = new ImprovedNoise(seed);
//    private ImprovedNoise octave2 = new ImprovedNoise(seed * 4);
//    private ImprovedNoise octave3 = new ImprovedNoise(seed * 8);
//    private ImprovedNoise octave4 = new ImprovedNoise(seed * 16);
//    private ImprovedNoise octave5 = new ImprovedNoise(seed * 32);
//    private ImprovedNoise octave6 = new ImprovedNoise(seed * 64);
//    private ImprovedNoise octave7 = new ImprovedNoise(seed * 128);
//    private ImprovedNoise octave8 = new ImprovedNoise(seed * 256);

    /**
     * @param largestFeature
     *     Size
     * @param roughness
     *     roughness
     * @param seed
     *     the random seed to use
     */
    public OctavePerlin(int largestFeature, double roughness, int seed) {
        this.largestFeature = largestFeature;
        this.persistence = roughness;
        this.seed = seed;
        random = new Random(seed);
//
//        //recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
        numberOfOctaves = (int) Math.ceil(Math.log10(largestFeature) / Math.log10(2));
//        Main.getConsoleLogger().log("Octaves = " + numberOfOctaves);
//
//        octaves = new ImprovedNoise[numberOfOctaves];
//        frequencies = new double[numberOfOctaves];
//        amplitudes = new double[numberOfOctaves];
//
//        Random rnd = new Random(seed);
//
//        for (int i = 0; i < numberOfOctaves; i++) {
//            octaves[i] = new ImprovedNoise(rnd.nextInt());
//
//            frequencies[i] = Math.pow(2, i);
//            amplitudes[i] = (Math.pow(roughness, octaves.length - i) / 2);
//            Main.getConsoleLogger().logf("F = %.3f %d", frequencies[i], i);
//            Main.getConsoleLogger().logf("A = %.3f %d", amplitudes[i], i);
//        }

    }

//    public static double noise(double x, double y, double z) {
//        return ImprovedNoise.noise(x, y, z, 8, 0.1);
//    }

}
