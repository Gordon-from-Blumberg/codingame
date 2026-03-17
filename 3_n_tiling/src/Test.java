public class Test {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
//
//        for (int i = 4; i < 111; i += 1)
//            testCalc(3, i);

//        for (int i = 1; i < 86; i++)
//            testCalc(3, i);

        testCalc(3,31);
//        testCalc(3,12);

//        testCalc(3, 999891);
//        long res = Solution.calc(3, 65);
//        if (res != 188449980) {
//            throw new RuntimeException("Failed! f(65) = " + res);
//        }
        Solution.debug("calc took %s ms", System.currentTimeMillis() - start);
    }

    static void test(int n, int k1) {
        int k2 = n - k1;
        System.out.println(String.format("n = %s, k1 = %s, k2 = %s", n, k1, k2));
        System.out.println(String.format("calcPerm = %s", Solution.calcPerm(n, k1, k2)));
//        System.out.println(String.format("fact(%s) = %s", n, Solution.fact(n)));
//        System.out.println(String.format("calcPerm with factorials = %s", (Solution.fact(n) / (Solution.fact(k1) * Solution.fact(k2)))));
    }

    static void testCalc(int k, int n) {
        long res = Solution.calc(k, n);
        Solution.debug("calc(%s, %s) = %s", k, n, res);
//        Solution.debug("calc(%s, %s) = %s + %s", k, n, res - n / 2, n / 2);
        System.err.println();
    }
}
