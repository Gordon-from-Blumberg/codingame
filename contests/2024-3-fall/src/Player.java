import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final int LANDING_PAD_TYPE = 0;
    static final int TELEPORTER_CAPACITY = 0;
    static final int POD_COST = 1000;
    static final int POD_DESTROY_COST = -750;
    static final int POD_RECREATE_COST = POD_COST + POD_DESTROY_COST;
    static final int TELEPORTER_COST = 5000;

    static final int TELEPORTER_NONE = 0;
    static final int TELEPORTER_ENTRANCE = 1;
    static final int TELEPORTER_EXIT = 2;

    static final int TUBE_LIMIT = 5;

    static final Map<Integer, Building> buildings = new HashMap<>();
    static final List<Building> newBuildings = new ArrayList<>();
    static final List<Building> allBuildings = new ArrayList<>();
    static final List<Building> landingPads = new ArrayList<>();
    static final Map<Integer, Pod> pods = new HashMap<>();
    static final List<Route> routes = new ArrayList<>();
    static final List<Route> pendingRoutes = new ArrayList<>();
    static final List<Ant> ants = new ArrayList<>();

    static final Commands commands = new Commands();

    static final Random rand = new Random();

    public static void main(String args[]) {
        final Scanner in = new Scanner(System.in);

        // game loop
        while (true) {
            int resources = in.nextInt();
            System.err.println("res = " + resources);

            for (Building b : buildings.values()) {
                b.next.clear();
                b.paths.clear();
            }

            routes.clear();
            int numTravelRoutes = in.nextInt();
            for (int i = 0; i < numTravelRoutes; i++) {
                Building b1 = buildings.get(in.nextInt());
                Building b2 = buildings.get(in.nextInt());
                int capacity = in.nextInt();

                if (b1.next.containsKey(b2.id)) {
                    b1.next.get(b2.id).capacity = capacity;
                } else {
                    Route route = new Route(b1, b2, capacity);
                    b1.next.put(b2.id, route);
                    if (capacity > TELEPORTER_CAPACITY) {
                        b2.next.put(b1.id, route);
                    }
                    routes.add(route);
                }
            }

            pods.clear();
            int numPods = in.nextInt();
            if (in.hasNextLine()) {
                in.nextLine();
            }
            for (int i = 0; i < numPods; i++) {
                Pod p = new Pod(in.nextLine());
                pods.put(p.id, p);
            }

            newBuildings.clear();
            int numNewBuildings = in.nextInt();
            if (in.hasNextLine()) {
                in.nextLine();
            }

            long start = System.nanoTime();
            for (int i = 0; i < numNewBuildings; i++) {
                Building newB = new Building(in.nextLine());
                buildings.put(newB.id, newB);
                newBuildings.add(newB);
                allBuildings.add(newB);
                if (newB.type == 0) landingPads.add(newB);
                // debug("new building " + newB);
                // debug("distances = " + newB.distances);
            }

            int n = allBuildings.size();
            for (int i = 0; i < n; ++i) {
                Building first = allBuildings.get(i);
                if (first.tubeCount() == TUBE_LIMIT) continue;

                secondLoop:
                for (int j = i + 1; j < n; ++j) {
                    Building second = allBuildings.get(j);
                    if (second == first || second.tubeCount() == TUBE_LIMIT || first.next.containsKey(second.id)) continue;

                    for (int routeIdx = 0, routeCount = routes.size(); routeIdx < routeCount; ++routeIdx) {
                        Route route = routes.get(routeIdx);
                        if (route.b1 == second || route.b2 == second || route.capacity == TELEPORTER_CAPACITY) continue;

                        if (intersect(first, second, route.b1, route.b2)) continue secondLoop;
                    }

                    for (Building third : buildings.values()) {
                        if (third == first || third == second) continue;

                        if (pointOnLine(third, first, second)) continue secondLoop;
                    }

                    int dist = tubeCost(first, second);
                    Path path = new Path(first, second, dist);
                    first.paths.add(path);
                    second.paths.add(path);
                    // debug("dist added " + newB.id + " -> " + second.id);
                }
            }

            for (Building b : allBuildings) {
                Collections.sort(b.paths);
            }

            debug("time = " + (System.nanoTime() - start) / 1e6 + "ms");
            debug("new buildings " + newBuildings.size() + ", total = " + buildings.size());

            // for (Building b : newBuildings) {
            //     debug("b#" + b.id + " distances has " + b.distances.size());
            // }

            pendingRoutes.clear();
            int needForPod = pods.size() == 0 ? POD_COST : POD_RECREATE_COST;
            for (Building newB : newBuildings) {
                if (newB.type == 0) {
                    for (Map.Entry<Integer, Integer> e : newB.astronauts.entrySet()) {
                        int type = e.getKey();
                        boolean tubePlanned = false;
                        for (Path d : newB.paths) {
                            if (commands.cost + d.value + needForPod > resources) break;

                            if (d.building.type == type) {
                                commands.tube(newB, d.building);
                                pendingRoutes.add(new Route(newB, d.building, 1));
                                tubePlanned = true;
                                break;
                            }
                        }

                        // todo if !tubePlanned
                    }
                }
            }


            // debug("cost tube 0 to 1 = " + tubeCost(buildings.get(0), buildings.get(1)));
            // debug("cost tube 0 to 2 = " + tubeCost(buildings.get(0), buildings.get(2)));
            // debug("should be left res = " + (resources - tubeCost(buildings.get(0), buildings.get(1)) - tubeCost(buildings.get(0), buildings.get(2))));
            // System.out.println("TUBE 0 1;TUBE 0 2;POD 42 0 1 0 2 0 1 0 2 0;"); // TUBE | UPGRADE | TELEPORT | POD | DESTROY | WAIT
            commands.write();
        }
    }

    static float dist(Building b1, Building b2) {
        int dx = b1.x - b2.x;
        int dy = b1.y - b2.y;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

    static int tubeCost(Building b1, Building b2) {
        return (int) (dist(b1, b2) * 10);
    }

    static int orientation(Building b1, Building b2, Building b3) {
        int prod = (b3.y-b1.y) * (b2.x-b1.x) - (b2.y-b1.y) * (b3.x-b1.x);
        return (int) Math.signum(prod);
    }

    static boolean intersect(Building a, Building b, Building c, Building d) {
        return orientation(a, b, c) * orientation(a, b, d) < 0 && orientation(c, d, a) * orientation(c, d, b) < 0;
    }

    static boolean pointOnLine(Building b, Building lineStart, Building lineEnd) {
        float e = 0.00001f;
        float diff = dist(lineStart, b) + dist(lineEnd, b) - dist(lineStart, lineEnd);
        return -e < diff && diff < e;
    }

    static class Building {
        final int type;
        final int id;
        final int x;
        final int y;
        final Map<Integer, Integer> astronauts;
        final Map<Integer, Route> next = new HashMap<>();
        final List<Path> paths = new ArrayList<>();
        final List<Ant> ants;
        int teleporter = TELEPORTER_NONE;

        Building(String line) {
            String[] parts = line.split(" ");
            final int n = parts.length;
            int[] ints = new int[n];
            for (int i = 0; i < n; ++i) ints[i] = Integer.parseInt(parts[i]);

            this.type = ints[0];
            this.id = ints[1];
            this.x = ints[2];
            this.y = ints[3];

            if (this.type == LANDING_PAD_TYPE) {
                this.astronauts = new HashMap<>();
                this.ants = new ArrayList<>(n - 5);
                for (int i = 5; i < n; ++i) {
                    if (this.astronauts.containsKey(ints[i])) {
                        this.astronauts.put(ints[i], this.astronauts.get(ints[i]) + 1);

                    } else {
                        this.astronauts.put(ints[i], 1);
                    }
                    Ant ant = new Ant(this.type, this);
                    this.ants.add(ant);
                    Player.ants.add(ant);
                }

            } else {
                this.astronauts = null;
                this.ants = null;
            }
        }

        int tubeCount() {
            int c = 0;
            for (Route r : next.values()) {
                if (r.capacity > TELEPORTER_CAPACITY) ++c;
            }
            return c;
        }

        @Override
        public String toString() {
            String name = (type > LANDING_PAD_TYPE ? "Module #" : "Landing pad #") + id;
            return name + " (" + x + "; " + y + ")(" + astronauts + ")";
        }
    }

    static class Pod {
        final int id;
        final int[] path;

        Pod(String line) {
            String[] parts = line.split(" ");
            final int n = parts.length;
            int[] ints = new int[n];
            for (int i = 0; i < n; ++i) ints[i] = Integer.parseInt(parts[i]);

            this.id = ints[0];
            this.path = new int[ints[1]];
            System.arraycopy(ints, 2, this.path, 0, ints[1]);
        }
    }

    static class Route {
        final Building b1;
        final Building b2;
        int capacity;

        Route(Building b1, Building b2, int capacity) {
            this.b1 = b1;
            this.b2 = b2;
            this.capacity = capacity;
        }
    }

    static class Path implements Comparable<Path> {
        final Building b1;
        final Building b2;
        final int value;
        final float[] weights = new float[20];

        Path(Building b1, Building b2, int value) {
            this.b1 = b1;
            this.b2 = b2;
            this.value = value;
        }

        @Override
        public int compareTo(Path other) {
            return value - other.value;
        }



        void resetWeights() {
            Arrays.fill(weights, 0f);
        }
    }

    static class Ant {
        static final List<Float> weights = new ArrayList<>();

        final int type;
        final List<Building> path = new ArrayList<>();

        Ant(int type, Building start) {
            this.type = type;
            this.path.add(start);
        }

        void resetTo(Building start) {
            path.clear();
            path.add(start);
        }

        void next() {
            if (path.get(path.size() - 1).type == type) {
                increaseWeight();
                resetTo(path.get(0));
            }
        }

        void increaseWeight() {
            for (int i = 0, n = path.size() - 1; i < n; ++i) {
                final Building prev = path.get(i);
                final Building next = path.get(i+1);
                for (int j = 0, n2 = prev.paths.size(); j < n2; ++j) {

                }
            }
        }
    }

    static class Commands {
        final StringBuilder sb = new StringBuilder();
        int cost = 0;

        void tube(Building b1, Building b2) {
            sb.append("TUBE ").append(b1.id).append(' ').append(b2.id).append(';');
            cost += tubeCost(b1, b2);
        }

        void write() {
            if (sb.length() == 0) sb.append("WAIT");
            System.out.println(sb);
            sb.setLength(0);
            cost = 0;
        }
    }

    static int randByWeights(List<Float> weights) {
        final int n = weights.size();
        float sum = 0;
        for (int i = 0; i < n; ++i) sum += weights.get(i);

        final float r = rand.nextFloat(sum);
        float s = 0;
        for (int i = 0; i < n; ++i) {
            s += weights.get(i);
            if (r < s) return i;
        }
        return n - 1;
    }

    static void debug(String str) {
        System.err.println(str);
    }
}
