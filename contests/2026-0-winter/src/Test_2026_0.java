import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test_2026_0 {
    public static void main(String[] args) throws Exception {
        final State baseState = new State(34, 19, 8, true);
        final State state = new State(34, 19, 8, false);

        try (BufferedReader br = open("resources/grid.txt")) {
            for (int i = 0; i < 34; ++i) {
                br.readLine().getChars(0, 19, baseState.grid[i], 0);
            }
        }

        for (int i = 0; i < 4; ++i) {
            baseState.snakeMap[i] = new Snake(i, true);
            baseState.snakeMap[i + 4] = new Snake(i + 4, false);
        }

        try (BufferedReader br = open("resources/power-sources.txt")) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] coords = line.split(", ");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                baseState.grid[x][y] = Player.powerSourceChar;
                baseState.powerSources.add(State.packCoords(x, y));
            }
        }

        try (BufferedReader br = open("resources/snakes.txt")) {
            baseState.updateSnakes(br.lines().toArray(String[]::new));
        }

        long start = System.nanoTime();
        Player.genetic(baseState, state, System.nanoTime() + 1000 * 1_000_000L);
        Player.geneticTime = System.nanoTime() - start;
        System.err.println(baseState.printGrid());

        System.err.println(Player.geneticTime + "\t- " + ((float) Player.geneticTime / Player.geneticTime) + "\t- genetic");
        System.err.println(Player.generationTime + "\t- " + ((float) Player.generationTime / Player.geneticTime) + "\t- generation");
        System.err.println(Player.simulationTime + "\t- " + ((float) Player.simulationTime / Player.geneticTime) + "\t- simulation");
        System.err.println(Player.moveTime + "\t- " + ((float) Player.moveTime / Player.geneticTime) + "\t- move");
        System.err.println(Player.setStateTime + "\t- " + ((float) Player.setStateTime / Player.geneticTime) + "\t- setState");
        System.err.println(Player.setDirsTime + "\t- " + ((float) Player.setDirsTime / Player.geneticTime) + "\t- setDirections");

        state.set(baseState);
        for (int i = 0; i < Player.moveSequenceSize; ++i) {
            short move = Player.bestMoveSequence[i];
            // set directions
            int snakeInd = 0;
            for (int s = 0, allSnakeCount = 8; s < allSnakeCount; ++s) {
                Snake snake = state.snakeMap[s];
                if (snake.mine) {
                    snake.dir = Direction.ALL[(move >> (2 * snakeInd++)) & 3];
                } else if (snake.head != null) {
                    SnakePart head = snake.head;
                    for (Direction d : Direction.ALL) {
                        int newX = head.x + d.x;
                        if (newX < 0 || newX >= 34) continue;
                        char ch = state.get(newX, head.y + d.y);
                        if (ch == Player.powerSourceChar) {
                            snake.dir = d;
                            break;
                        }
                        if (ch == Player.emptyChar) {
                            snake.dir = d;
                        }
                    }
                }
            }

            state.move();

            System.err.println("Turn #" + state.turn);
            System.err.println(state.printGrid());

            int mySnakeCount = 0, oppSnakeCount = 0;
            for (Snake snake : state.snakeMap) {
                if (snake.head != null) {
                    if (snake.mine) ++mySnakeCount;
                    else ++oppSnakeCount;
                }
            }
            if (mySnakeCount == 0 || oppSnakeCount == 0
                    || state.turn == Player.maxTurn || state.powerSources.isEmpty())
                break;
        }

        System.err.println();
        for (int i = 0; i < 4; ++i) {
            Snake snake = state.snakeMap[i];
            System.err.println("snake #" + i + ": " + snake.parts.size());
        }
    }

    static BufferedReader open(String file) throws IOException {
        return Files.newBufferedReader(Paths.get(file));
    }
}
