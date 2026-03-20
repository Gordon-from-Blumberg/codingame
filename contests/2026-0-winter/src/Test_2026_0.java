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


        Player.genetic(baseState, state, System.nanoTime() + Player.firstTurnLimit);
        System.err.println(baseState.printGrid());

        state.set(baseState);
        for (int i = 0; i < Player.moveSequenceSize; ++i) {
            short move = Player.bestMoveSequence[i];
            // set directions
            int snakeInd = 0;
            for (int s = 0, allSnakeCount = 8; s < allSnakeCount; ++s) {
                Snake snake = state.snakeMap[s];
                if (snake != null) {
                    if (snake.mine) {
                        snake.dir = Direction.ALL[(move >> (2 * snakeInd++)) & 3];
                    } else if (snake.head != null) {
                        SnakePart head = snake.head;
                        for (Direction d : Direction.ALL) {
                            char ch = state.get(head.x + d.x, head.y + d.y);
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
            }

            state.move();

            System.err.println(state.printGrid());
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
