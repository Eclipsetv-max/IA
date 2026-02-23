package mazeai;

import java.util.*;

public class GridWorld {
    private final int width;
    private final int height;
    private final char[][] cells;
    private final Position start;
    private final Position goal;

    public GridWorld(int width, int height, int enemies, Random random) {
        this.width = width;
        this.height = height;
        this.cells = new char[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                cells[y][x] = (border || random.nextDouble() < 0.12) ? '#' : '.';
            }
        }

        start = new Position(1, 1);
        goal = new Position(width - 2, height - 2);
        cells[start.y()][start.x()] = 'S';
        cells[goal.y()][goal.x()] = 'G';

        carveCorridor();
        placeEnemies(enemies, random);
    }

    private void carveCorridor() {
        int x = start.x();
        int y = start.y();
        while (x != goal.x()) {
            if (cells[y][x] != 'G') cells[y][x] = '.';
            x += Integer.compare(goal.x(), x);
        }
        while (y != goal.y()) {
            if (cells[y][x] != 'G') cells[y][x] = '.';
            y += Integer.compare(goal.y(), y);
        }
        cells[start.y()][start.x()] = 'S';
        cells[goal.y()][goal.x()] = 'G';
    }

    private void placeEnemies(int enemies, Random random) {
        int placed = 0;
        while (placed < enemies) {
            int x = 1 + random.nextInt(width - 2);
            int y = 1 + random.nextInt(height - 2);
            if (cells[y][x] == '.') {
                cells[y][x] = 'X';
                placed++;
            }
        }
    }

    public boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public char cell(int x, int y) {
        return cells[y][x];
    }

    public Position start() {
        return start;
    }

    public Position goal() {
        return goal;
    }

    public String render(Position agent, Set<Position> trail, Position death, int radius) {
        StringBuilder sb = new StringBuilder();
        int minY = Math.max(0, agent.y() - radius);
        int maxY = Math.min(height - 1, agent.y() + radius);
        int minX = Math.max(0, agent.x() - radius);
        int maxX = Math.min(width - 1, agent.x() + radius);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Position p = new Position(x, y);
                if (death != null && death.equals(p)) sb.append('M');
                else if (agent.equals(p)) sb.append('A');
                else if (trail.contains(p) && cells[y][x] == '.') sb.append('*');
                else sb.append(cells[y][x]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
