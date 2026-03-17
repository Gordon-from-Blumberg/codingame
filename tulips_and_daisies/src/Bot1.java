import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import static java.lang.Math.min;
import static java.lang.Math.max;

public class Bot1 {
    public static final int COST_COEF = 0;
    public static final Map<Character, int[]> COEF_MAP = new HashMap<>();
    public static final int[] OPP_FLOWER_COEF = new int[] {10, 11, 12};

    public static final char SOIL = 'S';
    public static final char GRASS = 'G';
    public static final char ROCKS = 'R';

    public static final Random RANDOM = new Random(47);

    static {
        COEF_MAP.put(SOIL, new int[] {1, 2, 3});
        COEF_MAP.put(GRASS, new int[] {4, 5, 6});
        COEF_MAP.put(ROCKS, new int[] {7, 8, 9});
    }

    private static final int PARAM_COUNT = 13;

    private Config config = new Config(PARAM_COUNT);

    private int width;
    private int height;

    private final Map<Character, Integer> costMap = new HashMap<>();

    private char ownFlowerType;
    private char oppFlowerType;

    private Cell[][] grid;

    private int turnsLeft;
    private int gold;
    private int oppGold;

    private PriorityQueue<Cell> priorityQueue;

    public void init(int width, int height,
                     int costSoil, int costGrass, int costRocks, int costFlower,
                     String ownFlowerType, String oppFlowerType) {

        this.width = width;
        this.height = height;

        costMap.clear();
        costMap.put(SOIL, costSoil);
        costMap.put(GRASS, costGrass);
        costMap.put(ROCKS, costRocks);

        this.ownFlowerType = ownFlowerType.toUpperCase().charAt(0);
        this.oppFlowerType = oppFlowerType.toUpperCase().charAt(0);

        costMap.put(this.oppFlowerType, costFlower);
        COEF_MAP.put(this.oppFlowerType, OPP_FLOWER_COEF);

        grid = new Cell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new Cell(x, y);
            }
        }

        priorityQueue = new PriorityQueue<>(width * height);
    }

    public String runTurn(int turnsLeft, int gold, int oppGold, String[] grid) {
        this.turnsLeft = turnsLeft;
        this.gold = gold;
        this.oppGold = oppGold;

        priorityQueue.clear();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                char type = grid[y].charAt(x);
                this.grid[x][y].type = type;

                if (type != ownFlowerType && costMap.get(type) > gold) {
                    this.grid[x][y].priority = getCellPriority(x, y);
                    priorityQueue.add(this.grid[x][y]);
                }
            }
        }

        return priorityQueue.peek().toString();
    }

    public void setRandomConfig() {
        for (int i = 0; i < PARAM_COUNT; i++) {
            config.params[i] = RANDOM.nextFloat();
        }
    }

    public int getHeight() {
        return height;
    }

    private float getCellPriority(int x, int y) {
        char cellType = grid[x][y].type;

        if (cellType == ownFlowerType || costMap.get(cellType) > gold) {
            return -1000;
        }

        float priority = - costMap.get(cellType) * config.params[COST_COEF];

        //north
        for (int i = 1, min = min(y, 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x][y - i].type)[i - 1]];
        }

        //north east
        for (int i = 1, min = min(min(width - 1 - x, y), 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x + i][y - i].type)[i - 1]];
        }

        //east
        for (int i = 1, min = min(width - 1 - x, 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x + i][y].type)[i - 1]];
        }

        //south east
        for (int i = 1, min = min(min(width - 1 - x, height - 1 - y), 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x + i][y + i].type)[i - 1]];
        }

        //south
        for (int i = 1, min = min(height - 1 - y, 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x][y + i].type)[i - 1]];
        }

        //south west
        for (int i = 1, min = min(min(x, height - 1 - y), 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x - i][y + i].type)[i - 1]];
        }

        //west
        for (int i = 1, min = min(x, 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x - i][y].type)[i - 1]];
        }

        //north west
        for (int i = 1, min = min(min(x, y), 3); i <= min; i++) {
            priority += config.params[COEF_MAP.get(grid[x - i][y - i].type)[i - 1]];
        }

        return priority;
    }

    public static class Config {
        private final float[] params;

        public Config(int paramCount) {
            params = new float[paramCount];
        }
    }

    private static class Cell implements Comparable<Cell> {
        private final int x;
        private final int y;
        private char type;
        private float priority;

        Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Cell o) {
            return (int) (priority - o.priority);
        }

        @Override
        public String toString() {
            return String.format("%d %d %10.3f", x, y, priority);
        }
    }
}
