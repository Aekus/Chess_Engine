public class Node implements Comparable<Node>{
    public Node(short move, double value) {
        this.move = move;
        this.value = value;
    }

    @Override
    public int compareTo(Node o) {
        if (this.value > o.value) return -1;
        if (this.value == o.value) return 0;
        if (this.value < o.value) return 1;
        return 0;
    }

    short move;
    double value;
}
