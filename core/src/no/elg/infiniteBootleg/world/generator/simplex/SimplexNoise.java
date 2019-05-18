package no.elg.infiniteBootleg.world.generator.simplex;

import java.util.Random;

public class SimplexNoise {

    private SimplexNoiseOctave[] octaves;
    private double[] frequencies;
    private double[] amplitudes;

    private int largestFeature;
    private double persistence;
    private int seed;

    /**
     * @param largestFeature
     * @param persistence
     * @param seed
     *     the random seed to use
     */
    public SimplexNoise(int largestFeature, double persistence, int seed) {
        this.largestFeature = largestFeature;
        this.persistence = persistence;
        this.seed = seed;

        //recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
        int numberOfOctaves = (int) Math.ceil(Math.log10(largestFeature) / Math.log10(2));
        System.out.println(numberOfOctaves);

        octaves = new SimplexNoiseOctave[numberOfOctaves];
        frequencies = new double[numberOfOctaves];
        amplitudes = new double[numberOfOctaves];

        Random rnd = new Random(seed);

        for (int i = 0; i < numberOfOctaves; i++) {
            octaves[i] = new SimplexNoiseOctave(rnd.nextInt());

            frequencies[i] = Math.pow(2, i);
            System.out.println("F=" + frequencies[i] + " " + i);
            amplitudes[i] = (Math.pow(persistence, octaves.length - i) / 2);
            System.out.println("A=" + amplitudes[i] + " " + i);
        }

    }


    public double getNoise(double x, double y) {
        double result = 0;

        for (int i = 0; i < octaves.length; i++) {
            result = result + octaves[i].noise(x / frequencies[i], y / frequencies[i]) * amplitudes[i];
        }
        return result;
    }

    public double getNoise(int x, int y, int z) {
        double result = 0;

        for (int i = 0; i < octaves.length; i++) {
            double frequency = Math.pow(2, i);
            double amplitude = Math.pow(persistence, octaves.length - i);
            result = result + octaves[i].noise(x / frequency, y / frequency, z / frequency) * amplitude;
        }
        return result;
    }

    public double getSingleNoise(int x, int y, int z) {
        double result;
        SimplexNoiseOctave octave = new SimplexNoiseOctave(0);
        result = octave.noise(x, y, z);
        return result;
    }

    public double getDoubleNoise(int x, int y, int z) {
        double result;
        SimplexNoiseOctave octave = new SimplexNoiseOctave(0);
        SimplexNoiseOctave octave2 = new SimplexNoiseOctave(1);
        result = (octave.noise(x, y, z) + (octave2.noise(x, y, z) / 2));
        result = result / 2;
        return result;
    }

    public double getQuadNoise(int x, int y, int z) {
        double result;
        SimplexNoiseOctave octave = new SimplexNoiseOctave(0);
        SimplexNoiseOctave octave2 = new SimplexNoiseOctave(10);
        SimplexNoiseOctave octave3 = new SimplexNoiseOctave(20);
        SimplexNoiseOctave octave4 = new SimplexNoiseOctave(40);
        result =
            (octave.noise(x, y, z) + (octave2.noise(x, y, z) / 2) + (octave3.noise(x, y, z) / 4) + (octave4.noise(x, y, z) / 8));
        result = result / 4;
        return result;
    }

    public double getOctNoise(int x, int y, int z) {
        double result;
        SimplexNoiseOctave octave = new SimplexNoiseOctave(0);
        SimplexNoiseOctave octave2 = new SimplexNoiseOctave(10);
        SimplexNoiseOctave octave3 = new SimplexNoiseOctave(20);
        SimplexNoiseOctave octave4 = new SimplexNoiseOctave(40);
        SimplexNoiseOctave octave5 = new SimplexNoiseOctave(80);
        SimplexNoiseOctave octave6 = new SimplexNoiseOctave(100);
        SimplexNoiseOctave octave7 = new SimplexNoiseOctave(120);
        SimplexNoiseOctave octave8 = new SimplexNoiseOctave(140);
        result =
            (octave.noise(x, y, z) + (octave2.noise(x, y, z) / 2) + (octave3.noise(x, y, z) / 4) + (octave4.noise(x, y, z) / 8) +
             (octave5.noise(x, y, z) / 16) + (octave6.noise(x, y, z) / 32) + (octave7.noise(x, y, z) / 64) +
             (octave8.noise(x, y, z) / 128));
        result = result / 8;
        return result;
    }
}
