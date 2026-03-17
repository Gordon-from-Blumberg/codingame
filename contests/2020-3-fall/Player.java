import java.util.*;
import java.util.function.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        List<Action> orders = new ArrayList<>();
        Witch[] witches = new Witch[] {new Witch(), new Witch()};
        List<Action> tome = new ArrayList<>();

        Turn prevTurn = null;

        // game loop
        while (true) {
            int actionCount = in.nextInt(); // the number of spells and recipes in play
            orders.clear();
            witches[0].spells.clear();
            tome.clear();
            for (int i = 0; i < actionCount; i++) {
                Action action = new Action(in.nextInt(), in.next(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt()
                        , in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());

                switch (action.actionType) {
                    case BREW:
                        orders.add(action);
                        break;
                    case CAST:
                        witches[0].spells.add(action);
                        break;
                    case OPPONENT_CAST:
                        witches[1].spells.add(action);
                        break;
                    case LEARN:
                        tome.add(action);
                        break;
                }

//                debug("%s#%d {price=%d, tome=%d, tax=%d, repeatable=%s}", action.actionType, action.id,
//                        action.price, action.tomeIndex, action.taxCount, action.repeatable);
            }

            for (int i = 0; i < 2; i++) {
                witches[i].read(in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
            }

            orders.sort(Comparator.comparingInt(o -> ((Action) o).price).reversed());

            MoveSequence bestSequence = null;
            int bestPriority = -100;
            for (Action order : orders) {
                MoveSequence moveSequence = witches[0].findMoveSequence(order, tome, prevTurn);
                int priority = order.price - moveSequence.turnList.size() * 2;
                Player.debug("order %d with price %d requires %d turns with priority %d",
                        order.id, order.price, moveSequence.turnList.size(), priority);

                if (bestSequence == null || priority > bestPriority) {
                    bestSequence = moveSequence;
                    bestPriority = priority;
                }
            }

            debug("Selected order = %d, turns = %s", bestSequence.order.id, bestSequence.turnList);
            System.out.println(String.format("%s target = %s", bestSequence.turnList.get(0), bestSequence.order.id));
            prevTurn = bestSequence.turnList.get(0);
        }
    }

    static void debug(String template, Object... args) {
        System.err.println(String.format(template, args));
    }
}

enum ActionType {BREW, CAST, OPPONENT_CAST, LEARN, REST}

class Action {
    int id;
    ActionType actionType;
    Tiers tiers;
    int price;
    int tomeIndex;
    int taxCount;
    boolean castable;
    boolean repeatable;

    Action(int id, String actionType, int tier0, int tier1, int tier2, int tier3,
           int price, int tome, int tax, int castable, int repeatable) {
        this.id = id;
        this.actionType = ActionType.valueOf(actionType);
        tiers = new Tiers(tier0, tier1, tier2, tier3);
        this.price = price;
        this.tomeIndex = tome;
        this.taxCount = tax;
        this.castable = castable == 1;
        this.repeatable = repeatable == 1;
    }
}

class Witch {
    static final int MAX_PRIORITY = 15;

    Tiers inventory = new Tiers();
    List<Action> spells = new ArrayList<>();

    int score;

    void read(int inv0, int inv1, int inv2, int inv3, int score) {
        inventory.read(inv0, inv1, inv2, inv3);
        this.score = score;
    }

    boolean canApply(Action action) {
        switch (action.actionType) {
            case BREW:
                return Tiers.noneSumIsNegative(inventory, action.tiers);
            case CAST:
                return action.castable && Tiers.noneSumIsNegative(inventory, action.tiers);
            case LEARN:
                return inventory.tiers[0] >= action.tomeIndex;
        }
        return false;
    }

