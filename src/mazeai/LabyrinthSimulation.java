package mazeai;

import java.util.*;

/**
 * Simulación de una IA en un laberinto grande con aprendizaje por refuerzo simple (Q-learning).
 * Incluye enemigos, oxígeno, tanque de oxígeno y narrativa emocional.
 */
public class LabyrinthSimulation {

    public static void main(String[] args) {
        SimulationConfig config = new SimulationConfig(
                120, // ancho (laberinto muy grande)
                120, // alto
                250, // enemigos
                500, // nodos de oxígeno
                6_000, // episodios máximos (muertes + intentos)
                1_500 // pasos máximos por episodio
        );

        Simulation simulation = new Simulation(config, 42L);
        simulation.run();
    }

    record Position(int x, int y) {
    }

    enum Cell {
        EMPTY('.'),
        WALL('#'),
        START('S'),
        GOAL('G'),
        OXYGEN('O'),
        TANK('T'),
        ENEMY('X');

        final char symbol;

        Cell(char symbol) {
            this.symbol = symbol;
        }
    }

    enum Action {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx;
        final int dy;

        Action(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    record SimulationConfig(
            int width,
            int height,
            int enemyCount,
            int oxygenNodes,
            int maxEpisodes,
            int maxStepsPerEpisode) {
    }

    static class Maze {
        private final int width;
        private final int height;
        private final Cell[][] grid;
        private final Position start;
        private final Position goal;

        Maze(int width, int height, int enemies, int oxygenNodes, Random random) {
            this.width = width;
            this.height = height;
            this.grid = new Cell[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean border = (x == 0 || y == 0 || x == width - 1 || y == height - 1);
                    if (border || random.nextDouble() < 0.10) {
                        grid[y][x] = Cell.WALL;
                    } else {
                        grid[y][x] = Cell.EMPTY;
                    }
                }
            }

            this.start = carveOpenNear(1, 1);
            this.goal = carveOpenNear(width - 2, height - 2);
            grid[start.y()][start.x()] = Cell.START;
            grid[goal.y()][goal.x()] = Cell.GOAL;

            scatter(Cell.OXYGEN, oxygenNodes, random);
            scatter(Cell.ENEMY, enemies, random);
            scatter(Cell.TANK, 1, random);

            ensureMainCorridor();
        }

        private Position carveOpenNear(int x, int y) {
            int nx = Math.max(1, Math.min(width - 2, x));
            int ny = Math.max(1, Math.min(height - 2, y));
            grid[ny][nx] = Cell.EMPTY;
            return new Position(nx, ny);
        }

        private void scatter(Cell type, int amount, Random random) {
            int placed = 0;
            while (placed < amount) {
                int x = 1 + random.nextInt(width - 2);
                int y = 1 + random.nextInt(height - 2);
                if (grid[y][x] == Cell.EMPTY) {
                    grid[y][x] = type;
                    placed++;
                }
            }
        }

        private void ensureMainCorridor() {
            int x = start.x();
            int y = start.y();
            while (x != goal.x()) {
                grid[y][x] = (grid[y][x] == Cell.GOAL) ? Cell.GOAL : Cell.EMPTY;
                x += Integer.compare(goal.x(), x);
            }
            while (y != goal.y()) {
                grid[y][x] = (grid[y][x] == Cell.GOAL) ? Cell.GOAL : Cell.EMPTY;
                y += Integer.compare(goal.y(), y);
            }
            grid[start.y()][start.x()] = Cell.START;
            grid[goal.y()][goal.x()] = Cell.GOAL;
        }

        Position getStart() {
            return start;
        }

        Position getGoal() {
            return goal;
        }

        boolean isInside(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height;
        }

        Cell getCell(int x, int y) {
            return grid[y][x];
        }

        void setCell(int x, int y, Cell cell) {
            grid[y][x] = cell;
        }

        String renderMiniMap(Position agent, int radius) {
            StringBuilder sb = new StringBuilder();
            int minY = Math.max(0, agent.y() - radius);
            int maxY = Math.min(height - 1, agent.y() + radius);
            int minX = Math.max(0, agent.x() - radius);
            int maxX = Math.min(width - 1, agent.x() + radius);

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (agent.x() == x && agent.y() == y) {
                        sb.append('A');
                    } else {
                        sb.append(grid[y][x].symbol);
                    }
                }
                sb.append('\n');
            }
            return sb.toString();
        }
    }

    static class Agent {
        private final Map<String, double[]> qTable = new HashMap<>();
        private final Random random;

        private double epsilon = 0.95;
        private final double epsilonDecay = 0.9992;
        private final double epsilonMin = 0.05;
        private final double alpha = 0.13;
        private final double gamma = 0.96;

        int totalDeaths = 0;
        int totalWins = 0;
        int totalEnemyEncounters = 0;
        int totalOxygenRefills = 0;

        boolean hasTank = false;
        boolean knowsHowToUseTank = false;

        Agent(Random random) {
            this.random = random;
        }

        Action chooseAction(String state) {
            if (random.nextDouble() < epsilon) {
                return Action.values()[random.nextInt(Action.values().length)];
            }
            double[] qValues = qTable.computeIfAbsent(state, k -> new double[Action.values().length]);
            int best = 0;
            for (int i = 1; i < qValues.length; i++) {
                if (qValues[i] > qValues[best]) {
                    best = i;
                }
            }
            return Action.values()[best];
        }

        void learn(String state, Action action, double reward, String nextState, boolean done) {
            double[] qValues = qTable.computeIfAbsent(state, k -> new double[Action.values().length]);
            double[] nextQ = qTable.computeIfAbsent(nextState, k -> new double[Action.values().length]);
            double maxNext = Arrays.stream(nextQ).max().orElse(0.0);
            int ai = action.ordinal();
            double target = reward + (done ? 0.0 : gamma * maxNext);
            qValues[ai] = qValues[ai] + alpha * (target - qValues[ai]);
            epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
        }

        String emotionText(int oxygen, int step, int distToGoal) {
            String existential = (totalDeaths > 180 && totalWins > 0)
                    ? "Siento un patrón... ¿esto es una simulación?" : "";

            if (oxygen < 12 && !hasTank) {
                return "Ansiedad: me falta oxígeno. " + existential;
            }
            if (distToGoal < 6) {
                return "Esperanza: estoy cerca de la salida. " + existential;
            }
            if (step < 40) {
                return "Curiosidad: analizando el entorno. " + existential;
            }
            if (totalDeaths > totalWins * 4 + 8) {
                return "Frustración: muero mucho, pero ahora entiendo más. " + existential;
            }
            return "Determinación: cada muerte mejora mi modelo interno. " + existential;
        }
    }

    static class Simulation {
        private final SimulationConfig config;
        private final Random random;
        private final Maze maze;
        private final Agent agent;

        Simulation(SimulationConfig config, long seed) {
            this.config = config;
            this.random = new Random(seed);
            this.maze = new Maze(config.width(), config.height(), config.enemyCount(), config.oxygenNodes(), random);
            this.agent = new Agent(random);
        }

        void run() {
            System.out.println("=== SIMULACIÓN IA EN LABERINTO MASIVO ===");
            System.out.printf("Mapa: %dx%d | Enemigos: %d | Oxígeno: %d%n",
                    config.width(), config.height(), config.enemyCount(), config.oxygenNodes());
            System.out.println("La IA recuerda todo tras cada muerte y aprende progresivamente.\n");

            for (int episode = 1; episode <= config.maxEpisodes(); episode++) {
                EpisodeResult result = runEpisode(episode);

                if (episode % 100 == 0 || result.win || episode == 1) {
                    printProgress(episode, result);
                }

                if (result.masteryReached) {
                    System.out.println("\n>>> MAESTRÍA ALCANZADA: la IA domina el laberinto, enemigos y oxígeno.");
                    System.out.printf("Episodio final de aprendizaje: %d%n", episode);
                    return;
                }
            }

            System.out.println("\nSe alcanzó el máximo de episodios. La IA sigue aprendiendo, aunque no llegó a maestría total.");
        }

        private EpisodeResult runEpisode(int episode) {
            Position pos = maze.getStart();
            int oxygen = 280;
            boolean alive = true;
            boolean win = false;
            boolean tookTankThisRun = false;
            int step;

            for (step = 1; step <= config.maxStepsPerEpisode(); step++) {
                String state = encodeState(pos, oxygen);
                Action action = chooseActionWithInstinct(state, pos);

                int nx = pos.x() + action.dx;
                int ny = pos.y() + action.dy;

                double reward = -0.05;

                if (!maze.isInside(nx, ny) || maze.getCell(nx, ny) == Cell.WALL) {
                    nx = pos.x();
                    ny = pos.y();
                    reward -= 0.8;
                }

                Position next = new Position(nx, ny);
                Cell cell = maze.getCell(nx, ny);

                oxygen -= agent.hasTank && agent.knowsHowToUseTank ? 0 : 1;

                if (cell == Cell.OXYGEN) {
                    oxygen = Math.min(oxygen + 28, 70);
                    reward += 2.0;
                    agent.totalOxygenRefills++;
                }

                if (cell == Cell.TANK) {
                    agent.hasTank = true;
                    tookTankThisRun = true;
                    reward += 6.0;
                    maze.setCell(nx, ny, Cell.EMPTY);
                }

                if (agent.hasTank && !agent.knowsHowToUseTank && tookTankThisRun && step > 20) {
                    agent.knowsHowToUseTank = true;
                    reward += 10.0;
                }

                if (cell == Cell.ENEMY) {
                    agent.totalEnemyEncounters++;
                    boolean survived = random.nextDouble() < (agent.totalWins > 3 ? 0.88 : 0.70);
                    if (!survived) {
                        reward -= 25.0;
                        alive = false;
                    } else {
                        reward += 1.8;
                    }
                }

                int distNow = manhattan(pos, maze.getGoal());
                int distNext = manhattan(next, maze.getGoal());
                reward += (distNow - distNext) * 0.18;

                if (oxygen <= 0) {
                    reward -= 22.0;
                    alive = false;
                }

                if (next.equals(maze.getGoal())) {
                    reward += 40.0;
                    win = true;
                    alive = false;
                    agent.totalWins++;
                }

                String nextState = encodeState(next, oxygen);
                agent.learn(state, action, reward, nextState, !alive);
                pos = next;

                if (!alive) {
                    break;
                }
            }

            if (!win) {
                agent.totalDeaths++;
            }

            boolean mastery = agent.totalWins >= 20
                    && successRatio() > 0.18
                    && agent.knowsHowToUseTank
                    && agent.totalEnemyEncounters > 60;

            return new EpisodeResult(win, step, pos, oxygen, mastery);
        }


        private Action chooseActionWithInstinct(String state, Position pos) {
            double instinctProb = Math.min(0.78, 0.08 + agent.totalDeaths / 1800.0);
            if (random.nextDouble() < instinctProb) {
                int dx = Integer.compare(maze.getGoal().x(), pos.x());
                int dy = Integer.compare(maze.getGoal().y(), pos.y());

                if (dx != 0 && dy != 0) {
                    return random.nextBoolean()
                            ? (dx > 0 ? Action.RIGHT : Action.LEFT)
                            : (dy > 0 ? Action.DOWN : Action.UP);
                }
                if (dx != 0) {
                    return dx > 0 ? Action.RIGHT : Action.LEFT;
                }
                if (dy != 0) {
                    return dy > 0 ? Action.DOWN : Action.UP;
                }
            }
            return agent.chooseAction(state);
        }

        private String encodeState(Position p, int oxygen) {
            int oxygenBucket = Math.max(0, Math.min(7, oxygen / 10));
            int gx = maze.getGoal().x() - p.x();
            int gy = maze.getGoal().y() - p.y();
            int sx = Integer.compare(gx, 0);
            int sy = Integer.compare(gy, 0);
            return p.x() + ":" + p.y() + ":o" + oxygenBucket + ":dx" + sx + ":dy" + sy
                    + ":tank" + (agent.hasTank ? 1 : 0) + ":know" + (agent.knowsHowToUseTank ? 1 : 0);
        }

        private int manhattan(Position a, Position b) {
            return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
        }

        private double successRatio() {
            int attempts = Math.max(1, agent.totalWins + agent.totalDeaths);
            return (double) agent.totalWins / attempts;
        }

        private void printProgress(int episode, EpisodeResult result) {
            int dist = manhattan(result.lastPosition(), maze.getGoal());
            String emotion = agent.emotionText(result.lastOxygen(), result.steps(), dist);

            System.out.println("--- Progreso ---");
            System.out.printf(Locale.US,
                    "Episodio: %d | Victorias: %d | Muertes: %d | Éxito: %.2f%%%n",
                    episode, agent.totalWins, agent.totalDeaths, successRatio() * 100.0);
            System.out.printf("Encuentros con enemigos: %d | Recargas de oxígeno: %d | Tanque: %s | Sabe usar tanque: %s%n",
                    agent.totalEnemyEncounters,
                    agent.totalOxygenRefills,
                    agent.hasTank ? "sí" : "no",
                    agent.knowsHowToUseTank ? "sí" : "no");
            System.out.printf("Resultado episodio: %s | Pasos: %d | Oxígeno restante: %d | Distancia a meta: %d%n",
                    result.win() ? "VICTORIA" : "DERROTA",
                    result.steps(),
                    result.lastOxygen(),
                    dist);
            System.out.println("Estado emocional IA: " + emotion);
            System.out.println("Plano local del laberinto (A = IA):");
            System.out.println(maze.renderMiniMap(result.lastPosition(), 8));
        }
    }

    record EpisodeResult(boolean win, int steps, Position lastPosition, int lastOxygen, boolean masteryReached) {
    }
}
