import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final String[] commands = { "UP", "LEFT", "DOWN", "RIGHT" };
    static final long LIMIT = 40 * 1_000_000; //ns
    static final long FIRST_LIMIT = 940 * 1_000_000; //ns
    static final int WIN_PTS = 20;
    static final int SEC_PTS = 5;
    static final int LOSE_PTS = -1;
    static final float OPP_ME_UP = 1.5f;
    static final float OPP_ME_DOWN = 0.75f;
    static final float OPP_OPP_UP = 1.2f;
    static final float OPP_OPP_DOWN = 0.85f;
    static final float[] opp1Probs = new float[4];
    static final float[] opp2Probs = new float[4];

    static final float MUTATION_CHANCE = 0.02f;
    static final int MY_GENERATION_SIZE = 32;
    static final int MOVE_COUNT = 32;
    static final long[] myGeneration = new long[MY_GENERATION_SIZE];
    static final long[] tempGeneration = new long[MY_GENERATION_SIZE];
    static final float[][] myGenerationFitness = new float[5][MY_GENERATION_SIZE];

    static long myBestMove;

    static int playerIdx;
    static int nbGames;

    public static void main(String args[]) {
        final Random rand = new Random();
        final Scanner in = new Scanner(System.in);
        playerIdx = in.nextInt();
        System.err.println("my index = " + playerIdx);
        nbGames = in.nextInt();

        final State state = new State();
        final State temp = new State();
        final int[] cmdPoints = new int[4];

        if (in.hasNextLine()) {
            in.nextLine();
        }

        // game loop
        while (true) {
            for (int i = 0; i < 3; i++) {
                String scoreInfo = in.nextLine();
                // System.err.println("score info " + i + " = " + scoreInfo);
                state.readScore(i, scoreInfo);
            }

            state.read(in);
            state.printState();
            state.calcCmdPriority();

            in.nextLine();

            int cmdIdx = 3;
            int maxPriority = -999;
            for (int i = 0; i < 4; ++i) {
                int priority = 0;
                for (int j = 0; j < nbGames; ++j) {
                    priority += state.games[j].cmdPriority[i];
                }
                if (priority > maxPriority) {
                    maxPriority = priority;
                    cmdIdx = i;
                }
//                System.err.println(commands[i] + ": " + priority);
            }

//            for (int i = 0; i < 4; ++i) cmdPoints[i] = 0;
//            monteCarlo(state, temp, cmdPoints, rand);
            cmdIdx = genetic(state, temp, rand);

//                 int maxPoint = Integer.MIN_VALUE;
//                for (int i = 0; i < 4; ++i) {
//                    System.err.println(commands[i] + " points = " + cmdPoints[i]);
//                    if (cmdPoints[i] > maxPoint) {
//                        maxPoint = cmdPoints[i];
//                        cmdIdx = i;
//                    }
//                }

            ++state.turn;
            System.out.println(commands[cmdIdx]);
        }
    }

    static void monteCarlo(State state, State temp, int[] cmdPoints, Random rand) {
        long end = System.nanoTime() + LIMIT;
        int iteration = 0;
        int myGold = state.totalGold(playerIdx);

        final float[] probs1 = opp1Probs;
        for (int i = 0; i < 4; ++i) probs1[i] = 1f;
        final float[] probs2 = opp2Probs;
        for (int i = 0; i < 4; ++i) probs2[i] = 1f;

        switch (playerIdx) {
            case 0 -> {
                while (System.nanoTime() < end) {
                    ++iteration;
                    for (int cmd = 0; cmd < 4; ++cmd) {
                        temp.set(state);

                        float s = 0;
                        for (int i = 0; i < 4; ++i) s += probs1[i];
                        float p = rand.nextFloat() * s;
                        int oppMove1 = 3;
                        float pp = 0;
                        for (int i = 0; i < 4; ++i) {
                            pp += probs1[i];
                            if (p < pp) {
                                oppMove1 = i;
                                break;
                            }
                        }

                        s = 0;
                        for (int i = 0; i < 4; ++i) s += probs2[i];
                        p = rand.nextFloat() * s;
                        int oppMove2 = 3;
                        pp = 0;
                        for (int i = 0; i < 4; ++i) {
                            pp += probs2[i];
                            if (p < pp) {
                                oppMove1 = i;
                                break;
                            }
                        }

                        if (!temp.move(cmd, oppMove1, oppMove2)) {
                            while (!temp.move(rand.nextInt(4), rand.nextInt(4), rand.nextInt(4))) { }
                            int myTotal = temp.totalScore(0);
                            int loseCount = 0;
                            int opp1Score = temp.totalScore(1);
                            int opp2Score = temp.totalScore(2);
                            if (opp1Score > myTotal) {
                                ++loseCount;
                                probs1[oppMove1] *= OPP_ME_UP;
                            } else {
                                probs1[oppMove1] *= OPP_ME_DOWN;
                            }
                            if (opp2Score > myTotal) {
                                ++loseCount;
                                probs2[oppMove2] *= OPP_ME_UP;
                            } else {
                                probs2[oppMove2] *= OPP_ME_DOWN;
                            }
                            if (opp1Score > opp2Score) {
                                probs1[oppMove1] *= OPP_OPP_UP;
                                probs2[oppMove2] *= OPP_OPP_DOWN;
                            } else if (opp1Score < opp2Score) {
                                probs2[oppMove2] *= OPP_OPP_UP;
                                probs1[oppMove1] *= OPP_OPP_DOWN;
                            }
                            if (loseCount == 0) cmdPoints[cmd] += WIN_PTS;
                            else if (loseCount == 1) cmdPoints[cmd] += SEC_PTS;
                            else cmdPoints[cmd] += LOSE_PTS;
                            cmdPoints[cmd] += temp.totalGold(0) - myGold;
                        }
                    }
                }
            }
            case 1 -> {
                while (System.nanoTime() < end) {
                    ++iteration;
                    for (int cmd = 0; cmd < 4; ++cmd) {
                        temp.set(state);

                        float s = 0;
                        for (int i = 0; i < 4; ++i) s += probs1[i];
                        float p = rand.nextFloat() * s;
                        int oppMove1 = 3;
                        float pp = 0;
                        for (int i = 0; i < 4; ++i) {
                            pp += probs1[i];
                            if (p < pp) {
                                oppMove1 = i;
                                break;
                            }
                        }

                        s = 0;
                        for (int i = 0; i < 4; ++i) s += probs2[i];
                        p = rand.nextFloat() * s;
                        int oppMove2 = 3;
                        pp = 0;
                        for (int i = 0; i < 4; ++i) {
                            pp += probs2[i];
                            if (p < pp) {
                                oppMove1 = i;
                                break;
                            }
                        }

                        if (!temp.move(oppMove1, cmd, oppMove2)) {
                            while (!temp.move(rand.nextInt(4), rand.nextInt(4), rand.nextInt(4))) { }
                            int myTotal = temp.totalScore(1);
                            int loseCount = 0;
                            int opp1Score = temp.totalScore(0);
                            int opp2Score = temp.totalScore(2);
                            if (opp1Score > myTotal) {
                                ++loseCount;
                                probs1[oppMove1] *= OPP_ME_UP;
                            } else {
                                probs1[oppMove1] *= OPP_ME_DOWN;
                            }
                            if (opp2Score > myTotal) {
                                ++loseCount;
                                probs2[oppMove2] *= OPP_ME_UP;
                            } else {
                                probs2[oppMove2] *= OPP_ME_DOWN;
                            }
                            if (opp1Score > opp2Score) {
                                probs1[oppMove1] *= OPP_OPP_UP;
                                probs2[oppMove2] *= OPP_OPP_DOWN;
                            } else if (opp1Score < opp2Score) {
                                probs2[oppMove2] *= OPP_OPP_UP;
                                probs1[oppMove1] *= OPP_OPP_DOWN;
                            }
                            if (loseCount == 0) cmdPoints[cmd] += WIN_PTS;
                            else if (loseCount == 1) cmdPoints[cmd] += SEC_PTS;
                            else cmdPoints[cmd] += LOSE_PTS;
                            cmdPoints[cmd] += temp.totalGold(1) - myGold;
                        }
                    }
                }
            }
            case 2 -> {
                while (System.nanoTime() < end) {
                    ++iteration;
                    for (int cmd = 0; cmd < 4; ++cmd) {
                        temp.set(state);

                        float s = 0;
                        for (int i = 0; i < 4; ++i) s += probs1[i];
                        float p = rand.nextFloat() * s;
                        int oppMove1 = 3;
                        float pp = 0;
                        for (int i = 0; i < 4; ++i) {
                            pp += probs1[i];
                            if (p < pp) {
                                oppMove1 = i;
                                break;
                            }
                        }

                        s = 0;
                        for (int i = 0; i < 4; ++i) s += probs2[i];
                        p = rand.nextFloat() * s;
                        int oppMove2 = 3;
                        pp = 0;
                        for (int i = 0; i < 4; ++i) {
                            pp += probs2[i];
                            if (p < pp) {
                                oppMove1 = i;
                                break;
                            }
                        }

                        if (!temp.move(oppMove1, oppMove2, cmd)) {
                            while (!temp.move(rand.nextInt(4), rand.nextInt(4), rand.nextInt(4))) { }
                            int myTotal = temp.totalScore(2);
                            int loseCount = 0;
                            int opp1Score = temp.totalScore(0);
                            int opp2Score = temp.totalScore(1);
                            if (opp1Score > myTotal) {
                                ++loseCount;
                                probs1[oppMove1] *= OPP_ME_UP;
                            } else {
                                probs1[oppMove1] *= OPP_ME_DOWN;
                            }
                            if (opp2Score > myTotal) {
                                ++loseCount;
                                probs2[oppMove2] *= OPP_ME_UP;
                            } else {
                                probs2[oppMove2] *= OPP_ME_DOWN;
                            }
                            if (opp1Score > opp2Score) {
                                probs1[oppMove1] *= OPP_OPP_UP;
                                probs2[oppMove2] *= OPP_OPP_DOWN;
                            } else if (opp1Score < opp2Score) {
                                probs2[oppMove2] *= OPP_OPP_UP;
                                probs1[oppMove1] *= OPP_OPP_DOWN;
                            }
                            if (loseCount == 0) cmdPoints[cmd] += WIN_PTS;
                            else if (loseCount == 1) cmdPoints[cmd] += SEC_PTS;
                            else cmdPoints[cmd] += LOSE_PTS;
                            cmdPoints[cmd] += temp.totalGold(2) - myGold;
                        }
                    }
                }
            }
        }

        System.err.println("iteration = " + iteration);
    }

    static int genetic(State state, State temp, Random rand) {
        long end = System.nanoTime() + (state.turn > 0 ? LIMIT : FIRST_LIMIT);
        int generation = 0;

        final long[] myGen = myGeneration;
        final long[] tempGen = tempGeneration;
        final float[][] myGenFit = myGenerationFitness;

        switch (playerIdx) {
            case 0 -> {
                while (System.nanoTime() < end) {
                    if (++generation == 1) {
                        // create my first generation
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
                            if (i == 0 && state.turn > 0) {
                                myGen[0] = myBestMove >>> 2;
                                continue;
                            }
                            long myMoves = 0;
                            for (int j = 0; j < MOVE_COUNT; ++j) {
                                myMoves |= (rand.nextLong(4) << j * 2);
                            }
                            myGen[i] = myMoves;
                        }
                    } else {
                        // create my new generation
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) myGenFit[4][i] *= myGenFit[4][i];
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
                            long par1 = myGen[getRand(myGenFit[4], rand)];
                            long par2 = myGen[getRand(myGenFit[4], rand)];
                            long myMoves = 0;
                            for (int j = 0; j < MOVE_COUNT; ++j) {
                                long gene = rand.nextFloat() < MUTATION_CHANCE
                                        ? rand.nextInt(4)
                                        : (((rand.nextFloat() < 0.5f ? par1 : par2) >>> j * 2) & 3);
                                myMoves |= (gene << j * 2);
                            }
                            tempGen[i] = myMoves;
                        }
                        System.arraycopy(tempGen, 0, myGen, 0, MY_GENERATION_SIZE);
                    }

                    for (int i = 0; i < 5; ++i)
                        for (int j = 0; j < MY_GENERATION_SIZE; ++j) myGenFit[i][j] = 0f;

                    // all simulations
                    int bestMove = -1;
                    float bestFit = -1f;
                    for (int myMovesIdx = 0; myMovesIdx < MY_GENERATION_SIZE; ++myMovesIdx) {
                        final long myMoves = myGen[myMovesIdx];

                        // simulation
                        temp.set(state);
                        for (int m = 0; m < MOVE_COUNT; ++m) {
                            if (temp.move((int) (myMoves >>> m * 2) & 3, rand.nextInt(4), rand.nextInt(4)))
                                break;
                        }

                        int myIdx = 0;
//                        temp.printState();
                        myGenFit[0][myMovesIdx] = raceFit(state, temp, myIdx);
                        myGenFit[1][myMovesIdx] = archeryFit(state, temp, myIdx);
                        myGenFit[2][myMovesIdx] = skatingFit(state, temp, myIdx);
                        myGenFit[3][myMovesIdx] = divingFit(state, temp, myIdx);

                        myGenFit[4][myMovesIdx] = myGenFit[0][myMovesIdx] * myGenFit[1][myMovesIdx]
                                * myGenFit[2][myMovesIdx] * myGenFit[3][myMovesIdx];

                        if (temp.isWon(myIdx)) myGenFit[4][myMovesIdx] *= 2;
                        else if (temp.isLose(myIdx)) myGenFit[4][myMovesIdx] /= 5f;

//                        if (myGenFit[4][myMovesIdx] > bestFit) {
//                            bestFit = myGenFit[4][myMovesIdx];
//                            bestMove = myMovesIdx;
//                        }
                    }

//                    System.err.println("Gen " + generation);
//                    System.err.println(myGenFit[0][bestMove]);
                }
            }

            case 1 -> {
                while (System.nanoTime() < end) {
                    if (++generation == 1) {
                        // create my first generation
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
                            if (i == 0 && state.turn > 0) {
                                myGen[0] = myBestMove >>> 2;
                                continue;
                            }
                            long myMoves = 0;
                            for (int j = 0; j < MOVE_COUNT; ++j) {
                                myMoves |= (rand.nextLong(4) << j * 2);
                            }
                            myGen[i] = myMoves;
                        }
                    } else {
                        // create my new generation
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
                            long par1 = myGen[getRand(myGenFit[4], rand)];
                            long par2 = myGen[getRand(myGenFit[4], rand)];
                            long myMoves = 0;
                            for (int j = 0; j < MOVE_COUNT; ++j) {
                                long gene = rand.nextFloat() < MUTATION_CHANCE
                                        ? rand.nextInt(4)
                                        : (((rand.nextFloat() < 0.5f ? par1 : par2) >>> j * 2) & 3);
                                myMoves |= (gene << j * 2);
                            }
                            tempGen[i] = myMoves;
                        }
                        System.arraycopy(tempGen, 0, myGen, 0, MY_GENERATION_SIZE);
                    }

                    for (int i = 0; i < 5; ++i)
                        for (int j = 0; j < MY_GENERATION_SIZE; ++j) myGenFit[i][j] = 0f;

                    // all simulations
                    for (int myMovesIdx = 0; myMovesIdx < MY_GENERATION_SIZE; ++myMovesIdx) {
                        final long myMoves = myGen[myMovesIdx];

                        // simulation
                        temp.set(state);
                        for (int m = 0; m < MOVE_COUNT; ++m) {
                            if (temp.move(rand.nextInt(4), (int) (myMoves >>> m * 2) & 3, rand.nextInt(4)))
                                break;
                        }

                        int myIdx = 1;
//                        temp.printState();
                        myGenFit[0][myMovesIdx] = raceFit(state, temp, myIdx);
                        myGenFit[1][myMovesIdx] = archeryFit(state, temp, myIdx);
                        myGenFit[2][myMovesIdx] = skatingFit(state, temp, myIdx);
                        myGenFit[3][myMovesIdx] = divingFit(state, temp, myIdx);

                        myGenFit[4][myMovesIdx] = myGenFit[0][myMovesIdx] * myGenFit[1][myMovesIdx]
                                * myGenFit[2][myMovesIdx] * myGenFit[3][myMovesIdx];

                        if (temp.isWon(myIdx)) myGenFit[4][myMovesIdx] *= 2;
                        else if (temp.isLose(myIdx)) myGenFit[4][myMovesIdx] /= 5f;
                    }
                }
            }

            case 2 -> {
                while (System.nanoTime() < end) {
                    if (++generation == 1) {
                        // create my first generation
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
                            if (i == 0 && state.turn > 0) {
                                myGen[0] = myBestMove >>> 2;
                                continue;
                            }
                            long myMoves = 0;
                            for (int j = 0; j < MOVE_COUNT; ++j) {
                                myMoves |= (rand.nextLong(4) << j * 2);
                            }
                            myGen[i] = myMoves;
                        }
                    } else {
                        // create my new generation
                        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
                            long par1 = myGen[getRand(myGenFit[4], rand)];
                            long par2 = myGen[getRand(myGenFit[4], rand)];
                            long myMoves = 0;
                            for (int j = 0; j < MOVE_COUNT; ++j) {
                                long gene = rand.nextFloat() < MUTATION_CHANCE
                                        ? rand.nextInt(4)
                                        : (((rand.nextFloat() < 0.5f ? par1 : par2) >>> j * 2) & 3);
                                myMoves |= (gene << j * 2);
                            }
                            tempGen[i] = myMoves;
                        }
                        System.arraycopy(tempGen, 0, myGen, 0, MY_GENERATION_SIZE);
                    }

                    for (int i = 0; i < 5; ++i)
                        for (int j = 0; j < MY_GENERATION_SIZE; ++j) myGenFit[i][j] = 0f;

                    // all simulations
                    for (int myMovesIdx = 0; myMovesIdx < MY_GENERATION_SIZE; ++myMovesIdx) {
                        final long myMoves = myGen[myMovesIdx];

                        // simulation
                        temp.set(state);
                        for (int m = 0; m < MOVE_COUNT; ++m) {
                            if (temp.move(rand.nextInt(4), rand.nextInt(4), (int) (myMoves >>> m * 2) & 3))
                                break;
                        }

                        int myIdx = 2;
//                        temp.printState();

                        myGenFit[0][myMovesIdx] = raceFit(state, temp, myIdx);
                        myGenFit[1][myMovesIdx] = archeryFit(state, temp, myIdx);
                        myGenFit[2][myMovesIdx] = skatingFit(state, temp, myIdx);
                        myGenFit[3][myMovesIdx] = divingFit(state, temp, myIdx);

                        myGenFit[4][myMovesIdx] = myGenFit[0][myMovesIdx] * myGenFit[1][myMovesIdx]
                                * myGenFit[2][myMovesIdx] * myGenFit[3][myMovesIdx];

                        if (temp.isWon(myIdx)) myGenFit[4][myMovesIdx] *= 2;
                        else if (temp.isLose(myIdx)) myGenFit[4][myMovesIdx] /= 5f;
                    }
                }
            }
        }

        System.err.println("generation = " + generation);

        float maxFit = -1;
        int bestMoveIdx = -1;
        for (int i = 0; i < MY_GENERATION_SIZE; ++i) {
            if (myGenFit[4][i] > maxFit) {
                maxFit = myGenFit[4][i];
                bestMoveIdx = i;
            }
        }
        myBestMove = myGen[bestMoveIdx];
        return (int) myGen[bestMoveIdx] & 3;
    }

    static float raceFit(State state, State temp, int plIdx) {
        float max = 0.85f;
        if (state.race.isGameOver) {
            return max;
        } else {
            Race race = temp.race;
            float res = race.bestResult * race.bestResult;
            float d = 2 * race.turn + (Race.LAST_POS - race.positions[plIdx] + race.stuns[plIdx]);
            res /= d * d;
            float k = Math.max(2f, state.race.scores[plIdx].bronze * 3 + state.race.scores[plIdx].silver);
            if (race.scores[plIdx].gold > state.race.scores[plIdx].gold)
                res *= k;
            else if (race.scores[plIdx].bronze > state.race.scores[plIdx].bronze)
                res /= k * 2;
            return Math.min(max, res);
        }
    }

    static float archeryFit(State state, State temp, int plIdx) {
        float max = 0.85f;
        if (state.archery.isGameOver) {
            return max;
        } else {
            Archery archery = temp.archery;
            float res = Math.max((100 - archery.dist2(plIdx)) / 100f, 0.001f);
            float k = Math.max(2f, state.archery.scores[plIdx].bronze * 3 + state.archery.scores[plIdx].silver);
            if (archery.scores[plIdx].gold > state.archery.scores[plIdx].gold)
                res *= k;
            else if (archery.scores[plIdx].bronze > state.archery.scores[plIdx].bronze)
                res /= k * 2;
            return Math.min(max, res);
        }
    }

    static float skatingFit(State state, State temp, int plIdx) {
        float max = 0.85f;
        if (state.skating.isGameOver) {
            return max;
        } else {
            Skating skating = temp.skating;
            float res = skating.travelled[plIdx] / temp.skating.bestResult;
            float k = Math.max(2f, state.skating.scores[plIdx].bronze * 3 + state.skating.scores[plIdx].silver);
            if (skating.scores[plIdx].gold > state.skating.scores[plIdx].gold)
                res *= k;
            else if (skating.scores[plIdx].bronze > state.skating.scores[plIdx].bronze)
                res /= k * 2;
            return Math.min(max, res);
        }
    }

    static float divingFit(State state, State temp, int plIdx) {
        float max = 0.85f;
        if (state.diving.isGameOver) {
            return max;
        } else {
            Diving diving = temp.diving;
            float res = diving.points[plIdx] / diving.bestResult;
            float k = Math.max(2f, state.diving.scores[plIdx].bronze * 3 + state.diving.scores[plIdx].silver);
            if (diving.scores[plIdx].gold > state.diving.scores[plIdx].gold)
                res *= k;
            else if (diving.scores[plIdx].bronze > state.diving.scores[plIdx].bronze)
                res /= k * 2;
            return Math.min(max, res);
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

class State {
    final Race race = new Race();
    final Archery archery = new Archery();
    final Skating skating = new Skating();
    final Diving diving = new Diving();
    final AbstractGame[] games = new AbstractGame[] { race, archery, skating, diving };
    int turn = 0;

    void readScore(int playerIdx, String scoreInfo) {
        String[] scoreParts = scoreInfo.split(" ");
        for (int j = 0; j < Player.nbGames; ++j)
            games[j].readScore(scoreParts, playerIdx, j);
    }

    void read(Scanner in) {
        for (int j = 0; j < Player.nbGames; ++j)
            games[j].read(in);
    }

    void calcCmdPriority() {
        for (int j = 0; j < Player.nbGames; ++j) {
            games[j].calcCmdPriority();
//            games[j].printPriorities();
        }
    }

    void printState() {
        race.printState();
//        archery.printState();
//        skating.printState();
//        diving.printState();
    }

    void set(State orig) {
        for (int i = 0; i < Player.nbGames; ++i) {
            games[i].set(orig.games[i]);
        }
        this.turn = orig.turn;
    }

    boolean move(int cmd0, int cmd1, int cmd2) {
        boolean finished = true;
        for (int i = 0; i < 4; ++i) {
            finished &= games[i].move(cmd0, cmd1, cmd2);
        }
        ++turn;

        return finished || turn == 100;
    }

    int totalGold(int plIdx) {
        return race.scores[plIdx].gold + archery.scores[plIdx].gold
                + skating.scores[plIdx].gold + diving.scores[plIdx].gold;
    }

    int totalScore(int playerIdx) {
        return race.scores[playerIdx].total() * archery.scores[playerIdx].total()
                * skating.scores[playerIdx].total() * diving.scores[playerIdx].total();
    }

    boolean isWon(int playerIdx) {
        int totalScore = totalScore(playerIdx);
        for (int i = 0; i < 3; ++i) {
            if (i != playerIdx && totalScore(i) > totalScore)
                return false;
        }
        return true;
    }

    boolean isLose(int playerIdx) {
        int totalScore = totalScore(playerIdx);
        for (int i = 0; i < 3; ++i) {
            if (i != playerIdx && totalScore(i) < totalScore)
                return false;
        }
        return true;
    }
}

class Race extends AbstractGame {
    static final int LAST_POS = 29;
    private static final int[] moves = new int[] { 2, 1, 2, 3 };

    final int[] positions = new int[3];
    final int[] stuns = new int[3];
    int turn = 0;

    @Override
    void read(Scanner in) {
        super.read(in);
        for (int i = 0; i < 3; ++i)
            positions[i] = in.nextInt();
        for (int i = 0; i < 3; ++i)
            stuns[i] = in.nextInt();
        in.nextInt();

        if (isGameOver) turn = -1;
        else ++turn;

        if (bestResult == -1f && !isGameOver) {
            int turns = 0;
            int position = 0;
            outer: while (true) {
                ++turns;
                for (int i = 0; i < 3; ++i) {
                    if (position + i + 1 >= gpu.length()) {
                        break outer;
                    }
                    char ch = gpu.charAt(position + i + 1);
                    if (ch == '#') {
                        if (i == 0) {
                            position += 2;
                        } else {
                            position += i;
                        }
                        continue outer;
                    }
                }
                position += 3;
            }
            bestResult = turns;
            System.err.println("Race best result = " + bestResult);
        }
    }

    @Override
    void calcCmdPriority() {
        super.calcCmdPriority();
        if (isGameOver()) return;
        if (stuns[Player.playerIdx] > 0) return;
        int myPos = positions[Player.playerIdx];

        if (myPos == LAST_POS - 1) return;


        int cmdIdx = 3;
        for (int k = 0, n = Math.min(3, gpu.length() - myPos - 1); k < n; ++k) {
            char ch = gpu.charAt(myPos + k + 1);
            if (ch == '#' && k < cmdIdx) {
                cmdIdx = k;
                break;
            }
        }

        switch (cmdIdx) {
            case 0 -> {
                cmdPriority[0] = 4;
                cmdPriority[1] = -2;
                cmdPriority[2] = -2;
                cmdPriority[3] = -2;
            }
            case 1 -> {
                cmdPriority[0] = -2;
                cmdPriority[1] = 4;
                cmdPriority[2] = -2;
                cmdPriority[3] = -2;
            }
            case 2 -> {
                cmdPriority[0] = 4;
                cmdPriority[1] = 1;
                cmdPriority[2] = 4;
                cmdPriority[3] = -2;
            }
            case 3 -> {
                switch (myPos) {
                    case LAST_POS - 3 -> cmdPriority[3] = 15;
                    case LAST_POS - 2 -> cmdPriority[1] = -15;
                    default -> {
                        cmdPriority[0] = 2;
                        cmdPriority[2] = 2;
                        cmdPriority[3] = 4;
                    }
                }
            }
        }

        Score myScore = scores[Player.playerIdx];
        if (3 * myScore.bronze > myScore.silver + myScore.gold) mul(3);
        if (hasHeadStart()) div(2);
    }

    @Override
    void set(AbstractGame orig) {
        super.set(orig);
        Race race = (Race) orig;
        System.arraycopy(race.positions, 0, this.positions, 0, 3);
        System.arraycopy(race.stuns, 0, this.stuns, 0, 3);
        this.turn = race.turn;
    }

    @Override
    boolean move(int cmd0, int cmd1, int cmd2) {
        if (isGameOver()) return true;
        for (int pos : positions)
            if (pos == LAST_POS) return true;

        ++turn;
        boolean finished = false;
        int move = moves[cmd0];
        int position = positions[0];
        if (stuns[0] == 0) {
            if (position + move > LAST_POS) move = LAST_POS - position;
            boolean stunned = false;
            for (int i = 0; i < move; ++i) {
                if (gpu.charAt(position + i + 1) == '#' && !(i == 0 && cmd0 == 0)) {
                    positions[0] = position + i + 1;
                    stuns[0] = 2;
                    stunned = true;
                    break;
                }
            }
            if (!stunned) positions[0] = position + move;
            finished |= positions[0] == LAST_POS;
        } else {
            --stuns[0];
        }

        move = moves[cmd1];
        position = positions[1];
        if (stuns[1] == 0) {
            if (position + move > LAST_POS) move = LAST_POS - position;
            boolean stunned = false;
            for (int i = 0; i < move; ++i) {
                if (gpu.charAt(position + i + 1) == '#' && !(i == 0 && cmd1 == 0)) {
                    positions[1] = position + i + 1;
                    stuns[1] = 2;
                    stunned = true;
                    break;
                }
            }
            if (!stunned) positions[1] = position + move;
            finished |= positions[1] == LAST_POS;
        } else {
            --stuns[1];
        }

        move = moves[cmd2];
        position = positions[2];
        if (stuns[2] == 0) {
            if (position + move > LAST_POS) move = LAST_POS - position;
            boolean stunned = false;
            for (int i = 0; i < move; ++i) {
                if (gpu.charAt(position + i + 1) == '#' && !(i == 0 && cmd2 == 0)) {
                    positions[2] = position + i + 1;
                    stuns[2] = 2;
                    stunned = true;
                    break;
                }
            }
            if (!stunned) positions[2] = position + move;
            finished |= positions[2] == LAST_POS;
        } else {
            --stuns[2];
        }

        if (finished) {
//            for (int i = 0; i < 3; ++i) {
//                System.err.println("player " + i + ": pos = " + positions[i]);
//                scores[i].print();
//            }

            for (int i = 0; i < 3; ++i) {
                for (int j = i + 1; j < 3; ++j) {
                    if (positions[rank[i]] < positions[rank[j]]) {
                        int t = rank[i];
                        rank[i] = rank[j];
                        rank[j] = t;
                    }
                }
            }

            ++scores[rank[0]].gold;
            if (positions[rank[1]] < positions[rank[0]]) ++scores[rank[1]].silver;
            else ++scores[rank[1]].gold;
            if (positions[rank[2]] < positions[rank[1]]) ++scores[rank[2]].bronze;
            else if (positions[rank[2]] < positions[rank[0]]) ++scores[rank[2]].silver;
            else ++scores[rank[2]].gold;

//            for (int i = 0; i < 3; ++i) {
//                scores[i].print();
//            }
        }

        return finished;
    }

    boolean hasHeadStart() {
        for (int i = 0; i < 3; ++i) {
            if (i != Player.playerIdx && positions[Player.playerIdx] - positions[i] - stuns[i] * 2 < 6)
                return false;
        }
        return true;
    }

    void printState() {
        System.err.println("Turn " + turn);
        for (int i = 0; i < 3; ++i) {
            System.err.println("Race - Player#" + i + ": pos = " + positions[i] + ", stun = " + stuns[i]);
        }
    }
}

class Archery extends AbstractGame {
    final int[][] coords = new int[3][2];
    private int turn;

    @Override
    void read(Scanner in) {
        super.read(in);
        for (int i = 0; i < 3; ++i) {
            coords[i][0] = in.nextInt();
            coords[i][1] = in.nextInt();
        }

        in.nextInt();

        if (bestResult == -1f && !isGameOver) {
            bestResult = 0;
        }
    }

    int windAt(int idx) {
        return Character.digit(gpu.charAt(idx), 10);
    }

    @Override
    void calcCmdPriority() {
        super.calcCmdPriority();
        if (isGameOver()) return;

        int wind = windAt(0);
        if (coords[Player.playerIdx][0] > 0) {
            cmdPriority[1] = wind;
            cmdPriority[3] = -wind;
        } else if (coords[Player.playerIdx][0] < 0) {
            cmdPriority[3] = wind;
            cmdPriority[1] = -wind;
        }
        if (coords[Player.playerIdx][1] > 0) {
            cmdPriority[0] = wind;
            cmdPriority[2] = -wind;
        } else if (coords[Player.playerIdx][1] < 0) {
            cmdPriority[2] = wind;
            cmdPriority[0] = -wind;
        }

        if (gpu.length() > 3) div(gpu.length() / 3);

        Score myScore = scores[Player.playerIdx];
        if (3 * myScore.bronze > myScore.silver + myScore.gold) mul(3);
    }

    void printState() {
        System.err.println("Archery - my coords: x = " + coords[Player.playerIdx][0] + ", y = " + coords[Player.playerIdx][1]);
    }

    @Override
    void set(AbstractGame orig) {
        super.set(orig);
        Archery archery = (Archery) orig;
        for (int i = 0; i < 3; ++i)
            System.arraycopy(archery.coords[i], 0, this.coords[i], 0, 2);
        this.turn = archery.turn;
    }

    @Override
    boolean move(int cmd0, int cmd1, int cmd2) {
        if (isGameOver() || turn == gpu.length()) return true;

        final int wind = windAt(turn++);
        boolean finished = false;
        int plIdx = 0;
        switch (cmd0) {
            case 0 -> {
                coords[plIdx][1] -= wind;
                if (coords[plIdx][1] < -20) coords[plIdx][1] = -20;
            }
            case 1 -> {
                coords[plIdx][0] -= wind;
                if (coords[plIdx][0] < -20) coords[plIdx][0] = -20;
            }
            case 2 -> {
                coords[plIdx][1] += wind;
                if (coords[plIdx][1] > 20) coords[plIdx][1] = 20;
            }
            case 3 -> {
                coords[plIdx][0] += wind;
                if (coords[plIdx][0] > 20) coords[plIdx][0] = 20;
            }
        }

        plIdx = 1;
        switch (cmd1) {
            case 0 -> {
                coords[plIdx][1] -= wind;
                if (coords[plIdx][1] < -20) coords[plIdx][1] = -20;
            }
            case 1 -> {
                coords[plIdx][0] -= wind;
                if (coords[plIdx][0] < -20) coords[plIdx][0] = -20;
            }
            case 2 -> {
                coords[plIdx][1] += wind;
                if (coords[plIdx][1] > 20) coords[plIdx][1] = 20;
            }
            case 3 -> {
                coords[plIdx][0] += wind;
                if (coords[plIdx][0] > 20) coords[plIdx][0] = 20;
            }
        }

        plIdx = 2;
        switch (cmd2) {
            case 0 -> {
                coords[plIdx][1] -= wind;
                if (coords[plIdx][1] < -20) coords[plIdx][1] = -20;
            }
            case 1 -> {
                coords[plIdx][0] -= wind;
                if (coords[plIdx][0] < -20) coords[plIdx][0] = -20;
            }
            case 2 -> {
                coords[plIdx][1] += wind;
                if (coords[plIdx][1] > 20) coords[plIdx][1] = 20;
            }
            case 3 -> {
                coords[plIdx][0] += wind;
                if (coords[plIdx][0] > 20) coords[plIdx][0] = 20;
            }
        }

        if (turn == gpu.length()) {
            finished = true;

//            for (int i = 0; i < 3; ++i) {
//                System.err.println("player " + i + ": d = " + (coords[i][0] * coords[i][0] + coords[i][1] * coords[i][1]));
//                scores[i].print();
//            }

            for (int i = 0; i < 3; ++i) {
                int xi = coords[rank[i]][0];
                int yi = coords[rank[i]][1];
                for (int j = i + 1; j < 3; ++j) {
                    int xj = coords[rank[j]][0];
                    int yj = coords[rank[j]][1];
                    if (xi * xi + yi * yi > xj * xj + yj * yj) {
                        int t = rank[i];
                        rank[i] = rank[j];
                        rank[j] = t;
                    }
                }
            }

            int d0 = coords[rank[0]][0] * coords[rank[0]][0] + coords[rank[0]][1] * coords[rank[0]][1];
            int d1 = coords[rank[1]][0] * coords[rank[1]][0] + coords[rank[1]][1] * coords[rank[1]][1];
            int d2 = coords[rank[2]][0] * coords[rank[2]][0] + coords[rank[2]][1] * coords[rank[2]][1];
            ++scores[rank[0]].gold;
            if (d1 > d0) ++scores[rank[1]].silver;
            else ++scores[rank[1]].gold;
            if (d2 > d1) ++scores[rank[2]].bronze;
            else if (d2 > d0) ++scores[rank[2]].silver;
            else ++scores[rank[2]].gold;
        }

        return finished;
    }

    int dist2(int plIdx) {
        int x = coords[plIdx][0];
        int y = coords[plIdx][1];
        return x * x + y * y;
    }
}

class Skating extends AbstractGame {
    static final int[] move = new int[] {1, 2, 2, 3};
    static final int[] dRisk = new int[] {-1, 0, 1, 2};

    final int[] travelled = new int[3];
    final int[] risk = new int[3];
    final int[] cmdToRisk = new int[4];
    final int[] riskToCmd = new int[4];
    int turnsLeft;

    @Override
    void read(Scanner in) {
        super.read(in);
        if (!isGameOver()) {
            for (int i = 0; i < 4; ++i) {
                switch (gpu.charAt(i)) {
                    case 'U' -> {
                        cmdToRisk[0] = i;
                        riskToCmd[i] = 0;
                    }
                    case 'L' -> {
                        cmdToRisk[1] = i;
                        riskToCmd[i] = 1;
                    }
                    case 'D' -> {
                        cmdToRisk[2] = i;
                        riskToCmd[i] = 2;
                    }
                    case 'R' -> {
                        cmdToRisk[3] = i;
                        riskToCmd[i] = 3;
                    }
                }
            }
        }
        for (int i = 0; i < 3; ++i) {
            travelled[i] = in.nextInt();
        }
        for (int i = 0; i < 3; ++i) {
            risk[i]= in.nextInt();
        }

        turnsLeft = in.nextInt();

        if (bestResult == -1f && !isGameOver) {
            bestResult = turnsLeft * 10f / 7;
            System.err.println("Skating best result = " + bestResult);
        }
    }

    @Override
    void calcCmdPriority() {
        super.calcCmdPriority();
        if (isGameOver()) return;
        if (risk[Player.playerIdx] < 0) return;
        if (maxLeading() > 3 * turnsLeft) return;

        if (turnsLeft == 1) {
            cmdPriority[riskToCmd[3]] = 5;
            cmdPriority[riskToCmd[2]] = 3;
            cmdPriority[riskToCmd[1]] = 3;
            cmdPriority[riskToCmd[0]] = 0;
        } else {
            for (int i = 0; i < 4; ++i) {
                int ro = cmdToRisk[i];
                int priority = move[ro] * 2 - dRisk[ro];
                if (risk[Player.playerIdx] + dRisk[ro] >= 5) priority /= 2;
                cmdPriority[i] = priority;
            }
        }

        Score myScore = scores[Player.playerIdx];
        if (3 * myScore.bronze > myScore.silver + myScore.gold) mul(3);
    }

    void printState() {
        System.err.println("Skating - risk order = " + gpu);
        for (int i = 0; i < 3; ++i) {
            System.err.println("Skating - Player #" + i + ": travelled = " + travelled[i] + ", risk = " + risk[i]);
        }
        System.err.println("Skating - turns left = " + turnsLeft);
    }

    int maxLeading() {
        int m = -999;
        for (int i = 0; i < 3; ++i) {
            if (i != Player.playerIdx) {
                int l = travelled[Player.playerIdx] - travelled[i];
                if (l > m) m = l;
            }
        }
        return m;
    }

    @Override
    void set(AbstractGame orig) {
        super.set(orig);
        Skating skating = (Skating) orig;
        System.arraycopy(skating.travelled, 0, this.travelled, 0, 3);
        System.arraycopy(skating.risk, 0, this.risk, 0, 3);
        System.arraycopy(skating.riskToCmd, 0, this.riskToCmd, 0, 4);
        System.arraycopy(skating.cmdToRisk, 0, this.cmdToRisk, 0, 4);
        this.turnsLeft = skating.turnsLeft;
    }

    @Override
    boolean move(int cmd0, int cmd1, int cmd2) {
        if (isGameOver || turnsLeft == 0) return true;

        boolean finished = false;
        int plIdx = 0;
        int r = cmdToRisk[cmd0];
        if (risk[plIdx] >= 0) {
            travelled[plIdx] += move[r];
            risk[plIdx] += dRisk[r];
            if (risk[plIdx] < 0) risk[plIdx] = 0;
        } else {
            ++risk[plIdx];
        }

        plIdx = 1;
        r = cmdToRisk[cmd1];
        if (risk[plIdx] >= 0) {
            travelled[plIdx] += move[r];
            risk[plIdx] += dRisk[r];
            if (risk[plIdx] < 0) risk[plIdx] = 0;
        } else {
            ++risk[plIdx];
        }

        plIdx = 2;
        r = cmdToRisk[cmd2];
        if (risk[plIdx] >= 0) {
            travelled[plIdx] += move[r];
            risk[plIdx] += dRisk[r];
            if (risk[plIdx] < 0) risk[plIdx] = 0;
        } else {
            ++risk[plIdx];
        }

        if (travelled[0] % 10 == travelled[1] % 10 && travelled[1] % 10 == travelled[2] % 10) {
            for (int i = 0; i < 3; ++i) {
                if (risk[i] >= 0) risk[i] += 2;
            }
        } else {
            outer: for (int i = 0; i < 3; ++i) {
                for (int j = i + 1; j < 3; ++j) {
                    if (travelled[i] % 10 == travelled[j] % 10) {
                        if (risk[i] >= 0) risk[i] += 2;
                        if (risk[j] >= 0) risk[j] += 2;
                        break outer;
                    }
                }
            }
        }
        for (int i = 0; i < 3; ++i) {
            if (risk[i] >= 5) risk[i] = -2;
        }

        if (--turnsLeft == 0) {
            finished = true;

            for (int i = 0; i < 3; ++i) {
                for (int j = i + 1; j < 3; ++j) {
                    if (travelled[rank[i]] < travelled[rank[j]]) {
                        int t = rank[i];
                        rank[i] = rank[j];
                        rank[j] = t;
                    }
                }
            }

            ++scores[rank[0]].gold;
            if (travelled[rank[1]] < travelled[rank[0]]) ++scores[rank[1]].silver;
            else ++scores[rank[1]].gold;
            if (travelled[rank[2]] < travelled[rank[1]]) ++scores[rank[2]].bronze;
            else if (travelled[rank[2]] < travelled[rank[0]]) ++scores[rank[2]].silver;
            else ++scores[rank[2]].gold;
        } else {
            int cmdToRisk0 = cmdToRisk[0];
            int riskToCmd0 = riskToCmd[0];
            cmdToRisk[0] = cmdToRisk[3];
            riskToCmd[0] = riskToCmd[3];
            cmdToRisk[3] = cmdToRisk[1];
            riskToCmd[3] = riskToCmd[1];
            cmdToRisk[1] = cmdToRisk[2];
            riskToCmd[1] = riskToCmd[2];
            cmdToRisk[2] = cmdToRisk0;
            riskToCmd[2] = riskToCmd0;
        }

        return finished;
    }
}

class Diving extends AbstractGame {
    final int[] points = new int[3];
    final int[] combo = new int[3];
    final List<Integer> goal = new ArrayList<>();

    @Override
    void read(Scanner in) {
        super.read(in);
        goal.clear();
        if (!isGameOver()) {
            for (int i = gpu.length(); --i >= 0;) {
                switch (gpu.charAt(i)) {
                    case 'U' -> goal.add(0);
                    case 'L' -> goal.add(1);
                    case 'D' -> goal.add(2);
                    case 'R' -> goal.add(3);
                }
            }
        }
        for (int i = 0; i < 3; ++i) {
            points[i] = in.nextInt();
        }
        for (int i = 0; i < 3; ++i) {
            combo[i]= in.nextInt();
        }

        in.nextInt();

        if (bestResult == -1f && !isGameOver) {
            bestResult = calcTheorMax(0);
            System.err.println("Diving best result = " + bestResult);
        }
    }

    @Override
    void calcCmdPriority() {
        super.calcCmdPriority();
        if (isGameOver()) return;
        if (isWon()) return;

        int myCombo = combo[Player.playerIdx];
        int nextCmdIdx = next();
        for (int i = 0; i < 4; ++i)
            cmdPriority[i] = Math.min(-myCombo, -3);
        cmdPriority[nextCmdIdx] = Math.max(myCombo, 3);

        boolean keepLeading = true;
        for (int i = 0; i < 3; ++i) {
            if (i != Player.playerIdx && points[i] + combo[i] + 1 > points[Player.playerIdx]) {
                keepLeading = false;
                break;
            }
        }
        if (keepLeading) div(2);
        Score myScore = scores[Player.playerIdx];
        if (3 * myScore.bronze > myScore.silver + myScore.gold) mul(3);
    }

    int next() {
        return goal.get(goal.size() - 1);
    }

    int calcTheorMax(int i) {
        int max = points[i], c = combo[i];
        for (int turn = 0, left = goal.size(); turn < left; ++turn) {
            max += ++c;
        }
        return max;
    }

    boolean isWon() {
        int myPoints = points[Player.playerIdx];
        for (int i = 0; i < 3; ++i) {
            if (i != Player.playerIdx && calcTheorMax(i) > myPoints) {
                return false;
            }
        }
        return true;
    }

    void printState() {
        for (int i = 0; i < 3; ++i) {
            System.err.println("Diving - Player #" + i + ": combo = " + combo[i] + ", points = " + points[i]);
        }
    }

    @Override
    void set(AbstractGame orig) {
        super.set(orig);
        Diving diving = (Diving) orig;
        System.arraycopy(diving.combo, 0, this.combo, 0, 3);
        System.arraycopy(diving.points, 0, this.points, 0, 3);
        this.goal.clear();
        this.goal.addAll(diving.goal);
    }

    @Override
    boolean move(int cmd0, int cmd1, int cmd2) {
        if (isGameOver || goal.isEmpty()) return true;

        boolean finished = false;
        int expectedCmd = goal.remove(goal.size() - 1);
        int plIdx = 0;
        if (expectedCmd == cmd0)
            points[plIdx] += ++combo[plIdx];
        else
            combo[plIdx] = 0;

        plIdx = 1;
        if (expectedCmd == cmd1)
            points[plIdx] += ++combo[plIdx];
        else
            combo[plIdx] = 0;

        plIdx = 2;
        if (expectedCmd == cmd2)
            points[plIdx] += ++combo[plIdx];
        else
            combo[plIdx] = 0;

        if (goal.isEmpty()) {
            finished = true;

            for (int i = 0; i < 3; ++i) {
                for (int j = i + 1; j < 3; ++j) {
                    if (points[rank[i]] < points[rank[j]]) {
                        int t = rank[i];
                        rank[i] = rank[j];
                        rank[j] = t;
                    }
                }
            }

            ++scores[rank[0]].gold;
            if (points[rank[1]] < points[rank[0]]) ++scores[rank[1]].silver;
            else ++scores[rank[1]].gold;
            if (points[rank[2]] < points[rank[1]]) ++scores[rank[2]].bronze;
            else if (points[rank[2]] < points[rank[0]]) ++scores[rank[2]].silver;
            else ++scores[rank[2]].gold;
        }

        return finished;
    }
}

class AbstractGame {
    static final int[] rank = new int[] { 0, 1, 2 };

    final Score[] scores = new Score[3];
    String gpu;
    boolean isGameOver;
    final int[] cmdPriority = new int[4];
    float bestResult = -1f;

    {
        for (int i = 0; i < 3; ++i)
            scores[i] = new Score();
    }

    void read(Scanner in) {
        gpu = in.next();
        isGameOver = "GAME_OVER".equals(gpu);
        if (isGameOver) bestResult = -1f;
    }

    void readScore(String[] scoreParts, int playerIdx, int offset) {
        Score score = scores[playerIdx];
        score.gold = Integer.parseInt(scoreParts[3 * offset + 1]);
        score.silver = Integer.parseInt(scoreParts[3 * offset + 2]);
        score.bronze = Integer.parseInt(scoreParts[3 * offset + 3]);
    }

    boolean isGameOver() {
        return isGameOver;
    }

    void calcCmdPriority() {
        for (int i = 0; i < 4; ++i) cmdPriority[i] = 0;
    }

    void set(AbstractGame orig) {
        for (int i = 0; i < 3; ++i) this.scores[i].set(orig.scores[i]);
        this.gpu = orig.gpu;
        this.isGameOver = orig.isGameOver;
        this.bestResult = orig.bestResult;
    }

    boolean move(int cmd0, int cmd1, int cmd2) { return true; }

    void printPriorities() {
        System.err.print(getClass().getName());
        System.err.print(": ");
        System.err.print(cmdPriority[0]);
        System.err.print(", ");
        System.err.print(cmdPriority[1]);
        System.err.print(", ");
        System.err.print(cmdPriority[2]);
        System.err.print(", ");
        System.err.print(cmdPriority[3]);
        System.err.println();
    }

    void mul(int k) {
        for (int i = 0; i < 4; ++i) cmdPriority[i] *= k;
    }

    void div(int k) {
        for (int i = 0; i < 4; ++i) cmdPriority[i] /= k;
    }
}

class Score {
    int gold;
    int silver;
    int bronze;

    int total() {
        return gold * 3 + silver;
    }

    void set(Score orig) {
        this.gold = orig.gold;
        this.silver = orig.silver;
        this.bronze = orig.bronze;
    }

    void print() {
        System.err.println("g=" + gold + " s=" + silver + " b=" + bronze + " t=" + total());
    }
}
