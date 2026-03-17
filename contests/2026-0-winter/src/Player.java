import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final long firstTurnLimit = toNano(1000, .95f);
    static final long turnLimit = toNano(50, .95f);
    static final int maxTurn = 200;

    static final char platformChar = '#';
    static final char emptyChar = '.';
    static final char powerSourceChar = '$';

    static int enoughScores = 0;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        long start = System.nanoTime();
        int myId = in.nextInt();
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

//            if (baseState.turn == 1) {
//                int length = 0;
//                for (Snake snake : baseState.snakeMap) {
//                    if (snake.mine) length += snake.parts.size();
//                }
//                enoughScores = length + powerSourceCount / 2 + 1;
//            }

            genetic(baseState, state, expiryTime);

            for (Snake snake : baseState.snakeMap) {
                if (snake != null && snake.mine) {
                    System.out.print(snake.id);
                    System.out.print(' ');
                    System.out.print(snake.dir);
                    System.out.print(';');
                }
            }
            System.out.println();
        }
    }

    static void genetic(State baseState, State state, long expiryTime) {

    }

    static long toNano(int ms, float coef) {
        return (long) (coef * 1_000_000 * ms);
    }
}

enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    final int x;
    final int y;

    Direction(int x, int y) {
        this.x = x;
        this.y = y;
    }

    char next(char[][] grid, int x, int y) {
        int nextX = x + this.x;
        if (nextX < 0 || nextX == grid.length) return Player.emptyChar;
        int nextY = y + this.y;
        if (nextY < 0 || nextY == grid[0].length) return Player.emptyChar;
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
            if (snake == null) continue;

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

        // fill moveTargets - set of cells where snakes are going to move (empty or with power source)
        for (Snake snake : snakeMap) {
            if (snake == null || snake.head == null) continue;

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
            if (snake == null || snake.head == null) continue;

            int nextX = snake.head.x + snake.dir.x;
            int nextY = snake.head.y + snake.dir.y;
            char target = snake.next(grid);
            if (target == Player.emptyChar || target == Player.powerSourceChar) {
                SnakePart newHead = SnakePart.instance(snake);
                newHead.set(nextX, nextY);
                snake.parts.addFirst(newHead);
                char ch = snake.idToChar();
                set(snake.head.x, snake.head.y, Character.toLowerCase(ch));
                snake.head = newHead;
                set(snake.head.x, snake.head.y, ch);
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

            snake.grow = false;
        }

        // remove heads
        for (Snake snake : snakeMap) {
            if (snake == null || snake.head == null || !snake.removeHead) continue;

            snake.removeHead = false;
            set(snake.head.x, snake.head.y, Player.emptyChar);
            snake.parts.removeFirst().free();
            snake.head = snake.parts.getFirst();
            grid[snake.head.x][snake.head.y] = Character.toUpperCase(grid[snake.head.x][snake.head.y]);
        }

        // move snakes down due to gravity
        for (Snake snake : snakeMap) {
            if (snake == null || snake.head == null) continue;

            char snakeChar = snake.idToChar();
            int minFall = grid[0].length;
            for (SnakePart part : snake.parts) {
                int x = part.x;
                if (x < 0 || x >= grid.length) continue;

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

            for (SnakePart part : snake.parts) {
                set(part.x, part.y, Player.emptyChar);
                part.y += minFall;
                set(part.x, part.y, part == snake.head ? Character.toUpperCase(snakeChar) : snakeChar);
            }
        }
    }

    void set(State original) {
        turn = original.turn;
        for (int i = 0, w = grid.length, h = grid[0].length; i < w; ++i) {
            System.arraycopy(original.grid[i], 0, grid[i], 0, h);
        }
        for (int i = 0, n = snakeMap.length; i < n; ++i) {
            Snake origSnake = original.snakeMap[i];
            if (origSnake != null) {
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