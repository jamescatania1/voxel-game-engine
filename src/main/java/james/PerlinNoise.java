package james;

import java.util.Random;

public class PerlinNoise {

    private static PerlinNoise defaultNoise;

    private static int[] permutation = new int[]{
        151,160,137,91,90,15,
        131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
        88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
        77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
        102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
        5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
        223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
        129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
        49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    };

    private byte[] key;
    private int[] p;

    public PerlinNoise(long seed){
        key = new byte[256];
        p = new int[512];
        Random random = new Random(seed);
        random.nextBytes(key);
        for(int i = 0; i < 256; i++){
            p[i] = p[256 + i] = permutation[(int)key[i] + 128];
        }
    }

    public double Evaluate(double x, double y){
        int xPos = (int)Math.floor(x) % 256;
        if(xPos < 0) xPos += 256;
        int yPos = (int)Math.floor(y) % 256;
        if(yPos < 0) yPos += 256;
        double _x = x % 1.0;
        if(_x < 0.0) _x += 1.0;
        double _y = y % 1.0;
        if(_y < 0.0) _y += 1.0;

        double xFade = fade(_x);
        double yFade = fade(_y);
        int hashA = p[xPos] + yPos;
        int hashB = p[xPos + 1] + yPos;
        int aa = p[hashA];
        int ba = p[hashB];
        int ab = p[hashA + 1];
        int bb = p[hashB + 1];

        int hash11 = p[aa];
        int hash01 = p[ba];
        int hash10 = p[ab];
        int hash00 = p[bb];

        double result = lerp(
            lerp(grad(_x, _y, hash11), grad(_x - 1.0, _y, hash01), xFade),
            lerp(grad(_x, _y - 1.0, hash10), grad(_x - 1.0, _y - 1.0, hash00), xFade),
            yFade
        );
        return 0.5 + result;
    }

    private static double lerp(double a, double b, double t){
        return a + t * (b - a);
    }
    private static double fade(double t){
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }
    private static double grad(double x, double y, int hash){
        return (((hash & 1) != 0) ? x : -x) + (((hash & 2) != 0) ? y : -y);
    }

    /**
     * Evaluates perlin noise for the "default noise", which has a random seed.
     * @param x
     * @param y
     * @return The result, in range (0, 1)
     */
    public static double Noise(double x, double y){
        if(defaultNoise == null) {
            defaultNoise = new PerlinNoise(new Random().nextInt(0, Integer.MAX_VALUE));
        }
        return defaultNoise.Evaluate(x, y);
    }

}