    MoveSequence findMoveSequence(Action order, List<Action> tome, Turn prevTurn) {
        if (canApply(order)) {
            Player.debug("order can be brown right now");
            return MoveSequence.brew(order);
        }

        Turn restTurn = new Turn(ActionType.REST);

        PriorityQueue<MoveSequence> queue = new PriorityQueue<>();
        queue.add(new MoveSequence(this, tome, order, prevTurn));

        while (!queue.isEmpty()) {
            MoveSequence moveSequence = queue.poll();
//            if (moveSequence.turnList.size() > 3) {
//                Player.debug("queue size = %d, turns = %d, priority = %d",
//                        queue.size(), moveSequence.turnList.size(), moveSequence.priority);
//            }

            if (moveSequence.turnList.size() >= 15)
                continue;

            if (moveSequence.priority >= MAX_PRIORITY) {
                moveSequence.priority -= 5;
                return moveSequence;
            }

            if (moveSequence.getLastActionType() == ActionType.LEARN) {
                Action spell = moveSequence.lastTurn.action;
                for (Action witchSpell : spells) {
                    if (witchSpell.tiers.equals(spell.tiers)) {
                        spell = witchSpell;
                        break;
                    }
                }
                if (Tiers.isInventoryCorrect(moveSequence.currentTiers, spell.tiers)) {
                    for (int repeat = 1; repeat == 1 || (spell.repeatable && Tiers.isInventoryCorrect(moveSequence.currentTiers, spell.tiers, repeat)); repeat++) {
                        MoveSequence afterCast = moveSequence.addTurn(Turn.cast(spell, repeat));
                        if (afterCast.isFinished()) {
                            return afterCast;
                        }
                        queue.add(afterCast);
                    }
                }
            } else {
                for (Map.Entry<Action, Boolean> entry : moveSequence.spells.entrySet()) {
                    Action spell = entry.getKey();
                    if (entry.getValue() && Tiers.isInventoryCorrect(moveSequence.currentTiers, spell.tiers)) {
                        for (int repeat = 1; repeat == 1 || (spell.repeatable && Tiers.isInventoryCorrect(moveSequence.currentTiers, spell.tiers, repeat)); repeat++) {
                            MoveSequence afterCast = moveSequence.addTurn(Turn.cast(spell, repeat));
                            if (afterCast.isFinished()) {
                                return afterCast;
                            }
                            queue.add(afterCast);
                        }
                    }
                }
            }

            if (moveSequence.getLastActionType() == ActionType.CAST) {
                queue.add(moveSequence.addTurn(restTurn));
            }

            if (moveSequence.getLastActionType() != ActionType.REST) {
                for (int i = 0; i < moveSequence.availableLearns.size(); i++) {
                    Action learn = moveSequence.availableLearns.get(i);
                    Tiers cost = new Tiers(-i, 0, 0, 0);
                    Tiers sumDiff = new Tiers(moveSequence.taxes.get(learn) - i, 0, 0, 0);
                    if (Tiers.isInventoryCorrect(moveSequence.currentTiers, cost)
                            && Tiers.isInventoryCorrect(moveSequence.currentTiers, sumDiff)) {
                        queue.add(moveSequence.addTurn(Turn.learn(learn)));
                    }
                }
            }
        }

        throw new IllegalStateException();
    }
}

class Tiers {
    int[] tiers = new int[4];

    Tiers() {}

    Tiers(int t0, int t1, int t2, int t3) {
        read(t0, t1, t2, t3);
    }

    void read(int t0, int t1, int t2, int t3) {
        tiers[0] = t0;
        tiers[1] = t1;
        tiers[2] = t2;
        tiers[3] = t3;
    }

    int sum() {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += tiers[i];
        }
        return result;
    }

    int sumNegative() {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            if (tiers[i] < 0) result += tiers[i];
        }
        return result;
    }

    void merge(Tiers other, IntBinaryOperator operator) {
        for (int i = 0; i < 4; i++) {
            tiers[i] = operator.applyAsInt(tiers[i], other.tiers[i]);
        }
    }

    boolean isAnyNegative() {
        for (int i = 0; i < 4; i++) {
            if (tiers[i] < 0) {
                return true;
            }
        }
        return false;
    }

    Tiers copy() {
        Tiers copy = new Tiers();
        System.arraycopy(tiers, 0, copy.tiers, 0, 4);
        return copy;
    }

    static boolean any(Tiers tiers1, Tiers tiers2, BiIntPredicate predicate) {
        for (int i = 0; i < 4; i++) {
            if (predicate.test(tiers1.tiers[i], tiers2.tiers[i])) {
                return true;
            }
        }

        return false;
    }

    static boolean noneSumIsNegative(Tiers tiers1, Tiers tiers2) {
        return noneSumIsNegative(tiers1, tiers2, 1);
    }

    static boolean noneSumIsNegative(Tiers tiers1, Tiers tiers2, int coef) {
        for (int i = 0; i < 4; i++) {
            if (tiers1.tiers[i] + tiers2.tiers[i] * coef < 0) {
                return false;
            }
        }

        return true;
    }

    static boolean isInventoryCorrect(Tiers tiers1, Tiers tiers2) {
        return isInventoryCorrect(tiers1, tiers2, 1);
    }

    static boolean isInventoryCorrect(Tiers tiers1, Tiers tiers2, int coef) {
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            int a = tiers1.tiers[i] + tiers2.tiers[i] * coef;
            if (a < 0) {
                return false;
            }
            sum += a;
        }

        return sum <= 10;
    }

    static Tiers merge(Tiers tiers1, Tiers tiers2, IntBinaryOperator operator) {
        Tiers result = new Tiers();
        for (int i = 0; i < 4; i++) {
            result.tiers[i] = operator.applyAsInt(tiers1.tiers[i], tiers2.tiers[i]);
        }
        return result;
    }

    static int reduce(Tiers tiers1, Tiers tiers2, IntBinaryOperator operator, IntBinaryOperator accumulator) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = accumulator.applyAsInt(result, operator.applyAsInt(tiers1.tiers[i], tiers2.tiers[i]));
        }
        return result;
    }

    static int sumNegativeOnly(Tiers tiers1, Tiers tiers2) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int sum = tiers1.tiers[i] + tiers2.tiers[i];
            if (sum < 0)
                result += sum;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Tiers)) return false;
        Tiers other = (Tiers) obj;
        for (int i = 0; i < 4; i++) {
            if (tiers[i] != other.tiers[i]) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Arrays.toString(tiers);
    }
}

