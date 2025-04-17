import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final int MOD = (1 << 30) - 1;
    static final Map<Key, Integer> CACHE = new HashMap<>(1 << 15);
    static final Key KEY = new Key(null, 0);

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int depth = in.nextInt();
        final int[][] pool = new int[depth][9];
        final int[] start = new int[9];

        for (int i = 0; i < 9; i++) {
            start[i] = in.nextInt();
        }

        System.out.println(step(start, pool, depth));

    }

    static int step(int[] state, int[][] pool, int depth) {
        KEY.set(state, depth);
        Integer cached = CACHE.get(KEY);
        if (cached != null) return cached;

        if (depth == 0) {
            int result = hash(state);
            CACHE.put(KEY.copy(), result);
            return result;
        }

        int result = 0;
        final int[] next = pool[pool.length - depth];
        boolean placed = false;
        for (int i = 0; i < 9; ++i) {
            if (state[i] == 0) {
                placed = true;
                System.arraycopy(state, 0, next, 0, 9);

                switch (i) {
                    case 0 -> {
                        int sum;
                        if (next[1] != 0 && next[3] != 0 && (sum = next[1] + next[3]) <= 6) {
                            next[0] = sum;
                            next[1] = next[3] = 0;
                        } else {
                            next[0] = 1;
                        }
                        result = (result + step(next, pool, depth - 1)) & MOD;
                    }
                    case 1 -> {
                        int count = 0;
                        if (next[0] != 0) ++count;
                        if (next[2] != 0) ++count;
                        if (next[4] != 0) ++count;
                        switch (count) {
                            case 0, 1 -> {
                                next[1] = 1;
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 2 -> {
                                int sum = next[0] + next[2] + next[4];
                                if (sum <= 6) {
                                    next[1] = sum;
                                    next[0] = next[2] = next[4] = 0;
                                } else {
                                    next[1] = 1;
                                }
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 3 -> {
                                int sum;
                                boolean captured = false;
                                if ((sum = next[0] + next[2]) <= 6) {
                                    captured = true;
                                    next[1] = sum;
                                    next[0] = next[2] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[0] + next[4]) <= 6) {
                                    captured = true;
                                    next[1] = sum;
                                    next[0] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[4] + next[2]) <= 6) {
                                    captured = true;
                                    next[1] = sum;
                                    next[4] = next[2] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[0] + next[2] + next[4]) <= 6) {
                                    captured = true;
                                    next[1] = sum;
                                    next[0] = next[2] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if (!captured) {
                                    next[1] = 1;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                }
                            }
                        }
                    }
                    case 2 -> {
                        int sum;
                        if (next[1] != 0 && next[5] != 0 && (sum = next[1] + next[5]) <= 6) {
                            next[2] = sum;
                            next[1] = next[5] = 0;
                        } else {
                            next[2] = 1;
                        }
                        result = (result + step(next, pool, depth - 1)) & MOD;
                    }
                    case 3 -> {
                        int count = 0;
                        if (next[0] != 0) ++count;
                        if (next[6] != 0) ++count;
                        if (next[4] != 0) ++count;
                        switch (count) {
                            case 0, 1 -> {
                                next[3] = 1;
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 2 -> {
                                int sum = next[0] + next[6] + next[4];
                                if (sum <= 6) {
                                    next[3] = sum;
                                    next[0] = next[6] = next[4] = 0;
                                } else {
                                    next[3] = 1;
                                }
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 3 -> {
                                int sum;
                                boolean captured = false;
                                if ((sum = next[0] + next[6]) <= 6) {
                                    captured = true;
                                    next[3] = sum;
                                    next[0] = next[6] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[0] + next[4]) <= 6) {
                                    captured = true;
                                    next[3] = sum;
                                    next[0] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[4] + next[6]) <= 6) {
                                    captured = true;
                                    next[3] = sum;
                                    next[4] = next[6] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[0] + next[6] + next[4]) <= 6) {
                                    captured = true;
                                    next[3] = sum;
                                    next[0] = next[6] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if (!captured) {
                                    next[3] = 1;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                }
                            }
                        }
                    }
                    case 4 -> {
                        boolean captured = false;
                        for (int c1 = 1; c1 < 8; c1 += 2) {
                            if (next[c1] == 0) continue;

                            for (int c2 = c1 + 2; c2 < 8; c2 += 2) {
                                int sum12;
                                if (next[c2] == 0 || (sum12 = next[c1] + next[c2]) > 6) continue;

                                captured = true;
                                next[4] = sum12;
                                next[c1] = next[c2] = 0;
                                result = (result + step(next, pool, depth - 1)) & MOD;
                                System.arraycopy(state, 0, next, 0, 9);

                                for (int c3 = c2 + 2; c3 < 8; c3 += 2) {
                                    int sum123;
                                    if (next[c3] == 0 || (sum123 = sum12 + next[c3]) > 6) continue;

                                    next[4] = sum123;
                                    next[c1] = next[c2] = next[c3] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);

                                    if (c3 == 5 && next[7] != 0 && sum123 + next[7] <= 6) {
                                        next[4] = sum123 + next[7];
                                        next[c1] = next[c2] = next[c3] = next[7] = 0;
                                        result = (result + step(next, pool, depth - 1)) & MOD;
                                        System.arraycopy(state, 0, next, 0, 9);
                                    }
                                }
                            }
                        }
                        if (!captured) {
                            next[4] = 1;
                            result = (result + step(next, pool, depth - 1)) & MOD;
                        }
                    }
                    case 5 -> {
                        int count = 0;
                        if (next[8] != 0) ++count;
                        if (next[2] != 0) ++count;
                        if (next[4] != 0) ++count;
                        switch (count) {
                            case 0, 1 -> {
                                next[5] = 1;
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 2 -> {
                                int sum = next[8] + next[2] + next[4];
                                if (sum <= 6) {
                                    next[5] = sum;
                                    next[8] = next[2] = next[4] = 0;
                                } else {
                                    next[5] = 1;
                                }
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 3 -> {
                                int sum;
                                boolean captured = false;
                                if ((sum = next[8] + next[2]) <= 6) {
                                    captured = true;
                                    next[5] = sum;
                                    next[8] = next[2] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[8] + next[4]) <= 6) {
                                    captured = true;
                                    next[5] = sum;
                                    next[8] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[4] + next[2]) <= 6) {
                                    captured = true;
                                    next[5] = sum;
                                    next[4] = next[2] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[8] + next[2] + next[4]) <= 6) {
                                    captured = true;
                                    next[5] = sum;
                                    next[8] = next[2] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if (!captured) {
                                    next[5] = 1;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                }
                            }
                        }
                    }
                    case 6 -> {
                        int sum;
                        if (next[7] != 0 && next[3] != 0 && (sum = next[7] + next[3]) <= 6) {
                            next[6] = sum;
                            next[7] = next[3] = 0;
                        } else {
                            next[6] = 1;
                        }
                        result = (result + step(next, pool, depth - 1)) & MOD;
                    }
                    case 7 -> {
                        int count = 0;
                        if (next[8] != 0) ++count;
                        if (next[6] != 0) ++count;
                        if (next[4] != 0) ++count;
                        switch (count) {
                            case 0, 1 -> {
                                next[7] = 1;
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 2 -> {
                                int sum = next[8] + next[6] + next[4];
                                if (sum <= 6) {
                                    next[7] = sum;
                                    next[8] = next[6] = next[4] = 0;
                                } else {
                                    next[7] = 1;
                                }
                                result = (result + step(next, pool, depth - 1)) & MOD;
                            }
                            case 3 -> {
                                int sum;
                                boolean captured = false;
                                if ((sum = next[8] + next[6]) <= 6) {
                                    captured = true;
                                    next[7] = sum;
                                    next[8] = next[6] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[8] + next[4]) <= 6) {
                                    captured = true;
                                    next[7] = sum;
                                    next[8] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[4] + next[6]) <= 6) {
                                    captured = true;
                                    next[7] = sum;
                                    next[4] = next[6] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if ((sum = next[8] + next[6] + next[4]) <= 6) {
                                    captured = true;
                                    next[7] = sum;
                                    next[8] = next[6] = next[4] = 0;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                    System.arraycopy(state, 0, next, 0, 9);
                                }
                                if (!captured) {
                                    next[7] = 1;
                                    result = (result + step(next, pool, depth - 1)) & MOD;
                                }
                            }
                        }
                    }
                    case 8 -> {
                        int sum;
                        if (next[7] != 0 && next[5] != 0 && (sum = next[7] + next[5]) <= 6) {
                            next[8] = sum;
                            next[7] = next[5] = 0;
                        } else {
                            next[8] = 1;
                        }
                        result = (result + step(next, pool, depth - 1)) & MOD;
                    }
                }
            }
        }

        result = placed ? result : hash(state);
        CACHE.put(new Key(state, depth), result);
        return result;
    }

    static int hash(int[] state) {
        int result = 0;
        int k = 1;
        for (int i = 8; i >= 0; --i) {
            result += state[i] * k;
            k *= 10;
        }
        return result;
    }

    static class Key {
        int[] state;
        int depth;

        Key(int[] state, int depth) {
            this.depth = depth;
            this.state = state;
        }

        void set(int[] state, int depth) {
            this.depth = depth;
            this.state = state;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(state) + 31 * depth;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key other) {
                return other.depth == depth && Arrays.equals(other.state, state);
            }
            return false;
        }

        public Key copy() {
            return new Key(Arrays.copyOf(state, 9), depth);
        }
    }
}