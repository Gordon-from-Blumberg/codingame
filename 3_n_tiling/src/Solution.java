import java.util.*;

class Solution {
    private static final int MODULO = 1000000007;
    private static final int[][] RESULT_MAP = new int[2][1000000];
    private static final int[] SUM = new int[1000000];

    private static int lastIndex2, lastIndex3;

    static {
        RESULT_MAP[0][0] = 0; //k = 2, n = 1
        RESULT_MAP[0][1] = 1; //k = 2, n = 2
        RESULT_MAP[0][2] = 1; //k = 2, n = 3
        RESULT_MAP[1][0] = 1; //k = 3, n = 1
        RESULT_MAP[1][1] = 1; //k = 3, n = 2
        RESULT_MAP[1][2] = 2; //k = 3, n = 3
        SUM[0] = 1;
        SUM[1] = 1;
        SUM[2] = 3;
        lastIndex2 = 3;
        lastIndex3 = 3;

        for (int i = 4; i < 12; i++) calcFor3(i);
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int count = in.nextInt();

        for (int i = 0; i < count; i++) {
            int k = in.nextInt();
            int n = in.nextInt();
            System.err.println("k = " + k + ", n = " + n);
            System.out.println(calc(k, n) % MODULO);
        }
    }

    static long calc(int k, int n) {
        switch (k) {
            case 1:
                return n % 3 == 0 ? 1 : 0;
            case 2:
                return calcFor2(n);
            case 3:
                return calcFor3(n);
            default:
                return 0;
        }
    }

    static int calcFor2(int n) {
        if (n <= lastIndex2) {
            return RESULT_MAP[0][n - 1];
        }

        for (int i = lastIndex2; i < n; i++) {
            RESULT_MAP[0][i] = (RESULT_MAP[0][i - 2] + RESULT_MAP[0][i - 3]) % MODULO;
        }

        lastIndex2 = n;
        return RESULT_MAP[0][n - 1];
    }

    static int calcFor3(int n) {
        if (n <= lastIndex3) {
            return RESULT_MAP[1][n - 1];
        }

        for (int i = lastIndex3 + 1; i <= n; i++) {
            if (i < 12) {
                RESULT_MAP[1][i - 1] = calcFor3Small(i);
                SUM[i - 1] = (int) (SUM[i - 4] + (long) RESULT_MAP[1][i - 1]) % MODULO;
                continue;
            }

            long res = ((long) RESULT_MAP[1][i - 4])
                    + RESULT_MAP[1][i - 2] - RESULT_MAP[1][i - 5]
                    + RESULT_MAP[1][i - 4] - RESULT_MAP[1][i - 7]
                    + SUM[i - 7] * 2;

            res %= MODULO;
            if (res < 0) res += MODULO;
            RESULT_MAP[1][i - 1] = (int) res;
            SUM[i - 1] = (int) (SUM[i - 4] + (long) RESULT_MAP[1][i - 1]) % MODULO;
        }

        lastIndex3 = n;
        return RESULT_MAP[1][n - 1];
    }

    static int calcFor3Small(int n) {
        //sum of f(n-1) and f(n-3)
        long res = (long) RESULT_MAP[1][n - 2] + RESULT_MAP[1][n - 4];

        for (int n2 = 6; n2 <= n; n2 += 3) {
            long add = 2 * calcUnsplitted(n2) * (long) RESULT_MAP[1][n == n2 ? 0 : n - n2 - 1] % MODULO;
//                debug("calc for n = %s, add for n2 = %s = %s", i, n2, add);
            res += add;
            res %= MODULO;
        }

        return (int) res;
    }

    static int calcUnsplitted(int n) {
        if (n % 3 != 0) return 0;
        if (n == 3) return 1;
        return n / 3 - 1;
    }

    static void debug(String str, Object... pars) {
       System.err.println(String.format(str, pars));
    }
}