class MoveSequence implements Comparable<MoveSequence> {
    final Action order;
    final List<Turn> turnList;
    final Tiers currentTiers;
    final Map<Action, Boolean> spells = new HashMap<>();
    final Map<Action, Integer> taxes = new HashMap<>();
    final List<Action> availableLearns = new ArrayList<>();
    Turn lastTurn;

    int priority;
    int shortage;

    MoveSequence(Witch witch, List<Action> tome, Action order, Turn prevTurn) {
        this.order = order;
        this.turnList = new ArrayList<>();
        this.currentTiers = witch.inventory.copy();
        for (Action spell : witch.spells) {
            this.spells.put(spell, spell.castable);
        }
        for (Action learn : tome) {
            this.taxes.put(learn, learn.taxCount);
        }
        this.availableLearns.addAll(tome);
        this.lastTurn = prevTurn;
    }

    MoveSequence(Action order) {
        this.order = order;
        this.turnList = new ArrayList<>();
        this.currentTiers = new Tiers();
    }

    private MoveSequence(MoveSequence source) {
        order = source.order;
        turnList = new ArrayList<>(source.turnList);
        currentTiers = source.currentTiers.copy();
        spells.putAll(source.spells);
        taxes.putAll(source.taxes);
        availableLearns.addAll(source.availableLearns);
    }

    MoveSequence addTurn(Turn turn) {
        MoveSequence moveSequence = new MoveSequence(this);
        moveSequence.lastTurn = turn;
        moveSequence.turnList.add(turn);

        switch (turn.actionType) {
            case REST:
                moveSequence.spells.entrySet().forEach(e -> e.setValue(true));
                break;
            case CAST:
                moveSequence.spells.put(turn.action, false);
                moveSequence.currentTiers.merge(turn.action.tiers, (ct, st) -> ct + st * turn.repeats);
                break;
            case LEARN:
                moveSequence.spells.put(turn.action, true);
                for (int i = 0; i < moveSequence.availableLearns.size(); i++) {
                    Action learn = moveSequence.availableLearns.get(i);
                    if (learn == turn.action) {
                        moveSequence.currentTiers.tiers[0] -= i;
                        break;
                    }
                    if (i > 0) {
                        moveSequence.taxes.merge(learn, 1, Integer::sum);
                    }
                }
                moveSequence.availableLearns.remove(turn.action);
                moveSequence.currentTiers.tiers[0] += moveSequence.taxes.get(turn.action);
                moveSequence.taxes.remove(turn.action);
                break;
        }

        moveSequence.calculatePriority();
        return moveSequence;
    }

    boolean isFinished() {
        return Tiers.noneSumIsNegative(currentTiers, order.tiers);
    }

    void calculatePriority() {
        shortage = Tiers.sumNegativeOnly(currentTiers, order.tiers);
        priority = turnList.size() - 2 * shortage;
    }

    ActionType getLastActionType() {
        return lastTurn != null ? lastTurn.actionType : null;
    }

    static MoveSequence brew(Action order) {
        MoveSequence moveSequence = new MoveSequence(order);
        moveSequence.turnList.add(new Turn(order));
        return moveSequence;
    }

    @Override
    public int compareTo(MoveSequence o) {
//        int result = o.shortage - shortage;
//        if (result != 0)
//            return result;
        return priority - o.priority;
    }
}

class Turn {
    Action action;
    ActionType actionType;
    Tiers tiers;
    int repeats = 1;

    Turn(Action order) {
        this.action = order;
        this.actionType = ActionType.BREW;
        this.tiers = order.tiers;
    }

    Turn(ActionType actionType) {
        this.actionType = actionType;
        this.tiers = new Tiers();
    }

    private Turn(Action spell, ActionType actionType) {
        this.action = spell;
        this.actionType = actionType;
        this.tiers = actionType == ActionType.LEARN ? new Tiers() : spell.tiers;
    }

    private Turn(Action spell, ActionType actionType, int repeats) {
        this.action = spell;
        this.actionType = actionType;
        this.tiers = spell.tiers;
        this.repeats = repeats;
    }

    static Turn learn(Action learn) {
        return new Turn(learn, ActionType.LEARN);
    }

    static Turn cast(Action spell, int repeats) {
        return new Turn(spell, ActionType.CAST, repeats);
    }

    @Override
    public String toString() {
        switch (actionType) {
            case REST:
                return "REST";
            case BREW:
            case LEARN:
                return actionType + " " + action.id;
            case CAST:
                String result = "CAST " + action.id;
                if (action.repeatable)
                    result += " " + repeats;
                return result;
        }
        throw new IllegalStateException();
    }
}

@FunctionalInterface
interface BiIntPredicate {
    boolean test(int i1, int i2);
}
