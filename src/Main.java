import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.position.Position;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Main {
    static Position position;
    static int minimaxNodeCount;
    static int quiescenceNodeCount;
    static int fromMemory;
    static int global_depth;
    static int MAX_TIME = 5000;
    static HashMap<Position, Object[]> positionTable = new HashMap<Position, Object[]>();
    static HashMap<Position, Node> PVTable = new HashMap<Position, Node>();
    static ArrayList<Node> nextMoves = new ArrayList<Node>();
    static long time;
    static Scanner stdin = new Scanner(System.in);
    static ArrayList<String> legalMoveSAN = new ArrayList<>();
    static boolean unicode = true;
    public static void main(String[] args) throws IllegalMoveException {
        position = Position.createInitialPosition();
        while(!position.isMate()) {
            printBoard(position.getFEN());
            doUserMove();
            printBoard(position.getFEN());
            doEngineMove();
        }
    }

    public static void doUserMove() throws IllegalMoveException {
        System.out.print(">>> ");
        String userMove = stdin.nextLine();
        short selectedMove = 0;
        for (short m : position.getAllMoves()) {
            position.doMove(m);
            Move move = position.getLastMove();
            position.undoMove();
            legalMoveSAN.add(move.getSAN());
            if (userMove.equals(move.getSAN())) {
                selectedMove = m;
                break;
            }
        }
        if (selectedMove != (short) 0) {
            position.doMove(selectedMove);
        } else {
            switch (userMove) {
                case "/moves":
                    printMoveList();
                    doUserMove();
                    break;
                case "/switch board":
                    unicode = !unicode;
                    printBoard(position.getFEN());
                    doUserMove();
                    break;
                case "/faster":
                    MAX_TIME /= 2;
                    doUserMove();
                    break;
                case "/slower":
                    MAX_TIME *= 2;
                    doUserMove();
                    break;
                default:
                    System.out.println("Not a valid move, please try again. For a list of moves, type /moves");
                    doUserMove();
            }
        }
    }

    public static void doEngineMove() throws IllegalMoveException {
        global_depth = 0;
        Node move = new Node((short) 0,0);
        Node prevmove = move;
        time = System.currentTimeMillis();
        while(move.value != Double.MAX_VALUE && move.value != -Double.MAX_VALUE) {
            move = minimax(global_depth);
            if (move.move == 0) {
                move = prevmove;
                break;
            }
            prevmove = move;
            positionTable.put(position, new Object[]{move.move, move.value, global_depth});
            global_depth++;
        }
        positionTable.clear();
        position.doMove(move.move);
        reportStats();
        resetCounts();
    }

    public static void printMoveList() {
        for (String m : legalMoveSAN) {
            System.out.print(m + ", ");
        }
        System.out.println();
    }

    public static Node minimax(int depth) throws IllegalMoveException {
        if (position.getToPlay() == 0) {
            Collections.sort(nextMoves);
            /*for(Node n: nextMoves) {
                System.out.println(Move.getString(n.move) + " " + n.value);
            }
            System.out.println(" ");*/
            ArrayList<Node> copy = (ArrayList<Node>) nextMoves.clone();
            nextMoves.clear();
            return maximizing(depth, -Double.MAX_VALUE, Double.MAX_VALUE, true, copy);
        } else {
            Collections.sort(nextMoves);
            Collections.reverse(nextMoves);
            /*for(Node n: nextMoves) {
                System.out.println(Move.getString(n.move) + " " + n.value);
            }
            System.out.println(" ");*/
            ArrayList<Node> copy = (ArrayList<Node>) nextMoves.clone();
            nextMoves.clear();
            return minimizing(depth, -Double.MAX_VALUE, Double.MAX_VALUE, true, copy);
        }

    }

    public static Node maximizing(int depth, double alpha, double beta, boolean firstMove, ArrayList<Node> override) throws IllegalMoveException {
        if (global_depth == depth && System.currentTimeMillis() - time > MAX_TIME) {
            return new Node((short) 0,0);
        }
        if (depth <= 0 || position.isMate() || position.isStaleMate()) {
            return simpleFindMax(alpha, beta, firstMove);
        }
        Object[] stored = positionTable.get(position);
        if (stored != null && (int) stored[2] >= depth) {
            fromMemory++;
            return new Node((short) stored[0], (double) stored[1]);
        }
        short[] movelist = position.getAllMoves();
        double currBest = -Double.MAX_VALUE;
        boolean found = false;
        if (stored != null) {
            short testMove = (short) stored[0];
            for (short m : movelist) {
                if (m == testMove) {
                    found = true;
                    break;
                }
            }
            if (found) {
                position.doMove(testMove);
                Node minPos = minimizing(depth - 1, alpha, beta, false, null);
                double minValue = minPos.value;
                //System.out.println(depth + " " + Move.getString(testMove) + " " + minValue);
                if (minPos.move != (short) 0) {
                    Object[] minStored = positionTable.get(position);
                    if (minStored == null || (int) minStored[2] < depth - 1) {
                        positionTable.put(position, new Object[]{minPos.move, minValue, depth-1});
                    }
                }
                position.undoMove();
                currBest = minValue;
                alpha = max(alpha,currBest);
                if (alpha >= beta) {
                    return new Node(testMove, currBest);
                }
            } else {
                //System.out.println("error");
            }
        }
        short bestMove;
        if (override != null && override.size() > 0) {
            bestMove = override.get(0).move;
            for (Node m : override) {
                position.doMove(m.move);
                Node minPos = minimizing(depth - 1, alpha, beta, false, null);
                double minValue = minPos.value;
                //System.out.println(depth + " " + Move.getString(m.move) + " " + minValue);
                if (minPos.move != (short) 0) {
                    Object[] minStored = positionTable.get(position);
                    if (minStored == null || (int) minStored[2] < depth - 1) {
                        positionTable.put(position, new Object[]{minPos.move, minValue, depth-1});
                    }
                }
                if (firstMove) {
                    nextMoves.add(new Node(m.move, minValue));
                }
                position.undoMove();
                if (minValue > currBest) {
                    currBest = minValue;
                    bestMove = m.move;
                }
                alpha = max(alpha, currBest);
                if (alpha >= beta) {
                    break;
                }
            }
            return new Node(bestMove, currBest);
        }
        bestMove = movelist[0];
        for (short m : movelist) {
            position.doMove(m);
            Node minPos = minimizing(depth - 1, alpha, beta, false, null);
            double minValue = minPos.value;
            //System.out.println(depth + " " + Move.getString(m) + " " + minValue);
            if (minPos.move != (short) 0) {
                Object[] minStored = positionTable.get(position);
                if (minStored == null || (int) minStored[2] < depth - 1) {
                    positionTable.put(position, new Object[]{minPos.move, minValue, depth-1});
                }
            }
            position.undoMove();
            if (minValue > currBest) {
                currBest = minValue;
                bestMove = m;
            }
            alpha = max(alpha, currBest);
            if (alpha >= beta) {
                break;
            }
        }
        return new Node(bestMove, currBest);
    }

    public static Node minimizing(int depth, double alpha, double beta, boolean firstMove, ArrayList<Node> override) throws IllegalMoveException {
        if (global_depth == depth && System.currentTimeMillis() - time > MAX_TIME) {
            return new Node((short) 0,0);
        }
        if (depth <= 0 || position.isMate() || position.isStaleMate()) {
            return SimpleFindMin(alpha, beta, firstMove);
        }
        Object[] stored = positionTable.get(position);
        if (stored != null && (int) stored[2] >= depth) {
            fromMemory++;
            return new Node((short) stored[0], (double) stored[1]);
        }
        short[] movelist = position.getAllMoves();
        double currBest = Double.MAX_VALUE;
        boolean found = false;
        if (stored != null) {
            short testMove = (short) stored[0];
            for (short m : movelist) {
                if (m == testMove) {
                    found = true;
                    break;
                }
            }
            if (found) {
                position.doMove(testMove);
                Node maxPos = maximizing(depth - 1, alpha, beta, false, null);
                double maxValue = maxPos.value;
                //System.out.println(depth + " " + Move.getString(testMove) + " " + maxValue);
                if (maxPos.move != (short) 0) {
                    Object[] maxStored = positionTable.get(position);
                    if (maxStored == null || (int) maxStored[2] < depth - 1) {
                        positionTable.put(position, new Object[]{maxPos.move, maxValue, depth-1});
                    }
                }
                position.undoMove();
                currBest = maxValue;
                beta = min(beta, currBest);
                if (alpha >= beta) {
                    return new Node(testMove, currBest);
                }
            } else {
                //System.out.println("error");
            }
        }
        short bestMove;
        if (override != null && override.size() > 0) {
            bestMove = override.get(0).move;
            for (Node m: override) {
                position.doMove(m.move);
                Node maxPos = maximizing(depth - 1, alpha, beta, false, null);
                double maxValue = maxPos.value;
                //System.out.println(depth + " " + Move.getString(m.move) + " " + maxValue);
                if (maxPos.move != (short) 0) {
                    Object[] maxStored = positionTable.get(position);
                    if (maxStored == null || (int) maxStored[2] < depth - 1) {
                        positionTable.put(position, new Object[]{maxPos.move, maxValue, depth-1});
                    }
                }
                if (firstMove) {
                    nextMoves.add(new Node(m.move, maxValue));
                }
                position.undoMove();
                if (maxValue < currBest) {
                    currBest = maxValue;
                    bestMove = m.move;
                }
                beta = min(beta, currBest);
                if (alpha >= beta) {
                    break;
                }
            }
            return new Node(bestMove, currBest);
        }
        bestMove = movelist[0];
        for (short m : movelist) {
            position.doMove(m);
            Node maxPos = maximizing(depth - 1, alpha, beta, false, null);
            double maxValue = maxPos.value;
            //System.out.println(depth + " " + Move.getString(m) + " " + maxValue);
            if (maxPos.move != (short) 0) {
                Object[] maxStored = positionTable.get(position);
                if (maxStored == null || (int) maxStored[2] < depth - 1) {
                    positionTable.put(position, new Object[]{maxPos.move, maxValue, depth-1});
                }
            }
            position.undoMove();
            if (maxValue < currBest) {
                currBest = maxValue;
                bestMove = m;
            }
            beta = min(beta, currBest);
            if (alpha >= beta) {
                break;
            }
        }
        return new Node(bestMove, currBest);
    }

    public static Node simpleFindMax(double alpha, double beta, boolean firstMove) throws IllegalMoveException {
        if (position.isMate()) {
            return new Node((short) 0, heuristic(position));
        }
        if (position.isStaleMate()) {
            return new Node((short) 0, heuristic(position));
        }
        Object[] stored = positionTable.get(position);
        if (stored != null) {
            fromMemory++;
            return new Node((short) stored[0], (double) stored[1]);
        }
        short[] capturingList = position.getAllCapturingMoves();
        short[] noncapturingList = position.getAllNonCapturingMoves();
        short bestMove;
        if (capturingList.length > 0) {
            bestMove = capturingList[0];
        } else {
            bestMove = noncapturingList[0];
        }
        double currBest = -Double.MAX_VALUE;
        double posVal = heuristic(position);
        for (short m : capturingList) {
            minimaxNodeCount++;
            position.doMove(m);
            Node minPos = quiescence(alpha, beta, Double.MAX_VALUE, posVal, false);
            double minValue = minPos.value;
            //System.out.println(0 + " " + Move.getString(m) + " " + minValue);
            position.undoMove();
            if (firstMove) {
                nextMoves.add(new Node(m, minValue));
            }
            if (minValue > currBest) {
                currBest = minValue;
                bestMove = m;
            }
            alpha = max(alpha, currBest);
            if (alpha >= beta) {
                return new Node(bestMove, currBest);
            }
        }
        for (short m : noncapturingList) {
            minimaxNodeCount++;
            position.doMove(m);
            posVal = heuristic(position);
            //System.out.println(0 + " " + Move.getString(m) + " " + posVal);
            if (firstMove) {
                nextMoves.add(new Node(m, posVal));
            }
            if (posVal > currBest) {
                bestMove = m;
                currBest = posVal;
            }
            position.undoMove();
            alpha = max(alpha, currBest);
            if (alpha >= beta) {
                return new Node(bestMove, currBest);
            }
        }
        return new Node(bestMove, currBest);
    }

    public static Node SimpleFindMin(double alpha, double beta, boolean firstMove) throws IllegalMoveException {
        if (position.isMate()) {
            return new Node((short) 0, heuristic(position));
        }
        if (position.isStaleMate()) {
            return new Node((short) 0, heuristic(position));
        }
        Object[] stored = positionTable.get(position);
        if (stored != null) {
            fromMemory++;
            return new Node((short) stored[0], (double) stored[1]);
        }
        short[] capturingList = position.getAllCapturingMoves();
        short[] noncapturingList = position.getAllNonCapturingMoves();
        short bestMove;
        if (capturingList.length > 0) {
            bestMove = capturingList[0];
        } else {
            bestMove = noncapturingList[0];
        }
        double currBest = Double.MAX_VALUE;
        double posVal = heuristic(position);
        for (short m : capturingList) {
            minimaxNodeCount++;
            position.doMove(m);
            Node maxPos = quiescence(alpha, beta, posVal, -Double.MAX_VALUE, true);
            double maxValue = maxPos.value;
            //System.out.println(0 + " " + Move.getString(m) + " " + maxValue);
            position.undoMove();
            if (firstMove) {
                nextMoves.add(new Node(m, maxValue));
            }
            if (maxValue < currBest) {
                currBest = maxValue;
                bestMove = m;
            }
            beta = min(beta, currBest);
            if (alpha >= beta) {
                return new Node(bestMove, currBest);
            }
        }
        for (short m : noncapturingList) {
            minimaxNodeCount++;
            position.doMove(m);
            posVal = heuristic(position);
            //System.out.println(0 + " " + Move.getString(m) + " " + posVal);
            if (firstMove) {
                nextMoves.add(new Node(m, posVal));
            }
            if (posVal < currBest) {
                bestMove = m;
                currBest = posVal;
            }
            position.undoMove();
            beta = min(beta, currBest);
            if (alpha >= beta) {
                return new Node(bestMove, currBest);
            }
        }
        return new Node(bestMove, currBest);
    }

    public static Node quiescence(double alpha, double beta, double maxLimit, double minLimit, boolean maximizing) throws IllegalMoveException {
        Object[] stored = positionTable.get(position);
        if (stored != null) {
            fromMemory++;
            return new Node((short) stored[0], (double) stored[1]);
        }
        short[] captures = position.getAllCapturingMoves();
        double posVal = heuristic(position);
        short bestMove;
        if (captures.length > 0) {
            bestMove = captures[0];
        } else {
            return new Node((short) 0, posVal);
        }
        if (maximizing) {
            double currBest = -Double.MAX_VALUE;
            if (posVal < minLimit) {
                return new Node(bestMove, posVal);
            }
            if (posVal > minLimit) {
                minLimit = posVal;
            }
            for (short m : captures) {
                quiescenceNodeCount++;
                position.doMove(m);
                Node minPos = quiescence(alpha, beta, maxLimit, minLimit, false);
                double minValue = minPos.value;
                position.undoMove();
                if (minValue > currBest) {
                    currBest = minValue;
                    bestMove = m;
                }
                alpha = max(alpha, currBest);
                if (alpha >= beta) {
                    break;
                }
            }
            return new Node(bestMove, currBest);
        } else {
            double currBest = Double.MAX_VALUE;
            if (posVal > maxLimit) {
                return new Node(bestMove, posVal);
            }
            if (posVal < maxLimit) {
                maxLimit = posVal;
            }
            for (short m : captures) {
                quiescenceNodeCount++;
                position.doMove(m);
                Node maxPos = quiescence(alpha, beta, maxLimit, minLimit, true);
                double maxValue = maxPos.value;
                position.undoMove();
                if (maxValue < currBest) {
                    currBest = maxValue;
                    bestMove = m;
                }
                beta = min(beta, currBest);
                if (alpha >= beta) {
                    break;
                }
            }
            return new Node(bestMove, currBest);
        }
    }

    public static double heuristic(Position p) {
        if (p.isMate()) {
            if (p.getToPlay()==0) {
                return -Double.MAX_VALUE;
            } else {
                return Double.MAX_VALUE;
            }
        }
        if (p.isStaleMate()) {
            return 0;
        }
        double value = 0;
        int bbcount = 0;
        int wbcount = 0;
        for (int i = 0; i < 64; i++) {
            int row = 1 + i / 8;
            int col = 1 + i % 8;
            double distFromCenter = (Math.sqrt(Math.pow(Math.abs(row - 4.5), 2) + Math.pow(Math.abs(col - 4.5), 2))) / 4.95;
            int piece = p.getStone(i);
            switch (piece) {
                case 5:
                    value += -100 * (0.7 + 0.1 * (3.5 - (Math.abs(col - 4.5))) + (0.05 * (9 - row)) + passedPawnBonus(p, i%8, i/8, -1, 0.5) + doubledPawnPenalty(p, i%8, i/8, -1, -0.15));
                    break;
                case (-5):
                    value += 100 * (0.7 + 0.1 * (3.5 - (Math.abs(col - 4.5))) + (0.05 * row) + passedPawnBonus(p, i%8, i/8, 1, 0.5) + doubledPawnPenalty(p, i%8, i/8, 1, -0.15));
                    break;
                case 1:
                    value += -300 * (1.17 - distFromCenter / 3);
                    break;
                case (-1):
                    value += 300 * (1.17 - distFromCenter / 3);
                    break;
                case 3:
                    value += -500 * (1 + openLaneBonus(p, i % 8, i / 8, -1, 0.1));
                    break;
                case (-3):
                    value += 500 * (1 + openLaneBonus(p, i % 8, i / 8, 1, 0.1));
                    break;
                case 2:
                    value += -325 * (1.12 - distFromCenter / 4);
                    bbcount++;
                    break;
                case (-2):
                    value += 325 * (1.12 - distFromCenter / 4);
                    wbcount++;
                    break;
                case 4:
                    value += -900;
                    break;
                case (-4):
                    value += 900;
                    break;
                case 6:
                    value += -(50-position.getPlyNumber())  * (kingBlockadeScore(p, i % 8, i / 8 ));
                    if (position.getPlyNumber() < 30) {
                        if (row == 8 && col >= 7) {
                            value += -100;
                        }
                        if (row == 8 && col <= 3) {
                            value += -50;
                        }
                    }
                    break;
                case (-6):
                    value += (50-position.getPlyNumber()) * (kingBlockadeScore(p, i % 8, i / 8));
                    if (position.getPlyNumber() < 30) {
                        if (row == 1 && col >= 7) {
                            value += 100;
                        } if (row == 1 && col <= 3) {
                            value += 50;
                        }
                    }
                    break;
            }
        }
        if (bbcount == 2) {
            value += -50;
        }
        if (wbcount == 2) {
            value += 50;
        }
        return value;
    }

    public static double doubledPawnPenalty(Position p, int col, int row, int dir, double bonus) {
        for (int i = row*8 + col + 8*dir; i < 64 && i > -1; i = i + 8*dir) {
            if (p.getStone(i) == -5*dir) {
                return bonus;
            }
        }
        return 0;
    }
    public static double passedPawnBonus(Position p, int col, int row, int dir, double bonus) {
        for (int i = row*8 + col + 8*dir; i < 64 && i > -1; i = i + 8*dir) {
            if (p.getStone(i) == 5*dir) {
                return 0.0;
            }
            if (col < 7 && p.getStone(i+1) == 5*dir) {
                return 0.0;
            }
            if (col > 0 && p.getStone(i-1) == 5*dir) {
                return 0.0;
            }
        }
        return bonus;
    }

    public static double openDiagonalBonus(Position p, int col, int row) {
        int score = 0;
        if (col < 4 && row < 4) {
            for(int i = 1; row + i < 8; i++) {
                if (col + i < 8 && p.getPiece((row + i)*8 + col + i) == 5) {
                    score++;
                }
            }
        }
        if (col > 3 && row < 4) {
            for(int i = 1; row + i < 8; i++) {
                if (col - i > -1 && p.getPiece((row + i)*8 + col - i) == 5) {
                    score++;
                }
            }
        }
        if (col < 4 && row > 3) {
            for(int i = 1; row - i > -1; i++) {
                if (col + i < 8 && p.getPiece((row - i)*8 + col + i) == 5) {
                    score++;
                }
            }
        }
        if (col > 3 && row > 3) {
            for(int i = 1; row - i > -1; i++) {
                if (col - i > -1 && p.getPiece((row - i)*8 + col - i) == 5) {
                    score++;
                }
            }
        }
        return 2 - score;
    }

    public static double openLaneBonus(Position p, int col, int row, int dir, double bonus) {
        for (int i = row*8 + col + 8*dir; i < 64 && i > -1; i = i + 8*dir) {
            if (p.getPiece(i) == 5) {
                return 0.0;
            }
        }
        return bonus;
    }

    public static double kingBlockadeScore(Position p, int col, int row) {
        double score = 0;
        if (col > 0) {
            if (p.isSquareEmpty(row*8+col-1)) {
                score++;
            }
            if (row < 7) {
                if (p.isSquareEmpty((row+1)*8+col-1)) {
                    score++;
                }
            }
        }
        if (row > 0) {
            if (p.isSquareEmpty((row-1)*8+col)) {
                score++;
            }
            if (col > 0) {
                if (p.isSquareEmpty((row-1)*8+col-1)) {
                    score++;
                }
            }
        }
        if (col < 7) {
            if (p.isSquareEmpty(row*8+col+1)) {
                score++;
            }
            if (row > 0) {
                if (p.isSquareEmpty((row-1)*8+col+1)) {
                    score++;
                }
            }
        }
        if (row < 7) {
            if (p.isSquareEmpty((row+1)*8+col)) {
                score++;
            }
            if (col < 7) {
                if (p.isSquareEmpty((row+1)*8+col+1)) {
                    score++;
                }
            }
        }
        if (score > 2) {
            return 8-score;
        } else {
            return 6;
        }
    }

    /* Utility */
    public static void resetCounts() {
        nextMoves.clear();
        legalMoveSAN.clear();
        minimaxNodeCount = 0;
        quiescenceNodeCount = 0;
        fromMemory = 0;
    }
    public static void reportStats() {
        System.out.println(position.toString());
        System.out.println(heuristic(position));
        System.out.println("depth: " + global_depth);
        System.out.println("minimax nodes: " + minimaxNodeCount);
        System.out.println("quiescence nodes: " + quiescenceNodeCount);
        System.out.println("from memory: " + fromMemory);
    }

    public static void printBoard(String FEN) {
        String[] rows = FEN.split(" ", -1)[0].split("/",-1);
        for(int i = 0; i < rows.length; i++) {
            char[] pieces = rows[i].toCharArray();
            System.out.println("-------------------------------------------");
            System.out.print("|");
            for (char p : pieces) {
                if (unicode) {
                    if (p == 'k') {
                        System.out.print(" \u2654 |");
                    } else if (p == 'q') {
                        System.out.print(" \u2655 |");
                    } else if (p == 'r') {
                        System.out.print(" \u2656 |");
                    } else if (p == 'b') {
                        System.out.print(" \u2657 |");
                    } else if (p == 'n') {
                        System.out.print(" \u2658 |");
                    } else if (p == 'p') {
                        System.out.print(" \u2659 |");
                    } else if (p == 'K') {
                        System.out.print(" \u265A |");
                    } else if (p == 'Q') {
                        System.out.print(" \u265B |");
                    } else if (p == 'R') {
                        System.out.print(" \u265C |");
                    } else if (p == 'B') {
                        System.out.print(" \u265D |");
                    } else if (p == 'N') {
                        System.out.print(" \u265E |");
                    } else if (p == 'P') {
                        System.out.print(" \u265F |");
                    } else {
                        for (int j = 0; j < Character.getNumericValue(p); j++) {
                            System.out.print("    |");
                        }
                    }
                } else {
                    if (Character.isDigit(p)) {
                        for (int j = 0; j < Character.getNumericValue(p); j++) {
                            System.out.print("   |");
                        }
                    } else {
                        System.out.print(" " + p + " |");
                    }
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}
