import java.util.Random;

public class Test {
    public static void main(String[] args) {
        Random rand = new Random();
        int moves = 0;
        for (int i = 0; i < 16; ++i) {
            int gene = rand.nextInt(4);
            System.err.print(gene);
            moves |= (gene << i * 2);
        }
        System.err.println();
        System.err.println(Integer.toString(moves, 4));

        for (int i = 0; i < 16; ++i) {
            System.err.print((moves >>> i * 2) & 3);
        }

        float[] probs = new float[] { 7, 10, 2, 5, 130, 1 };

        for (int i = 0; i < 20; ++i) {
            System.out.println(getRand(probs, rand));
        }
    }

    static int getRand(float[] probs, Random rand) {
        float s = 0;
        for (int i = 0, n = probs.length; i < n; ++i) s += probs[i];
        float p = rand.nextFloat() * s;
        s = 0;
        for (int i = 0, n = probs.length; i < n; ++i) {
            s += probs[i];
            if (p < s) {
                return i;
            }
        }
        return probs.length - 1;
    }
}
