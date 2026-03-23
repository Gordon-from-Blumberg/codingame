import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final long firstTurnLimit = toNano(1000, .95f);
    static final long turnLimit = toNano(50, .90f);
    static final int maxTurn = 200;
    static final float mutationChance = 0.03f;
    static final int generationSize = 24;
    static final int moveSequenceSize = 32;
    static final short[][] generation = new short[generationSize][moveSequenceSize];
    static final short[] bestMoveSequence = new short[moveSequenceSize];
    static final float[] fitness = new float[generationSize];
    static final float[] probs = new float[generationSize];
    static final short[][] temp = new short[generationSize][moveSequenceSize];
    static final float baseFitness = 1f;

    static long geneticTime = 0, generationTime = 0, simulationTime = 0, moveTime = 0, setStateTime = 0, setDirsTime = 0;

    static final char platformChar = '#';
    static final char emptyChar = '.';
    static final char powerSourceChar = '$';
    static final Random rand = new Random();

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int myId = in.nextInt();
        long start = System.nanoTime();
        int width = in.nextInt();
        int height = in.nextInt();

        final char[][] baseGrid = new char[width][height];

        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; ++i) {
            String row = in.nextLine();
            for (int j = 0; j < width; ++j) {
                baseGrid[j][i] = row.charAt(j);
            }
        }

        int snakesPerPlayer = in.nextInt();
        final State baseState = new State(width, height, snakesPerPlayer << 1, true);
        final State state = new State(width, height, snakesPerPlayer << 1, false);
        final String[] snakeBodies = new String[snakesPerPlayer << 1];

        for (int i = 0; i < snakesPerPlayer; i++) {
            int mySnakeId = in.nextInt();
            Snake snake = new Snake(mySnakeId, true);
            baseState.snakeMap[mySnakeId] = snake;
        }
        for (int i = 0; i < snakesPerPlayer; i++) {
            int oppSnakeId = in.nextInt();
            Snake snake = new Snake(oppSnakeId, false);
            baseState.snakeMap[oppSnakeId] = snake;
        }

        // game loop
        while (true) {
            baseState.newTurn(baseGrid);

            int powerSourceCount = in.nextInt();
            long s = System.nanoTime();
            long expiryTime = baseState.turn == 1 ? start + firstTurnLimit : System.nanoTime() + turnLimit;

            for (int i = 0; i < powerSourceCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                baseState.grid[x][y] = powerSourceChar;
                baseState.powerSources.add(State.packCoords(x, y));
            }

            Arrays.fill(snakeBodies, null);
            int snakeCount = in.nextInt();
            for (int i = 0; i < snakeCount; i++) {
                int snakeId = in.nextInt();
                String body = in.next();
                snakeBodies[snakeId] = body;
            }

            baseState.updateSnakes(snakeBodies);

            genetic(baseState, state, expiryTime);
            for (Snake snake : baseState.snakeMap) {
                if (snake != null && snake.mine && snake.head != null) {
                    System.out.print(snake.id);
                    System.out.print(' ');
                    System.out.print(snake.dir);
                    System.out.print(' ');
                    System.out.print(baseState.generation);
                    System.out.print(';');
                }
            }
            System.out.println();
        }
    }

    static void genetic(State baseState, State state, long expiryTime) {
        // count of snakes for one player
        final int turn = baseState.turn;
        final int allSnakeCount = baseState.snakeMap.length;
        final int snakeCount = allSnakeCount >> 1;
        final int width = baseState.grid.length;

        int myBaseScore = 0, myBaseSnakeCount = 0;
        for (Snake snake : baseState.snakeMap) {
            if (snake.head != null) {
                if (snake.mine) {
                    ++myBaseSnakeCount;
                    myBaseScore += snake.parts.size();
                }
            }
        }
        final float loseScoreCoef = 0.5f / myBaseScore;

        int generationNumber = 0;
        // simulation loop
        while (System.nanoTime() < expiryTime) {
//        while (generationNumber < 500) {
            long genTime = System.nanoTime();
            if (generationNumber == 0) {
                // generate first generation
                for (int i = 0; i < generationSize; ++i) {
                    if (i == 0 && baseState.turn > 1) {
                        System.arraycopy(bestMoveSequence, 1, generation[0], 0, moveSequenceSize - 1);
                        generation[0][moveSequenceSize - 1] = randomMove(snakeCount);
                        continue;
                    }

                    for (int j = 0; j < moveSequenceSize; ++j) {
                        generation[i][j] = randomMove(snakeCount);
                    }
                }
            } else {
                // generate new generation by previous
                for (int i = 0; i < generationSize; ++i) probs[i] = fitness[i] * fitness[i];
//                System.arraycopy(fitness, 0, probs, 0, generationSize);
                for (int i = 0; i < generationSize; ++i) {
                    if (i == 0) {
                        System.arraycopy(bestMoveSequence, 0, temp[0], 0, moveSequenceSize);
                        continue;
                    }
                    int par1Idx = getRand(probs);
                    int par2Idx = getRand(probs);
//                    while (par2Idx == par1Idx) par2Idx = getRand(probs);

                    final short[] par1 = generation[par1Idx];
                    final short[] par2 = generation[par2Idx];
                    for (int j = 0; j < moveSequenceSize; ++j) {
                        final short par1Move = par1[j];
                        final short par2Move = par2[j];
                        short move = 0;
                        for (int s = 0; s < allSnakeCount; s += 2) {
                            short snakeMove = (short) (rand.nextFloat() < mutationChance
                                    ? (rand.nextInt(4) << s)
                                    : (rand.nextFloat() < 0.5f ? par1Move : par2Move) & (3 << s));
                            move |= snakeMove;
                        }
                        temp[i][j] = move;
                    }
                }

                for (int i = 0; i < generationSize; ++i) {
                    System.arraycopy(temp[i], 0, generation[i], 0, moveSequenceSize);
                }
            }
            generationTime += System.nanoTime() - genTime;

//            System.err.println("generate new generation took " + (System.nanoTime() - genTime));

            ++generationNumber;
            // simulation
            long simTime = System.nanoTime();
            for (int i = 0; i < generationSize; ++i) {
                // set state to base for new move sequence
                long stateStart = System.nanoTime();
                state.set(baseState);
                setStateTime += System.nanoTime() - stateStart;
                for (int j = 0; j < moveSequenceSize; ++j) {
                    long dirStart = System.nanoTime();
                    final short move = generation[i][j];

                    // set directions
                    int snakeInd = 0;
                    for (int s = 0; s < allSnakeCount; ++s) {
                        Snake snake = state.snakeMap[s];
                        if (snake.mine) {
                            snake.dir = Direction.ALL[(move >> (2 * snakeInd++)) & 3];
                        } else if (snake.head != null) {
                            SnakePart head = snake.head;
                            for (Direction d : Direction.ALL) {
                                int newX = head.x + d.x;
                                if (newX < 0 || newX >= width) continue;
                                char ch = state.get(newX, head.y + d.y);
                                if (ch == powerSourceChar) {
                                    snake.dir = d;
                                    break;
                                }
                                if (ch == emptyChar) {
                                    snake.dir = d;
                                }
                            }
                        }
                    }
                    setDirsTime += System.nanoTime() - dirStart;
                    long mvTime = System.nanoTime();
                    state.move();
                    moveTime += System.nanoTime() - mvTime;
                    int mySnakeCount = 0, oppSnakeCount = 0, myScore = 0;
                    for (Snake snake : state.snakeMap) {
                        if (snake.head != null) {
                            if (snake.mine) {
                                ++mySnakeCount;
                                myScore += snake.parts.size();
                            }
                            else {
                                ++oppSnakeCount;
                            }
                        }
                    }

                    state.myScoreSum += myScore * (1 + 0.1f * (state.turn - turn));

                    if (mySnakeCount == 0 || oppSnakeCount == 0
                            || state.turn == maxTurn || state.powerSources.isEmpty())
                        break;
                }

                // calculate fitness
                int myScore = 0, mySnakeCount = 0;
                int oppScore = 0;
                for (Snake snake : state.snakeMap) {
                    if (snake.head != null) {
                        if (snake.mine) {
                            ++mySnakeCount;
                            myScore += snake.parts.size();
                        } else {
                            oppScore += snake.parts.size();
                        }
                    }
                }

                float fit = state.myScoreSum / 10;
                if (mySnakeCount < myBaseSnakeCount) {
                    fit *= 1 + 0.5f * (mySnakeCount - 1);
                } else {
                    fit *= 5;
                }
                if (myScore < myBaseScore) {
                    fit *= 1 - loseScoreCoef * (myBaseScore - myScore);
                } else if (myScore > myBaseScore) {
                    float k = 1 + 0.5f * (myScore - myBaseScore);
                    fit *= k * k;
                }
                fit /= 1 + 0.2f * state.outOfScreen;
//                if (myScore > oppScore) {
//                    fit *= 1 + 0.2f * (myScore - oppScore);
//                }
//                fit *= 1 + 0.8f * (state.myMaxScore - myBaseScore);
//                fit *= 1 + 0.1f * state.mySnakeCountSum / (state.turn - turn);

                fitness[i] = fit;
            }
            simulationTime += System.nanoTime() - simTime;

//            System.err.println("simulation took " + (System.nanoTime() - genTime));
            float bestFit = -1f;
            int bestFitIdx = 0;
            for (int i = 0; i < generationSize; ++i) {
                float f = fitness[i];
                if (f > bestFit) {
                    bestFit = f;
                    bestFitIdx = i;
                }
            }

            System.arraycopy(generation[bestFitIdx],0, bestMoveSequence, 0, moveSequenceSize);
//            System.err.println("generation #" + generationNumber + "; best fit = " + bestFit + "; time = " + (System.nanoTime() - genTime));
        }

        System.err.println("generation = " + generationNumber);
        baseState.generation = generationNumber;
        setMove(baseState, snakeCount, bestMoveSequence[0]);
    }

    static void setMove(State state, int snakeCount, short move) {
        int snakeInd = 0;
        for (int i = 0; snakeInd < snakeCount; ++i) {
            Snake snake = state.snakeMap[i];
            if (snake != null && snake.mine) {
                snake.dir = Direction.ALL[(move >> (2 * snakeInd++)) & 3];
            }
        }
    }

    static short randomMove(int snakeCount) {
        short move = 0;
        for (int i = 0; i < snakeCount; ++i) {
            move |= (rand.nextInt(4) << (i * 2));
        }
        return move;
    }

    static long toNano(int ms, float coef) {
        return (long) (coef * 1_000_000 * ms);
    }

    static int getRand(float[] probs) {
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

enum Direction {
    UP(0, -1),
    RIGHT(1, 0),
    DOWN(0, 1),
    LEFT(-1, 0),
    ;

    static final Direction[] ALL = Direction.values();

    final int x;
    final int y;

    Direction(int x, int y) {
        this.x = x;
        this.y = y;
    }

    Direction next() {
        return ALL[(ordinal() + 1) % 4];
    }

    Direction prev() {
        return ALL[(ordinal() + 3) % 4];
    }

    Direction opposite() {
        return ALL[(ordinal() + 2) % 4];
    }

    char next(char[][] grid, int x, int y) {
        int nextX = x + this.x;
        if (nextX < 0 || nextX >= grid.length) return Player.emptyChar;
        int nextY = y + this.y;
        if (nextY < 0 || nextY >= grid[0].length) return Player.emptyChar;
        return grid[nextX][nextY];
    }
}

class State {
    static final int bits16 = (1 << 16) - 1;

    int turn;
    final char[][] grid;
    final Snake[] snakeMap;
    final Set<Integer> moveTargets;
    final Set<Integer> powerSources = new HashSet<>();
    float myScoreSum;
    int outOfScreen;
    int generation;

    State(int width, int height, int snakeCount, boolean base) {
        this.grid = new char[width][height];
        this.snakeMap = new Snake[snakeCount];
        this.moveTargets = base ? null : new HashSet<>();
    }

    void newTurn(char[][] baseGrid) {
        ++turn;
        for (int i = 0, w = baseGrid.length, h = baseGrid[0].length; i < w; ++i) {
            System.arraycopy(baseGrid[i], 0, grid[i], 0, h);
        }
        powerSources.clear();
    }

    void updateSnakes(String[] snakeBodies) {
        for (int i = 0, n = snakeBodies.length; i < n; ++i) {
            Snake snake = snakeMap[i];

            String body = snakeBodies[i];
            if (body == null) {
                snake.reset();
            } else {
                snake.read(body);
                // render snake on grid
                char ch = snake.idToChar();
                for (SnakePart snakePart : snake.parts) {
                    if (snakePart == snake.head)
                        set(snakePart.x, snakePart.y, Character.toUpperCase(ch));
                    else
                        set(snakePart.x, snakePart.y, ch);
                }
            }
        }
    }

    void move() {
        final char[][] grid = this.grid;
        final int width = grid.length;
        final int height = grid[0].length;

        // fill moveTargets - set of cells where snakes are going to move (empty or with power source)
        for (Snake snake : snakeMap) {
            if (snake.head == null) continue;

            char target = snake.next(grid);
            if (target == Player.emptyChar || target == Player.powerSourceChar) {
                int coords = packCoords(snake.head.x + snake.dir.x, snake.head.y + snake.dir.y);
                moveTargets.add(coords);
                if (target == Player.powerSourceChar) {
                    snake.grow = true;
                }
            }
        }

        // move snakes
        for (Snake snake : snakeMap) {
            if (snake.head == null) continue;

            int nextX = snake.head.x + snake.dir.x;
            int nextY = snake.head.y + snake.dir.y;
            char target = snake.next(grid);
            if (target == Player.emptyChar || target == Player.powerSourceChar) {
                SnakePart newHead = SnakePart.instance(snake);
                newHead.set(nextX, nextY);
                snake.parts.addFirst(newHead);
                char ch = snake.idToChar();
                set(snake.head.x, snake.head.y, ch);
                snake.head = newHead;
                set(snake.head.x, snake.head.y, Character.toUpperCase(ch));
                if (target == Player.powerSourceChar) {
                    powerSources.remove(packCoords(nextX, nextY));
                }
            }

            // head of another snake, but this cell was empty
            if (target >= 'A' && target <= 'Z' && moveTargets.contains(packCoords(nextX, nextY))) {
                snakeMap[Snake.charToId(target)].removeHead = true;
            }

            // when snake does not eat power source its tail is removed
            if (!snake.grow) {
                SnakePart tail = snake.parts.removeLast();
                set(tail.x, tail.y, Player.emptyChar);
                tail.free();
            }

            snake.grow = false;
        }

        // remove heads
        for (Snake snake : snakeMap) {
            if (snake.head == null || !snake.removeHead) continue;

            snake.removeHead = false;
            set(snake.head.x, snake.head.y, Player.emptyChar);
            snake.parts.removeFirst().free();
            snake.head = snake.parts.getFirst();
            toUpperCase(snake.head.x, snake.head.y);
        }

        // check for out of screen and minimum length
        for (Snake snake : snakeMap) {
            if (snake.head == null) continue;

            int minX = snake.head.x;
            int maxX = minX;
            for (SnakePart part : snake.parts) {
                if (part.x < minX) minX = part.x;
                if (part.x > maxX) maxX = part.x;
            }

            // is snake length < 3 or snake out of screen
            if (snake.parts.size() < 3 || maxX < 0 || minX >= width) {
                for (SnakePart part : snake.parts) {
                    set(part.x, part.y, Player.emptyChar);
                }
                snake.reset();
            }
        }

        // move snakes down due to gravity
        for (Snake snake : snakeMap) {
            if (snake.head == null) continue;

            char snakeChar = snake.idToChar();
            int minFall = height;
            for (SnakePart part : snake.parts) {
                int x = part.x;
                if (x < 0 || x >= width) continue;

                int fall = 0;
                // find
                while (fall < minFall) {
                    char cell = get(x, part.y + fall + 1);
                    if (cell == Player.emptyChar || Character.toLowerCase(cell) == snakeChar) {
                        ++fall;
                        continue;
                    }
                    break;
                }
                if (fall < minFall) minFall = fall;
            }

            if (minFall == 0) continue;

            // first remove
            for (SnakePart part : snake.parts) {
                set(part.x, part.y, Player.emptyChar);
            }
            // then render
            for (SnakePart part : snake.parts) {
                part.y += minFall;
                set(part.x, part.y, part == snake.head ? Character.toUpperCase(snakeChar) : snakeChar);
            }
        }

        for (Snake snake : snakeMap) {
            if (snake.head != null && snake.mine) {
                for (SnakePart part : snake.parts) {
                    if (part.x < 0 || part.x >= width || part.y < 0 || part.y >= height)
                        ++outOfScreen;
                }
            }
        }

        ++turn;
    }

    void set(State original) {
        turn = original.turn;
        for (int i = 0, w = grid.length, h = grid[0].length; i < w; ++i) {
            System.arraycopy(original.grid[i], 0, grid[i], 0, h);
        }
        myScoreSum = 0;
        outOfScreen = 0;
        for (int i = 0, n = snakeMap.length; i < n; ++i) {
            Snake origSnake = original.snakeMap[i];
            if (origSnake != null) {
                if (snakeMap[i] == null) {
                    snakeMap[i] = new Snake(origSnake.id, origSnake.mine);
                }
                snakeMap[i].set(origSnake);
            }
        }
        powerSources.clear();
        powerSources.addAll(original.powerSources);
    }

    char get(int x, int y) {
        return x < 0 || x >= grid.length || y < 0 || y >= grid[0].length ? Player.emptyChar : grid[x][y];
    }

    void set(int x, int y, char ch) {
        if (x >= 0 && x < grid.length && y >= 0 && y < grid[0].length) {
            grid[x][y] = ch;
        }
    }

    void toUpperCase(int x, int y) {
        if (x >= 0 && x < grid.length && y >= 0 && y < grid[0].length) {
            grid[x][y] = Character.toUpperCase(grid[x][y]);
        }
    }

    String printGrid() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < grid[0].length; ++y) {
            for (char[] chars : grid) {
                sb.append(chars[y]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    static int packCoords(int x, int y) {
        return (x << 16) | (y & bits16);
    }

    static int getX(int coords) {
        return coords >> 16;
    }

    static int getY(int coords) {
        return (coords << 16) >> 16;
    }
}

class Snake {
    int id;
    boolean mine;
    boolean grow;
    boolean removeHead;
    Direction dir = Direction.UP;
    SnakePart head;
    final Deque<SnakePart> parts = new ArrayDeque<>();

    Snake(int id, boolean mine) {
        this.id = id;
        this.mine = mine;
    }

    void read(String body) {
        String[] bodyParts = body.split(":");
        if (bodyParts.length < parts.size()) {
            parts.removeFirst().free();
        }
        int i = 0;
        for (SnakePart snakePart : parts) {
            snakePart.set(bodyParts[i++]);
        }
        while (i < bodyParts.length) {
            SnakePart snakePart = SnakePart.instance(this);
            snakePart.set(bodyParts[i++]);
            parts.add(snakePart);
        }
        head = parts.getFirst();
    }

    void set(Snake original) {
        if (original.head == null)
            return;
        dir = original.dir;
        int sizeDiff = parts.size() - original.parts.size();
        while (sizeDiff > 0) {
            parts.removeLast().free();
            --sizeDiff;
        }
        while (sizeDiff < 0) {
            parts.add(SnakePart.instance(this));
            ++sizeDiff;
        }

        Iterator<SnakePart> thisIt = parts.iterator();
        Iterator<SnakePart> origIt = original.parts.iterator();
        while (thisIt.hasNext()) {
            SnakePart origPart = origIt.next();
            thisIt.next().set(origPart.x, origPart.y);
        }
        head = parts.getFirst();
        grow = false;
    }

    void reset() {
        head = null;
        Iterator<SnakePart> it = parts.iterator();
        while (it.hasNext()) {
            SnakePart snakePart = it.next();
            snakePart.free();
            it.remove();
        }
    }

    char next(char[][] grid) {
        return dir.next(grid, head.x, head.y);
    }

    char idToChar() {
        return (char) ('a' + id);
    }

    static int charToId(char ch) {
        return Character.toLowerCase(ch) - 'a';
    }
}

class SnakePart {
    static final List<SnakePart> pool = new ArrayList<>();

    Snake snake;
    int x;
    int y;

    static SnakePart instance(Snake snake) {
        SnakePart snakePart = !pool.isEmpty() ? pool.remove(pool.size() - 1) : new SnakePart();
        snakePart.snake = snake;
        return snakePart;
    }

    void set(String partCoords) {
        String[] coords = partCoords.split(",");
        this.x = Integer.parseInt(coords[0]);
        this.y = Integer.parseInt(coords[1]);
    }

    void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void free() {
        pool.add(this);
    }
}