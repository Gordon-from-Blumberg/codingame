import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Solution {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int N = in.nextInt();
        if (in.hasNextLine()) {
            in.nextLine();
        }

        RationalNumber rationalNumber = new RationalNumber();
        RNTNode node = new RNTNode();

        for (int i = 0; i < N; i++) {
            String line = in.nextLine();
            System.out.println(line.contains("/") ? findPath(line, node, rationalNumber) : getValueByPath(line, node));
        }
    }

    static String findPath(String input, RNTNode node, RationalNumber rn) {
        node.reset();
        rn.parse(input);
        StringBuilder path = new StringBuilder();

        final double value = rn.getValue();

        while (!rn.equals(node.value)) {
            if (value < node.value.getValue()) {
                node.left();
                path.append("L");
            } else {
                node.right();
                path.append("R");
            }
        }

        return path.toString();
    }

    static String getValueByPath(String path, RNTNode node) {
        node.reset();
        String[] pathParts = path.split("");
        for (String pathPart : pathParts) {
            if (pathPart.equals("L")) {
                node.left();
            } else {
                node.right();
            }
        }

        return node.toString();
    }

    static class RNTNode {
        RationalNumber leftParent = new RationalNumber(0, 1);
        RationalNumber rightParent = new RationalNumber(1, 0);
        RationalNumber value = new RationalNumber(1, 1);

        void left() {
            rightParent.set(value);
            value.add(leftParent);
        }

        void right() {
            leftParent.set(value);
            value.add(rightParent);
        }

        double getLeft() {
            return (double) (value.numerator + leftParent.numerator) / (value.denominator + leftParent.denominator);
        }

        double getRight() {
            return (double) (value.numerator + rightParent.numerator) / (value.denominator + rightParent.denominator);
        }

        void reset() {
            leftParent.set(0, 1);
            rightParent.set(1, 0);
            value.set(1, 1);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    static class RationalNumber {
        long numerator;
        long denominator;

        RationalNumber() {}

        RationalNumber(long numerator, long denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        void set(long numerator, long denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        void set(RationalNumber rn) {
            this.numerator = rn.numerator;
            this.denominator = rn.denominator;
        }

        void add(RationalNumber rn) {
            numerator += rn.numerator;
            denominator += rn.denominator;
        }

        double getValue() {
            return (double) numerator / denominator;
        }

        void parse(String str) {
            String[] parts = str.split("/");
            numerator = Long.parseLong(parts[0]);
            denominator = Long.parseLong(parts[1]);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || obj.getClass() != getClass()) return false;
            RationalNumber rn = (RationalNumber) obj;
            return numerator == rn.numerator && denominator == rn.denominator;
        }

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }
    }
}