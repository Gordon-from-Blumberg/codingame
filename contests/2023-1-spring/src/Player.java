import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final int EMPTY_TYPE = 0;
    static final int EGG_TYPE = 1;
    static final int CRYSTAL_TYPE = 2;

    public static void main(String[] args) {
        final Scanner in = new Scanner(System.in);
        final PrintStream out = System.out;
        final int numberOfCells = in.nextInt(); // amount of hexagonal cells in this map
        final Grid grid = new Grid(numberOfCells);

        for (int i = 0; i < numberOfCells; i++) {
            int type = in.nextInt(); // 0 for empty, 1 for eggs, 2 for crystal
            int initialResources = in.nextInt(); // the initial amount of eggs/crystals on this cell
            int neigh0 = in.nextInt(); // the index of the neighbouring cell for each direction
            int neigh1 = in.nextInt();
            int neigh2 = in.nextInt();
            int neigh3 = in.nextInt();
            int neigh4 = in.nextInt();
            int neigh5 = in.nextInt();
            grid.cells[i] = new Cell(type, initialResources, neigh0, neigh1, neigh2, neigh3, neigh4, neigh5);
        }

        final int numberOfBases = in.nextInt();
        final int[] myBases = new int[numberOfBases];
        for (int i = 0; i < numberOfBases; i++) {
            myBases[i] = in.nextInt();
        }
        final int[] oppBases = new int[numberOfBases];
        for (int i = 0; i < numberOfBases; i++) {
            oppBases[i] = in.nextInt();
        }

        findDistancesToMyBases(grid, myBases);

        final Pool pool = new Pool();
        final Deque<Command> queue = new ArrayDeque<>();
        final List<Command> commandList = new ArrayList<>();
        while (true) {
            int myAnts = 0, oppAnts = 0;
            for (int i = 0; i < numberOfCells; i++) {
                final Cell cell = grid.cells[i];
                cell.resources = in.nextInt(); // the current amount of eggs/crystals on this cell
                cell.myAnts = in.nextInt(); // the amount of your ants on this cell
                cell.oppAnts = in.nextInt(); // the amount of opponent ants on this cell

                myAnts += cell.myAnts;
                oppAnts += cell.oppAnts;
            }

            for (int i = 0; i < numberOfCells; i++) {
                final Cell cell = grid.cells[i];

                if (cell.resources > 0) {
                    int distToBase = cell.distToMyBases[cell.closestBase];
                    int priority = cell.resources / (distToBase * distToBase) + 1;
                    Command command = pool.get();
                    command.type = Command.LINE;
                    command.args[0] = myBases[cell.closestBase];
                    command.args[1] = i;
                    command.args[2] = priority;
                    commandList.add(command);
                }
            }

            Collections.sort(commandList);
//            System.err.print("Priority after sort: ");
//            for (Command c : commandList) {
//                System.err.print(c.args[2]);
//                System.err.print(", ");
//            }
//            System.err.println();

            for (int i = 0, n = Math.min(myAnts / 5, commandList.size()); i < n; ++i) {
                queue.add(commandList.get(i));
            }

            if (queue.isEmpty()) {
                out.println("WAIT");
                commandList.clear();
                continue;
            }

            System.err.println("Path count = " + queue.size());

            while (!queue.isEmpty()) {
                Command command = queue.removeLast();
                command.print();
                pool.free(command);
            }
            out.println();
            commandList.clear();
        }
    }

    static void findDistancesToMyBases(Grid grid, int[] myBases) {
        final Cell[] cells = grid.cells;
        final Queue<Integer> cellQueue = new ArrayDeque<>();
        final Set<Integer> visited = new HashSet<>();

        final int baseCount = myBases.length;
        for (int baseIndex = 0; baseIndex < baseCount; ++baseIndex) {
            int dist = 0;
            cellQueue.add(myBases[baseIndex]);
            if (cells[myBases[baseIndex]].distToMyBases == null)
                cells[myBases[baseIndex]].distToMyBases = new int[baseCount];
            cells[myBases[baseIndex]].distToMyBases[baseIndex] = dist;

            while (!cellQueue.isEmpty()) {
                int curInd = cellQueue.remove();
                visited.add(curInd);
                Cell cur = cells[curInd];
                dist = cur.distToMyBases[baseIndex];
                for (int neighInd : cur.neighs) {
                    if (neighInd > -1 && !visited.contains(neighInd)) {
                        cellQueue.add(neighInd);
                        if (cells[neighInd].distToMyBases == null)
                            cells[neighInd].distToMyBases = new int[baseCount];
                        cells[neighInd].distToMyBases[baseIndex] = dist + 1;
                    }
                }
            }
        }

        final int cellCount = cells.length;
        for (Cell cell : cells) {
            int minDist = cellCount;
            int closestBase = -1;
            for (int i = 0; i < baseCount; ++i) {
                if (cell.distToMyBases[i] < minDist) {
                    minDist = cell.distToMyBases[i];
                    closestBase = i;
                }
            }
            cell.closestBase = closestBase;
        }
    }
}

class Grid {
    final Cell[] cells;
    Grid(int cellNumber) {
        cells = new Cell[cellNumber];
    }
}

class Cell {
    final int type;
    int resources;
    final int[] neighs = new int[6];
    int myAnts, oppAnts;
    int[] distToMyBases;
    int closestBase = -1;

    Cell(int type, int resources, int neigh0, int neigh1, int neigh2, int neigh3, int neigh4, int neigh5) {
        this.type = type;
        this.resources = resources;
        this.neighs[0] = neigh0;
        this.neighs[1] = neigh1;
        this.neighs[2] = neigh2;
        this.neighs[3] = neigh3;
        this.neighs[4] = neigh4;
        this.neighs[5] = neigh5;
    }
}

class Command implements Comparable<Command> {
    static final String WAIT = "WAIT";
    static final String LINE = "LINE";
    static final String BEACON = "BEACON";
    static final String MESSAGE = "MESSAGE";

    String type;
    final int[] args = new int[3];
    String message;

    void print() {
        final PrintStream out = System.out;
        out.print(type);
        switch (type) {
            case LINE:
                for (int i = 0; i < 3; ++i) {
                    out.print(' ');
                    out.print(args[i]);
                }
                break;
            case BEACON:
                for (int i = 0; i < 2; ++i) {
                    out.print(' ');
                    out.print(args[i]);
                }
                break;
            case MESSAGE:
                out.print(' ');
                out.print(message);
                break;
        }
        out.print(';');
    }

    @Override
    public int compareTo(Command o) {
        return o.args[2] - this.args[2];
    }
}

class Pool {
    final List<Command> commands = new ArrayList<>();
    Pool() {
        for (int i = 0; i < 10; ++i) {
            commands.add(new Command());
        }
    }

    Command get() {
        return commands.isEmpty() ? new Command() : commands.remove(commands.size() - 1);
    }

    void free(Command command) {
        commands.add(command);
    }
}