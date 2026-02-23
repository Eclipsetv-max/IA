package mazeai;

public enum Action {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    public final int dx;
    public final int dy;

    Action(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
}

record Position(int x, int y) {
}
